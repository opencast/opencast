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

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentVersion;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final String JSON_KEY_VERSION = "version";
  private static final String JSON_KEY_VENDOR = "vendor";
  private static final String JSON_KEY_MODEL = "model";
  private static final String JSON_KEY_HARDWARE = "hardware";
  private static final String JSON_KEY_FIRMWARE = "firmware";

  private static final String JSON_KEY_CONFIG = "config";
  private static final String JSON_KEY_LOCAL = "local";
  private static final String JSON_KEY_CAPTURE = "capture";
  private static final String JSON_KEY_STARTPAUSED = "startpaused";
  private static final String JSON_KEY_STREAM = "stream";
  private static final String JSON_KEY_INPUTS = "inputs";
  private static final String JSON_KEY_STREAMS = "streams";
  private static final String JSON_KEY_POSITIONS = "positions";
  private static final String JSON_KEY_CONTENTS = "contents";
  private static final String JSON_KEY_VENDORS = "vendorKeys";

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
    result.put(JSON_KEY_VENDOR, agent.getVendor());
    result.put(JSON_KEY_MODEL, agent.getModel());
    result.put(JSON_KEY_HARDWARE, agent.getHardware());
    result.put(JSON_KEY_FIRMWARE, agent.getFirmware());
    result.put(JSON_KEY_VERSION, agent.getVersion().toString());
    JSONObject config =  new JSONObject();

    JSONObject local = new JSONObject();
    local.put(JSON_KEY_CAPTURE, agent.canCaptureLocal());
    local.put(JSON_KEY_STARTPAUSED, agent.canStartLocalPaused());
    config.put(JSON_KEY_LOCAL, local);
    JSONObject stream = new JSONObject();
    stream.put(JSON_KEY_STREAM, agent.canStream());
    stream.put(JSON_KEY_STARTPAUSED, agent.canStartStreamPaused());
    config.put(JSON_KEY_STREAM, stream);

    JSONArray inputs = new JSONArray();
    inputs.addAll(agent.getInputs());
    config.put(JSON_KEY_INPUTS, inputs);

    JSONArray streamConfigs = new JSONArray();
    streamConfigs.addAll(agent.streamConfigurations());
    config.put(JSON_KEY_STREAMS, streamConfigs);

    JSONArray positions = new JSONArray();
    positions.addAll(agent.devicePositions());
    config.put(JSON_KEY_POSITIONS, positions);

    JSONArray content = new JSONArray();
    content.addAll(agent.contentConfigurations());
    config.put(JSON_KEY_CONTENTS, content);

    JSONObject vendorkeys = new JSONObject();
    vendorkeys.putAll(agent.vendorKeys());

    config.put(JSON_KEY_VENDORS, vendorkeys);
    result.put(JSON_KEY_CONFIG, config);

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
    expect(agent.getVendor()).andReturn((String) json.getOrDefault(JSON_KEY_VENDOR, "")).anyTimes();
    expect(agent.getModel()).andReturn((String) json.getOrDefault(JSON_KEY_MODEL, "")).anyTimes();
    expect(agent.getHardware()).andReturn((String) json.getOrDefault(JSON_KEY_HARDWARE, "")).anyTimes();
    expect(agent.getFirmware()).andReturn((String) json.getOrDefault(JSON_KEY_FIRMWARE, "")).anyTimes();
    expect(agent.getLastHeardFrom()).andReturn(updateMillis).anyTimes();
    expect(agent.getUrl()).andReturn((String) json.get(JSON_KEY_URL)).anyTimes();
    expect(agent.getVersion()).andReturn(AgentVersion.fromVersion((String) json.get(JSON_KEY_VERSION))).anyTimes();
    JSONObject config = (JSONObject) json.get(JSON_KEY_CONFIG);

    JSONObject local = (JSONObject) config.getOrDefault(JSON_KEY_LOCAL, new JSONObject());
    // Agents can by default capture locally (ie, 1.x agents), so we assume if this is missing that it's *true*
    // since 1.x agents do not send this data at all and thus it won't be in our input json here
    expect(agent.canCaptureLocal()).andReturn(
        (Boolean) local.getOrDefault(JSON_KEY_CAPTURE ,true)).anyTimes();
    expect(agent.canStartLocalPaused()).andReturn(
        (Boolean) local.getOrDefault(JSON_KEY_STARTPAUSED ,false)).anyTimes();
    JSONObject stream = (JSONObject) config.getOrDefault(JSON_KEY_STREAM, new JSONObject());
    expect(agent.canStream()).andReturn(
        (Boolean) stream.getOrDefault(JSON_KEY_STREAM ,false)).anyTimes();
    expect(agent.canStartStreamPaused()).andReturn(
        (Boolean) stream.getOrDefault(JSON_KEY_STARTPAUSED ,false)).anyTimes();

    expect(agent.getInputs())
        .andReturn(setify((JSONArray) config.getOrDefault(JSON_KEY_INPUTS, new JSONArray()))).anyTimes();
    expect(agent.streamConfigurations())
        .andReturn(setify((JSONArray) config.getOrDefault(JSON_KEY_STREAMS, new JSONArray()))).anyTimes();
    expect(agent.devicePositions())
        .andReturn(setify((JSONArray) config.getOrDefault(JSON_KEY_POSITIONS, new JSONArray()))).anyTimes();
    expect(agent.contentConfigurations())
        .andReturn(setify((JSONArray) config.getOrDefault(JSON_KEY_CONTENTS, new JSONArray()))).anyTimes();
    expect(agent.vendorKeys()).andReturn(Map.of()).anyTimes();
    replay(agent);
    return agent;
  }

  private static Set<String> setify(JSONArray array) {
    return (Set<String>) array.stream()
        .map(obj -> (String) obj)
        .collect(Collectors.toSet());
  }

  private static String readResource(String path) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(Paths.get(TestCaptureAgentsEndpoint
        .class.getResource(path).toURI())), StandardCharsets.UTF_8);
  }
}
