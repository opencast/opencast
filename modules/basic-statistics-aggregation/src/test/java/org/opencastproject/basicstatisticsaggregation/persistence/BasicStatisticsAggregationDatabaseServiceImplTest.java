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
package org.opencastproject.basicstatisticsaggregation.persistence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatisticsaggregation.AggregatedEvent;
import org.opencastproject.basicstatisticsaggregation.AggregatedTotal;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

/**
 * Covers:
 * - a day's hourly buckets can be upserted and read back
 * - all-time total accumulates across multiple calls rather than being overwritten by them.
 */
public class BasicStatisticsAggregationDatabaseServiceImplTest {

  private static final String ORGANIZATION = "mh_default_org";
  private static final ItemType ITEM_TYPE = ItemType.VIDEO;
  private static final String ITEM_ID = "item-1";
  private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

  private BasicStatisticsAggregationDatabaseServiceImpl persistence;

  @Before
  public void setUp() {
    persistence = new BasicStatisticsAggregationDatabaseServiceImpl();
    persistence.setEntityManagerFactory(
        newEntityManagerFactory(BasicStatisticsAggregationDatabaseServiceImpl.PERSISTENCE_UNIT));
    persistence.setDBSessionFactory(getDbSessionFactory());
    persistence.activate(null);
  }

  @Test
  public void updateAggregatedEventUpsertsRatherThanDuplicating() throws Exception {
    int[] views = new int[24];
    views[10] = 2;
    long[] watchtime = new long[24];
    watchtime[10] = 90;

    AggregatedEvent firstWrite = new AggregatedEvent(ORGANIZATION, ITEM_TYPE, ITEM_ID, DAY);
    firstWrite.setViews(views);
    firstWrite.setWatchtime(watchtime);
    persistence.updateAggregatedEvent(firstWrite);

    AggregatedEvent stored = persistence.getAggregatedEvent(ORGANIZATION, ITEM_TYPE, ITEM_ID, DAY);
    assertArrayEquals(views, stored.getViews());
    assertArrayEquals(watchtime, stored.getWatchtime());

    // Re-aggregating the same day should update the existing row, not create a second one.
    int[] recomputedViews = new int[24];
    recomputedViews[10] = 5;
    AggregatedEvent secondWrite = new AggregatedEvent(ORGANIZATION, ITEM_TYPE, ITEM_ID, DAY);
    secondWrite.setViews(recomputedViews);
    secondWrite.setWatchtime(watchtime);
    persistence.updateAggregatedEvent(secondWrite);

    List<AggregatedEvent> all = persistence.getAggregatedEvents(ORGANIZATION, ITEM_TYPE, ITEM_ID, DAY,
        DAY);
    assertEquals(1, all.size());
    assertArrayEquals(recomputedViews, all.get(0).getViews());
  }

  @Test
  public void totalAccumulatesAcrossCalls() throws Exception {
    AggregatedTotal beforeAnyActivity = persistence.getTotal(ORGANIZATION, ITEM_TYPE, ITEM_ID);
    assertEquals(0, beforeAnyActivity.getTotalViews());

    persistence.addToTotal(ORGANIZATION, ITEM_TYPE, ITEM_ID, 3, 100);
    persistence.addToTotal(ORGANIZATION, ITEM_TYPE, ITEM_ID, 2, 50);

    AggregatedTotal total = persistence.getTotal(ORGANIZATION, ITEM_TYPE, ITEM_ID);
    assertEquals(5, total.getTotalViews());
    assertEquals(150, total.getTotalWatchtime());
  }
}
