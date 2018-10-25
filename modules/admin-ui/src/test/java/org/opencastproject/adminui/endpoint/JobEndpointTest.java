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
import static org.junit.Assert.assertThat;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.test.rest.RestServiceTestEnv;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.restassured.http.ContentType;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class JobEndpointTest {
  private static final Logger logger = LoggerFactory.getLogger(JobEndpointTest.class);
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestJobEndpoint.class);

  private JSONParser parser;

  @Test
  public void testSimpleTasksRequest() throws ParseException, IOException {
    InputStream stream = JobEndpointTest.class.getResourceAsStream("/tasks.json");
    InputStreamReader reader = new InputStreamReader(stream);
    JSONObject expected = (JSONObject) new JSONParser().parse(reader);
    JSONObject actual = (JSONObject) parser.parse(given().expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/tasks.json")).asString());

    ServiceEndpointTestsUtil.testJSONObjectEquality(expected, actual);
  }

  @Test
  public void testJobsRequest() throws Exception {
    String eventString = IOUtils.toString(getClass().getResource("/jobs.json"), "utf-8");

    String actual = given().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/jobs.json")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(actual));

    eventString = IOUtils.toString(getClass().getResource("/jobsLimitOffset.json"), "utf-8");

    actual = given().queryParam("offset", 1).queryParam("limit", 1).expect().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/jobs.json")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(actual));
  }

  @Test
  public void testSortCreator() {
    given().param("sort", "creator:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].creator", equalTo("testuser1"))
            .body("results[1].creator", equalTo("testuser1"))
            .body("results[2].creator", equalTo("testuser2"))
            .body("results[3].creator", equalTo("testuser3"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "creator:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].creator", equalTo("testuser3"))
            .body("results[1].creator", equalTo("testuser2"))
            .body("results[2].creator", equalTo("testuser1"))
            .body("results[3].creator", equalTo("testuser1"))
            .when().get(rt.host("/jobs.json"));
  }

  @Test
  public void testSortOperation() {
    given().param("sort", "operation:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].operation", equalTo("Encode"))
            .body("results[1].operation", equalTo("Inspect"))
            .body("results[2].operation", equalTo("RESUME"))
            .body("results[3].operation", equalTo("test"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "operation:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].operation", equalTo("test"))
            .body("results[1].operation", equalTo("RESUME"))
            .body("results[2].operation", equalTo("Inspect"))
            .body("results[3].operation", equalTo("Encode"))
            .when().get(rt.host("/jobs.json"));
  }

  @Test
  public void testSortProcessingHost() {
    given().param("sort", "processingHost:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].processingHost", equalTo("host1"))
            .body("results[1].processingHost", equalTo("host1"))
            .body("results[2].processingHost", equalTo("host2"))
            .body("results[3].processingHost", equalTo("host3"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "processingHost:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].processingHost", equalTo("host3"))
            .body("results[1].processingHost", equalTo("host2"))
            .body("results[2].processingHost", equalTo("host1"))
            .body("results[3].processingHost", equalTo("host1"))
            .when().get(rt.host("/jobs.json"));
  }

  @Test
  public void testSortStarted() {
    given().param("sort", "started:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].started", equalTo("2014-06-05T09:05:00Z"))
            .body("results[1].started", equalTo("2014-06-05T09:10:00Z"))
            .body("results[2].started", equalTo("2014-06-05T09:11:11Z"))
            .body("results[3].started", equalTo("2014-06-05T09:16:00Z"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "started:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].started", equalTo("2014-06-05T09:16:00Z"))
            .body("results[1].started", equalTo("2014-06-05T09:11:11Z"))
            .body("results[2].started", equalTo("2014-06-05T09:10:00Z"))
            .body("results[3].started", equalTo("2014-06-05T09:05:00Z"))
            .when().get(rt.host("/jobs.json"));
  }

  @Test
  public void testSortSubmitted() {
    given().param("sort", "submitted:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].submitted", equalTo("2014-06-05T09:05:00Z"))
            .body("results[1].submitted", equalTo("2014-06-05T09:10:00Z"))
            .body("results[2].submitted", equalTo("2014-06-05T09:11:11Z"))
            .body("results[3].submitted", equalTo("2014-06-05T09:16:00Z"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "started:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].submitted", equalTo("2014-06-05T09:16:00Z"))
            .body("results[1].submitted", equalTo("2014-06-05T09:11:11Z"))
            .body("results[2].submitted", equalTo("2014-06-05T09:10:00Z"))
            .body("results[3].submitted", equalTo("2014-06-05T09:05:00Z"))
            .when().get(rt.host("/jobs.json"));
  }

  @Test
  public void testSortType() {
    given().param("sort", "type:ASC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].type", equalTo("org.opencastproject.composer"))
            .body("results[1].type", equalTo("org.opencastproject.composer"))
            .body("results[2].type", equalTo("org.opencastproject.inspection"))
            .body("results[3].type", equalTo("org.opencastproject.workflow"))
            .when().get(rt.host("/jobs.json"));

    given().param("sort", "type:DESC").expect()
            .statusCode(org.apache.commons.httpclient.HttpStatus.SC_OK).contentType(ContentType.JSON)
            .body("count", equalTo(4)).body("total", equalTo(4))
            .body("results[0].type", equalTo("org.opencastproject.workflow"))
            .body("results[1].type", equalTo("org.opencastproject.inspection"))
            .body("results[2].type", equalTo("org.opencastproject.composer"))
            .body("results[3].type", equalTo("org.opencastproject.composer"))
            .when().get(rt.host("/jobs.json"));
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
