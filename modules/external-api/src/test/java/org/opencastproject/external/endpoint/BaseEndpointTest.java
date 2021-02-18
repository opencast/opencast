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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link BaseEndpoint} */
public class BaseEndpointTest {

  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestBaseEndpoint.class);

  private static final JSONParser parser = new JSONParser();

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  /** Test case for {@link BaseEndpoint#getEndpointInfo()} */
  @Test
  public void testGetApiInfo() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/")).asString();

    JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("https://api.opencast.org", json.get("url"));
    assertEquals("v1.5.0", json.get("version"));
  }

  /** Test case for {@link BaseEndpoint#getUserInfo()} */
  @Test
  public void testGetUserInfo() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/info/me")).asString();

    JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("nowhere@opencast.org", json.get("email"));
    assertEquals("Opencast Student", json.get("name"));
    assertEquals("opencast", json.get("provider"));
    assertEquals("ROLE_USER_92623987_OPENCAST_ORG", json.get("userrole"));
    assertEquals("92623987@opencast.org", json.get("username"));
  }

  /** Test case for {@link BaseEndpoint#getUserRoles()} */
  @Test
  public void testGetUserRoles() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/info/me/roles"))
            .asString();

    JSONArray json = (JSONArray) parser.parse(response);
    assertTrue("User does not have role 'ROLE_USER_92623987_OPENCAST_ORG'",
            json.contains("ROLE_USER_92623987_OPENCAST_ORG"));
    assertTrue("User does not have role 'ROLE_STUDENT'", json.contains("ROLE_STUDENT"));
    assertEquals(2, json.size());
  }

  /** Test case for {@link BaseEndpoint#getOrganizationInfo()} */
  @Test
  public void testGetOrganizationInfo() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/info/organization"))
            .asString();

    JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("ROLE_ADMIN", json.get("adminRole"));
    assertEquals("ROLE_ANONYMOUS", json.get("anonymousRole"));
    assertEquals("opencast", json.get("id"));
    assertEquals("Opencast", json.get("name"));
  }

  /** Test case for {@link BaseEndpoint#getOrganizationProperties()} */
  @Test
  public void testGetOrganizationProperties() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when()
            .get(env.host("/info/organization/properties")).asString();

    JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("https://feeds.opencast.org", json.get("org.opencastproject.feed.url"));
    assertEquals("https://documentation.opencast.org", json.get("org.opencastproject.admin.documentation.url"));
    assertEquals("https://api.opencast.org", json.get("org.opencastproject.external.api.url"));
  }

  /** Test case for {@link BaseEndpoint#getVersion()} */
  @Test
  public void testGetVersion() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/version")).asString();

    JSONObject json = (JSONObject) parser.parse(response);
    JSONArray version = (JSONArray) json.get("versions");
    assertEquals("v1.5.0", json.get("default"));
    assertTrue(version.contains("v1.0.0"));
    assertTrue(version.contains("v1.1.0"));
    assertTrue(version.contains("v1.2.0"));
    assertTrue(version.contains("v1.3.0"));
    assertTrue(version.contains("v1.4.0"));
    assertTrue(version.contains("v1.5.0"));
    assertEquals(6, version.size());
  }

  /** Test case for {@link BaseEndpoint#getVersionDefault()} */
  @Test
  public void testGetVersionDefault() throws Exception {
    final String response = given().log().all().expect().statusCode(SC_OK).when().get(env.host("/version/default"))
            .asString();

    JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("v1.5.0", json.get("default"));
  }

  /** Test case for {@link BaseEndpoint#recreateIndex()} */
  @Test
  public void testRecreateIndex() throws Exception {
    given().log().all().expect().statusCode(SC_OK).when().post(env.host("/recreateIndex"));
  }

}
