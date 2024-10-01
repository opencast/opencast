/*
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
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;
import org.opencastproject.assetmanager.impl.query.AQueryBuilderImpl;
import org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.util.AccessInformationUtil;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventIndexUtils;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
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
import com.entwinemedia.fn.fns.Booleans;
import com.google.common.collect.Sets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

/**
 * The Asset Manager implementation.
 */
@Component(
    property = {
        "service.description=Opencast Asset Manager"
    },
    immediate = true,
    service = { AssetManager.class, IndexProducer.class }
)
public class AssetManagerImpl extends AbstractIndexProducer implements AssetManager,
    AbstractADeleteQuery.DeleteEpisodeHandler {

  private static final Logger logger = LoggerFactory.getLogger(AssetManagerImpl.class);

  private static final int PAGE_SIZE = 1000;

  enum AdminRole {
    GLOBAL, ORGANIZATION, NONE
  }

  public static final String WRITE_ACTION = "write";
  public static final String READ_ACTION = "read";
  public static final String SECURITY_NAMESPACE = "org.opencastproject.assetmanager.security";

  private static final String MANIFEST_DEFAULT_NAME = "manifest";

  private CopyOnWriteArrayList<AssetManagerUpdateHandler> handlers = new CopyOnWriteArrayList<>();

  private SecurityService securityService;
  private AuthorizationService authorizationService;
  private OrganizationDirectoryService orgDir;
  private Workspace workspace;
  private AssetStore assetStore;
  private HttpAssetProvider httpAssetProvider;
  private String systemUserName;
  private Database db;
  private DBSessionFactory dbSessionFactory;
  private EntityManagerFactory emf;
  private AclServiceFactory aclServiceFactory;
  private ElasticsearchIndex index;
  private Map<String, List<EventCatalogUIAdapter>> extendedEventCatalogUIAdapters = new HashMap<>();

  // Settings for role filter
  private boolean includeAPIRoles;
  private boolean includeCARoles;
  private boolean includeUIRoles;

  public static final Set<MediaPackageElement.Type> MOVABLE_TYPES = Sets.newHashSet(
          MediaPackageElement.Type.Attachment,
          MediaPackageElement.Type.Catalog,
          MediaPackageElement.Type.Track
  );

  private final HashMap<String, RemoteAssetStore> remoteStores = new LinkedHashMap<>();

  /**
   * OSGi callback.
   */
  @Activate
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activating AssetManager.");
    db = new Database(dbSessionFactory.createSession(emf));
    systemUserName = SecurityUtil.getSystemUserName(cc);

    includeAPIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeAPIRoles"), null));
    includeCARoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeCARoles"), null));
    includeUIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeUIRoles"), null));
  }

  /**
   * OSGi dependencies
   */

  @Reference(target = "(osgi.unit.name=org.opencastproject.assetmanager.impl)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Reference
  public void setOrgDir(OrganizationDirectoryService orgDir) {
    this.orgDir = orgDir;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setAssetStore(AssetStore assetStore) {
    this.assetStore = assetStore;
  }

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeEventHandler"
  )
  public void addEventHandler(AssetManagerUpdateHandler handler) {
    this.handlers.add(handler);
  }

  public void removeEventHandler(AssetManagerUpdateHandler handler) {
    this.handlers.remove(handler);
  }

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeRemoteAssetStore"
  )
  public synchronized void addRemoteAssetStore(RemoteAssetStore assetStore) {
    remoteStores.put(assetStore.getStoreType(), assetStore);
  }

  public void removeRemoteAssetStore(RemoteAssetStore store) {
    remoteStores.remove(store.getStoreType());
  }

  @Reference
  public void setHttpAssetProvider(HttpAssetProvider httpAssetProvider) {
    this.httpAssetProvider = httpAssetProvider;
  }

  @Reference
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  @Reference
  public void setIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
          target = "(common-metadata=false)")
  public synchronized void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    List<EventCatalogUIAdapter> list = extendedEventCatalogUIAdapters.computeIfAbsent(
            catalogUIAdapter.getOrganization(), k -> new ArrayList());
    list.add(catalogUIAdapter);
  }

  public synchronized void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    if (extendedEventCatalogUIAdapters.containsKey(catalogUIAdapter.getOrganization())) {
      extendedEventCatalogUIAdapters.get(catalogUIAdapter.getOrganization()).remove(catalogUIAdapter);
    }
  }

  /**
   * AssetManager implementation
   */

  @Override
  public Optional<MediaPackage> getMediaPackage(String mediaPackageId) {
    final AQueryBuilder q = createQuery();
    final AResult r = q.select(q.snapshot()).where(q.mediaPackageId(mediaPackageId).and(q.version().isLatest()))
            .run();

    if (r.getSize() == 0) {
      return Optional.empty();
    }
    return Optional.of(r.getRecords().stream().findFirst().get().getSnapshot().get().getMediaPackage());
  }

  @Override
  public Optional<Asset> getAsset(Version version, String mpId, String mpElementId) {
    if (isAuthorized(mpId, READ_ACTION)) {
      // try to fetch the asset
      var asset = getDatabase().getAsset(RuntimeTypes.convert(version), mpId, mpElementId);
      if (asset.isPresent()) {
        var storageId = getSnapshotStorageLocation(version, mpId);
        if (storageId.isPresent()) {
          var store = getAssetStore(storageId.get());
          if (store.isPresent()) {
            var assetStream = store.get().get(StoragePath.mk(
                asset.get().getOrganizationId(),
                mpId,
                version,
                mpElementId
            ));
            if (assetStream.isPresent()) {

              Checksum checksum = null;
              try {
                checksum = Checksum.fromString(asset.get().getAssetDto().getChecksum());
              } catch (NoSuchAlgorithmException e) {
                logger.warn("Invalid checksum for asset {} of media package {}", mpElementId, mpId, e);
              }

              final Asset a = new AssetImpl(
                      AssetId.mk(version, mpId, mpElementId),
                      assetStream.get(),
                      asset.get().getAssetDto().getMimeType(),
                      asset.get().getAssetDto().getSize(),
                      asset.get().getStorageId(),
                      asset.get().getAvailability(),
                      checksum);
              return Optional.of(a);
            }
          }
        }
      }
      return Optional.empty();
    }
    return chuck(new UnauthorizedException(
            format("Not allowed to read assets of snapshot %s, version=%s", mpId, version)
    ));
  }

  @Override
  public Optional<AssetStore> getAssetStore(String storeId) {
    if (assetStore.getStoreType().equals(storeId)) {
      return Optional.of(assetStore);
    } else {
      if (remoteStores.containsKey(storeId)) {
        return Optional.of(remoteStores.get(storeId));
      } else {
        return Optional.empty();
      }
    }
  }

  @Override
  public AssetStore getLocalAssetStore() {
    return assetStore;
  }

  @Override
  public List<AssetStore> getRemoteAssetStores() {
    return new ArrayList<>(remoteStores.values());
  }

  /** Snapshots */

  @Override
  public boolean snapshotExists(final String mediaPackageId) {
    return getDatabase().snapshotExists(mediaPackageId);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId, final String organization) {
    return getDatabase().snapshotExists(mediaPackageId, organization);
  }

  @Override
  public Snapshot takeSnapshot(MediaPackage mp) {
    return takeSnapshot(null, mp);
  }

  @Override
  public Snapshot takeSnapshot(String owner, MediaPackage mp) {

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

      final AccessControlList acl = authorizationService.getActiveAcl(mp).getA();
      // store acl as properties
      // Drop old ACL rules
      deleteProperties(mediaPackageId, SECURITY_NAMESPACE);
      // Set new ACL rules
      for (final AccessControlEntry ace : acl.getEntries()) {
        getDatabase().saveProperty(Property.mk(PropertyId.mk(mediaPackageId, SECURITY_NAMESPACE,
                mkPropertyName(ace.getRole(), ace.getAction())), Value.mk(ace.isAllow())));
      }

      updateEventInIndex(snapshot);

      logger.info("Trigger update handlers for snapshot {}, version {}",
          snapshot.getMediaPackage().getIdentifier(), snapshot.getVersion());
      fireEventHandlers(mkTakeSnapshotMessage(snapshot));

      return snapshot;
    }
    return chuck(new UnauthorizedException("Not allowed to take snapshot of media package " + mediaPackageId));
  }

  private Snapshot takeSnapshotInternal(MediaPackage mediaPackage) {
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    AQueryBuilder queryBuilder = createQuery();
    AResult result = queryBuilder.select(queryBuilder.snapshot())
            .where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest())).run();
    Optional<ARecord> record = result.getRecords().stream().findFirst();
    if (record.isPresent()) {
      Optional<Snapshot> snapshot = Optional.of(record.get().getSnapshot().get());
      if (snapshot.isPresent()) {
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

  /**
   * Create a {@link AssetManagerItem.TakeSnapshot} message.
   * <p>
   * Do not call outside of a security context.
   */
  private AssetManagerItem.TakeSnapshot mkTakeSnapshotMessage(Snapshot snapshot) {
    final MediaPackage mp = snapshot.getMediaPackage();

    long version;
    try {
      version = Long.parseLong(snapshot.getVersion().toString());
    } catch (NumberFormatException e) {
      // The index requires a version to be a long value.
      // Since the asset manager default implementation uses long values that should be not a problem.
      // However, a decent exception message is helpful if a different implementation of the asset manager
      // is used.
      throw new RuntimeException("The current implementation of the index requires versions being of type 'long'.");
    }

    return AssetManagerItem.add(workspace, mp, authorizationService.getActiveAcl(mp).getA(),
            version, snapshot.getArchivalDate());
  }

  /**
   * Update the event in the Elasticsearch index.
   *
   * @param snapshot
   *         The newest snapshot of the event to update
   */
  private void updateEventInIndex(Snapshot snapshot) {
    final MediaPackage mp = snapshot.getMediaPackage();
    String eventId = mp.getIdentifier().toString();
    final String organization = securityService.getOrganization().getId();
    final User user = securityService.getUser();
    logger.debug("Updating event {} in the {} index.", eventId, index.getIndexName());

    Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
      Event event = eventOpt.orElse(new Event(eventId, organization));

      AccessControlList acl = authorizationService.getActiveAcl(mp).getA();
      List<ManagedAcl> acls = aclServiceFactory.serviceFor(securityService.getOrganization()).getAcls();
      for (final ManagedAcl managedAcl : AccessInformationUtil.matchAcls(acls, acl)) {
        event.setManagedAcl(managedAcl.getName());
      }
      event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
      event.setArchiveVersion(Long.parseLong(snapshot.getVersion().toString()));
      if (StringUtils.isBlank(event.getCreator())) {
        event.setCreator(securityService.getUser().getName());
      }
      EventIndexUtils.updateEvent(event, mp);

      // common metadata
      for (Catalog catalog: mp.getCatalogs(MediaPackageElements.EPISODE)) {
        try (InputStream in = workspace.read(catalog.getURI())) {
          EventIndexUtils.updateEvent(event, DublinCores.read(in));
        } catch (IOException | NotFoundException e) {
          throw new IllegalStateException(String.format("Unable to load common dublin core catalog for event '%s'",
                  mp.getIdentifier()), e);
        }
      }

      // extended metadata
      event.resetExtendedMetadata();  // getting rid of old data
      for (EventCatalogUIAdapter extendedCatalogUIAdapter : extendedEventCatalogUIAdapters.getOrDefault(organization,
              Collections.emptyList())) {
        for (Catalog catalog: mp.getCatalogs(extendedCatalogUIAdapter.getFlavor())) {
          try (InputStream in = workspace.read(catalog.getURI())) {
            EventIndexUtils.updateEventExtendedMetadata(event, DublinCores.read(in),
                    extendedCatalogUIAdapter.getFlavor());
          } catch (IOException | NotFoundException e) {
            throw new IllegalStateException(String.format("Unable to load extended dublin core catalog '%s' for event "
                            + "'%s'", catalog.getFlavor(), mp.getIdentifier()), e);
          }
        }
      }

      // Update series name if not already done
      try {
        EventIndexUtils.updateSeriesName(event, organization, user, index);
      } catch (SearchIndexException e) {
        logger.error("Error updating the series name of the event {} in the {} index.", eventId, index.getIndexName(),
                e);
      }
      return Optional.of(event);
    };

    // Persist the scheduling event
    try {
      index.addOrUpdateEvent(eventId, updateFunction, organization, user);
      logger.debug("Event {} updated in the {} index.", eventId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error updating the event {} in the {} index.", eventId, e);
    }
  }

  /**
   * Remove the event from the Elasticsearch index
   *
   * @param eventId
   *         The id of the event to remove
   */
  private void removeArchivedVersionFromIndex(String eventId) {
    final String orgId = securityService.getOrganization().getId();
    final User user = securityService.getUser();
    logger.debug("Received AssetManager delete episode message {}", eventId);

    Function<Optional<Event>, Optional<Event>> updateFunction = (Optional<Event> eventOpt) -> {
      if (eventOpt.isEmpty()) {
        logger.warn("Event {} not found for deletion", eventId);
        return Optional.empty();
      }
      Event event = eventOpt.get();
      event.setArchiveVersion(null);
      return Optional.of(event);
    };

    try {
      index.addOrUpdateEvent(eventId, updateFunction, orgId, user);
      logger.debug("Event {} removed from the {} index", eventId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Error deleting the event {} from the {} index.", eventId, e);
    }
  }

  @Override
  public RichAResult getSnapshotsById(final String mpId) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId);
    return Enrichments.enrich(query.run());
  }

  @Override
  public RichAResult getSnapshotsByIdOrderedByVersion(String mpId, boolean asc) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId);
    if (asc) {
      query = query.orderBy(q.version().asc());
    } else {
      query = query.orderBy(q.version().desc());
    }
    return Enrichments.enrich(query.run());
  }

  @Override
  public RichAResult getSnapshotsByIdAndVersion(final String mpId, final Version version) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    RequireUtil.notNull(version, "version");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, version, mpId);
    return Enrichments.enrich(query.run());
  }

  @Override
  public RichAResult getSnapshotsByDate(final Date start, final Date end) {
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q).where(q.archived().ge(start)).where(q.archived().le(end));
    return Enrichments.enrich(query.run());
  }

  @Override
  public RichAResult getSnapshotsByDateOrderedById(Date start, Date end) {
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q).where(q.archived().ge(start)).where(q.archived().le(end));
    return Enrichments.enrich(query.orderBy(q.mediapackageId().asc()).run());
  }

  @Override
  public RichAResult getSnapshotsByIdAndDate(final String mpId, final Date start, final Date end) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId).where(q.archived().ge(start)).where(q.archived().le(end));
    return Enrichments.enrich(query.run());
  }

  @Override
  public RichAResult getSnapshotsByIdAndDateOrderedByVersion(String mpId, Date start, Date end, boolean asc) {
    RequireUtil.requireNotBlank(mpId, "mpId");
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q, mpId).where(q.archived().ge(start)).where(q.archived().le(end));
    if (asc) {
      query = query.orderBy(q.version().asc());
    } else {
      query = query.orderBy(q.version().desc());
    }
    return Enrichments.enrich(query.run());
  }

  @Override
  public void moveSnapshotsById(final String mpId, final String targetStore) throws NotFoundException {
    RichAResult results = getSnapshotsById(mpId);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("Mediapackage " + mpId + " not found!");
    }

    processOperations(results, targetStore);
  }

  @Override
  public void moveSnapshotsByIdAndVersion(final String mpId, final Version version, final String targetStore)
          throws NotFoundException {
    RichAResult results = getSnapshotsByIdAndVersion(mpId, version);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("Mediapackage " + mpId + "@" + version.toString() + " not found!");
    }

    processOperations(results, targetStore);
  }

  @Override
  public void moveSnapshotsByDate(final Date start, final Date end, final String targetStore)
          throws NotFoundException {
    // We don't use #getSnapshotsByDate() as this includes also all snapshots already in targetStore. On large installs
    // this could lead to memory overflow.
    AQueryBuilder q = createQuery();
    ASelectQuery query = baseQuery(q)
        .where(q.storage(targetStore).not())
        .where(q.archived().ge(start))
        .where(q.archived().le(end));
    RichAResult results = Enrichments.enrich(query.run());

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("No media packages found between " + start + " and " + end);
    }

    processOperations(results, targetStore);
  }

  @Override
  public void moveSnapshotsByIdAndDate(final String mpId, final Date start, final Date end, final String targetStore)
          throws NotFoundException {
    RichAResult results = getSnapshotsByIdAndDate(mpId, start, end);

    if (results.getRecords().isEmpty()) {
      throw new NotFoundException("No media package with id " + mpId + " found between " + start + " and " + end);
    }

    processOperations(results, targetStore);
  }

  @Override
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
    results.getRecords().forEach(record -> {
      Snapshot s = record.getSnapshot().get();
      Optional<String> currentStoreId = getSnapshotStorageLocation(s);

      if (currentStoreId.isEmpty()) {
        logger.warn("IsNone store ID");
        return;
      }

      //If this snapshot is already stored in the desired store
      if (currentStoreId.get().equals(targetStoreId)) {
        //return, since we don't need to move anything
        return;
      }

      AssetStore currentStore;
      AssetStore targetStore;

      Optional<AssetStore> optCurrentStore = getAssetStore(currentStoreId.get());
      Optional<AssetStore> optTargetStore = getAssetStore(targetStoreId);

      if (!optCurrentStore.isEmpty()) {
        currentStore = optCurrentStore.get();
      } else {
        logger.error("Unknown current store: " + currentStoreId.get());
        return;
      }
      if (!optTargetStore.isEmpty()) {
        targetStore = optTargetStore.get();
      } else {
        logger.error("Unknown target store: " + targetStoreId);
        return;
      }

      //If the content is already local, or is moving from a remote to the local
      // Returns true if the store id is equal to the local asset store's id
      String localAssetStoreType = getLocalAssetStore().getStoreType();
      if (localAssetStoreType.equals(currentStoreId.get()) || localAssetStoreType.equals(targetStoreId)) {
        logger.debug("Moving {} from {} to {}", s, currentStoreId, targetStoreId);

        try {
          copyAssetsToStore(s, targetStore);
          copyManifest(s, targetStore);
        } catch (Exception e) {
          Functions.chuck(e);
        }
        getDatabase().setStorageLocation(s, targetStoreId);
        currentStore.delete(DeletionSelector.delete(s.getOrganizationId(),
                s.getMediaPackage().getIdentifier().toString(), s.getVersion()
        ));
      } else {
        //Else, the content is *not* local and is going to a *different* remote
        String intermediateStore = getLocalAssetStore().getStoreType();
        logger.debug("Moving {} from {} to {}, then to {}",
                s, currentStoreId, intermediateStore, targetStoreId);
        Version version = s.getVersion();
        String mpId = s.getMediaPackage().getIdentifier().toString();
        try {
          moveSnapshotToStore(version, mpId, intermediateStore);
          moveSnapshotToStore(version, mpId, targetStoreId);
        } catch (NotFoundException e) {
          Functions.chuck(e);
        }
      }
    });
  }

  // Return the asset store ID that is currently storing the snapshot
  public Optional<String> getSnapshotStorageLocation(final Version version, final String mpId) {
    RichAResult result = getSnapshotsByIdAndVersion(mpId, version);

    for (Snapshot snapshot : result.getSnapshots()) {
      return Optional.of(snapshot.getStorageId());
    }

    logger.error("Mediapackage " + mpId + "@" + version + " not found!");
    return Optional.empty();
  }

  public Optional<String> getSnapshotStorageLocation(final Snapshot snap) {
    return getSnapshotStorageLocation(snap.getVersion(), snap.getMediaPackage().getIdentifier().toString());
  }

  /** Properties */

  @Override
  public boolean setProperty(Property property) {
    final String mpId = property.getId().getMediaPackageId();
    if (isAuthorized(mpId, WRITE_ACTION)) {
      return getDatabase().saveProperty(property);
    }
    return chuck(new UnauthorizedException("Not allowed to set property on episode " + mpId));
  }

  @Override
  public List<Property> selectProperties(final String mediaPackageId, String namespace) {
    if (isAuthorized(mediaPackageId, READ_ACTION)) {
      return getDatabase().selectProperties(mediaPackageId, namespace);
    }
    return chuck(new UnauthorizedException(format("Not allowed to read properties of event %s", mediaPackageId)));
  }

  @Override
  public int deleteProperties(final String mediaPackageId) {
    return getDatabase().deleteProperties(mediaPackageId);
  }

  @Override
  public int deleteProperties(final String mediaPackageId, final String namespace) {
    return getDatabase().deleteProperties(mediaPackageId, namespace);
  }

  /** Misc. */

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

  private AQueryBuilder createQueryWithoutSecurityCheck() {
    return new AQueryBuilderDecorator(new AQueryBuilderImpl(this)) {
      @Override
      public ADeleteQuery delete(String owner, Target target) {
        return new ADeleteQueryWithMessaging(super.delete(owner, target));
      }
    };
  }

  @Override
  public Optional<Version> toVersion(String version) {
    try {
      return Optional.of(VersionImpl.mk(Long.parseLong(version)));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  @Override
  public long countEvents(final String organization) {
    return getDatabase().countEvents(organization);
  }

  @Override
  public void handleDeletedEpisode(String mpId) {
    logger.info("Firing event handlers for deleting event {}", mpId);
    fireEventHandlers(AssetManagerItem.deleteEpisode(mpId, new Date()));

    removeArchivedVersionFromIndex(mpId);
  }

  /**
   * AbstractIndexProducer Implementation
   */

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.AssetManager;
  }

  @Override
  public void repopulate() throws IndexRebuildException {
    final Organization originalOrg = securityService.getOrganization();
    final User originalUser = (originalOrg != null ? securityService.getUser() : null);
    try {
      final Organization defaultOrg = new DefaultOrganization();
      final User defaultSystemUser = SecurityUtil.createSystemUser(systemUserName, defaultOrg);
      securityService.setOrganization(defaultOrg);
      securityService.setUser(defaultSystemUser);

      int offset = 0;
      int total = (int) countEvents(null);
      final AQueryBuilder q = createQuery();
      RichAResult r;
      int current = 0;
      logIndexRebuildBegin(logger, total, "snapshot(s)");
      var updatedEventRange = new ArrayList<Event>();
      do {
        r = enrich(q.select(q.snapshot()).where(q.version().isLatest()).orderBy(q.mediapackageId().desc())
          .page(offset, PAGE_SIZE).run());
        offset += PAGE_SIZE;
        int n = 20;

        final Map<String, List<Snapshot>> byOrg = r.getSnapshots().stream()
            .collect(Collectors.groupingBy(Snapshot::getOrganizationId));
        for (String orgId : byOrg.keySet()) {
          final Organization snapshotOrg;
          try {
            snapshotOrg = orgDir.getOrganization(orgId);
            User snapshotSystemUser = SecurityUtil.createSystemUser(systemUserName, snapshotOrg);
            securityService.setOrganization(snapshotOrg);
            securityService.setUser(snapshotSystemUser);
            for (Snapshot snapshot : byOrg.get(orgId)) {
              try {
                current++;

                var updatedEventData = index.getEvent(snapshot.getMediaPackage().getIdentifier().toString(), orgId,
                    snapshotSystemUser);
                updatedEventData = getEventUpdateFunction(snapshot, orgId, snapshotSystemUser).apply(updatedEventData);
                updatedEventRange.add(updatedEventData.get());

                if (updatedEventRange.size() >= n || current >= total) {
                  index.bulkEventUpdate(updatedEventRange);
                  logIndexRebuildProgress(logger, total, current, n);
                  updatedEventRange.clear();
                }
              } catch (Throwable t) {
                logSkippingElement(logger, "event", snapshot.getMediaPackage().getIdentifier().toString(),
                        snapshotOrg, t);
              }
            }
          } catch (Throwable t) {
            logIndexRebuildError(logger, t, originalOrg);
            throw new IndexRebuildException(getService(), originalOrg, t);
          } finally {
            securityService.setOrganization(defaultOrg);
            securityService.setUser(defaultSystemUser);
          }
        }
      } while (offset < total);
    } finally {
      securityService.setOrganization(originalOrg);
      securityService.setUser(originalUser);
    }
  }

  /**
   * Used for testing
   */
  public void setAvailability(Version version, String mpId, Availability availability) {
    if (isAuthorized(mpId, WRITE_ACTION)) {
      getDatabase().setAvailability(RuntimeTypes.convert(version), mpId, availability);
    } else {
      chuck(new UnauthorizedException("Not allowed to set availability of episode " + mpId));
    }
  }

  public void setDatabase(Database database) {
    this.db = database;
  }

  public Database getDatabase() {
    return db;
  }

  public HttpAssetProvider getHttpAssetProvider() {
    return httpAssetProvider;
  }

  /*
   * Security handling
   */

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
            .map((role) -> q.property(Value.BOOLEAN, SECURITY_NAMESPACE, mkPropertyName(role.getName(), action))
                    .eq(true))
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
        return getDatabase().selectProperties(mediaPackageId, SECURITY_NAMESPACE).parallelStream()
                .map(p -> p.getId().getName())
                .filter(p -> p.endsWith(action))
                .anyMatch(p -> roles.stream().anyMatch(r -> r.equals(p)));
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

  private String mkPropertyName(String role, String action) {
    return role + " | " + action;
  }

  /**
   * Configurable filter for roles
   */
  private final java.util.function.Predicate<Role> roleFilter = (role) -> {
    final String name = role.getName();
    return (includeAPIRoles || !name.startsWith("ROLE_API_"))
            && (includeCARoles  || !name.startsWith("ROLE_CAPTURE_AGENT_"))
            && (includeUIRoles  || !name.startsWith("ROLE_UI_"));
  };

  /*
   * Utility
   */

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

  /** Move the assets for a snapshot to the target store */
  private void copyAssetsToStore(Snapshot snap, AssetStore store) {
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
                version, store.getStoreType());
        continue;
      }

      // find asset in versions & stores
      final Optional<StoragePath> existingAssetOpt =
          getDatabase()
          .findAssetByChecksumAndStoreAndOrg(e.getChecksum().toString(), store.getStoreType(), orgId)
          .map(dto -> StoragePath.mk(
              dto.getOrganizationId(),
              dto.getMediaPackageId(),
              dto.getVersion(),
              dto.getAssetDto().getMediaPackageElementId()
          ));

      if (existingAssetOpt.isPresent()) {
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
        final Optional<Long> size = e.getSize() > 0 ? Optional.of(e.getSize()) : Optional.empty();
        store.put(storagePath, Source.mk(e.getURI(), size, Optional.ofNullable(e.getMimeType())));
      }
      getDatabase().setAssetStorageLocation(VersionImpl.mk(version), mpId, e.getIdentifier(), store.getStoreType());
    }
  }

  private void copyManifest(Snapshot snap, AssetStore targetStore) throws IOException, NotFoundException {
    final String mpId = snap.getMediaPackage().getIdentifier().toString();
    final String orgId = snap.getOrganizationId();
    final Version version = snap.getVersion();

    AssetStore currentStore = getAssetStore(snap.getStorageId()).get();
    Optional<String> manifestOpt = findManifestBaseName(snap, MANIFEST_DEFAULT_NAME, currentStore);
    if (manifestOpt.isEmpty()) {
      return; // Nothing to do, already moved to long-term storage
    }

    // Copy the manifest file
    String manifestBaseName = manifestOpt.get();
    StoragePath pathToManifest = new StoragePath(orgId, mpId, version, manifestBaseName);

    // Already copied?
    if (!targetStore.contains(pathToManifest)) {
      Optional<InputStream> inputStreamOpt;
      InputStream inputStream = null;
      String manifestFileName = null;
      try {
        inputStreamOpt = currentStore.get(pathToManifest);
        if (inputStreamOpt.isEmpty()) { // This should never happen because it has been tested before
          throw new NotFoundException(
                  String.format("Unexpected error. Manifest %s not found in current asset store", manifestBaseName));
        }

        inputStream = inputStreamOpt.get();
        manifestFileName = UUID.randomUUID() + ".xml";
        URI manifestTmpUri = workspace.putInCollection("archive", manifestFileName, inputStream);
        targetStore.put(pathToManifest, Source.mk(manifestTmpUri, Optional.empty(), Optional.of(MimeTypes.XML)));
      } finally {
        IOUtils.closeQuietly(inputStream);
        try {
          // Make sure to clean up the temporary file
          workspace.deleteFromCollection("archive", manifestFileName);
        } catch (NotFoundException e) {
          // This is OK, we are deleting it anyway
        } catch (IOException e) {
          // This usually happens when the collection directory cannot be deleted
          // because another process is running at the same time and wrote a file there
          // after it was tested but before it was actually deleted. We will consider this ok.
          // Does the error message mention the manifest file name?
          if (e.getMessage().contains(manifestFileName)) {
            logger.warn("The manifest file {} didn't get deleted from the archive collection: {}",
                    manifestBaseName, e);
          }
          // Else the error is related to the file-archive collection, which is fine
        }
      }
    }
  }

  Optional<String> findManifestBaseName(Snapshot snap, String manifestName, AssetStore store) {
    StoragePath path = new StoragePath(snap.getOrganizationId(), snap.getMediaPackage().getIdentifier().toString(),
            snap.getVersion(), manifestName);
    // If manifest_.xml, etc not found, return previous name (copied from the EpsiodeServiceImpl logic)
    if (!store.contains(path)) {
      // If first call, manifest is not found, which probably means it has already been moved
      if (MANIFEST_DEFAULT_NAME.equals(manifestName)) {
        return Optional.empty(); // No manifest found in current store
      } else {
        return Optional.of(manifestName.substring(0, manifestName.length() - 1));
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
    final Fx<MediaPackageElement> addChecksum = new Fx<MediaPackageElement>() {
      @Override public void apply(MediaPackageElement mpe) {
        File file = null;
        try {
          logger.trace("Calculate checksum for {}", mpe.getURI());
          file = workspace.get(mpe.getURI(), true);
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
    pmp.getElements().filter(hasNoChecksum.toFn()).each(addChecksum).run();
  }

  /** Mutates mp and its elements, so make sure to work on a copy. */
  private SnapshotDto addInternal(String owner, final MediaPackage mp) throws Exception {
    final Date now = new Date();
    // claim a new version for the media package
    final String mpId = mp.getIdentifier().toString();
    final VersionImpl version = getDatabase().claimVersion(mpId);
    logger.info("Creating new version {} of media package {}", version, mp);
    final PartialMediaPackage pmp = assetsOnly(mp);
    // make sure they have a checksum
    calcChecksumsForMediaPackageElements(pmp);
    // download and archive elements
    storeAssets(pmp, version);
    // store mediapackage in db
    final SnapshotDto snapshotDto;
    try {
      // rewrite URIs for archival
      Fn<MediaPackageElement, URI> uriCreator = new Fn<MediaPackageElement, URI>() {
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

      for (MediaPackageElement mpe : pmp.getElements()) {
        mpe.setURI(uriCreator.apply(mpe));
      }

      String currentOrgId = securityService.getOrganization().getId();
      snapshotDto = getDatabase().saveSnapshot(
              currentOrgId, pmp, now, version,
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

  /**
   * Store all elements of <code>pmp</code> under the given version.
   */
  private void storeAssets(final PartialMediaPackage pmp, final Version version) {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = securityService.getOrganization().getId();
    for (final MediaPackageElement e : pmp.getElements()) {
      logger.debug("Archiving {} {} {}", e.getFlavor(), e.getMimeType(), e.getURI());
      final StoragePath storagePath = StoragePath.mk(orgId, mpId, version, e.getIdentifier());
      // find asset in versions
      final Optional<StoragePath> existingAssetOpt = getDatabase()
          .findAssetByChecksumAndStoreAndOrg(e.getChecksum().toString(), getLocalAssetStore().getStoreType(), orgId)
          .map(dto -> StoragePath.mk(
                  dto.getOrganizationId(),
                  dto.getMediaPackageId(),
                  dto.getVersion(),
                  dto.getAssetDto().getMediaPackageElementId()));

      if (existingAssetOpt.isPresent()) {
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
        final Optional<Long> size = e.getSize() > 0 ? Optional.of(e.getSize()) : Optional.empty();
        getLocalAssetStore().put(storagePath, Source.mk(e.getURI(), size, Optional.ofNullable(e.getMimeType())));
      }
    }
  }

  private void storeManifest(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = securityService.getOrganization().getId();
    // store the manifest.xml
    // TODO make use of checksums
    logger.debug("Archiving manifest of media package {} version {}", mpId, version);
    // temporarily save the manifest XML into the workspace to
    // Fix file not found exception when several snapshots are taken at the same time
    final String manifestFileName = format("manifest_%s_%s.xml", pmp.getMediaPackage().getIdentifier(), version);
    final URI manifestTmpUri = workspace.putInCollection(
            "archive",
            manifestFileName,
            IOUtils.toInputStream(MediaPackageParser.getAsXml(pmp.getMediaPackage()), "UTF-8"));
    try {
      getLocalAssetStore().put(
              StoragePath.mk(orgId, mpId, version, manifestAssetId(pmp, "manifest")),
              Source.mk(manifestTmpUri, Optional.empty(), Optional.of(MimeTypes.XML)));
    } finally {
      // make sure to clean up the temporary file
      workspace.deleteFromCollection("archive", manifestFileName);
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
      throw unwrapExceptionUntil(AssetManagerException.class, e).orElse(new AssetManagerException(e));
    }
  }

  /**
   * Walk up the stacktrace to find a cause of type <code>type</code>. Return none if no such
   * type can be found.
   */
  static <A extends Throwable> Optional<A> unwrapExceptionUntil(Class<A> type, Throwable e) {
    if (e == null) {
      return Optional.empty();
    } else if (type.isAssignableFrom(e.getClass())) {
      return Optional.of((A) e);
    } else {
      return unwrapExceptionUntil(type, e.getCause());
    }
  }

  /**
   * Return a partial media package filtering assets. Assets are elements the archive is going to manager, i.e. all
   * non-publication elements.
   */
  static PartialMediaPackage assetsOnly(MediaPackage mp) {
    final Pred<MediaPackageElement> isAsset = Pred.mk(isNotPublication.toFn());
    return PartialMediaPackage.mk(mp, isAsset);
  }

  /**
   * Extract the file name from a media package elements URN.
   *
   * @return the file name or none if it could not be determined
   */
  public static Optional<String> getFileNameFromUrn(MediaPackageElement mpe) {
    Fn<URI, String> toString = new Fn<URI, String>() {
      @Override
      public String apply(URI uri) {
        return uri.toString();
      }
    };

    Optional<URI> uri = Optional.ofNullable(mpe.getURI());
    if (uri.isPresent() && "urn".equals(uri.get().getScheme())) {
      String[] tmp = uri.get().toString().split(":");
      if (tmp.length < 1) {
        return Optional.empty();
      }
      return Optional.of(tmp[tmp.length - 1]);
    }
    return Optional.empty();
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

  public void fireEventHandlers(AssetManagerItem item) {
    while (handlers.size() != 2) {
      logger.warn("Expecting 2 handlers, but {} are registered.  Waiting 10s then retrying...", handlers.size());
      try {
        Thread.sleep(10000L);
      } catch (InterruptedException e) { /* swallow this, nothing to do */ }
    }
    for (AssetManagerUpdateHandler handler : handlers) {
      handler.execute(item);
    }
  }

  /**
   * Call {@link
   * org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery#run(AbstractADeleteQuery.DeleteEpisodeHandler)}
   * with a delete handler. Also make sure to propagate the behaviour to subsequent instances.
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

  /**
   * Get the function to update a commented event in the Elasticsearch index.
   *
   * @return the function to do the update
   */
  private Function<Optional<Event>, Optional<Event>> getEventUpdateFunction(Snapshot snapshot,
          String orgId, User user) {
    return (Optional<Event> eventOpt) -> {
      MediaPackage mp = snapshot.getMediaPackage();
      String eventId = mp.getIdentifier().toString();
      Event event = eventOpt.orElse(new Event(eventId, orgId));

      AccessControlList acl = authorizationService.getActiveAcl(mp).getA();
      List<ManagedAcl> acls = aclServiceFactory.serviceFor(securityService.getOrganization()).getAcls();
      for (final ManagedAcl managedAcl : AccessInformationUtil.matchAcls(acls, acl)) {
        event.setManagedAcl(managedAcl.getName());
      }
      event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
      event.setArchiveVersion(Long.parseLong(snapshot.getVersion().toString()));
      if (StringUtils.isBlank(event.getCreator())) {
        event.setCreator(securityService.getUser().getName());
      }
      EventIndexUtils.updateEvent(event, mp);

      for (Catalog catalog: mp.getCatalogs(MediaPackageElements.EPISODE)) {
        try (InputStream in = workspace.read(catalog.getURI())) {
          EventIndexUtils.updateEvent(event, DublinCores.read(in));
        } catch (IOException | NotFoundException e) {
          throw new IllegalStateException(String.format("Unable to load dublin core catalog for event '%s'",
                  mp.getIdentifier()), e);
        }
      }

      // Update series name if not already done
      try {
        EventIndexUtils.updateSeriesName(event, orgId, user, index);
      } catch (SearchIndexException e) {
        logger.error("Error updating the series name of the event {} in the {} index.", eventId, index.getIndexName(),
                e);
      }
      return Optional.of(event);
    };
  }
}
