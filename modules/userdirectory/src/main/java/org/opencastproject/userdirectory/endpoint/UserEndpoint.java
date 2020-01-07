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

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.JaxbUserList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.userdirectory.JpaUserAndRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

/**
 * Provides a sorted set of known users
 */
@Path("/")
@RestService(
  name = "UsersUtils",
  title = "User utils",
  notes = "This service offers the default CRUD Operations for the internal Opencast users.",
  abstractText = "Provides operations for internal Opencast users")
public class UserEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserEndpoint.class);

  private JpaUserAndRoleProvider jpaUserAndRoleProvider;

  private SecurityService securityService;

  private String endpointBaseUrl;

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    logger.info("Start users endpoint");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param jpaUserAndRoleProvider
   *          the persistenceProperties to set
   */
  public void setJpaUserAndRoleProvider(JpaUserAndRoleProvider jpaUserAndRoleProvider) {
    this.jpaUserAndRoleProvider = jpaUserAndRoleProvider;
  }

  @GET
  @Path("users.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
    name = "allusersasjson",
    description = "Returns a list of users",
    returnDescription = "Returns a JSON representation of the list of user accounts",
    restParameters = {
      @RestParameter(
        name = "limit",
        defaultValue = "100",
        description = "The maximum number of items to return per page.",
        isRequired = false,
        type = RestParameter.Type.STRING),
      @RestParameter(
        name = "offset",
        defaultValue = "0",
        description = "The page number.",
        isRequired = false,
        type = RestParameter.Type.STRING)
    }, reponses = {
      @RestResponse(
        responseCode = SC_OK,
        description = "The user accounts.")
    })
  public JaxbUserList getUsersAsJson(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
          throws IOException {

    // Set the maximum number of items to return to 100 if this limit parameter is not given
    if (limit < 1) {
      limit = 100;
    }

    JaxbUserList userList = new JaxbUserList();
    for (Iterator<User> i = jpaUserAndRoleProvider.findUsers("%", offset, limit); i.hasNext();) {
      userList.add(i.next());
    }
    return userList;
  }

  @GET
  @Path("{username}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
    name = "user",
    description = "Returns a user",
    returnDescription = "Returns a JSON representation of a user",
    pathParameters = {
      @RestParameter(
        name = "username",
        description = "The username.",
        isRequired = true,
        type = STRING)
    }, reponses = {
      @RestResponse(
        responseCode = SC_OK,
        description = "The user account."),
      @RestResponse(
        responseCode = SC_NOT_FOUND,
        description = "User not found")
    })
  public Response getUserAsJson(@PathParam("username") String username) throws NotFoundException {
    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null) {
      logger.debug("Requested user not found: {}", username);
      return Response.status(SC_NOT_FOUND).build();
    }
    return Response.ok(JaxbUser.fromUser(user)).build();
  }

  @GET
  @Path("users/md5.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "users-with-insecure-hashing",
      description = "Returns a list of users which passwords are stored using MD5 hashes",
      returnDescription = "Returns a JSON representation of the list of matching user accounts",
      reponses = {
      @RestResponse(
          responseCode = SC_OK,
          description = "The user accounts.")
  })
  public JaxbUserList getUserWithInsecurePasswordHashingAsJson() {
    JaxbUserList userList = new JaxbUserList();
    for (User user: jpaUserAndRoleProvider.findInsecurePasswordHashes()) {
      userList.add(user);
    }
    return userList;
  }

  @POST
  @Path("/")
  @RestQuery(
    name = "createUser",
    description = "Create a new  user",
    returnDescription = "Location of the new ressource",
    restParameters = {
      @RestParameter(
        name = "username",
        description = "The username.",
        isRequired = true,
        type = STRING),
      @RestParameter(
        name = "password",
        description = "The password.",
        isRequired = true,
        type = STRING),
      @RestParameter(
        name = "name",
        description = "The name.",
        isRequired = false,
        type = STRING),
      @RestParameter(
        name = "email",
        description = "The email.",
        isRequired = false,
        type = STRING),
      @RestParameter(
        name = "roles",
        description = "The user roles as a json array, for example: [\"ROLE_USER\", \"ROLE_ADMIN\"]",
        isRequired = false,
        type = STRING)
    }, reponses = {
      @RestResponse(
        responseCode = SC_BAD_REQUEST,
        description = "Malformed request syntax."),
      @RestResponse(
        responseCode = SC_CREATED,
        description = "User has been created."),
      @RestResponse(
        responseCode = SC_CONFLICT,
        description = "An user with this username already exist."),
      @RestResponse(
        responseCode = SC_FORBIDDEN,
        description = "Not enough permissions to create a user with the admin role.")
    })
  public Response createUser(@FormParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles) {

    if (jpaUserAndRoleProvider.loadUser(username) != null) {
      return Response.status(SC_CONFLICT).build();
    }

    try {
      Set<JpaRole> rolesSet = parseRoles(roles);

      /* Add new user */
      logger.debug("Updating user {}", username);
      JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
      JpaUser user = new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider.getName(), true,
              rolesSet);
      try {
      jpaUserAndRoleProvider.addUser(user);
      return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build();
    } catch (UnauthorizedException ex) {
      logger.debug("Create user failed", ex);
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    } catch (IllegalArgumentException e) {
      logger.debug("Request with malformed ROLE data: {}", roles);
      return Response.status(SC_BAD_REQUEST).build();
    }
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(
    name = "updateUser",
    description = "Update an user",
    returnDescription = "Status ok",
    restParameters = {
      @RestParameter(
        name = "password",
        description = "The password.",
        isRequired = true,
        type = STRING),
      @RestParameter(
        name = "name",
        description = "The name.",
        isRequired = false,
        type = STRING),
      @RestParameter(
        name = "email",
        description = "The email.",
        isRequired = false,
        type = STRING),
      @RestParameter(
        name = "roles",
        description = "The user roles as a json array, for example: [\"ROLE_USER\", \"ROLE_ADMIN\"]",
        isRequired = false,
        type = STRING)
    }, pathParameters = @RestParameter(
      name = "username",
      description = "The username",
      isRequired = true,
      type = STRING),
    reponses = {
      @RestResponse(
        responseCode = SC_BAD_REQUEST,
        description = "Malformed request syntax."),
      @RestResponse(
        responseCode = SC_FORBIDDEN,
        description = "Not enough permissions to update a user with the admin role."),
      @RestResponse(
        responseCode = SC_OK,
        description = "User has been updated.")    })
  public Response setUser(@PathParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles) {

    try {
      User user = jpaUserAndRoleProvider.loadUser(username);
      if (user == null) {
        return createUser(username, password, name, email, roles);
      }

      Set<JpaRole> rolesSet = parseRoles(roles);

      logger.debug("Updating user {}", username);
      JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
      jpaUserAndRoleProvider.updateUser(new JpaUser(username, password, organization, name, email,
                jpaUserAndRoleProvider.getName(), true, rolesSet));
      return Response.status(SC_OK).build();
    } catch (NotFoundException e) {
      logger.debug("User {} not found.", username);
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.debug("Update user failed", e);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IllegalArgumentException e) {
      logger.debug("Request with malformed ROLE data: {}", roles);
      return Response.status(SC_BAD_REQUEST).build();
    }
  }

  @DELETE
  @Path("{username}.json")
  @RestQuery(
    name = "deleteUser",
    description = "Delete a new  user",
    returnDescription = "Status ok",
    pathParameters = @RestParameter(
      name = "username",
      type = STRING,
      isRequired = true,
      description = "The username"),
    reponses = {
      @RestResponse(
        responseCode = SC_OK,
        description = "User has been deleted."),
      @RestResponse(
        responseCode = SC_FORBIDDEN,
        description = "Not enough permissions to delete a user with the admin role."),
      @RestResponse(
        responseCode = SC_NOT_FOUND,
        description = "User not found.")
    })
  public Response deleteUser(@PathParam("username") String username) {
    try {
      jpaUserAndRoleProvider.deleteUser(username, securityService.getOrganization().getId());
    } catch (NotFoundException e) {
      logger.debug("User {} not found.", username);
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.debug("Error during deletion of user {}: {}", username, e);
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("Error during deletion of user {}: {}", username, e);
      return Response.status(SC_INTERNAL_SERVER_ERROR).build();
    }

    logger.debug("User {} removed.", username);
    return Response.status(SC_OK).build();
  }

  /**
   * Parse JSON roles array.
   *
   * @param roles
   *          String representation of JSON array containing roles
   */
  private Set<JpaRole> parseRoles(String roles) throws IllegalArgumentException {
    JSONArray rolesArray = null;
    /* Try parsing JSON. Return Bad Request if malformed. */
    try {
      rolesArray = (JSONArray) JSONValue.parseWithException(StringUtils.isEmpty(roles) ? "[]" : roles);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error parsing JSON array", e);
    }

    Set<JpaRole> rolesSet = new HashSet<JpaRole>();
    /* Add given roles */
    for (Object role : rolesArray) {
      try {
        rolesSet.add(new JpaRole((String) role, (JpaOrganization) securityService.getOrganization()));
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Error parsing array vales as String", e);
      }
    }

    return rolesSet;
  }

}
