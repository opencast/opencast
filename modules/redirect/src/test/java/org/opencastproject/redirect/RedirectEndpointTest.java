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

package org.opencastproject.redirect;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import javax.ws.rs.core.Response;

/** Tests for the redirection endpoints */
public class RedirectEndpointTest {
  /** The endpoint under test */
  private final RedirectEndpoint endpoint;

  public RedirectEndpointTest() {
    endpoint = new RedirectEndpoint();
    endpoint.configure(Map.of("allow.localhost", "^https?://localhost:"));
  }

  /** Test the `POST /redirect/get` endpoint */
  @Test
  public void testPostRedirectGet() {
    String url = "https://localhost:8080/studio";
    Response response = endpoint.get(url);
    Assert.assertEquals(response.getStatus(), 303);
    Assert.assertEquals(response.getLocation().toString(), url);
  }

  /** Test `POST /redirect/get` with missing target */
  @Test
  public void testPostRedirectGetMissingTarget() {
    Response response = endpoint.get(null);
    Assert.assertEquals(response.getStatus(), 400);
  }

  /** Test `POST /redirect/get` with invalid target */
  @Test
  public void testPostRedirectGetInvalidTarget() {
    Response response = endpoint.get("https://localhost:invalid URL");
    Assert.assertEquals(response.getStatus(), 400);
  }

  /** Test `POST /redirect/get` with a non-allowed URL */
  @Test
  public void testPostRedirectGetNotAllowed() {
    Response response = endpoint.get("https://google.com");
    Assert.assertEquals(response.getStatus(), 400);
  }
}
