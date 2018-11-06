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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
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

}
