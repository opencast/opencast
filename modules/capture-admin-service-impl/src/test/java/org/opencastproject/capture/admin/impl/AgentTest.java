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

import static org.opencastproject.capture.admin.api.AgentState.CAPTURING;
import static org.opencastproject.capture.admin.api.AgentState.IDLE;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.security.api.DefaultOrganization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class AgentTest {
  private Agent agent = null;
  private Long time = 0L;

  @BeforeEach
  public void setUp() {
    agent = new AgentImpl("test", DefaultOrganization.DEFAULT_ORGANIZATION_ID, IDLE, "http://localhost/", null);
    Assertions.assertNotNull(agent);
    time = agent.getLastHeardFrom();
  }

  @AfterEach
  public void tearDown() {
    agent = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assertions.assertEquals("test", agent.getName());
    Assertions.assertEquals(IDLE, agent.getState());
    Assertions.assertEquals("http://localhost/", agent.getUrl());
  }

  @Test
  public void changedInformation() throws InterruptedException {
    Assertions.assertEquals("test", agent.getName());
    Assertions.assertEquals(IDLE, agent.getState());
    Assertions.assertEquals(time, agent.getLastHeardFrom());

    Thread.sleep(1);
    agent.setState(CAPTURING);

    Assertions.assertEquals("test", agent.getName());
    Assertions.assertEquals(CAPTURING, agent.getState());
    Thread.sleep(1);
    if (agent.getLastHeardFrom() <= time || agent.getLastHeardFrom() > System.currentTimeMillis()) {
      Assertions.fail("Invalid checkin time");
    }
  }

  @Test
  public void onlyKeysStartingWithTheDevicePrefixBecomeCapabilities() {
    Properties configuration = new Properties();
    configuration.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "CAMERA");
    configuration.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA.src", "/dev/video0");
    configuration.setProperty("legacy." + CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA.src", "/dev/video9");

    agent.setConfiguration(configuration);

    Properties capabilities = agent.getCapabilities();
    Assertions.assertEquals("CAMERA", capabilities.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES));
    Assertions.assertEquals("/dev/video0",
        capabilities.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA.src"));
    Assertions.assertFalse(capabilities.containsKey("legacy." + CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA.src"),
        "A key merely containing the device prefix must not become a capability");
  }
}
