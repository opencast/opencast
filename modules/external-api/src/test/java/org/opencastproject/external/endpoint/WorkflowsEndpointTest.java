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
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
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

public class WorkflowsEndpointTest {

  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestWorkflowsEndpoint.class);

  private static final JSONParser parser = new JSONParser();

  private static final long RUNNING_WORKFLOW_ID = 84L;
  private static final long STOPPED_WORKFLOW_ID = 42L;
  private static final long MISSING_WORKFLOW_ID = 24L;
  private static final long UNAUTHORIZED_WORKFLOW_ID = 12L;

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  // GET /

  @Test
  public void testGetWorkflows() throws Exception {
    final String response = given().queryParam("withoperations", "true")
                                   .queryParam("withconfiguration", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/"))
                                   .asString();

    final JSONArray json = (JSONArray) parser.parse(response);
    assertEquals(2, json.size());

    assertRunningWorkflow((JSONObject) json.get(0));
    assertStoppedWorkflow((JSONObject) json.get(1));
  }

  private void assertRunningWorkflow(JSONObject wi) {
    assertEquals(RUNNING_WORKFLOW_ID, wi.get("identifier"));
    assertEquals("Running Workflow", wi.get("title"));
    assertEquals("A running workflow", wi.get("description"));
    assertEquals("fast", wi.get("workflow_definition_identifier"));
    assertEquals("905672ed-181c-4d60-b7cd-02758f61e713", wi.get("event_identifier"));
    assertEquals("running", wi.get("state"));
    final JSONObject wiCfg = (JSONObject) wi.get("configuration");
    assertEquals("5678", wiCfg.get("efgh"));

    final JSONArray ops = (JSONArray) wi.get("operations");
    assertEquals(1, ops.size());

    final JSONObject woi = (JSONObject) ops.get(0);
    assertEquals(1234L, woi.get("identifier"));
    assertEquals("my-op", woi.get("operation"));
    assertEquals("Example Operation", woi.get("description"));
    assertEquals("running", woi.get("state"));
    assertEquals(20L, woi.get("time_in_queue"));
    assertEquals("http://localhost:8080", woi.get("host"));
    assertEquals("${letfail}", woi.get("if"));
    assertEquals("", woi.get("unless"));
    assertEquals(true, woi.get("fail_workflow_on_error"));
    assertEquals("fail", woi.get("error_handler_workflow"));
    assertEquals("retry", woi.get("retry_strategy"));
    assertEquals(42L, woi.get("max_attempts"));
    assertEquals(1L, woi.get("failed_attempts"));
    assertEquals("2018-01-01T12:00:00Z", woi.get("start"));
    assertEquals("", woi.get("completion"));
    final JSONObject woiCfg = (JSONObject) woi.get("configuration");
    assertEquals("value", woiCfg.get("key"));
    assertEquals("bar", woiCfg.get("foo"));
  }

  private void assertStoppedWorkflow(JSONObject wi) {
    assertEquals(STOPPED_WORKFLOW_ID, wi.get("identifier"));
    assertEquals("Stopped Workflow", wi.get("title"));
    assertEquals("A stopped workflow", wi.get("description"));
    assertEquals("fast", wi.get("workflow_definition_identifier"));
    assertEquals("905672ed-181c-4d60-b7cd-02758f61e713", wi.get("event_identifier"));
    assertEquals("stopped", wi.get("state"));
    final JSONObject wiCfg = (JSONObject) wi.get("configuration");
    assertEquals("9000", wiCfg.get("ijklm"));

    final JSONArray ops = (JSONArray) wi.get("operations");
    assertEquals(1, ops.size());

    final JSONObject woi = (JSONObject) ops.get(0);
    assertEquals(5678L, woi.get("identifier"));
    assertEquals("my-op2", woi.get("operation"));
    assertEquals("Example Operation2", woi.get("description"));
    assertEquals("succeeded", woi.get("state"));
    assertEquals(30L, woi.get("time_in_queue"));
    assertEquals("http://localhost:8080", woi.get("host"));
    assertEquals("", woi.get("if"));
    assertEquals("${letfail}", woi.get("unless"));
    assertEquals(false, woi.get("fail_workflow_on_error"));
    assertEquals("", woi.get("error_handler_workflow"));
    assertEquals("hold", woi.get("retry_strategy"));
    assertEquals(0L, woi.get("max_attempts"));
    assertEquals(0L, woi.get("failed_attempts"));
    assertEquals("2018-02-03T12:00:00Z", woi.get("start"));
    assertEquals("2018-02-03T13:14:15Z", woi.get("completion"));
    final JSONObject woiCfg = (JSONObject) woi.get("configuration");
    assertEquals("1234", woiCfg.get("abcd"));
  }

  // POST /

  @Test
  public void testPostWorkflow() throws Exception {
    final String response = given().formParam("event_identifier", "905672ed-181c-4d60-b7cd-02758f61e713")
                                   .formParam("workflow_definition_identifier", "fast")
                                   .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
                                   .queryParam("withoperations", "true")
                                   .queryParam("withconfiguration", "true")
                                   .expect()
                                   .statusCode(SC_CREATED)
                                   .header("Location", "https://api.opencast.org/" + RUNNING_WORKFLOW_ID)
                                   .when()
                                   .post(env.host("/"))
                                   .asString();

    assertRunningWorkflow((JSONObject) parser.parse(response));
  }

  @Test
  public void testPostWorkflowWithoutEventIdentifier() {
    given().formParam("workflow_definition_identifier", "fast")
           .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
           .expect()
           .statusCode(SC_BAD_REQUEST)
           .when()
           .post(env.host("/"));
  }

  @Test
  public void testPostWorkflowWithoutWorkflowDefinitionIdentifier() {
    given().formParam("event_identifier", "905672ed-181c-4d60-b7cd-02758f61e713")
           .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
           .expect()
           .statusCode(SC_BAD_REQUEST)
           .when()
           .post(env.host("/"));
  }

  @Test
  public void testPostWorkflowWithInvalidConfigurations() {
    given().formParam("event_identifier", "905672ed-181c-4d60-b7cd-02758f61e713")
           .formParam("workflow_definition_identifier", "fast")
           .formParam("configuration", "{key: value}")
           .expect()
           .statusCode(SC_BAD_REQUEST)
           .when()
           .post(env.host("/"));
  }

  @Test
  public void testPostWorkflowWithLockedMediaPackage() throws Exception {
    final String response = given().formParam("event_identifier", "mediapackage-with-running-workflow")
                                   .formParam("workflow_definition_identifier", "fast")
                                   .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
                                   .expect()
                                   .statusCode(SC_CONFLICT)
                                   .when()
                                   .post(env.host("/"))
                                   .asString();

    final JSONObject json = (JSONObject) parser.parse(response);
    assertTrue(json.get("message").toString().contains("Illegal state msg"));
  }

  @Test
  public void testPostWorkflowWithMissingEvent() {
    given().formParam("event_identifier", "missing")
           .formParam("workflow_definition_identifier", "fast")
           .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
           .expect()
           .statusCode(SC_NOT_FOUND)
           .when()
           .post(env.host("/"));
  }

  //  @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),

  @Test
  public void testPostWorkflowWithMissingWorkflowDefinition() {
    given().formParam("event_identifier", "905672ed-181c-4d60-b7cd-02758f61e713")
           .formParam("workflow_definition_identifier", "missing")
           .formParam("configuration", "{\"key\": \"value\", \"foo\": \"bar\"}")
           .expect()
           .statusCode(SC_NOT_FOUND)
           .when()
           .post(env.host("/"));
  }

  // GET /{workflowInstanceId}

  @Test
  public void testGetWorkflowWithRunningWorkflow() throws Exception {
    final String response = given().pathParam("workflowInstanceId", RUNNING_WORKFLOW_ID)
                                   .queryParam("withoperations", "true")
                                   .queryParam("withconfiguration", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .get(env.host("/{workflowInstanceId}"))
                                   .asString();

    assertRunningWorkflow((JSONObject) parser.parse(response));
  }

  @Test
  public void testGetWorkflowWithUnauthorizedWorkflow() {
    given().pathParam("workflowInstanceId", UNAUTHORIZED_WORKFLOW_ID)
           .expect()
           .statusCode(SC_FORBIDDEN)
           .when()
           .get(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testGetWorkflowWithMissingWorkflow() {
    given().pathParam("workflowInstanceId", MISSING_WORKFLOW_ID)
           .expect()
           .statusCode(SC_NOT_FOUND)
           .when()
           .get(env.host("/{workflowInstanceId}"));
  }

  // PUT /{workflowInstanceId}

  @Test
  public void testPutWorkflowWithRunningWorkflow() throws Exception {
    final String response = given().pathParam("workflowInstanceId", RUNNING_WORKFLOW_ID)
                                   .queryParam("withoperations", "true")
                                   .queryParam("withconfiguration", "true")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .put(env.host("/{workflowInstanceId}"))
                                   .asString();

    assertRunningWorkflow((JSONObject) parser.parse(response));
  }

  @Test
  public void testPutWorkflowWithStoppedWorkflow() throws Exception {
    final String response = given().pathParam("workflowInstanceId", STOPPED_WORKFLOW_ID)
                                   .queryParam("withconfiguration", "true")
                                   .formParam("state", "paused")
                                   .formParam("configuration", "{\"abc\": \"123\"}")
                                   .expect()
                                   .statusCode(SC_OK)
                                   .when()
                                   .put(env.host("/{workflowInstanceId}"))
                                   .asString();

    final JSONObject wi = (JSONObject) parser.parse(response);
    assertEquals("paused", wi.get("state"));
    final JSONObject wiCfg = (JSONObject) wi.get("configuration");
    assertEquals("123", wiCfg.get("abc"));
  }

  @Test
  public void testPutWorkflowWithIllegalState() {
    given().pathParam("workflowInstanceId", STOPPED_WORKFLOW_ID)
           .formParam("state", "running")
           .expect()
           .statusCode(SC_CONFLICT)
           .when()
           .put(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testPutWorkflowWithIllegalConfiguration() {
    given().pathParam("workflowInstanceId", STOPPED_WORKFLOW_ID)
           .formParam("configuration", "{fdas:123}")
           .expect()
           .statusCode(SC_BAD_REQUEST)
           .when()
           .put(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testPutWorkflowWithUnauthorizedWorkflow() {
    given().pathParam("workflowInstanceId", UNAUTHORIZED_WORKFLOW_ID)
           .expect()
           .statusCode(SC_FORBIDDEN)
           .when()
           .put(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testPutWorkflowWithMissingWorkflow() {
    given().pathParam("workflowInstanceId", MISSING_WORKFLOW_ID)
           .expect()
           .statusCode(SC_NOT_FOUND)
           .when()
           .put(env.host("/{workflowInstanceId}"));
  }

  // DELETE /{workflowInstanceId}

  @Test
  public void testDeleteWorkflowWithStoppedWorkflow() {
    given().pathParam("workflowInstanceId", STOPPED_WORKFLOW_ID)
           .expect()
           .statusCode(SC_NO_CONTENT)
           .when()
           .delete(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testDeleteWorkflowWithRunningWorkflow() {
    given().pathParam("workflowInstanceId", RUNNING_WORKFLOW_ID)
           .expect()
           .statusCode(SC_CONFLICT)
           .when()
           .delete(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testDeleteWorkflowWithUnauthorizedWorkflow() {
    given().pathParam("workflowInstanceId", UNAUTHORIZED_WORKFLOW_ID)
           .expect()
           .statusCode(SC_FORBIDDEN)
           .when()
           .delete(env.host("/{workflowInstanceId}"));
  }

  @Test
  public void testDeleteWorkflowWithMissingWorkflow() {
    given().pathParam("workflowInstanceId", MISSING_WORKFLOW_ID)
           .expect()
           .statusCode(SC_NOT_FOUND)
           .when()
           .delete(env.host("/{workflowInstanceId}"));
  }
}
