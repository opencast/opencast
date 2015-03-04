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
package org.opencastproject.adminui.endpoint;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class CaptureAgentsEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestCaptureAgentsEndpoint.class);
  private JSONParser parser = new JSONParser();

  private JSONObject getCaptureAgent(String name, JSONArray captureAgents) {
    JSONObject result = null;
    for (Object agent : captureAgents) {
      JSONObject agentJson = (JSONObject) agent;
      if (!StringUtils.isBlank(agentJson.get("Name").toString())
              && agentJson.get("Name").toString().equalsIgnoreCase(name)) {
        result = agentJson;
      }
    }
    return result;
  }

  /**
   * Check the results from the capture agent endpoint.
   *
   * @param expected
   *          The expected JSONObject
   * @param actual
   *          The actual JSONObject
   * @param checkInputs
   *          Whether to check the capture agent's inputs ]
   */
  private void checkCaptureAgentResults(String expected, String actual, boolean checkInputs) throws ParseException {
    JSONObject expectedJson = (JSONObject) parser.parse(expected);
    JSONObject resultJson = (JSONObject) parser.parse(actual);
    assertEquals("The expected total should match the result total", expectedJson.get("total"), resultJson.get("total"));
    assertEquals("The expected offset should match the result offset", expectedJson.get("offset"),
            resultJson.get("offset"));
    assertEquals("The expected count should match the result count", expectedJson.get("count"), resultJson.get("count"));
    assertEquals("The expected limit should match the result limit", expectedJson.get("limit"), resultJson.get("limit"));
    checkCaptureAgentResults((JSONArray) expectedJson.get("results"), (JSONArray) resultJson.get("results"),
            checkInputs);
  }

  /**
   * Check an individual agent's actual json against an expected json
   *
   * @param expectedJson
   *          The expected json array
   * @param resultJson
   *          The actual json array
   * @param checkInputs
   *          Whether to check the agent's inputs
   */
  private void checkCaptureAgentResults(JSONArray expectedJson, JSONArray resultJson, boolean checkInputs) {
    for (Object captureAgent : resultJson) {
      JSONObject captureAgentJson = (JSONObject) captureAgent;
      String name = captureAgentJson.get("Name").toString();
      if (!StringUtils.isBlank(name)) {
        JSONObject expected = getCaptureAgent(name, expectedJson);
        assertEquals(expected.get("Name"), captureAgentJson.get("Name"));
        assertEquals(expected.get("Status"), captureAgentJson.get("Status"));
        assertEquals(expected.get("Update"), captureAgentJson.get("Update"));
        assertEquals(expected.get("roomId"), captureAgentJson.get("roomId"));

        // Check blacklist
        if ("".equals(captureAgentJson.get("blacklist"))) {
          assertEquals(expected.get("blacklist"), captureAgentJson.get("blacklist"));
        } else {
          JSONObject expectedBlacklist = (JSONObject) expected.get("blacklist");
          JSONObject actualBlacklist = (JSONObject) captureAgentJson.get("blacklist");
          assertEquals(expectedBlacklist.get("start"), actualBlacklist.get("start"));
          assertEquals(expectedBlacklist.get("end"), actualBlacklist.get("end"));
        }
        if (checkInputs) {
          // Check inputs
          JSONObject expectedInputs = (JSONObject) ((JSONArray) expected.get("inputs")).get(0);
          JSONObject actualInputs = (JSONObject) ((JSONArray) captureAgentJson.get("inputs")).get(0);
          assertEquals(expectedInputs.get("id"), actualInputs.get("id"));
          assertEquals(expectedInputs.get("value"), actualInputs.get("value"));
        }
      } else {
        fail("Capture agents needs to have a name as it is a unique id.");
      }
    }
  }

  @Test
  public void testGetAllCaptureAgents() throws Exception {
    String expectedWithInputs = IOUtils.toString(CaptureAgentsEndpointTest.class.getResource("/capture_agents.json"));
    String expectedWithoutInputs = IOUtils.toString(CaptureAgentsEndpointTest.class
            .getResource("/capture_agents_noinputs.json"));

    String result = given().queryParam("inputs", true).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(4)).body("offset", equalTo(0))
            .body("limit", equalTo(0)).body("results", hasSize(4)).when().get(rt.host("/agents.json")).asString();

    checkCaptureAgentResults(expectedWithInputs, result, true);

    result = given().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("total", equalTo(4))
            .body("offset", equalTo(0)).body("limit", equalTo(0)).body("results", hasSize(4)).when()
            .get(rt.host("/agents.json")).asString();

    checkCaptureAgentResults(expectedWithoutInputs, result, false);
  }

  @Test
  public void testGetAllWithParams() throws ParseException, IOException {
    int limit = 0;
    int offset = 2;
    int total = 4;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit > 0 ? limit : total - offset)).when()
            .get(rt.host("/agents.json"));

    offset = 0;
    limit = 2;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit > 0 ? limit : total - offset)).when()
            .get(rt.host("/agents.json"));

    offset = 2;
    limit = 2;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit > 0 ? limit : total - offset)).when()
            .get(rt.host("/agents.json"));
  }

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

}
