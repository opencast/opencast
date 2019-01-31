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
package org.opencastproject.oaipmh.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.test.rest.RestServiceTestEnv;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AbstractOaiPmhServerInfoRestEndpointTest {
  private static final RestServiceTestEnv env = testEnvForClasses(localhostRandomPort(), TestRestService.class);

  @Test
  public void testHasRepo() throws Exception {
    given().pathParam("repoId", "UNKNOWN")
            .expect()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo("false"))
            .when().get(env.host("/hasrepo/{repoId}"));
    given().pathParam("repoId", "default")
            .expect()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo("true"))
            .when().get(env.host("/hasrepo/{repoId}"));
  }

  @Test
  public void testGetMountPoint() throws Exception {
    given().expect()
            .body(equalTo("/oaipmh"))
            .when().get(env.host("/mountpoint"));
  }

  // CHECKSTYLE:OFF
  @BeforeClass
  public static void setUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void tearDown() {
    env.tearDownServer();
  }
  // CHECKSTYLE:ON
}
