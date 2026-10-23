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

import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpStatus;
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


  private void compareIds(String key, JsonObject expected, JsonObject actual) {
    JsonArray expectedArray = expected.getAsJsonArray(key);
    JsonArray actualArray = actual.getAsJsonArray(key);

    Assert.assertEquals(expectedArray.size(), actualArray.size());
    JsonObject exObject;
    JsonObject acObject;
    int actualId;
    for (JsonElement anActualArray : actualArray) {
      acObject = anActualArray.getAsJsonObject();
      actualId = acObject.get("id").getAsInt() - 1;
      exObject = expectedArray.get(actualId).getAsJsonObject();
      Set<String> exEntrySet = exObject.keySet();
      Assert.assertEquals(exEntrySet.size(), acObject.size());

      for (String item : exEntrySet) {
        JsonElement exValue = exObject.get(item);
        JsonElement acValue = acObject.get(item);
        Assert.assertEquals(exValue, acValue);
      }

    }
  }

  @Test
  public void testGetUserSettingsInputsDefaultsExpectsDefaultLimitsAndOffsets() throws IOException {
    InputStream stream = UsersSettingsEndpointTest.class.getResourceAsStream("/usersettings.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonObject expected = JsonParser.parseReader(reader).getAsJsonObject();
    JsonObject actual = JsonParser.parseString(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(10)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(10)).when().get(rt.host("/settings.json")).asString()
                ).getAsJsonObject();

    logger.info(actual.toString());
    compareIds("results", expected, actual);
  }

  @Test
  public void testGetUserSettingsInputsNormalLimitsAndOffsetsExpectsDefaultLimitsAndOffsets() throws Exception,
          IOException {
    InputStream stream = UsersSettingsEndpointTest.class.getResourceAsStream("/usersettings.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonObject expected = JsonParser.parseReader(reader).getAsJsonObject();

    JsonObject actual = JsonParser.parseString(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(10)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(10)).when()
            .get(rt.host("/settings.json?limit=100&offset=0")).asString()).getAsJsonObject();

    logger.info(actual.toString());
    compareIds("results", expected, actual);
  }

  @Test
  public void testPostSettingExpectsOK() throws IOException {
    String key = "example_key";
    String value = "example_value";

    JsonObject actual = JsonParser.parseString(given().formParam("key", key).formParam("value", value)
            .expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("key", equalTo(key))
            .body("value", equalTo(value)).when().post(rt.host("setting")).asString()).getAsJsonObject();
    logger.info(actual.toString());
  }

  @Test
  public void testDeleteUserSettingExpectsOK() throws IOException {
    given().expect().statusCode(HttpStatus.SC_OK).when().delete(rt.host("/setting/18"));
  }

  @Test
  public void testPutSettingExpectsOK() throws IOException {
    String key = TestUserSettingsEndpoint.EXAMPLE_KEY;
    String value = TestUserSettingsEndpoint.EXAMPLE_VALUE;

    given().pathParam("settingId", Long.toString(18)).formParam("key", key).formParam("value", value)
            .expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("key", equalTo(key))
            .body("value", equalTo(value)).when().put(rt.host("/setting/{settingId}")).asString();
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
