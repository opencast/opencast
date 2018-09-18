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

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.RestServiceTestEnv;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowDefinitionsEndpointTest {

  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(),
          TestWorkflowDefinitionsEndpoint.class);

  private static final JSONParser parser = new JSONParser();

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  @Test
  public void testGetWorkflowDefinitions() throws Exception {
    final String response = given().expect().statusCode(SC_OK).when().get(env.host("/")).asString();

    final JSONArray json = (JSONArray) parser.parse(response);
    assertEquals(2, json.size());

    // Workflow Definition 1
    final JSONObject wd1 = (JSONObject) json.get(0);
    assertEquals("example1", wd1.get("identifier"));
    assertEquals("Example workflow", wd1.get("title"));
    assertEquals("Example workflow definition", wd1.get("description"));
    assertArrayEquals(new String[] { "archive", "my-tag" }, ((JSONArray) wd1.get("tags")).toArray());
    assertFalse(wd1.containsKey("configuration_panel"));
    assertFalse(wd1.containsKey("operations"));

    // Workflow Definition 2
    final JSONObject wd2 = (JSONObject) json.get(1);
    assertEquals("example2", wd2.get("identifier"));
    assertEquals("Another workflow", wd2.get("title"));
    assertEquals("Example workflow definition", wd2.get("description"));
    assertArrayEquals(new String[] {}, ((JSONArray) wd2.get("tags")).toArray());
    assertFalse(wd2.containsKey("configuration_panel"));
    assertFalse(wd2.containsKey("operations"));
  }

  @Test
  public void testGetWorkflowDefinitionsIncludingOptionalValues() throws Exception {
    final String response = given().queryParam("withoperations", "true")
                                   .queryParam("withconfigurationpanel", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/"))
                                   .asString();

    final JSONArray json = (JSONArray) parser.parse(response);
    assertEquals(2, json.size());

    // Workflow Definition 1
    final JSONObject wd1 = (JSONObject) json.get(0);
    assertEquals("<h3>Config</h3>", wd1.get("configuration_panel"));
    final JSONArray ops1 = (JSONArray) wd1.get("operations");
    assertEquals(1, ops1.size());

    // Workflow Operation Definition 1
    final JSONObject wod1 = (JSONObject) ops1.get(0);
    assertEquals("my-op", wod1.get("operation"));
    assertEquals("Example Operation", wod1.get("description"));
    assertTrue((Boolean) wod1.get("fail_workflow_on_error"));
    assertEquals("${letfail}", wod1.get("if"));
    assertEquals("", wod1.get("unless"));
    assertEquals("fail", wod1.get("error_handler_workflow"));
    assertEquals(42L, wod1.get("max_attempts"));
    assertEquals("hold", wod1.get("retry_strategy"));
    final JSONObject cfg1 = (JSONObject) wod1.get("configuration");
    assertEquals("value", cfg1.get("key"));
    assertEquals("bar", cfg1.get("foo"));

    // Workflow Definition 2
    final JSONObject wd2 = (JSONObject) json.get(1);
    assertEquals("<h3>Config2</h3>", wd2.get("configuration_panel"));
    final JSONArray ops2 = (JSONArray) wd2.get("operations");
    assertEquals(1, ops2.size());

    // Workflow Operation Definition 1
    final JSONObject wod2 = (JSONObject) ops2.get(0);
    assertEquals("my-op2", wod2.get("operation"));
    assertEquals("Example Operation2", wod2.get("description"));
    assertFalse((Boolean) wod2.get("fail_workflow_on_error"));
    assertEquals("", wod2.get("if"));
    assertEquals("${letfail}", wod2.get("unless"));
    assertEquals("", wod2.get("error_handler_workflow"));
    assertEquals(0L, wod2.get("max_attempts"));
    assertEquals("retry", wod2.get("retry_strategy"));
    final JSONObject cfg2 = (JSONObject) wod2.get("configuration");
    assertEquals("1234", cfg2.get("abcd"));
  }

  @Test
  public void testGetWorkflowDefinitionIncludingOptionalValues() throws Exception {
    final String response = given().queryParam("withoperations", "true")
                                   .queryParam("withconfigurationpanel", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/example1"))
                                   .asString();

    final JSONObject wd = (JSONObject) parser.parse(response);
    assertEquals("example1", wd.get("identifier"));
    assertEquals("Example workflow", wd.get("title"));
    assertEquals("Example workflow definition", wd.get("description"));
    assertArrayEquals(new String[] { "archive", "my-tag" }, ((JSONArray) wd.get("tags")).toArray());
    assertEquals("<h3>Config</h3>", wd.get("configuration_panel"));
    final JSONArray ops = (JSONArray) wd.get("operations");
    assertEquals(1, ops.size());

    // Workflow Operation Definition
    final JSONObject wod = (JSONObject) ops.get(0);
    assertEquals("my-op", wod.get("operation"));
    assertEquals("Example Operation", wod.get("description"));
    assertTrue((Boolean) wod.get("fail_workflow_on_error"));
    assertEquals("${letfail}", wod.get("if"));
    assertEquals("", wod.get("unless"));
    assertEquals("fail", wod.get("error_handler_workflow"));
    assertEquals(42L, wod.get("max_attempts"));
    assertEquals("hold", wod.get("retry_strategy"));
    final JSONObject cfg = (JSONObject) wod.get("configuration");
    assertEquals("value", cfg.get("key"));
    assertEquals("bar", cfg.get("foo"));
  }
}
