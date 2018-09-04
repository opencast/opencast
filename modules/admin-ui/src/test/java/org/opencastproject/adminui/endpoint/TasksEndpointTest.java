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
import static org.junit.Assert.assertEquals;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.RestServiceTestEnv;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.restassured.http.ContentType;

public class TasksEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestTasksEndpoint.class);

  private JSONParser parser;

  @Test
  public void testGetProcessing() throws ParseException, IOException {
    InputStream stream = TasksEndpointTest.class.getResourceAsStream("/taskProcessing.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray expected = (JSONArray) new JSONParser().parse(reader);
    JSONArray actual = (JSONArray) parser
            .parse(given().queryParam("tags", "archive").expect().statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON).when().get(rt.host("/processing.json")).asString());

    assertEquals(expected, actual);
  }

  @Test
  public void testCreateTask() throws ParseException, IOException {
    given().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/new"));

    given().formParam("metadata", "empty").expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/new"));

    // configuration missing
    given().formParam("metadata", "{\"workflow\":\"full\"}").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/new"));

    // workflow missing
    given().formParam("metadata", "{\"configuration\":{}}").expect()
            .statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/new"));

    // invalid workflow id
    given().formParam("metadata", "{\"workflow\":\"exception\", \"configuration\":{}}")
            .expect().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).when().post(rt.host("/new"));

    String result = given()
            .formParam("metadata", "{\"workflow\":\"full\", \"configuration\":{\"id1\": {\"foo\": \"bar\"},\"id2\": {\"baz\": \"qux\"}}}")
            .expect().statusCode(HttpStatus.SC_CREATED).when().post(rt.host("/new")).asString();
    assertEquals("[5,10]", result);
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
