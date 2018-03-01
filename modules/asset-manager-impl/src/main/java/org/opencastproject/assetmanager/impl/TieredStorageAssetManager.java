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

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.RemoteAssetStore;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import java.util.Date;
import java.util.Set;

public interface TieredStorageAssetManager extends AssetManager {

  Set<String> getRemoteAssetStoreIds();

  void addRemoteAssetStore(RemoteAssetStore assetStore);
  void removeRemoteAssetStore(RemoteAssetStore assetStore);
  Opt<AssetStore> getRemoteAssetStore(String id);

  Opt<AssetStore> getAssetStore(String storeId);

  void moveSnapshotToStore(Version version, String mpId, String storeId)
          throws NotFoundException;

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
   * Moves all versions of a given mediapackage ID from their respective source stores to a single target store
   * @param mpId
   *   The mediapackage ID to move
   * @param targetStore
   *   The store ID to move all versions of this mediapackage to
   * @throws NotFoundException
   */
  void moveSnapshotsById(String mpId, String targetStore) throws NotFoundException;

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
  void moveSnapshotsByIdAndVersion(String mpId, Version version, String targetStore)
          throws NotFoundException;

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
  void moveSnapshotsByDate(Date start, Date end, String targetStore)
          throws NotFoundException;

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
  void moveSnapshotsByIdAndDate(String mpId, Date start, Date end, String targetStore)
          throws NotFoundException;

  Opt<String> getSnapshotStorageLocation(Version version, String mpId) throws NotFoundException;

  Opt<String> getSnapshotStorageLocation(Snapshot snap) throws NotFoundException;

  Opt<String> getSnapshotRetrievalTime(Version version, String mpId);

  Opt<String> getSnapshotRetrievalCost(Version version, String mpId);
}
