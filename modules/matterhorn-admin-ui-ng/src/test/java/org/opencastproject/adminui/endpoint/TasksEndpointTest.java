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
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

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

public class TasksEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestTasksEndpoint.class);

  private JSONParser parser;

  @Test
  public void testGetProcessing() throws ParseException, IOException {
    InputStream stream = TasksEndpointTest.class.getResourceAsStream("/taskProcessing.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONArray expected = (JSONArray) new JSONParser().parse(reader);
    JSONArray actual = (JSONArray) parser.parse(given().queryParam("tags", "archive").expect().log().all()
            .statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when().get(rt.host("/processing.json"))
            .asString());

    Assert.assertEquals(expected.size(), actual.size());
  }

  @Test
  public void testCreateTask() throws ParseException, IOException {
    InputStream stream = TasksEndpointTest.class.getResourceAsStream("/taskNew.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject metadata = (JSONObject) new JSONParser().parse(reader);

    given().formParam("metadata", "empty").expect().log().all().statusCode(HttpStatus.SC_BAD_REQUEST).when()
            .post(rt.host("/new"));

    String result = given().formParam("metadata", metadata.toJSONString()).expect().log().all()
            .statusCode(HttpStatus.SC_CREATED).when().post(rt.host("/new")).asString();
    Assert.assertEquals("5,10", result);
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
