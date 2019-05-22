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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * This REST endpoint redirects users to the currently configured default player, allowing the default to be changed
 * without re-publishing all events.
 */
@Path("/")
@RestService(
  name = "PlayerRedirect",
  title = "Configurable Player Endpoint",
  abstractText = "This service redirects to configured players.",
  notes = {})
public class PlayerRedirect {

  private static final Logger logger = LoggerFactory.getLogger(PlayerRedirect.class);

  private static final String PLAYER_DEFAULT = "/engage/theodul/ui/core.html?id=#{id}";

  private SecurityService securityService;

  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @GET
  @Path("/{id:.+}")
  @RestQuery(name = "redirect", description = "Player redirect",
          pathParameters = {
            @RestParameter(name = "id", description = "The event identifier", isRequired = true, type = STRING)
          }, reponses = {
            @RestResponse(description = "Returns the paella configuration", responseCode = SC_TEMPORARY_REDIRECT)
          }, returnDescription = "")
  public Response redirect(@PathParam("id") String id) {
    final Organization org = securityService.getOrganization();
    final String playerPath = Objects.toString(org.getProperties().get("player"), PLAYER_DEFAULT)
            .replace("#{id}", id);
    logger.debug("redirecting to player: {}", playerPath);
    return Response
            .status(Response.Status.TEMPORARY_REDIRECT)
            .header("location", playerPath)
            .build();
  }
}
