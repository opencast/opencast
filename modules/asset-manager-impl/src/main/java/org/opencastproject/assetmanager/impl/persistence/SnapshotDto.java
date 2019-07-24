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

import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.SnapshotImpl;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;

import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.UniqueConstraint;

/** JPA DTO. */
@Entity(name = "Snapshot")
@Table(name = "oc_assets_snapshot",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"mediapackage_id", "version"})})
@NamedQueries({
        @NamedQuery(name = "Snapshot.countByMediaPackage", query = "select count(s) from Snapshot s "
                + "where s.mediaPackageId = :mediaPackageId"),
        @NamedQuery(name = "Snapshot.countByMediaPackageAndOrg", query = "select count(s) from Snapshot s "
                + "where s.mediaPackageId = :mediaPackageId and s.organizationId = :organizationId")})
// Maintain own generator to support database migrations from Archive to AssetManager
// The generator's initial value has to be set after the data migration.
// Otherwise duplicate key errors will most likely happen.
@TableGenerator(name = "seq_oc_assets_snapshot", initialValue = 0, allocationSize = 50)
public class SnapshotDto {
  private static final Logger logger = LoggerFactory.getLogger(SnapshotDto.class);

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "seq_oc_assets_snapshot")
  private Long id;

  @Column(name = "mediapackage_id", length = 128, nullable = false)
  private String mediaPackageId;

  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "series_id", length = 128)
  private String seriesId;

  @Column(name = "organization_id", length = 128, nullable = false)
  private String organizationId;

  @Column(name = "archival_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date archivalDate;

  @Column(name = "availability", nullable = false)
  private String availability;

  @Column(name = "storage_id", nullable = false)
  private String storageId;

  @Column(name = "owner", nullable = false)
  private String owner;

  @Lob
  @Column(name = "mediapackage_xml", length = 65535, nullable = false)
  private String mediaPackageXml;

  @CascadeOnDelete
  @OneToMany(targetEntity = AssetDto.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "snapshot")
  private Set<AssetDto> assets;

  public static SnapshotDto mk(
          MediaPackage mediaPackage,
          VersionImpl version,
          String organization,
          Date archivalDate,
          Availability availability,
          String storageId,
          String owner) {
    try {
      final SnapshotDto dto = new SnapshotDto();
      dto.mediaPackageId = mediaPackage.getIdentifier().toString();
      dto.version = version.value();
      dto.seriesId = mediaPackage.getSeries();
      dto.organizationId = organization;
      dto.archivalDate = archivalDate;
      dto.mediaPackageXml = MediaPackageParser.getAsXml(mediaPackage);
      dto.availability = availability.name();
      dto.storageId = storageId;
      dto.owner = owner;
      return dto;
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public static SnapshotDto mk(Snapshot snapshot) {
    try {
      return mk(snapshot.getMediaPackage(),
              VersionImpl.mk(Long.parseLong(snapshot.getVersion().toString())),
              snapshot.getOrganizationId(),
              snapshot.getArchivalDate(),
              snapshot.getAvailability(),
              snapshot.getStorageId(),
              snapshot.getOwner());
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public Long getId() {
    return Database.insidePersistenceContextCheck(id);
  }

  public VersionImpl getVersion() {
    return Conversions.toVersion(version);
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getStorageId() {
    return storageId;
  }

  void setAvailability(Availability a) {
    this.availability = a.name();
  }

  void setStorageId(String id) {
    this.storageId = id;
  }

  public boolean addAsset(AssetDto asset) {
    return this.assets.add(asset);
  }

  public boolean removeAsset(AssetDto asset) {
    return this.assets.remove(asset);
  }

  public Snapshot toSnapshot() {
    MediaPackage mediaPackage = Conversions.toMediaPackage(mediaPackageXml);
    // ensure elements are tagged `archive`
    for (MediaPackageElement element: mediaPackage.getElements()) {
      if (!Arrays.asList(element.getTags()).contains("archive")) {
        logger.debug("Adding additional tag `archive` to element {} retrieved from asset manager", element);
        element.addTag("archive");
      }
    }
    return new SnapshotImpl(
            id,
            Conversions.toVersion(version),
            organizationId,
            archivalDate,
            Availability.valueOf(availability),
            storageId,
            owner,
            mediaPackage);
  }

  /**
   * Check if any snapshot with the given media package exists.
   *
   * @param em
   *          An entity manager to sue
   * @param mediaPackageId
   *          The media package identifier to check for
   * @return If a snapshot exists for the given media package
   */
  public static boolean exists(EntityManager em, final String mediaPackageId) {
    return exists(em, mediaPackageId, null);
  }

  /**
   * Check if any snapshot with the given media package exists.
   *
   * @param em
   *          An entity manager to sue
   * @param mediaPackageId
   *          The media package identifier to check for
   * @param organization
   *          An organization to limit the check for
   * @return If a snapshot exists for the given media package
   */
  public static boolean exists(EntityManager em, final String mediaPackageId, final String organization) {
    TypedQuery<Long> query;
    if (organization == null) {
      query = em.createNamedQuery("Snapshot.countByMediaPackage", Long.class)
              .setParameter("mediaPackageId", mediaPackageId);
    } else {
      query = em.createNamedQuery("Snapshot.countByMediaPackageAndOrg", Long.class)
              .setParameter("mediaPackageId", mediaPackageId)
              .setParameter("organizationId", organization);
    }
    logger.debug("Executing query {}", query);
    return query.getSingleResult() > 0;
  }
}
