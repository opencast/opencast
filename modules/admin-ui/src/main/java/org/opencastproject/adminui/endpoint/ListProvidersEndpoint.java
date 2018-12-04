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

import static org.opencastproject.adminui.endpoint.EndpointUtil.addRequestFiltersToQuery;
import static org.opencastproject.adminui.endpoint.EndpointUtil.generateJSONObject;

import org.opencastproject.adminui.exception.JsonCreationException;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.exception.ListProviderNotFoundException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.AclsListQuery;
import org.opencastproject.index.service.resources.list.query.AgentsListQuery;
import org.opencastproject.index.service.resources.list.query.EventListQuery;
import org.opencastproject.index.service.resources.list.query.GroupsListQuery;
import org.opencastproject.index.service.resources.list.query.JobsListQuery;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.resources.list.query.SeriesListQuery;
import org.opencastproject.index.service.resources.list.query.ServersListQuery;
import org.opencastproject.index.service.resources.list.query.ServicesListQuery;
import org.opencastproject.index.service.resources.list.query.ThemesListQuery;
import org.opencastproject.index.service.resources.list.query.UsersListQuery;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "ResourceListsProviders", title = "Admin UI - Resources List",
  abstractText = "This service provides key-value list from different resources to use in the admin UI.",
  notes = { "This service offers access to list providers for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class ListProvidersEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(ListProvidersEndpoint.class);

  public static final Response UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();
  public static final Response NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build();
  public static final Response SERVER_ERROR = Response.serverError().build();
  public static final Response NO_CONTENT = Response.noContent().build();

  private SecurityService securityService;
  private ListProvidersService listProvidersService;
  private SeriesEndpoint seriesEndpoint;

  protected void activate(BundleContext bundleContext) {
    logger.info("Activate list provider service");
  }

  /** OSGi callback for series services. */
  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  /** OSGi callback for sercurity service. */
  public void setSecurityService(SecurityService securitySerivce) {
    this.securityService = securitySerivce;
  }

  /** OSGi callback for series end point. */
  public void setSeriesEndpoint(SeriesEndpoint seriesEndpoint) {
    this.seriesEndpoint = seriesEndpoint;
  }

  @GET
  @Path("{source}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "list", description = "Provides key-value list from the given source", pathParameters = { @RestParameter(name = "source", description = "The source for the key-value list", isRequired = true, type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(description = "The maximum number of items to return per page", isRequired = false, name = "limit", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "The offset", isRequired = false, name = "offset", type = RestParameter.Type.INTEGER),
          @RestParameter(description = "Filters", isRequired = false, name = "filter", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "Returns the key-value list for the given source.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getList(@PathParam("source") final String source, @QueryParam("limit") final int limit,
          @QueryParam("filter") final String filter, @QueryParam("offset") final int offset,
          @Context HttpHeaders headers) {

    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setLimit(limit);
    query.setOffset(offset);
    addRequestFiltersToQuery(filter, query);
    Map<String, String> autocompleteList;
    try {
      autocompleteList = listProvidersService.getList(source, query, false);
    } catch (ListProviderNotFoundException e) {
      logger.debug("No list found for {}", source, e);
      return NOT_FOUND;
    } catch (ListProviderException e) {
      logger.error("Server error when getting list from provider {}", source, e);
      return SERVER_ERROR;
    }

    JSONObject jsonList;
    try {
      jsonList = generateJSONObject(autocompleteList);
    } catch (JsonCreationException e) {
      logger.error("Not able to generate resources list JSON from source {}", source, e);
      return SERVER_ERROR;
    }

    return Response.ok(jsonList.toString()).build();
  }

  @GET
  @Path("components.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "components", description = "Provides a set of constants lists (right now only eventCommentReasons) for use in the admin UI",
    reponses = { @RestResponse(description = "Returns a set of constants lists (right now only eventCommentReasons) for use in the admin UI",
    responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getComponents(@Context HttpHeaders headers) {
    String[] sources = { "eventCommentReasons" };
    ResourceListQuery query = new ResourceListQueryImpl();

    JSONObject list = new JSONObject();

    for (String source : sources) {
      if (listProvidersService.hasProvider(source)) {
        JSONObject subList;
        try {
          subList = generateJSONObject(listProvidersService.getList(source, query, true));
          list.put(source, subList);
        } catch (JsonCreationException e) {
          logger.error("Not able to generate resources list JSON from source {}: {}", source, e);
          return SERVER_ERROR;
        } catch (ListProviderException e) {
          logger.error("Not able to get list from provider {}: {}", source, e);
          return SERVER_ERROR;
        }
      } else {
        return NOT_FOUND;
      }
    }

    return Response.ok(list.toString()).build();
  }

  @GET
  @Path("providers.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "availableProviders", description = "Provides the list of the available list providers", reponses = { @RestResponse(description = "Returns the availables list providers.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getAvailablesProviders(@Context HttpHeaders headers) {
    JSONArray list = new JSONArray();

    list.add(listProvidersService.getAvailableProviders());

    return Response.ok(list.toString()).build();
  }

  @GET
  @Path("{page}/filters.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "filters", description = "Provides filters for the given page", pathParameters = { @RestParameter(name = "page", description = "The page for which the filters are required", isRequired = true, type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "Returns the filters for the given page.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getFilters(@PathParam("page") final String page, @Context HttpHeaders headers)
          throws ListProviderException {

    ResourceListQuery query = new ResourceListQueryImpl();

    if ("series".equals(page)) {
      query = new SeriesListQuery();
    } else if ("events".equals(page)) {
      query = new EventListQuery();
    } else if ("jobs".equals(page)) {
      query = new JobsListQuery();
    } else if ("recordings".equals(page)) {
      query = new AgentsListQuery();
    } else if ("users".equals(page)) {
      query = new UsersListQuery();
    } else if ("groups".equals(page)) {
      query = new GroupsListQuery();
    } else if ("acls".equals(page)) {
      query = new AclsListQuery();
    } else if ("servers".equals(page)) {
      query = new ServersListQuery();
    } else if ("services".equals(page)) {
      query = new ServicesListQuery();
    } else if ("themes".equals(page)) {
      query = new ThemesListQuery();
    } else {
      logger.debug("No filters defined for the page {}.", page);
      return NO_CONTENT;
    }

    try {
      if ("events".equals(page) && seriesEndpoint.getOnlySeriesWithWriteAccessEventsFilter()) {
        Map<String, String> seriesWriteAccess = seriesEndpoint.getUserSeriesByAccess(true);
        return RestUtils.okJson(JSONUtils.filtersToJSONSeriesWriteAccess(query, listProvidersService,
                securityService.getOrganization(), seriesWriteAccess));
      } else {
        return RestUtils.okJson(JSONUtils.filtersToJSON(query, listProvidersService, securityService.getOrganization()));
      }
    } catch (ListProviderException e) {
      logger.error("Not able to get list of options for the filters for the page {}: {}", page, e);
      return SERVER_ERROR;
    }
  }

}
