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

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UsersEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestUsersEndpoint.class);

  private JSONParser parser;

  @Test
  public void testGetAllUsers() throws ParseException, IOException {
    InputStream stream = UsersEndpointTest.class.getResourceAsStream("/users.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    JSONObject actual = (JSONObject) parser.parse(given().log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(4)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(4)).when().get(rt.host("/users.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testGetAllWithParams() throws ParseException, IOException {
    int limit = 100;
    int offset = 2;
    int total = 4;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(2)).when().get(rt.host("/users.json"));

    offset = 0;
    limit = 2;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit > 0 ? limit : total - offset)).when()
            .get(rt.host("/users.json"));

    offset = 2;
    limit = 2;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(total)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(limit > 0 ? limit : total - offset)).when()
            .get(rt.host("/users.json"));
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
