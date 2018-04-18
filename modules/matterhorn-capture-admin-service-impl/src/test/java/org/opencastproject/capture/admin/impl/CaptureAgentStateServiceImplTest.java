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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.capture.admin.api.AgentState.CAPTURING;
import static org.opencastproject.capture.admin.api.AgentState.IDLE;
import static org.opencastproject.capture.admin.api.AgentState.OFFLINE;
import static org.opencastproject.capture.admin.api.AgentState.UNKNOWN;
import static org.opencastproject.capture.admin.api.AgentState.UPLOADING;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.Prelude;

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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateServiceImpl service = null;
  private Properties capabilities;
  private static BundleContext bundleContext;
  private static ComponentContext cc;

  @Before
  public void setUp() throws Exception {
    setupService();

    capabilities = new Properties();
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA", "/dev/video0");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN", "/dev/video1");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO", "hw:0");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "CAMERA,SCREEN,AUDIO");
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
    service.setEntityManagerFactory(newTestEntityManagerFactory(CaptureAgentStateServiceImpl.PERSISTENCE_UNIT));

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
      service.setAgentConfiguration(null, capabilities);
      fail("Null agent name accepted");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, service.getKnownAgents().size());

    try {
      service.setAgentConfiguration("", capabilities);
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

  private void verifyAgent(String name, String state, Properties caps) {
    try {
      Agent agent = service.getAgent(name);
      assertEquals(name, agent.getName());
      assertEquals(state, agent.getState());
      assertEquals(caps, agent.getCapabilities());
    } catch (NotFoundException e) {
      if (state != null)
        fail();
    }
  }

  @Test
  public void oneAgentState() {
    service.setAgentState("agent1", IDLE);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", IDLE, new Properties());

    service.setAgentState("agent1", CAPTURING);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", CAPTURING, new Properties());
  }

  @Test
  public void oneAgentCapabilities() {
    service.setAgentConfiguration("agent1", capabilities);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", UNKNOWN, capabilities);

    service.setAgentState("agent1", IDLE);
    assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", IDLE, capabilities);

    service.setAgentConfiguration("agent1", new Properties());
    assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAnAgent", null, null);
    verifyAgent("agent1", IDLE, new Properties());
  }

  @Test
  public void removeAgent() {
    service.setAgentConfiguration("agent1", capabilities);
    assertEquals(1, service.getKnownAgents().size());
    service.setAgentConfiguration("agent2", capabilities);
    service.setAgentState("agent2", UPLOADING);

    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", UNKNOWN, capabilities);
    verifyAgent("agent2", UPLOADING, capabilities);

    try {
      service.removeAgent("agent1");
      assertEquals(1, service.getKnownAgents().size());
      verifyAgent("notAnAgent", null, capabilities);
      verifyAgent("agent1", null, capabilities);
      verifyAgent("agent2", UPLOADING, capabilities);
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
    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", null, capabilities);
    verifyAgent("agent2", UPLOADING, capabilities);
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

    service.setAgentConfiguration("agent", capabilities);
    Properties agentCapabilities;
    try {
      agentCapabilities = service.getAgentCapabilities("agent");
      assertEquals(capabilities, agentCapabilities);
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
    cap1.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + "key", "value");
    cap1.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "key");
    Properties cap2 = new Properties();
    cap2.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + "foo", "bar");
    cap2.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "foo");
    Properties cap3 = new Properties();
    cap3.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + "bam", "bam");
    cap3.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "bam");

    // Setup the two agents and persist them
    service.setAgentState("sticky1", IDLE);
    service.setAgentConfiguration("sticky1", cap1);
    service.setAgentState("sticky2", CAPTURING);
    service.setAgentConfiguration("sticky2", cap2);
    service.setAgentState("sticky3", UPLOADING);
    service.setAgentConfiguration("sticky3", cap3);

    // Make sure they're set right
    assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    assertEquals(IDLE, service.getAgent("sticky1").getState());
    assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    assertEquals(cap3, service.getAgentCapabilities("sticky3"));
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
    assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    assertEquals(IDLE, service.getAgent("sticky1").getState());
    assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    assertEquals(cap3, service.getAgentCapabilities("sticky3"));
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
    Prelude.sleep(100L);
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
