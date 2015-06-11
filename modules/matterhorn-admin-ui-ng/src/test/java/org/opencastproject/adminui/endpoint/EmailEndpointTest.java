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
import static org.junit.Assert.assertEquals;
import static org.opencastproject.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.rest.BulkOperationResult;
import org.opencastproject.rest.NotFoundExceptionMapper;
import org.opencastproject.rest.RestServiceTestEnv;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EmailEndpointTest {
  private static final RestServiceTestEnv rt = testEnvForClasses(localhostRandomPort(), TestEmailEndpoint.class,
          NotFoundExceptionMapper.class);

  @BeforeClass
  public static void oneTimeSetUp() {
    rt.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    rt.tearDownServer();
  }

  @Test
  public void testDeleteTemplates() {
    BulkOperationResult emptyResult = new BulkOperationResult();

    BulkOperationResult normalResult = new BulkOperationResult();
    normalResult.addOk(Long.toString(1L));
    normalResult.addOk(Long.toString(2L));
    normalResult.addOk(Long.toString(3L));
    normalResult.addNotFound(Long.toString(4L));

    BulkOperationResult allNotFound = new BulkOperationResult();
    allNotFound.addNotFound(Long.toString(1L));
    allNotFound.addNotFound(Long.toString(2L));
    allNotFound.addNotFound(Long.toString(3L));
    allNotFound.addNotFound(Long.toString(4L));

    given().log().all().expect().statusCode(HttpStatus.SC_BAD_REQUEST).when().post(rt.host("/deleteTemplates"));

    String result = given().body("[]").log().all().expect().statusCode(HttpStatus.SC_OK).when()
            .post(rt.host("/deleteTemplates")).asString();
    assertEquals(emptyResult.toJson(), result);

    result = given().body("[\"1\",\"2\",\"3\",\"4\"]").log().all().expect().statusCode(HttpStatus.SC_OK).when()
            .post(rt.host("/deleteTemplates")).asString();
    assertEquals(normalResult.toJson(), result);
  }
}
