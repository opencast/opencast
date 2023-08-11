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

package org.opencastproject.assetmanager.aws.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.assetmanager.api.storage.StoragePath;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "AwsAssetMapping")
@Table(
    name = "oc_aws_asset_mapping",
    indexes = {
        @Index(
            name = "IX_oc_aws_asset_mapping_object_key",
            columnList = ("object_key")
        )
    },
    uniqueConstraints = @UniqueConstraint(
        name = "UNQ_aws_archive_mapping_0",
        columnNames = {"organization", "mediapackage", "mediapackage_element", "version"}
    )
)
@NamedQueries({
    // These exclude deleted mappings!
    @NamedQuery(
        name = "AwsAssetMapping.findActiveMapping",
        query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND "
            + "m.mediaPackageId = :mediaPackageId AND m.mediaPackageElementId = :mediaPackageElementId AND "
            + "m.version = :version AND m.deletionDate IS NULL"
    ),
    @NamedQuery(
        name = "AwsAssetMapping.findAllActiveByObjectKey",
        query = "SELECT m FROM AwsAssetMapping m WHERE m.objectKey = :objectKey AND m.deletionDate IS NULL"
    ),
    @NamedQuery(
        name = "AwsAssetMapping.findAllActiveByMediaPackage",
        query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND "
            + "m.mediaPackageId = :mediaPackageId  AND m.deletionDate IS NULL"
    ),
    @NamedQuery(
        name = "AwsAssetMapping.findAllActiveByMediaPackageAndVersion",
        query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND "
            + "m.mediaPackageId = :mediaPackageId AND m.version = :version AND m.deletionDate IS NULL"
    ),
    // This is to be used when restoring/re-ingesting mps so includes deleted mapings!
    @NamedQuery(
        name = "AwsAssetMapping.findAllByMediaPackage",
        query = "SELECT m FROM AwsAssetMapping m WHERE m.mediaPackageId = :mediaPackageId ORDER BY m.version DESC"
    )
})
public class AwsAssetMappingDto {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  @XmlAttribute
  private long id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organizationId;

  @Column(name = "mediapackage", nullable = false, length = 128)
  private String mediaPackageId;

  @Column(name = "mediapackage_element", nullable = false, length = 128)
  private String mediaPackageElementId;

  @Column(name = "version", nullable = false)
  private Long version;

  // This is the AWS object key
  @Column(name = "object_key", nullable = false, length = 1024)
  private String objectKey;

  // This is the AWS object version
  @Column(name = "object_version", nullable = false, length = 1024)
  private String objectVersion;

  @Column(name = "deletion_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date deletionDate;

  public AwsAssetMappingDto() {
  }

  /** Constructor with all fields. */
  public AwsAssetMappingDto(String organizationId, String mediaPackageId, String elementId, Long version,
          String objectKey, String objectVersion) {
    this.organizationId = organizationId;
    this.mediaPackageId = mediaPackageId;
    this.mediaPackageElementId = elementId;
    this.version = version;
    this.objectKey = objectKey;
    this.objectVersion = objectVersion;
  }

  /** Convert into business object. */
  public AwsAssetMapping toAWSArchiveMapping() {
    return new AwsAssetMapping(organizationId, mediaPackageId, mediaPackageElementId, version, objectKey,
            objectVersion, deletionDate);
  }

  public static Function<EntityManager, AwsAssetMappingDto> storeMappingQuery(StoragePath path, String objectKey,
      String objectVersion) {
    return em -> {
      AwsAssetMappingDto mapDto = new AwsAssetMappingDto(
          path.getOrganizationId(),
          path.getMediaPackageId(),
          path.getMediaPackageElementId(),
          Long.valueOf(path.getVersion().toString()),
          objectKey,
          objectVersion
      );

      Optional<AwsAssetMappingDto> existing = findMappingQuery(path).apply(em);

      // If we've already seen this at some point but deleted it, just undelete it
      if (existing.isPresent() && objectKey.equals(existing.get().objectKey)
          && objectVersion.equals(existing.get().objectVersion)) {
        existing.get().setDeletionDate(null);
        return existing.get();
      } else {
        em.persist(mapDto);
        return mapDto;
      }
    };
  }

  /** Find a mapping by its storage path. Returns null if not found. */
  public static Function<EntityManager, Optional<AwsAssetMappingDto>> findMappingQuery(final StoragePath path) {
    return namedQuery.findOpt(
        "AwsAssetMapping.findActiveMapping",
        AwsAssetMappingDto.class,
        Pair.of("organizationId", path.getOrganizationId()),
        Pair.of("mediaPackageId", path.getMediaPackageId()),
        Pair.of("mediaPackageElementId", path.getMediaPackageElementId()),
        Pair.of("version", Long.valueOf(path.getVersion().toString()))
    );
  }

  /** Find all assets that link to the AWS S3 object passed. */
  public static Function<EntityManager, List<AwsAssetMappingDto>> findMappingsByKeyQuery(final String objectKey) {
    return namedQuery.findAll(
        "AwsAssetMapping.findAllActiveByObjectKey",
        AwsAssetMappingDto.class,
        Pair.of("objectKey", objectKey)
    );
  }

  /** Find all assets that belong to a media package and version (optional). */
  public static Function<EntityManager, List<AwsAssetMappingDto>> findMappingsByMediaPackageAndVersionQuery(
      final StoragePath path) {
    return em -> {
      // Find a specific versions?
      TypedQuery<AwsAssetMappingDto> query;
      if (path.getVersion() != null) {
        query = em.createNamedQuery("AwsAssetMapping.findAllActiveByMediaPackageAndVersion",
            AwsAssetMappingDto.class);
        query.setParameter("version", Long.valueOf(path.getVersion().toString()));
      } else {
        query = em.createNamedQuery("AwsAssetMapping.findAllActiveByMediaPackage", AwsAssetMappingDto.class);
      }

      query.setParameter("organizationId", path.getOrganizationId());
      query.setParameter("mediaPackageId", path.getMediaPackageId());

      return query.getResultList();
    };
  }

  /**
   * Marks mapping as deleted.
   */
  public static Consumer<EntityManager> deleteMapppingQuery(StoragePath path) {
    return em -> {
      Optional<AwsAssetMappingDto> mapDto = findMappingQuery(path).apply(em);
      if (mapDto.isEmpty()) {
        return;
      }
      mapDto.get().setDeletionDate(new Date());
      em.merge(mapDto.get());
    };
  }

  /** Find all mappings that belong to a media package id. Also returns deleted mappings! */
  public static Function<EntityManager, List<AwsAssetMappingDto>> findMappingsByMediaPackageQuery(final String mpId) {
    return namedQuery.findAll(
        "AwsAssetMapping.findAllByMediaPackage",
        AwsAssetMappingDto.class,
        Pair.of("mediaPackageId", mpId)
    );
  }

  public void setDeletionDate(Date deletionDate) {
    this.deletionDate = deletionDate;
  }
}
