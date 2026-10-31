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
package org.opencastproject.external.util;

import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.util.DateTimeSupport.toUTC;

import org.opencastproject.capture.admin.api.Agent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class CaptureAgentUtils {

  private static final String JSON_KEY_AGENT_ID = "agent_id";
  private static final String JSON_KEY_STATUS = "status";
  private static final String JSON_KEY_URL = "url";
  private static final String JSON_KEY_INPUTS = "inputs";
  private static final String JSON_KEY_UPDATE = "update";
  private static final String JSON_KEY_VERSION = "version";
  private static final String JSON_KEY_VENDOR = "vendor";
  private static final String JSON_KEY_MODEL = "model";
  private static final String JSON_KEY_HARDWARE = "hardware";
  private static final String JSON_KEY_FIRMWARE = "firmware";
  private static final String JSON_KEY_LOCAL = "local";
  private static final String JSON_KEY_STREAM = "stream";

  private CaptureAgentUtils() {
  }

  /**
   * Generate a JSON Object for the given capture agent
   *
   * @param agent
   *          The capture agent
   * @return A {@link JsonObject} representing the capture agent
   */
  public static JsonObject generateJsonAgent(Agent agent) {
    JsonObject json = new JsonObject();
    json.addProperty(JSON_KEY_STATUS, safeString(agent.getState()));
    json.addProperty(JSON_KEY_AGENT_ID, agent.getName());
    json.addProperty(JSON_KEY_UPDATE, safeString(toUTC(agent.getLastHeardFrom())));
    json.addProperty(JSON_KEY_URL, safeString(agent.getUrl()));
    json.addProperty(JSON_KEY_VENDOR, safeString(agent.getVendor()));
    json.addProperty(JSON_KEY_MODEL, safeString(agent.getModel()));
    json.addProperty(JSON_KEY_HARDWARE, safeString(agent.getHardware()));
    json.addProperty(JSON_KEY_FIRMWARE, safeString(agent.getFirmware()));
    json.addProperty(JSON_KEY_VERSION, agent.getVersion().toString());

    /* Add the core agent configuration data */
    JsonObject localCapabilities = new JsonObject();
    JsonObject streamCapabilities = new JsonObject();
    localCapabilities.addProperty("capture", agent.canCaptureLocal());
    localCapabilities.addProperty("startpaused", agent.canStartLocalPaused());
    streamCapabilities.addProperty("stream", agent.canStream());
    streamCapabilities.addProperty("startpaused", agent.canStartStreamPaused());

    JsonObject config = new JsonObject();
    config.add("local", localCapabilities);
    config.add("stream", streamCapabilities);

    JsonArray inputs = new JsonArray();
    agent.getInputs().forEach(inputs::add);
    config.add(JSON_KEY_INPUTS, inputs);

    Gson gson = new Gson();
    config.add("streams", gson.toJsonTree(
        agent.streamConfigurations()
            .stream()
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toSet())
    ).getAsJsonArray());

    config.add("positions", gson.toJsonTree(
        agent.devicePositions()
            .stream()
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toSet())
    ).getAsJsonArray());

    config.add("contents", gson.toJsonTree(
        agent.contentConfigurations()
            .stream()
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toSet())
    ).getAsJsonArray());

    JsonObject vendorKeys = new JsonObject();
    agent.vendorKeys().forEach(vendorKeys::addProperty);
    config.add("vendorKeys", vendorKeys);

    json.add("config", config);

    return json;
  }
}
