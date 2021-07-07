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
package org.opencastproject.assetmanager.api;

import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import java.util.Date;
import java.util.List;

/**
 * The AssetManager stores versioned {@linkplain Snapshot snapshots} of media packages.
 * <p>
 * It also supports the association of {@linkplain Property properties} to a
 * history of snapshots which is called an episode.
 *
 * <h1>Terms</h1>
 * <h2>Snapshot</h2>
 * A snapshot saves a particular version of a media package. Snapshots are
 * immutable and can only be deleted.
 *
 * <h2>Episode</h2>
 * An episode is the set of snapshots of a media package.
 *
 * <h2>Properties</h2>
 * Properties are associated with an episode and have a volatile character.
 * They support the quick and easy storage of meta data.
 * This removes the need for services to create their own persistence layer if
 * they want to associate metadata with a media package.
 *
 * <h1>Notes</h1>
 * Media package IDs are considered to be unique throughout the whole system.
 * The organization ID is just a discriminator and not necessary to uniquely identify a media package.
 */
public interface AssetManager {
  String DEFAULT_OWNER = "default";

  /**
   * Get the media package from the lates snapshot.
   *
   * @param mediaPackageId
   * @return mediapackage
   */
  Opt<MediaPackage> getMediaPackage(String mediaPackageId);

  /**
   * Get the asset that is uniquely identified by the triple {version, media package ID, media package element ID}.
   *
   * @param version the version
   * @param mpId the media package ID
   * @param mpeId the media package element ID
   * @return the asset or none, if no such asset exists
   */
  Opt<Asset> getAsset(Version version, String mpId, String mpeId);

  /**
   * Get an asset store by id (local or remote).
   *
   * @param storeId the store id
   * @return the asset store if it exists
   */
  Opt<AssetStore> getAssetStore(String storeId);

  /**
   * Get the remote asset stores as a list.
   *
   * @return a list of asset stores
   */
  List<AssetStore> getRemoteAssetStores();

  /**
   * Get the local asset store.
   *
   * @return the asset store
   */
  AssetStore getLocalAssetStore();


  /* Snapshots */

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @return If a snapshot exists for the given media package
   */
  boolean snapshotExists(String mediaPackageId);

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @param organization
   *          The organization to limit the search to
   * @return If a snapshot exists for the given media package
   */
  boolean snapshotExists(String mediaPackageId, String organization);

  /**
   * Returns a stream of {@link RichAResult} filtered by mediapackage IDs
   *
   * @param mpId
   *   The mediapackage ID to filter results for
   * @return
   *   The {@link RichAResult} stream filtered by mediapackage ID
   */
  RichAResult getSnapshotsById(String mpId);

  /**
   * Returns a stream of {@link RichAResult} filtered by mediapackage ID and version
   *
   * @param mpId
   *   The mediapackage ID to filter results for
   * @param version
   *   The version to filter results for
   * @return
   *   The {@link RichAResult} stream filtered by mediapackage ID
   */
  RichAResult getSnapshotsByIdAndVersion(String mpId, Version version);

  /**
   * Returns a stream of {@link RichAResult} filtered by date. This stream consists of all versions of all mediapackages
   * archived within the date range.
   *
   * @param start
   *   The start {@link Date} to filter by
   * @param end
   *   The end{@link Date} to filter by
   * @return
   *   The {@link RichAResult} stream filtered by date
   */
  RichAResult getSnapshotsByDate(Date start, Date end);

  /**
   * Returns a stream of {@link RichAResult} filtered by date and mediapackage. This stream consists of all versions of
   * a mediapackage archived within the date range.
   *
   * @param mpId
   *   The mediapackage ID to filter for
   * @param start
   *   The start {@link Date} to filter by
   * @param end
   *   The end{@link Date} to filter by
   * @return
   *   The {@link RichAResult} stream filtered by date
   */
  RichAResult getSnapshotsByIdAndDate(String mpId, Date start, Date end);

  /**
   * Take a versioned snapshot of a media package.
   * <p>
   * Snapshot are tagged with string identifying the owner. Only the owner
   * of a snapshot is allowed to delete it.
   * Ownership only affects the deletion of a snapshot.
   *
   * @param owner
   *          the owner of the snapshot, e.g. the name of the calling service
   */
  Snapshot takeSnapshot(String owner, MediaPackage mp);

  /**
   * Take a versioned snapshot of a media package using the owner of the last
   * snapshot or the default owner if it does not exist.
   *
   * @param mediaPackage
   *          The media package to snapshot
   * @return A new snapshot
   */
  Snapshot takeSnapshot(MediaPackage mediaPackage);

  /**
   * Move snapshot from current store to new store
   * Note: This may require downloading and re-uploading
   *
   * @param version The version to move
   * @param mpId The media package to move
   * @param storeId The store to move to
   * @throws NotFoundException
   */
  void moveSnapshotToStore(Version version, String mpId, String storeId) throws NotFoundException;

  /**
   * Moves all versions of a given mediapackage ID from their respective source stores to a single target store
   * @param mpId
   *   The mediapackage ID to move
   * @param targetStore
   *   The store ID to move all versions of this mediapackage to
   * @throws NotFoundException
   */
  void moveSnapshotsById(String mpId, String targetStore) throws NotFoundException;

  /**
   * Moves a specific version of a given mediapackage ID to a new store
   *
   * @param mpId
   *   The mediapackage ID to move
   * @param version
   *   The version to move
   * @param targetStore
   *   The store ID to move this version of the mediapackage to
   * @throws NotFoundException
   */
  void moveSnapshotsByIdAndVersion(String mpId, Version version, String targetStore) throws NotFoundException;

  /**
   * Moves all versions of all mediapackages archived within a data range to a new storage location.
   *
   * @param start
   *   The start {@link Date} to filter by
   * @param end
   *   The end{@link Date} to filter by
   * @param targetStore
   *   THe store ID to move the snapshots to
   * @throws NotFoundException
   */
  void moveSnapshotsByDate(Date start, Date end, String targetStore) throws NotFoundException;

  /**
   * Moves all versions of a mediapackage archived within a data range to a new storage location.
   *
   * @param mpId
   *   The mediapackage ID to filter for
   * @param start
   *   The start {@link Date} to filter by
   * @param end
   *   The end{@link Date} to filter by
   * @param targetStore
   *   THe store ID to move the snapshots to
   * @throws NotFoundException
   */
  void moveSnapshotsByIdAndDate(String mpId, Date start, Date end, String targetStore) throws NotFoundException;


  /* Properties */

  /**
   * Set a property. Use this method to either insert a new property or update an existing one.
   * Properties are stored per episode.
   *
   * @return false, if the referenced episode does not exist.
   */
  boolean setProperty(Property property);

  /**
   * Select all properties for a specific media package.
   *
   * @param mediaPackageId
   *          Media package identifier to check for
   * @param namespace
   *          Namespace to limit the search to
   * @return List of properties
   */
  List<Property> selectProperties(String mediaPackageId, String namespace);

  /**
   * Delete all properties for a given media package identifier
   *
   * @param mediaPackageId
   *          Media package identifier
   * @return Number of deleted properties
   */
  int deleteProperties(String mediaPackageId);

  /**
   * Delete all properties for a given media package identifier and namespace.
   *
   * @param mediaPackageId
   *          Media package identifier
   * @param namespace
   *          A namespace prefix to use for deletion
   * @return Number of deleted properties
   */
  int deleteProperties(String mediaPackageId, String namespace);


  /* Misc. */

  /** Create a new query builder. */
  AQueryBuilder createQuery();

  /**
   * Deserialize a version from a string. This is the inverse function of {@link Version#toString()}.
   *
   * @return a version or none, if no version can be archived from the given string
   */
  Opt<Version> toVersion(String version);

  /**
   * Count the number of events stored for a given organization.
   *
   * @param organization
   *          The organization to check
   * @return Number of events
   */
  long countEvents(String organization);
}
