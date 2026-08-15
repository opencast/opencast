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
import org.opencastproject.test.rest.NotFoundExceptionMapper;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.restassured.http.ContentType;

public class AclEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(TestAclEndpoint.class, NotFoundExceptionMapper.class);


  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  @Before
  public void setUp() {
  }

  @Test
  public void testGetAllAcls() throws IOException {
    InputStream stream = AclEndpointTest.class.getResourceAsStream("/acls.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonObject expected = JsonParser.parseReader(reader).getAsJsonObject();

    JsonObject actual = JsonParser.parseString(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(2)).when().get(rt.host("/acls.json")).asString()
                ).getAsJsonObject();

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testGetAllRoles() throws IOException {
    InputStream stream = AclEndpointTest.class.getResourceAsStream("/roles.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonArray expectedArray = JsonParser.parseReader(reader).getAsJsonArray();

    JsonArray actualArray = JsonParser.parseString(given().expect().statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON).when().get(rt.host("/roles.json")).asString()).getAsJsonArray();

    Assert.assertEquals(expectedArray, actualArray);
  }

  @Test
  public void testGetAllAclsWithParams() throws IOException {
    int limit = 100;
    int offset = 1;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(1)).when().get(rt.host("/acls.json"));

    offset = 0;
    limit = 1;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(1)).when().get(rt.host("/acls.json"));

    offset = 2;
    limit = 0;

    given().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(100)).body("results", hasSize(0)).when().get(rt.host("/acls.json"));
  }

  @Test
  public void testGetAllRolesWithParams() throws IOException {

    InputStream stream = AclEndpointTest.class.getResourceAsStream("/roles.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JsonArray allRoles = JsonParser.parseReader(reader).getAsJsonArray();

    int limit = 2;
    int offset = 1;
    String target = "ACL";

    JsonArray expectedArray = new JsonArray();
    expectedArray.add(allRoles.get(1));
    expectedArray.add(allRoles.get(3));

    JsonArray actualArray = JsonParser.parseString(given().queryParam("limit", limit).queryParam("offset", offset)
        .queryParam("target", target).expect().statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON).when().get(rt.host("/roles.json")).asString()).getAsJsonArray();
    Assert.assertEquals(expectedArray, actualArray);
  }

}
