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

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowDefinitionsEndpointTest {

  private static final RestServiceTestEnv env = testEnvForClasses(TestWorkflowDefinitionsEndpoint.class);


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

    final JsonArray json = JsonParser.parseString(response).getAsJsonArray();
    assertEquals(2, json.size());

    // Workflow Definition 1
    final JsonObject wd1 = json.get(0).getAsJsonObject();
    assertEquals("example1", wd1.get("identifier").getAsString());
    assertEquals("Example workflow", wd1.get("title").getAsString());
    assertEquals("Example workflow definition", wd1.get("description").getAsString());
    assertArrayEquals(new String[] { "archive", "my-tag" }, wd1.getAsJsonArray("tags").asList().stream()
            .map(JsonElement::getAsString).toArray(String[]::new));
    assertFalse(wd1.has("configuration_panel_json"));
    assertFalse(wd1.has("operations"));

    // Workflow Definition 2
    final JsonObject wd2 = json.get(1).getAsJsonObject();
    assertEquals("example2", wd2.get("identifier").getAsString());
    assertEquals("Another workflow", wd2.get("title").getAsString());
    assertEquals("Example workflow definition", wd2.get("description").getAsString());
    assertArrayEquals(new String[] {}, wd2.getAsJsonArray("tags").asList().stream()
            .map(JsonElement::getAsString).toArray(String[]::new));
    assertFalse(wd2.has("configuration_panel_json"));
    assertFalse(wd2.has("operations"));
  }

  @Test
  public void testGetWorkflowDefinitionsIncludingOptionalValues() throws Exception {
    final String response = given().queryParam("withoperations", "true")
                                   .queryParam("withconfigurationpaneljson", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/"))
                                   .asString();

    final JsonArray json = JsonParser.parseString(response).getAsJsonArray();
    assertEquals(2, json.size());

    // Workflow Definition 1
    final JsonObject wd1 = json.get(0).getAsJsonObject();
    assertThat(wd1.get("configuration_panel_json").getAsString(), startsWith("[{ \"legend\": \"Config\""));
    final JsonArray ops1 = wd1.getAsJsonArray("operations");
    assertEquals(1, ops1.size());

    // Workflow Operation Definition 1
    final JsonObject wod1 = ops1.get(0).getAsJsonObject();
    assertEquals("my-op", wod1.get("operation").getAsString());
    assertEquals("Example Operation", wod1.get("description").getAsString());
    assertTrue(wod1.get("fail_workflow_on_error").getAsBoolean());
    assertEquals("${letfail}", wod1.get("if").getAsString());
    assertEquals("", wod1.get("unless").getAsString());
    assertEquals("fail", wod1.get("error_handler_workflow").getAsString());
    assertEquals(42L, wod1.get("max_attempts").getAsLong());
    assertEquals("hold", wod1.get("retry_strategy").getAsString());
    final JsonObject cfg1 = wod1.getAsJsonObject("configuration");
    assertEquals("value", cfg1.get("key").getAsString());
    assertEquals("bar", cfg1.get("foo").getAsString());

    // Workflow Definition 2
    final JsonObject wd2 = json.get(1).getAsJsonObject();
    assertThat(wd2.get("configuration_panel_json").getAsString(), startsWith("[{ \"legend\": \"Config2\""));
    final JsonArray ops2 = wd2.getAsJsonArray("operations");
    assertEquals(1, ops2.size());

    // Workflow Operation Definition 1
    final JsonObject wod2 = ops2.get(0).getAsJsonObject();
    assertEquals("my-op2", wod2.get("operation").getAsString());
    assertEquals("Example Operation2", wod2.get("description").getAsString());
    assertFalse(wod2.get("fail_workflow_on_error").getAsBoolean());
    assertEquals("", wod2.get("if").getAsString());
    assertEquals("${letfail}", wod2.get("unless").getAsString());
    assertEquals("", wod2.get("error_handler_workflow").getAsString());
    assertEquals(0L, wod2.get("max_attempts").getAsLong());
    assertEquals("retry", wod2.get("retry_strategy").getAsString());
    final JsonObject cfg2 = wod2.getAsJsonObject("configuration");
    assertEquals("1234", cfg2.get("abcd").getAsString());
  }

  @Test
  public void testGetWorkflowDefinitionIncludingOptionalValues() throws Exception {
    final String response = given().queryParam("withoperations", "true")
                                   .queryParam("withconfigurationpaneljson", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/example1"))
                                   .asString();

    final JsonObject wd = JsonParser.parseString(response).getAsJsonObject();
    assertEquals("example1", wd.get("identifier").getAsString());
    assertEquals("Example workflow", wd.get("title").getAsString());
    assertEquals("Example workflow definition", wd.get("description").getAsString());
    assertArrayEquals(new String[] { "archive", "my-tag" }, wd.getAsJsonArray("tags").asList().stream()
            .map(JsonElement::getAsString).toArray(String[]::new));
    assertThat(wd.get("configuration_panel_json").getAsString(), startsWith("[{ \"legend\": \"Config\""));
    final JsonArray ops = wd.getAsJsonArray("operations");
    assertEquals(1, ops.size());

    // Workflow Operation Definition
    final JsonObject wod = ops.get(0).getAsJsonObject();
    assertEquals("my-op", wod.get("operation").getAsString());
    assertEquals("Example Operation", wod.get("description").getAsString());
    assertTrue(wod.get("fail_workflow_on_error").getAsBoolean());
    assertEquals("${letfail}", wod.get("if").getAsString());
    assertEquals("", wod.get("unless").getAsString());
    assertEquals("fail", wod.get("error_handler_workflow").getAsString());
    assertEquals(42L, wod.get("max_attempts").getAsLong());
    assertEquals("hold", wod.get("retry_strategy").getAsString());
    final JsonObject cfg = wod.getAsJsonObject("configuration");
    assertEquals("value", cfg.get("key").getAsString());
    assertEquals("bar", cfg.get("foo").getAsString());
  }
}
