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

package org.opencastproject.basicstatistics.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseServiceImpl.PERSISTENCE_UNIT;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatistics.StatisticsVersion;
import org.opencastproject.util.requests.SortCriterion;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Tests how events and versions are stored, with a real database (H2) and the real JPA mapping.
 */
public class BasicStatisticsDatabaseServiceTest {

  private static final SortCriterion BY_ID = new SortCriterion("id", SortCriterion.Order.Ascending);

  private EntityManagerFactory emf;
  private BasicStatisticsDatabaseServiceImpl service;

  @Before
  public void setUp() {
    emf = newEntityManagerFactory(PERSISTENCE_UNIT);
    service = new BasicStatisticsDatabaseServiceImpl();
    service.setEntityManagerFactory(emf);
    service.setDBSessionFactory(getDbSessionFactory());
    service.activate(null);
  }

  @After
  public void tearDown() {
    emf.close();
  }

  private static RawEvent event(String organization, Instant timestamp, long session, ItemType itemType,
      EventType eventType, String payload) {
    return new RawEvent(organization, timestamp, session, itemType, "item", eventType, payload);
  }

  private static RawEvent event(Instant timestamp) {
    return event("org", timestamp, 1L, ItemType.VIDEO, EventType.VIDEO_PLAY, null);
  }

  /**
   * Run a SQL query directly on the database. Only for checking how things are physically stored, which cannot be seen
   * through the service: reading an event back works the same whether a type is stored as a name, an ordinal or an id.
   */
  private List<?> query(String sql) {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNativeQuery(sql).getResultList();
    } finally {
      em.close();
    }
  }

  private void deleteVersions() {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      em.createQuery("DELETE FROM StatisticsVersion v").executeUpdate();
      em.getTransaction().commit();
    } finally {
      em.close();
    }
  }

  private void insertVersion(long revision, int version, String activatedAt) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      em.persist(new StatisticsVersion(revision, version, Instant.parse(activatedAt)));
      em.getTransaction().commit();
    } finally {
      em.close();
    }
  }

  /** @return the recorded versions, as "revision:version", in order */
  private List<String> versions() {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createQuery("SELECT v FROM StatisticsVersion v ORDER BY v.revision", StatisticsVersion.class)
          .getResultList().stream()
          .map(version -> version.getRevision() + ":v" + version.getVersion())
          .collect(Collectors.toList());
    } finally {
      em.close();
    }
  }

  @Test
  public void testTypesAreStoredAsStableIds() throws Exception {
    service.createRawEvents(List.of(event("org", Instant.now(), 1L, ItemType.PLAYLIST, EventType.FETCH_FILE,
        "{\"elem\":\"a\",\"from\":0}")));

    // not as ordinals, which would change if the enums were reordered, and not as names
    Object[] row = (Object[]) query("SELECT item_type, event_type FROM oc_basic_statistics_raw_event").get(0);
    assertEquals(3, ((Number) row[0]).intValue());
    assertEquals(7, ((Number) row[1]).intValue());
    RawEvent read = service.getRawEvents("org", 10, 0, BY_ID).get(0);
    assertEquals(ItemType.PLAYLIST, read.getItemType());
    assertEquals(EventType.FETCH_FILE, read.getEventType());
  }

  @Test
  public void testTimestampsAreStoredAsTimestampsInUtc() throws Exception {
    assertEquals("TIMESTAMP", query("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE "
        + "TABLE_NAME = 'OC_BASIC_STATISTICS_RAW_EVENT' AND COLUMN_NAME = 'TIMESTAMP'").get(0));
    assertEquals("TIMESTAMP", query("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE "
        + "TABLE_NAME = 'OC_BASIC_STATISTICS_VERSION' AND COLUMN_NAME = 'ACTIVATED_AT'").get(0));

    // The two instants in October are an hour apart, but have the same local time in Germany (daylight saving time
    // ends), which makes them collapse into one if the time zone of the JVM is used for storing them. The tests run
    // in that time zone, see the argLine in the pom.xml, as H2 does not notice a change of the default time zone.
    Instant first = Instant.parse("2026-10-25T00:30:00Z");
    Instant second = Instant.parse("2026-10-25T01:30:00Z");
    assumeTrue("The JVM must run in a time zone like Europe/Berlin for this test, e.g. -Duser.timezone",
        LocalDateTime.ofInstant(first, ZoneId.systemDefault()).equals(
            LocalDateTime.ofInstant(second, ZoneId.systemDefault())));
    List<Instant> timestamps = List.of(
        Instant.parse("2025-01-01T00:00:00Z"),
        Instant.parse("2026-06-01T11:59:59.999Z"),
        Instant.parse("2026-06-01T12:00:00.500Z"),
        first,
        second,
        Instant.parse("2040-01-01T00:00:00Z"));

    service.createRawEvents(timestamps.stream().map(BasicStatisticsDatabaseServiceTest::event)
        .collect(Collectors.toList()));

    List<Instant> read = service.getRawEvents("org", 100, 0,
        new SortCriterion("timestamp", SortCriterion.Order.Ascending)).stream()
        .map(RawEvent::getTimestamp).collect(Collectors.toList());
    assertEquals(timestamps, read);
  }

  @Test
  public void testVersionIsRecordedOnce() throws Exception {
    assertEquals(List.of("1:v1"), versions());

    service.ensureVersionRecorded();
    service.ensureVersionRecorded();

    assertEquals(List.of("1:v1"), versions());
  }

  @Test
  public void testNodesStartingTogetherRecordVersionOnce() throws Exception {
    int nodes = 8;
    for (int run = 0; run < 3; run++) {
      deleteVersions();
      CountDownLatch go = new CountDownLatch(1);
      ExecutorService pool = Executors.newFixedThreadPool(nodes);
      try {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
          futures.add(pool.submit(() -> {
            go.await();
            service.ensureVersionRecorded();
            return null;
          }));
        }
        go.countDown();
        for (Future<?> future : futures) {
          future.get();
        }
      } finally {
        pool.shutdown();
      }

      assertEquals("run " + run, List.of("1:v1"), versions());
    }
  }

  @Test
  public void testDowngradeIsRecordedAgain() throws Exception {
    deleteVersions();
    // a later version has been active in between
    insertVersion(1, 1, "2026-01-01T00:00:00Z");
    insertVersion(2, 2, "2026-03-01T00:00:00Z");

    service.ensureVersionRecorded();

    assertEquals(List.of("1:v1", "2:v2", "3:v1"), versions());
    // the first activation of version 1 counts
    assertEquals(Instant.parse("2026-01-01T00:00:00Z"), service.getVersion1Timestamp());
    // and nothing changes if the same version is started again
    service.ensureVersionRecorded();
    assertEquals(List.of("1:v1", "2:v2", "3:v1"), versions());
  }
}
