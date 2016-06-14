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

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static com.entwinemedia.fn.data.json.Jsons.vN;

import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    logger.info("Activating External API - Events Endpoint");
  }

  @GET
  @Path("")
  @Produces({ "application/json", "application/v1.0.0+json" })
  public Response getGroups(@HeaderParam("Accept") String acceptHeader, @QueryParam("id") String id,
          @QueryParam("filter") String filter, @QueryParam("sort") String sort, @QueryParam("offset") Integer offset,
          @QueryParam("limit") Integer limit) {
    Opt<Integer> optLimit = Opt.nul(limit);
    if (limit <= 0)
      optLimit = Opt.none();
    Opt<Integer> optOffset = Opt.nul(offset);
    if (offset < 0)
      optOffset = Opt.none();

    SearchResult<Group> results;
    try {
      results = indexService.getGroups(filter, optLimit, optOffset, Opt.nul(StringUtils.trimToNull(sort)),
              externalIndex);
    } catch (SearchIndexException e) {
      logger.error("The External Search Index was not able to get the groups list: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    // If the results list if empty, we return already a response.
    List<JValue> groupsList = new ArrayList<JValue>();
    for (SearchResultItem<Group> item : results.getItems()) {
      groupsList.add(groupToJSON(item.getSource()));
    }
    return ApiResponses.Json.ok(ApiVersion.VERSION_1_0_0, a(groupsList));
  }

  @GET
  @Path("{groupId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  public Response getGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id)
          throws Exception {
    for (final Group group : indexService.getGroup(id, externalIndex)) {
      return ApiResponses.Json.ok(ApiVersion.CURRENT_VERSION, group.toJSON());
    }
    return ApiResponses.notFound("Cannot find an event with id '%s'.", id);
  }

  @DELETE
  @Path("{groupId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  public Response deleteGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id)
          throws NotFoundException {
    return indexService.removeGroup(id);
  }

  @PUT
  @Path("{groupId}")
  @Produces({ "application/json", "application/v1.0.0+json" })
  public Response updateGroup(@HeaderParam("Accept") String acceptHeader, @PathParam("groupId") String id,
          @FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("members") String members) throws Exception {
    return indexService.updateGroup(id, name, description, roles, members);
  }

  @POST
  @Path("")
  public Response createGroup(@HeaderParam("Accept") String acceptHeader, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("members") String members) {
    return indexService.createGroup(name, description, roles, members);
  }

  /**
   * Transform an {@link Group} to Json
   *
   * @param group
   *          The group to transform.
   * @return The group in json format.
   */
  protected JValue groupToJSON(Group group) {
    List<JField> fields = new ArrayList<JField>();
    fields.add(f("id", v(group.getIdentifier())));
    fields.add(f("organization", v(group.getOrganization())));
    fields.add(f("role", v(group.getRole())));
    fields.add(f("name", vN(group.getName())));
    fields.add(f("description", vN(group.getDescription())));
    fields.add(f("roles", vN(StringUtils.join(group.getRoles(), ","))));
    fields.add(f("members", vN(StringUtils.join(group.getMembers(), ","))));
    return j(fields);
  }

}
