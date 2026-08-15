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
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.restassured.http.ContentType;

public class TasksEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(TestTasksEndpoint.class);


  @Test
  public void testGetProcessing() throws IOException {
    InputStream stream = TasksEndpointTest.class.getResourceAsStream("/taskProcessing.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonArray expected = JsonParser.parseReader(reader).getAsJsonArray();
    JsonArray actual = JsonParser.parseString(
            given().queryParam("tags", "archive").expect().statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON).when().get(rt.host("/processing.json")).asString())
            .getAsJsonArray();

    assertEquals(expected, actual);
  }

  @Test
  public void testCreateTask() throws IOException {
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
        .formParam("metadata", "{\"workflow\":\"full\", \"configuration\":{\"id1\": {\"foo\": \"bar\"},"
            + "\"id2\": {\"baz\": \"qux\"}}}")
        .expect().statusCode(HttpStatus.SC_CREATED).when().post(rt.host("/new")).asString();
    assertEquals("[5,10]", result);
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
