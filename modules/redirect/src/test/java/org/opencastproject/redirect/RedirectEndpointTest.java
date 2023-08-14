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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/** Tests for the redirection endpoints */
public class RedirectEndpointTest {
  /** The endpoint under test */
  private final RedirectEndpoint endpoint = new RedirectEndpoint();
  private final UriInfo uriInfo;
  {
    uriInfo = createNiceMock(UriInfo.class);
    expect(uriInfo.getBaseUri())
            .andReturn(URI.create("http://localhost:8080"))
            .anyTimes();
    replay(uriInfo);
  }

  /** Test the `POST /redirect/get` endpoint */
  @Test
  public void testPostRedirectGet() throws MalformedURLException {
    String target = "/studio";
    Response response = endpoint.get(target, uriInfo);
    Assert.assertEquals(303, response.getStatus());
    String expected = new URL(uriInfo.getBaseUri().toURL(), target).toString();
    Assert.assertEquals(response.getLocation().toString(), expected);
  }

  /** Test `POST /redirect/get` with missing target */
  @Test
  public void testPostRedirectGetMissingTarget() {
    Response response = endpoint.get(null, uriInfo);
    Assert.assertEquals(400, response.getStatus());
  }

  /** Test `POST /redirect/get` with invalid target */
  @Test
  public void testPostRedirectGetInvalidTarget() {
    Response response = endpoint.get("/%", uriInfo);
    Assert.assertEquals(400, response.getStatus());
  }

  /** Test `POST /redirect/get` with a non-relative target */
  @Test
  public void testPostRedirectGetNonRelativeTarget() {
    Response response = endpoint.get("https://opencast.org", uriInfo);
    Assert.assertEquals(400, response.getStatus());
  }
}
