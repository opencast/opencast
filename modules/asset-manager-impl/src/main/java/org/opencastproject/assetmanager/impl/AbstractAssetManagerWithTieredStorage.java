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

import static java.lang.String.format;
import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Enrichments;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractAssetManagerWithTieredStorage extends AbstractAssetManager implements TieredStorageAssetManager {

  public static final Set<MediaPackageElement.Type> MOVABLE_TYPES =
          Sets.newHashSet(MediaPackageElement.Type.Attachment, MediaPackageElement.Type.Catalog, MediaPackageElement.Type.Track);

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractAssetManager.class);

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
          } catch (Exception e) {
            chuck(e);
          }
          getDb().setStorageLocation(s, targetStoreId);
          deleteAssetsFromStore(s, currentStore);
        } else {
          //Else, the content is *not* local and is going to a *different* remote
          String intermediateStore = getLocalAssetStore().getStoreType();
          logger.debug(format("Moving {} from {} to {}, then to {}",
                  s.toString(), currentStoreId, intermediateStore, targetStoreId));
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
  private ASelectQuery getStoredInStore(final AQueryBuilder q, final Version version, final String mpId, final String storeId) {
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
          for (final InputStream assetStream : store.get(StoragePath.mk(asset.getOrganizationId(), mpId, version, mpeId))) {
            final Asset a = new AssetImpl(
                    AssetId.mk(version, mpId, mpeId),
                    assetStream,
                    asset.getAssetDto().getMimeType(),
                    asset.getAssetDto().getSize(),
                    asset.getStorageId(),
                    asset.getAvailability());
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
    logger.debug(format("Moving assets for snapshot %s to store %s", prettyMpId, store.getStoreType()));
    for (final MediaPackageElement e : snap.getMediaPackage().getElements()) {
      if (!MOVABLE_TYPES.contains(e.getElementType())) {
        logger.debug(format("Skipping %s because type is %s", e.getIdentifier(), e.getElementType()));
        continue;
      }
      logger.debug(format("Moving %s to store %s", e.getIdentifier(), store.getStoreType()));
      final StoragePath storagePath = StoragePath.mk(orgId, mpId, version, e.getIdentifier());
      if (store.contains(storagePath)) {
        logger.debug(format("Element %s (version %s) is already in store %s so skipping it", e.getIdentifier(),
                version.toString(),
                store.getStoreType()));
        continue;
      }
      final Opt<StoragePath> existingAssetOpt = findAssetInVersionsAndStores(e.getChecksum().toString(), store.getStoreType());
      if (existingAssetOpt.isSome()) {
        final StoragePath existingAsset = existingAssetOpt.get();
        logger.debug("Content of asset {} with checksum {} already exists in {}",
                existingAsset.getMediaPackageElementId(), e.getChecksum(), store.getStoreType());
        if (!store.copy(existingAsset, storagePath)) {
          throw new AssetManagerException(
                  format("An asset with checksum %s has already been archived but trying to copy or link asset %s to it failed",
                          e.getChecksum(), existingAsset));
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
    store.delete(DeletionSelector.delete(snap.getOrganizationId(), snap.getMediaPackage().getIdentifier().toString(), snap.getVersion()));
  }

  /** Check if element <code>e</code> is already part of the history and in a specific store */
  private Opt<StoragePath> findAssetInVersionsAndStores(final String checksum, final String storeId) throws Exception {
    return getDb().findAssetByChecksumAndStore(checksum, storeId).map(new Fn<AssetDtos.Full, StoragePath>() {
      @Override public StoragePath apply(AssetDtos.Full dto) {
        return StoragePath.mk(dto.getOrganizationId(), dto.getMediaPackageId(), dto.getVersion(), dto.getAssetDto().getMediaPackageElementId());
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
}
