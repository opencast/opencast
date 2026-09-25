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
package org.opencastproject.basicstatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseServiceImpl;
import org.opencastproject.basicstatisticssecret.impl.BasicStatisticsSecretServiceImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Covers:
 * - persisting raw events
 * - determining the statistics completeness threshold.
 */
public class BasicStatisticsServiceTest {

  private static final String ORGANIZATION = DefaultOrganization.DEFAULT_ORGANIZATION_ID;
  private static final String ITEM_ID = "item-1";

  private BasicStatisticsDatabaseServiceImpl persistence;
  private BasicStatisticsSecretServiceImpl secretService;
  private BasicStatisticsService service;

  @Before
  public void setUp() {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    persistence = new BasicStatisticsDatabaseServiceImpl();
    persistence.setEntityManagerFactory(newEntityManagerFactory(BasicStatisticsDatabaseServiceImpl.PERSISTENCE_UNIT));
    persistence.setDBSessionFactory(getDbSessionFactory());
    persistence.activate(null);

    secretService = new BasicStatisticsSecretServiceImpl();
    secretService.activate(null);

    service = new BasicStatisticsService();
    service.setPersistence(persistence);
    service.setSecurityService(securityService);
    service.setBasicStatisticsSecretService(secretService);
  }

  @After
  public void tearDown() {
    secretService.deactivate();
  }

  @Test
  public void createSetsOrganizationAndPersistsEvents() {
    RawEvent event = new RawEvent(null, null, Instant.now(), "session-1", ItemType.VIDEO, ITEM_ID,
        EventType.VIDEO_PLAY, null);

    service.create(List.of(event));

    List<RawEvent> stored = service.getRawEvents(10, 0);
    assertEquals(1, stored.size());
    assertEquals(ORGANIZATION, stored.get(0).getOrganization());
    assertEquals(ITEM_ID, stored.get(0).getItemId());
  }

  @Test
  public void recordFileFetchedGeneratesSessionHashAndPersistsEvent() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getRemoteAddr()).andReturn("127.0.0.1").anyTimes();
    EasyMock.expect(request.getHeader("User-Agent")).andReturn("test-agent").anyTimes();
    EasyMock.replay(request);

    RawEvent event = new RawEvent(null, null, Instant.now(), null, ItemType.VIDEO, ITEM_ID,
        EventType.FETCH_FILE, null);

    service.recordFileFetched(event, request);

    List<RawEvent> stored = service.getRawEvents(10, 0);
    assertEquals(1, stored.size());
    assertNotNull(stored.get(0).getSession());
    assertFalse(stored.get(0).getSession().isEmpty());
  }

  @Test
  public void completenessThresholdUsesConfiguredOverrideWhenSet() {
    service.activate(Collections.singletonMap("completeness.threshold.date", "2026-06-15"));

    assertEquals(LocalDate.of(2026, 6, 15), service.getCompletenessThreshold());
  }

  @Test
  public void completenessThresholdFallsBackToVersion1TimestampWhenNotSet() {
    // Recording the version-1 timestamp happens as a side effect of persistence.activate() in setUp().
    service.activate(Collections.emptyMap());

    assertEquals(LocalDate.now(ZoneOffset.UTC), service.getCompletenessThreshold());
  }
}
