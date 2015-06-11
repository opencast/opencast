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
import static org.junit.Assert.assertThat;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.adminui.util.ServiceEndpointTestsUtil;
import org.opencastproject.rest.RestServiceTestEnv;

import com.jayway.restassured.http.ContentType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JobEndpointTest {
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
    String eventString = IOUtils.toString(getClass().getResource("/jobs.json"));

    String actual = given().expect().statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).when()
            .get(rt.host("/jobs.json")).asString();

    assertThat(eventString, SameJSONAs.sameJSONAs(actual));

    eventString = IOUtils.toString(getClass().getResource("/jobsLimitOffset.json"));

    actual = given().queryParam("offset", 1).queryParam("limit", 1).expect().log().all().statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON).when().get(rt.host("/jobs.json")).asString();
    System.out.print(actual);

    assertThat(eventString, SameJSONAs.sameJSONAs(actual));
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
