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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_SEE_OTHER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Endpoints for redirection schemes.
 *
 * Useful for example to enable external applications to implement certain authentication schemes.
 */
@Component(
    service = RedirectEndpoint.class,
    immediate = true,
    property = {
        "service.description=Redirection REST Endpoints",
        "opencast.service.type=org.opencastproject.redirect",
        "opencast.service.path=/redirect",
        "opencast.service.jobproducer=false"
    }
)
@RestService(
    name = "Redirect",
    title = "Redirection Service",
    abstractText = "This service redirects to given URLs.",
    notes = {
        "This is mostly useful for external applications, for example to realize certain (pre-)authentication schemes.."
    }
)
@Path("/")
public class RedirectEndpoint {

  /** Set of URLs to allow redirection to */
  private final ArrayList<Pattern> allowList = new ArrayList<>();

  private static final String ALLOW_LIST_PREFIX = "allow.";

  /** Update configuration */
  @Activate
  @Modified
  public void configure(Map<String, Object> properties) {
    allowList.clear();

    if (properties == null) {
      return;
    }

    properties.forEach((key, value) -> {
      if (key.startsWith(ALLOW_LIST_PREFIX)) {
        allowList.add(Pattern.compile((String) value));
      }
    });
  }

  /**
   * Essentially the Post/Redirect/Get pattern
   *
   * @see <a href="https://en.wikipedia.org/wiki/Post/Redirect/Get">Wikipedia</a>
   */
  @RestQuery(
      name = "get",
      description = "Redirects to the given `target` URL specifically instructing the client to issue a `GET` request.",
      restParameters = {
          @RestParameter(name = "target", description = "The URL to redirect to", isRequired = true, type = STRING)
      },
      responses = {
          @RestResponse(description = "successful redirect", responseCode = SC_SEE_OTHER),
          @RestResponse(description = "missing or invalid target URL", responseCode = SC_BAD_REQUEST),
          @RestResponse(description = "not allowed to redirect to this URL", responseCode = SC_BAD_REQUEST)
      },
      returnDescription = "A temporary redirect to the given `target` URL"
  )
  @POST
  @Path("get")
  public Response get(@FormParam("target") String target) {
    if (target == null) {
      return Response.status(Status.BAD_REQUEST).entity("missing `target` URL").build();
    }

    boolean allowed = allowList.stream()
            .map(pattern -> pattern.matcher(target))
            .anyMatch(Matcher::find);
    if (!allowed) {
      return Response.status(Status.BAD_REQUEST).entity("not allowed to redirect to target").build();
    }

    URI targetUri;
    try {
      targetUri = URI.create(target);
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).entity("invalid `target` URL").build();
    }

    return Response.seeOther(targetUri).build();
  }
}
