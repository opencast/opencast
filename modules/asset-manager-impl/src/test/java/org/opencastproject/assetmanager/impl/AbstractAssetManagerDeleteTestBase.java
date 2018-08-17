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
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.EntityPaths;

import com.entwinemedia.fn.Fn;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Predicate;

import javax.persistence.EntityManager;

public class AbstractAssetManagerDeleteTestBase extends AbstractAssetManagerTestBase implements EntityPaths {
  void assertPropertiesTotal(long count) {
//    assertEquals("[AssetManager] There should be " + count + " properties total",
//                 count,
//                 enrich(q.select(q.properties()).run()).countProperties());
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

  void assertPropertiesOfMediaPackage(long count, String mpId) {
    assertEquals(format("[AssetManager] There should be %d properties for episode %s", count, mpId),
                 count,
                 enrich(q.select(q.properties()).where(q.mediaPackageId(mpId)).run()).countProperties());
    assertEquals(format("[SQL] There should be %d properties for episode %s", count, mpId),
                 count,
                 runCount(new JPAQuery().from(Q_PROPERTY).where(Q_PROPERTY.mediaPackageId.eq(mpId))));
  }

  void assertTotals(long snapshots, long assets, long properties) {
    assertSnapshotsTotal(snapshots);
    assertAssetsTotal(assets);
    assertPropertiesTotal(properties);
  }

  static Fn<EntityManager, JPADeleteClause> deleteFrom(final EntityPath<?> from) {
    return new Fn<EntityManager, JPADeleteClause>() {
      @Override public JPADeleteClause apply(EntityManager em) {
        return new JPADeleteClause(em, from, Database.TEMPLATES);
      }
    };
  }

  static final Fn<JPADeleteClause, Long> execute = new Fn<JPADeleteClause, Long>() {
    @Override public Long apply(JPADeleteClause delete) {
      return delete.execute();
    }
  };

  static Fn<JPADeleteClause, JPADeleteClause> where(final Predicate... p) {
    return new Fn<JPADeleteClause, JPADeleteClause>() {
      @Override public JPADeleteClause apply(JPADeleteClause delete) {
        return delete.where(p);
      }
    };
  }

  static Fn<EntityManager, Long> deleteFromWhere(final EntityPath<?> from, final Predicate... where) {
    return new Fn<EntityManager, Long>() {
      @Override public Long apply(EntityManager em) {
        return deleteFrom(from).then(where(where)).then(execute).apply(em);
      }
    };
  }

  long delete(final EntityPath<?> from, final Predicate... where) {
    return penv.tx(deleteFromWhere(from, where));
  }
}
