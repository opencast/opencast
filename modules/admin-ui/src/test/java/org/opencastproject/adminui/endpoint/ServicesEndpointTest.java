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
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.rest.RestServiceTestEnv;

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

/**
 * Services Endpoint unit tests.
 */
public class ServicesEndpointTest {
  /** Test data ressource json file. */
  private static final String TEST_DATA_JSON = "/services.json";

  /** REST endpoint test environment. */
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestServicesEndpoint.class);

  /** Json parser. */
  private JSONParser parser;

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

  @Test
  public void testSimpleRequest() throws ParseException, IOException {
    InputStream stream = ServicesEndpointTest.class.getResourceAsStream(TEST_DATA_JSON);
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host(TEST_DATA_JSON)).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testLimitAndOffset() {
    given().param("limit", 10).param("offset", 2).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("limit", equalTo(10))
            .body("count", equalTo(4))
            .body("total", equalTo(6))
            .body("results[0].name", equalTo("service3"))
            .body("results[3].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("limit", 2).param("offset", 3).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("limit", equalTo(2))
            .body("count", equalTo(2))
            .body("total", equalTo(6))
            .body("results[0].name", equalTo("service4"))
            .body("results[1].name", equalTo("service5"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("limit", 0).param("offset", 10).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("limit", equalTo(0))
            .body("count", equalTo(0))
            .body("total", equalTo(6))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testNameFilter() {
    given().param("filter", "name:service2").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(1))
            .body("total", equalTo(1))
            .body("results[0].name", equalTo("service2"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "name:service").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(0))
            .body("total", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", " name:service2 ").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(1))
            .body("total", equalTo(1))
            .body("results[0].name", equalTo("service2"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testHostnameFilter() {
    given().param("filter", "hostname:host1").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(2))
            .body("total", equalTo(2))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "hostname:host").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(0))
            .body("total", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", " hostname:host1 ").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(2))
            .body("total", equalTo(2))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testActionsFilter() {
    given().param("filter", "actions:true").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(2))
            .body("total", equalTo(2))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[0].name", equalTo("service2"))
            .body("results[1].hostname", equalTo("host3"))
            .body("results[1].name", equalTo("service4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "actions:false").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(4))
            .body("total", equalTo(4))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[0].name", equalTo("service1"))
            .body("results[1].hostname", equalTo("host2"))
            .body("results[1].name", equalTo("service3"))
            .body("results[2].hostname", equalTo("host2"))
            .body("results[2].name", equalTo("service5"))
            .body("results[3].hostname", equalTo("host4"))
            .body("results[3].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testFreeTextFilter() {
    given().param("filter", "textFilter:host1").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(2))
            .body("total", equalTo(2))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "textFilter:service4").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(1))
            .body("total", equalTo(1))
            .body("results[0].name", equalTo("service4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "textFilter:2").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(3))
            .body("total", equalTo(3))
            .body("results[0].name", equalTo("service2"))
            .body("results[1].hostname", equalTo("host2"))
            .body("results[2].hostname", equalTo("host2"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testHostSort() {
    given().param("sort", "hostname:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[0].hostname", equalTo("host1"))
            .body("results[1].hostname", equalTo("host1"))
            .body("results[2].hostname", equalTo("host2"))
            .body("results[3].hostname", equalTo("host2"))
            .body("results[4].hostname", equalTo("host3"))
            .body("results[5].hostname", equalTo("host4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "hostname:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[5].hostname", equalTo("host1"))
            .body("results[4].hostname", equalTo("host1"))
            .body("results[3].hostname", equalTo("host2"))
            .body("results[2].hostname", equalTo("host2"))
            .body("results[1].hostname", equalTo("host3"))
            .body("results[0].hostname", equalTo("host4"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testNameSort() {
    given().param("sort", "name:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[0].name", equalTo("service1"))
            .body("results[1].name", equalTo("service2"))
            .body("results[2].name", equalTo("service3"))
            .body("results[3].name", equalTo("service4"))
            .body("results[4].name", equalTo("service5"))
            .body("results[5].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "name:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[5].name", equalTo("service1"))
            .body("results[4].name", equalTo("service2"))
            .body("results[3].name", equalTo("service3"))
            .body("results[2].name", equalTo("service4"))
            .body("results[1].name", equalTo("service5"))
            .body("results[0].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testRunningJobsSort() {
    given().param("sort", "running:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[3].running", equalTo(0))
            .body("results[4].running", equalTo(1))
            .body("results[5].running", equalTo(2))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "running:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[0].running", equalTo(2))
            .body("results[1].running", equalTo(1))
            .body("results[2].running", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testQueuedJobsSort() {
    given().param("sort", "queued:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[1].queued", equalTo(0))
            .body("results[2].queued", equalTo(1))
            .body("results[3].queued", equalTo(1))
            .body("results[4].queued", equalTo(3))
            .body("results[5].queued", equalTo(5))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "queued:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[4].queued", equalTo(0))
            .body("results[3].queued", equalTo(1))
            .body("results[2].queued", equalTo(1))
            .body("results[1].queued", equalTo(3))
            .body("results[0].queued", equalTo(5))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testCompletedJobsSort() {
    given().param("sort", "completed:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[2].completed", equalTo(0))
            .body("results[3].completed", equalTo(5))
            .body("results[4].completed", equalTo(10))
            .body("results[5].completed", equalTo(20))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "completed:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[3].completed", equalTo(0))
            .body("results[2].completed", equalTo(5))
            .body("results[1].completed", equalTo(10))
            .body("results[0].completed", equalTo(20))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testMeanRunTimeSort() {
    given().param("sort", "meanRunTime:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[2].meanRunTime", equalTo(0))
            .body("results[3].meanRunTime", equalTo(10))
            .body("results[4].meanRunTime", equalTo(30))
            .body("results[5].meanRunTime", equalTo(123))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "meanRunTime:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[3].meanRunTime", equalTo(0))
            .body("results[2].meanRunTime", equalTo(10))
            .body("results[1].meanRunTime", equalTo(30))
            .body("results[0].meanRunTime", equalTo(123))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testMeanQueuedTimeSort() {
    given().param("sort", "meanQueueTime:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[0].meanQueueTime", equalTo(0))
            .body("results[1].meanQueueTime", equalTo(0))
            .body("results[2].meanQueueTime", equalTo(10))
            .body("results[3].meanQueueTime", equalTo(30))
            .body("results[4].meanQueueTime", equalTo(60))
            .body("results[5].meanQueueTime", equalTo(456))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "meanQueueTime:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[5].meanQueueTime", equalTo(0))
            .body("results[4].meanQueueTime", equalTo(0))
            .body("results[3].meanQueueTime", equalTo(10))
            .body("results[2].meanQueueTime", equalTo(30))
            .body("results[1].meanQueueTime", equalTo(60))
            .body("results[0].meanQueueTime", equalTo(456))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testStatusSort() {
    given().param("sort", "status:asc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[0].status", equalTo("SYSTEMS.SERVICES.STATUS.NORMAL"))
            .body("results[1].status", equalTo("SYSTEMS.SERVICES.STATUS.NORMAL"))
            .body("results[4].status", equalTo("SYSTEMS.SERVICES.STATUS.WARNING"))
            .body("results[5].status", equalTo("SYSTEMS.SERVICES.STATUS.ERROR"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "status:desc").expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("count", equalTo(6))
            .body("total", equalTo(6))
            .body("results[5].status", equalTo("SYSTEMS.SERVICES.STATUS.NORMAL"))
            .body("results[4].status", equalTo("SYSTEMS.SERVICES.STATUS.NORMAL"))
            .body("results[1].status", equalTo("SYSTEMS.SERVICES.STATUS.WARNING"))
            .body("results[0].status", equalTo("SYSTEMS.SERVICES.STATUS.ERROR"))
            .when().get(rt.host(TEST_DATA_JSON));
  }
}
