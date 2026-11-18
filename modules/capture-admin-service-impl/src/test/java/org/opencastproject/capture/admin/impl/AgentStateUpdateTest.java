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

package org.opencastproject.capture.admin.impl;

import static org.opencastproject.capture.admin.api.AgentState.IDLE;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentStateUpdate;
import org.opencastproject.security.api.DefaultOrganization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgentStateUpdateTest {

  private Agent agent = null;
  private AgentStateUpdate asu = null;

  @BeforeEach
  public void setUp() throws InterruptedException {
    agent = new AgentImpl("test", DefaultOrganization.DEFAULT_ORGANIZATION_ID, IDLE, "", null);
    Assertions.assertNotNull(agent);
    Thread.sleep(5);
    asu = new AgentStateUpdate(agent);
    Assertions.assertNotNull(asu);
  }

  @AfterEach
  public void tearDown() {
    agent = null;
    asu = null;
  }

  @Test
  public void correctInformation() {
    Assertions.assertEquals("test", asu.getName());
    Assertions.assertEquals(IDLE, asu.getState());
    if (asu.getTimeSinceLastUpdate() <= 1) {
      Assertions.fail("Invalid update time in agent state update");
    }
  }

  @Test
  // This is a stupid test, but it gets us up to 100%...
  public void blank() {
    asu = new AgentStateUpdate();
    Assertions.assertNotNull(asu);
    Assertions.assertNull(asu.getName());
    Assertions.assertNull(asu.getState());
    Assertions.assertNull(asu.getTimeSinceLastUpdate());
  }

}
