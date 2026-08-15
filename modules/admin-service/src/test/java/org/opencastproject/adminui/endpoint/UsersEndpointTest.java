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

package org.opencastproject.adminui.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpStatus;
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
  private static final RestServiceTestEnv rt = testEnvForClasses(TestUsersEndpoint.class);


  @Test
  public void testGetAllUsers() throws IOException {
    InputStream stream = UsersEndpointTest.class.getResourceAsStream("/users.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonObject expected = JsonParser.parseReader(reader).getAsJsonObject();

    JsonObject actual = JsonParser.parseString(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(4)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(4)).when().get(rt.host("/users.json")).asString()
                ).getAsJsonObject();

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testGetAllWithParams() throws IOException {
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
    JsonObject actual = JsonParser.parseString(given().queryParam("sort", "name:ASC").expect()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("total", equalTo(4))
            .body("offset", equalTo(0)).body("limit", equalTo(100)).body("results", hasSize(4)).when()
            .get(rt.host("/users.json")).asString()).getAsJsonObject();
    JsonArray users = actual.getAsJsonArray("results");
    JsonObject user1 = users.get(0).getAsJsonObject();
    JsonObject user2 = users.get(1).getAsJsonObject();
    JsonObject user3 = users.get(2).getAsJsonObject();
    JsonObject user4 = users.get(3).getAsJsonObject();
    Assert.assertEquals("User1", user1.get("name").getAsString());
    Assert.assertEquals("user2", user2.get("name").getAsString());
    Assert.assertEquals("User3", user3.get("name").getAsString());
    Assert.assertEquals("user4", user4.get("name").getAsString());
  }

  @Before
  public void setUp() {
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
