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
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.test.rest.RestServiceTestEnv;

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

import io.restassured.http.ContentType;

public class ServerEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestServerEndpoint.class);

  private JSONParser parser;

  @Test
  public void testSimpleRequest() throws ParseException, IOException {
    InputStream stream = ServerEndpointTest.class.getResourceAsStream("/servers.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/servers.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testLimitAndOffset() {
    int total = 4;
    int limit = 3;
    int offset = 2;
    given().param("limit", limit).param("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("limit", equalTo(limit)).content("offset", equalTo(offset))
            .body("count", equalTo(total - offset)).content("total", equalTo(total)).when().get(rt.host("/servers.json"));

    limit = 10;
    offset = 2;
    given().param("limit", limit).param("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("limit", equalTo(limit)).content("offset", equalTo(offset))
            .body("count", equalTo(total - offset)).content("total", equalTo(total)).when().get(rt.host("/servers.json"));

    offset = 4;
    given().param("limit", limit).param("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("limit", equalTo(limit)).content("offset", equalTo(offset))
            .body("count", equalTo(total - offset)).content("total", equalTo(total)).when().get(rt.host("/servers.json"));

    limit = 0;
    offset = 0;
    given().param("limit", limit).param("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("limit", equalTo(limit)).content("offset", equalTo(offset))
            .body("count", equalTo(total - offset)).content("total", equalTo(total)).when().get(rt.host("/servers.json"));

    limit = 4;
    offset = -1; // negatiive offset not allowed and will be changed to 0
    given().param("limit", limit).param("offset", offset).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("limit", equalTo(limit)).content("offset", equalTo(offset))
            .body("count", equalTo(4)).content("total", equalTo(total)).when().get(rt.host("/servers.json"));

  }

  @Test
  public void testHostNameFilter() {
    given().param("filter", "hostname:host1").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(1))
            .body("count", equalTo(1))
            .body("results[0].hostname", equalTo("host1"))
            .when().get(rt.host("/servers.json"));

    given().param("filter", "hostname:non-existing").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(0))
            .body("count", equalTo(0))
            .when().get(rt.host("/servers.json"));
  }

  @Test
  public void testStatusFilter() {
    given().param("filter", "status:online").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(3))
            .body("count", equalTo(3))
            .body("results[0].online", equalTo(true))
            .body("results[1].online", equalTo(true))
            .body("results[2].online", equalTo(true))
            .when().get(rt.host("/servers.json"));

    given().param("filter", "status:offline").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(1))
            .body("count", equalTo(1))
            .body("results[0].online", equalTo(false))
            .when().get(rt.host("/servers.json"));

    given().param("filter", "status:maintenance").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(2))
            .body("count", equalTo(2))
            .body("results[0].maintenance", equalTo(true))
            .body("results[1].maintenance", equalTo(true))
            .when().get(rt.host("/servers.json"));
  }

  @Test
  public void testFreeTextFilter() {
    given().param("filter", "textFilter:host1").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(1))
            .body("count", equalTo(1))
            .body("results[0].hostname", equalTo("host1"))
            .when().get(rt.host("/servers.json"));

    given().param("filter", "textFilter:ost").param("sort", "hostname:ASC").expect()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("total", equalTo(4))
            .body("count", equalTo(4))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host2"))
            .body("results[2].hostname", equalTo("host3"))
            .body("results[3].hostname", equalTo("host4"))
            .when().get(rt.host("/servers.json"));
  }

  @Test
  public void testSortCores() throws ParseException {
    given().param("sort", "CORES:ASC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host3"))
            .body("results[1].hostname", equalTo("host2")).when().get(rt.host("/servers.json"));

    given().param("sort", "CORES:DESC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host4")).when().get(rt.host("/servers.json"));
  }

  @Test
  public void testSortHostName() throws ParseException {
    given().param("sort", "HOSTNAME:ASC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host2")).when().get(rt.host("/servers.json"));

    given().param("sort", "HOSTNAME:DESC").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).body("count", equalTo(4)).content("total", equalTo(4))
            .body("results[0].hostname", equalTo("host4")).content("results[1].hostname", equalTo("host3")).when()
            .get(rt.host("/servers.json"));
  }

  @Test
  public void testSortMeanRunTime() {
    given().param("sort", "meanRunTime:ASC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host3"))
            .body("results[1].hostname", equalTo("host1")).content("results[2].hostname", equalTo("host2"))
            .when().get(rt.host("/servers.json"));

    given().param("sort", "meanRunTime:DESC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host4"))
            .body("results[1].hostname", equalTo("host2")).content("results[2].hostname", equalTo("host1"))
            .when().get(rt.host("/servers.json"));
  }

  @Test
  public void testSortMeanQueueTime() {
    given().param("sort", "meanQueueTime:asc").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host2"))
            .body("results[1].hostname", equalTo("host1")).content("results[2].hostname", equalTo("host3"))
            .when().get(rt.host("/servers.json"));

    given().param("sort", "meanQueueTime:desc").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host4"))
            .body("results[1].hostname", equalTo("host3")).content("results[2].hostname", equalTo("host1"))
            .when().get(rt.host("/servers.json"));
  }

  @Test
  public void testSortStatus() throws ParseException {
    given().param("sort", "ONLINE:ASC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host3"))
            .when().get(rt.host("/servers.json"));

    given().param("sort", "ONLINE:DESC").expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).content("total", equalTo(4)).content("results[0].hostname", equalTo("host1"))
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
