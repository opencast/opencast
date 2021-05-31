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
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery.DeleteSnapshotHandler;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.RemoteAssetStore;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bind an asset manager to ActiveMQ messaging.
 * <p>
 * Please make sure to {@link #close()} the AssetManager.
 */
public class AssetManagerWithMessaging implements DeleteSnapshotHandler, AutoCloseable, TieredStorageAssetManager {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerWithMessaging.class);

  protected final TieredStorageAssetManager delegate;

  public static final String WRITE_ACTION = "write";
  public static final String READ_ACTION = "read";
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";

  private final MessageSender messageSender;
  private final AuthorizationService authSvc;
  private final Workspace workspace;
  private final SecurityService secSvc;

  // Settings for role filter
  private boolean includeAPIRoles;
  private boolean includeCARoles;
  private boolean includeUIRoles;

  public AssetManagerWithMessaging(final TieredStorageAssetManager delegate, final MessageSender messageSender,
          AuthorizationService authSvc, Workspace workspace, SecurityService secSvc,
          final boolean includeAPIRoles,
          final boolean includeCARoles,
          final boolean includeUIRoles) {
    this.delegate = delegate;
    this.messageSender = messageSender;
    this.authSvc = authSvc;
    this.workspace = workspace;
    this.secSvc = secSvc;
    this.includeAPIRoles = includeAPIRoles;
    this.includeCARoles = includeCARoles;
    this.includeUIRoles = includeUIRoles;
  }

  @Override
  public void close() throws Exception {
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
      final Snapshot snapshot;
      if (owner == null) {
        snapshot = delegate.takeSnapshot(mp);
      } else {
        snapshot = delegate.takeSnapshot(owner, mp);
      }
      // We pass the original media package here, instead of using
      // snapshot.getMediaPackage(), for security reasons. The original media
      // package has elements with URLs of type http://.../files/... in it. These
      // URLs will be pulled from the Workspace cache without a HTTP call.
      //
      // Were we to use snapshot.getMediaPackage(), we'd have a HTTP call on our
      // hands that's secured via the asset manager security model. But the
      // snapshot taken here doesn't have the necessary security properties
      // installed (yet). This happens in AssetManagerWithSecurity, some layers
      // higher up. So there's a weird loop in here.
      notifyTakeSnapshot(snapshot, mp);

      final AccessControlList acl = authSvc.getActiveAcl(mp).getA();
      storeAclAsProperties(snapshot, acl);
      return snapshot;
    }
    return chuck(new UnauthorizedException("Not allowed to take snapshot of media package " + mediaPackageId));
  }

  @Override public Snapshot takeSnapshot(MediaPackage mp) {
    return takeSnapshot(null, mp);
  }

  private AQueryBuilder createQueryWithoutSecurityCheck() {
    return new AQueryBuilderDecorator(delegate.createQuery()) {
      @Override
      public ADeleteQuery delete(String owner, Target target) {
        return new ADeleteQueryWithMessaging(super.delete(owner, target));
      }
    };
  }

  @Override
  public AQueryBuilder createQuery() {
    return new AQueryBuilderDecorator(createQueryWithoutSecurityCheck()) {
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

  public void notifyTakeSnapshot(Snapshot snapshot, MediaPackage mp) {
    logger.info("Send update message for snapshot {}, {} to ActiveMQ",
            snapshot.getMediaPackage().getIdentifier().toString(), snapshot.getVersion());
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            mkTakeSnapshotMessage(snapshot, mp));
  }

  @Override
  public void notifyDeleteSnapshot(String mpId, VersionImpl version) {
    logger.info("Send delete message for snapshot {}, {} to ActiveMQ", mpId, version);
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            AssetManagerItem.deleteSnapshot(mpId, version.value(), new Date()));
  }

  @Override
  public void notifyDeleteEpisode(String mpId) {
    logger.info("Send delete message for episode {} to ActiveMQ", mpId);
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            AssetManagerItem.deleteEpisode(mpId, new Date()));
  }

  /**
   * Create a {@link TakeSnapshot} message.
   * <p>
   * Do not call outside of a security context.
   */
  TakeSnapshot mkTakeSnapshotMessage(Snapshot snapshot, MediaPackage mp) {
    final MediaPackage chosenMp;
    if (mp != null) {
      chosenMp = mp;
    } else {
      chosenMp = snapshot.getMediaPackage();
    }
    return AssetManagerItem.add(workspace, chosenMp, authSvc.getActiveAcl(chosenMp).getA(), getVersionLong(snapshot),
            snapshot.getArchivalDate());
  }

  private long getVersionLong(Snapshot snapshot) {
    try {
      return Long.parseLong(snapshot.getVersion().toString());
    } catch (NumberFormatException e) {
      // The index requires a version to be a long value.
      // Since the asset manager default implementation uses long values that should be not a problem.
      // However, a decent exception message is helpful if a different implementation of the asset manager
      // is used.
      throw new RuntimeException("The current implementation of the index requires versions being of type 'long'.");
    }
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * Call {@link org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery#run(DeleteSnapshotHandler)} with a
   * delete handler that sends messages to ActiveMQ. Also make sure to propagate the behaviour to subsequent instances.
   */
  private final class ADeleteQueryWithMessaging extends ADeleteQueryDecorator {
    ADeleteQueryWithMessaging(ADeleteQuery delegate) {
      super(delegate);
    }

    @Override
    public long run() {
      return RuntimeTypes.convert(delegate).run(AssetManagerWithMessaging.this);
    }

    @Override
    protected ADeleteQueryDecorator mkDecorator(ADeleteQuery delegate) {
      return new ADeleteQueryWithMessaging(delegate);
    }
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    if (isAuthorized(mpId, WRITE_ACTION)) {
      delegate.setAvailability(version, mpId, availability);
    } else {
      chuck(new UnauthorizedException("Not allowed to set availability of episode " + mpId));
    }
  }

  @Override public boolean setProperty(Property property) {
    final String mpId = property.getId().getMediaPackageId();
    if (isAuthorized(mpId, WRITE_ACTION)) {
      return delegate.setProperty(property);
    }
    return chuck(new UnauthorizedException("Not allowed to set property on episode " + mpId));
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpElementId) {
    if (isAuthorized(mpId, READ_ACTION)) {
      return delegate.getAsset(version, mpId, mpElementId);
    }
    return chuck(new UnauthorizedException(
            format("Not allowed to read assets of snapshot %s, version=%s", mpId, version)
    ));
  }

  @Override
  public List<Property> selectProperties(final String mediaPackageId, String namespace) {
    if (isAuthorized(mediaPackageId, READ_ACTION)) {
      return delegate.selectProperties(mediaPackageId, namespace);
    }
    return chuck(new UnauthorizedException(format("Not allowed to read properties of event %s", mediaPackageId)));
  }

  @Override
  public Set<String> getRemoteAssetStoreIds() {
    return delegate.getRemoteAssetStoreIds();
  }

  @Override
  public void addRemoteAssetStore(RemoteAssetStore assetStore) {
    delegate.addRemoteAssetStore(assetStore);
  }

  @Override
  public void removeRemoteAssetStore(RemoteAssetStore assetStore) {
    delegate.removeRemoteAssetStore(assetStore);
  }

  @Override public Opt<MediaPackage> getMediaPackage(String mediaPackageId) {
    return delegate.getMediaPackage(mediaPackageId);
  }

  @Override
  public int deleteProperties(final String mediaPackageId) {
    return delegate.deleteProperties(mediaPackageId);
  }

  @Override
  public int deleteProperties(final String mediaPackageId, final String namespace) {
    return delegate.deleteProperties(mediaPackageId, namespace);
  }

  @Override
  public long countEvents(String organization) {
    return delegate.countEvents(organization);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId, final String organization) {
    return delegate.snapshotExists(mediaPackageId, organization);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId) {
    return delegate.snapshotExists(mediaPackageId);
  }

  @Override public Opt<Version> toVersion(String version) {
    return delegate.toVersion(version);
  }

  @Override
  public Opt<AssetStore> getRemoteAssetStore(String id) {
    return delegate.getRemoteAssetStore(id);
  }

  @Override
  public Opt<AssetStore> getAssetStore(String storeId) {
    return delegate.getAssetStore(storeId);
  }

  @Override
  public void moveSnapshotToStore(Version version, String mpId, String storeId) throws NotFoundException {
    delegate.moveSnapshotToStore(version, mpId, storeId);
  }

  @Override
  public RichAResult getSnapshotsById(String mpId) {
    return delegate.getSnapshotsById(mpId);
  }

  @Override
  public void moveSnapshotsById(String mpId, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsById(mpId, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByIdAndVersion(String mpId, Version version) {
    return delegate.getSnapshotsByIdAndVersion(mpId, version);
  }

  @Override
  public void moveSnapshotsByIdAndVersion(String mpId, Version version, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByIdAndVersion(mpId, version, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByDate(Date start, Date end) {
    return delegate.getSnapshotsByDate(start, end);
  }

  @Override
  public void moveSnapshotsByDate(Date start, Date end, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByDate(start, end, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByIdAndDate(String mpId, Date start, Date end) {
    return delegate.getSnapshotsByIdAndDate(mpId, start, end);
  }

  @Override
  public void moveSnapshotsByIdAndDate(String mpId, Date start, Date end, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByIdAndDate(mpId, start, end, targetStore);
  }

  @Override
  public Opt<String> getSnapshotStorageLocation(Version version, String mpId) throws NotFoundException {
    return delegate.getSnapshotStorageLocation(version, mpId);
  }

  @Override
  public Opt<String> getSnapshotStorageLocation(Snapshot snap) throws NotFoundException {
    return delegate.getSnapshotStorageLocation(snap);
  }

  @Override
  public Opt<String> getSnapshotRetrievalTime(Version version, String mpId) {
    return delegate.getSnapshotRetrievalTime(version, mpId);
  }

  @Override
  public Opt<String> getSnapshotRetrievalCost(Version version, String mpId) {
    return delegate.getSnapshotRetrievalCost(version, mpId);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Create an authorization predicate to be used with {@link #isAuthorized(String, String)},
   * restricting access to the user's organization and the given action.
   *
   * @param action
   *     the action to restrict access to
   */
  private Predicate mkAuthPredicate(final String action) {
    final AQueryBuilder q = createQueryWithoutSecurityCheck();
    return secSvc.getUser().getRoles().stream()
            .filter(roleFilter)
            .map((role) -> mkSecurityProperty(q, role.getName(), action).eq(true))
            .reduce(Predicate::or)
            .orElseGet(() -> q.always().not())
            .and(restrictToUsersOrganization());
  }

  /** Create a predicate that restricts access to the user's organization. */
  private Predicate restrictToUsersOrganization() {
    return createQueryWithoutSecurityCheck().organizationId().eq(secSvc.getUser().getOrganization().getId());
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
        return delegate.selectProperties(mediaPackageId, SECURITY_NAMESPACE).parallelStream()
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
    deleteProperties(mediaPackageId, SECURITY_NAMESPACE);
    // Set new ACL rules
    for (final AccessControlEntry ace : acl.getEntries()) {
      delegate.setProperty(Property.mk(
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
