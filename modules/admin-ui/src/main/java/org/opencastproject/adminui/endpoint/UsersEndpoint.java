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

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
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

import org.opencastproject.adminui.util.TextFilter;
import org.opencastproject.elasticsearch.api.SearchQuery.Order;
import org.opencastproject.elasticsearch.api.SortCriterion;
import org.opencastproject.index.service.resources.list.query.UsersListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.userdirectory.JpaUserAndRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
@RestService(name = "users", title = "User service",
  abstractText = "Provides operations for users",
  notes = { "This service offers the default users CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
  immediate = true,
  service = UsersEndpoint.class,
  property = {
    "service.description=Admin UI - Users facade Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.UsersEndpoint",
    "opencast.service.path=/admin-ng/users"
  }
)
public class UsersEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(UsersEndpoint.class);

  /** The global user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The internal role and user provider */
  private JpaUserAndRoleProvider jpaUserAndRoleProvider;

  /** The security service */
  private SecurityService securityService;

  /** Base url of this endpoint */
  private String endpointBaseUrl;

  /** For JSON serialization */
  private static final Type listType = new TypeToken<ArrayList<JsonRole>>() { }.getType();
  private static final Gson gson = new Gson();

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param jpaUserAndRoleProvider
   *          the user provider to set
   */
  @Reference
  public void setJpaUserAndRoleProvider(JpaUserAndRoleProvider jpaUserAndRoleProvider) {
    this.jpaUserAndRoleProvider = jpaUserAndRoleProvider;
  }

  /** OSGi callback. */
  @Activate
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Users facade endpoint");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  @GET
  @Path("users.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "allusers", description = "Returns a list of users", returnDescription = "Returns a JSON representation of the list of user accounts", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: STATUS, NAME OR LAST_UPDATED.  Add '_DESC' to reverse the sort order (e.g. STATUS_DESC).", type = STRING),
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, responses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  public Response getUsers(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("limit") int limit, @QueryParam("offset") int offset) throws IOException {
    if (limit < 1)
      limit = 100;

    sort = trimToNull(sort);
    String filterName = null;
    String filterRole = null;
    String filterProvider = null;
    String filterText = null;

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      String value = filters.get(name);
      if (UsersListQuery.FILTER_NAME_NAME.equals(name)) {
        filterName = value;
      } else if (UsersListQuery.FILTER_ROLE_NAME.equals(name)) {
        filterRole = value;
      } else if (UsersListQuery.FILTER_PROVIDER_NAME.equals(name)) {
        filterProvider = value;
      } else if ((UsersListQuery.FILTER_TEXT_NAME.equals(name)) && (StringUtils.isNotBlank(value))) {
        filterText = value;
      }
    }

    // Filter users by filter criteria
    List<User> filteredUsers = new ArrayList<>();
    for (Iterator<User> i = userDirectoryService.getUsers(); i.hasNext();) {
      User user = i.next();

      // Filter list
      final String finalFilterRole = filterRole;
      if (filterName != null && !filterName.equals(user.getName())
              || (filterRole != null
        && user.getRoles().stream().noneMatch((r) -> r.getName().equals(finalFilterRole)))
              || (filterProvider != null
                  && !filterProvider.equals(user.getProvider()))
              || (filterText != null
                  && !TextFilter.match(filterText, user.getUsername(), user.getName(), user.getEmail(), user.getProvider())
                  && !TextFilter.match(filterText,
                      user.getRoles().stream().map(Role::getName).collect(Collectors.joining(" "))))) {
        continue;
      }
      filteredUsers.add(user);
    }
    int total = filteredUsers.size();

    // Sort by name, description or role
    if (sort != null) {
      final Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);
      filteredUsers.sort((user1, user2) -> {
        for (SortCriterion criterion : sortCriteria) {
          Order order = criterion.getOrder();
          switch (criterion.getFieldName()) {
            case "name":
              if (order.equals(Order.Descending))
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getName()), trimToEmpty(user1.getName()));
              return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getName()), trimToEmpty(user2.getName()));
            case "username":
              if (order.equals(Order.Descending))
                return CASE_INSENSITIVE_ORDER
                  .compare(trimToEmpty(user2.getUsername()), trimToEmpty(user1.getUsername()));
              return CASE_INSENSITIVE_ORDER
                .compare(trimToEmpty(user1.getUsername()), trimToEmpty(user2.getUsername()));
            case "email":
              if (order.equals(Order.Descending))
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getEmail()), trimToEmpty(user1.getEmail()));
              return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getEmail()), trimToEmpty(user2.getEmail()));
            case "roles":
              String roles1 = user1.getRoles().stream().map(Role::getName).collect(Collectors.joining(","));
              String roles2 = user1.getRoles().stream().map(Role::getName).collect(Collectors.joining(","));
              if (order.equals(Order.Descending))
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(roles2), trimToEmpty(roles1));
              return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(roles1), trimToEmpty(roles2));
            case "provider":
              if (order.equals(Order.Descending))
                return CASE_INSENSITIVE_ORDER
                  .compare(trimToEmpty(user2.getProvider()), trimToEmpty(user1.getProvider()));
              return CASE_INSENSITIVE_ORDER
                .compare(trimToEmpty(user1.getProvider()), trimToEmpty(user2.getProvider()));
            default:
              logger.info("Unknown sort type: {}", criterion.getFieldName());
              return 0;
          }
        }
        return 0;
      });
    }

    // Apply Limit and offset
    filteredUsers = new SmartIterator<User>(limit, offset).applyLimitAndOffset(filteredUsers);

    List<Map<String, Object>> usersJSON = new ArrayList<>();
    for (User user : filteredUsers) {
      usersJSON.add(generateJsonUser(user));
    }

    Map<String, Object> response = new HashMap<>();
    response.put("results", usersJSON);
    response.put("count", usersJSON.size());
    response.put("offset", offset);
    response.put("limit", limit);
    response.put("total", total);
    return Response.ok(gson.toJson(response)).build();
  }

  @POST
  @Path("/")
  @RestQuery(name = "createUser", description = "Create a new  user", returnDescription = "The location of the new ressource", restParameters = {
          @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING),
          @RestParameter(description = "The password.", isRequired = true, name = "password", type = STRING),
          @RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING),
          @RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array, e.g. <br>"
                  + "[{'name': 'ROLE_ADMIN', 'type': 'INTERNAL'}, {'name': 'ROLE_XY', 'type': 'INTERNAL'}]") },
          responses = {
          @RestResponse(responseCode = SC_CREATED, description = "User has been created."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a user with a admin role."),
          @RestResponse(responseCode = SC_CONFLICT, description = "An user with this username already exist.")})
  public Response createUser(@FormParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles) {

    if (StringUtils.isBlank(username)) {
      return Response.status(SC_BAD_REQUEST).entity("Missing username").build();
    }
    if (StringUtils.isBlank(password)) {
      return Response.status(SC_BAD_REQUEST).entity("Missing password").build();
    }

    User existingUser = jpaUserAndRoleProvider.loadUser(username);
    if (existingUser != null) {
      return Response.status(SC_CONFLICT).build();
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    Set<JpaRole> rolesSet;
    try {
      rolesSet = parseJsonRoles(roles);
    } catch (IllegalArgumentException e) {
      logger.debug("Received invalid JSON for roles", e);
      return Response.status(SC_BAD_REQUEST).entity("Invalid JSON for roles").build();
    }

    if (rolesSet == null) {
      rolesSet = new HashSet<>();
      rolesSet.add(new JpaRole(organization.getAnonymousRole(), organization));
    }

    JpaUser user = new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider.getName(), true,
            rolesSet);
    try {
      jpaUserAndRoleProvider.addUser(user);
      return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    }
  }

  @GET
  @Path("{username}.json")
  @RestQuery(name = "getUser", description = "Get an user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), responses = {
          @RestResponse(responseCode = SC_OK, description = "User has been found."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response getUser(@PathParam("username") String username) {

    User user = userDirectoryService.loadUser(username);
    if (user == null) {
      return Response.status(SC_NOT_FOUND).build();
    }

    return Response.ok(gson.toJson(generateJsonUser(user))).build();
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(name = "updateUser", description = "Update an user", returnDescription = "Status ok", restParameters = {
          @RestParameter(description = "The password.", isRequired = false, name = "password", type = STRING),
          @RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING),
          @RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array") }, pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), responses = {
          @RestResponse(responseCode = SC_OK, description = "User has been updated."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update a user with admin role."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Invalid data provided.")})
  public Response updateUser(@PathParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles) {

    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null) {
      return createUser(username, password, name, email, roles);
    }

    Set<JpaRole> rolesSet;
    try {
      rolesSet = parseJsonRoles(roles);
    } catch (IllegalArgumentException e) {
      logger.debug("Received invalid JSON for roles", e);
      return Response.status(SC_BAD_REQUEST).build();
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    if (rolesSet == null) {
      //  use the previous roles if no new ones are provided
      rolesSet = new HashSet<>();
      for (Role role : user.getRoles()) {
        rolesSet.add(new JpaRole(role.getName(), organization, role.getDescription(), role.getType()));
      }
    }

    try {
      jpaUserAndRoleProvider.updateUser(new JpaUser(username, password, organization, name, email,
        jpaUserAndRoleProvider.getName(), true, rolesSet));
      userDirectoryService.invalidate(username);
      return Response.status(SC_OK).build();
    } catch (UnauthorizedException ex) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (NotFoundException e) {
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{username}.json")
  @RestQuery(name = "deleteUser", description = "Deleter a new  user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), responses = {
          @RestResponse(responseCode = SC_OK, description = "User has been deleted."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to delete a user with admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response deleteUser(@PathParam("username") String username) throws NotFoundException {
    Organization organization = securityService.getOrganization();

    try {
      jpaUserAndRoleProvider.deleteUser(username, organization.getId());
      userDirectoryService.invalidate(username);
    } catch (NotFoundException e) {
      logger.debug("User {} not found.", username);
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("Error during deletion of user {}: {}", username, e);
      return Response.status(SC_INTERNAL_SERVER_ERROR).build();
    }

    logger.debug("User {} removed.", username);
    return Response.status(SC_OK).build();
  }

  /**
   * Parse a JSON roles string.
   *
   * @param roles
   *          Array of roles as JSON strings.
   * @return Set of roles or null
   * @throws IllegalArgumentException
   *          Invalid JSON data
   */
  private Set<JpaRole> parseJsonRoles(final String roles) throws IllegalArgumentException {
    List<JsonRole> rolesList;
    try {
      rolesList = gson.fromJson(roles, listType);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    if (rolesList == null) {
      return null;
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    Set<JpaRole> rolesSet = new HashSet<>();
    for (JsonRole role: rolesList) {
      try {
        rolesSet.add(new JpaRole(role.getName(), organization, null, role.getType()));
      } catch (NullPointerException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return rolesSet;
  }

  private Map<String, Object> generateJsonUser(User user) {
    // Prepare the roles
    Map<String, Object> userData = new HashMap<>();
    userData.put("username", user.getUsername());
    userData.put("manageable", user.isManageable());
    userData.put("name", user.getName());
    userData.put("email", user.getEmail());
    userData.put("provider", user.getProvider());
    userData.put("roles", user.getRoles().stream()
      .sorted(Comparator.comparing(Role::getName))
      .map((r) -> new JsonRole(r.getName(), r.getType()))
      .collect(Collectors.toList()));
    return userData;
  }

  class JsonRole {
    private String name;
    private String type;

    JsonRole(String name, Role.Type type) {
      this.name = name;
      this.type = type.toString();
    }

    public String getName() {
      return name;
    }

    public Role.Type getType() {
      return Role.Type.valueOf(type);
    }
  }

}
