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
package org.opencastproject.external.util;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.util.DateTimeSupport.toUTC;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
   * @return A {@link JValue} representing the capture agent
   */
  public static JValue generateJsonAgent(Agent agent) {
    List<Field> fields = new ArrayList<>();
    String devices = (String) agent.getCapabilities().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
    fields.add(f(JSON_KEY_STATUS, v(agent.getState(), Jsons.BLANK)));
    fields.add(f(JSON_KEY_AGENT_ID, v(agent.getName())));
    fields.add(f(JSON_KEY_UPDATE, v(toUTC(agent.getLastHeardFrom()), Jsons.BLANK)));
    fields.add(f(JSON_KEY_URL, v(agent.getUrl(), Jsons.BLANK)));
    fields.add(f(JSON_KEY_INPUTS, (StringUtils.isEmpty(devices)) ? arr() : arr(devices.split(","))));
    return obj(fields);
  }
}
