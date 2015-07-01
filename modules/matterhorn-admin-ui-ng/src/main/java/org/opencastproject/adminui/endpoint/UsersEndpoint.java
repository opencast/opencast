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

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.index.service.util.JSONUtils.blacklistToJSON;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.index.service.resources.list.query.UsersListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.pm.api.Blacklist;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabaseException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.userdirectory.JpaRole;
import org.opencastproject.userdirectory.JpaUser;
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
import com.entwinemedia.fn.data.json.JString;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
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
@RestService(name = "users", title = "User service", notes = "This service offers the default users CRUD Operations for the admin UI.", abstractText = "Provides operations for users")
public class UsersEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(UsersEndpoint.class);

  /** The global user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The internal role and user provider */
  private JpaUserAndRoleProvider jpaUserAndRoleProvider;

  /** The participation persistence */
  private ParticipationManagementDatabase participationPersistence;

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
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setJpaUserAndRoleProvider(JpaUserAndRoleProvider jpaUserAndRoleProvider) {
    this.jpaUserAndRoleProvider = jpaUserAndRoleProvider;
  }

  /** OSGi callback for participation persistence. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationPersistence) {
    this.participationPersistence = participationPersistence;
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

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (UsersListQuery.FILTER_NAME_NAME.equals(name))
        filterName = Option.some(filters.get(name));
      if (UsersListQuery.FILTER_ROLE_NAME.equals(name))
        filterRole = Option.some(filters.get(name));
      if (UsersListQuery.FILTER_PROVIDER_NAME.equals(name)) {
        filterProvider = Option.some(filters.get(name));
      }
    }

    // Filter agents by filter criteria
    List<User> filteredUsers = new ArrayList<User>();
    for (Iterator<User> i = userDirectoryService.getUsers(); i.hasNext();) {
      User user = i.next();

      // Filter list
      boolean mismatchName = filterName.isSome() && !filterName.get().equals(user.getName());
      boolean mismatchRole = filterRole.isSome()
              && !Stream.$(user.getRoles()).map(getRoleName).toSet().contains(filterRole.get());
      boolean mismatchProvider = filterProvider.isSome() && !filterProvider.get().equals(user.getProvider());
      if (mismatchName || mismatchRole || mismatchProvider)
        continue;

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
                  return ObjectUtils.compare(user2.getName(), user1.getName());
                return ObjectUtils.compare(user1.getName(), user2.getName());
              case "username":
                if (order.equals(Order.Descending))
                  return ObjectUtils.compare(user2.getUsername(), user1.getUsername());
                return ObjectUtils.compare(user1.getUsername(), user2.getUsername());
              case "email":
                if (order.equals(Order.Descending))
                  return ObjectUtils.compare(user2.getEmail(), user1.getEmail());
                return ObjectUtils.compare(user1.getEmail(), user2.getEmail());
              case "roles":
                String roles1 = Stream.$(user1.getRoles()).map(getRoleName).sort(sortByName).mkString(",");
                String roles2 = Stream.$(user2.getRoles()).map(getRoleName).sort(sortByName).mkString(",");
                if (order.equals(Order.Descending))
                  return ObjectUtils.compare(roles2, roles1);
                return ObjectUtils.compare(roles1, roles2);
              case "provider":
                if (order.equals(Order.Descending))
                  return ObjectUtils.compare(user2.getProvider(), user1.getProvider());
                return ObjectUtils.compare(user1.getProvider(), user2.getProvider());
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

    List<JValue> usersJSON = new ArrayList<JValue>();
    for (User user : filteredUsers) {
      List<Blacklist> blacklist = new ArrayList<Blacklist>();
      Person person = null;
      if (participationPersistence != null) {
        try {
          person = participationPersistence.getPerson(user.getEmail());
          blacklist.addAll(participationPersistence.findBlacklists(person));
        } catch (ParticipationManagementDatabaseException e) {
          logger.warn("Not able to find the blacklist for the user {}: {}", user.getEmail(), e);
          return Response.status(SC_INTERNAL_SERVER_ERROR).build();
        } catch (NotFoundException e) {
          logger.debug("Not able to find the person with the email address {}.", user.getEmail());
        }
      }
      usersJSON.add(generateJsonUser(user, Option.option(person), blacklist));
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
          @RestResponse(responseCode = SC_CONFLICT, description = "An user with this username already exist.") })
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

    Set<JpaRole> rolesSet = new HashSet<JpaRole>();

    // Add the roles given
    if (rolesArray.isSome()) {
      // Add the roles given
      for (Object role : rolesArray.get()) {
        rolesSet.add(new JpaRole((String) role, organization));
      }
    } else {
      rolesSet.add(new JpaRole(organization.getAnonymousRole(), organization));
    }

    JpaUser user = new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider.getName(), true,
            rolesSet);
    jpaUserAndRoleProvider.addUser(user);

    return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build();
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

    // Reload user from internal user provider
    if (JpaUserAndRoleProvider.PROVIDER_NAME.equals(user.getProvider()))
      user = jpaUserAndRoleProvider.loadUser(username);

    List<Blacklist> blacklist = new ArrayList<Blacklist>();
    Person person = null;
    if (participationPersistence != null) {
      try {
        person = participationPersistence.getPerson(user.getEmail());
        blacklist.addAll(participationPersistence.findBlacklists(person));
      } catch (ParticipationManagementDatabaseException e) {
        logger.warn("Not able to find the blacklist for the user {}: {}", user.getEmail(), e);
        return Response.status(SC_INTERNAL_SERVER_ERROR).build();
      } catch (NotFoundException e) {
        logger.debug("Not able to find the person with the email address {}.", user.getEmail());
      }
    }

    return RestUtils.okJson(generateJsonUser(user, Option.option(person), blacklist));
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(name = "updateUser", description = "Update an user", returnDescription = "Status ok", restParameters = {
          @RestParameter(description = "The password.", isRequired = false, name = "password", type = STRING),
          @RestParameter(description = "The name.", isRequired = false, name = "name", type = STRING),
          @RestParameter(description = "The email.", isRequired = false, name = "email", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array") }, pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response updateUser(@PathParam("username") String username, @FormParam("password") String password,
          @FormParam("name") String name, @FormParam("email") String email, @FormParam("roles") String roles)
          throws NotFoundException {

    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null) {
      throw new NotFoundException("User " + username + " does not exist.");
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    Set<JpaRole> rolesSet = new HashSet<JpaRole>();

    Option<JSONArray> rolesArray = Option.none();
    if (StringUtils.isNotBlank(roles)) {
      rolesArray = Option.some((JSONArray) JSONValue.parse(roles));
    }
    if (rolesArray.isSome()) {
      // Add the roles given
      for (Object role : rolesArray.get()) {
        rolesSet.add(new JpaRole((String) role, organization));
      }
    } else {
      // Or the use the one from the user if no one is given
      for (Role role : user.getRoles()) {
        rolesSet.add(new JpaRole(role.getName(), organization));
      }
    }

    jpaUserAndRoleProvider.updateUser(new JpaUser(username, password, organization, name, email, jpaUserAndRoleProvider
            .getName(), true, rolesSet));
    return Response.status(SC_OK).build();
  }

  @DELETE
  @Path("{username}.json")
  @RestQuery(name = "deleteUser", description = "Deleter a new  user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been deleted."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response deleteUser(@PathParam("username") String username) throws NotFoundException {
    Organization organization = securityService.getOrganization();

    try {
      jpaUserAndRoleProvider.deleteUser(username, organization.getId());
    } catch (NotFoundException e) {
      logger.error("User {} not found.", username);
      return Response.status(SC_NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Error during deletion of user {}: {}", username, e);
      return Response.status(SC_INTERNAL_SERVER_ERROR).build();
    }

    logger.debug("User {} removed.", username);
    return Response.status(SC_OK).build();
  }

  /**
   * Generate a JSON Object for the given user with its related blacklist periods
   *
   * @param user
   *          The target user
   * @param person
   *          the participation person
   * @param blacklist
   *          The blacklist periods related to the user
   * @return A {@link JValue} representing the user
   */
  private JValue generateJsonUser(User user, Option<Person> person, List<Blacklist> blacklist) {
    JValue blacklistJSON = blacklistToJSON(blacklist);

    // Prepare the roles
    List<JString> rolesJSON = Stream.$(user.getRoles()).map(getRoleName).sort(sortByName).map(toJString).toList();

    JValue personValue = v(-1);
    if (person.isSome())
      personValue = v(person.get().getId());

    return j(f("username", vN(user.getUsername())), f("manageable", v(user.isManageable())),
            f("name", vN(user.getName())), f("email", vN(user.getEmail())), f("roles", a(rolesJSON)),
            f("provider", vN(user.getProvider())), f("personId", personValue), f("blacklist", blacklistJSON));
  }

  private static final Fn<Role, String> getRoleName = new Fn<Role, String>() {
    @Override
    public String ap(Role role) {
      return role.getName();
    }
  };

  private static final Fn<String, JString> toJString = new Fn<String, JString>() {
    @Override
    public JString ap(String string) {
      return v(string);
    }
  };

  private static final Comparator<String> sortByName = new Comparator<String>() {
    @Override
    public int compare(String name1, String name2) {
      return name1.compareTo(name2);
    }
  };

}
