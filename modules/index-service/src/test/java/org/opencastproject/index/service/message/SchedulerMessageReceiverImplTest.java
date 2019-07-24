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
package org.opencastproject.index.service.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.message.broker.api.scheduler.SchedulerItemList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;

import org.junit.Before;
import org.junit.Test;

public class SchedulerMessageReceiverImplTest {

  private SchedulerMessageReceiverImpl scheduler;
  private final TestSearchIndex index = new TestSearchIndex();

  @Before
  public void setUp() throws Exception {
    SecurityService securityService = TestSearchIndex.createSecurityService(new DefaultOrganization());
    scheduler = new SchedulerMessageReceiverImpl();
    scheduler.setSecurityService(securityService);
    scheduler.setSearchIndex(index);
  }

  @Test
  public void testUpdateCreator() throws Exception {
    DublinCoreCatalog catalog = DublinCores.read(getClass().getResourceAsStream("/dublincore.xml"));
    SchedulerItemList schedulerItem = new SchedulerItemList("uuid", SchedulerItem.updateCatalog(catalog));

    // Test initial set of creator
    scheduler.execute(schedulerItem);
    Event event = index.getEventResult();
    assertNotNull(event);
    assertEquals("Current user is expected to be creator as no other creator has been set explicitly", "Creator",
            event.getCreator());

    // Test updating creator
    event.setCreator("Hans");
    index.setInitialEvent(event);
    scheduler.execute(schedulerItem);
    event = index.getEventResult();
    assertNotNull(event);
    assertEquals("Creator has been updated", "Hans", event.getCreator());
  }

}
