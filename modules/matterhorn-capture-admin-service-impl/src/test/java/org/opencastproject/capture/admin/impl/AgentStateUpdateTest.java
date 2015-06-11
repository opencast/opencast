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

package org.opencastproject.capture.admin.impl;

import static org.opencastproject.capture.admin.api.AgentState.IDLE;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentStateUpdate;
import org.opencastproject.security.api.DefaultOrganization;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AgentStateUpdateTest {

  private Agent agent = null;
  private AgentStateUpdate asu = null;

  @Before
  public void setUp() throws InterruptedException {
    agent = new AgentImpl("test", DefaultOrganization.DEFAULT_ORGANIZATION_ID, IDLE, "", null);
    Assert.assertNotNull(agent);
    Thread.sleep(5);
    asu = new AgentStateUpdate(agent);
    Assert.assertNotNull(asu);
  }

  @After
  public void tearDown() {
    agent = null;
    asu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", asu.getName());
    Assert.assertEquals(IDLE, asu.getState());
    if (asu.getTimeSinceLastUpdate() <= 1) {
      Assert.fail("Invalid update time in agent state update");
    }
  }

  @Test
  // This is a stupid test, but it gets us up to 100%...
  public void blank() {
    asu = new AgentStateUpdate();
    Assert.assertNotNull(asu);
    Assert.assertNull(asu.getName());
    Assert.assertNull(asu.getState());
    Assert.assertNull(asu.getTimeSinceLastUpdate());
  }

}
