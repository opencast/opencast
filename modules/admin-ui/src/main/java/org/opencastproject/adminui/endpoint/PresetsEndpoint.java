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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import org.opencastproject.presets.api.PresetProvider;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "PresetsProxyService", title = "UI Presets",
  abstractText = "This service provides the presets data for the UI.",
  notes = { "This service offers information about organizations and series for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
        immediate = true,
        service = PresetsEndpoint.class,
        property = {
                "service.description=Admin UI - Presets Endpoint",
                "opencast.service.type=org.opencastproject.adminui.PresetsEndpoint",
                "opencast.service.path=/admin-ng/presets",
        }
)
public class PresetsEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(PresetsEndpoint.class);
  /** A preset provider to get the presets from. */
  private PresetProvider presetProvider;

  @Reference
  public void setPresetProvider(PresetProvider presetProvider) {
    this.presetProvider = presetProvider;
  }

  @Activate
  protected void activate(ComponentContext cc) {
    logger.info("Activate presets endpoint");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesId}/property/{propertyName}.json")
  @RestQuery(name = "getProperty", description = "Returns a property value if set as a preset", returnDescription = "Returns the property value", pathParameters = {
          @RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING),
          @RestParameter(name = "propertyName", description = "Name of the property which is the key for it", isRequired = true, type = Type.STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "The access control list."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action") })
  public Response getProperty(@PathParam("seriesId") String seriesId, @PathParam("propertyName") String propertyName)
          throws UnauthorizedException, NotFoundException {
    if (StringUtils.isBlank(seriesId)) {
      logger.warn("Series id parameter is blank '{}'.", seriesId);
      return Response.status(BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(propertyName)) {
      logger.warn("Series property name parameter is blank '{}'.", propertyName);
      return Response.status(BAD_REQUEST).build();
    }
    try {
      String propertyValue = presetProvider.getProperty(seriesId, propertyName);
      return Response.ok(propertyValue).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Could not perform search query", e);
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }
}
