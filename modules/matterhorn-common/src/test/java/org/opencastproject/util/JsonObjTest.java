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
package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.JsonVal.asBoolean;
import static org.opencastproject.util.JsonVal.asInteger;
import static org.opencastproject.util.JsonVal.asString;

import org.junit.Test;

public class JsonObjTest {
  private static final String JSON = "{\"service\": {\n" + "\t\t\t\"type\": \"org.opencastproject.analytics\",\n"
          + "\t\t\t\"host\": \"http:\\/\\/localhost:8080\",\n" + "\t\t\t\"path\": \"\\/analytics-rest\",\n"
          + "\t\t\t\"active\": true,\n" + "\t\t\t\"online\": true,\n" + "\t\t\t\"maintenance\": false,\n"
          + "\t\t\t\"jobproducer\": false,\n" + "\t\t\t\"onlinefrom\": \"2013-11-04T17:26:1201:00\",\n"
          + "\t\t\t\"service_state\": \"NORMAL\",\n" + "\t\t\t\"state_changed\": \"2013-10-16T15:54:3702:00\",\n"
          + "\t\t\t\"error_state_trigger\": 0,\n" + "\t\t\t\"warning_state_trigger\": 0\n"
          + "\t\t\t\"array\": [1, 2, true, \"val\"]\n" + "\t\t}}";

  @Test
  public void testParse() {
    final JsonObj json = JsonObj.jsonObj(JSON);
    assertEquals("org.opencastproject.analytics", json.getObj("service").get(String.class, "type"));
    assertEquals(true, json.obj("service").get(Boolean.class, "active"));
    assertEquals(true, json.obj("service").val("active").as(asBoolean));
    assertEquals("http://localhost:8080", json.obj("service").val("host").as(asString));
    assertEquals(new Integer(1), json.obj("service").arr("array").val(0).as(asInteger));
  }

  @Test(expected = Exception.class)
  public void testTypeMismatch() {
    final JsonObj json = JsonObj.jsonObj(JSON);
    json.getObj("service").get(String.class, "active");
  }
}
