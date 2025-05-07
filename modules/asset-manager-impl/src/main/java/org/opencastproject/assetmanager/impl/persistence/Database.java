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
package org.opencastproject.assetmanager.impl.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.PartialMediaPackage;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.Queries;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 * Data access object.
 */
@ParametersAreNonnullByDefault
public class Database {
  private static final Logger logger = LoggerFactory.getLogger(Database.class);

  private final DBSession db;

  public Database(DBSession db) {
    this.db = db;
  }

  /**
   * Save a property to the database. This is either an insert or an update operation.
   */
  public boolean saveProperty(final Property property) {
    return db.execTx(em -> {
      final PropertyId pId = property.getId();
      final String mpId = pId.getMediaPackageId();
      final String namespace = pId.getNamespace();
      final String propertyName = pId.getName();

      // Check if media package exists in oc_assets_snapshot table
      Long snapshotCount = em.createQuery(
              "SELECT COUNT(s.id) FROM Snapshot s "
                  + "WHERE s.mediaPackageId = :mediaPackageId", Long.class)
          .setParameter("mediaPackageId", mpId)
          .getSingleResult();

      if (snapshotCount == 0) {
        return false; // media package does not exist
      }

      // Try to find existing property
      TypedQuery<PropertyDto> query = em.createQuery(
          "SELECT p FROM Property p "
              + "WHERE p.mediaPackageId = :mediaPackageId "
              + "AND p.namespace = :namespace "
              + "AND p.propertyName = :propertyName",
          PropertyDto.class);
      query.setParameter("mediaPackageId", mpId);
      query.setParameter("namespace", namespace);
      query.setParameter("propertyName", propertyName);

      PropertyDto existing = null;
      try {
        existing = query.getSingleResult();
      } catch (NoResultException e) {
        // property does not exist, we'll insert
      }

      PropertyDto updatedOrNew = existing == null
          ? PropertyDto.mk(property)
          : existing.update(property.getValue());

      namedQuery.persistOrUpdate(updatedOrNew).apply(em);
      return true;
    });
  }



  /**
   * Claim a new version for media package <code>mpId</code>.
   */
  public VersionImpl claimVersion(final String mpId) {
    return db.execTx(em -> {
      final Optional<VersionClaimDto> lastOpt = VersionClaimDto.findLastQuery(mpId).apply(em);
      if (lastOpt.isPresent()) {
        final VersionImpl claim = VersionImpl.next(lastOpt.get().getLastClaimed());
        VersionClaimDto.updateQuery(mpId, claim.value()).apply(em);
        return claim;
      } else {
        final VersionImpl first = VersionImpl.FIRST;
        em.persist(VersionClaimDto.mk(mpId, first.value()));
        return first;
      }
    });
  }

  /**
   * Save a snapshot and all of its assets.
   */
  public SnapshotDto saveSnapshot(
          final String orgId,
          final PartialMediaPackage pmp,
          final Date archivalDate,
          final VersionImpl version,
          final Availability availability,
          final String storageId,
          final String owner) {
    final SnapshotDto snapshotDto = SnapshotDto.mk(
            pmp.getMediaPackage(),
            version,
            orgId,
            archivalDate,
            availability,
            storageId,
            owner);
    return db.execTx(em -> {
      // persist snapshot
      em.persist(snapshotDto);
      // persist assets
      for (MediaPackageElement e : pmp.getElements()) {
        final AssetDto a = AssetDto.mk(
            e.getIdentifier(),
            snapshotDto,
            e.getChecksum().toString(),
            Optional.ofNullable(e.getMimeType()),
            storageId,
            e.getSize());
        em.persist(a);
      }
      return snapshotDto;
    });
  }

  public void setStorageLocation(Snapshot snapshot, final String storageId) {
    setStorageLocation(
        VersionImpl.mk(snapshot.getVersion()),
        snapshot.getMediaPackage().getIdentifier().toString(),
        storageId
    );
  }

  public void setStorageLocation(final VersionImpl version, final String mpId, final String storageId) {
    db.execTx(em -> {
      // Update snapshot
      namedQuery.update(
          "Snapshot.updateStorageIdByVersionAndMpId",
          Pair.of("storageId", storageId),
          Pair.of("version", version.value()),
          Pair.of("mediaPackageId", mpId)
      );

      // Update asset
      Optional<SnapshotDtos.Medium> optSnapshot = getSnapshot(version, mpId);
      if (optSnapshot.isPresent()) {
        // Update all associated assets
        namedQuery.update(
            "Asset.updateStorageIdBySnapshot",
            Pair.of("storageId", storageId),
            Pair.of("snapshot", optSnapshot.get().getSnapshotDto())
        ).apply(em);
      }

      return null;
    });
  }

  public void setAssetStorageLocation(final VersionImpl version, final String mpId, final String mpeId,
          final String storageId) {
    db.execTx(em -> {
      Optional<SnapshotDtos.Medium> optSnapshot = getSnapshot(version, mpId);
      if (optSnapshot.isPresent()) {
        // Update the asset store id
        namedQuery.update(
            "Asset.updateStorageIdBySnapshotAndMpElementId",
            Pair.of("storageId", storageId),
            Pair.of("snapshot", optSnapshot.get().getSnapshotDto()),
            Pair.of("mediaPackageElementId", mpeId)
        ).apply(em);
      }

      return null;
    });
  }

  public int setAvailability(final VersionImpl version, final String mpId, final Availability availability) {
    return db.execTx(em -> {
      return namedQuery.update(
          "Snapshot.updateAvailabilityByVersionAndMpId",
          Pair.of("availability", availability.name()),
          Pair.of("version", version.value()),
          Pair.of("mediaPackageId", mpId)
      ).apply(em);
    });
  }

  /**
   * Get an asset. If no version is specified return the latest version.
   *
   * @return the asset or none, if no asset can be found
   */
  public Optional<AssetDtos.Medium> getAsset(final VersionImpl version, final String mpId, final String mpeId) {
    return db.execTx(em -> {
      return Queries.namedQuery
          .findOpt(
              "Asset.findMediumByMpIdMpeIdAndVersion",
              Object[].class,
              Pair.of("mpId", mpId),
              Pair.of("mpeId", mpeId),
              Pair.of("version", version.value()))
          .apply(em)
          .map(result -> {
            AssetDto assetDto = (AssetDto) result[0];
            String availability = (String) result[1];
            String organizationId = (String) result[2];
            return new AssetDtos.Medium(assetDto, Availability.valueOf(availability), organizationId);
          });
    });
  }

  public Optional<SnapshotDtos.Medium> getSnapshot(final VersionImpl version, final String mpId) {
    return db.execTx(em -> {
      return namedQuery.findOpt(
          "Snapshot.findMediumByMpIdAndVersion",
            SnapshotDto.class,
            Pair.of("mpId", mpId),
            Pair.of("version", version.value()))
          .apply(em)
          .map(result -> new SnapshotDtos.Medium(
              result,
              Availability.valueOf(result.getAvailability()),
              result.getStorageId(),
              result.getOrganizationId(),
              result.getOwner()
          ));
    });
  }

  public Optional<AssetDtos.Full> findAssetByChecksum(final String checksum) {
    return db.execTx(em -> {
      return namedQuery.findOpt(
              "Asset.findByChecksum",
              AssetDto.class,
              Pair.of("checksum", checksum))
          .apply(em)
          .map(result -> {
            SnapshotDto snapshot = result.getSnapshot();
            return new AssetDtos.Full(
                result,
                Availability.valueOf(snapshot.getAvailability()),
                snapshot.getStorageId(),
                snapshot.getOrganizationId(),
                snapshot.getOwner(),
                snapshot.getMediaPackageId(),
                snapshot.getVersion()
            );
          });
    });
  }


  /**
   * Delete all properties for a given media package identifier
   *
   * @param mediaPackageId
   *          Media package identifier
   * @return Number of deleted rows
   */
  public int deleteProperties(final String mediaPackageId) {
    return db.execTx(PropertyDto.deleteQuery(mediaPackageId));
  }

  /**
   * Delete all properties for a given media package identifier and namespace.
   *
   * @param mediaPackageId
   *          Media package identifier
   * @param namespace
   *          A namespace prefix to use for deletion
   * @return Number of deleted rows
   */
  public int deleteProperties(final String mediaPackageId, final String namespace) {
    if (StringUtils.isBlank(namespace)) {
      return db.execTx(PropertyDto.deleteQuery(mediaPackageId));
    }
    return db.execTx(PropertyDto.deleteQuery(mediaPackageId, namespace));
  }

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @return If a snapshot exists for the given media package
   */
  public boolean snapshotExists(final String mediaPackageId) {
    return db.execTx(SnapshotDto.existsQuery(mediaPackageId));
  }

  /**
   * Check if any snapshot with the given media package identifier exists.
   *
   * @param mediaPackageId
   *          The media package identifier to check for
   * @param organization
   *          The organization to filter for
   * @return If a snapshot exists for the given media package
   */
  public boolean snapshotExists(final String mediaPackageId, final String organization) {
    return db.exec(SnapshotDto.existsQuery(mediaPackageId, organization));
  }

  /**
   * Select all properties for a specific media package.
   *
   * @param mediaPackageId
   *          Media package identifier to check for
   * @param namespace
   *          Namespace to limit the search to
   * @return List of properties
   */
  public List<Property> selectProperties(final String mediaPackageId, final String namespace) {
    return db.exec(PropertyDto.selectQuery(mediaPackageId, namespace));
  }

  /**
   * Count events with snapshots in the asset manager
   *
   * @param organization
   *          An organization to count in
   * @return Number of events
   */
  public long countEvents(final String organization) {
    return db.exec(SnapshotDto.countEventsQuery(organization));
  }

  /**
   * Count events with snapshots in the asset manager
   *
   * @param organization
   *          An organization to count in
   * @return Number of events
   */
  public long countSnapshots(final String organization) {
    return db.exec(SnapshotDto.countSnapshotsQuery(organization));
  }

  public long countAssets() {
    return db.exec(AssetDto.countAssetsQuery());
  }

  public long countProperties() {
    return db.exec(PropertyDto.countPropertiesQuery());
  }

  public Optional<AssetDtos.Full> findAssetByChecksumAndStoreAndOrg(final String checksum, final String storeId,
      final String orgId) {
    return db.execTx(em -> {
      return namedQuery.findOpt(
              "Asset.findByChecksumStorageIdAndOrganizationId",
              AssetDto.class,
              Pair.of("checksum", checksum),
              Pair.of("storageId", storeId),
              Pair.of("orgId", orgId))
          .apply(em)
          .map(result -> {
            SnapshotDto snapshot = result.getSnapshot();
            return new AssetDtos.Full(
                result,
                Availability.valueOf(snapshot.getAvailability()),
                snapshot.getStorageId(),
                snapshot.getOrganizationId(),
                snapshot.getOwner(),
                snapshot.getMediaPackageId(),
                snapshot.getVersion()
            );
          });
    });
  }

  public Optional<Snapshot> getLatestSnapshot(String mediaPackageId) {
    return getLatestSnapshot(mediaPackageId, null);
  }

  public Optional<Snapshot> getLatestSnapshot(String mediaPackageId, String orgId) {
    return db.execTx(em -> {
      Optional<SnapshotDto> snapshotDto = namedQuery.findOpt(
          "Snapshot.findLatest",
          SnapshotDto.class,
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("organizationId", orgId)
      ).apply(em);

      if (snapshotDto.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(snapshotDto.get().toSnapshot());
    });
  }

  public Optional<MediaPackage> getMediaPackage(String mediaPackageId) {
    return getMediaPackage(mediaPackageId, null);
  }
  public Optional<MediaPackage> getMediaPackage(String mediaPackageId, String orgId) {
    return getLatestSnapshot(mediaPackageId, orgId).map(Snapshot::getMediaPackage);
  }

  public List<Snapshot> getSnapshots(String mediaPackageId) {
    return getSnapshots(mediaPackageId, null);
  }

  public List<Snapshot> getSnapshots(String mediaPackageId, String orgId) {
    return getSnapshots(mediaPackageId, orgId, null);
  }

  public List<Snapshot> getSnapshots(String mediaPackageId, String orgId, String orderByVersion) {
    return db.execTx(em -> {
      String namedQueryName = "Snapshot.findByMpId";
      if ("ASC".equals(orderByVersion)) {
        namedQueryName = "Snapshot.findOldestVersionFirst";
      }
      if ("DESC".equals(orderByVersion)) {
        namedQueryName = "Snapshot.findLatestVersionFirst";
      }

      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          namedQueryName,
          SnapshotDto.class,
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public int deleteSnapshots(String mediaPackageId, String orgId) {
    return db.execTx(em -> {
      return namedQuery.delete(
          "Snapshot.delete",
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("organizationId", orgId)
      ).apply(em);
    });
  }

  public int deleteAllButLatestSnapshot(String mediaPackageId, String orgId) {
    return db.execTx(em -> {
      return namedQuery.delete(
          "Snapshot.deleteAllButLatest",
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("organizationId", orgId)
      ).apply(em);
    });
  }

  public List<Snapshot> getSnapshotsByMpIdAndVersion(String mediaPackageId, Long version, String orgId) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          "Snapshot.findByMpIdAndVersion",
          SnapshotDto.class,
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("version", version),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public List<Snapshot> getSnapshotsByDateOrderByMpId(Date start, Date end, String orgId) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          "Snapshot.findByDateOrderByMpId",
          SnapshotDto.class,
          Pair.of("startDate", start),
          Pair.of("endDate", end),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public List<Snapshot> getSnapshotsByMpdIdAndDate(String mediaPackageId, Date start, Date end, String orgId) {
    return getSnapshotsByMpdIdAndDate(mediaPackageId, start, end, orgId, null);
  }

  public List<Snapshot> getSnapshotsByMpdIdAndDate(String mediaPackageId, Date start, Date end, String orgId,
      String orderByVersion) {
    return db.execTx(em -> {
      String namedQueryName = "Snapshot.findByMpIdAndDate";
      if ("ASC".equals(orderByVersion)) {
        namedQueryName = "Snapshot.findByMpIdAndDateOldestVersionFirst";
      }
      if ("DESC".equals(orderByVersion)) {
        namedQueryName = "Snapshot.findByMpIdAndDateLatestVersionFirst";
      }

      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          namedQueryName,
          SnapshotDto.class,
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("startDate", start),
          Pair.of("endDate", end),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public List<Snapshot> getSnapshotsByNotStorageAndDate(String storageId, Date start, Date end, String orgId) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          "Snapshot.findByNotStorageAndDate",
          SnapshotDto.class,
          Pair.of("storageId", storageId),
          Pair.of("startDate", start),
          Pair.of("endDate", end),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public List<Snapshot> getSnapshotsBySeries(String seriesId, String orgId) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          "Snapshot.findLatestBySeriesId",
          SnapshotDto.class,
          Pair.of("seriesId", seriesId),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public List<Snapshot> getLatestSnapshotsByMediaPackageIds(Collection mediaPackageIds, String orgId) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findAll(
          "Snapshot.findLatestByMpIds",
          SnapshotDto.class,
          Pair.of("mediaPackageIds", mediaPackageIds),
          Pair.of("organizationId", orgId)
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  public Optional<Snapshot> getSnapshot(String mediaPackageId, String orgId, Long version) {
    return db.execTx(em -> {
      Optional<SnapshotDto> snapshotDto = namedQuery.findOpt(
          "Snapshot.findByMpIdOrgIdAndVersion",
          SnapshotDto.class,
          Pair.of("mediaPackageId", mediaPackageId),
          Pair.of("organizationId", orgId),
          Pair.of("version", version)
      ).apply(em);

      if (snapshotDto.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(snapshotDto.get().toSnapshot());
    });
  }

  public List<Snapshot> getSnapshotsForIndexRebuild(int offset, int limit) {
    return db.execTx(em -> {
      List<SnapshotDto> snapshotDto = namedQuery.findSome(
          "Snapshot.findForIndexRebuild",
          offset,
          limit,
          SnapshotDto.class
      ).apply(em);

      return snapshotDtoToSnapshot(snapshotDto);
    });
  }

  //
  // Utility
  //

  public static <A> A insidePersistenceContextCheck(A a) {
    if (a != null) {
      return a;
    } else {
      throw new RuntimeException(
          "Used DTO outside of a persistence context or the DTO has not been assigned an ID yet.");
    }
  }

  private List<Snapshot> snapshotDtoToSnapshot(List<SnapshotDto> snapshotDtos) {
    return snapshotDtos.stream()
        .map(s -> s.toSnapshot())
        .collect(Collectors.toList());
  }
}
