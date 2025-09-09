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

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class CaptureAgentUtils {

  private static final String JSON_KEY_AGENT_ID = "agent_id";
  private static final String JSON_KEY_STATUS = "status";
  private static final String JSON_KEY_URL = "url";
  private static final String JSON_KEY_INPUTS = "inputs";
  private static final String JSON_KEY_UPDATE = "update";


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
    String devices = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
    json.addProperty(JSON_KEY_STATUS, safeString(agent.getState()));
    json.addProperty(JSON_KEY_AGENT_ID, agent.getName());
    json.addProperty(JSON_KEY_UPDATE, safeString(toUTC(agent.getLastHeardFrom())));
    json.addProperty(JSON_KEY_URL, safeString(agent.getUrl()));
    JsonArray inputs = new JsonArray();
    if (devices != null && !devices.trim().isEmpty()) {
      for (String device : devices.split(",")) {
        inputs.add(new JsonPrimitive(device.trim()));
      }
    }
    json.add("inputs", inputs);
    return json;
  }
}
