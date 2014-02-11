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
package org.opencastproject.episode.endpoint;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.rest.RestServiceTestEnv;
import org.opencastproject.util.UrlSupport;

import javax.ws.rs.core.Response;

import static com.jayway.restassured.RestAssured.given;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.rest.RestServiceTestEnv.testEnvForClasses;

@Ignore
public class EpisodeServiceRestEndpointTest {
  private static final RestServiceTestEnv env = testEnvForClasses(UrlSupport.url(TestRestService.BASE_URL), TestRestService.class);

  private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
  private static final int OK = Response.Status.OK.getStatusCode();
  private static final int CREATED = Response.Status.CREATED.getStatusCode();
  private static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
  private static final int NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();
  private static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();

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

  @Test
  public void testFind() {
    final MediaPackage mp = loadFromClassPath("/manifest-full.xml");
    given().formParam("mediapackage", MediaPackageParser.getAsXml(mp))
            .expect().statusCode(NO_CONTENT).when().post(env.host("/users"));

  }
}
