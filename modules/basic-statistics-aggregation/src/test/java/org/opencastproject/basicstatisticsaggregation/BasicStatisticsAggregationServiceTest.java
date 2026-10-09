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
package org.opencastproject.basicstatisticsaggregation;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseServiceImpl;
import org.opencastproject.basicstatisticsaggregation.persistence.BasicStatisticsAggregationDatabaseServiceImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Covers reaggregate function and getters.
 */
public class BasicStatisticsAggregationServiceTest {

  private static final String ORGANIZATION = DefaultOrganization.DEFAULT_ORGANIZATION_ID;
  private static final String ITEM_ID = "item-1";
  private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

  private BasicStatisticsDatabaseServiceImpl rawEventDatabase;
  private BasicStatisticsAggregationDatabaseServiceImpl aggregationDatabase;
  private BasicStatisticsAggregationService service;

  @Before
  public void setUp() {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    rawEventDatabase = new BasicStatisticsDatabaseServiceImpl();
    rawEventDatabase.setEntityManagerFactory(newEntityManagerFactory(BasicStatisticsDatabaseServiceImpl
        .PERSISTENCE_UNIT));
    rawEventDatabase.setDBSessionFactory(getDbSessionFactory());
    rawEventDatabase.activate(null);

    aggregationDatabase = new BasicStatisticsAggregationDatabaseServiceImpl();
    aggregationDatabase.setEntityManagerFactory(newEntityManagerFactory(BasicStatisticsAggregationDatabaseServiceImpl
        .PERSISTENCE_UNIT));
    aggregationDatabase.setDBSessionFactory(getDbSessionFactory());
    aggregationDatabase.activate(null);

    service = new BasicStatisticsAggregationService();
    service.setBasicStatisticsDatabaseService(rawEventDatabase);
    service.setBasicStatisticsAggregationDatabaseService(aggregationDatabase);
    service.setSecurityService(securityService);
    service.setDurationLookupForTesting(new DurationLookup(null, null, null));
  }

  @Test
  public void reaggregateTurnsRawActivityIntoAggregateAndTotal() throws Exception {
    Instant play = DAY.atStartOfDay(ZoneOffset.UTC).plusSeconds(10 * 3600).toInstant();
    rawEventDatabase.createRawEvents(List.of(
        rawEvent(EventType.VIDEO_PLAY, play, null),
        rawEvent(EventType.FETCH_FILE, play.plusSeconds(5), null),
        // 35s claimed, 15s after play: within the 2.5x rate cap (37s), so fully counted.
        rawEvent(EventType.VIDEO_WATCHED, play.plusSeconds(15), "{\"from\":0,\"to\":35000}")));

    int daysRecomputed = service.reaggregate(ITEM_ID, DAY, DAY);

    assertEquals(1, daysRecomputed);

    AggregatedEvent event = aggregationDatabase.getAggregatedEvent(ORGANIZATION, ItemType.VIDEO, ITEM_ID, DAY);
    assertEquals(1, event.getViews()[10]);
    assertEquals(35, event.getWatchtime()[10]);

    AggregatedTotal total = aggregationDatabase.getTotal(ORGANIZATION, ItemType.VIDEO, ITEM_ID);
    assertEquals(1, total.getTotalViews());
    assertEquals(35, total.getTotalWatchtime());
  }

  private static RawEvent rawEvent(EventType eventType, Instant timestamp, String payload) {
    return new RawEvent(UUID.randomUUID().toString(), ORGANIZATION, timestamp, "session-1", ItemType.VIDEO, ITEM_ID,
        eventType, payload);
  }
}
