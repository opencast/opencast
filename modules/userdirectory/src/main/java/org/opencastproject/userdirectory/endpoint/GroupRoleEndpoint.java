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

package org.opencastproject.userdirectory.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.security.api.JaxbGroupList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.userdirectory.ConflictException;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

/**
 * A REST EndPoint for JpaGroupRoleProvider.
 */
@Path("/")
@RestService(name = "groups", title = "Internal group manager", abstractText = "This service offers the ability to manage the groups for internal accounts.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
                "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                        + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class GroupRoleEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(GroupRoleEndpoint.class);

  /** The JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** the jpaGroupRoleProvider Impl service */
  private JpaGroupRoleProvider jpaGroupRoleProvider;

  /**
   * @param jpaGroupRoleProvider
   *          the jpaGroupRoleProvider to set
   */
  public void setJpaGroupRoleProvider(JpaGroupRoleProvider jpaGroupRoleProvider) {
    this.jpaGroupRoleProvider = jpaGroupRoleProvider;
  }

  /**
   * Callback for activation of this component.
   */
  public void activate() {
    logger.info("Activating  {}", getClass().getName());
  }

  @GET
  @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON })
  @Path("groups.{format:xml|json}")
  @RestQuery(name = "allgroup", description = "Returns a list of groups", returnDescription = "Returns a JSON or XML representation of the list of groups available the current user's organization", pathParameters = {
          @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The groups.") })
  public Response getGroupsAsJsonOrXml(@PathParam("format") String format, @QueryParam("limit") int limit,
          @QueryParam("offset") int offset)
          throws IOException {
    try {
      final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
      JaxbGroupList list = jpaGroupRoleProvider.getGroupsAsXml(limit, offset);
      return Response.ok().entity(list).type(type).build();
    } catch (Exception e) {
      logger.info("Unable to get groups", e);
      return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
    }
  }

  @DELETE
  @Path("{id}")
  @RestQuery(name = "removegroup", description = "Remove a group", returnDescription = "Return no content", pathParameters = {
          @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Group deleted"),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to remove a group with the admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found."),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An internal server error occured.") })
  public Response removeGroup(@PathParam("id") String groupId) {
    try {
      jpaGroupRoleProvider.removeGroup(groupId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("")
  @RestQuery(name = "createGroup", description = "Add a group", returnDescription = "Return the status codes", restParameters = {
          @RestParameter(name = "name", description = "The group name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The group description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "roles", description = "A comma seperated string of additional group roles", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "users", description = "A comma seperated string of group members", isRequired = false, type = Type.TEXT) }, reponses = {
                  @RestResponse(responseCode = SC_CREATED, description = "Group created"),
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long"),
                  @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a group with the admin role."),
                  @RestResponse(responseCode = SC_CONFLICT, description = "An group with this name already exists.") })
  public Response createGroup(@FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("users") String users) {
    try {
      jpaGroupRoleProvider.createGroup(name, description, roles, users);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to create group {}: {}", name, e.getMessage());
      return Response.status(SC_BAD_REQUEST).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (ConflictException e) {
      return Response.status(SC_CONFLICT).build();
    }
    return Response.status(SC_CREATED).build();
  }

  @PUT
  @Path("{id}")
  @RestQuery(name = "updateGroup", description = "Update a group", returnDescription = "Return the status codes", pathParameters = { @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = Type.STRING) }, restParameters = {
          @RestParameter(name = "name", description = "The group name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The group description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "roles", description = "A comma seperated string of additional group roles", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "users", description = "A comma seperated string of group members", isRequired = true, type = Type.TEXT) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Group updated"),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update a group with the admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long") })
  public Response updateGroup(@PathParam("id") String groupId, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("users") String users) throws NotFoundException {
    try {
      jpaGroupRoleProvider.updateGroup(groupId, name, description, roles, users);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to update group id {}: {}", groupId, e.getMessage());
      return Response.status(SC_BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    }
    return Response.ok().build();
  }

  /**
   * Borrowed from FileUploadRestService.java
   *
   * Builds an error message in case of an unexpected error in an endpoint method, includes the exception type and
   * message if existing.
   *
   * TODO append stack trace
   *
   * @param e
   *          Exception that was thrown
   * @return error message
   */
  private String buildUnexpectedErrorMessage(Exception e) {
    StringBuilder sb = new StringBuilder();
    sb.append("Unexpected error (").append(e.getClass().getName()).append(")");
    String message = e.getMessage();
    if (StringUtils.isNotBlank(message)) {
      sb.append(": ").append(message);
    }
    return sb.toString();
  }
}
