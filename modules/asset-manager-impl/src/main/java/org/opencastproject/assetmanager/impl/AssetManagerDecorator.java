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

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.RemoteAssetStore;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import java.util.Date;
import java.util.Set;

public class AssetManagerDecorator<A extends TieredStorageAssetManager> implements TieredStorageAssetManager {
  protected final A delegate;

  public AssetManagerDecorator(A delegate) {
    this.delegate = delegate;
  }

  @Override public Snapshot takeSnapshot(String owner, MediaPackage mp) {
    return owner == null ? delegate.takeSnapshot(mp) : delegate.takeSnapshot(owner, mp);
  }

  @Override public Snapshot takeSnapshot(MediaPackage mp) {
    return takeSnapshot(null, mp);
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpeId) {
    return delegate.getAsset(version, mpId, mpeId);
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    delegate.setAvailability(version, mpId, availability);
  }

  @Override public boolean setProperty(Property property) {
    return delegate.setProperty(property);
  }

  @Override public AQueryBuilder createQuery() {
    return delegate.createQuery();
  }

  @Override public Opt<Version> toVersion(String version) {
    return delegate.toVersion(version);
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
}
