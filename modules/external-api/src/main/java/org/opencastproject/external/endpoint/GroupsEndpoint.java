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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.opencastproject.external.common.ApiVersion.VERSION_1_6_0;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponseBuilder;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.index.service.resources.list.query.GroupsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
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

import org.apache.commons.collections4.ComparatorUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/api/groups")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0,
            ApiMediaType.VERSION_1_3_0, ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0,
            ApiMediaType.VERSION_1_6_0, ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0,
            ApiMediaType.VERSION_1_9_0, ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0 })
@RestService(name = "externalapigroups", title = "External API Groups Service", notes = {}, abstractText = "Provides resources and operations related to the groups")
@Component(
    immediate = true,
    service = GroupsEndpoint.class,
    property = {
        "service.description=External API - Groups Endpoint",
        "opencast.service.type=org.opencastproject.external.groups",
        "opencast.service.path=/api/groups"
    }
)
@JaxrsResource
public class GroupsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(GroupsEndpoint.class);

  /* OSGi service references */
  private ElasticsearchIndex elasticsearchIndex;
  private JpaGroupRoleProvider jpaGroupRoleProvider;
  private SecurityService securityService;

  /** OSGi DI */
  @Reference
  void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  /** OSGi DI. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI */
  @Reference
  public void setGroupRoleProvider(JpaGroupRoleProvider jpaGroupRoleProvider) {
    this.jpaGroupRoleProvider = jpaGroupRoleProvider;
  }

  /** OSGi activation method */
  @Activate
  void activate() {
    logger.info("Activating External API - Groups Endpoint");
  }

  @GET
  @Path("")
  @RestQuery(name = "getgroups", description = "Returns a list of groups.", returnDescription = "", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form [Filter Name]:[Value to Filter With].", type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = RestParameter.Type.INTEGER) }, responses = {
                  @RestResponse(description = "A (potentially empty) list of groups.", responseCode = HttpServletResponse.SC_OK) })
  public Response getGroups(@HeaderParam("Accept") String acceptHeader, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
    Optional<Integer> optLimit = Optional.ofNullable(limit);
    Optional<Integer> optOffset = Optional.ofNullable(offset);

    if (optLimit.isPresent() && limit <= 0) {
      optLimit = Optional.empty();
    }
    if (optOffset.isPresent() && offset < 0) {
      optOffset = Optional.empty();
    }

    // The API currently does not offer full text search for groups
    Map<String, String> filters = RestUtils.parseFilter(filter);
    Optional<String> optNameFilter = Optional.ofNullable(filters.get(GroupsListQuery.FILTER_NAME_NAME));

    Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);

    // sorting by members & roles is not supported by the database, so we do this afterwards
    Set<SortCriterion> deprecatedSortCriteria = new HashSet<>();
    if (requestedVersion.isSmallerThan(VERSION_1_6_0)) {
      deprecatedSortCriteria = sortCriteria.stream().filter(
              sortCriterion -> sortCriterion.getFieldName().equals("members")
                      || sortCriterion.getFieldName().equals("roles")).collect(Collectors.toSet());
      sortCriteria.removeAll(deprecatedSortCriteria);
    }

    List<JpaGroup> results = jpaGroupRoleProvider.getGroups(optLimit, optOffset, optNameFilter, Optional.empty(),
            sortCriteria);

    // sorting by members & roles is only available for api versions < 1.6.0
    if (requestedVersion.isSmallerThan(VERSION_1_6_0)) {
      List<Comparator<JpaGroup>> comparators = new ArrayList<>();
      for (SortCriterion sortCriterion : deprecatedSortCriteria) {
        Comparator<JpaGroup> comparator;
        switch (sortCriterion.getFieldName()) {
          case "members":
            comparator = new GroupComparator() {
              @Override
              public Set<String> getGroupAttribute(JpaGroup jpaGroup) {
                return jpaGroup.getMembers();
              }
            };
            break;
          case "roles":
            comparator = new GroupComparator() {
              @Override
              public Set<String> getGroupAttribute(JpaGroup jpaGroup) {
                return jpaGroup.getRoleNames();
              }
            };
            break;
          default:
            continue;
        }

        if (sortCriterion.getOrder() == SortCriterion.Order.Descending) {
          comparator = comparator.reversed();
        }
        comparators.add(comparator);
      }
      Collections.sort(results, ComparatorUtils.chainedComparator(comparators));
    }

    List<JValue> groupsJSON = new ArrayList<>();
    for (JpaGroup group : results) {
      List<Field> fields = new ArrayList<>();
      fields.add(f("identifier", v(group.getGroupId())));
      fields.add(f("organization", v(group.getOrganization().getId())));
      fields.add(f("role", v(group.getRole())));
      fields.add(f("name", v(group.getName(), Jsons.BLANK)));
      fields.add(f("description", v(group.getDescription(), Jsons.BLANK)));
      fields.add(f("roles", v(join(group.getRoleNames(), ","), Jsons.BLANK)));
      fields.add(f("members", v(join(group.getMembers(), ","), Jsons.BLANK)));
      groupsJSON.add(obj(fields));
    }
    return ApiResponseBuilder.Json.ok(acceptHeader, arr(groupsJSON));
  }

  /**
   * Compare groups by set attributes like members or roles.
   */
  private abstract class GroupComparator implements Comparator<JpaGroup> {

    public abstract Set<String> getGroupAttribute(JpaGroup jpaGroup);

    @Override
    public int compare(JpaGroup group1, JpaGroup group2) {
      List<String> members1 = new ArrayList<>(getGroupAttribute(group1));
      List<String> members2 = new ArrayList<>(getGroupAttribute(group2));

      for (int i = 0; i < members1.size() && i < members2.size(); i++) {
        String member1 = members1.get(i);
        String member2 = members2.get(i);
        int result = member1.compareTo(member2);
        if (result != 0) {
          return result;
        }
      }
      return (members1.size() - members2.size());
    }
  }

  @GET
  @Path("{groupId}")
  @RestQuery(name = "getgroup", description = "Returns a single group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, responses = {
                  @RestResponse(description = "The group is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id) {
    JpaGroup group = jpaGroupRoleProvider.getGroup(id);

    if (group == null) {
      return ApiResponseBuilder.notFound("Cannot find a group with id '%s'.", id);
    }

    return ApiResponseBuilder.Json.ok(acceptHeader,
            obj(
                    f("identifier", v(group.getGroupId())),
                    f("organization", v(group.getOrganization().getId())),  f("role", v(group.getRole())),
                    f("name", v(group.getName(), Jsons.BLANK)),
                    f("description", v(group.getDescription(), Jsons.BLANK)),
                    f("roles", v(join(group.getRoleNames(), ","), Jsons.BLANK)),
                    f("members", v(join(group.getMembers(), ","), Jsons.BLANK))
            )
    );
  }

  @DELETE
  @Path("{groupId}")
  @RestQuery(name = "deletegroup", description = "Deletes a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, responses = {
                  @RestResponse(description = "The group has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id)
          throws NotFoundException {
    try {
      jpaGroupRoleProvider.removeGroup(id);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("Unable to delete group {}", id, e);
      throw new WebApplicationException(e, SC_INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("{groupId}")
  @RestQuery(name = "updategroup", description = "Updates a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "name", isRequired = false, description = "Group Name", type = STRING),
                  @RestParameter(name = "description", description = "Group Description", isRequired = false, type = STRING),
                  @RestParameter(name = "roles", description = "Comma-separated list of roles", isRequired = false, type = STRING),
                  @RestParameter(name = "members", description = "Comma-separated list of members", isRequired = false, type = STRING) }, responses = {
                          @RestResponse(description = "The group has been updated.", responseCode = HttpServletResponse.SC_CREATED),
                          @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response updateGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("members") String members) throws Exception {
    try {
      jpaGroupRoleProvider.updateGroup(id, name, description, roles, members);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to update group id {}: {}", id, e.getMessage());
      return Response.status(SC_BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    }
    return Response.ok().build();
  }

  @POST
  @Path("")
  @RestQuery(name = "creategroup", description = "Creates a group.", returnDescription = "", restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "Group Name", type = STRING),
          @RestParameter(name = "description", description = "Group Description", isRequired = false, type = STRING),
          @RestParameter(name = "roles", description = "Comma-separated list of roles", isRequired = false, type = STRING),
          @RestParameter(name = "members", description = "Comma-separated list of members", isRequired = false, type = STRING) }, responses = {
                  @RestResponse(description = "A new group is created.", responseCode = SC_CREATED),
                  @RestResponse(description = "The request is invalid or inconsistent.", responseCode = SC_BAD_REQUEST) })
  public Response createGroup(@HeaderParam("Accept") String acceptHeader, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("members") String members) {
    try {
      jpaGroupRoleProvider.createGroup(name, description, roles, members);
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

  @POST
  @Path("{groupId}/members")
  @RestQuery(name = "addgroupmember", description = "Adds a member to a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "member", description = "Member Name", isRequired = true, type = STRING) }, responses = {
                          @RestResponse(description = "The member was already member of the group.", responseCode = SC_OK),
                          @RestResponse(description = "The member has been added.", responseCode = SC_NO_CONTENT),
                          @RestResponse(description = "The specified group does not exist.", responseCode = SC_NOT_FOUND) })
  public Response addGroupMember(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @FormParam("member") String member) {
    try {
      if (jpaGroupRoleProvider.addMemberToGroup(id, member)) {
        return Response.ok().build();
      } else {
        return ApiResponseBuilder.Json.ok(acceptHeader, "Member is already member of group.");
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to add member to group id {}.", id, e);
      return Response.status(SC_BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find group with id '%s'.", id);
    } catch (Exception e) {
      logger.warn("Could not update the group with id {}.",id, e);
      return ApiResponseBuilder.serverError("Could not update group with id '%s', reason: '%s'",id,getMessage(e));
    }
  }

  @DELETE
  @Path("{groupId}/members/{memberId}")
  @RestQuery(name = "removegroupmember", description = "Removes a member from a group", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING),
          @RestParameter(name = "memberId", description = "The member id", isRequired = true, type = STRING) }, responses = {
                  @RestResponse(description = "The member has been removed.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified group or member does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response removeGroupMember(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @PathParam("memberId") String memberId) {
    try {
      if (jpaGroupRoleProvider.removeMemberFromGroup(id, memberId)) {
        return Response.ok().build();
      } else {
        return ApiResponseBuilder.Json.ok(acceptHeader, "Member is already not member of group.");
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to remove member from group id {}.", id, e);
      return Response.status(SC_BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find group with id '%s'.", id);
    } catch (Exception e) {
      logger.warn("Could not update the group with id {}.", id, e);
      return ApiResponseBuilder.serverError("Could not update group with id '%s', reason: '%s'", id, getMessage(e));
    }
  }
}
