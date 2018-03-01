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
import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

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
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.data.Opt;

import java.io.InputStream;

/**
 * Security layer.
 */
public class AssetManagerWithSecurity extends AssetManagerDecorator<TieredStorageAssetManager> {
  public static final String WRITE_ACTION = "write";
  public static final String READ_ACTION = "read";
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";

  private final AuthorizationService authSvc;
  private final SecurityService secSvc;

  public AssetManagerWithSecurity(
      TieredStorageAssetManager delegate,
      AuthorizationService authSvc,
      SecurityService secSvc) {
    super(delegate);
    this.authSvc = authSvc;
    this.secSvc = secSvc;
  }

  private boolean isAuthorizedByAcl(AccessControlList acl, String action) {
    final User user = secSvc.getUser();
    final Organization org = secSvc.getOrganization();
    return AccessControlUtil.isAuthorized(acl, user, org, action);
  }

  private boolean isAuthorizedByAcl(MediaPackage mp, String action) {
    final AccessControlList acl = authSvc.getActiveAcl(mp).getA();
    return isAuthorizedByAcl(acl, action);
  }

  private boolean isAuthorizedByAcl(Version version, String mpId, String action) {
    Opt<Asset> secAsset = super.getAsset(version, mpId, "security-policy-episode");
    if (secAsset.isSome()) {
      InputStream in = secAsset.get().getInputStream();
      final AccessControlList acl = authSvc.getAclFromInputStream(in).getA();
      return isAuthorizedByAcl(acl, action);
    }

    return false;
  }

 @Override public Snapshot takeSnapshot(String owner, MediaPackage mp) {
    if (isAuthorizedByAcl(mp, WRITE_ACTION)) {
      final Snapshot snapshot = super.takeSnapshot(owner, mp);
      final AccessControlList acl = authSvc.getActiveAcl(mp).getA();
      storeAclAsProperties(snapshot, acl);
      return snapshot;
    } else {
      return chuck(new UnauthorizedException("Not allowed to take snapshot of media package " + mp.getIdentifier().toString()));
    }
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    if (isAuthorized(mkAuthPredicate(mpId, WRITE_ACTION))) {
      super.setAvailability(version, mpId, availability);
    } else {
      chuck(new UnauthorizedException("Not allowed to set availability of episode " + mpId));
    }
  }

  @Override public boolean setProperty(Property property) {
    final String mpId = property.getId().getMediaPackageId();
    if (isAuthorized(mkAuthPredicate(mpId, WRITE_ACTION))) {
      return super.setProperty(property);
    } else {
      return chuck(new UnauthorizedException("Not allowed to set property on episode " + mpId));
    }
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpElementId) {
    final boolean isUserAuthorized = isAuthorized(mkAuthPredicate(mpId, READ_ACTION))
                                     || isAuthorizedByAcl(version, mpId, READ_ACTION);
    if (isUserAuthorized) {
      return super.getAsset(version, mpId, mpElementId);
    }
    else {
      return chuck(new UnauthorizedException(format("Not allowed to read assets of snapshot %s, version=%s", mpId, version)));
    }
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

  /* ------------------------------------------------------------------------------------------------------------------ */

  /** Create a new query builder. */
  private AQueryBuilder q() {
    return delegate.createQuery();
  }

  /**
   * Create an authorization predicate to be used with {@link #isAuthorized(Predicate)},
   * restricting access to the user's organization and the given action.
   *
   * @param action
   *     the action to restrict access to
   */
  private Predicate mkAuthPredicate(final String action) {
    final AQueryBuilder q = q();
    return $(secSvc.getUser().getRoles())
        .foldl(q.always().not(),
               new Fn2<Predicate, Role, Predicate>() {
                 @Override public Predicate apply(Predicate predicate, Role role) {
                   return predicate.or(mkSecurityProperty(q, role.getName(), action).eq(true));
                 }
               })
        .and(restrictToUsersOrganization());
  }

  private Predicate mkAuthPredicate(final String mpId, final String action) {
    return q().mediaPackageId(mpId).and(mkAuthPredicate(action));
  }

  /** Create a predicate that restricts access to the user's organization. */
  private Predicate restrictToUsersOrganization() {
    return q().organizationId().eq(secSvc.getUser().getOrganization().getId());
  }

  /** Check authorization based on the given predicate. */
  private boolean isAuthorized(Predicate p) {
    switch (isAdmin()) {
      case GLOBAL:
        return true;
      case ORGANIZATION:
        return true;
      default:
        final AQueryBuilder q = delegate.createQuery();
        return !q.select().where(p).run().getRecords().isEmpty();
    }
  }

  private AdminRole isAdmin() {
    final User user = secSvc.getUser();
    if (user.hasRole(GLOBAL_ADMIN_ROLE)) {
      return AdminRole.GLOBAL;
    } else if (user.hasRole(secSvc.getOrganization().getAdminRole())) {
      return AdminRole.ORGANIZATION;
    } else {
      return AdminRole.NONE;
    }
  }

  enum AdminRole {
    GLOBAL, ORGANIZATION, NONE
  }

  private void storeAclAsProperties(Snapshot snapshot, AccessControlList acl) {
    for (final AccessControlEntry ace : acl.getEntries()) {
      super.setProperty(Property.mk(
          PropertyId.mk(
              snapshot.getMediaPackage().getIdentifier().toString(),
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
}
