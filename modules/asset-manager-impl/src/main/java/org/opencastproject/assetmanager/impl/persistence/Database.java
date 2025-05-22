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
import org.opencastproject.assetmanager.impl.persistence.AssetDtos.Full;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos.Medium;
import org.opencastproject.db.DBSession;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.util.data.Function;

import com.entwinemedia.fn.data.Opt;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.JPQLTemplates;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.expr.BooleanExpression;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Data access object.
 */
@ParametersAreNonnullByDefault
public class Database implements EntityPaths {
  private static final Logger logger = LoggerFactory.getLogger(Database.class);

  public static final JPQLTemplates TEMPLATES = EclipseLinkTemplates.DEFAULT;

  private final DBSession db;

  public Database(DBSession db) {
    this.db = db;
  }

  /**
   * Run a Queryldsl query inside a persistence context/transaction.
   *
   * @param q the query function to run
   */
  public <A> A run(final Function<JPAQueryFactory, A> q) {
    return db.execTx(em -> {
      return q.apply(new JPAQueryFactory(TEMPLATES, () -> em));
    });
  }

  public void logQuery(JPAQuery q) {
    logger.debug("\n---\nQUERY\n{}\n---", q);
  }

  public void logDelete(String queryName, JPADeleteClause q) {
    logger.debug("\n---\nDELETE {}\n{}\n---", queryName, q);
  }

  /**
   * Save a property to the database. This is either an insert or an update operation.
   */
  public boolean saveProperty(final Property property) {
    return db.execTx(em -> {
      final PropertyId pId = property.getId();
      // check the existence of both the media package and the property in one query
      //
      // either the property matches or it does not exist <- left outer join
      final BooleanExpression eitherMatchOrNull =
          Q_PROPERTY.namespace.eq(pId.getNamespace())
              .and(Q_PROPERTY.propertyName.eq(pId.getName())).or(Q_PROPERTY.namespace.isNull());
      final Tuple result = new JPAQuery(em, TEMPLATES)
          .from(Q_SNAPSHOT)
          .leftJoin(Q_PROPERTY).on(Q_SNAPSHOT.mediaPackageId.eq(Q_PROPERTY.mediaPackageId).and(eitherMatchOrNull))
          .where(Q_SNAPSHOT.mediaPackageId.eq(pId.getMediaPackageId()))
          // only one result is interesting, no need to fetch all versions of the media package
          .singleResult(Q_SNAPSHOT.id, Q_PROPERTY);
      if (result != null) {
        // media package exists, now check if the property exists
        final PropertyDto exists = result.get(Q_PROPERTY);
        namedQuery
            .persistOrUpdate(exists == null
                ? PropertyDto.mk(property)
                : exists.update(property.getValue()))
            .apply(em);
        return true;
      } else {
        // media package does not exist
        return false;
      }
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
      final QSnapshotDto q = QSnapshotDto.snapshotDto;
      final QAssetDto a = QAssetDto.assetDto;
      // Update the snapshot
      new JPAUpdateClause(em, q, TEMPLATES)
          .where(q.version.eq(version.value()).and(q.mediaPackageId.eq(mpId)))
          .set(q.storageId, storageId)
          .execute();
      // Get the snapshot (to get its database ID)
      Optional<SnapshotDtos.Medium> s = getSnapshot(version, mpId);
      // Update the assets
      new JPAUpdateClause(em, a, TEMPLATES)
          .where(a.snapshot.id.eq(s.get().getSnapshotDto().getId()))
          .set(a.storageId, storageId)
          .execute();
      return null;
    });
  }

  public void setAssetStorageLocation(final VersionImpl version, final String mpId, final String mpeId,
          final String storageId) {
    db.execTx(em -> {
      final QAssetDto a = QAssetDto.assetDto;
      Optional<SnapshotDtos.Medium> s = getSnapshot(version, mpId);
      // Update the asset store id
      new JPAUpdateClause(em, a, TEMPLATES)
          .where(a.snapshot.id.eq(s.get().getSnapshotDto().getId()).and(a.mediaPackageElementId.eq(mpeId)))
          .set(a.storageId, storageId).execute();
      return null;
    });
  }

  public void setAvailability(final VersionImpl version, final String mpId, final Availability availability) {
    db.execTx(em -> {
      final QSnapshotDto q = QSnapshotDto.snapshotDto;
      new JPAUpdateClause(em, q, TEMPLATES)
          .where(q.version.eq(version.value()).and(q.mediaPackageId.eq(mpId)))
          .set(q.availability, availability.name())
          .execute();
      return null;
    });
  }

  /**
   * Get an asset. If no version is specified return the latest version.
   *
   * @return the asset or none, if no asset can be found
   */
  public Optional<AssetDtos.Medium> getAsset(final VersionImpl version, final String mpId, final String mpeId) {
    return db.execTx(em -> {
      final QAssetDto assetDto = QAssetDto.assetDto;
      final Tuple result = AssetDtos.baseJoin(em)
          .where(assetDto.snapshot.mediaPackageId.eq(mpId)
              .and(assetDto.mediaPackageElementId.eq(mpeId))
              .and(assetDto.snapshot.version.eq(version.value())))
          // if no version has been specified make sure to get the latest by ordering
          .orderBy(assetDto.snapshot.version.desc())
          .uniqueResult(Medium.select);
      var dtoOpt = Opt.nul(result).map(AssetDtos.Medium.fromTuple);
      return dtoOpt.isSome() ? Optional.of(dtoOpt.get()) : Optional.empty();
    });
  }

  public Optional<SnapshotDtos.Medium> getSnapshot(final VersionImpl version, final String mpId) {
    return db.execTx(em -> {
      final QSnapshotDto snapshotDto = QSnapshotDto.snapshotDto;
      final Tuple result = SnapshotDtos.baseQuery(em)
          .where(snapshotDto.mediaPackageId.eq(mpId)
              .and(snapshotDto.version.eq(version.value())))
          // if no version has been specified make sure to get the latest by ordering
          .orderBy(snapshotDto.version.desc())
          .uniqueResult(SnapshotDtos.Medium.select);
      var dtoOpt = Opt.nul(result).map(SnapshotDtos.Medium.fromTuple);
      return dtoOpt.isSome() ? Optional.of(dtoOpt.get()) : Optional.empty();
    });
  }

  public Optional<AssetDtos.Full> findAssetByChecksum(final String checksum) {
    return db.execTx(em -> {
      final Tuple result = AssetDtos.baseJoin(em)
          .where(QAssetDto.assetDto.checksum.eq(checksum))
          .singleResult(Full.select);
      var dtoOpt = Opt.nul(result).map(Full.fromTuple);
      return dtoOpt.isSome() ? Optional.of(dtoOpt.get()) : Optional.empty();
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

  public Optional<AssetDtos.Full> findAssetByChecksumAndStoreAndOrg(final String checksum, final String storeId,
      final String orgId) {
    return db.execTx(em -> {
      final Tuple result = AssetDtos.baseJoin(em)
          .where(QAssetDto.assetDto.checksum.eq(checksum)
              .and(QAssetDto.assetDto.storageId.eq(storeId))
              .and(QAssetDto.assetDto.snapshot.organizationId.eq(orgId)))
          .singleResult(Full.select);
      var dtoOpt = Opt.nul(result).map(Full.fromTuple);
      return dtoOpt.isSome() ? Optional.of(dtoOpt.get()) : Optional.empty();
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
}
