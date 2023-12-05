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
package org.opencastproject.assetmanager.impl;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.Queries.nativeQuery;

import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBTestEnv;
import org.opencastproject.util.data.Function;

import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Predicate;

import javax.persistence.EntityManager;

public class AssetManagerDeleteTestBase extends AssetManagerTestBase implements EntityPaths {

  protected DBSession db;

  @Override
  public AssetManagerImpl makeAssetManager() throws Exception {
    db = DBTestEnv.newDBSession(PERSISTENCE_UNIT);
    // empty database
    db.execTx(em -> {
      nativeQuery.delete("delete from oc_assets_asset").apply(em);
      nativeQuery.delete("delete from oc_assets_properties").apply(em);
      nativeQuery.delete("delete from oc_assets_snapshot").apply(em);
      nativeQuery.delete("delete from oc_assets_version_claim").apply(em);
    });

    final Database database = new Database(db);
    am = super.makeAssetManager();
    am.setDatabase(database);
    return am;
  }

  long runCount(final JPAQuery q) {
    return db.execTx(em -> {
      return q.clone(em, Database.TEMPLATES).count();
    });
  }

  void assertPropertiesTotal(long count) {
    assertEquals(format("[SQL] There should be %d properties total", count),
                 count,
                 runCount(new JPAQuery().from(Q_PROPERTY)));
  }

  void assertAssetsTotal(long count) {
    assertEquals(format("[SQL] There should be %d assets total", count),
                 count,
                 runCount(new JPAQuery().from(Q_ASSET)));
  }

  void assertSnapshotsTotal(long count) {
    assertEquals("[AssetManager] There should be " + count + " snapshots total",
                 count,
                 q.select(q.snapshot()).run().getSize());
    assertEquals(format("[SQL] There should be %d snapshots total", count),
                 count,
                 runCount(new JPAQuery().from(Q_SNAPSHOT)));
  }

  void assertTotals(long snapshots, long assets, long properties) {
    assertSnapshotsTotal(snapshots);
    assertAssetsTotal(assets);
    assertPropertiesTotal(properties);
  }

  static Function<EntityManager, JPADeleteClause> deleteFrom(final EntityPath<?> from) {
    return new Function<EntityManager, JPADeleteClause>() {
      @Override public JPADeleteClause apply(EntityManager em) {
        return new JPADeleteClause(em, from, Database.TEMPLATES);
      }
    };
  }

  static final Function<JPADeleteClause, Long> execute = new Function<JPADeleteClause, Long>() {
    @Override public Long apply(JPADeleteClause delete) {
      return delete.execute();
    }
  };

  static Function<JPADeleteClause, JPADeleteClause> where(final Predicate... p) {
    return new Function<JPADeleteClause, JPADeleteClause>() {
      @Override public JPADeleteClause apply(JPADeleteClause delete) {
        return delete.where(p);
      }
    };
  }

  static java.util.function.Function<EntityManager, Long> deleteFromWhere(final EntityPath<?> from,
      final Predicate... where) {
    return em -> deleteFrom(from)
        .then(where(where))
        .then(execute)
        .apply(em);
  }

  long delete(final EntityPath<?> from, final Predicate... where) {
    return db.execTx(deleteFromWhere(from, where));
  }
}
