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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

import org.apache.commons.httpclient.HttpStatus;
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

public class AclEndpointTest {

  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestAclEndpoint.class,
          NotFoundExceptionMapper.class);

  private JSONParser parser;

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
    parser = new JSONParser();
  }

  @Test
  public void testGetAllAcls() throws IOException, ParseException {
    InputStream stream = AclEndpointTest.class.getResourceAsStream("/acls.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);

    JSONObject actual = (JSONObject) parser.parse(given().log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(0))
            .body("limit", equalTo(100)).body("results", hasSize(2)).when().get(rt.host("/acls.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testGetAllWithParams() throws ParseException, IOException {
    int limit = 100;
    int offset = 1;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(1)).when().get(rt.host("/acls.json"));

    offset = 0;
    limit = 1;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(limit)).body("results", hasSize(1)).when().get(rt.host("/acls.json"));

    offset = 2;
    limit = 0;

    given().log().all().queryParam("limit", limit).queryParam("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("total", equalTo(2)).body("offset", equalTo(offset))
            .body("limit", equalTo(100)).body("results", hasSize(0)).when().get(rt.host("/acls.json"));
  }

}
