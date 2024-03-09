/*
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
import static java.lang.Math.max;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.resources.list.query.GroupsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.userdirectory.ConflictException;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/admin-ng/groups")
@RestService(name = "groups", title = "Group service",
  abstractText = "Provides operations for groups",
  notes = { "This service offers the default groups CRUD operations for the admin interface.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
        immediate = true,
        service = GroupsEndpoint.class,
        property = {
                "service.description=Admin UI - Groups Endpoint",
                "opencast.service.type=org.opencastproject.adminui.GroupsEndpoint",
                "opencast.service.path=/admin-ng/groups",
        }
)
public class GroupsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(GroupsEndpoint.class);

  /** The admin UI search index */
  private ElasticsearchIndex searchIndex;

  /** The security service */
  private SecurityService securityService;

  /** The user directory service */
  private UserDirectoryService userDirectoryService;

  /** The index service */
  private IndexService indexService;

  /** The group provider */
  private JpaGroupRoleProvider jpaGroupRoleProvider;

  /** OSGi callback for the security service. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for the index service. */
  @Reference
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi callback for users services. */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /** OSGi callback for the search index. */
  @Reference
  public void setSearchIndex(ElasticsearchIndex searchIndex) {
    this.searchIndex = searchIndex;
  }

  /** OSGi callback for the group provider. */
  @Reference
  public void setGroupRoleProvider(JpaGroupRoleProvider jpaGroupRoleProvider) {
    this.jpaGroupRoleProvider = jpaGroupRoleProvider;
  }

  /** OSGi callback. */
  @Activate
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Groups facade endpoint");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("groups.json")
  @RestQuery(
    name = "allgroupsasjson",
    description = "Returns a list of groups",
    returnDescription = "List of groups for the current user's organization as JSON.",
    restParameters = {
      @RestParameter(name = "filter", isRequired = false, type = STRING,
        description = "Filter used for the query, formatted like: 'filter1:value1,filter2:value2'"),
      @RestParameter(name = "sort", isRequired = false, type = STRING,
        description = "The sort order. May include any of the following: NAME, DESCRIPTION, ROLE. "
        + "Add '_DESC' to reverse the sort order (e.g. NAME_DESC)."),
      @RestParameter(name = "limit", isRequired = false, type = INTEGER, defaultValue = "100",
        description = "The maximum number of items to return per page."),
      @RestParameter(name = "offset", isRequired = false, type = INTEGER, defaultValue = "0",
        description = "The page number.")},
    responses = {
      @RestResponse(responseCode = SC_OK, description = "The groups.")})
  public Response getGroups(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) throws IOException {
    Optional<Integer> optLimit = Optional.ofNullable(limit);
    Optional<Integer> optOffset = Optional.ofNullable(offset);

    Map<String, String> filters = RestUtils.parseFilter(filter);
    Optional<String> optNameFilter = Optional.ofNullable(filters.get(GroupsListQuery.FILTER_NAME_NAME));
    Optional<String> optTextFilter = Optional.ofNullable(filters.get(GroupsListQuery.FILTER_TEXT_NAME));

    Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);

    List<JpaGroup> results = jpaGroupRoleProvider.getGroups(optLimit, optOffset, optNameFilter, optTextFilter,
            sortCriteria);

    // load users
    List<String> userNames = results.stream().flatMap(item -> item.getMembers().stream())
            .collect(Collectors.toList());
    final Map<String, User> users = new HashMap<>(userNames.size());
    userDirectoryService.loadUsers(userNames).forEachRemaining(user -> users.put(user.getUsername(), user));

    List<JValue> groupsJSON = new ArrayList<>();
    for (JpaGroup group : results) {
      List<Field> fields = new ArrayList<>();
      fields.add(f("id", v(group.getGroupId())));
      fields.add(f("name", v(group.getName(), Jsons.BLANK)));
      fields.add(f("description", v(group.getDescription(), Jsons.BLANK)));
      fields.add(f("role", v(group.getRole())));
      fields.add(
        f("users", membersToJSON(group.getMembers().stream().map(users::get).filter(Objects::nonNull).iterator())));
      groupsJSON.add(obj(fields));
    }

    long dbTotal = jpaGroupRoleProvider.countTotalGroups(optNameFilter, optTextFilter);
    long resultsTotal = optOffset.orElse(0) + results.size();

    // groups could've been added or deleted in the meantime, so...
    long total;
    // don't show next page if current page isn't full
    if (!optLimit.isPresent() || results.size() < optLimit.get()) {
      total = resultsTotal;
    // don't show less than the current results
    } else {
      total = max(dbTotal, resultsTotal);
    }

    return okJsonList(groupsJSON, optOffset, optLimit, total);
  }

  @DELETE
  @Path("{id}")
  @RestQuery(
    name = "removegrouop",
    description = "Remove a group",
    returnDescription = "Returns no content",
    pathParameters = {
      @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING)},
    responses = {
      @RestResponse(responseCode = SC_OK, description = "Group deleted"),
      @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to delete the group with admin role."),
      @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found."),
      @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An internal server error occured.")})
  public Response removeGroup(@PathParam("id") String groupId) throws NotFoundException {
    try {
      jpaGroupRoleProvider.removeGroup(groupId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("Unable to delete group {}", groupId, e);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("")
  @RestQuery(
    name = "createGroup",
    description = "Add a group",
    returnDescription = "Returns Created (201) if the group has been created",
    restParameters = {
      @RestParameter(name = "name", description = "The group name", isRequired = true, type = STRING),
      @RestParameter(name = "description", description = "The group description", isRequired = false, type = STRING),
      @RestParameter(name = "roles", description = "Comma seperated list of roles", isRequired = false, type = TEXT),
      @RestParameter(name = "users", description = "Comma seperated list of members", isRequired = false, type = TEXT)},
    responses = {
      @RestResponse(responseCode = SC_CREATED, description = "Group created"),
      @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long"),
      @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a group with admin role."),
      @RestResponse(responseCode = SC_CONFLICT, description = "An group with this name already exists.") })
  public Response createGroup(@FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("users") String users) {
    try {
      jpaGroupRoleProvider.createGroup(name, description, roles, users);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to create group with name {}: {}", name, e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (ConflictException e) {
      return Response.status(SC_CONFLICT).build();
    }
    return Response.status(Status.CREATED).build();
  }

  @PUT
  @Path("{id}")
  @RestQuery(
    name = "updateGroup",
    description = "Update a group",
    returnDescription = "Return the status codes",
    pathParameters = {
      @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING) },
    restParameters = {
      @RestParameter(name = "name", description = "The group name", isRequired = true, type = STRING),
      @RestParameter(name = "description", description = "The group description", isRequired = false, type = STRING),
      @RestParameter(name = "roles", description = "Comma seperated list of roles", isRequired = false, type = TEXT),
      @RestParameter(name = "users", description = "Comma seperated list of members", isRequired = false, type = TEXT)},
    responses = {
      @RestResponse(responseCode = SC_OK, description = "Group updated"),
      @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update the group with admin role."),
      @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found"),
      @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long")})
  public Response updateGroup(@PathParam("id") String groupId, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("users") String users) throws NotFoundException {
    try {
      jpaGroupRoleProvider.updateGroup(groupId, name, description, roles, users);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to update group with id {}: {}", groupId, e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    }
    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  @RestQuery(
    name = "getGroup",
    description = "Get a single group",
    returnDescription = "Return the status codes",
    pathParameters = {
      @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = STRING)},
    responses = {
      @RestResponse(responseCode = SC_OK, description = "Group found and returned as JSON"),
      @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found")})
  public Response getGroup(@PathParam("id") String groupId) throws NotFoundException {
    JpaGroup group = jpaGroupRoleProvider.getGroup(groupId);

    if (group == null) {
      throw new NotFoundException("Group " + groupId + " does not exist.");
    }

    // convert roles
    List<JValue> rolesJSON = new ArrayList<>();
    for (String role : group.getRoleNames()) {
      rolesJSON.add(v(role));
    }

    Iterator<User> users = userDirectoryService.loadUsers(group.getMembers());
    return RestUtils.okJson(obj(f("id", v(group.getGroupId())), f("name", v(group.getName(), Jsons.BLANK)),
      f("description", v(group.getDescription(), Jsons.BLANK)), f("role", v(group.getRole(), Jsons.BLANK)),
      f("roles", arr(rolesJSON)), f("users", membersToJSON(users))));
  }

  /**
   * Generate a JSON array based on the given set of members
   *
   * @param members
   *          the members source
   * @return a JSON array ({@link JValue}) with the given members
   */
  private JValue membersToJSON(Iterator<User> members) {
    List<JValue> membersJSON = new ArrayList<>();

    while (members.hasNext()) {
      User user = members.next();
      String name = user.getUsername();

      if (StringUtils.isNotBlank(user.getName())) {
        name = user.getName();
      }

      membersJSON.add(obj(f("username", v(user.getUsername())), f("name", v(name))));
    }

    return arr(membersJSON);
  }
}
