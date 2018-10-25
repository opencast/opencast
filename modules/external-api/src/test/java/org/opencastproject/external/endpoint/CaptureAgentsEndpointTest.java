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
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertThat;
import static org.opencastproject.external.endpoint.TestCaptureAgentsEndpoint.UNKNOWN_AGENT;
import static org.opencastproject.external.endpoint.TestCaptureAgentsEndpoint.loadAgents;
import static org.opencastproject.external.endpoint.TestCaptureAgentsEndpoint.toJson;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.test.rest.RestServiceTestEnv;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class CaptureAgentsEndpointTest {
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestCaptureAgentsEndpoint.class);

  private static List<Agent> allAgents;

   @BeforeClass
   public static void oneTimeSetUp() throws ParseException, IOException, URISyntaxException {
     env.setUpServer();
     allAgents = loadAgents();
   }

   @AfterClass
   public static void oneTimeTearDown() {
     env.tearDownServer();
   }

   @Test
   public void testGetAgent() throws Exception {
     final Agent expectedAgent = allAgents.stream().findAny().get();
     final String expectedJson = toJson(expectedAgent).toJSONString();
     String result = given().pathParam("agentId", expectedAgent.getName()).expect().statusCode(SC_OK).when()
         .get(env.host("{agentId}")).asString();
     assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
   }

  @Test
  public void testGetNonExistingAgent() {
    given().pathParam("agentId", UNKNOWN_AGENT).expect().statusCode(SC_NOT_FOUND).when()
        .get(env.host("{agentId}")).asString();
  }

  @Test
  public void testGetAgents() {
    final String expectedJson = toJson(allAgents).toJSONString();
    String result = given().expect().statusCode(SC_OK).when().get(env.host("")).asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetAgentsWithOffset() {
    final int offset = 1;
    final String expectedJson = toJson(allAgents.subList(offset, allAgents.size())).toJSONString();
    String result = given().queryParam("offset", offset).expect().statusCode(SC_OK).when().get(env.host(""))
        .asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetAgentsWithLimit() {
    final int limit = 1;
    final String expectedJson = toJson(allAgents.subList(0, limit)).toJSONString();
    String result = given().queryParam("limit", limit).expect().statusCode(SC_OK).when().get(env.host(""))
        .asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetAgentsWithInvalidOffset() {
    final int offset = allAgents.size();
    final String expectedJson = new JSONArray().toJSONString();
    String result = given().queryParam("offset", offset).expect().statusCode(SC_OK).when().get(env.host(""))
        .asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

  @Test
  public void testGetAgentsWithInvalidLimit() {
    final int limit = 0;
    final String expectedJson = toJson(allAgents).toJSONString();
    String result = given().queryParam("limit", limit).expect().statusCode(SC_OK).when().get(env.host(""))
        .asString();
    assertThat(result, SameJSONAs.sameJSONAs(expectedJson).allowingAnyArrayOrdering());
  }

}
