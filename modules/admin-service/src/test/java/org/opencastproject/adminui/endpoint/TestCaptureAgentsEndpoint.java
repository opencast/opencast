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

package org.opencastproject.adminui.endpoint;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentVersion;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestCaptureAgentsEndpoint extends CaptureAgentsEndpoint {

  private final CaptureAgentStateService captureAgentService;
  private ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  public TestCaptureAgentsEndpoint() throws Exception {

    captureAgentService = EasyMock.createNiceMock(CaptureAgentStateService.class);

    Map<String, Agent> agents = new HashMap<String, Agent>();
    agents.put("agent1",
            new TestAgent("agent1", "ok", "http://agent1", DateTimeSupport.fromUTC("2014-05-26T15:37:02Z")));
    agents.put("agent2",
            new TestAgent("agent2", "ok", "http://agent2", DateTimeSupport.fromUTC("2016-05-26T07:07:07Z")));
    agents.put("agent3",
            new TestAgent("agent3", "ok", "http://agent3", DateTimeSupport.fromUTC("2016-06-09T18:00:00Z")));
    agents.put("agent4",
            new TestAgent("agent4", "ok", "http://agent4", DateTimeSupport.fromUTC("2016-06-09T06:00:00Z")));
    //NB: The class below has a ton of if-else dependent on this specific name
    //    Someday we should come back and fix that, but for now this'll do.
    //    Doing it nicely would end up duplicating a *ton* of logic from AgentImpl
    //    ...maybe Agent should be AgentImpl rather than just an interface
    agents.put("pyca-2.x",
            new TestAgent("pyca-2.x", "ok", "http://pyca-2.x", DateTimeSupport.fromUTC("2026-10-30T20:36:46Z")));

    expect(captureAgentService.getKnownAgents()).andStubReturn(agents);
    replay(captureAgentService);

    this.setCaptureAgentService(captureAgentService);
  }

  private class TestAgent implements Agent {

    private final String name;
    private String state;
    private String url;
    private Long time;
    private final boolean isManaged;

    TestAgent(String name, String state, String url, Long time) {
      this.name = name;
      this.state = state;
      this.url = url;
      this.time = time;
      this.isManaged = true;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setState(String newState) {
      state = newState;
    }

    @Override
    public String getState() {
      return state;
    }

    @Override
    public AgentVersion getVersion() {
      if ("pyca-2.x".equals(name)) {
        return AgentVersion.VERSION_2;
      }
      return AgentVersion.VERSION_1;
    }

    @Override
    public void setUrl(String agentUrl) {
      this.url = agentUrl;
    }

    @Override
    public String getUrl() {
      return url;
    }

    @Override
    public void setLastHeardFrom(Long time) {
      this.time = time;
    }

    @Override
    public Long getLastHeardFrom() {
      return time;
    }

    @Override
    public Properties getCapabilities() {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public Properties getConfiguration() {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public void setConfiguration(Properties configuration) {
      throw new RuntimeException("Not implemented");
    }

    public boolean canCaptureLocal() {
      return true;
    }

    public boolean canStartLocalPaused() {
      return false;
    }

    public boolean canStream() {
      return "pyca-2.x".equals(name);
    }

    public boolean canStartStreamPaused() {
      return false;
    }

    public Set<String> getInputs() {
      if ("pyca-2.x".equals(name)) {
        return Set.of("alpha", "beta");
      }
      return Set.of("microphone");
    }

    public Set<String> streamConfigurations() {
      if ("pyca-2.x".equals(name)) {
        return Set.of("a", "b", "c");
      }
      return Set.of();
    }

    public Set<String> devicePositions() {
      if ("pyca-2.x".equals(name)) {
        return Set.of("d", "e", "f");
      }
      return Set.of();
    }

    public Set<String> contentConfigurations() {
      if ("pyca-2.x".equals(name)) {
        return Set.of("g", "h", "i");
      }
      return Set.of();
    }

    public Map<String, String> vendorKeys() {
      if ("pyca-2.x".equals(name)) {
        return Map.of("X-pyca.needsonly", "X- prefix", "X-pyca-thing1", "vendor extensions woo");
      }
      return Map.of();
    }

    public String getVendor() {
      if ("pyca-2.x".equals(name)) {
        return "pyca";
      }
      return "";
    }

    public String getModel() {
      if ("pyca-2.x".equals(name)) {
        return "pyca";
      }
      return "";
    }

    public String getHardware() {
      if ("pyca-2.x".equals(name)) {
        return "git_sha";
      }
      return "";
    }

    public String getFirmware() {
      if ("pyca-2.x".equals(name)) {
        return "git_sha";
      }
      return "";
    }
  }

}
