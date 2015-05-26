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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ListProvidersEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestListProvidersEndpoint.class);

  private JSONParser parser;

  @Test
  public void testGetGeneric() throws ParseException {
    JSONObject all = (JSONObject) parser.parse(given().log().all()
            .pathParam("id", TestListProvidersEndpoint.PROVIDER_NAME).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("2", containsString(TestListProvidersEndpoint.PROVIDER_VALUES[2]))
            .body("", hasValue("z")).when().get(rt.host("/{id}.json")).asString());

    assertEquals(TestListProvidersEndpoint.PROVIDER_VALUES.length, all.entrySet().size());

    given().log().all().pathParam("id", "missingprovider").expect().statusCode(HttpStatus.SC_NOT_FOUND).when()
            .get(rt.host("/{id}.json"));

    int limit = 2;
    int offset = 2;
    JSONObject limited = (JSONObject) parser.parse(given().log().all()
            .pathParam("id", TestListProvidersEndpoint.PROVIDER_NAME).queryParam("limit", limit)
            .queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK).when().get(rt.host("/{id}.json"))
            .asString());

    assertEquals(limit, limited.entrySet().size());

    Object[] allValues = all.values().toArray();
    Object[] limitedValues = limited.values().toArray();

    for (int i = 0; i < limitedValues.length; i++) {
      assertEquals(limitedValues[i], allValues[offset + i]);
    }
  }

  @Test
  public void testGetWithFilters() throws ParseException {
    Response response = given().log().all().pathParam("id", "services").parameters("filter", "name=non existing name")
            .expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).get(rt.host("/{id}.json"));
    ResponseBody body = response.getBody();
    String content = body.asString();
    JSONObject json = (JSONObject) parser.parse(content);
    JSONArray array = (JSONArray) json.get("results");
    assertEquals(0, array.size());
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
