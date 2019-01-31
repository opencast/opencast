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

package org.opencastproject.adminui.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.test.rest.RestServiceTestEnv;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.restassured.http.ContentType;

public class UsersEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestUsersEndpoint.class);

  private JSONParser parser;

  @Test
  public void testGetAllUsers() throws ParseException, IOException {
    InputStream stream = UsersEndpointTest.class.getResourceAsStream("/users.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(4)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(4)).when().get(rt.host("/users.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testGetAllWithParams() throws ParseException, IOException {
    int limit = 100;
    int offset = 2;
    int total = 4;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(2)).when().get(rt.host("/users.json"));

    offset = 0;
    limit = 2;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit)).when()
            .get(rt.host("/users.json"));

    offset = 2;
    limit = 2;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit)).when()
            .get(rt.host("/users.json"));
  }

  @Test
  public void testSorting() throws Exception {
    JSONObject actual = (JSONObject) parser.parse(given().queryParam("sort", "name:ASC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("total", equalTo(4))
            .body("offset", equalTo(0)).body("limit", equalTo(100)).body("results", hasSize(4)).when()
            .get(rt.host("/users.json")).asString());
    JSONArray users = (JSONArray) actual.get("results");
    JSONObject user1 = (JSONObject) users.get(0);
    JSONObject user2 = (JSONObject) users.get(1);
    JSONObject user3 = (JSONObject) users.get(2);
    JSONObject user4 = (JSONObject) users.get(3);
    Assert.assertEquals("User1", user1.get("name"));
    Assert.assertEquals("user2", user2.get("name"));
    Assert.assertEquals("User3", user3.get("name"));
    Assert.assertEquals("user4", user4.get("name"));
  }

  @Before
  public void setUp() {
    parser = new JSONParser();
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
