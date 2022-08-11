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

package org.opencastproject.editor.external.url;

import static javax.servlet.http.HttpServletResponse.SC_SEE_OTHER;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * External URL for the internal Opencast editor.
 */
@Path("/")
@RestService(name = "EditorExternalUrlEndpoint",
    title = "Editor External URL Endpoint",
    abstractText = "External URL Endpoint",
    notes = {
        "This provides an external URL for the internal Opencast editor e.g. for use with Shibboleth."
    }
)
@Component(
    immediate = true,
    service = EditorExternalUrlEndpoint.class,
    property = {
        "service.description=External URL for internal Opencast editor",
        "opencast.service.type=org.opencastproject.editor.external.url.EditorExternalUrlEndpoint",
        "opencast.service.path=/admin-ng/editor",
    }
)
public class EditorExternalUrlEndpoint {

  @GET
  @Path("{eventId}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
      name = "editor",
      description = "An external URL for the internal Opencast editor",
      pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, type = Type.STRING, description = "Event ID")
      },
      responses = {
          @RestResponse(description = "Redirect", responseCode = SC_SEE_OTHER)
      },
      returnDescription = "Redirects to the internal Opencast editor.")
  public Response editorExternalURL(@PathParam("eventId") String eventId) {
    Response.ResponseBuilder builder = Response.status(Response.Status.SEE_OTHER);
    builder.header("Location", "/admin-ng/index.html#!/events/events/" + eventId + "/tools/editor");
    return builder.build();
  }
}
