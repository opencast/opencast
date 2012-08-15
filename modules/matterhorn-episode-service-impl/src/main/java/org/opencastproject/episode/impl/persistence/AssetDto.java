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

import static org.opencastproject.episode.api.Version.version;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.PersistenceUtil.runFirstResultQuery;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;

import org.opencastproject.episode.impl.StoragePath;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/** An archived media package element. */
@Entity(name = "Asset")
@Table(name = "episode_asset")
@NamedQueries({
        @NamedQuery(name = "Asset.findByUri", query = "SELECT a FROM Asset a WHERE a.uri = :uri"),
        @NamedQuery(name = "Asset.findByChecksum", query = "SELECT a FROM Asset a WHERE a.checksum = :checksum"),
        @NamedQuery(name = "Asset.findByElementIdAndChecksum", query = "SELECT a FROM Asset a WHERE a.checksum = :checksum AND a.mediaPackageElementId = :mediaPackageElementId") })
public final class AssetDto {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "uri", length = 256, nullable = false)
  private String uri;

  @Column(name = "organization_id", nullable = false)
  private String organizationId;

  @Column(name = "mediapackage_id", nullable = false)
  private String mediaPackageId;

  @Column(name = "mediapackageelement_id", nullable = false)
  private String mediaPackageElementId;

  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "checksum", nullable = false)
  private String checksum;

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

  public Asset toAsset() {
    return new Asset(URI.create(uri), spath(organizationId, mediaPackageId, version(version), mediaPackageElementId), checksum);
  }

  public static Function<EntityManager, Option<AssetDto>> findByUri(final URI uri) {
    return new Function<EntityManager, Option<AssetDto>>() {
      @Override public Option<AssetDto> apply(EntityManager em) {
        return runSingleResultQuery(em, "Asset.findByUri", tuple("id", uri.toString()));
      }
    };
  }

  public static Function<EntityManager, Option<AssetDto>> findByChecksum(final String checksum) {
    return new Function<EntityManager, Option<AssetDto>>() {
      @Override public Option<AssetDto> apply(EntityManager em) {
        return runFirstResultQuery(em, "Asset.findByChecksum", tuple("checksum", checksum));
      }
    };
  }

  public static Function<EntityManager, Option<AssetDto>> findByElementIdAndChecksum(final String elementId,
          final String checksum) {
    return new Function<EntityManager, Option<AssetDto>>() {
      @Override
      public Option<AssetDto> apply(EntityManager em) {
        return runFirstResultQuery(em, "Asset.findByElementIdAndChecksum", tuple("mediaPackageElementId", elementId),
                tuple("checksum", checksum));
      }
    };
  }

  public static final Function<AssetDto, Asset> toAsset = new Function<AssetDto, Asset>() {
    @Override public Asset apply(AssetDto dto) {
      return dto.toAsset();
    }
  };
}

