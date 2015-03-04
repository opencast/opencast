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
package org.opencastproject.adminui.endpoint;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Blacklistable;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Period;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Option;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestCaptureAgentsEndpoint extends CaptureAgentsEndpoint {

  private ParticipationManagementDatabase pmService;
  private CaptureAgentStateService captureAgentService;
  private ArrayList<User> users;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  public TestCaptureAgentsEndpoint() throws Exception {

    pmService = EasyMock.createNiceMock(ParticipationManagementDatabase.class);
    captureAgentService = EasyMock.createNiceMock(CaptureAgentStateService.class);

    Map<String, Agent> agents = new HashMap<String, Agent>();
    agents.put("agent1",
            new TestAgent("agent1", "ok", "http://agent1", DateTimeSupport.fromUTC("2014-05-26T15:37:02Z")));
    agents.put("agent2",
            new TestAgent("agent2", "ok", "http://agent2", DateTimeSupport.fromUTC("2014-05-26T15:37:02Z")));
    agents.put("agent3",
            new TestAgent("agent3", "ok", "http://agent3", DateTimeSupport.fromUTC("2014-05-26T15:37:02Z")));
    agents.put("agent4",
            new TestAgent("agent4", "ok", "http://agent4", DateTimeSupport.fromUTC("2014-05-26T15:37:02Z")));

    Room room1 = new Room("Test");
    room1.setId(12L);
    List<Period> periods = new ArrayList<Period>();
    periods.add(new Period(Option.some(12L), new Date(DateTimeSupport.fromUTC("2025-12-12T12:12:12Z")), new Date(
            DateTimeSupport.fromUTC("2025-12-24T12:12:12Z")), Option.<String> none(), Option.<String> none()));
    periods.add(new Period(Option.some(14L), new Date(DateTimeSupport.fromUTC("2026-12-12T12:12:12Z")), new Date(
            DateTimeSupport.fromUTC("2026-12-12T12:12:12Z")), Option.<String> none(), Option.<String> none()));

    List<CaptureAgent> caAgents = new ArrayList<CaptureAgent>();
    caAgents.add(new CaptureAgent(room1, "agent1"));

    List<Blacklist> blacklist = new ArrayList<Blacklist>();
    blacklist.add(new Blacklist(room1, periods));

    EasyMock.expect(pmService.getCaptureAgents()).andReturn(caAgents).anyTimes();
    EasyMock.expect(pmService.getRoom(EasyMock.anyLong())).andReturn(room1).anyTimes();
    EasyMock.expect(pmService.findBlacklists(EasyMock.anyObject(Blacklistable.class))).andReturn(blacklist).anyTimes();

    EasyMock.expect(captureAgentService.getKnownAgents()).andReturn(agents).anyTimes();

    EasyMock.replay(pmService);
    EasyMock.replay(captureAgentService);

    this.setParticipationPersistence(pmService);
    this.setCaptureAgentService(captureAgentService);
  }

  private class TestAgent implements Agent {

    private String name;
    private String state;
    private String url;
    private Long time;

    public TestAgent(String name, String state, String url, Long time) {
      this.name = name;
      this.state = state;
      this.url = url;
      this.time = time;
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
      Properties capabilities = new Properties();
      capabilities.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "microphone");
      return capabilities;

    }

    @Override
    public Properties getConfiguration() {
      return null;
    }

    @Override
    public void setConfiguration(Properties configuration) {

    }

  }

}
