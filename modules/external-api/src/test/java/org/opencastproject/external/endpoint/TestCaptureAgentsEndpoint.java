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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

  public static List<Agent> loadAgents() throws IOException, URISyntaxException {
    JsonArray json = JsonParser.parseString(readResource(AGENTS_PATH)).getAsJsonArray();
    return json.asList().stream()
        .map(j -> mockAgent(j.getAsJsonObject()))
        .collect(Collectors.toList());
  }

  public static JsonObject toJson(Agent agent) {
    final JsonObject result = new JsonObject();
    result.addProperty(JSON_KEY_UPDATE, DateTimeFormatter.ISO_DATE_TIME.format(
        Instant.ofEpochMilli(agent.getLastHeardFrom()).atZone(UTC)));
    result.addProperty(JSON_KEY_AGENT_ID, agent.getName());
    result.addProperty(JSON_KEY_STATUS, agent.getState());
    result.addProperty(JSON_KEY_URL, agent.getUrl());
    JsonArray inputs = new JsonArray();
    Arrays.asList(agent.getCapabilities().getProperty(CAPTURE_DEVICE_NAMES).split(",")).forEach(inputs::add);
    result.add(JSON_KEY_INPUTS, inputs);
    return result;
  }

  public static JsonArray toJson(List<Agent> agents) {
    JsonArray result = new JsonArray();
    agents.stream().map(a -> toJson(a)).forEach(result::add);
    return result;
  }

  private static Agent mockAgent(JsonObject json) {
    Agent agent = createNiceMock(Agent.class);
    final String update = json.get(JSON_KEY_UPDATE).getAsString();
    long updateMillis = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(update)).toEpochMilli();
    expect(agent.getName()).andReturn(json.get(JSON_KEY_AGENT_ID).getAsString()).anyTimes();
    expect(agent.getState()).andReturn(json.get(JSON_KEY_STATUS).getAsString()).anyTimes();
    expect(agent.getLastHeardFrom()).andReturn(updateMillis).anyTimes();
    expect(agent.getUrl()).andReturn(json.get(JSON_KEY_URL).getAsString()).anyTimes();
    Properties capabilities = new Properties();
    List<String> inputs = new ArrayList<>();
    json.getAsJsonArray(JSON_KEY_INPUTS).forEach(input -> inputs.add(input.getAsString()));
    capabilities.setProperty(CAPTURE_DEVICE_NAMES, String.join(",", inputs));
    expect(agent.getCapabilities()).andReturn(capabilities).anyTimes();
    replay(agent);
    return agent;
  }

  private static String readResource(String path) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(Paths.get(TestCaptureAgentsEndpoint
        .class.getResource(path).toURI())), StandardCharsets.UTF_8);
  }
}
