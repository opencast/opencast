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
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
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

public class ServerEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestServerEndpoint.class);

  private JSONParser parser;

  @Test
  public void testSimpleRequest() throws ParseException, IOException {
    InputStream stream = ServerEndpointTest.class.getResourceAsStream("/servers.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/servers.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testLimitAndOffset() {
    given().param("limit", 3).param("offset", 2).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).content("limit", equalTo(3)).content("count", equalTo(2))
            .content("total", equalTo(4)).when().get(rt.host("/servers.json"));
  }

  @Test
  public void testFilters() {
    given().param("online", true).param("maintenance", true).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).content("count", equalTo(2)).content("total", equalTo(4)).when()
            .get(rt.host("/servers.json"));

    given().param("q", "host1").param("maintenance", true).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).content("count", equalTo(0)).content("total", equalTo(4)).when()
            .get(rt.host("/servers.json"));

    given().param("q", "host1").param("maintenance", false).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).content("count", equalTo(1)).content("total", equalTo(4)).when()
            .get(rt.host("/servers.json"));
  }

  @Test
  public void testSort() throws ParseException {
    given().param("sort", "CORES").log().all().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .content("count", equalTo(4)).content("total", equalTo(4)).content("results[0].name", equalTo("host3"))
            .content("results[1].name", equalTo("host2")).when().get(rt.host("/servers.json"));

    given().param("sort", "CORES_DESC").log().all().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .content("count", equalTo(4)).content("total", equalTo(4)).content("results[0].name", equalTo("host1"))
            .content("results[1].name", equalTo("host4")).when().get(rt.host("/servers.json"));

    given().param("sort", "HOSTNAME").log().all().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .content("count", equalTo(4)).content("total", equalTo(4)).content("results[0].name", equalTo("host1"))
            .content("results[1].name", equalTo("host2")).when().get(rt.host("/servers.json"));

    given().param("sort", "HOSTNAME_DESC").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).content("count", equalTo(4)).content("total", equalTo(4))
            .content("results[0].name", equalTo("host4")).content("results[1].name", equalTo("host3")).when()
            .get(rt.host("/servers.json"));

    given().param("sort", "STATUS").log().all().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .content("count", equalTo(4)).content("total", equalTo(4)).content("results[0].name", equalTo("host3"))
            .when().get(rt.host("/servers.json"));
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
