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
package org.opencastproject.assetmanager.impl.query;

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Equality.hash;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import java.util.List;
import java.util.Optional;

public final class ARecordImpl implements ARecord {
  private final long snapshotId;
  private final String mediaPackageId;
  private final List<Property> properties;
  private Optional<Snapshot> snapshot;
  private Opt<SnapshotDto> snapshotDto;

  public ARecordImpl(
          long snapshotId,
          String mediaPackageId,
          List<Property> properties) {
    this.snapshotId = snapshotId;
    this.mediaPackageId = mediaPackageId;
    this.properties = properties;
    this.snapshot = Optional.empty();
    this.snapshotDto = Opt.none(SnapshotDto.class);
  }

  public ARecordImpl(
          long snapshotId,
          String mediaPackageId,
          List<Property> properties,
          Snapshot snapshot) {
    this(snapshotId, mediaPackageId, properties);
    if (snapshot != null) {
      this.snapshot = Optional.of(snapshot);
    }
  }

  public ARecordImpl(
          long snapshotId,
          String mediaPackageId,
      List<Property> properties,
          SnapshotDto snapshotDto) {
    this(snapshotId, mediaPackageId, properties);
    if (snapshotDto != null) {
      this.snapshotDto = Opt.some(snapshotDto);
    }
  }


  /** Get the database ID of the snapshot. */
  public long getSnapshotId() {
    return snapshotId;
  }

  @Override public String getMediaPackageId() {
    return mediaPackageId;
  }

  @Override public List<Property> getProperties() {
    return properties;
  }

  /**
   * Get the snapshot if set.
   * Otherwise try to convert snapshotDto to {@link Snapshot} with method call {@link SnapshotDto#toSnapshot()},
   * cache and return the result.
   *
   * @return the snapshot
   */
  @Override public Optional<Snapshot> getSnapshot() {
    if (snapshot.isEmpty() && snapshotDto.isSome()) {
      snapshot = Optional.of(snapshotDto.get().toSnapshot());
    }
    return snapshot;
  }

  /** Get the snapshotDto. */
  public Opt<SnapshotDto> getSnapshotDto() {
    return snapshotDto;
  }

  @Override public int hashCode() {
    return hash(snapshotId);
  }

  /** Two records are considered equal if their database IDs are equal. */
  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof ARecordImpl && eqFields((ARecordImpl) that));
  }

  private boolean eqFields(ARecordImpl that) {
    return eq(snapshotId, that.snapshotId);
  }

  public static final Fn<ARecordImpl, String> getMediaPackageId = new Fn<ARecordImpl, String>() {
    @Override public String apply(ARecordImpl a) {
      return a.getMediaPackageId();
    }
  };
}
