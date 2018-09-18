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

import static java.lang.String.format;

import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.PartialMediaPackage;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos.Full;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos.Medium;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.util.persistencefn.PersistenceEnv;
import org.opencastproject.util.persistencefn.PersistenceUtil;
import org.opencastproject.util.persistencefn.PersistenceUtil.DatabaseVendor;
import org.opencastproject.util.persistencefn.Queries;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.JPQLTemplates;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.expr.BooleanExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Provider;
import javax.persistence.EntityManager;

/**
 * Data access object.
 */
@ParametersAreNonnullByDefault
public class Database implements EntityPaths {
  private static final Logger logger = LoggerFactory.getLogger(Database.class);

  public static final JPQLTemplates TEMPLATES = EclipseLinkTemplates.DEFAULT;

  private final PersistenceEnv penv;

  public Database(PersistenceEnv penv) {
    this.penv = penv;
  }

  /**
   * Run a Queryldsl query inside a persistence context/transaction.
   *
   * @param q the query function to run
   */
  public <A> A run(final Fn<JPAQueryFactory, A> q) {
    return penv.tx(new Fn<EntityManager, A>() {
      @Override public A apply(final EntityManager em) {
        return q.apply(new JPAQueryFactory(TEMPLATES, new Provider<EntityManager>() {
          @Override public EntityManager get() {
            return em;
          }
        }));
      }
    });
  }

  public <A> A runSql(final Sql<A> sql) {
    return penv.tx(new Fn<EntityManager, A>() {
      @Override public A apply(EntityManager em) {
        for (DatabaseMetaData md : PersistenceUtil.getDatabaseMetadata(em)) {
          final DatabaseVendor vendor = PersistenceUtil.getVendor(md);
          switch (vendor) {
            case H2:
              return sql.h2(em);
            case MYSQL:
              return sql.mysql(em);
            case POSTGRES:
              return sql.postgres(em);
            default:
              throw new UnsupportedOperationException("Unsupported database vendor " + vendor);
          }
        }
        logger.warn("Cannot determine database vendor, trying H2");
        return sql.h2(em);
      }
    });
  }

  public interface Sql<A> {
    A h2(EntityManager em);
    A mysql(EntityManager em);
    A postgres(EntityManager em);
  }

  public void logQuery(JPAQuery q) {
    logger.debug(format("\n---\nQUERY\n%s\n---", q.toString()));
  }

  public void logDelete(String queryName, JPADeleteClause q) {
    logger.debug(format("\n---\nDELETE %s\n%s\n---", queryName, q.toString()));
  }

  /**
   * Save a property to the database. This is either an insert or an update operation.
   */
  public boolean saveProperty(final Property property) {
    return penv.tx(new Fn<EntityManager, Boolean>() {
      @Override public Boolean apply(EntityManager em) {
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
          Queries.persistOrUpdate(em, exists == null
                  ? PropertyDto.mk(property)
                  : exists.update(property.getValue()));
          return true;
        } else {
          // media package does not exist
          return false;
        }
      }
    });
  }

  /**
   * Claim a new version for media package <code>mpId</code>.
   */
  public VersionImpl claimVersion(final String mpId) {
    return penv.tx(new Fn<EntityManager, VersionImpl>() {
      @Override public VersionImpl apply(final EntityManager em) {
        final Opt<VersionClaimDto> lastOpt = VersionClaimDto.findLast(em, mpId);
        if (lastOpt.isSome()) {
          final VersionImpl claim = VersionImpl.next(lastOpt.get().getLastClaimed());
          VersionClaimDto.update(em, mpId, claim.value());
          return claim;
        } else {
          final VersionImpl first = VersionImpl.FIRST;
          em.persist(VersionClaimDto.mk(mpId, first.value()));
          return first;
        }
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
    return penv.tx(new Fn<EntityManager, SnapshotDto>() {
      @Override public SnapshotDto apply(EntityManager em) {
        // persist snapshot
        em.persist(snapshotDto);
        // persist assets
        for (MediaPackageElement e : pmp.getElements()) {
          final AssetDto a = AssetDto.mk(
                  e.getIdentifier(),
                  snapshotDto.getId(),
                  e.getChecksum().toString(),
                  Opt.nul(e.getMimeType()),
                  storageId,
                  e.getSize());
          em.persist(a);
        }
        return snapshotDto;
      }
    });
  }

  public void setStorageLocation(Snapshot snapshot, final String storageId) {
    setStorageLocation(VersionImpl.mk(snapshot.getVersion()), snapshot.getMediaPackage().getIdentifier().compact(), storageId);
  }

  public void setStorageLocation(final VersionImpl version, final String mpId, final String storageId) {
    penv.tx(new Fx<EntityManager>() {
      @Override public void apply(EntityManager em) {
        final QSnapshotDto q = QSnapshotDto.snapshotDto;
        final QAssetDto a = QAssetDto.assetDto;
        //Update the snapshot
        new JPAUpdateClause(em, q, TEMPLATES)
                .where(q.version.eq(version.value()).and(q.mediaPackageId.eq(mpId)))
                .set(q.storageId, storageId)
                .execute();
        //Get the snapshot (to get its database ID)
        Opt<SnapshotDtos.Medium> s = getSnapshot(version, mpId);
        //Update the assets
        new JPAUpdateClause(em, a, TEMPLATES)
                .where(a.snapshotId.eq(s.get().getSnapshotDto().getId()))
                .set(a.storageId, storageId)
                .execute();
      }
    }.toFn());
  }

  public void setAssetStorageLocation(final VersionImpl version, final String mpId, final String mpeId,
          final String storageId) {
    penv.tx(new Fx<EntityManager>() {
      @Override
      public void apply(EntityManager em) {
        final QAssetDto a = QAssetDto.assetDto;
        Opt<SnapshotDtos.Medium> s = getSnapshot(version, mpId);
        // Update the asset store id
        new JPAUpdateClause(em, a, TEMPLATES)
                .where(a.snapshotId.eq(s.get().getSnapshotDto().getId()).and(a.mediaPackageElementId.eq(mpeId)))
                .set(a.storageId, storageId).execute();
      }
    }.toFn());
  }

  public void setAvailability(final VersionImpl version, final String mpId, final Availability availability) {
    penv.tx(new Fx<EntityManager>() {
      @Override public void apply(EntityManager em) {
        final QSnapshotDto q = QSnapshotDto.snapshotDto;
        new JPAUpdateClause(em, q, TEMPLATES)
        .where(q.version.eq(version.value()).and(q.mediaPackageId.eq(mpId)))
        .set(q.availability, availability.name())
        .execute();
      }
    }.toFn());
  }

  /**
   * Get an asset. If no version is specified return the latest version.
   *
   * @return the asset or none, if no asset can be found
   */
  public Opt<AssetDtos.Medium> getAsset(final VersionImpl version, final String mpId, final String mpeId) {
    return penv.tx(new Fn<EntityManager, Opt<AssetDtos.Medium>>() {
      @Override public Opt<AssetDtos.Medium> apply(EntityManager em) {
        final QAssetDto assetDto = QAssetDto.assetDto;
        final QSnapshotDto snapshotDto = QSnapshotDto.snapshotDto;
        final Tuple result = AssetDtos.baseJoin(em)
                .where(snapshotDto.mediaPackageId.eq(mpId)
                               .and(assetDto.mediaPackageElementId.eq(mpeId))
                               .and(snapshotDto.version.eq(version.value())))
                        // if no version has been specified make sure to get the latest by ordering
                .orderBy(snapshotDto.version.desc())
                .uniqueResult(Medium.select);
        return Opt.nul(result).map(AssetDtos.Medium.fromTuple);
      }
    });
  }

  public Opt<SnapshotDtos.Medium> getSnapshot(final VersionImpl version, final String mpId) {
    return penv.tx(new Fn<EntityManager, Opt<SnapshotDtos.Medium>>() {
      @Override public Opt<SnapshotDtos.Medium> apply(EntityManager em) {
        final QSnapshotDto snapshotDto = QSnapshotDto.snapshotDto;
        final Tuple result = SnapshotDtos.baseQuery(em)
                .where(snapshotDto.mediaPackageId.eq(mpId)
                  .and(snapshotDto.version.eq(version.value())))
                // if no version has been specified make sure to get the latest by ordering
                .orderBy(snapshotDto.version.desc())
                .uniqueResult(SnapshotDtos.Medium.select);
        return Opt.nul(result).map(SnapshotDtos.Medium.fromTuple);
      }
    });
  }

  public Opt<AssetDtos.Full> findAssetByChecksum(final String checksum) {
    return penv.tx(new Fn<EntityManager, Opt<AssetDtos.Full>>() {
      @Override public Opt<AssetDtos.Full> apply(EntityManager em) {
        final Tuple result = AssetDtos.baseJoin(em).where(QAssetDto.assetDto.checksum.eq(checksum)).singleResult(Full.select);
        return Opt.nul(result).map(Full.fromTuple);
      }
    });
  }

  public Opt<AssetDtos.Full> findAssetByChecksumAndStore(final String checksum, final String storeId) {
    return penv.tx(new Fn<EntityManager, Opt<AssetDtos.Full>>() {
      @Override
      public Opt<AssetDtos.Full> apply(EntityManager em) {
        final Tuple result = AssetDtos.baseJoin(em)
                .where(QAssetDto.assetDto.checksum.eq(checksum).and(QAssetDto.assetDto.storageId.eq(storeId)))
                .singleResult(Full.select);
        return Opt.nul(result).map(Full.fromTuple);
      }
    });
  }

  //
  // Utility
  //

  public static <A> A insidePersistenceContextCheck(A a) {
    if (a != null) {
      return a;
    } else {
      throw new RuntimeException("Used DTO outside of a persistence context or the DTO has not been assigned an ID yet.");
    }
  }
}
