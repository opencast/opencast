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

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.util.TextFilter;
import org.opencastproject.authorization.xacml.manager.endpoint.JsonConv;
import org.opencastproject.index.service.resources.list.query.UsersListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;
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
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

@Path("/")
@RestService(name = "users", title = "User service",
  abstractText = "Provides operations for users",
  notes = { "This service offers the default users CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
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

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
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
   *          the user provider to set
   */
  public void setJpaUserAndRoleProvider(JpaUserAndRoleProvider jpaUserAndRoleProvider) {
    this.jpaUserAndRoleProvider = jpaUserAndRoleProvider;
  }

  /** OSGi callback. */
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
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  public Response getUsers(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("limit") int limit, @QueryParam("offset") int offset) throws IOException {
    if (limit < 1)
      limit = 100;

    Option<String> optSort = Option.option(trimToNull(sort));
    Option<String> filterName = Option.none();
    Option<String> filterRole = Option.none();
    Option<String> filterProvider = Option.none();
    Option<String> filterText = Option.none();

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      String value = filters.get(name);
      if (UsersListQuery.FILTER_NAME_NAME.equals(name)) {
        filterName = Option.some(value);
      } else if (UsersListQuery.FILTER_ROLE_NAME.equals(name)) {
        filterRole = Option.some(value);
      } else if (UsersListQuery.FILTER_PROVIDER_NAME.equals(name)) {
        filterProvider = Option.some(value);
      } else if ((UsersListQuery.FILTER_TEXT_NAME.equals(name)) && (StringUtils.isNotBlank(value))) {
        filterText = Option.some(value);
      }
    }

    // Filter users by filter criteria
    List<User> filteredUsers = new ArrayList<>();
    for (Iterator<User> i = userDirectoryService.getUsers(); i.hasNext();) {
      User user = i.next();

      // Filter list
      if (filterName.isSome() && !filterName.get().equals(user.getName())
              || (filterRole.isSome()
                  && !Stream.$(user.getRoles()).map(getRoleName).toSet().contains(filterRole.get()))
              || (filterProvider.isSome()
                  && !filterProvider.get().equals(user.getProvider()))
              || (filterText.isSome()
                  && !TextFilter.match(filterText.get(), user.getUsername(), user.getName(), user.getEmail(), user.getProvider())
                  && !TextFilter.match(filterText.get(), Stream.$(user.getRoles()).map(getRoleName).mkString(" ")))) {
        continue;
      }
      filteredUsers.add(user);
    }
    int total = filteredUsers.size();

    // Sort by name, description or role
    if (optSort.isSome()) {
      final Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      Collections.sort(filteredUsers, new Comparator<User>() {
        @Override
        public int compare(User user1, User user2) {
          for (SortCriterion criterion : sortCriteria) {
            Order order = criterion.getOrder();
            switch (criterion.getFieldName()) {
              case "name":
                if (order.equals(Order.Descending))
                  return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getName()), trimToEmpty(user1.getName()));
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getName()), trimToEmpty(user2.getName()));
              case "username":
                if (order.equals(Order.Descending))
                  return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getUsername()),
                          trimToEmpty(user1.getUsername()));
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getUsername()),
                        trimToEmpty(user2.getUsername()));
              case "email":
                if (order.equals(Order.Descending))
                  return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getEmail()), trimToEmpty(user1.getEmail()));
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getEmail()), trimToEmpty(user2.getEmail()));
              case "roles":
                String roles1 = Stream.$(user1.getRoles()).map(getRoleName).sort(sortByName).mkString(",");
                String roles2 = Stream.$(user2.getRoles()).map(getRoleName).sort(sortByName).mkString(",");
                if (order.equals(Order.Descending))
                  return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(roles2), trimToEmpty(roles1));
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(roles1), trimToEmpty(roles2));
              case "provider":
                if (order.equals(Order.Descending))
                  return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user2.getProvider()),
                          trimToEmpty(user1.getProvider()));
                return CASE_INSENSITIVE_ORDER.compare(trimToEmpty(user1.getProvider()),
                        trimToEmpty(user2.getProvider()));
              default:
                logger.info("Unkown sort type: {}", criterion.getFieldName());
                return 0;
            }
          }
          return 0;
        }
      });
    }

    // Apply Limit and offset
    filteredUsers = new SmartIterator<User>(limit, offset).applyLimitAndOffset(filteredUsers);

    List<JValue> usersJSON = new ArrayList<>();
    for (User user : filteredUsers) {
      usersJSON.add(generateJsonUser(user));
    }

    return okJsonList(usersJSON, offset, limit, total);
  }

  @POST
  @Path("/")
  @RestQuery(name = "createUser", description = "Create a new  user", returnDescription = "The location of the new ressource", restParameters = {
          @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING),
          @RestParameter(description = "The password.", isRequired = true, name = "password", type = STRING),
          @RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING),
          @RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array") }, reponses = {
          @RestResponse(responseCode = SC_CREATED, description = "User has been created."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a user with a admin role."),
          @RestResponse(responseCode = SC_CONFLICT, description = "An user with this username already exist.")})
  public Response createUser(@FormParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles)
          throws NotFoundException {

    if (StringUtils.isBlank(username))
      return RestUtil.R.badRequest("No username set");
    if (StringUtils.isBlank(password))
      return RestUtil.R.badRequest("No password set");

    User existingUser = jpaUserAndRoleProvider.loadUser(username);
    if (existingUser != null) {
      return Response.status(SC_CONFLICT).build();
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();

    Option<JSONArray> rolesArray = Option.none();
    if (StringUtils.isNotBlank(roles)) {
      rolesArray = Option.option((JSONArray) JSONValue.parse(roles));
    }

    Set<JpaRole> rolesSet = new HashSet<>();

    // Add the roles given
    if (rolesArray.isSome()) {
      // Add the roles given
      for (Object role : rolesArray.get()) {
        JSONObject roleAsJson = (JSONObject) role;
        Role.Type roletype = Role.Type.valueOf((String) roleAsJson.get("type"));
        rolesSet.add(new JpaRole(roleAsJson.get("id").toString(), organization, null, roletype));
      }
    } else {
      rolesSet.add(new JpaRole(organization.getAnonymousRole(), organization));
    }

    JpaUser user = new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider.getName(), true,
            rolesSet);
    try {
      jpaUserAndRoleProvider.addUser(user);
      return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build();
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
  }

  @GET
  @Path("{username}.json")
  @RestQuery(name = "getUser", description = "Get an user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been found."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response getUser(@PathParam("username") String username) throws NotFoundException {

    User user = userDirectoryService.loadUser(username);
    if (user == null) {
      throw new NotFoundException("User " + username + " does not exist.");
    }

    return RestUtils.okJson(generateJsonUser(user));
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(name = "updateUser", description = "Update an user", returnDescription = "Status ok", restParameters = {
          @RestParameter(description = "The password.", isRequired = false, name = "password", type = STRING),
          @RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING),
          @RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array") }, pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been updated."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update a user with admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.")})
  public Response updateUser(@PathParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles)
          throws NotFoundException {

    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null) {
      throw new NotFoundException("User " + username + " does not exist.");
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    Set<JpaRole> rolesSet = new HashSet<>();

    Option<JSONArray> rolesArray = Option.none();
    if (StringUtils.isNotBlank(roles)) {
      rolesArray = Option.some((JSONArray) JSONValue.parse(roles));
    }
    if (rolesArray.isSome()) {
      // Add the roles given
      for (Object roleObj : rolesArray.get()) {
        JSONObject role = (JSONObject) roleObj;
        String rolename = (String) role.get("id");
        Role.Type roletype = Role.Type.valueOf((String) role.get("type"));
        rolesSet.add(new JpaRole(rolename, organization, null, roletype));
      }
    } else {
      // Or the use the one from the user if no one is given
      for (Role role : user.getRoles()) {
        rolesSet.add(new JpaRole(role.getName(), organization, role.getDescription(), role.getType()));
      }
    }

    try {
      jpaUserAndRoleProvider.updateUser(new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider
              .getName(), true, rolesSet));
      userDirectoryService.invalidate(username);
      return Response.status(SC_OK).build();
    } catch (UnauthorizedException ex) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
  }

  @DELETE
  @Path("{username}.json")
  @RestQuery(name = "deleteUser", description = "Deleter a new  user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been deleted."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to delete a user with admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response deleteUser(@PathParam("username") String username) throws NotFoundException {
    Organization organization = securityService.getOrganization();

    try {
      jpaUserAndRoleProvider.deleteUser(username, organization.getId());
      userDirectoryService.invalidate(username);
    } catch (NotFoundException e) {
      logger.error("User {} not found.", username);
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

  private JValue generateJsonUser(User user) {
    // Prepare the roles
    List<JValue> rolesJSON = Stream.$(user.getRoles()).sort(sortRolesByName).map(serializeRole).toList();

    return obj(f("username", v(user.getUsername(), Jsons.BLANK)), f("manageable", v(user.isManageable())),
            f("name", v(user.getName(), Jsons.BLANK)), f("email", v(user.getEmail(), Jsons.BLANK)),
            f("roles", arr(rolesJSON)), f("provider", v(user.getProvider(), Jsons.BLANK)));
  }

  private static final Fn<Role, JValue> serializeRole = new Fn<Role, JValue>() {
    @Override
    public JValue apply(Role role) {
      return roleToJson(role);
    }
  };

  private static JObject roleToJson(Role role) {
    List<Field> fields = new ArrayList<>();
    fields.add(f(JsonConv.KEY_NAME, v(role.getName())));
    fields.add(f("type", v(role.getType().toString())));
    return obj(fields);
  }

  private static final Fn<Role, String> getRoleName = new Fn<Role, String>() {
    @Override
    public String apply(Role role) {
      return role.getName();
    }
  };

  private static final Comparator<String> sortByName = new Comparator<String>() {
    @Override
    public int compare(String name1, String name2) {
      return name1.compareTo(name2);
    }
  };

  private static final Comparator<Role> sortRolesByName = new Comparator<Role>() {
    @Override
    public int compare(Role name1, Role name2) {
      return name1.getName().compareTo(name2.getName());
    }
  };

}
