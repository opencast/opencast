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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.capture.admin.api.AgentState.CAPTURING;
import static org.opencastproject.capture.admin.api.AgentState.IDLE;
import static org.opencastproject.capture.admin.api.AgentState.OFFLINE;
import static org.opencastproject.capture.admin.api.AgentState.UNKNOWN;
import static org.opencastproject.capture.admin.api.AgentState.UPLOADING;
import static org.opencastproject.capture.admin.api.CaptureAgentStateService.BOOLEAN_OFF;
import static org.opencastproject.capture.admin.api.CaptureAgentStateService.BOOLEAN_ON;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentVersion;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateServiceImpl service = null;
  /* This is what a 1.x agent would return from any of the config endpoints when fully populated with data. */
  private Properties agentConfig1x;
  /* This is what a 2.x agent would return from any of the capabilities endpoints. */
  private Properties agentCaps2x;
  /* This is what a 2.x agent would return from any of the config endpoints. */
  private Properties agentConfig2x;
  /* This is what a 2.x agent needs to register. */
  private Properties agentRegistration2x;
  private static BundleContext bundleContext;
  private static ComponentContext cc;

  @Before
  public void setUp() throws Exception {
    setupService();

    agentConfig1x = new Properties();
    agentConfig1x.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA", "/dev/video0");
    agentConfig1x.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN", "/dev/video1");
    agentConfig1x.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO", "hw:0");
    agentConfig1x.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "CAMERA,SCREEN,AUDIO");
    agentConfig1x.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    agentCaps2x = new Properties();
    agentCaps2x.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_2.toString());
    agentCaps2x.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);
    agentCaps2x.setProperty(CaptureParameters.CAPTURE_LOCAL_CAPABLE, BOOLEAN_ON);
    agentCaps2x.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_OFF);

    agentRegistration2x = new Properties();
    agentRegistration2x.setProperty(CaptureParameters.VENDOR_NAME, "Mock Vendor");
    agentRegistration2x.setProperty(CaptureParameters.VENDOR_MODEL, "Mock Model");
    agentRegistration2x.setProperty(CaptureParameters.VENDOR_HARDWARE, "Mock Hardware");
    agentRegistration2x.setProperty(CaptureParameters.VENDOR_FIRMWARE, "Mock Firmware");
    agentRegistration2x.setProperty(CaptureParameters.CAPTURE_LOCAL_CAPABLE, BOOLEAN_ON);

    agentConfig2x = new Properties();
    agentConfig2x.putAll(agentCaps2x);
    agentConfig2x.putAll(agentRegistration2x);
  }

  private void setupCC() {

    String configKey = CaptureAgentStateServiceImpl.CAPTURE_AGENT_TIMEOUT_KEY;
    String configValue = "15";

    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(configKey)).andReturn(configValue).anyTimes();
    EasyMock.replay(bundleContext);
    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext);
    EasyMock.replay(cc);

  }

  private void setupService() throws Exception {
    service = new CaptureAgentStateServiceImpl();
    service.setEntityManagerFactory(newEntityManagerFactory(CaptureAgentStateServiceImpl.PERSISTENCE_UNIT));
    service.setDBSessionFactory(getDbSessionFactory());

    DefaultOrganization organization = new DefaultOrganization();

    HashSet<JaxbRole> roles = new HashSet<>();
    roles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, organization, ""));
    User user = new JaxbUser("testuser", "test", organization, roles);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    setupCC();

    service.activate(cc);
    service.setupAgentCache(1, TimeUnit.HOURS);
  }

  @After
  public void tearDown() {
    service.deactivate();
  }

  @Test
  public void nonExistantAgent() {
    try {
      service.getAgent("doesNotExist");
      fail("Agent has been found");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void noAgents() {
    assertEquals(0, service.getKnownAgents().size());
  }

  @Test
  public void badAgentStates() throws NotFoundException {
    try {
      service.setAgentState(null, "something");
      assertEquals(0, service.getKnownAgents().size());
      fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }

    try {
      assertEquals(0, service.getKnownAgents().size());
      service.setAgentState("", "something");
      fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }

    try {
      assertEquals(0, service.getKnownAgents().size());
      service.setAgentState("something", null);
      fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void badAgentCapabilities() {
    try {
      service.setAgentConfiguration(null, agentConfig1x);
      fail("Null agent name accepted");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, service.getKnownAgents().size());

    try {
      service.setAgentConfiguration("", agentConfig1x);
      fail("Empty agent name accepted");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, service.getKnownAgents().size());

    try {
      service.setAgentState("something", null);
      fail("Null agent state accepted");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, service.getKnownAgents().size());
  }

  private void verifyAgentCapabilities(String name, String state, Properties caps) {
    try {
      Agent agent = service.getAgent(name);
      assertEquals(name, agent.getName());
      assertEquals(state, agent.getState());
      Properties agentCaps = agent.getCapabilities();
      Set<String> keynames = Stream.concat(
              caps.stringPropertyNames().stream(),
              agentCaps.stringPropertyNames().stream())
          .collect(Collectors.toSet());
      for (String key : keynames) {
        assertEquals("Key " + key + " does not match", caps.getProperty(key), agentCaps.getProperty(key));
      }
    } catch (NotFoundException e) {
      if (state != null) {
        fail("Agent not found by the service, but the desired state is not null");
      }
    }
  }

  private void verifyAgentConfiguration(String name, String state, Properties caps) {
    try {
      Agent agent = service.getAgent(name);
      assertEquals(name, agent.getName());
      assertEquals(state, agent.getState());
      Properties agentConf = agent.getConfiguration();
      Set<String> keynames = Stream.concat(
              caps.stringPropertyNames().stream(),
              agentConf.stringPropertyNames().stream())
          .collect(Collectors.toSet());
      for (String key : keynames) {
        assertEquals("Key " + key + " does not match", caps.getProperty(key), agentConf.getProperty(key));
      }
    } catch (NotFoundException e) {
      if (state != null)
        fail("Agent not found by the service, but the desired state is not null");
    }
  }

  @Test
  public void oneAgentState() {
    Properties bare1xAgent = new Properties();
    bare1xAgent.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    service.setAgentState("agent1", IDLE);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgentCapabilities("notAgent1", null, null);
    verifyAgentCapabilities("agent1", IDLE, bare1xAgent);

    service.setAgentState("agent1", CAPTURING);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgentCapabilities("notAgent1", null, null);
    verifyAgentCapabilities("agent1", CAPTURING, bare1xAgent);
  }

  @Test
  public void agentRegistration2x() {
    Properties bare1xAgent = new Properties();
    bare1xAgent.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    Properties bare2xAgent = new Properties();
    bare2xAgent.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_2.toString());

    // We're going to make agent2 a 2.x agent.  These *must* register with more info, but this one didn't
    // What happens now?
    service.setAgentState("agent2", IDLE);
    assertEquals(1, service.getKnownAgents().size());
    // That's right, it shows up as a *1.x* agent.
    verifyAgentCapabilities("agent2", IDLE, bare1xAgent);

    // Note: *just* setting the config, but not the state does not finalize the registration process!
    service.setAgentConfiguration("agent2", agentRegistration2x);
    // Now, with the configuration, it's a 2.x agent!
    verifyAgentCapabilities("agent2", IDLE, agentCaps2x);

    // Now we do agent 3 to demonstrate that you don't need to register the state first
    // Order of operation here *does not* matter
    service.setAgentConfiguration("agent3", agentRegistration2x);
    service.setAgentState("agent3", IDLE);
    verifyAgentCapabilities("agent3", IDLE, agentCaps2x);

    // Iterate through each of the required fields, and make them too long or missing
    // this should result in a 1x agent, since the required field now fails validation
    List<String> reqFields = List.of(CaptureParameters.VENDOR_FIRMWARE, CaptureParameters.VENDOR_HARDWARE,
        CaptureParameters.VENDOR_NAME, CaptureParameters.VENDOR_MODEL);
    for (String field : reqFields) {
      Properties agentReg = new Properties();
      agentReg.putAll(agentRegistration2x);

      Properties agent4Caps = new Properties();
      //Note that we should not have the caps generated by the core!  These won't register as a 2x agent!
      agent4Caps.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

      Properties agent4Conf = new Properties();
      // Similar to above, we're adding the registration data (the vendor keys), but not the generated caps
      agent4Conf.putAll(agentRegistration2x);
      agent4Conf.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

      agentReg.remove(field);
      agent4Conf.remove(field);

      //We set this state once, then vary the config.
      service.setAgentState("agent4", IDLE);

      // Test what happens if the required field is completely missing
      // This should spit out a 1.x agent with the rest of the keys
      service.setAgentConfiguration("agent4", agentReg);
      verifyAgentCapabilities("agent4", IDLE, agent4Caps);
      verifyAgentConfiguration("agent4", IDLE, agent4Conf);

      // Test what happens when the required field is blank
      // This should spit out a 1.x agent with the keys, even the blank one
      agentReg.putAll(agentRegistration2x);
      agentReg.put(field, "");
      agent4Conf.put(field, "");

      service.setAgentConfiguration("agent4", agentReg);
      verifyAgentCapabilities("agent4", IDLE, agent4Caps);
      verifyAgentConfiguration("agent4", IDLE, agent4Conf);

      // Now we test strings which are too long
      // This should spit out a 1.x agent with all of the keys
      agentReg.putAll(agentRegistration2x);
      agentReg.put(field, "a".repeat(257));
      agent4Conf.put(field, "a".repeat(257));

      service.setAgentConfiguration("agent4", agentReg);
      verifyAgentCapabilities("agent4", IDLE, agent4Caps);
      verifyAgentConfiguration("agent4", IDLE, agent4Conf);
    }
  }

  @Test
  public void testAgentDowngrade() {
    Properties bare1xAgent = new Properties();
    bare1xAgent.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    service.setAgentState("agent1", IDLE);
    service.setAgentConfiguration("agent1", agentRegistration2x);
    verifyAgentCapabilities("agent1", IDLE, agentCaps2x);
    verifyAgentConfiguration("agent1", IDLE, agentConfig2x);

    try {
      assertEquals(AgentVersion.VERSION_2, service.getAgent("agent1").getVersion());
    } catch (NotFoundException e) {
      fail("Agent not found");
    }

    // This is an edge case, but re-configuring an agent that's *already* a 2.x agent
    // should spit out a 1.x agent
    agentRegistration2x.setProperty(CaptureParameters.VENDOR_NAME, "");
    Properties downgraded = new Properties();
    downgraded.putAll(agentRegistration2x);
    downgraded.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());
    downgraded.setProperty(CaptureParameters.VENDOR_NAME, "");

    service.setAgentConfiguration("agent1", agentRegistration2x);
    verifyAgentCapabilities("agent1", IDLE, bare1xAgent);
    verifyAgentConfiguration("agent1", IDLE, downgraded);

    try {
      assertEquals(AgentVersion.VERSION_1, service.getAgent("agent1").getVersion());
    } catch (NotFoundException e) {
      fail("Agent not found");
    }
  }

  @Test
  public void testNullConfiguration() {
    assert2xAgentException("agentFail", IDLE, null);
  }

  @Test
  public void testInvalid2xAgent() {
    agentRegistration2x.setProperty(CaptureParameters.CAPTURE_LOCAL_CAPABLE, BOOLEAN_OFF);
    agentRegistration2x.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);

    assert2xAgentException("agentFail", IDLE, agentRegistration2x);
  }

  // This verifies the *configuration* of the agent.  This is the config data + the capabilities
  private void assert2xAgentConf(String agentName, String agentState, Properties sentConfig, Properties returnedConfig) {
    service.setAgentState(agentName, agentState);
    service.setAgentConfiguration(agentName, sentConfig);

    verifyAgentConfiguration(agentName, agentState, returnedConfig);
  }

  // This verifies the *capabilities* of the agent, which is a subset of the full configuration above
  private void assert2xAgentCaps(String agentName, String agentState, Properties sentConfig, Properties returnedConfig) {
    service.setAgentState(agentName, agentState);
    service.setAgentConfiguration(agentName, sentConfig);

    verifyAgentCapabilities(agentName, agentState, returnedConfig);
  }


  private void assert2xAgentException(String agentName, String agentState, Properties sentConfig) {
    service.setAgentState(agentName, agentState);
    assertThrows(RuntimeException.class, () -> service.setAgentConfiguration(agentName, sentConfig));
  }

  @Test
  public void agent2xBasic() {
    // Case 1: Happy path
    // This is what the CA is sending to the core
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");

    // This is what the core should respond with in terms of configuration data
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: Still happy, fixed inputs so no devices
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);

    // This is what the core should respond with in terms of configuration data
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);

    assert2xAgentCaps("test2", IDLE, sentConfig, returnedConfig);

    // Case 3: Devices string is too long (> 256 char)
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "a".repeat(257));

    assert2xAgentException("test3", IDLE, sentConfig);

    // Case 4: Provide the key, but no devices
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "");

    assert2xAgentException("test3", IDLE, sentConfig);
  }

  @Test
  public void agent2xWithCaps() {
    // Case 1: Happy path
    // This is what the CA is sending to the core
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    sentConfig.setProperty("capture.device.alpha.capability1", "test");

    // This is what the core should respond with in terms of configuration data
    Properties returnedCaps = new Properties();
    returnedCaps.putAll(agentCaps2x);
    returnedCaps.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    returnedCaps.setProperty("capture.device.alpha.capability1", "test");

    Properties returnedConf = new Properties();
    returnedConf.putAll(agentConfig2x);
    returnedConf.putAll(returnedCaps);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedCaps);
    assert2xAgentConf("test", IDLE, sentConfig, returnedConf);

    // Case 2: Still happy, fixed inputs so no devices
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);

    // This is what the core should respond with in terms of configuration data
    returnedCaps = new Properties();
    returnedCaps.putAll(agentCaps2x);

    assert2xAgentCaps("test2", IDLE, sentConfig, returnedCaps);

    // Case 3: Devices string is too long (> 256 char)
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "a".repeat(257));

    assert2xAgentException("test3", IDLE, sentConfig);

    // Case 4: Provide the key, but no devices
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "");

    assert2xAgentException("test3", IDLE, sentConfig);
  }

  @Test
  public void agent2xLocalStartPaused() {
    // Case 1: starting paused is *not* supported
    // This is what the CA is sending to the core
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    sentConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_OFF);

    // This is what the core should respond with in terms of configuration data
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    returnedConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_OFF);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: starting paused *is* supported
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_ON);
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_ON);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 3: invalid data is sent to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, "banana");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, "3");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xStreamCapable() {
    // Case 1: streaming is *not* supported
    // This is what the CA is sending to the core
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);

    // This is what the core should respond with in terms of configuration data
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: streaming *is* supported
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 3: invalid data is sent to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, "banana");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, "3");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xStreamStartPaused() {
    // Case 1: streaming is *not* supported, but we (erroneously) say we support starting paused
    // This is technically an error case, though the core should just silently ignore the flag
    // This is what the CA is sending to the core
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_ON);

    // This is what the core should respond with in terms of configuration data
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: streaming *is* supported, starting paused is not
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF);
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 3: streaming and starting paused are both supported
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_ON);
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_ON);

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 4: bad data passed to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, "banana");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, "3");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xStreamConfigs() {
    // Case 1: The happy path
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "my config with spaces");
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "my config with spaces");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    // NOTE: There's an extra space here after the comma!
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "b config,  a config");
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF);
    // NOTE: The extra space above has been trimmed, and the configs sorted
    returnedConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "a config,b config");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: An empty list, this is an error case and should be rejected
    // This is what the CA is sending to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "alpha");
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "");

    assert2xAgentException("test", IDLE, sentConfig);

    // Case 3: bad length config items
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "a".repeat(33));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "b," + "A".repeat(33));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "A".repeat(33) + ", b");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_ON);
    // Note: There's a double comma here, so three items but one of them is blank
    sentConfig.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "a,,b");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xDevicePositions() {
    // Case 1: The happy path
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "my positions with spaces");
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "my positions with spaces");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    // NOTE: There's an extra space here after the comma!
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "b position,  a position");
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    // NOTE: The extra space above has been trimmed, and the configs sorted
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "a position,b position");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: An empty list, this is an error case and should be rejected
    // This is what the CA is sending to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "");

    assert2xAgentException("test", IDLE, sentConfig);

    // Case 3: bad length config items
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "a".repeat(257));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "b," + "A".repeat(257));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "A".repeat(257) + ", b");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    // Note: There's a double comma here, so three items but one of them is blank
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "a,,b");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xContent() {
    // Case 1: The happy path
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "my content with spaces");
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "my content with spaces");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    // NOTE: There's an extra space here after the comma!
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "b content,  a content");
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    // NOTE: The extra space above has been trimmed, and the configs sorted
    returnedConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "a content,b content");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: An empty list, this is an error case and should be rejected
    // This is what the CA is sending to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "");

    assert2xAgentException("test", IDLE, sentConfig);

    // Case 3: bad length config items
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "a".repeat(257));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "b," + "A".repeat(257));

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "A".repeat(257) + ", b");

    assert2xAgentException("test", IDLE, sentConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    // Note: There's a double comma here, so three items but one of them is blank
    sentConfig.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "a,,b");

    assert2xAgentException("test", IDLE, sentConfig);
  }

  @Test
  public void agent2xVendorExtensions() {
    // Case 1: The happy path
    Properties sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast", "with commas, my vendor extension");
    Properties returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    // Note: We don't touch the contents, so this *does not* get split, trimmed, and/or sorted
    returnedConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast",
        "with commas, my vendor extension");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast", " we do trim spaces ");
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    // Note: We do, however, trim the overall string
    returnedConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast", "we do trim spaces");

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

    // Case 2: An empty list, this is an error case and should be rejected
    // This is what the CA is sending to the core
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast", "");

    assert2xAgentException("test", IDLE, sentConfig);

    // Case 3: bad length config items
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "opencast", "a".repeat(257));

    assert2xAgentException("test", IDLE, sentConfig);

    // Case 4: The *key* is too long
    sentConfig = new Properties();
    sentConfig.putAll(agentRegistration2x);
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "o".repeat(257), "does not matter");
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "not matching", "does not matter");
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX + "diacritics-like-ümlauts-no-bueno", "does not matter");
    // This next one has the prefix, but nothing else
    sentConfig.setProperty(CaptureParameters.CAPTURE_EXTENSION_PREFIX, "does not matter");
    returnedConfig = new Properties();
    returnedConfig.putAll(agentCaps2x);
    // None of the extensions above match the vendor key requirements, so they are *silently* stripped!

    assert2xAgentCaps("test", IDLE, sentConfig, returnedConfig);

  }

  @Test
  public void oneAgentCapabilities() {
    service.setAgentConfiguration("agent1", agentConfig1x);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgentCapabilities("notAgent1", null, new Properties());
    verifyAgentCapabilities("agent1", UNKNOWN, agentConfig1x);

    service.setAgentState("agent1", IDLE);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgentCapabilities("notAgent1", null, new Properties());
    verifyAgentCapabilities("agent1", IDLE, agentConfig1x);

    service.setAgentConfiguration("agent1", new Properties());
    assertEquals(1, service.getKnownAgents().size());

    verifyAgentCapabilities("notAnAgent", null, new Properties());
    Properties bareConfig1x = new Properties();
    bareConfig1x.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());
    verifyAgentCapabilities("agent1", IDLE, bareConfig1x);
  }

  @Test
  public void removeAgent() {
    service.setAgentConfiguration("agent1", agentConfig1x);
    assertEquals(1, service.getKnownAgents().size());
    service.setAgentConfiguration("agent2", agentConfig1x);
    service.setAgentState("agent2", UPLOADING);

    verifyAgentCapabilities("notAnAgent", null, agentConfig1x);
    verifyAgentCapabilities("agent1", UNKNOWN, agentConfig1x);
    verifyAgentCapabilities("agent2", UPLOADING, agentConfig1x);

    try {
      service.removeAgent("agent1");
      assertEquals(1, service.getKnownAgents().size());
      verifyAgentCapabilities("notAnAgent", null, agentConfig1x);
      verifyAgentCapabilities("agent1", null, agentConfig1x);
      verifyAgentCapabilities("agent2", UPLOADING, agentConfig1x);
    } catch (NotFoundException e) {
      fail();
    }

    try {
      service.removeAgent("notAnAgent");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
    assertEquals(1, service.getKnownAgents().size());
    verifyAgentCapabilities("notAnAgent", null, agentConfig1x);
    verifyAgentCapabilities("agent1", null, agentConfig1x);
    verifyAgentCapabilities("agent2", UPLOADING, agentConfig1x);
  }

  @Test
  public void agentCapabilities() {
    try {
      service.getAgentCapabilities("agent");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
    try {
      service.getAgentCapabilities("NotAgent");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }

    service.setAgentConfiguration("agent", agentConfig1x);
    Properties agentCapabilities;
    try {
      agentCapabilities = service.getAgentCapabilities("agent");
      assertEquals(agentConfig1x.toString(), agentCapabilities.toString());
    } catch (NotFoundException e) {
      fail();
    }
    try {
      service.getAgentCapabilities("NotAgent");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void stickyAgents() throws Exception {
    assertEquals(0, service.getKnownAgents().size());

    Properties cap1 = new Properties();
    cap1.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "key", "value");
    cap1.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "key");
    cap1.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());
    Properties cap2 = new Properties();
    cap2.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "foo", "bar");
    cap2.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "foo");
    cap2.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());
    Properties cap3 = new Properties();
    cap3.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "bam", "bam");
    cap3.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "bam");
    cap3.setProperty(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    // Setup the two agents and persist them
    service.setAgentState("sticky1", IDLE);
    service.setAgentConfiguration("sticky1", cap1);
    service.setAgentState("sticky2", CAPTURING);
    service.setAgentConfiguration("sticky2", cap2);
    service.setAgentState("sticky3", UPLOADING);
    service.setAgentConfiguration("sticky3", cap3);

    // Make sure they're set right
    assertEquals(cap1.toString(), service.getAgentCapabilities("sticky1").toString());
    assertEquals(IDLE, service.getAgent("sticky1").getState());
    assertEquals(cap2.toString(), service.getAgentCapabilities("sticky2").toString());
    assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    assertEquals(cap3.toString(), service.getAgentCapabilities("sticky3").toString());
    assertEquals(UPLOADING, service.getAgent("sticky3").getState());
    try {
      service.getAgentCapabilities("sticky4");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
    try {
      service.getAgent("sticky4");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }

    assertEquals(3, service.getKnownAgents().size());

    // The agents should still be there
    assertEquals(cap1.toString(), service.getAgentCapabilities("sticky1").toString());
    assertEquals(IDLE, service.getAgent("sticky1").getState());
    assertEquals(cap2.toString(), service.getAgentCapabilities("sticky2").toString());
    assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    assertEquals(cap3.toString(), service.getAgentCapabilities("sticky3").toString());
    assertEquals(UPLOADING, service.getAgent("sticky3").getState());
    try {
      service.getAgentCapabilities("sticky4");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
    try {
      service.getAgent("sticky4");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testAgentVisibility() throws Exception {
    // Create a new capture agent called "visibility"
    String agentName = "visibility";
    service.setAgentState(agentName, IDLE);

    // Ensure we can see it
    assertEquals(1, service.getKnownAgents().size());

    // Set the roles allowed to use this agent
    Set<String> roles = new HashSet<>();
    roles.add("a_role_we_do_not_have");
    AgentImpl agent = (AgentImpl) service.getAgent(agentName);
    agent.setSchedulerRoles(roles);
    service.updateAgentInDatabase(agent);

    // Since we are an organizational admin, we should still see the agent
    assertEquals(1, service.getKnownAgents().size());

    // Use a security service that identifies us as a non-administrative user
    DefaultOrganization organization = new DefaultOrganization();
    HashSet<JaxbRole> roleSet = new HashSet<>();
    roleSet.add(new JaxbRole("ROLE_NOT_ADMIN", organization, ""));
    User user = new JaxbUser("testuser", "test", organization, roleSet);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    // Ensure we can no longer see the agent, since we don't have an administrative role
    assertEquals(0, service.getKnownAgents().size());

    // TODO: Do we need to enforce access strictly? If someone asks for an agent by name, but they do not have the
    // appropriate scheduler role, should we throw UnauthorizedException?
  }

  @Test
  public void testManagedServiceFactory() throws Exception {
    // Make sure we can register a capture agent with specific scheduler roles
    String pid = UUID.randomUUID().toString();
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put("id", "agent1");
    properties.put("organization", DefaultOrganization.DEFAULT_ORGANIZATION_ID);
    properties.put("url", "http://agent1:8080/");
    properties.put("schedulerRoles", DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN + ", SOME_OTHER_ROLE");
    service.updated(pid, properties);

    // If any of the three values are missing, we should throw
    properties.remove("id");
    try {
      service.updated(pid, properties);
      fail();
    } catch (ConfigurationException e) {
      // expected
    }
  }

  @Test
  public void testUpdatedTimeSinceLastUpdate() throws Exception {
    // See MH-10031
    String name = "agent1";
    Long lastHeardFrom = 0L;
    Agent agent = null;
    service.setAgentState(name, IDLE);

    agent = service.getAgent(name);
    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, CAPTURING);
    agent = service.getAgent(name);
    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());

    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);
    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());

    lastHeardFrom = agent.getLastHeardFrom();
    Thread.sleep(100L);
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);
    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());

    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, UNKNOWN);
    agent = service.getAgent(name);
    assertTrue(lastHeardFrom.equals(agent.getLastHeardFrom()));
  }

  @Test
  public void testAgentStateTimeout() throws Exception {
    service.setupAgentCache(1, TimeUnit.SECONDS);
    String name = "agent1";
    Long lastHeardFrom = 0L;
    Agent agent = null;
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);

    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());
    assertTrue(agent.getLastHeardFrom() <= System.currentTimeMillis());

    Thread.sleep(1500);
    assertEquals(OFFLINE, service.getAgentState(name));
  }

  @Test
  public void testAllAgentsStateTimeout() throws Exception {
    service.setupAgentCache(1, TimeUnit.SECONDS);
    String name = "agent1";
    Long lastHeardFrom = 0L;
    Agent agent = null;
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);

    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());
    assertTrue(agent.getLastHeardFrom() <= System.currentTimeMillis());

    Thread.sleep(1500);
    Map<String, Agent> agents = service.getKnownAgents();

    assertEquals(OFFLINE, agents.get(name).getState());
  }

  @Test
  public void testAgentReturn() throws Exception {
    service.setupAgentCache(1, TimeUnit.SECONDS);
    String name = "agent1";
    Long lastHeardFrom = 0L;
    Agent agent = null;
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);

    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());
    assertTrue(agent.getLastHeardFrom() <= System.currentTimeMillis());

    Thread.sleep(1500);
    Map<String, Agent> agents = service.getKnownAgents();

    assertEquals(OFFLINE, agents.get(name).getState());
    assertEquals(OFFLINE, service.getAgentState(name));

    service.setAgentState(name, IDLE);
    long time = System.currentTimeMillis();
    agent = service.getAgent(name);

    assertTrue(lastHeardFrom <= agent.getLastHeardFrom());
    assertTrue(time - agent.getLastHeardFrom() <= 5);
  }
}
