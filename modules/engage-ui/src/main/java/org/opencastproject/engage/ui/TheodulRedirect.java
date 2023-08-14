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

package org.opencastproject.engage.ui;

import static javax.servlet.http.HttpServletResponse.SC_TEMPORARY_REDIRECT;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * This REST endpoint redirects users to the currently configured default player, allowing the default to be changed
 * without re-publishing all events.
 */
@Path("/")
@RestService(
    name = "TheodulRedirect",
    title = "Redirect for old Theodul links",
    abstractText = "This service redirects all theodul links back to the default play redirector",
    notes = {})
@Component(
    immediate = true,
    service = TheodulRedirect.class,
    property = {
        "service.description=Theodul Redirect Endpoint",
        "opencast.service.type=org.opencastproject.engage.theodul.player.redirect",
        "opencast.service.path=/engage/theodul/ui"
    }
)
public class TheodulRedirect {

  private static final Logger logger = LoggerFactory.getLogger(PlayerRedirect.class);

  private static final String dep_warning = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "  <head>\n"
      + "<meta charset=\"utf-8\">\n"
      + "<meta name=\"description\" content=\"Opencast Media Player - Deprecation Warning\">\n"
      + "<meta name=\"author\" content=\"Opencast\">\n"
      + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no\">\n"
      + "<title>Opencast Media Player - Deprecation Warning</title>\n" + "  </head>\n" + "  <body>\n"
      + "<div>\n" + "      <img src=\"img/opencast.svg\" alt=\"\" class=\"loadingImg\" />\n"
      + "  <h1>Deprecation Warning</h1>\n" + "      <p>\n"
      + "    Opencast has deprecated this player (Theodul)\n" + "        <br />\n"
      + "    If you see this warning please tell your administrators!\n" + "        <br />\n"
      + "    Go <a href=\"#{url}\">here to continue to your video</a>\n" + "      </p>\n" + "    </div>\n"
      + "</body>\n" + "</html>\n";

  private static final String PLAY = "/play/#{id}";
  private static final String THEODUL_PATH = "/engage/theodul-deprecated/ui/core.html?id=#{id}";

  private SecurityService securityService;

  @Reference
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @GET
  @Path("/core.html")
  @RestQuery(
      name = "redirect",
      description = "Player redirect",
      restParameters = {
          @RestParameter(name = "id", description = "The event identifier", isRequired = false, type = STRING)
      },
      responses = {
          @RestResponse(description = "Returns the redirect", responseCode = SC_TEMPORARY_REDIRECT)
      },
      returnDescription = ""
  )
  public Response redirect(@QueryParam("id") String id) {
    final Organization org = securityService.getOrganization();
    //If we're here and haven't explicitly said we want theodul then throw up warnings
    boolean provideTheodul = Boolean.valueOf(Objects.toString(org.getProperties().get("really.want.theodul"), "false"));
    if (provideTheodul) {
      String playerPath = THEODUL_PATH.replace("#{id}", StringUtils.trimToEmpty(id));
      logger.warn("Providing old Theodul link to deprecated player: {}", playerPath);
      return Response.ok(dep_warning.replace("#{url}", playerPath)).build();
    } else {
      String playerPath = PLAY.replace("#{id}", StringUtils.trimToEmpty(id));
      logger.debug("Transparently redirecting theodul link back to /play forwarder");
      return Response.status(Response.Status.TEMPORARY_REDIRECT).header("location", playerPath).build();
    }
  }
}
