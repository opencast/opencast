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
package org.opencastproject.adminui.endpoint;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.usersettings.UserSetting;
import org.opencastproject.adminui.usersettings.UserSettings;
import org.opencastproject.adminui.usersettings.UserSettingsService;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.comments.Comment;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.messages.MailService;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.persistence.MailServiceException;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "usersettings", title = "User Settings service", notes = "This service offers the default CRUD Operations for user settings for the admin UI.", abstractText = "Provides operations for user settings")
public class UserSettingsEndpoint {

  /** The logging facility */
  private static final Log logger = new Log(LoggerFactory.getLogger(ServerEndpoint.class));

  /** Base url of this endpoint */
  private String endpointBaseUrl;

  private UserSettingsService userSettingsService;
  private MailService mailService;

  /** The security service */
  private SecurityService securityService;

  /**
   * OSGi callback to set the service to retrieve user settings from.
   */
  public void setUserSettingsService(UserSettingsService userSettingsService) {
    this.userSettingsService = userSettingsService;
  }

  /**
   * OSGi callback to set the service to retrieve signatures from.
   */
  public void setMailService(MailService mailService) {
    this.mailService = mailService;
  }

  /** OSGi callback for the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback. */
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Users facade endpoint");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  @GET
  @Path("/signature")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getSignature", description = "Returns the email signatures for the current user", returnDescription = "Returns a JSON representation of the current user's signatures", reponses = { @RestResponse(responseCode = SC_OK, description = "The email signatures.") })
  public Response getSignature() throws IOException, NotFoundException {
    try {
      MessageSignature messageSignature = mailService.getCurrentUsersSignature();
      return Response.ok(messageSignature.toJson().toJson()).build();
    } catch (MailServiceException e) {
      logger.error("Unable to get user settings:", e);
      return (Response.serverError().build());
    }
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
  @Path("/signature")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createemailsignature", description = "Returns the created email signature as JSON", returnDescription = "The created email signature as JSON", restParameters = {
          @RestParameter(name = "name", description = "The signature name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_name", description = "The name to show as the sender of the email", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_address", description = "The email address to use to send the email", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_name", description = "The name to put who they are replying to", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_address", description = "The email address they are replying to", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "text", description = "The signature text", isRequired = true, type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "Returns the created email template as JSON", responseCode = HttpServletResponse.SC_OK) })
  public Response createEmailSignature(@FormParam("name") String name, @FormParam("from_name") String emailName,
          @FormParam("from_address") String address, @FormParam("reply_name") String replyName,
          @FormParam("reply_address") String replyAddress, @FormParam("text") String text) throws NotFoundException {
    User currentUser = securityService.getUser();
    Option<EmailAddress> reply = Option.<EmailAddress> none();
    if (StringUtils.isNotBlank(replyAddress) && StringUtils.isNotBlank(replyName)) {
      reply = Option.some(EmailAddress.emailAddress(replyAddress, replyName));
    }
    EmailAddress emailAddress = EmailAddress.emailAddress(address, emailName);
    MessageSignature messageSignature = new MessageSignature(null, name, currentUser, emailAddress, reply, text,
            new Date(), nil(Comment.class));
    try {
      MessageSignature updatedMessageSignature = mailService.updateMessageSignature(messageSignature);
      return Response.ok(updatedMessageSignature.toJson().toJson()).build();
    } catch (Exception e) {
      logger.error("Could not create the email signature: %s", ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
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
  @Path("/signature/{signatureId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateemailsignature", description = "Returns the updated email signature by the given id as JSON", returnDescription = "The updated email signature as JSON", pathParameters = { @RestParameter(name = "signatureId", description = "The signature id", isRequired = true, type = RestParameter.Type.INTEGER) }, restParameters = {
          @RestParameter(name = "name", description = "The signature name", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_name", description = "The name to show as the sender of the email", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "from_address", description = "The email address to use to send the email", isRequired = true, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_name", description = "The name to put who they are replying to", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "reply_address", description = "The email address they are replying to", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "text", description = "The signature text", isRequired = true, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns the email signature as JSON", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The signature has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response updateEmailSignature(@PathParam("signatureId") long signatureId, @FormParam("name") String name,
          @FormParam("from_name") String emailName, @FormParam("from_address") String address,
          @FormParam("reply_name") String replyName, @FormParam("reply_address") String replyAddress,
          @FormParam("text") String text) throws NotFoundException {

    Option<EmailAddress> reply = Option.<EmailAddress> none();
    if (StringUtils.isNotBlank(replyAddress) && StringUtils.isNotBlank(replyName))
      reply = Option.some(EmailAddress.emailAddress(replyAddress, replyName));

    try {
      MessageSignature messageSignature = mailService.getMessageSignature(signatureId);
      messageSignature.setName(name);
      messageSignature.setSender(EmailAddress.emailAddress(address, emailName));
      messageSignature.setReplyTo(reply);
      messageSignature.setSignature(text);

      MessageSignature updatedMessageSignature = mailService.updateMessageSignature(messageSignature);
      return Response.ok(updatedMessageSignature.toJson().toJson()).build();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update the email signature {}: {}", signatureId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
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
  @Path("/signature/{signatureId}")
  @RestQuery(name = "deleteSignature", description = "Delete a user's signature", returnDescription = "Status ok", pathParameters = @RestParameter(name = "signatureId", type = INTEGER, isRequired = true, description = "The id of the user's signature."), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User's signature has been deleted."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User's signature not found.") })
  public Response deleteSignature(@PathParam("signatureId") long id) throws NotFoundException {
    try {
      mailService.deleteMessageSignature(id);
    } catch (MailServiceException e) {
      logger.error("Unable to delete message signature", e);
      return Response.serverError().build();
    }
    logger.debug("User setting with id %d removed.", id);
    return Response.status(SC_OK).build();
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
