/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.episode.impl.persistence;

import org.opencastproject.episode.impl.StoragePath;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.net.URI;

import static org.opencastproject.episode.api.Version.version;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.PersistenceUtil.runFirstResultQuery;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;
import static org.opencastproject.util.persistence.PersistenceUtil.runUpdate;

/** JPA link to {@link org.opencastproject.episode.impl.persistence.Asset}. */
@Entity(name = "Asset")
@Table(name = "episode_asset")
@NamedQueries({
        @NamedQuery(name = "Asset.findByUri", query = "SELECT a FROM Asset a WHERE a.uri = :uri"),
        @NamedQuery(name = "Asset.findByChecksum", query = "SELECT a FROM Asset a WHERE a.checksum = :checksum"),
        @NamedQuery(name = "Asset.deleteByMediaPackageId", query = "DELETE FROM Asset a WHERE a.mediaPackageId = :mpId") })
public final class AssetDto {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "uri", length = 256, nullable = false)
  private String uri;

  @Column(name = "organization_id", nullable = false, length = 128)
  private String organizationId;

  @Column(name = "mediapackage_id", nullable = false, length = 128)
  private String mediaPackageId;

  @Column(name = "mediapackageelement_id", nullable = false, length = 128)
  private String mediaPackageElementId;

  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "checksum", nullable = false, length = 64)
  private String checksum;

  /** Create a new DTO. */
  public static AssetDto create(URI uri, StoragePath path, String checksum) {
    final AssetDto dto = new AssetDto();
    dto.uri = uri.toString();
    dto.organizationId = path.getOrganizationId();
    dto.mediaPackageId = path.getMediaPackageId();
    dto.mediaPackageElementId = path.getMediaPackageElementId();
    dto.version = path.getVersion().value();
    dto.checksum = checksum;
    return dto;
  }

  /** Convert into business object. */
  public Asset toAsset() {
    return new Asset(URI.create(uri), spath(organizationId, mediaPackageId, version(version), mediaPackageElementId), checksum);
  }

  /** Find an asset by its URI. */
  public static Function<EntityManager, Option<AssetDto>> findByUri(final URI uri) {
    return new Function<EntityManager, Option<AssetDto>>() {
      @Override public Option<AssetDto> apply(EntityManager em) {
        return runSingleResultQuery(em, "Asset.findByUri", tuple("id", uri.toString()));
      }
    };
  }

  /** Find an arbitrary asset having the same checksum. */
  public static Function<EntityManager, Option<AssetDto>> findOneByChecksum(final String checksum) {
    return new Function<EntityManager, Option<AssetDto>>() {
      @Override public Option<AssetDto> apply(EntityManager em) {
        return runFirstResultQuery(em, "Asset.findByChecksum", tuple("checksum", checksum));
      }
    };
  }

  /**
   * Delete assets by media package ID.
   * @return true if at least on asset has been deleted
   */
  public static boolean deleteByMediaPackageId(EntityManager em, String mpId) {
    return runUpdate(em, "Asset.deleteByMediaPackageId", tuple("mpId", mpId));
  }

  /** Convert a DTO into the corresponding business object. */
  public static final Function<AssetDto, Asset> toAsset = new Function<AssetDto, Asset>() {
    @Override public Asset apply(AssetDto dto) {
      return dto.toAsset();
    }
  };
}

