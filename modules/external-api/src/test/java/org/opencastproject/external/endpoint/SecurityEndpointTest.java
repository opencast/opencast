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
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;
import static org.opencastproject.util.DateTimeSupport.fromUTC;
import static org.opencastproject.util.DateTimeSupport.toUTC;

import org.opencastproject.test.rest.RestServiceTestEnv;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

/** Test cases for {@link SecurityEndpoint} */
public class SecurityEndpointTest {

  private static final String APP_V1_0_0_JSON = "application/v1.0.0+json";
  private static final String APP_V1_0_0_XML = "application/v1.0.0+xml";

  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestSecurityEndpoint.class);

  /** The json parser */
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
  public void testSignUrlJson() throws Exception {
    final Date validUntil = new Date(1429253432000L);
    final String response = given().formParam("url", "http://mycdn.com/path/movie.mp4")
            .formParam("valid-until", toUTC(validUntil.getTime())).accept(APP_V1_0_0_JSON).log().all().expect()
            .statusCode(SC_OK).when().post(env.host("/sign")).asString();

    final JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("http://mycdn.com/path/movie.mp4?signature", json.get("url"));
    assertEquals(validUntil.getTime(), fromUTC((String) json.get("valid-until")));
  }

  @Test
  public void testSignUrlXml() throws Exception {
    final Date validUntil = new Date(1429253432000L);
    given().formParam("url", "http://mycdn.com/path/movie.mp4").formParam("valid-until", toUTC(validUntil.getTime()))
            .accept(APP_V1_0_0_XML).log().all().expect().statusCode(SC_NOT_ACCEPTABLE).when().post(env.host("/sign"))
            .asString();
  }

  @Test
  public void testSignUrlWithoutValidity() throws Exception {
    final String response = given().formParam("url", "http://mycdn.com/path/movie.mp4").accept(APP_V1_0_0_JSON).log()
            .all().expect().statusCode(SC_OK).when().post(env.host("/sign")).asString();

    final JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("http://mycdn.com/path/movie.mp4?signature", json.get("url"));
    assertTrue(new Date().getTime() < fromUTC((String) json.get("valid-until")));
  }

  @Test
  public void testSignUnsupportedUrl() throws Exception {
    final String response = given().formParam("url", "http://otherhost.com/path/file.txt").accept(APP_V1_0_0_JSON).log()
            .all().expect().statusCode(SC_OK).when().post(env.host("/sign")).asString();

    final JSONObject json = (JSONObject) parser.parse(response);
    assertEquals("Given URL cannot be signed", json.get("error"));
  }

}
