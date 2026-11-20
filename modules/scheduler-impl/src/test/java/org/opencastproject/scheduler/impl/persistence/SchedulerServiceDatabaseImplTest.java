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

package org.opencastproject.scheduler.impl.persistence;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import com.google.gson.Gson;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests persistent storage.
 */
public class SchedulerServiceDatabaseImplTest {

  private SchedulerServiceDatabaseImpl schedulerDatabase;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase.setEntityManagerFactory(newEntityManagerFactory(SchedulerServiceDatabaseImpl.PERSISTENCE_UNIT));
    schedulerDatabase.setDBSessionFactory(getDbSessionFactory());
    schedulerDatabase.setSecurityService(securityService);
    schedulerDatabase.activate(null);
  }

  @Test
  public void testLastModifed() throws Exception {
    Date now = new Date();
    String agentId = "agent1";

    try {
      schedulerDatabase.getLastModified(agentId);
      Assertions.fail();
    } catch (NotFoundException e) {
      Assertions.assertNotNull(e);
    }

    Assertions.assertTrue(schedulerDatabase.getLastModifiedDates().isEmpty());

    schedulerDatabase.touchLastEntry(agentId);

    Date lastModified = schedulerDatabase.getLastModified(agentId);
    Assertions.assertTrue(lastModified.after(now));

    Map<String, Date> dates = schedulerDatabase.getLastModifiedDates();
    Assertions.assertEquals(1, dates.size());
    lastModified = dates.get(agentId);
    Assertions.assertTrue(lastModified.after(now));
  }

  @Test
  public void testCreateEvent() throws Exception {
    final String mpId = "mpid";
    final String orgId = new DefaultOrganization().getId();
    final String agentId = "agent1";
    final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
    final Date end = new Date(1570953300000L); // 2019-10-13T07:55:00Z
    final String source = "source";
    final String recordingState = "recordingState";
    final Long lastHeard = new Date().getTime();
    final String presenters = "Werner";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    final Optional<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertFalse(initialEvent.isPresent());

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Optional.of(agentId),
        Optional.of(start),
        Optional.of(end),
        Optional.of(source),
        Optional.of(recordingState),
        Optional.of(lastHeard),
        Optional.of(presenters),
        Optional.of(lastModifiedDate),
        Optional.of(checksum),
        Optional.of(wfProperties),
        Optional.of(caProperties)
    );
    final Optional<ExtendedEventDto> newEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertTrue(newEvent.isPresent());
    Assertions.assertEquals(mpId, newEvent.get().getMediaPackageId());
    Assertions.assertEquals(orgId, newEvent.get().getOrganization());
    Assertions.assertEquals(agentId, newEvent.get().getCaptureAgentId());
    Assertions.assertEquals(start, newEvent.get().getStartDate());
    Assertions.assertEquals(end, newEvent.get().getEndDate());
    Assertions.assertEquals(source, newEvent.get().getSource());
    Assertions.assertEquals(recordingState, newEvent.get().getRecordingState());
    Assertions.assertEquals(lastHeard, newEvent.get().getRecordingLastHeard());
    Assertions.assertEquals(presenters, newEvent.get().getPresenters());
    Assertions.assertEquals(lastModifiedDate, newEvent.get().getLastModifiedDate());
    Assertions.assertEquals(checksum, newEvent.get().getChecksum());
    Assertions.assertEquals(new Gson().toJson(wfProperties), newEvent.get().getWorkflowProperties());
    Assertions.assertEquals(new Gson().toJson(caProperties), newEvent.get().getCaptureAgentProperties());
  }

  @Test
  public void testUpdateEvent() throws Exception {
    final String mpId = "mpid";
    final String orgId = new DefaultOrganization().getId();
    final String agentId = "agent1";
    final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
    final Date end = new Date(1570953300000L); // 2019-10-13T07:55:00Z
    final String source = "source";
    final String recordingState = "recordingState";
    final Long lastHeard = new Date().getTime();
    final String presenters = "Werner";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    final Optional<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertFalse(initialEvent.isPresent());

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Optional.of(agentId),
        Optional.of(start),
        Optional.of(end),
        Optional.of(source),
        Optional.of(recordingState),
        Optional.of(lastHeard),
        Optional.of(presenters),
        Optional.of(lastModifiedDate),
        Optional.of(checksum),
        Optional.of(wfProperties),
        Optional.of(caProperties)
    );
    final String updatedRecordingState = "updatedRecordingState";
    final Map<String, String> updatedCaProperties = new HashMap<String, String>() {
      {
        put("cafoo", "cabar");
        put("cabazupdated", "cabazzupdated");
      }
    };
    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(updatedRecordingState),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(updatedCaProperties)
    );
    final Optional<ExtendedEventDto> updatedEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertTrue(updatedEvent.isPresent());
    Assertions.assertEquals(mpId, updatedEvent.get().getMediaPackageId());
    Assertions.assertEquals(orgId, updatedEvent.get().getOrganization());
    Assertions.assertEquals(agentId, updatedEvent.get().getCaptureAgentId());
    Assertions.assertEquals(start, updatedEvent.get().getStartDate());
    Assertions.assertEquals(end, updatedEvent.get().getEndDate());
    Assertions.assertEquals(source, updatedEvent.get().getSource());
    Assertions.assertEquals(updatedRecordingState, updatedEvent.get().getRecordingState());
    Assertions.assertEquals(lastHeard, updatedEvent.get().getRecordingLastHeard());
    Assertions.assertEquals(presenters, updatedEvent.get().getPresenters());
    Assertions.assertEquals(lastModifiedDate, updatedEvent.get().getLastModifiedDate());
    Assertions.assertEquals(checksum, updatedEvent.get().getChecksum());
    Assertions.assertEquals(new Gson().toJson(wfProperties), updatedEvent.get().getWorkflowProperties());
    Assertions.assertEquals(new Gson().toJson(updatedCaProperties), updatedEvent.get().getCaptureAgentProperties());
  }

  @Test
  public void testGetEvents() throws Exception {
    // We create 4 events:
    // - Event no. 1 ends way before the interval we query for                -> no hit
    // - Event no. 2 starts before the interval and ends within the interval  -> hit
    // - Event no. 3 starts and ends within the interval                      -> hit
    // - Event no. 4 starts exactly when the interval ends                    -> hit because separationMillis > 0

    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date intervalStart = new Date(nowMillis);
    final Date intervalEnd = new Date(nowMillis + 5 * oneHourMillis);
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(intervalEnd.getTime()),
    };
    for (int i = 0; i < 4; i++) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentId),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    final int separationMillis = 60 * 1000;
    final List<String> eventIds = schedulerDatabase.getEvents(agentId, intervalStart, intervalEnd, separationMillis);
    Assertions.assertEquals(3, eventIds.size());
    Assertions.assertFalse(eventIds.contains(mpId + 0));
    Assertions.assertTrue(eventIds.contains(mpId + 1));
    Assertions.assertTrue(eventIds.contains(mpId + 2));
    Assertions.assertTrue(eventIds.contains(mpId + 3));
  }

  @Test
  public void testSearch() throws Exception {
    // We create 4 events, each with a duration of 2 hours:
    // - Event no. 1 ends way before now
    // - Event no. 2 starts before the now and ends after now
    // - Event no. 3 starts when no. 2 ends
    // - Event no. 4 starts two hours after no. 3 ends
    // So we have:  [no.1][free][no.2][no.3][free][no.4]

    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(nowMillis + 5 * oneHourMillis),
    };
    final List<Integer> indeces = Arrays.asList(0, 1, 2, 3);
    Collections.shuffle(indeces); // we want to insert in random order to test if method under test sorts correctly.
    for (int i : indeces) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentId),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    // All events which start from now. expected: no.3 and no.4
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events1.size());
    Assertions.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(events1.get(1).getMediaPackageId().equals(mpId + 3));

    // All events which start to now. expected: no.1 and no.2
    final List<ExtendedEventDto> events2 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events2.size());
    Assertions.assertTrue(events2.get(0).getMediaPackageId().equals(mpId + 0));
    Assertions.assertTrue(events2.get(1).getMediaPackageId().equals(mpId + 1));

    // All events which end from now. expected: no.2, no.3, and no.4
    final List<ExtendedEventDto> events3 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(3, events3.size());
    Assertions.assertTrue(events3.get(0).getMediaPackageId().equals(mpId + 1));
    Assertions.assertTrue(events3.get(1).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(events3.get(2).getMediaPackageId().equals(mpId + 3));

    // All events which end to now. expected: no.1,
    final List<ExtendedEventDto> events4 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty()
    );
    Assertions.assertEquals(1, events4.size());
    Assertions.assertTrue(events4.get(0).getMediaPackageId().equals(mpId + 0));

    // All events which start from now AND start to now -> illegal combination, no results expected
    Assertions.assertTrue(schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    ).isEmpty());

    // All events which end from now AND end to now -> illegal combination, no results expected
    Assertions.assertTrue(schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.of(new Date(nowMillis)),
        Optional.empty()
    ).isEmpty());

    // All events which start from now AND end to now -> illegal combination, no results expected
    Assertions.assertTrue(schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty()
    ).isEmpty());

    // All events which start from now + 1 hour. expected: no. 3 and no. 4
    final List<ExtendedEventDto> events5 = schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis + oneHourMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events5.size());
    Assertions.assertTrue(events5.get(0).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(events5.get(1).getMediaPackageId().equals(mpId + 3));

    // All events which start to now + 1 hour. expected: no. 1 and no. 2, but not no. 3
    final List<ExtendedEventDto> events6 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis + oneHourMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events6.size());
    Assertions.assertTrue(events6.get(0).getMediaPackageId().equals(mpId + 0));
    Assertions.assertTrue(events6.get(1).getMediaPackageId().equals(mpId + 1));

    // All events which end from now + 1 hour. expected: no2., no.3, and no.4
    final List<ExtendedEventDto> events7 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis + oneHourMillis)),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(3, events7.size());
    Assertions.assertTrue(events7.get(0).getMediaPackageId().equals(mpId + 1));
    Assertions.assertTrue(events7.get(1).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(events7.get(2).getMediaPackageId().equals(mpId + 3));

    // All events which end to now + 1 hour. expected: no.1
    final List<ExtendedEventDto> events8 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis + oneHourMillis)),
        Optional.empty()
    );
    Assertions.assertEquals(1, events8.size());
    Assertions.assertTrue(events8.get(0).getMediaPackageId().equals(mpId + 0));

    // No start/end dates given. expected: all events are returned
    final List<ExtendedEventDto> events9 = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(4, events9.size());
    Assertions.assertTrue(events9.get(0).getMediaPackageId().equals(mpId + 0));
    Assertions.assertTrue(events9.get(1).getMediaPackageId().equals(mpId + 1));
    Assertions.assertTrue(events9.get(2).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(events9.get(3).getMediaPackageId().equals(mpId + 3));

    // All events which start to now and end to now. expected: no. 1
    final List<ExtendedEventDto> eventsA = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty()
    );
    Assertions.assertEquals(1, eventsA.size());
    Assertions.assertTrue(eventsA.get(0).getMediaPackageId().equals(mpId + 0));

    // All events which start to now and end from now. expected: no. 2
    final List<ExtendedEventDto> eventsB = schedulerDatabase.search(
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(1, eventsB.size());
    Assertions.assertTrue(eventsB.get(0).getMediaPackageId().equals(mpId + 1));

    // All events which start from now and end from now. expected: no. 3 and no. 4
    final List<ExtendedEventDto> eventsC = schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, eventsC.size());
    Assertions.assertTrue(eventsC.get(0).getMediaPackageId().equals(mpId + 2));
    Assertions.assertTrue(eventsC.get(1).getMediaPackageId().equals(mpId + 3));
  }

  @Test
  public void testSearchLimit() throws Exception {
    // We create 4 events, each with a duration of 2 hours:
    // - Event no. 1 ends way before now
    // - Event no. 2 starts before the now and ends after now
    // - Event no. 3 starts when no. 2 ends
    // - Event no. 4 starts two hours after no. 3 ends
    // So we have:  [no.1][free][no.2][no.3][free][no.4]

    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(nowMillis + 5 * oneHourMillis),
    };
    final List<Integer> indeces = Arrays.asList(0, 1, 2, 3);
    Collections.shuffle(indeces); // we want to insert in random order to test if method under test sorts correctly.
    for (int i : indeces) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentId),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    // All events which start from now. expected: no.3 and no.4, but limit is 1, so just no. 3
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Optional.empty(),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(1)
    );
    Assertions.assertEquals(1, events1.size());
    Assertions.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));
  }

  @Test
  public void testSearchCaptureAgent() throws Exception {
    // We create 4 events, each with a duration of 2 hours:
    // - Event no. 1 ends way before now                       <- agent 1
    // - Event no. 2 starts before the now and ends after now  <- agent 2
    // - Event no. 3 starts when no. 2 ends                    <- agent 1
    // - Event no. 4 starts two hours after no. 3 ends         <- agent 2
    // So we have:  [no.1][free][no.2][no.3][free][no.4]

    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String[] agentIds = {"agent1", "agent2"};
    final String orgId = new DefaultOrganization().getId();
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(nowMillis + 5 * oneHourMillis),
    };
    final List<Integer> indeces = Arrays.asList(0, 1, 2, 3);
    Collections.shuffle(indeces); // we want to insert in random order to test if method under test sorts correctly.
    for (int i : indeces) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentIds[i % 2]),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    // All events which start from now on agent 1. expected: no.3
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Optional.of(agentIds[0]),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(1, events1.size());
    Assertions.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));

    // All events which start from now on agent 2. expected: no.4
    final List<ExtendedEventDto> events2 = schedulerDatabase.search(
        Optional.of(agentIds[1]),
        Optional.of(new Date(nowMillis)),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(1, events2.size());
    Assertions.assertTrue(events2.get(0).getMediaPackageId().equals(mpId + 3));

    // No start/end dates given. expected: all events of agent 1 are returned
    final List<ExtendedEventDto> events3 = schedulerDatabase.search(
        Optional.of(agentIds[0]),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events3.size());
    Assertions.assertTrue(events3.get(0).getMediaPackageId().equals(mpId + 0));
    Assertions.assertTrue(events3.get(1).getMediaPackageId().equals(mpId + 2));

    // No start/end dates given. expected: all events of agent 2 are returned
    final List<ExtendedEventDto> events4 = schedulerDatabase.search(
        Optional.of(agentIds[1]),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    Assertions.assertEquals(2, events4.size());
    Assertions.assertTrue(events4.get(0).getMediaPackageId().equals(mpId + 1));
    Assertions.assertTrue(events4.get(1).getMediaPackageId().equals(mpId + 3));
  }

  @Test
  public void testDeleteEvent() throws Exception {
    final long oneHourMillis = 3600_000;
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date start = new Date();
    final Date end = new Date(start.getTime() + 2 * oneHourMillis);
    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Optional.of(agentId),
        Optional.of(start),
        Optional.of(end),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    final Optional<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertTrue(initialEvent.isPresent());

    schedulerDatabase.deleteEvent(mpId);

    final Optional<ExtendedEventDto> newEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertFalse(newEvent.isPresent());
  }

  @Test
  public void testGetKnownRecordings() throws Exception {
    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date intervalEnd = new Date(nowMillis + 5 * oneHourMillis);
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(intervalEnd.getTime()),
    };
    final String recordingState = "recordingState";
    final Long lastHeard = new Date().getTime();
    for (int i = 0; i < 4; i++) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentId),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          i % 2 == 0 ? Optional.of(recordingState) : Optional.empty(),
          i % 2 == 0 ? Optional.of(lastHeard) : Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    final List<ExtendedEventDto> knownEvents = schedulerDatabase.getKnownRecordings();
    Assertions.assertEquals(2, knownEvents.size());
    Assertions.assertTrue(knownEvents.get(0).getMediaPackageId().equals(mpId + 0)
        || knownEvents.get(1).getMediaPackageId().equals(mpId + 0));
    Assertions.assertTrue(knownEvents.get(0).getMediaPackageId().equals(mpId + 2)
        || knownEvents.get(1).getMediaPackageId().equals(mpId + 2));
  }

  @Test
  public void testResetRecordingState() throws Exception {
    final String mpId = "mpid";
    final String orgId = new DefaultOrganization().getId();
    final String agentId = "agent1";
    final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
    final Date end = new Date(1570953300000L); // 2019-10-13T07:55:00Z
    final String source = "source";
    final String recordingState = "recordingState";
    final Long lastHeard = new Date().getTime();
    final String presenters = "Werner";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Optional.of(agentId),
        Optional.of(start),
        Optional.of(end),
        Optional.of(source),
        Optional.of(recordingState),
        Optional.of(lastHeard),
        Optional.of(presenters),
        Optional.of(lastModifiedDate),
        Optional.of(checksum),
        Optional.of(wfProperties),
        Optional.of(caProperties)
    );

    schedulerDatabase.resetRecordingState(mpId);

    final Optional<ExtendedEventDto> updatedEvent = schedulerDatabase.getEvent(mpId);
    Assertions.assertTrue(updatedEvent.isPresent());
    Assertions.assertEquals(mpId, updatedEvent.get().getMediaPackageId());
    Assertions.assertEquals(orgId, updatedEvent.get().getOrganization());
    Assertions.assertEquals(agentId, updatedEvent.get().getCaptureAgentId());
    Assertions.assertEquals(start, updatedEvent.get().getStartDate());
    Assertions.assertEquals(end, updatedEvent.get().getEndDate());
    Assertions.assertEquals(source, updatedEvent.get().getSource());
    Assertions.assertNull(updatedEvent.get().getRecordingState());
    Assertions.assertNull(updatedEvent.get().getRecordingLastHeard());
    Assertions.assertEquals(presenters, updatedEvent.get().getPresenters());
    Assertions.assertEquals(lastModifiedDate, updatedEvent.get().getLastModifiedDate());
    Assertions.assertEquals(checksum, updatedEvent.get().getChecksum());
    Assertions.assertEquals(new Gson().toJson(wfProperties), updatedEvent.get().getWorkflowProperties());
    Assertions.assertEquals(new Gson().toJson(caProperties), updatedEvent.get().getCaptureAgentProperties());
  }

  @Test
  public void testCountEvents() throws Exception {
    final long oneHourMillis = 3600_000;
    final long nowMillis = new Date().getTime();
    final String mpId = "mpId";
    final String agentId = "agent1";
    final String orgId = new DefaultOrganization().getId();
    final Date[] eventStartDates = {
        new Date(nowMillis - 4 * oneHourMillis),
        new Date(nowMillis - oneHourMillis),
        new Date(nowMillis + oneHourMillis),
        new Date(nowMillis + 5 * oneHourMillis),
    };
    for (int i = 0; i < 4; i++) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Optional.of(agentId),
          Optional.of(start),
          Optional.of(end),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
      );
    }
    int count = schedulerDatabase.countEvents();
    Assertions.assertEquals(4, count);
  }
}
