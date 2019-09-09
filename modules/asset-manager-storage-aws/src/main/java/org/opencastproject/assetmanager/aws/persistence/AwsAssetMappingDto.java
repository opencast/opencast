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

package org.opencastproject.assetmanager.aws.persistence;

import org.opencastproject.assetmanager.impl.storage.StoragePath;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "AwsAssetMapping")
@Table(name = "oc_aws_asset_mapping")
@NamedQueries({
        // These exclude deleted mappings!
        @NamedQuery(name = "AwsAssetMapping.findActiveMapping", query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND m.mediaPackageId = :mediaPackageId AND m.mediaPackageElementId = :mediaPackageElementId AND m.version = :version AND m.deletionDate IS NULL"),
        @NamedQuery(name = "AwsAssetMapping.findAllActiveByObjectKey", query = "SELECT m FROM AwsAssetMapping m WHERE m.objectKey = :objectKey AND m.deletionDate IS NULL"),
        @NamedQuery(name = "AwsAssetMapping.findAllActiveByMediaPackage", query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND m.mediaPackageId = :mediaPackageId  AND m.deletionDate IS NULL"),
        @NamedQuery(name = "AwsAssetMapping.findAllActiveByMediaPackageAndVersion", query = "SELECT m FROM AwsAssetMapping m WHERE m.organizationId = :organizationId AND m.mediaPackageId = :mediaPackageId AND m.version = :version AND m.deletionDate IS NULL"),
        // This is to be used when restoring/re-ingesting mps so includes deleted mapings!
        @NamedQuery(name = "AwsAssetMapping.findAllByMediaPackage", query = "SELECT m FROM AwsAssetMapping m WHERE m.mediaPackageId = :mediaPackageId ORDER BY m.version DESC") })
public final class AwsAssetMappingDto {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  @XmlAttribute
  private long id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organizationId;

  @Column(name = "media_package", nullable = false, length = 128)
  private String mediaPackageId;

  @Column(name = "media_package_element", nullable = false, length = 128)
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

  public static AwsAssetMappingDto storeMapping(EntityManager em, StoragePath path, String objectKey,
          String objectVersion) throws AwsAssetDatabaseException {
    AwsAssetMappingDto mapDto = new AwsAssetMappingDto(path.getOrganizationId(), path.getMediaPackageId(),
            path.getMediaPackageElementId(), Long.valueOf(path.getVersion().toString()), objectKey, objectVersion);

    EntityTransaction tx = em.getTransaction();
    try {
      AwsAssetMappingDto existing = findMapping(em, path);

      //If we've already seen this at some point but deleted it, just undelete it
      if (null != existing && objectKey.equals(existing.objectKey) && objectVersion.equals(existing.objectVersion)) {
        tx.begin();
        existing.setDeletionDate(null);
        tx.commit();
        return existing;
      } else {
        tx.begin();
        em.persist(mapDto);
        tx.commit();
        return mapDto;
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
    }
      throw new AwsAssetDatabaseException(String.format("Could not store mapping for path %s", path), e);
    } finally {
      em.close();
    }
  }

  /** Find a mapping by its storage path. Returns null if not found. */
  public static AwsAssetMappingDto findMapping(EntityManager em, final StoragePath path)
          throws AwsAssetDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("AwsAssetMapping.findActiveMapping");
      query.setParameter("organizationId", path.getOrganizationId());
      query.setParameter("mediaPackageId", path.getMediaPackageId());
      query.setParameter("mediaPackageElementId", path.getMediaPackageElementId());
      query.setParameter("version", Long.valueOf(path.getVersion().toString()));
      return (AwsAssetMappingDto) query.getSingleResult();
    } catch (NoResultException e) {
      return null; // Not found
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  /** Find all assets that link to the AWS S3 object passed. */
  @SuppressWarnings("unchecked")
  public static List<AwsAssetMappingDto> findMappingsByKey(EntityManager em, final String objectKey)
          throws AwsAssetDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("AwsAssetMapping.findAllActiveByObjectKey");
      query.setParameter("objectKey", objectKey);
      return query.getResultList();
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  /** Find all assets that belong to a media package and version (optional). */
  @SuppressWarnings("unchecked")
  public static List<AwsAssetMappingDto> findMappingsByMediaPackageAndVersion(EntityManager em,
          final StoragePath path) throws AwsAssetDatabaseException {
    Query query = null;
    try {
      // Find a specific versions?
      if (path.getVersion() != null) {
        query = em.createNamedQuery("AwsAssetMapping.findAllActiveByMediaPackageAndVersion");
        query.setParameter("version", Long.valueOf(path.getVersion().toString()));
      } else
        query = em.createNamedQuery("AwsAssetMapping.findAllActiveByMediaPackage");

      query.setParameter("organizationId", path.getOrganizationId());
      query.setParameter("mediaPackageId", path.getMediaPackageId());

      return query.getResultList();
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  /**
   * Marks mapping as deleted.
   */
  public static void deleteMappping(EntityManager em, StoragePath path) throws AwsAssetDatabaseException {
    AwsAssetMappingDto mapDto = findMapping(em, path);
    if (mapDto == null)
      return;

    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      mapDto.setDeletionDate(new Date());
      em.merge(mapDto);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new AwsAssetDatabaseException(String.format("Could not store mapping for path %s", path), e);
    } finally {
      em.close();
    }
  }

  /** Find all mappings that belong to a media package id. Also returns deleted mappings! */
  @SuppressWarnings("unchecked")
  public static List<AwsAssetMappingDto> findMappingsByMediaPackage(EntityManager em, final String mpId)
          throws AwsAssetDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("AwsAssetMapping.findAllByMediaPackage");
      query.setParameter("mediaPackageId", mpId);

      return query.getResultList();
    } catch (Exception e) {
      throw new AwsAssetDatabaseException(e);
    }
  }

  public void setDeletionDate(Date deletionDate) {
    this.deletionDate = deletionDate;
  }

}
