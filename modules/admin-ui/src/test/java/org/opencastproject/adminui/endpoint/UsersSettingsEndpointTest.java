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
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import io.restassured.http.ContentType;

public class UsersSettingsEndpointTest {
  private static final Logger logger = LoggerFactory.getLogger(UsersSettingsEndpointTest.class);
  private static final RestServiceTestEnv rt = testEnvForClasses(TestUserSettingsEndpoint.class);

  private JSONParser parser;

  private void compareIds(String key, JSONObject expected, JSONObject actual) {
    JSONArray expectedArray = (JSONArray) expected.get(key);
    JSONArray actualArray = (JSONArray) actual.get(key);

    Assert.assertEquals(expectedArray.size(), actualArray.size());
    JSONObject exObject;
    JSONObject acObject;
    int actualId;
    for (Object anActualArray : actualArray) {
      acObject = (JSONObject) anActualArray;
      actualId = Integer.parseInt(acObject.get("id").toString()) - 1;
      exObject = (JSONObject) expectedArray.get(actualId);
      Set exEntrySet = exObject.keySet();
      Assert.assertEquals(exEntrySet.size(), acObject.size());

      for (Object item : exEntrySet) {
        Object exValue = exObject.get(item);
        Object acValue = acObject.get(item);
        Assert.assertEquals(exValue, acValue);
      }

    }
  }

  @Test
  public void testGetUserSettingsInputsDefaultsExpectsDefaultLimitsAndOffsets() throws ParseException, IOException {
    InputStream stream = UsersSettingsEndpointTest.class.getResourceAsStream("/usersettings.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(10)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(10)).when().get(rt.host("/settings.json")).asString());

    logger.info(actual.toJSONString());
    compareIds("results", expected, actual);
  }

  @Test
  public void testGetUserSettingsInputsNormalLimitsAndOffsetsExpectsDefaultLimitsAndOffsets() throws ParseException,
          IOException {
    InputStream stream = UsersSettingsEndpointTest.class.getResourceAsStream("/usersettings.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(10)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(10)).when()
            .get(rt.host("/settings.json?limit=100&offset=0")).asString());

    logger.info(actual.toJSONString());
    compareIds("results", expected, actual);
  }

  @Test
  public void testPostSettingExpectsOK() throws ParseException, IOException {
    String key = "example_key";
    String value = "example_value";

    JSONObject actual = (JSONObject) parser.parse(given().formParam("key", key).formParam("value", value)
            .expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("key", equalTo(key))
            .body("value", equalTo(value)).when().post(rt.host("setting")).asString());
    logger.info(actual.toJSONString());
  }

  @Test
  public void testDeleteUserSettingExpectsOK() throws ParseException, IOException {
    given().expect().statusCode(HttpStatus.SC_OK).when().delete(rt.host("/setting/18"));
  }

  @Test
  public void testPutSettingExpectsOK() throws ParseException, IOException {
    String key = TestUserSettingsEndpoint.EXAMPLE_KEY;
    String value = TestUserSettingsEndpoint.EXAMPLE_VALUE;

    given().pathParam("settingId", Long.toString(18)).formParam("key", key).formParam("value", value)
            .expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("key", equalTo(key))
            .body("value", equalTo(value)).when().put(rt.host("/setting/{settingId}")).asString();
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
