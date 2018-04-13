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
    JSONObject actual = (JSONObject) parser.parse(given().log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host(TEST_DATA_JSON)).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testLimitAndOffset() {
    given().param("limit", 10).param("offset", 2).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("limit", equalTo(10))
            .content("count", equalTo(4))
            .content("total", equalTo(6))
            .content("results[0].name", equalTo("service3"))
            .content("results[3].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("limit", 2).param("offset", 3).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("limit", equalTo(2))
            .content("count", equalTo(2))
            .content("total", equalTo(6))
            .content("results[0].name", equalTo("service4"))
            .content("results[1].name", equalTo("service5"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("limit", 0).param("offset", 10).log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("limit", equalTo(0))
            .content("count", equalTo(0))
            .content("total", equalTo(6))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testNameFilter() {
    given().param("filter", "name:service2").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(1))
            .content("total", equalTo(1))
            .content("results[0].name", equalTo("service2"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "name:service").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(0))
            .content("total", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", " name:service2 ").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(1))
            .content("total", equalTo(1))
            .content("results[0].name", equalTo("service2"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testHostnameFilter() {
    given().param("filter", "hostname:host1").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(2))
            .content("total", equalTo(2))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "hostname:host").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(0))
            .content("total", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", " hostname:host1 ").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(2))
            .content("total", equalTo(2))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testActionsFilter() {
    given().param("filter", "actions:true").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(2))
            .content("total", equalTo(2))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[0].name", equalTo("service2"))
            .content("results[1].hostname", equalTo("host3"))
            .content("results[1].name", equalTo("service4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "actions:false").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(4))
            .content("total", equalTo(4))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[0].name", equalTo("service1"))
            .content("results[1].hostname", equalTo("host2"))
            .content("results[1].name", equalTo("service3"))
            .content("results[2].hostname", equalTo("host2"))
            .content("results[2].name", equalTo("service5"))
            .content("results[3].hostname", equalTo("host4"))
            .content("results[3].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testFreeTextFilter() {
    given().param("filter", "textFilter:host1").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(2))
            .content("total", equalTo(2))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[1].hostname", equalTo("host1"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "textFilter:service4").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(1))
            .content("total", equalTo(1))
            .content("results[0].name", equalTo("service4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("filter", "textFilter:2").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(3))
            .content("total", equalTo(3))
            .content("results[0].name", equalTo("service2"))
            .content("results[1].hostname", equalTo("host2"))
            .content("results[2].hostname", equalTo("host2"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testHostSort() {
    given().param("sort", "hostname:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[0].hostname", equalTo("host1"))
            .content("results[1].hostname", equalTo("host1"))
            .content("results[2].hostname", equalTo("host2"))
            .content("results[3].hostname", equalTo("host2"))
            .content("results[4].hostname", equalTo("host3"))
            .content("results[5].hostname", equalTo("host4"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "hostname:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[5].hostname", equalTo("host1"))
            .content("results[4].hostname", equalTo("host1"))
            .content("results[3].hostname", equalTo("host2"))
            .content("results[2].hostname", equalTo("host2"))
            .content("results[1].hostname", equalTo("host3"))
            .content("results[0].hostname", equalTo("host4"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testNameSort() {
    given().param("sort", "name:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[0].name", equalTo("service1"))
            .content("results[1].name", equalTo("service2"))
            .content("results[2].name", equalTo("service3"))
            .content("results[3].name", equalTo("service4"))
            .content("results[4].name", equalTo("service5"))
            .content("results[5].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "name:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[5].name", equalTo("service1"))
            .content("results[4].name", equalTo("service2"))
            .content("results[3].name", equalTo("service3"))
            .content("results[2].name", equalTo("service4"))
            .content("results[1].name", equalTo("service5"))
            .content("results[0].name", equalTo("service6"))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testRunningJobsSort() {
    given().param("sort", "running:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[3].running", equalTo(0))
            .content("results[4].running", equalTo(1))
            .content("results[5].running", equalTo(2))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "running:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[0].running", equalTo(2))
            .content("results[1].running", equalTo(1))
            .content("results[2].running", equalTo(0))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testQueuedJobsSort() {
    given().param("sort", "queued:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[1].queued", equalTo(0))
            .content("results[2].queued", equalTo(1))
            .content("results[3].queued", equalTo(1))
            .content("results[4].queued", equalTo(3))
            .content("results[5].queued", equalTo(5))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "queued:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[4].queued", equalTo(0))
            .content("results[3].queued", equalTo(1))
            .content("results[2].queued", equalTo(1))
            .content("results[1].queued", equalTo(3))
            .content("results[0].queued", equalTo(5))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testCompletedJobsSort() {
    given().param("sort", "completed:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[2].completed", equalTo(0))
            .content("results[3].completed", equalTo(5))
            .content("results[4].completed", equalTo(10))
            .content("results[5].completed", equalTo(20))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "completed:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[3].completed", equalTo(0))
            .content("results[2].completed", equalTo(5))
            .content("results[1].completed", equalTo(10))
            .content("results[0].completed", equalTo(20))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testMeanRunTimeSort() {
    given().param("sort", "meanRunTime:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[2].meanRunTime", equalTo(0))
            .content("results[3].meanRunTime", equalTo(10))
            .content("results[4].meanRunTime", equalTo(30))
            .content("results[5].meanRunTime", equalTo(123))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "meanRunTime:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[3].meanRunTime", equalTo(0))
            .content("results[2].meanRunTime", equalTo(10))
            .content("results[1].meanRunTime", equalTo(30))
            .content("results[0].meanRunTime", equalTo(123))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testMeanQueuedTimeSort() {
    given().param("sort", "meanQueueTime:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[0].meanQueueTime", equalTo(0))
            .content("results[1].meanQueueTime", equalTo(0))
            .content("results[2].meanQueueTime", equalTo(10))
            .content("results[3].meanQueueTime", equalTo(30))
            .content("results[4].meanQueueTime", equalTo(60))
            .content("results[5].meanQueueTime", equalTo(456))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "meanQueueTime:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[5].meanQueueTime", equalTo(0))
            .content("results[4].meanQueueTime", equalTo(0))
            .content("results[3].meanQueueTime", equalTo(10))
            .content("results[2].meanQueueTime", equalTo(30))
            .content("results[1].meanQueueTime", equalTo(60))
            .content("results[0].meanQueueTime", equalTo(456))
            .when().get(rt.host(TEST_DATA_JSON));
  }

  @Test
  public void testStatusSort() {
    given().param("sort", "status:asc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[0].status", equalTo("NORMAL"))
            .content("results[1].status", equalTo("NORMAL"))
            .content("results[4].status", equalTo("WARNING"))
            .content("results[5].status", equalTo("ERROR"))
            .when().get(rt.host(TEST_DATA_JSON));

    given().param("sort", "status:desc").log().all().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .content("count", equalTo(6))
            .content("total", equalTo(6))
            .content("results[5].status", equalTo("NORMAL"))
            .content("results[4].status", equalTo("NORMAL"))
            .content("results[1].status", equalTo("WARNING"))
            .content("results[0].status", equalTo("ERROR"))
            .when().get(rt.host(TEST_DATA_JSON));
  }
}
