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

package org.opencastproject.scheduler.impl.persistence;

import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests persistent storage.
 */
public class SchedulerServiceDatabaseImplTest {

  private SchedulerServiceDatabaseImpl schedulerDatabase;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase
        .setEntityManagerFactory(newTestEntityManagerFactory(SchedulerServiceDatabaseImpl.PERSISTENCE_UNIT));
    schedulerDatabase.setSecurityService(securityService);
    schedulerDatabase.activate(null);
  }

  @Test
  public void testLastModifed() throws Exception {
    Date now = new Date();
    String agentId = "agent1";

    try {
      schedulerDatabase.getLastModified(agentId);
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    Assert.assertTrue(schedulerDatabase.getLastModifiedDates().isEmpty());

    schedulerDatabase.touchLastEntry(agentId);

    Date lastModified = schedulerDatabase.getLastModified(agentId);
    Assert.assertTrue(lastModified.after(now));

    Map<String, Date> dates = schedulerDatabase.getLastModifiedDates();
    Assert.assertEquals(1, dates.size());
    lastModified = dates.get(agentId);
    Assert.assertTrue(lastModified.after(now));
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
    final String lastModifiedOrigin = "lastModifiedOrigin";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    final Opt<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertFalse(initialEvent.isSome());

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Opt.some(agentId),
        Opt.some(start),
        Opt.some(end),
        Opt.some(source),
        Opt.some(recordingState),
        Opt.some(lastHeard),
        Opt.some(presenters),
        Opt.some(lastModifiedDate),
        Opt.some(checksum),
        Opt.some(wfProperties),
        Opt.some(caProperties)
    );
    final Opt<ExtendedEventDto> newEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertTrue(newEvent.isSome());
    Assert.assertEquals(mpId, newEvent.get().getMediaPackageId());
    Assert.assertEquals(orgId, newEvent.get().getOrganization());
    Assert.assertEquals(agentId, newEvent.get().getCaptureAgentId());
    Assert.assertEquals(start, newEvent.get().getStartDate());
    Assert.assertEquals(end, newEvent.get().getEndDate());
    Assert.assertEquals(source, newEvent.get().getSource());
    Assert.assertEquals(recordingState, newEvent.get().getRecordingState());
    Assert.assertEquals(lastHeard, newEvent.get().getRecordingLastHeard());
    Assert.assertEquals(presenters, newEvent.get().getPresenters());
    Assert.assertEquals(lastModifiedDate, newEvent.get().getLastModifiedDate());
    Assert.assertEquals(checksum, newEvent.get().getChecksum());
    Assert.assertEquals(new Gson().toJson(wfProperties), newEvent.get().getWorkflowProperties());
    Assert.assertEquals(new Gson().toJson(caProperties), newEvent.get().getCaptureAgentProperties());
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
    final String lastModifiedOrigin = "lastModifiedOrigin";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    final Opt<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertFalse(initialEvent.isSome());

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Opt.some(agentId),
        Opt.some(start),
        Opt.some(end),
        Opt.some(source),
        Opt.some(recordingState),
        Opt.some(lastHeard),
        Opt.some(presenters),
        Opt.some(lastModifiedDate),
        Opt.some(checksum),
        Opt.some(wfProperties),
        Opt.some(caProperties)
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
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(updatedRecordingState),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(updatedCaProperties)
    );
    final Opt<ExtendedEventDto> updatedEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertTrue(updatedEvent.isSome());
    Assert.assertEquals(mpId, updatedEvent.get().getMediaPackageId());
    Assert.assertEquals(orgId, updatedEvent.get().getOrganization());
    Assert.assertEquals(agentId, updatedEvent.get().getCaptureAgentId());
    Assert.assertEquals(start, updatedEvent.get().getStartDate());
    Assert.assertEquals(end, updatedEvent.get().getEndDate());
    Assert.assertEquals(source, updatedEvent.get().getSource());
    Assert.assertEquals(updatedRecordingState, updatedEvent.get().getRecordingState());
    Assert.assertEquals(lastHeard, updatedEvent.get().getRecordingLastHeard());
    Assert.assertEquals(presenters, updatedEvent.get().getPresenters());
    Assert.assertEquals(lastModifiedDate, updatedEvent.get().getLastModifiedDate());
    Assert.assertEquals(checksum, updatedEvent.get().getChecksum());
    Assert.assertEquals(new Gson().toJson(wfProperties), updatedEvent.get().getWorkflowProperties());
    Assert.assertEquals(new Gson().toJson(updatedCaProperties), updatedEvent.get().getCaptureAgentProperties());
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
          Opt.some(agentId),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    final int separationMillis = 60 * 1000;
    final List<String> eventIds = schedulerDatabase.getEvents(agentId, intervalStart, intervalEnd, separationMillis);
    Assert.assertEquals(3, eventIds.size());
    Assert.assertFalse(eventIds.contains(mpId + 0));
    Assert.assertTrue(eventIds.contains(mpId + 1));
    Assert.assertTrue(eventIds.contains(mpId + 2));
    Assert.assertTrue(eventIds.contains(mpId + 3));
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
          Opt.some(agentId),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    // All events which start from now. expected: no.3 and no.4
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events1.size());
    Assert.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(events1.get(1).getMediaPackageId().equals(mpId + 3));

    // All events which start to now. expected: no.1 and no.2
    final List<ExtendedEventDto> events2 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events2.size());
    Assert.assertTrue(events2.get(0).getMediaPackageId().equals(mpId + 0));
    Assert.assertTrue(events2.get(1).getMediaPackageId().equals(mpId + 1));

    // All events which end from now. expected: no.2, no.3, and no.4
    final List<ExtendedEventDto> events3 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(3, events3.size());
    Assert.assertTrue(events3.get(0).getMediaPackageId().equals(mpId + 1));
    Assert.assertTrue(events3.get(1).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(events3.get(2).getMediaPackageId().equals(mpId + 3));

    // All events which end to now. expected: no.1,
    final List<ExtendedEventDto> events4 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none()
    );
    Assert.assertEquals(1, events4.size());
    Assert.assertTrue(events4.get(0).getMediaPackageId().equals(mpId + 0));

    // All events which start from now AND start to now -> illegal combination, no results expected
    Assert.assertTrue(schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none()
    ).isEmpty());

    // All events which end from now AND end to now -> illegal combination, no results expected
    Assert.assertTrue(schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.some(new Date(nowMillis)),
        Opt.none()
    ).isEmpty());

    // All events which start from now AND end to now -> illegal combination, no results expected
    Assert.assertTrue(schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none()
    ).isEmpty());

    // All events which start from now + 1 hour. expected: no. 3 and no. 4
    final List<ExtendedEventDto> events5 = schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis + oneHourMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events5.size());
    Assert.assertTrue(events5.get(0).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(events5.get(1).getMediaPackageId().equals(mpId + 3));

    // All events which start to now + 1 hour. expected: no. 1 and no. 2, but not no. 3
    final List<ExtendedEventDto> events6 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis + oneHourMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events6.size());
    Assert.assertTrue(events6.get(0).getMediaPackageId().equals(mpId + 0));
    Assert.assertTrue(events6.get(1).getMediaPackageId().equals(mpId + 1));

    // All events which end from now + 1 hour. expected: no2., no.3, and no.4
    final List<ExtendedEventDto> events7 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis + oneHourMillis)),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(3, events7.size());
    Assert.assertTrue(events7.get(0).getMediaPackageId().equals(mpId + 1));
    Assert.assertTrue(events7.get(1).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(events7.get(2).getMediaPackageId().equals(mpId + 3));

    // All events which end to now + 1 hour. expected: no.1
    final List<ExtendedEventDto> events8 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis + oneHourMillis)),
        Opt.none()
    );
    Assert.assertEquals(1, events8.size());
    Assert.assertTrue(events8.get(0).getMediaPackageId().equals(mpId + 0));

    // No start/end dates given. expected: all events are returned
    final List<ExtendedEventDto> events9 = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(4, events9.size());
    Assert.assertTrue(events9.get(0).getMediaPackageId().equals(mpId + 0));
    Assert.assertTrue(events9.get(1).getMediaPackageId().equals(mpId + 1));
    Assert.assertTrue(events9.get(2).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(events9.get(3).getMediaPackageId().equals(mpId + 3));

    // All events which start to now and end to now. expected: no. 1
    final List<ExtendedEventDto> eventsA = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none()
    );
    Assert.assertEquals(1, eventsA.size());
    Assert.assertTrue(eventsA.get(0).getMediaPackageId().equals(mpId + 0));

    // All events which start to now and end from now. expected: no. 2
    final List<ExtendedEventDto> eventsB = schedulerDatabase.search(
        Opt.none(),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(1, eventsB.size());
    Assert.assertTrue(eventsB.get(0).getMediaPackageId().equals(mpId + 1));

    // All events which start from now and end from now. expected: no. 3 and no. 4
    final List<ExtendedEventDto> eventsC = schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, eventsC.size());
    Assert.assertTrue(eventsC.get(0).getMediaPackageId().equals(mpId + 2));
    Assert.assertTrue(eventsC.get(1).getMediaPackageId().equals(mpId + 3));
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
          Opt.some(agentId),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    // All events which start from now. expected: no.3 and no.4, but limit is 1, so just no. 3
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Opt.none(),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.some(1)
    );
    Assert.assertEquals(1, events1.size());
    Assert.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));
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
          Opt.some(agentIds[i % 2]),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    // All events which start from now on agent 1. expected: no.3
    final List<ExtendedEventDto> events1 = schedulerDatabase.search(
        Opt.some(agentIds[0]),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(1, events1.size());
    Assert.assertTrue(events1.get(0).getMediaPackageId().equals(mpId + 2));

    // All events which start from now on agent 2. expected: no.4
    final List<ExtendedEventDto> events2 = schedulerDatabase.search(
        Opt.some(agentIds[1]),
        Opt.some(new Date(nowMillis)),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(1, events2.size());
    Assert.assertTrue(events2.get(0).getMediaPackageId().equals(mpId + 3));

    // No start/end dates given. expected: all events of agent 1 are returned
    final List<ExtendedEventDto> events3 = schedulerDatabase.search(
        Opt.some(agentIds[0]),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events3.size());
    Assert.assertTrue(events3.get(0).getMediaPackageId().equals(mpId + 0));
    Assert.assertTrue(events3.get(1).getMediaPackageId().equals(mpId + 2));

    // No start/end dates given. expected: all events of agent 2 are returned
    final List<ExtendedEventDto> events4 = schedulerDatabase.search(
        Opt.some(agentIds[1]),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    Assert.assertEquals(2, events4.size());
    Assert.assertTrue(events4.get(0).getMediaPackageId().equals(mpId + 1));
    Assert.assertTrue(events4.get(1).getMediaPackageId().equals(mpId + 3));
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
        Opt.some(agentId),
        Opt.some(start),
        Opt.some(end),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none(),
        Opt.none()
    );
    final Opt<ExtendedEventDto> initialEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertTrue(initialEvent.isSome());

    schedulerDatabase.deleteEvent(mpId);

    final Opt<ExtendedEventDto> newEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertFalse(newEvent.isSome());
  }

  @Test
  public void testGetKnownRecordings() throws Exception {
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
    final String recordingState = "recordingState";
    final Long lastHeard = new Date().getTime();
    for (int i = 0; i < 4; i++) {
      final Date start = eventStartDates[i];
      final Date end = new Date(start.getTime() + 2 * oneHourMillis);
      schedulerDatabase.storeEvent(
          mpId + i,
          orgId,
          Opt.some(agentId),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          i % 2 == 0 ? Opt.some(recordingState) : Opt.none(),
          i % 2 == 0 ? Opt.some(lastHeard) : Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    final List<ExtendedEventDto> knownEvents = schedulerDatabase.getKnownRecordings();
    Assert.assertEquals(2, knownEvents.size());
    Assert.assertTrue(knownEvents.get(0).getMediaPackageId().equals(mpId + 0)
        || knownEvents.get(1).getMediaPackageId().equals(mpId + 0));
    Assert.assertTrue(knownEvents.get(0).getMediaPackageId().equals(mpId + 2)
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
    final String lastModifiedOrigin = "lastModifiedOrigin";
    final Date lastModifiedDate = new Date();
    final String checksum = "checksum";
    final Map<String, String> wfProperties = Collections.singletonMap("wffoo", "wfbar");
    final Map<String, String> caProperties = Collections.singletonMap("cafoo", "cabar");

    schedulerDatabase.storeEvent(
        mpId,
        orgId,
        Opt.some(agentId),
        Opt.some(start),
        Opt.some(end),
        Opt.some(source),
        Opt.some(recordingState),
        Opt.some(lastHeard),
        Opt.some(presenters),
        Opt.some(lastModifiedDate),
        Opt.some(checksum),
        Opt.some(wfProperties),
        Opt.some(caProperties)
    );

    schedulerDatabase.resetRecordingState(mpId);

    final Opt<ExtendedEventDto> updatedEvent = schedulerDatabase.getEvent(mpId);
    Assert.assertTrue(updatedEvent.isSome());
    Assert.assertEquals(mpId, updatedEvent.get().getMediaPackageId());
    Assert.assertEquals(orgId, updatedEvent.get().getOrganization());
    Assert.assertEquals(agentId, updatedEvent.get().getCaptureAgentId());
    Assert.assertEquals(start, updatedEvent.get().getStartDate());
    Assert.assertEquals(end, updatedEvent.get().getEndDate());
    Assert.assertEquals(source, updatedEvent.get().getSource());
    Assert.assertNull(updatedEvent.get().getRecordingState());
    Assert.assertNull(updatedEvent.get().getRecordingLastHeard());
    Assert.assertEquals(presenters, updatedEvent.get().getPresenters());
    Assert.assertEquals(lastModifiedDate, updatedEvent.get().getLastModifiedDate());
    Assert.assertEquals(checksum, updatedEvent.get().getChecksum());
    Assert.assertEquals(new Gson().toJson(wfProperties), updatedEvent.get().getWorkflowProperties());
    Assert.assertEquals(new Gson().toJson(caProperties), updatedEvent.get().getCaptureAgentProperties());
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
          Opt.some(agentId),
          Opt.some(start),
          Opt.some(end),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none(),
          Opt.none()
      );
    }
    int count = schedulerDatabase.countEvents();
    Assert.assertEquals(4, count);
  }
}
