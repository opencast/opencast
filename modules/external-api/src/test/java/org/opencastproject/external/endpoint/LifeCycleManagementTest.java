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
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.test.rest.RestServiceTestEnv;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link LifeCycleManagementEndpoint} */
public class LifeCycleManagementTest {
  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(TestLifeCycleManagementEndpoint.class);

  private static final JSONParser parser = new JSONParser();

  private static final String POLICY_ID = "42";
  private static final String MISSING_POLICY_ID = "4444";

  private static final String UNAUTHORIZED_POLICY_ID = "1";

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  private void assertPolicy(JSONObject json) {
    assertEquals("title", json.get("title"));
    assertEquals(TargetType.EVENT.toString(), json.get("targetType"));
    assertEquals(Action.START_WORKFLOW.toString(), json.get("action"));
    assertEquals("{\"workflowId\":\"noop\",\"workflowParameters\":\"{}\"}", json.get("actionParameters"));

    assertEquals("2023-11-30T16:16:47Z", json.get("actionDate"));
    assertEquals(Timing.SPECIFIC_DATE.toString(), json.get("timing"));

    assertEquals("{}", json.get("targetFilters"));

    final JSONArray accessControlEntries = (JSONArray) json.get("accessControlEntries");
    assertEquals(1, accessControlEntries.size());

    final JSONObject accessControlEntry = (JSONObject) accessControlEntries.get(0);
    assertEquals(true, accessControlEntry.get("allow"));
    assertEquals("ROLE_USER_BOB", accessControlEntry.get("role"));
    assertEquals("read", accessControlEntry.get("action"));
  }

  @Test
  public void testGetPolicy() throws Exception {
    String response = given()
        .pathParam("id", POLICY_ID)
        .expect()
        .statusCode(SC_OK).when()
        .get(env.host("/policies/{id}"))
        .asString();

    assertPolicy((JSONObject) parser.parse(response));
  }

  @Test
  public void testGetPolicyThatDoesNotExist() throws Exception {
    given()
        .pathParam("id", MISSING_POLICY_ID)
        .expect()
        .statusCode(SC_NOT_FOUND).when()
        .get(env.host("/policies/{id}"));
  }

  @Test
  public void testGetPolicyUnauthorized() throws Exception {
    given()
        .pathParam("id", UNAUTHORIZED_POLICY_ID)
        .expect()
        .statusCode(SC_FORBIDDEN).when()
        .get(env.host("/policies/{id}"));
  }

  @Test
  public void testGetPolicies() throws Exception {
    String response = given()
        .queryParam("limit", 100)
        .queryParam("offset", 0)
        .queryParam("sortByUpdated", false)
        .queryParam("sortByUpdatedAscending", false)
        .expect()
        .statusCode(SC_OK).when()
        .get(env.host("/policies"))
        .asString();

    JSONObject responseObj = (JSONObject) parser.parse(response);
    final JSONArray policies = (JSONArray) responseObj.get("results");
    assertPolicy((JSONObject) policies.get(0));
  }

  @Test
  public void testCreatePolicy() throws Exception {
    String response = given()
        .formParam("title", "title")
        .formParam("targetType", "EVENT")
        .formParam("action", "START_WORKFLOW")
        .formParam("actionParameters", "{ workflowId: noop }")
        .formParam("actionDate", "2023-11-30T16:16:47Z")
        .formParam("timing", "SPECIFIC_DATE")
        .expect()
        .statusCode(SC_CREATED).when()
        .post(env.host("policies"))
        .asString();

//    assertPolicy((JSONObject) parser.parse(response));
  }

  @Test
  public void testCreatePolicyInvalid() throws Exception {
    given()
        .formParam("title", "title")
        .expect()
        .statusCode(SC_BAD_REQUEST).when()
        .post(env.host("/policies"));
  }

  @Test
  public void testRemovePolicy() throws Exception {
    given()
        .pathParam("id", POLICY_ID)
        .expect()
        .statusCode(SC_OK).when()
        .delete(env.host("/policies/{id}"));
  }

  @Test
  public void testRemoveMissingPolicy() throws Exception {
    given()
        .pathParam("id", MISSING_POLICY_ID)
        .expect()
        .statusCode(SC_NOT_FOUND).when()
        .delete(env.host("/policies/{id}"));
  }

  @Test
  public void testRemovePolicyUnauthorized() throws Exception {
    given()
        .pathParam("id", UNAUTHORIZED_POLICY_ID)
        .expect()
        .statusCode(SC_FORBIDDEN).when()
        .delete(env.host("/policies/{id}"));
  }
}
