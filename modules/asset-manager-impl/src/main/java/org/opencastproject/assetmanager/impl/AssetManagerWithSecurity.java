/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.Prelude.chuck;
import static java.lang.String.format;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Security layer.
 */
public class AssetManagerWithSecurity extends AssetManagerDecorator<TieredStorageAssetManager> {
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerWithSecurity.class);

  public static final String WRITE_ACTION = "write";
  public static final String READ_ACTION = "read";
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";

  private final AuthorizationService authSvc;
  private final SecurityService secSvc;

  // Settings for role filter
  private boolean includeAPIRoles;
  private boolean includeCARoles;
  private boolean includeUIRoles;

  public AssetManagerWithSecurity(
      TieredStorageAssetManager delegate,
      AuthorizationService authSvc,
      SecurityService secSvc,
      final boolean includeAPIRoles,
      final boolean includeCARoles,
      final boolean includeUIRoles) {
    super(delegate);
    this.authSvc = authSvc;
    this.secSvc = secSvc;
    this.includeAPIRoles = includeAPIRoles;
    this.includeCARoles = includeCARoles;
    this.includeUIRoles = includeUIRoles;
  }

  @Override public Snapshot takeSnapshot(String owner, MediaPackage mp) {

    final String mediaPackageId = mp.getIdentifier().toString();
    final boolean firstSnapshot = !snapshotExists(mediaPackageId);

    // Allow this if:
    //  - no previous snapshot exists
    //  - the user has write access to the previous snapshot
    if (firstSnapshot) {
      // if it's the first snapshot, ensure that old, leftover properties are removed
      deleteProperties(mediaPackageId);
    }
    if (firstSnapshot || isAuthorized(mediaPackageId, WRITE_ACTION)) {
      final Snapshot snapshot = super.takeSnapshot(owner, mp);
      final AccessControlList acl = authSvc.getActiveAcl(mp).getA();
      storeAclAsProperties(snapshot, acl);
      return snapshot;
    }
    return chuck(new UnauthorizedException("Not allowed to take snapshot of media package " + mediaPackageId));
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    if (isAuthorized(mpId, WRITE_ACTION)) {
      super.setAvailability(version, mpId, availability);
    } else {
      chuck(new UnauthorizedException("Not allowed to set availability of episode " + mpId));
    }
  }

  @Override public boolean setProperty(Property property) {
    final String mpId = property.getId().getMediaPackageId();
    if (isAuthorized(mpId, WRITE_ACTION)) {
      return super.setProperty(property);
    }
    return chuck(new UnauthorizedException("Not allowed to set property on episode " + mpId));
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpElementId) {
    if (isAuthorized(mpId, READ_ACTION)) {
      return super.getAsset(version, mpId, mpElementId);
    }
    return chuck(new UnauthorizedException(
        format("Not allowed to read assets of snapshot %s, version=%s", mpId, version)
    ));
  }

  @Override public AQueryBuilder createQuery() {
    return new AQueryBuilderDecorator(super.createQuery()) {
      @Override public ASelectQuery select(Target... target) {
        switch (isAdmin()) {
          case GLOBAL:
            return super.select(target);
          case ORGANIZATION:
            return super.select(target).where(restrictToUsersOrganization());
          default:
            return super.select(target).where(mkAuthPredicate(READ_ACTION));
        }
      }

      @Override public ADeleteQuery delete(String owner, Target target) {
        switch (isAdmin()) {
          case GLOBAL:
            return super.delete(owner, target);
          case ORGANIZATION:
            return super.delete(owner, target).where(restrictToUsersOrganization());
          default:
            return super.delete(owner, target).where(mkAuthPredicate(WRITE_ACTION));
        }
      }
    };
  }

  @Override
  public List<Property> selectProperties(final String mediaPackageId, String namespace) {
    if (isAuthorized(mediaPackageId, READ_ACTION)) {
      return super.selectProperties(mediaPackageId, namespace);
    }
    return chuck(new UnauthorizedException(format("Not allowed to read properties of event %s", mediaPackageId)));
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /** Create a new query builder. */
  private AQueryBuilder q() {
    return delegate.createQuery();
  }

  /**
   * Create an authorization predicate to be used with {@link #isAuthorized(String, String)},
   * restricting access to the user's organization and the given action.
   *
   * @param action
   *     the action to restrict access to
   */
  private Predicate mkAuthPredicate(final String action) {
    final AQueryBuilder q = q();
    return secSvc.getUser().getRoles().stream()
            .filter(roleFilter)
            .map((role) -> mkSecurityProperty(q, role.getName(), action).eq(true))
            .reduce(Predicate::or)
            .orElseGet(() -> q.always().not())
            .and(restrictToUsersOrganization());
  }

  /** Create a predicate that restricts access to the user's organization. */
  private Predicate restrictToUsersOrganization() {
    return q().organizationId().eq(secSvc.getUser().getOrganization().getId());
  }

  /** Check authorization based on the given predicate. */
  private boolean isAuthorized(final String mediaPackageId, final String action) {
    switch (isAdmin()) {
      case GLOBAL:
        // grant general access
        logger.debug("Access granted since user is global admin");
        return true;
      case ORGANIZATION:
        // ensure that the requested assets belong to this organization
        logger.debug("User is organization admin. Checking organization. Checking organization ID of asset.");
        return snapshotExists(mediaPackageId, secSvc.getOrganization().getId());
      default:
        // check organization
        logger.debug("Non admin user. Checking organization.");
        final String org = secSvc.getOrganization().getId();
        if (!snapshotExists(mediaPackageId, org)) {
          return false;
        }
        // check acl rules
        logger.debug("Non admin user. Checking ACL rules.");
        final List<String> roles = secSvc.getUser().getRoles().parallelStream()
                .filter(roleFilter)
                .map((role) -> mkPropertyName(role.getName(), action))
                .collect(Collectors.toList());
        return super.selectProperties(mediaPackageId, SECURITY_NAMESPACE).parallelStream()
                .map(p -> p.getId().getName())
                .anyMatch(p -> roles.parallelStream().anyMatch(r -> r.equals(p)));
    }
  }

  private AdminRole isAdmin() {
    final User user = secSvc.getUser();
    if (user.hasRole(GLOBAL_ADMIN_ROLE)) {
      return AdminRole.GLOBAL;
    } else if (user.hasRole(secSvc.getOrganization().getAdminRole())
            || user.hasRole(GLOBAL_CAPTURE_AGENT_ROLE)) {
      // In this context, we treat capture agents the same way as organization admins, allowing them access so that
      // they can ingest new media without requiring them to be explicitly specified in the ACLs.
      return AdminRole.ORGANIZATION;
    } else {
      return AdminRole.NONE;
    }
  }

  enum AdminRole {
    GLOBAL, ORGANIZATION, NONE
  }

  /**
   * Update the ACL properties. Note that this method assumes proper proper authorization.
   *
   * @param snapshot
   *          Snapshot to reference the media package identifier
   * @param acl
   *          ACL to set
   */
  private void storeAclAsProperties(Snapshot snapshot, AccessControlList acl) {
    final String mediaPackageId =  snapshot.getMediaPackage().getIdentifier().toString();
    // Drop old ACL rules
    super.deleteProperties(mediaPackageId, SECURITY_NAMESPACE);
    // Set new ACL rules
    for (final AccessControlEntry ace : acl.getEntries()) {
      super.setProperty(Property.mk(
          PropertyId.mk(
              mediaPackageId,
              SECURITY_NAMESPACE,
              mkPropertyName(ace)),
          Value.mk(ace.isAllow())));
    }
  }

  private PropertyField<Boolean> mkSecurityProperty(AQueryBuilder q, String role, String action) {
    return q.property(Value.BOOLEAN, SECURITY_NAMESPACE, mkPropertyName(role, action));
  }

  private String mkPropertyName(AccessControlEntry ace) {
    return mkPropertyName(ace.getRole(), ace.getAction());
  }

  private String mkPropertyName(String role, String action) {
    return role + " | " + action;
  }

  /**
   * Configurable filter for roles
   */
  private java.util.function.Predicate<Role> roleFilter = (role) -> {
    final String name = role.getName();
    return (includeAPIRoles || !name.startsWith("ROLE_API_"))
        && (includeCARoles  || !name.startsWith("ROLE_CAPTURE_AGENT_"))
        && (includeUIRoles  || !name.startsWith("ROLE_UI_"));
  };
}
