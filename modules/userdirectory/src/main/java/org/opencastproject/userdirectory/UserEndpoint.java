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

package org.opencastproject.userdirectory;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.JaxbUserList;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Provides a sorted set of known users
 */
@Path("/")
@RestService(
    name = "users",
    title = "User account manager",
    notes = "This service offers the ability to manage the roles for internal accounts.",
    abstractText = "Displays the users available in the current user's organization"
)
@Component(
    property = {
        "service.description=User listing REST endpoint",
        "opencast.service.type=org.opencastproject.userdirectory.users",
        "opencast.service.path=/users",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = { UserEndpoint.class }
)
public class UserEndpoint {

  /** The role directory service */
  protected UserDirectoryService userDirectoryService = null;

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference(name = "userDirectoryService")
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @GET
  @Path("users.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(
      name = "allusersasxml",
      description = "Returns a list of users",
      returnDescription = "Returns a XML representation of the list of user accounts",
      restParameters = {
          @RestParameter(
              name = "query",
              description = "The search query, must be at lest 3 characters long.",
              isRequired = false,
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "limit",
              defaultValue = "100",
              description = "The maximum number of items to return per page.",
              isRequired = false,
              type = RestParameter.Type.INTEGER
          ),
          @RestParameter(
              name = "offset",
              defaultValue = "0",
              description = "The page number.",
              isRequired = false,
              type = RestParameter.Type.INTEGER
          ),
      },
      responses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") }
  )
  public Response getUsersAsXml(
      @QueryParam("query") String queryString,
      @QueryParam("limit") int limit,
      @QueryParam("offset") int offset
  ) throws IOException {
    if (limit < 1) {
      limit = 100;
    }

    String query = "%";
    if (StringUtils.isNotBlank(queryString)) {
      if (queryString.trim().length() < 3) {
        return Response.status(Status.BAD_REQUEST).build();
      }
      query = queryString;
    }

    JaxbUserList userList = new JaxbUserList();
    for (Iterator<User> i = userDirectoryService.findUsers(query, offset, limit); i.hasNext();) {
      userList.add(i.next());
    }
    return Response.ok(userList).build();
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
              name = "query",
              description = "The search query, must be at lest 3 characters long.",
              isRequired = false,
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "limit",
              defaultValue = "100",
              description = "The maximum number of items to return per page.",
              isRequired = false,
              type = RestParameter.Type.INTEGER
          ),
          @RestParameter(
              name = "offset",
              defaultValue = "0",
              description = "The page number.",
              isRequired = false,
              type = RestParameter.Type.INTEGER
          ),
      },
      responses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") }
  )
  public Response getUsersAsJson(
      @QueryParam("query") String queryString,
      @QueryParam("limit") int limit,
      @QueryParam("offset") int offset
  ) throws IOException {
    return getUsersAsXml(queryString, limit, offset);
  }

  @GET
  @Path("{username}.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(
      name = "user",
      description = "Returns a user",
      returnDescription = "Returns a XML representation of a user",
      pathParameters = {
          @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING),
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The user account."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found"),
      }
  )
  public JaxbUser getUserAsXml(@PathParam("username") String username) throws NotFoundException {
    User user = userDirectoryService.loadUser(username);
    if (user == null) {
      throw new NotFoundException();
    }
    return JaxbUser.fromUser(user);
  }

  @GET
  @Path("{username}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "user",
      description = "Returns a user",
      returnDescription = "Returns a JSON representation of a user",
      pathParameters = {
          @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING),
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The user account."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found"),
      }
  )
  public JaxbUser getUserAsJson(@PathParam("username") String username) throws NotFoundException {
    return getUserAsXml(username);
  }

}
