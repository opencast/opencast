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

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.usersettings.UserSetting;
import org.opencastproject.adminui.usersettings.UserSettings;
import org.opencastproject.adminui.usersettings.UserSettingsService;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "usersettings", title = "User Settings service",
  abstractText = "Provides operations for user settings",
  notes = { "This service offers the default CRUD Operations for user settings for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class UserSettingsEndpoint {

  /** The logging facility */
  private static final Log logger = Log.mk(ServerEndpoint.class);

  /** Base url of this endpoint */
  private String endpointBaseUrl;

  private UserSettingsService userSettingsService;

  /**
   * OSGi callback to set the service to retrieve user settings from.
   */
  public void setUserSettingsService(UserSettingsService userSettingsService) {
    this.userSettingsService = userSettingsService;
  }

  /** OSGi callback. */
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Users facade endpoint");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  @GET
  @Path("/settings.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getUserSettings", description = "Returns a list of the user settings for the current user", returnDescription = "Returns a JSON representation of the list of user settings", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user settings.") })
  public Response getUserSettings(@QueryParam("limit") int limit, @QueryParam("offset") int offset) throws IOException {
    if (limit < 1) {
      limit = 100;
    }

    UserSettings userSettings;
    try {
      userSettings = userSettingsService.findUserSettings(limit, offset);
    } catch (UserSettingsServiceException e) {
      logger.error("Unable to get user settings:", e);
      return (Response.serverError().build());
    }

    return Response.ok(userSettings.toJson().toJson()).build();
  }

  @POST
  @Path("/setting")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createUserSetting", description = "Create a new user setting", returnDescription = "Status ok", restParameters = {
          @RestParameter(description = "The key used to represent this setting.", isRequired = true, name = "key", type = STRING),
          @RestParameter(description = "The value representing this setting.", isRequired = true, name = "value", type = STRING) }, reponses = { @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "User setting has been created.") })
  public Response createUserSetting(@FormParam("key") String key, @FormParam("value") String value)
          throws NotFoundException {
    try {
      UserSetting newUserSetting = userSettingsService.addUserSetting(key, value);
      return Response.ok(newUserSetting.toJson().toJson(), MediaType.APPLICATION_JSON).build();
    } catch (UserSettingsServiceException e) {
      return Response.serverError().build();
    }
  }

  @PUT
  @Path("/setting/{settingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateUserSetting", description = "Update a user setting", returnDescription = "The updated user setting as JSON", pathParameters = { @RestParameter(name = "settingId", description = "The setting's id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(description = "The key used to represent this setting.", isRequired = true, name = "key", type = STRING),
          @RestParameter(description = "The value representing this setting.", isRequired = true, name = "value", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "User setting has been created.") })
  public Response updateUserSetting(@PathParam("settingId") final int id, @FormParam("key") String key,
          @FormParam("value") String value) throws NotFoundException {
    try {
      UserSetting updatedUserSetting = userSettingsService.updateUserSetting(id, key, value);
      return Response.ok(updatedUserSetting.toJson().toJson(), MediaType.APPLICATION_JSON).build();
    } catch (UserSettingsServiceException e) {
      logger.error("Unable to update user setting", e);
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("/setting/{settingId}")
  @RestQuery(name = "deleteUserSetting", description = "Delete a user setting", returnDescription = "Status ok", pathParameters = @RestParameter(name = "settingId", type = INTEGER, isRequired = true, description = "The id of the user setting."), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User setting has been deleted."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User setting not found.") })
  public Response deleteUserSetting(@PathParam("settingId") long id) throws NotFoundException {
    try {
      userSettingsService.deleteUserSetting(id);
    } catch (UserSettingsServiceException e) {
      logger.error("Unable to remove user setting id:'%s':'%s'", id, ExceptionUtils.getStackTrace(e));
      return Response.serverError().build();
    }
    logger.debug("User setting with id %d removed.", id);
    return Response.status(SC_OK).build();
  }
}
