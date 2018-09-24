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
package org.opencastproject.assetmanager.impl.query;

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;

import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.persistence.Conversions;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.assetmanager.impl.persistence.QPropertyDto;
import org.opencastproject.assetmanager.impl.persistence.QSnapshotDto;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.util.persistencefn.Queries;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.SetB;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

public abstract class AbstractADeleteQuery implements ADeleteQuery, DeleteQueryContributor, EntityPaths {
  private static final Logger logger = LoggerFactory.getLogger(AbstractADeleteQuery.class);

  private AbstractAssetManager am;
  private String owner;

  public AbstractADeleteQuery(AbstractAssetManager am, String owner) {
    this.am = am;
    this.owner = owner;
  }

  @Override public ADeleteQuery name(final String queryName) {
    return new AbstractADeleteQuery(am, owner) {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final DeleteQueryContribution cParent = AbstractADeleteQuery.this.contributeDelete(owner);
        return DeleteQueryContribution.mk(cParent).name(queryName);
      }
    };
  }

  @Override public ADeleteQuery where(final Predicate predicate) {
    return new AbstractADeleteQuery(am, owner) {
      @Override public DeleteQueryContribution contributeDelete(String owner) {
        final DeleteQueryContribution cParent = AbstractADeleteQuery.this.contributeDelete(owner);
        final DeleteQueryContribution cPredicate = RuntimeTypes.convert(predicate).contributeDelete(owner);
        return DeleteQueryContribution.mk()
                .from(cParent.from.append(cPredicate.from))
                .targetPredicate(cParent.targetPredicate)
                .where(JpaFns.allOf(cParent.where, cPredicate.where));
      }

      @Override public String toString() {
        return "where " + predicate;
      }
    };
  }

  public long run(DeleteSnapshotHandler deleteSnapshotHandler) {
    // run query and map the result to records
    final long startTime = System.nanoTime();
    // resolve AST
    final DeleteQueryContribution c = contributeDelete(owner);
    // run all queries in a single transaction
    final DeletionResult deletion = am.getDb().run(new Fn<JPAQueryFactory, DeletionResult>() {
      @Override public DeletionResult apply(final JPAQueryFactory jpa) {
        return runQueries(jpa, c);
      }
    });
    logger.debug("Pure query ms " + (System.nanoTime() - startTime) / 1000000);
    // delete from store
    for (Tuple t : deletion.deletedSnapshots) {
      // all three t.get(..) calls won't return null since the database fields are not null.
      final String orgId = t.get(Q_SNAPSHOT.organizationId);
      final String mpId = t.get(Q_SNAPSHOT.mediaPackageId);
      final VersionImpl version = Conversions.toVersion(t.get(Q_SNAPSHOT.version));
      am.getLocalAssetStore().delete(DeletionSelector.delete(orgId, mpId, version));
      deleteSnapshotHandler.notifyDeleteSnapshot(mpId, version);
    }
    for (String mpId : deletion.deletedEpisodes) {
      deleteSnapshotHandler.notifyDeleteEpisode(mpId);
    }
    final long searchTime = (System.nanoTime() - startTime) / 1000000;
    logger.debug("Complete query ms " + searchTime);
    return deletion.deletedItemsCount;
  }

  /** Run this in a transaction. */
  private DeletionResult runQueries(JPAQueryFactory jpa, DeleteQueryContribution c) {
    // # create Querydsl delete clause
    // # from
    // put into a set to remove duplicates
    final EntityPath<?> from;
    {
      final Set<EntityPath<?>> f = c.from.toSet(SetB.MH);
      if (f.size() == 1) {
        from = $(f).head2();
      } else {
        throw new RuntimeException("Only one entity is allowed in the from clause");
      }
    }
    //
    if (from instanceof QSnapshotDto) {
      // from Snapshot
      //
      final BooleanExpression where = Expressions.allOf(
              c.targetPredicate.orNull(),
              c.where.apply(Q_SNAPSHOT));
      // get snapshots to delete
      // TODO ATTENTION: this query has the potential to yield a massive amount of elements
      // return the list of snapshots to delete them outside the transaction since
      // it may take a while.
      final List<Tuple> deletedSnapshots = jpa.query()
              .from(Q_SNAPSHOT)
              .where(where)
              .list(Q_SNAPSHOT.organizationId, Q_SNAPSHOT.mediaPackageId, Q_SNAPSHOT.version);
// <BLOCK>
// TODO database only approach to determine deleted episodes
// TODO does not run with H2 so unit tests break
      /*
SELECT
  e.mediapackage_id,
  count(*) AS v
FROM oc_assets_snapshot e
GROUP BY e.mediapackage_id
HAVING v = (SELECT count(*)
            FROM oc_assets_snapshot e2
            WHERE e.mediapackage_id = e2.mediapackage_id
                  AND
                  -- delete where clause
                  (e2.version = 2 OR e2.mediapackage_id = '24ec925e-ea57-43a5-a7bb-58dc5aae54dd')
            GROUP BY mediapackage_id);
       */
//      final QSnapshotDto e2 = new QSnapshotDto("eee");
//      final List<String> deletedSnapshots = jpa.query()
//              .from(e2)
//              .groupBy(e2.mediaPackageId)
//              .having(e2.count().eq(
//                      jpa.subQuery()
//                              .from(Q_SNAPSHOT)
//                              .where(Q_SNAPSHOT.mediaPackageId.eq(e2.mediaPackageId).and(where))
//                              .groupBy(Q_SNAPSHOT.mediaPackageId)
//                              .count()))
//              .list(e2.mediaPackageId);
// </BLOCK>
      // delete assets from database
      final JPADeleteClause qAssets = jpa
              .delete(Q_ASSET)
              .where(Q_ASSET.snapshotId.in(
                      new JPASubQuery().from(Q_SNAPSHOT).where(where).list(Q_SNAPSHOT.id)));
      am.getDb().logDelete(formatQueryName(c.name, "delete assets"), qAssets);
      qAssets.execute();
      // main delete query
      final JPADeleteClause qMain = jpa.delete(Q_SNAPSHOT).where(where);
      am.getDb().logDelete(formatQueryName(c.name, "main"), qMain);
      final long deletedItems = qMain.execute();
      // delete orphaned properties
      deleteOrphanedProperties();
      // <BLOCK>
      // TODO Bad solution. Yields all media package IDs which can easily be thousands
      // TODO The above SQL solution does not work with H2 so I suspect the query is not 100% clean
      // TODO Rework the query and replace this code.
      // calculate deleted episodes, i.e. where all snapshots have been deleted
      final Set<String> deletedEpisodes;
      {
        final List<String> remainingSnapshots = jpa.query()
                .from(Q_SNAPSHOT)
                .distinct()
                .list(Q_SNAPSHOT.mediaPackageId);
        final Set<String> d = $(deletedSnapshots).map(new Fn<Tuple, String>() {
          @Override public String apply(Tuple tuple) {
            return tuple.get(Q_SNAPSHOT.mediaPackageId);
          }
        }).toSet(SetB.MH);
        d.removeAll(remainingSnapshots);
        deletedEpisodes = Collections.unmodifiableSet(d);
      }
      // </BLOCK>
      return new DeletionResult(deletedItems, deletedSnapshots, deletedEpisodes);
    } else if (from instanceof QPropertyDto) {
      // from Property
      //
      final BooleanExpression where;
      {
        final BooleanExpression w = c.where.apply(Q_PROPERTY);
        if (w != null) {
          /* The original sub query used an "ON" clause to filter the join by mediapackage id [1].
             Unfortunately Eclipse link drops this clause completely when transforming the query
             into SQL. It creates a cross join instead of the inner join, which is perfectly legal
             if the "ON" clause would be moved to the "WHERE" clause.
             The example [2] shows that neither an "ON" clause nor an additional "WHERE" predicate is generated.

             [1]
             new JPASubQuery()
                .from(Q_PROPERTY)
                .join(Q_SNAPSHOT) <- inner join
                .on(Q_PROPERTY.mediaPackageId.eq(Q_SNAPSHOT.mediaPackageId)) <- dropped by Eclipse link
                .where(Q_PROPERTY.mediaPackageId.eq(Q_SNAPSHOT.mediaPackageId).and(w))
                .distinct()
                .list(Q_PROPERTY.mediaPackageId)

             [2]
             SELECT DISTINCT t1.mediapackage_id FROM oc_assets_snapshot t2, oc_assets_properties t1 WHERE (t2.organization_id = ?)
           */
          where = Q_PROPERTY.mediaPackageId.in(
                  new JPASubQuery()
                          .from(Q_PROPERTY)
                          .join(Q_SNAPSHOT)
                          // move the join condition from the "ON" clause (mediapackage_id) to the where clause. Find an explanation above. */
                          .where(Q_PROPERTY.mediaPackageId.eq(Q_SNAPSHOT.mediaPackageId).and(w))
                          .distinct()
                          .list(Q_PROPERTY.mediaPackageId));
        } else {
          where = null;
        }
      }
      final JPADeleteClause qProperties = jpa.delete(from).where(Expressions.allOf(c.targetPredicate.orNull(), where));
      am.getDb().logDelete(formatQueryName(c.name, "main"), qProperties);
      final long deletedItems = qProperties.execute();
      return new DeletionResult(deletedItems, Collections.<Tuple>emptyList(), Collections.<String>emptySet());
    } else {
      // from contains an unsupported entity
      throw new RuntimeException("[Bug]");
    }
  }

  @Override public long run() {
    return run(NOP_DELETE_SNAPSHOT_HANDLER);
  }

  /**
   * Delete all orphaned properties. Orphaned properties refer to a non-existing media package.
   */
  private void deleteOrphanedProperties() {
    logger.debug("\n---\nDELETE [orphaned properties]\n---");
    am.getDb().runSql(new Database.Sql<Unit>() {
      @Override public Unit h2(EntityManager em) {
        Queries.sql.update(em, "DELETE FROM oc_assets_properties\n"
                + "WHERE id in (\n"
                + "  SELECT p.id\n"
                + "  FROM oc_assets_properties p LEFT JOIN oc_assets_snapshot e ON p.mediapackage_id = e.mediapackage_id\n"
                + "  WHERE e.id IS NULL);");
        return Unit.unit;
      }

      @Override public Unit mysql(EntityManager em) {
        Queries.sql.update(em, "DELETE p FROM\n"
                + "oc_assets_properties p LEFT JOIN oc_assets_snapshot e ON p.mediapackage_id = e.mediapackage_id\n"
                + "WHERE e.id IS NULL;");
        return Unit.unit;
      }

      @Override public Unit postgres(EntityManager em) {
        Queries.sql.update(em, "DELETE p FROM\n"
                + "oc_assets_properties p LEFT JOIN oc_assets_snapshot e ON p.mediapackage_id = e.mediapackage_id\n"
                + "WHERE e.id IS NULL;");
        return Unit.unit;
      }
    });
  }

  private static String formatQueryName(String name, String subQueryName) {
    return format("[%s] [%s]", name, subQueryName);
  }

  /**
   * Call {@link #run(DeleteSnapshotHandler)} with a deletion handler to get notified about deletions.
   */
  public interface DeleteSnapshotHandler {
    void notifyDeleteSnapshot(String mpId, VersionImpl version);

    void notifyDeleteEpisode(String mpId);
  }

  public static final DeleteSnapshotHandler NOP_DELETE_SNAPSHOT_HANDLER = new DeleteSnapshotHandler() {
    @Override public void notifyDeleteSnapshot(String mpId, VersionImpl version) {
    }

    @Override public void notifyDeleteEpisode(String mpId) {
    }
  };

  public final class DeletionResult {
    // CHECKSTYLE:OFF
    public final long deletedItemsCount;
    public final List<Tuple> deletedSnapshots;
    public final Set<String> deletedEpisodes;
    // CHECKSTYLE:ON

    public DeletionResult(
            long deletedItemsCount, List<Tuple> deletedSnapshots, Set<String> deletedEpisodes) {
      this.deletedItemsCount = deletedItemsCount;
      this.deletedSnapshots = deletedSnapshots;
      this.deletedEpisodes = deletedEpisodes;
    }
  }
}
