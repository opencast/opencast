/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.admin.impl;

import static junit.framework.Assert.fail;
import static org.opencastproject.capture.admin.api.AgentState.IDLE;
import static org.opencastproject.capture.admin.api.AgentState.UNKNOWN;
import static org.opencastproject.capture.admin.api.RecordingState.CAPTURING;
import static org.opencastproject.capture.admin.api.RecordingState.UPLOADING;
import static org.opencastproject.capture.admin.api.RecordingState.UPLOAD_FINISHED;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateServiceImpl service = null;
  private Properties capabilities;
  private ComboPooledDataSource pooledDataSource = null;
  private long timestamp = -1L;
  private long random = -1L;

  @Before
  public void setUp() throws Exception {
    timestamp = System.currentTimeMillis();
    random = Math.abs(new Random().nextLong());
    setupService();

    capabilities = new Properties();
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "CAMERA", "/dev/video0");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN", "/dev/video1");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO", "hw:0");
    capabilities.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "CAMERA,SCREEN,AUDIO");
  }

  private void setupService() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + timestamp + "_" + random);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new CaptureAgentStateServiceImpl();
    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);

    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowInstances((WorkflowQuery) EasyMock.anyObject()))
            .andReturn(new WorkflowSetImpl()).anyTimes();
    EasyMock.replay(workflowService);
    service.setWorkflowService(workflowService);

    DefaultOrganization organization = new DefaultOrganization();

    HashSet<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, organization, ""));
    User user = new JaxbUser("testuser", "test", organization, roles);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    service.activate(null);
  }

  @After
  public void tearDown() {
    service.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void nonExistantAgent() {
    try {
      service.getAgent("doesNotExist");
      Assert.fail("Agent has been found");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void noAgents() {
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  @Test
  public void badAgentStates() throws NotFoundException {
    try {
      service.setAgentState(null, "something");
      Assert.assertEquals(0, service.getKnownAgents().size());
      Assert.fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }

    try {
      Assert.assertEquals(0, service.getKnownAgents().size());
      service.setAgentState("", "something");
      Assert.fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }

    try {
      Assert.assertEquals(0, service.getKnownAgents().size());
      service.setAgentState("something", null);
      Assert.fail("IllegalArgument not thrown!");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void badAgentCapabilities() {
    try {
      service.setAgentConfiguration(null, capabilities);
      Assert.fail("Null agent name accepted");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(0, service.getKnownAgents().size());

    try {
      service.setAgentConfiguration("", capabilities);
      Assert.fail("Empty agent name accepted");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(0, service.getKnownAgents().size());

    try {
      service.setAgentState("something", null);
      Assert.fail("Null agent state accepted");
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  private void verifyAgent(String name, String state, Properties caps) {
    try {
      Agent agent = service.getAgent(name);
      Assert.assertEquals(name, agent.getName());
      Assert.assertEquals(state, agent.getState());
      Assert.assertEquals(caps, agent.getCapabilities());
    } catch (NotFoundException e) {
      if (state != null)
        Assert.fail();
    }
  }

  @Test
  public void oneAgentState() {
    service.setAgentState("agent1", IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", IDLE, new Properties());

    service.setAgentState("agent1", CAPTURING);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", CAPTURING, new Properties());
  }

  @Test
  public void oneAgentCapabilities() {
    service.setAgentConfiguration("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", UNKNOWN, capabilities);

    service.setAgentState("agent1", IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", IDLE, capabilities);

    service.setAgentConfiguration("agent1", new Properties());
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAnAgent", null, null);
    verifyAgent("agent1", IDLE, new Properties());
  }

  @Test
  public void removeAgent() {
    service.setAgentConfiguration("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());
    service.setAgentConfiguration("agent2", capabilities);
    service.setAgentState("agent2", UPLOADING);

    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", UNKNOWN, capabilities);
    verifyAgent("agent2", UPLOADING, capabilities);

    try {
      service.removeAgent("agent1");
      Assert.assertEquals(1, service.getKnownAgents().size());
      verifyAgent("notAnAgent", null, capabilities);
      verifyAgent("agent1", null, capabilities);
      verifyAgent("agent2", UPLOADING, capabilities);
    } catch (NotFoundException e) {
      Assert.fail();
    }

    try {
      service.removeAgent("notAnAgent");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(1, service.getKnownAgents().size());
    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", null, capabilities);
    verifyAgent("agent2", UPLOADING, capabilities);
  }

  @Test
  public void agentCapabilities() {
    try {
      service.getAgentCapabilities("agent");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    try {
      service.getAgentCapabilities("NotAgent");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    service.setAgentConfiguration("agent", capabilities);
    Properties agentCapabilities;
    try {
      agentCapabilities = service.getAgentCapabilities("agent");
      Assert.assertEquals(capabilities, agentCapabilities);
    } catch (NotFoundException e) {
      Assert.fail();
    }
    try {
      service.getAgentCapabilities("NotAgent");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void stickyAgents() throws Exception {
    Assert.assertEquals(0, service.getKnownAgents().size());

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
    Assert.assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    Assert.assertEquals(IDLE, service.getAgent("sticky1").getState());
    Assert.assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    Assert.assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    Assert.assertEquals(cap3, service.getAgentCapabilities("sticky3"));
    Assert.assertEquals(UPLOADING, service.getAgent("sticky3").getState());
    try {
      service.getAgentCapabilities("sticky4");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    try {
      service.getAgent("sticky4");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    // Shut down the service completely
    service.deactivate();
    service = null;

    // Restart the service with the same configuration as before
    setupService();

    Assert.assertEquals(3, service.getKnownAgents().size());

    // The agents should still be there
    Assert.assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    Assert.assertEquals(IDLE, service.getAgent("sticky1").getState());
    Assert.assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    Assert.assertEquals(CAPTURING, service.getAgent("sticky2").getState());
    Assert.assertEquals(cap3, service.getAgentCapabilities("sticky3"));
    Assert.assertEquals(UPLOADING, service.getAgent("sticky3").getState());
    try {
      service.getAgentCapabilities("sticky4");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    try {
      service.getAgent("sticky4");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void nonExistantRecording() {
    try {
      service.getRecordingState("doesNotExist");
      Assert.fail("Non existing recording has been found");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void badRecordingData() {
    try {
      service.setRecordingState(null, CAPTURING);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(0, service.getKnownRecordings().size());

    try {
      service.setRecordingState("", IDLE);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(0, service.getKnownRecordings().size());

    Assert.assertFalse(service.setRecordingState("something", "bad_state"));
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  @Test
  public void noRecordings() {
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  private void verifyRecording(String id, String state) {
    if (state == null) {
      try {
        service.getRecordingState(id);
        Assert.fail("");
      } catch (NotFoundException e) {
        Assert.assertNotNull(e);
      }
    } else {
      try {
        Recording recording = service.getRecordingState(id);
        Assert.assertEquals(id, recording.getID());
        Assert.assertEquals(state, recording.getState());
      } catch (NotFoundException e) {
        Assert.fail("");
      }
    }
  }

  @Test
  public void oneRecording() {
    service.setRecordingState("Recording1", UPLOAD_FINISHED);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording("Recording1", UPLOAD_FINISHED);

    service.setRecordingState("Recording1", CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording("Recording1", CAPTURING);
  }

  @Test
  public void removeRecording() {
    service.setRecordingState("Recording1", CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());
    service.setRecordingState("Recording2", UPLOADING);
    Assert.assertEquals(2, service.getKnownRecordings().size());

    verifyRecording("notAnRecording", null);
    verifyRecording("Recording1", CAPTURING);
    verifyRecording("Recording2", UPLOADING);

    try {
      service.removeRecording("Recording1");
    } catch (NotFoundException e) {
      Assert.fail();
    }
    try {
      service.removeRecording("asdfasdf");
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
    Assert.assertEquals(1, service.getKnownRecordings().size());
    verifyRecording("notAnRecording", null);
    verifyRecording("Recording1", null);
    verifyRecording("Recording2", UPLOADING);
  }

  @Test
  public void testAgentVisibility() throws Exception {
    // Create a new capture agent called "visibility"
    String agentName = "visibility";
    service.setAgentState(agentName, IDLE);

    // Ensure we can see it
    Assert.assertEquals(1, service.getKnownAgents().size());

    // Set the roles allowed to use this agent
    Set<String> roles = new HashSet<String>();
    roles.add("a_role_we_do_not_have");
    AgentImpl agent = (AgentImpl) service.getAgent(agentName);
    agent.setSchedulerRoles(roles);
    service.updateAgentInDatabase(agent);

    // Since we are an organizational admin, we should still see the agent
    Assert.assertEquals(1, service.getKnownAgents().size());

    // Use a security service that identifies us as a non-administrative user
    DefaultOrganization organization = new DefaultOrganization();
    HashSet<JaxbRole> roleSet = new HashSet<JaxbRole>();
    roleSet.add(new JaxbRole("ROLE_NOT_ADMIN", organization, ""));
    User user = new JaxbUser("testuser", "test", organization, roleSet);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    // Ensure we can no longer see the agent, since we don't have an administrative role
    Assert.assertEquals(0, service.getKnownAgents().size());

    // TODO: Do we need to enforce access strictly? If someone asks for an agent by name, but they do not have the
    // appropriate scheduler role, should we throw UnauthorizedException?
  }

  @Test
  public void testManagedServiceFactory() throws Exception {
    // Make sure we can register a capture agent with specific scheduler roles
    String pid = UUID.randomUUID().toString();
    Dictionary<String, String> properties = new Hashtable<String, String>();
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
    //See MH-10031
    String name = "agent1";
    Long lastHeardFrom = 0L;
    Agent agent = null;
    service.setAgentState(name, IDLE);

    agent = service.getAgent(name);
    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, CAPTURING);
    agent = service.getAgent(name);
    Assert.assertTrue(lastHeardFrom < agent.getLastHeardFrom());

    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);
    Assert.assertTrue(lastHeardFrom < agent.getLastHeardFrom());

    lastHeardFrom = agent.getLastHeardFrom();
    service.setAgentState(name, IDLE);
    agent = service.getAgent(name);
    Assert.assertTrue(lastHeardFrom < agent.getLastHeardFrom());
  }


}
