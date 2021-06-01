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
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.hasNoChecksum;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;
import static org.opencastproject.mediapackage.MediaPackageSupport.getFileName;
import static org.opencastproject.mediapackage.MediaPackageSupport.getMediaPackageElementId;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;
import org.opencastproject.assetmanager.impl.query.AQueryBuilderImpl;
import org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildException;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.P1Lazy;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Prelude;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Booleans;
import com.entwinemedia.fn.fns.Strings;
import com.google.common.collect.Sets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

/**
 * // TODO
 */
@Component(
    property = {
        "service.description=Opencast Asset Manager"
    },
    immediate = true,
    service = { AssetManager.class, IndexProducer.class }
)
public class AssetManagerImpl extends AbstractIndexProducer implements AssetManager,
        AbstractADeleteQuery.DeleteSnapshotHandler {
  /**
   * Log facility
   */
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerImpl.class);

  public static final String WRITE_ACTION = "write";
  public static final String READ_ACTION = "read";
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";
  // Base name of manifest file
  private static final String MANIFEST_DEFAULT_NAME = "manifest";

  private SecurityService securityService;
  private AuthorizationService authorizationService;
  private OrganizationDirectoryService orgDir;
  private Workspace workspace;
  private AssetStore assetStore;
  private HttpAssetProvider httpAssetProvider;
  private MessageSender messageSender;
  private String systemUserName;
  private Database db;

  // Settings for role filter
  private boolean includeAPIRoles;
  private boolean includeCARoles;
  private boolean includeUIRoles;

  public static final Set<MediaPackageElement.Type> MOVABLE_TYPES = Sets.newHashSet(
          MediaPackageElement.Type.Attachment,
          MediaPackageElement.Type.Catalog,
          MediaPackageElement.Type.Track
  );

  private HashMap<String, RemoteAssetStore> remoteStores = new LinkedHashMap<>();


  /**
   * OSGi callback.
   */
  @Activate
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activating AssetManager");
    systemUserName = SecurityUtil.getSystemUserName(cc);

    includeAPIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeAPIRoles"), null));
    includeCARoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeCARoles"), null));
    includeUIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeUIRoles"), null));
  }


  //
  // OSGi depedency injection
  //

  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.assetmanager.impl)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.db = new Database(emf);
  }

  @Reference(name = "securityService")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference(name = "authSvc")
  public void setAuthSvc(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Reference(name = "orgDir")
  public void setOrgDir(OrganizationDirectoryService orgDir) {
    this.orgDir = orgDir;
  }

  @Reference(name = "workspace")
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference(name = "assetStore")
  public void setAssetStore(AssetStore assetStore) {
    this.assetStore = assetStore;
  }

  @Reference(
      name = "remoteAssetStores",
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeRemoteAssetStore"
  )
  @Override
  public synchronized void addRemoteAssetStore(RemoteAssetStore assetStore) {
    remoteStores.put(assetStore.getStoreType(), assetStore);

  }

  @Reference(name = "httpAssetProvider")
  public void setHttpAssetProvider(HttpAssetProvider httpAssetProvider) {
    this.httpAssetProvider = httpAssetProvider;
  }

  @Reference(name = "messageSender")
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * AbstractIndexProducer Implementation
   */

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.AssetManager;
  }

  @Override
  public void repopulate(final AbstractSearchIndex index) throws IndexRebuildException {
    final Organization org = securityService.getOrganization();
    final User user = (org != null ? securityService.getUser() : null);
    try {
      final Organization defaultOrg = new DefaultOrganization();
      final User systemUser = SecurityUtil.createSystemUser(systemUserName, defaultOrg);
      securityService.setOrganization(defaultOrg);
      securityService.setUser(systemUser);

      final AQueryBuilder q = createQuery();
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.version().isLatest()).run());
      final int total = r.countSnapshots();
      int current = 0;
      logIndexRebuildBegin(logger, index.getIndexName(), total, "snapshot(s)");

      final Map<String, List<Snapshot>> byOrg = r.getSnapshots().groupMulti(Snapshots.getOrganizationId);
      for (String orgId : byOrg.keySet()) {
        final Organization snapshotOrg;
        try {
          snapshotOrg = orgDir.getOrganization(orgId);
          securityService.setOrganization(snapshotOrg);
          securityService.setUser(SecurityUtil.createSystemUser(systemUserName, snapshotOrg));

          for (Snapshot snapshot : byOrg.get(orgId)) {
            current += 1;
            try {
              AssetManagerItem.TakeSnapshot takeSnapshot = mkTakeSnapshotMessage(snapshot, null);
              messageSender.sendObjectMessage(
                      AssetManagerItem.ASSETMANAGER_QUEUE_PREFIX + WordUtils.capitalize(index.getIndexName()),
                      MessageSender.DestinationType.Queue, takeSnapshot);
            } catch (Throwable t) {
              logSkippingElement(logger, "event", snapshot.getMediaPackage().getIdentifier().toString(), org, t);
            }
            logIndexRebuildProgress(logger, index.getIndexName(), total, current);
          }
        } catch (Throwable t) {
          logIndexRebuildError(logger, index.getIndexName(), t, org);
          throw new IndexRebuildException(index.getIndexName(), getService(), org, t);
        } finally {
          securityService.setOrganization(defaultOrg);
          securityService.setUser(systemUser);
        }
      }
    } finally {
      securityService.setOrganization(org);
      securityService.setUser(user);
    }
  }

  /////////////////////////////////// NEW

  @Override public Snapshot takeSnapshot(MediaPackage mp) {
    return takeSnapshot(null, mp);
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
        snapshot = takeSnapshotInternal(mp);
      } else {
        snapshot = takeSnapshotInternal(owner, mp);
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

      final AccessControlList acl = authorizationService.getActiveAcl(mp).getA();
      storeAclAsProperties(snapshot, acl);
      return snapshot;
    }
    return chuck(new UnauthorizedException("Not allowed to take snapshot of media package " + mediaPackageId));
  }

  private Snapshot takeSnapshotInternal(MediaPackage mediaPackage) {
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    AQueryBuilder queryBuilder = createQuery();
    AResult result = queryBuilder.select(queryBuilder.snapshot())
            .where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest())).run();
    Opt<ARecord> record = result.getRecords().head();
    if (record.isSome()) {
      Opt<Snapshot> snapshot = record.get().getSnapshot();
      if (snapshot.isSome()) {
        return takeSnapshotInternal(snapshot.get().getOwner(), mediaPackage);
      }
    }
    return takeSnapshotInternal(DEFAULT_OWNER, mediaPackage);
  }

  private Snapshot takeSnapshotInternal(final String owner, final MediaPackage mp) {
    return handleException(new P1Lazy<Snapshot>() {
      @Override public Snapshot get1() {
        try {
          final Snapshot archived = addInternal(owner, MediaPackageSupport.copy(mp)).toSnapshot();
          return getHttpAssetProvider().prepareForDelivery(archived);
        } catch (Exception e) {
          return Prelude.chuck(e);
        }
      }
    });
  }

  private AQueryBuilder createQueryWithoutSecurityCheck() {
    return new AQueryBuilderDecorator(new AQueryBuilderImpl(this)) {
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
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, MessageSender.DestinationType.Queue,
            mkTakeSnapshotMessage(snapshot, mp));
  }

  @Override
  public void notifyDeleteSnapshot(String mpId, VersionImpl version) {
    logger.info("Send delete message for snapshot {}, {} to ActiveMQ", mpId, version);
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, MessageSender.DestinationType.Queue,
            AssetManagerItem.deleteSnapshot(mpId, version.value(), new Date()));
  }

  @Override
  public void notifyDeleteEpisode(String mpId) {
    logger.info("Send delete message for episode {} to ActiveMQ", mpId);
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, MessageSender.DestinationType.Queue,
            AssetManagerItem.deleteEpisode(mpId, new Date()));
  }

  /**
   * Create a {@link AssetManagerItem.TakeSnapshot} message.
   * <p>
   * Do not call outside of a security context.
   */
  AssetManagerItem.TakeSnapshot mkTakeSnapshotMessage(Snapshot snapshot, MediaPackage mp) {
    final MediaPackage chosenMp;
    if (mp != null) {
      chosenMp = mp;
    } else {
      chosenMp = snapshot.getMediaPackage();
    }
    return AssetManagerItem.add(workspace, chosenMp, authorizationService.getActiveAcl(chosenMp).getA(),
            getVersionLong(snapshot),
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

  // used for testing
  public void setDatabase(Database database) {
    this.db = database;
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * Call {@link
   * org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery#run(AbstractADeleteQuery.DeleteSnapshotHandler)}
   * with a delete handler that sends messages to ActiveMQ. Also make sure to propagate the behaviour to subsequent
   * instances.
   */
  private final class ADeleteQueryWithMessaging extends ADeleteQueryDecorator {
    ADeleteQueryWithMessaging(ADeleteQuery delegate) {
      super(delegate);
    }

    @Override
    public long run() {
      return RuntimeTypes.convert(delegate).run(AssetManagerImpl.this);
    }

    @Override
    protected ADeleteQueryDecorator mkDecorator(ADeleteQuery delegate) {
      return new ADeleteQueryWithMessaging(delegate);
    }
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    if (isAuthorized(mpId, WRITE_ACTION)) {
      getDb().setAvailability(RuntimeTypes.convert(version), mpId, availability);
    } else {
      chuck(new UnauthorizedException("Not allowed to set availability of episode " + mpId));
    }
  }

  @Override public boolean setProperty(Property property) {
    final String mpId = property.getId().getMediaPackageId();
    if (isAuthorized(mpId, WRITE_ACTION)) {
      return getDb().saveProperty(property);
    }
    return chuck(new UnauthorizedException("Not allowed to set property on episode " + mpId));
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpElementId) {
    if (isAuthorized(mpId, READ_ACTION)) {
      // try to fetch the asset
      for (final AssetDtos.Medium asset : getDb().getAsset(RuntimeTypes.convert(version), mpId, mpElementId)) {
        for (final String storageId : getSnapshotStorageLocation(version, mpId)) {
          for (final AssetStore store : getAssetStore(storageId)) {
            for (final InputStream assetStream
                    : store.get(StoragePath.mk(asset.getOrganizationId(), mpId, version, mpElementId))) {

              Checksum checksum = null;
              try {
                checksum = Checksum.fromString(asset.getAssetDto().getChecksum());
              } catch (NoSuchAlgorithmException e) {
                logger.warn("Invalid checksum for asset {} of media package {}", mpElementId, mpId, e);
              }

              final Asset a = new AssetImpl(
                      AssetId.mk(version, mpId, mpElementId),
                      assetStream,
                      asset.getAssetDto().getMimeType(),
                      asset.getAssetDto().getSize(),
                      asset.getStorageId(),
                      asset.getAvailability(),
                      checksum);
              return Opt.some(a);
            }
          }
        }
      }
      return Opt.none();
    }
    return chuck(new UnauthorizedException(
            format("Not allowed to read assets of snapshot %s, version=%s", mpId, version)
    ));
  }

  @Override
  public List<Property> selectProperties(final String mediaPackageId, String namespace) {
    if (isAuthorized(mediaPackageId, READ_ACTION)) {
      return getDb().selectProperties(mediaPackageId, namespace);
    }
    return chuck(new UnauthorizedException(format("Not allowed to read properties of event %s", mediaPackageId)));
  }

  @Override
  public Set<String> getRemoteAssetStoreIds() {
    return remoteStores.keySet();
  }

  @Override
  public Opt<AssetStore> getRemoteAssetStore(String id) {
    if (remoteStores.containsKey(id)) {
      return Opt.some(remoteStores.get(id));
    } else {
      return Opt.none();
    }
  }

  @Override
  public void removeRemoteAssetStore(RemoteAssetStore store) {
    remoteStores.remove(store.getStoreType());
  }

  @Override
  public Opt<MediaPackage> getMediaPackage(String mediaPackageId) {
    final AQueryBuilder q = createQuery();
    final AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mediaPackageId).and(q.version().isLatest()))
            .run();

    if (r.getSize() == 0) {
      return Opt.none();
    }
    return Opt.some(r.getRecords().head2().getSnapshot().get().getMediaPackage());
  }

  @Override
  public int deleteProperties(final String mediaPackageId) {
    return getDb().deleteProperties(mediaPackageId);
  }

  @Override
  public int deleteProperties(final String mediaPackageId, final String namespace) {
    return getDb().deleteProperties(mediaPackageId, namespace);
  }

  @Override
  public long countEvents(final String organization) {
    return getDb().countEvents(organization);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId) {
    return getDb().snapshotExists(mediaPackageId);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId, final String organization) {
    return getDb().snapshotExists(mediaPackageId, organization);
  }


  @Override public Opt<Version> toVersion(String version) {
    try {
      return Opt.<Version>some(VersionImpl.mk(Long.parseLong(version)));
    } catch (NumberFormatException e) {
      return Opt.none();
    }
  }


  public RichAResult getSnapshotsById(final String mpId) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId);
    return Enrichments.enrich(query.run());
  }

  public void moveSnapshotsById(final String mpId, final String targetStore) throws NotFoundException {
    RichAResult results = getSnapshotsById(mpId);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("Mediapackage " + mpId + " not found!");
    }

    processOperations(results, targetStore);
  }

  public RichAResult getSnapshotsByIdAndVersion(final String mpId, final Version version) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    RequireUtil.notNull(version, "version");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, version, mpId);
    return Enrichments.enrich(query.run());
  }

  public void moveSnapshotsByIdAndVersion(final String mpId, final Version version, final String targetStore)
          throws NotFoundException {
    RichAResult results = getSnapshotsByIdAndVersion(mpId, version);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("Mediapackage " + mpId + "@" + version.toString() + " not found!");
    }

    processOperations(results, targetStore);
  }

  public RichAResult getSnapshotsByDate(final Date start, final Date end) {
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q).where(q.archived().ge(start)).where(q.archived().le(end));
    return Enrichments.enrich(query.run());
  }

  public void moveSnapshotsByDate(final Date start, final Date end, final String targetStore)
          throws NotFoundException {
    RichAResult results = getSnapshotsByDate(start, end);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("No media packages found between " + start + " and " + end);
    }

    processOperations(results, targetStore);
  }

  public RichAResult getSnapshotsByIdAndDate(final String mpId, final Date start, final Date end) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId).where(q.archived().ge(start)).where(q.archived().le(end));
    return Enrichments.enrich(query.run());
  }

  public void moveSnapshotsByIdAndDate(final String mpId, final Date start, final Date end, final String targetStore)
          throws NotFoundException {
    RichAResult results = getSnapshotsByDate(start, end);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("No media package with id " + mpId + " found between " + start + " and " + end);
    }

    processOperations(results, targetStore);
  }

  // Return the asset store ID that is currently storing the snapshot
  public Opt<String> getSnapshotStorageLocation(final Version version, final String mpId) {
    RichAResult result = getSnapshotsByIdAndVersion(mpId, version);

    for (Snapshot snapshot : result.getSnapshots()) {
      return Opt.some(snapshot.getStorageId());
    }

    logger.error("Mediapackage " + mpId + "@" + version + " not found!");
    return Opt.none();
  }

  public Opt<String> getSnapshotStorageLocation(final Snapshot snap) {
    return getSnapshotStorageLocation(snap.getVersion(), snap.getMediaPackage().getIdentifier().toString());
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
    return securityService.getUser().getRoles().stream()
            .filter(roleFilter)
            .map((role) -> mkSecurityProperty(q, role.getName(), action).eq(true))
            .reduce(Predicate::or)
            .orElseGet(() -> q.always().not())
            .and(restrictToUsersOrganization());
  }

  /** Create a predicate that restricts access to the user's organization. */
  private Predicate restrictToUsersOrganization() {
    return createQueryWithoutSecurityCheck().organizationId().eq(securityService.getUser().getOrganization().getId());
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
        return snapshotExists(mediaPackageId, securityService.getOrganization().getId());
      default:
        // check organization
        logger.debug("Non admin user. Checking organization.");
        final String org = securityService.getOrganization().getId();
        if (!snapshotExists(mediaPackageId, org)) {
          return false;
        }
        // check acl rules
        logger.debug("Non admin user. Checking ACL rules.");
        final List<String> roles = securityService.getUser().getRoles().parallelStream()
                .filter(roleFilter)
                .map((role) -> mkPropertyName(role.getName(), action))
                .collect(Collectors.toList());
        return getDb().selectProperties(mediaPackageId, SECURITY_NAMESPACE).parallelStream()
                .map(p -> p.getId().getName())
                .anyMatch(p -> roles.parallelStream().anyMatch(r -> r.equals(p)));
    }
  }

  private AdminRole isAdmin() {
    final User user = securityService.getUser();
    if (user.hasRole(GLOBAL_ADMIN_ROLE)) {
      return AdminRole.GLOBAL;
    } else if (user.hasRole(securityService.getOrganization().getAdminRole())
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
      getDb().saveProperty(Property.mk(
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

  //////////// ASSET MANAGER WITH TIERED STORAGE

  public Database getDb() {
    return db;
  }

  public HttpAssetProvider getHttpAssetProvider() {
    return httpAssetProvider;
  }

  public AssetStore getLocalAssetStore() {
    return assetStore;
  }

  protected Workspace getWorkspace() {
    return workspace;
  }

  protected String getCurrentOrgId() {
    return securityService.getOrganization().getId();
  }

  public Opt<AssetStore> getAssetStore(String storeId) {
    if (getLocalAssetStore().getStoreType().equals(storeId)) {
      return Opt.some(getLocalAssetStore());
    } else {
      return getRemoteAssetStore(storeId);
    }
  }

  //Move snapshot from current store to this store
  //Note: This may require downloading and re-uploading
  public void moveSnapshotToStore(final Version version, final String mpId, final String storeId)
          throws NotFoundException {

    //Find the snapshot
    AQueryBuilder q = createQuery();
    RichAResult results = Enrichments.enrich(baseQuery(q, version, mpId).run());

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("Mediapackage " + mpId + "@" + version.toString() + " not found!");
    }
    processOperations(results, storeId);
  }

  //Do the actual moving
  private void processOperations(final RichAResult results, final String targetStoreId) {
    results.getRecords().forEach(new Consumer<ARecord>() {
      @Override
      public void accept(ARecord record) {
        Snapshot s = record.getSnapshot().get();
        Opt<String> currentStoreId = getSnapshotStorageLocation(s);

        if (currentStoreId.isNone()) {
          logger.warn("IsNone store ID");
          return;
        }

        //If this snapshot is already stored in the desired store
        if (currentStoreId.get().equals(targetStoreId)) {
          //return, since we don't need to move anything
          return;
        }

        AssetStore currentStore = null;
        AssetStore targetStore = null;

        Opt<AssetStore> optCurrentStore = getAssetStore(currentStoreId.get());
        Opt<AssetStore> optTargetStore = getAssetStore(targetStoreId);

        if (!optCurrentStore.isNone()) {
          currentStore = optCurrentStore.get();
        } else {
          logger.error("Unknown current store: " + currentStoreId.get());
          return;
        }
        if (!optTargetStore.isNone()) {
          targetStore = optTargetStore.get();
        } else {
          logger.error("Unknown target store: " + targetStoreId);
          return;
        }

        //If the content is already local, or is moving from a remote to the local
        if (isLocalAssetStoreId(currentStoreId.get()) || isLocalAssetStoreId(targetStoreId)) {
          logger.debug("Moving {} from {} to {}", s.toString(), currentStoreId, targetStoreId);

          try {
            copyAssetsToStore(s, targetStore);
            copyManifest(s, targetStore);
          } catch (Exception e) {
            Functions.chuck(e);
          }
          getDb().setStorageLocation(s, targetStoreId);
          deleteAssetsFromStore(s, currentStore);
        } else {
          //Else, the content is *not* local and is going to a *different* remote
          String intermediateStore = getLocalAssetStore().getStoreType();
          logger.debug("Moving {} from {} to {}, then to {}",
                  s.toString(), currentStoreId, intermediateStore, targetStoreId);
          Version version = s.getVersion();
          String mpId = s.getMediaPackage().getIdentifier().toString();
          try {
            moveSnapshotToStore(version, mpId, intermediateStore);
            moveSnapshotToStore(version, mpId, targetStoreId);
          } catch (NotFoundException e) {
            Functions.chuck(e);
          }
        }
      }
    });
  }

  /**
   * Return a basic query which returns the snapshot and its current storage location
   *
   * @param q
   *   The query builder object to configure
   * @return
   *   The {@link ASelectQuery} configured with as described above
   */
  private ASelectQuery baseQuery(final AQueryBuilder q) {
    RequireUtil.notNull(q, "q");
    return q.select(q.snapshot());
  }

  /**
   * Return a mediapackage filtered query which returns the snapshot and its current storage location
   *
   * @param q
   *   The query builder object to configure
   * @param mpId
   *   The mediapackage ID to filter results for
   * @return
   *   The {@link ASelectQuery} configured with as described above
   */
  private ASelectQuery baseQuery(final AQueryBuilder q, final String mpId) {
    RequireUtil.notNull(q, "q");
    ASelectQuery query = baseQuery(q);
    if (StringUtils.isNotEmpty(mpId)) {
      return query.where(q.mediaPackageId(mpId));
    } else {
      return query;
    }
  }

  /**
   * Return a mediapackage and version filtered query which returns the snapshot and its current storage location
   *
   * @param q
   *   The query builder object to configure
   * @param version
   *   The version to filter results for
   * @param mpId
   *   The mediapackage ID to filter results for
   * @return
   *   The {@link ASelectQuery} configured with as described above
   */
  private ASelectQuery baseQuery(final AQueryBuilder q, final Version version, final String mpId) {
    RequireUtil.notNull(q, "q");
    RequireUtil.requireNotBlank(mpId, "mpId");
    ASelectQuery query = baseQuery(q, mpId);
    if (null != version) {
      return query.where(q.version().eq(version));
    } else {
      return query;
    }
  }

  /** Returns a query to find locally stored snapshots */
  private ASelectQuery getStoredLocally(final AQueryBuilder q, final Version version, final String mpId) {
    return baseQuery(q, version, mpId).where(q.storage(getLocalAssetStore().getStoreType()));
  }

  /** Returns a query to find remotely stored snapshots */
  private ASelectQuery getStoredRemotely(final AQueryBuilder q, final Version version, final String mpId) {
    return baseQuery(q, version, mpId).where(q.storage(getLocalAssetStore().getStoreType()).not());
  }

  /** Returns a query to find remotely stored snapshots in a specific store */
  private ASelectQuery getStoredInStore(
          final AQueryBuilder q,
          final Version version,
          final String mpId,
          final String storeId
  ) {
    return baseQuery(q, version, mpId).where(q.storage(storeId));
  }

  /** Returns true if the store id is equal to the local asset store's id */
  private boolean isLocalAssetStoreId(String storeId) {
    return getLocalAssetStore().getStoreType().equals(storeId);
  }

  /** Returns true if the store id is not equal to the local asset store's id */
  private boolean isRemoteAssetStoreId(String storeId) {
    return !isLocalAssetStoreId(storeId);
  }

  /** Move the assets for a snapshot to the target store */
  private void copyAssetsToStore(Snapshot snap, AssetStore store) throws Exception {
    final String mpId = snap.getMediaPackage().getIdentifier().toString();
    final String orgId = snap.getOrganizationId();
    final Version version = snap.getVersion();
    final String prettyMpId = mpId + "@v" + version;
    logger.debug("Moving assets for snapshot {} to store {}", prettyMpId, store.getStoreType());
    for (final MediaPackageElement e : snap.getMediaPackage().getElements()) {
      if (!MOVABLE_TYPES.contains(e.getElementType())) {
        logger.debug("Skipping {} because type is {}", e.getIdentifier(), e.getElementType());
        continue;
      }
      logger.debug("Moving {} to store {}", e.getIdentifier(), store.getStoreType());
      final StoragePath storagePath = StoragePath.mk(orgId, mpId, version, e.getIdentifier());
      if (store.contains(storagePath)) {
        logger.debug("Element {} (version {}) is already in store {} so skipping it", e.getIdentifier(),
                version.toString(),
                store.getStoreType());
        continue;
      }
      final Opt<StoragePath> existingAssetOpt
              = findAssetInVersionsAndStores(e.getChecksum().toString(), store.getStoreType());
      if (existingAssetOpt.isSome()) {
        final StoragePath existingAsset = existingAssetOpt.get();
        logger.debug("Content of asset {} with checksum {} already exists in {}",
                existingAsset.getMediaPackageElementId(), e.getChecksum(), store.getStoreType());
        if (!store.copy(existingAsset, storagePath)) {
          throw new AssetManagerException(format(
                  "An asset with checksum %s has already been archived but trying to copy or link asset %s to it "
                          + "failed",
                  e.getChecksum(),
                  existingAsset
          ));
        }
      } else {
        final Opt<Long> size = e.getSize() > 0 ? Opt.some(e.getSize()) : Opt.<Long>none();
        store.put(storagePath, Source.mk(e.getURI(), size, Opt.nul(e.getMimeType())));
      }
      getDb().setAssetStorageLocation(VersionImpl.mk(version), mpId, e.getIdentifier(), store.getStoreType());
    }
  }

  /** Deletes the content of a snapshot from a store */
  private void deleteAssetsFromStore(Snapshot snap, AssetStore store) {
    store.delete(DeletionSelector.delete(
            snap.getOrganizationId(),
            snap.getMediaPackage().getIdentifier().toString(),
            snap.getVersion()
    ));
  }

  /** Check if element <code>e</code> is already part of the history and in a specific store */
  private Opt<StoragePath> findAssetInVersionsAndStores(final String checksum, final String storeId) throws Exception {
    return getDb().findAssetByChecksumAndStore(checksum, storeId).map(new Fn<AssetDtos.Full, StoragePath>() {
      @Override public StoragePath apply(AssetDtos.Full dto) {
        return StoragePath.mk(
                dto.getOrganizationId(),
                dto.getMediaPackageId(),
                dto.getVersion(),
                dto.getAssetDto().getMediaPackageElementId()
        );
      }
    });
  }

  private void copyManifest(Snapshot snap, AssetStore targetStore) throws IOException, NotFoundException {
    final String mpId = snap.getMediaPackage().getIdentifier().toString();
    final String orgId = snap.getOrganizationId();
    final Version version = snap.getVersion();

    AssetStore currentStore = getAssetStore(snap.getStorageId()).get();
    Opt<String> manifestOpt = findManifestBaseName(snap, MANIFEST_DEFAULT_NAME, currentStore);
    if (manifestOpt.isNone()) {
      return; // Nothing to do, already moved to long-term storage
    }

    // Copy the manifest file
    String manifestBaseName = manifestOpt.get();
    StoragePath pathToManifest = new StoragePath(orgId, mpId, version, manifestBaseName);

    // Already copied?
    if (!targetStore.contains(pathToManifest)) {
      Opt<InputStream> inputStreamOpt;
      InputStream inputStream = null;
      String manifestFileName = null;
      try {
        inputStreamOpt = currentStore.get(pathToManifest);
        if (inputStreamOpt.isNone()) { // This should never happen because it has been tested before
          throw new NotFoundException(
                  String.format("Unexpected error. Manifest %s not found in current asset store", manifestBaseName));
        }

        inputStream = inputStreamOpt.get();
        manifestFileName = UUID.randomUUID().toString() + ".xml";
        URI manifestTmpUri = getWorkspace().putInCollection("archive", manifestFileName, inputStream);
        targetStore.put(pathToManifest, Source.mk(manifestTmpUri, Opt.<Long> none(), Opt.some(MimeTypes.XML)));
      } finally {
        IOUtils.closeQuietly(inputStream);
        try {
          // Make sure to clean up the temporary file
          getWorkspace().deleteFromCollection("archive", manifestFileName);
        } catch (NotFoundException e) {
          // This is OK, we are deleting it anyway
        } catch (IOException e) {
          // This usually happens when the collection directory cannot be deleted
          // because another process is running at the same time and wrote a file there
          // after it was tested but before it was actually deleted. We will consider this ok.
          // Does the error message mention the manifest file name?
          if (e.getMessage().indexOf(manifestFileName) > -1) {
            logger.warn("The manifest file {} didn't get deleted from the archive collection: {}",
                    manifestBaseName, e);
          }
          // Else the error is related to the file-archive collection, which is fine
        }
      }
    }
  }

  Opt<String> findManifestBaseName(Snapshot snap, String manifestName, AssetStore store) {
    StoragePath path = new StoragePath(snap.getOrganizationId(), snap.getMediaPackage().getIdentifier().toString(),
            snap.getVersion(), manifestName);
    // If manifest_.xml, etc not found, return previous name (copied from the EpsiodeServiceImpl logic)
    if (!store.contains(path)) {
      // If first call, manifest is not found, which probably means it has already been moved
      if (MANIFEST_DEFAULT_NAME.equals(manifestName)) {
        return Opt.none(); // No manifest found in current store
      } else {
        return Opt.some(manifestName.substring(0, manifestName.length() - 1));
      }
    }
    // This is the same logic as when building the manifest name: manifest, manifest_, manifest__, etc
    return findManifestBaseName(snap, manifestName + "_", store);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Make sure each of the elements has a checksum.
   */
  void calcChecksumsForMediaPackageElements(PartialMediaPackage pmp) {
    pmp.getElements().filter(hasNoChecksum.toFn()).each(addChecksum).run();
  }

  /** Mutates mp and its elements, so make sure to work on a copy. */
  private SnapshotDto addInternal(String owner, final MediaPackage mp) throws Exception {
    final Date now = new Date();
    // claim a new version for the media package
    final String mpId = mp.getIdentifier().toString();
    final VersionImpl version = getDb().claimVersion(mpId);
    logger.info("Creating new version {} of media package {}", version, mp);
    final PartialMediaPackage pmp = assetsOnly(mp);
    // make sure they have a checksum
    calcChecksumsForMediaPackageElements(pmp);
    // download and archive elements
    storeAssets(pmp, version);
    // store mediapackage in db
    final SnapshotDto snapshotDto;
    try {
      rewriteUrisForArchival(pmp, version);
      snapshotDto = getDb().saveSnapshot(
              getCurrentOrgId(), pmp, now, version,
              Availability.ONLINE, getLocalAssetStore().getStoreType(), owner
      );
    } catch (AssetManagerException e) {
      logger.error("Could not take snapshot {}: {}", mpId, e);
      throw new AssetManagerException(e);
    }
    // save manifest to element store
    // this is done at the end after the media package element ids have been rewritten to neutral URNs
    storeManifest(pmp, version);
    return snapshotDto;
  }

  private final Fx<MediaPackageElement> addChecksum = new Fx<MediaPackageElement>() {
    @Override public void apply(MediaPackageElement mpe) {
      File file = null;
      try {
        logger.trace("Calculate checksum for {}", mpe.getURI());
        file = getWorkspace().get(mpe.getURI(), true);
        mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
      } catch (IOException | NotFoundException e) {
        throw new AssetManagerException(format(
                "Cannot calculate checksum for media package element %s",
                mpe.getURI()
        ), e);
      } finally {
        if (file != null) {
          FileUtils.deleteQuietly(file);
        }
      }
    }
  };

  /**
   * Store all elements of <code>pmp</code> under the given version.
   */
  private void storeAssets(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getCurrentOrgId();
    for (final MediaPackageElement e : pmp.getElements()) {
      logger.debug("Archiving {} {} {}", e.getFlavor(), e.getMimeType(), e.getURI());
      final StoragePath storagePath = StoragePath.mk(orgId, mpId, version, e.getIdentifier());
      final Opt<StoragePath> existingAssetOpt = findAssetInVersions(e.getChecksum().toString());
      if (existingAssetOpt.isSome()) {
        final StoragePath existingAsset = existingAssetOpt.get();
        logger.debug("Content of asset {} with checksum {} has been archived before",
                existingAsset.getMediaPackageElementId(), e.getChecksum());
        if (!getLocalAssetStore().copy(existingAsset, storagePath)) {
          throw new AssetManagerException(format(
                  "An asset with checksum %s has already been archived but trying to copy or link asset %s to it "
                          + "failed",
                  e.getChecksum(),
                  existingAsset
          ));
        }
      } else {
        final Opt<Long> size = e.getSize() > 0 ? Opt.some(e.getSize()) : Opt.<Long>none();
        getLocalAssetStore().put(storagePath, Source.mk(e.getURI(), size, Opt.nul(e.getMimeType())));
      }
    }
  }

  /** Check if element <code>e</code> is already part of the history. */
  private Opt<StoragePath> findAssetInVersions(final String checksum) throws Exception {
    return getDb().findAssetByChecksumAndStore(checksum, getLocalAssetStore().getStoreType())
            .map(new Fn<AssetDtos.Full, StoragePath>() {
              @Override public StoragePath apply(AssetDtos.Full dto) {
                return StoragePath.mk(
                        dto.getOrganizationId(),
                        dto.getMediaPackageId(),
                        dto.getVersion(),
                        dto.getAssetDto().getMediaPackageElementId()
                );
              }
            });
  }

  private void storeManifest(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getCurrentOrgId();
    // store the manifest.xml
    // TODO make use of checksums
    logger.debug("Archiving manifest of media package {} version {}", mpId, version);
    // temporarily save the manifest XML into the workspace to
    // Fix file not found exception when several snapshots are taken at the same time
    final String manifestFileName = format("manifest_%s_%s.xml", pmp.getMediaPackage().getIdentifier(), version);
    final URI manifestTmpUri = getWorkspace().putInCollection(
            "archive",
            manifestFileName,
            IOUtils.toInputStream(MediaPackageParser.getAsXml(pmp.getMediaPackage()), "UTF-8"));
    try {
      getLocalAssetStore().put(
              StoragePath.mk(orgId, mpId, version, manifestAssetId(pmp, "manifest")),
              Source.mk(manifestTmpUri, Opt.<Long>none(), Opt.some(MimeTypes.XML)));
    } finally {
      // make sure to clean up the temporary file
      getWorkspace().deleteFromCollection("archive", manifestFileName);
    }
  }

  /**
   * Create a unique id for the manifest xml. This is to avoid an id collision
   * in the rare case that the media package contains an XML element with the id
   * used for the manifest. A UUID could also be used but this is far less
   * readable.
   *
   * @param seedId
   *          the id to start with
   */
  private String manifestAssetId(PartialMediaPackage pmp, String seedId) {
    if ($(pmp.getElements()).map(getMediaPackageElementId.toFn()).exists(Booleans.eq(seedId))) {
      return manifestAssetId(pmp, seedId + "_");
    } else {
      return seedId;
    }
  }

  /* --------------------------------------------------------------------------------------------------------------- */

  /**
   * Unify exception handling by wrapping any occurring exception in an
   * {@link AssetManagerException}.
   */
  static <A> A handleException(final P1<A> p) throws AssetManagerException {
    try {
      return p.get1();
    } catch (Exception e) {
      logger.error("An error occurred", e);
      throw unwrapExceptionUntil(AssetManagerException.class, e).getOr(new AssetManagerException(e));
    }
  }

  /**
   * Walk up the stacktrace to find a cause of type <code>type</code>. Return none if no such
   * type can be found.
   */
  static <A extends Throwable> Opt<A> unwrapExceptionUntil(Class<A> type, Throwable e) {
    if (e == null) {
      return Opt.none();
    } else if (type.isAssignableFrom(e.getClass())) {
      return Opt.some((A) e);
    } else {
      return unwrapExceptionUntil(type, e.getCause());
    }
  }

  /**
   * Return a partial media package filtering assets. Assets are elements the archive is going to manager, i.e. all
   * non-publication elements.
   */
  static PartialMediaPackage assetsOnly(MediaPackage mp) {
    return PartialMediaPackage.mk(mp, isAsset);
  }

  static final Pred<MediaPackageElement> isAsset = Pred.mk(isNotPublication.toFn());

  /**
   * Create a URN for a media package element of a certain version.
   * Use this URN for the archived media package.
   */
  static Fn<MediaPackageElement, URI> createUrn(final String mpId, final Version version) {
    return new Fn<MediaPackageElement, URI>() {
      @Override
      public URI apply(MediaPackageElement mpe) {
        try {
          String fileName = getFileName(mpe).getOr("unknown");
          return new URI(
                  "urn",
                  "matterhorn:" + mpId + ":" + version + ":" + mpe.getIdentifier() + ":" + fileName,
                  null
          );
        } catch (URISyntaxException e) {
          throw new AssetManagerException(e);
        }
      }
    };
  }

  /**
   * Extract the file name from a media package elements URN.
   *
   * @return the file name or none if it could not be determined
   */
  public static Opt<String> getFileNameFromUrn(MediaPackageElement mpe) {
    Opt<URI> uri = Opt.nul(mpe.getURI());
    if (uri.isSome() && "urn".equals(uri.get().getScheme())) {
      return uri.toStream().map(toString).bind(Strings.split(":")).drop(1).reverse().head();
    }
    return Opt.none();
  }

  private static final Fn<URI, String> toString = new Fn<URI, String>() {
    @Override
    public String apply(URI uri) {
      return uri.toString();
    }
  };

  /**
   * Rewrite URIs of assets of media package elements. Please note that this method modifies the given media package.
   */
  static void rewriteUrisForArchival(PartialMediaPackage pmp, Version version) {
    rewriteUris(pmp, createUrn(pmp.getMediaPackage().getIdentifier().toString(), version));
  }

  /**
   * Rewrite URIs of all asset elements of a media package.
   * Please note that this method modifies the given media package.
   */
  static void rewriteUris(PartialMediaPackage pmp, Fn<MediaPackageElement, URI> uriCreator) {
    for (MediaPackageElement mpe : pmp.getElements()) {
      mpe.setURI(uriCreator.apply(mpe));
    }
  }

  /**
   * Rewrite URIs of all asset elements of a snapshot's media package.
   * This method does not mutate anything.
   */
  public static Snapshot rewriteUris(Snapshot snapshot, Fn<MediaPackageElement, URI> uriCreator) {
    final MediaPackage mpCopy = MediaPackageSupport.copy(snapshot.getMediaPackage());
    for (final MediaPackageElement mpe : assetsOnly(mpCopy).getElements()) {
      mpe.setURI(uriCreator.apply(mpe));
    }
    return new SnapshotImpl(
            snapshot.getVersion(),
            snapshot.getOrganizationId(),
            snapshot.getArchivalDate(),
            snapshot.getAvailability(),
            snapshot.getStorageId(),
            snapshot.getOwner(),
            mpCopy);
  }
}
