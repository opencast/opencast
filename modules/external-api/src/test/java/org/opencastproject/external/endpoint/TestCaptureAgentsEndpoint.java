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
package org.opencastproject.external.endpoint;

import static java.time.ZoneOffset.UTC;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.capture.CaptureParameters.CAPTURE_DEVICE_NAMES;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

@Path("/")
public class TestCaptureAgentsEndpoint extends CaptureAgentsEndpoint {

  public static final String UNKNOWN_AGENT = "unknown";

  private static final String AGENTS_PATH = "/agents/agents.json";

  private static final String JSON_KEY_UPDATE = "update";
  private static final String JSON_KEY_AGENT_ID = "agent_id";
  private static final String JSON_KEY_STATUS = "status";
  private static final String JSON_KEY_URL = "url";
  private static final String JSON_KEY_INPUTS = "inputs";

  public TestCaptureAgentsEndpoint() throws Exception {
    final CaptureAgentStateService agentStateService = createMock(CaptureAgentStateService.class);

    List<Agent> agents = loadAgents();
    for (Agent agent : agents) {
      expect(agentStateService.getAgent(eq(agent.getName()))).andReturn(agent).anyTimes();
    }
    expect(agentStateService.getAgent(eq(UNKNOWN_AGENT))).andReturn(null).anyTimes();
    expect(agentStateService.getKnownAgents()).andReturn(
        agents.stream().collect(Collectors.toMap(Agent::getName, a -> a))).anyTimes();

    replay(agentStateService);
    setAgentStateService(agentStateService);
  }

  @SuppressWarnings("unchecked")
  public static List<Agent> loadAgents() throws IOException, URISyntaxException, ParseException {
    JSONParser parser = new JSONParser();
    JSONArray json = (JSONArray) parser.parse(readResource(AGENTS_PATH));
    return (List<Agent>) json.stream()
        .map(j -> mockAgent((JSONObject) j))
        .collect(Collectors.toList());
  }

  public static JSONObject toJson(Agent agent) {
    final JSONObject result = new JSONObject();
    result.put(JSON_KEY_UPDATE, DateTimeFormatter.ISO_DATE_TIME.format(
        Instant.ofEpochMilli(agent.getLastHeardFrom()).atZone(UTC)));
    result.put(JSON_KEY_AGENT_ID, agent.getName());
    result.put(JSON_KEY_STATUS, agent.getState());
    result.put(JSON_KEY_URL, agent.getUrl());
    JSONArray inputs = new JSONArray();
    inputs.addAll(Arrays.asList(agent.getCapabilities().getProperty(CAPTURE_DEVICE_NAMES).split(",")));
    result.put(JSON_KEY_INPUTS, inputs);
    return result;
  }

  public static JSONArray toJson(List<Agent> agents) {
    JSONArray result = new JSONArray();
    result.addAll(agents.stream().map(a -> toJson(a)).collect(Collectors.toList()));
    return result;
  }

  private static Agent mockAgent(JSONObject json) {
    Agent agent = createNiceMock(Agent.class);
    final String update = (String) json.get(JSON_KEY_UPDATE);
    long updateMillis = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(update)).toEpochMilli();
    expect(agent.getName()).andReturn((String) json.get(JSON_KEY_AGENT_ID)).anyTimes();
    expect(agent.getState()).andReturn((String) json.get(JSON_KEY_STATUS)).anyTimes();
    expect(agent.getLastHeardFrom()).andReturn(updateMillis).anyTimes();
    expect(agent.getUrl()).andReturn((String) json.get(JSON_KEY_URL)).anyTimes();
    Properties capabilities = new Properties();
    capabilities.setProperty(CAPTURE_DEVICE_NAMES, String.join(",", ((JSONArray) json.get(JSON_KEY_INPUTS))));
    expect(agent.getCapabilities()).andReturn(capabilities).anyTimes();
    replay(agent);
    return agent;
  }

  private static String readResource(String path) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(Paths.get(TestCaptureAgentsEndpoint
        .class.getResource(path).toURI())), StandardCharsets.UTF_8);
  }
}
