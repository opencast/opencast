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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ "application/json", "application/v1.0.0+json", "application/v1.1.0+json" })
@RestService(name = "externalapigroups", title = "External API Groups Service", notes = "", abstractText = "Provides resources and operations related to the groups")
public class GroupsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(GroupsEndpoint.class);

  /* OSGi service references */
  private ExternalIndex externalIndex;
  private IndexService indexService;

  /** OSGi DI */
  void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  /** OSGi DI */
  void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi activation method */
  void activate() {
    logger.info("Activating External API - Groups Endpoint");
  }

  @GET
  @Path("")
  @RestQuery(name = "getgroups", description = "Returns a list of groups.", returnDescription = "", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.", type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = RestParameter.Type.INTEGER) }, reponses = {
                  @RestResponse(description = "A (potentially empty) list of groups.", responseCode = HttpServletResponse.SC_OK) })
  public Response getGroups(@HeaderParam("Accept") String acceptHeader, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    Opt<Integer> optLimit = Opt.nul(limit);
    if (optLimit.isSome() && limit <= 0)
      optLimit = Opt.none();
    Opt<Integer> optOffset = Opt.nul(offset);
    if (optOffset.isSome() && offset < 0)
      optOffset = Opt.none();

    SearchResult<Group> results;
    try {
      results = indexService.getGroups(filter, optLimit, optOffset, Opt.nul(StringUtils.trimToNull(sort)),
              externalIndex);
    } catch (SearchIndexException e) {
      logger.error("The External Search Index was not able to get the groups list: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve groups, reason: '%s'", getMessage(e));
    }

    // If the results list if empty, we return already a response.
    List<JValue> groupsList = new ArrayList<>();
    for (SearchResultItem<Group> item : results.getItems()) {
      groupsList.add(groupToJSON(item.getSource()));
    }
    return ApiResponses.Json.ok(acceptHeader, arr(groupsList));
  }

  @GET
  @Path("{groupId}")
  @RestQuery(name = "getgroup", description = "Returns a single group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The group is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id)
          throws Exception {
    for (final Group group : indexService.getGroup(id, externalIndex)) {
      return ApiResponses.Json.ok(acceptHeader, groupToJSON(group));
    }
    return ApiResponses.notFound("Cannot find a group with id '%s'.", id);
  }

  @DELETE
  @Path("{groupId}")
  @RestQuery(name = "deletegroup", description = "Deletes a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The group has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response deleteGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id)
          throws NotFoundException {
    return indexService.removeGroup(id);
  }

  @PUT
  @Path("{groupId}")
  @RestQuery(name = "updategroup", description = "Updates a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "name", isRequired = false, description = "Group Name", type = STRING),
                  @RestParameter(name = "description", description = "Group Description", isRequired = false, type = STRING),
                  @RestParameter(name = "roles", description = "Comma-separated list of roles", isRequired = false, type = STRING),
                  @RestParameter(name = "members", description = "Comma-separated list of members", isRequired = false, type = STRING) }, reponses = {
                          @RestResponse(description = "The group has been updated.", responseCode = HttpServletResponse.SC_CREATED),
                          @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response updateGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("members") String members) throws Exception {
    return indexService.updateGroup(id, name, description, roles, members);
  }

  @POST
  @Path("")
  @RestQuery(name = "creategroup", description = "Creates a group.", returnDescription = "", restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "Group Name", type = STRING),
          @RestParameter(name = "description", description = "Group Description", isRequired = false, type = STRING),
          @RestParameter(name = "roles", description = "Comma-separated list of roles", isRequired = false, type = STRING),
          @RestParameter(name = "members", description = "Comma-separated list of members", isRequired = false, type = STRING) }, reponses = {
                  @RestResponse(description = "A new group is created.", responseCode = HttpServletResponse.SC_CREATED),
                  @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response createGroup(@HeaderParam("Accept") String acceptHeader, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("members") String members) {
    return indexService.createGroup(name, description, roles, members);
  }

  @POST
  @Path("{groupId}/members")
  @RestQuery(name = "addgroupmember", description = "Adds a member to a group.", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING) }, restParameters = {
                  @RestParameter(name = "member", description = "Member Name", isRequired = true, type = STRING) }, reponses = {
                          @RestResponse(description = "The member was already member of the group.", responseCode = HttpServletResponse.SC_OK),
                          @RestResponse(description = "The member has been added.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                          @RestResponse(description = "The specified group does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response addGroupMember(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @FormParam("member") String member) {
    try {
      Opt<Group> groupOpt = indexService.getGroup(id, externalIndex);
      if (groupOpt.isSome()) {
        Group group = groupOpt.get();
        Set<String> members = group.getMembers();
        if (!members.contains(member)) {
          group.addMember(member);
          return indexService.updateGroup(group.getIdentifier(), group.getName(), group.getDescription(),
                  StringUtils.join(group.getRoles(), ","), StringUtils.join(group.getMembers(), ","));
        } else {
          return ApiResponses.Json.ok(acceptHeader, "Member is already member of group");
        }
      } else {
        return ApiResponses.notFound("Cannot find group with id '%s'.", id);
      }
    } catch (SearchIndexException e) {
      logger.warn("The external search index was not able to retrieve the group with id '%s', reason: ",
              getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve group with id '%s', reason: '%s'", id, getMessage(e));
    } catch (NotFoundException e) {
      logger.warn("The external search index was not able to update the group with id {}, ", getStackTrace(e));
      return ApiResponses.serverError("Could not update group with id '%s', reason: '%s'", id, getMessage(e));
    }
  }

  @DELETE
  @Path("{groupId}/members/{memberId}")
  @RestQuery(name = "removegroupmember", description = "Removes a member from a group", returnDescription = "", pathParameters = {
          @RestParameter(name = "groupId", description = "The group id", isRequired = true, type = STRING),
          @RestParameter(name = "memberId", description = "The member id", isRequired = true, type = STRING) }, reponses = {
                  @RestResponse(description = "The member has been removed.", responseCode = HttpServletResponse.SC_NO_CONTENT),
                  @RestResponse(description = "The specified group or member does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response removeGroupMember(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @PathParam("memberId") String memberId) {
    try {
      Opt<Group> groupOpt = indexService.getGroup(id, externalIndex);
      if (groupOpt.isSome()) {
        Group group = groupOpt.get();
        Set<String> members = group.getMembers();
        if (members.contains(memberId)) {
          members.remove(memberId);
          group.setMembers(members);
          return indexService.updateGroup(group.getIdentifier(), group.getName(), group.getDescription(),
                  StringUtils.join(group.getRoles(), ","), StringUtils.join(group.getMembers(), ","));
        } else {
          return ApiResponses.notFound("Cannot find member '%s' in group '%s'.", memberId, id);
        }
      } else {
        return ApiResponses.notFound("Cannot find group with id '%s'.", id);
      }
    } catch (SearchIndexException e) {
      logger.warn("The external search index was not able to retrieve the group with id {}, ", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve groups, reason: '%s'", getMessage(e));
    } catch (NotFoundException e) {
      logger.warn("The external search index was not able to update the group with id {}, ", getStackTrace(e));
      return ApiResponses.serverError("Could not update group with id '%s', reason: '%s'", id, getMessage(e));
    }
  }

  /**
   * Transform an {@link Group} to Json
   *
   * @param group
   *          The group to transform.
   * @return The group in json format.
   */
  protected JValue groupToJSON(Group group) {
    List<Field> fields = new ArrayList<>();
    fields.add(f("identifier", v(group.getIdentifier())));
    fields.add(f("organization", v(group.getOrganization())));
    fields.add(f("role", v(group.getRole())));
    fields.add(f("name", v(group.getName(), Jsons.BLANK)));
    fields.add(f("description", v(group.getDescription(), Jsons.BLANK)));
    fields.add(f("roles", v(join(group.getRoles(), ","), Jsons.BLANK)));
    fields.add(f("members", v(join(group.getMembers(), ","), Jsons.BLANK)));
    return obj(fields);
  }

}
