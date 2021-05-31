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

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.hasNoChecksum;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;
import static org.opencastproject.mediapackage.MediaPackageSupport.getFileName;
import static org.opencastproject.mediapackage.MediaPackageSupport.getMediaPackageElementId;
import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;
import org.opencastproject.assetmanager.impl.query.AQueryBuilderImpl;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RequireUtil;
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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class AssetManagerWithTieredStorage implements AssetManager {

  private final Database db;
  private final HttpAssetProvider httpAssetProvider;
  private final AssetStore assetStore;
  private final Workspace workspace;
  private final SecurityService securityService;

  private HashMap<String, RemoteAssetStore> remoteStores = new LinkedHashMap<>();

  public AssetManagerWithTieredStorage(Database db, HttpAssetProvider httpAssetProvider, AssetStore assetStore,
          Workspace workspace, SecurityService securityService) {
    this.db = db;
    this.httpAssetProvider = httpAssetProvider;
    this.assetStore = assetStore;
    this.workspace = workspace;
    this.securityService = securityService;
  }
  public Database getDb() {
    return db;
  }

  public HttpAssetProvider getHttpAssetProvider() {
    return httpAssetProvider;
  }

  public AssetStore getLocalAssetStore() {
    return assetStore;
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
  public void addRemoteAssetStore(RemoteAssetStore store) {
    remoteStores.put(store.getStoreType(), store);
  }

  @Override
  public void removeRemoteAssetStore(RemoteAssetStore store) {
    remoteStores.remove(store.getStoreType());
  }

  protected Workspace getWorkspace() {
    return workspace;
  }

  protected String getCurrentOrgId() {
    return securityService.getOrganization().getId();
  }

  public static final Set<MediaPackageElement.Type> MOVABLE_TYPES = Sets.newHashSet(
      MediaPackageElement.Type.Attachment,
      MediaPackageElement.Type.Catalog,
      MediaPackageElement.Type.Track
  );

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerWithTieredStorage.class);

  // Base name of manifest file
  private static final String MANIFEST_DEFAULT_NAME = "manifest";

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
            chuck(e);
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
            chuck(e);
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

  /** Returns true if the store id is equal to the local asset store's id */
  private boolean isLocalAssetStoreId(String storeId) {
    return getLocalAssetStore().getStoreType().equals(storeId);
  }

  /** Returns true if the store id is not equal to the local asset store's id */
  private boolean isRemoteAssetStoreId(String storeId) {
    return !isLocalAssetStoreId(storeId);
  }

  @Override
  public Opt<Asset> getAsset(Version version, final String mpId, final String mpeId) {
    // try to fetch the asset
    for (final AssetDtos.Medium asset : getDb().getAsset(RuntimeTypes.convert(version), mpId, mpeId)) {
      for (final String storageId : getSnapshotStorageLocation(version, mpId)) {
        for (final AssetStore store : getAssetStore(storageId)) {
          for (final InputStream assetStream
              : store.get(StoragePath.mk(asset.getOrganizationId(), mpId, version, mpeId))) {

            Checksum checksum = null;
            try {
              checksum = Checksum.fromString(asset.getAssetDto().getChecksum());
            } catch (NoSuchAlgorithmException e) {
              logger.warn("Invalid checksum for asset {} of media package {}", mpeId, mpId, e);
            }

            final Asset a = new AssetImpl(
                    AssetId.mk(version, mpId, mpeId),
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
              "An asset with checksum %s has already been archived but trying to copy or link asset %s to it failed",
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

  // Return an estimated time to retrieve the snapshot to local disk
  public Opt<String> getSnapshotRetrievalTime(final Version version, final String mpId) {
    throw new NotImplementedException("");
  }

  // Return an estimated cost to retrieve the snapshot to local disk
  public Opt<String> getSnapshotRetrievalCost(final Version version, final String mpId) {
    throw new NotImplementedException("");
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
  public Snapshot takeSnapshot(final String owner, final MediaPackage mp) {
    if (owner == null) {
      return takeSnapshot(mp);
    }

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

  @Override
  public Snapshot takeSnapshot(MediaPackage mediaPackage) {
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    AQueryBuilder queryBuilder = createQuery();
    AResult result = queryBuilder.select(queryBuilder.snapshot())
            .where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest())).run();
    Opt<ARecord> record = result.getRecords().head();
    if (record.isSome()) {
      Opt<Snapshot> snapshot = record.get().getSnapshot();
      if (snapshot.isSome()) {
        return takeSnapshot(snapshot.get().getOwner(), mediaPackage);
      }
    }
    return takeSnapshot(DEFAULT_OWNER, mediaPackage);
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    getDb().setAvailability(RuntimeTypes.convert(version), mpId, availability);
  }

  @Override public boolean setProperty(Property property) {
    return getDb().saveProperty(property);
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

  @Override
  public List<Property> selectProperties(final String mediaPackageId, final String namespace) {
    return getDb().selectProperties(mediaPackageId, namespace);
  }

  @Override public AQueryBuilder createQuery() {
    return new AQueryBuilderImpl(this);
  }

  @Override public Opt<Version> toVersion(String version) {
    try {
      return Opt.<Version>some(VersionImpl.mk(Long.parseLong(version)));
    } catch (NumberFormatException e) {
      return Opt.none();
    }
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
                  "An asset with checksum %s has already been archived but trying to copy or link asset %s to it failed",
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

  /* -------------------------------------------------------------------------------------------------------------- */

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
