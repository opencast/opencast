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
package org.opencastproject.assetmanager.impl.persistence;

import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.impl.VersionImpl;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.ListBuilders;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.Expression;

import javax.persistence.EntityManager;

/**
 * Collection of non-JPA DTOs to tuple up the {@link AssetDto} JPA entity with some additional joined data.
 */
public final class AssetDtos {
  private AssetDtos() {
  }

  /**
   * Create base join for a {@link AssetDtos} query.
   */
  public static JPAQuery baseJoin(EntityManager em) {
    final QAssetDto assetDto = QAssetDto.assetDto;
    final QSnapshotDto snapshotDto = QSnapshotDto.snapshotDto;
    return new JPAQuery(em, Database.TEMPLATES)
            .from(assetDto)
            .leftJoin(snapshotDto).on(snapshotDto.id.eq(assetDto.snapshotId));
  }

  /**
   * With some more data.
   */
  public static class Medium {
    private final AssetDto assetDto;
    private final String availability;
    private final String storageId;
    private final String organizationId;
    private final String owner;

    public Medium(AssetDto assetDto, Availability availability, String storageId, String organizationId, String owner) {
      this.assetDto = assetDto;
      this.availability = availability.name();
      this.storageId = storageId;
      this.organizationId = organizationId;
      this.owner = owner;
    }

    public AssetDto getAssetDto() {
      return assetDto;
    }

    public Availability getAvailability() {
      return Availability.valueOf(availability);
    }

    public String getStorageId() {
      return storageId;
    }

    public String getOrganizationId() {
      return organizationId;
    }

    public String getOwner() {
      return owner;
    }

    public static final Fn<Tuple, Medium> fromTuple = new Fn<Tuple, Medium>() {
      @Override public Medium apply(Tuple result) {
        return new Medium(
                result.get(QAssetDto.assetDto),
 Availability.valueOf(result
                .get(QSnapshotDto.snapshotDto.availability)),
                result.get(QSnapshotDto.snapshotDto.storageId),
                result.get(QSnapshotDto.snapshotDto.organizationId),
                result.get(QSnapshotDto.snapshotDto.owner));
      }
    };

    /**
     * Parameter for query execution methods like
     * {@link com.mysema.query.jpa.impl.JPAQuery#singleResult(com.mysema.query.types.Expression[])} or
     * {@link com.mysema.query.jpa.impl.JPAQuery#list(Expression[])}.
     */
    public static final Expression<?>[] select =
            new Expression[]{QAssetDto.assetDto, QSnapshotDto.snapshotDto.availability, QSnapshotDto.snapshotDto.organizationId};
  }

  /**
   * ... and even more.
   */
  public static class Full extends Medium {
    private final String mediaPackageId;
    private final VersionImpl version;

    public Full(AssetDto assetDto, Availability availability, String storageId, String organizationId, String owner,
                String mediaPackageId, VersionImpl version) {
      super(assetDto, availability, storageId, organizationId, owner);
      this.mediaPackageId = mediaPackageId;
      this.version = version;
    }

    public String getMediaPackageId() {
      return mediaPackageId;
    }

    public VersionImpl getVersion() {
      return version;
    }

    public static final Fn<Tuple, Full> fromTuple = new Fn<Tuple, Full>() {
      @Override public Full apply(Tuple result) {
        final Medium m = Medium.fromTuple.apply(result);
        return new Full(m.getAssetDto(), m.getAvailability(), m.getStorageId(), m.getOrganizationId(), m.getOwner(),
                        result.get(QSnapshotDto.snapshotDto.mediaPackageId),
                        VersionImpl.mk(result.get(QSnapshotDto.snapshotDto.version)));
      }
    };

    /**
     * Parameter for query execution methods like
     * {@link com.mysema.query.jpa.impl.JPAQuery#singleResult(com.mysema.query.types.Expression[])} or
     * {@link com.mysema.query.jpa.impl.JPAQuery#list(Expression[])}.
     */
    public static final Expression<?>[] select =
            $(Medium.select).append(ListBuilders.SIA.mk(
                    QSnapshotDto.snapshotDto.mediaPackageId, QSnapshotDto.snapshotDto.version))
                    .toList().toArray(new Expression[2]);
  }
}
