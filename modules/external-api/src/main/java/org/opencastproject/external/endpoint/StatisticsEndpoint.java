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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.index.ExternalIndex;
import org.opencastproject.external.util.statistics.QueryUtils;
import org.opencastproject.external.util.statistics.ResourceTypeUtils;
import org.opencastproject.external.util.statistics.StatisticsProviderUtils;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_3_0 })
@RestService(
  name = "externalapistatistics", title = "External API Statistics Endpoint",
  notes = {}, abstractText = "Provides statistics")
public class StatisticsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsEndpoint.class);

  private SecurityService securityService;
  private IndexService indexService;
  private ExternalIndex externalIndex;
  private StatisticsService statisticsService;

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  public void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  public void setStatisticsService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  /** OSGi activation method */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Statistics Endpoint");
  }

  @GET
  @Path("providers")
  @RestQuery(
    name = "getproviders",
    description = "Returns a list of available statistics providers",
    returnDescription = "The list of available statistics providers as JSON",
    restParameters = {
      @RestParameter(
        name = "filter", isRequired = false,
        description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.",
        type = RestParameter.Type.STRING),
      @RestParameter(
        name = "withparameters", isRequired = false,
        description = "Whether the parameters should be included in the response.",
        type = RestParameter.Type.BOOLEAN)
    },
    reponses = {
      @RestResponse(
        description = "Returns the requested statistics providers as JSON",
        responseCode = HttpServletResponse.SC_OK),
      @RestResponse(
        description = "If the current user is not authorized to perform this action",
        responseCode = HttpServletResponse.SC_UNAUTHORIZED)
    })
  public Response getProviders(@HeaderParam("Accept") String acceptHeader, @QueryParam("filter") String filter,
        @QueryParam("withparameters") Boolean withParameters) {

    ResourceType resourceType = null;

    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        String[] filterTuple = f.split(":");
        if (filterTuple.length != 2) {
          logger.debug("No value for filter {} in filters list: {}", filterTuple[0], filter);
          continue;
        }
        String name = filterTuple[0];
        String value = f.substring(name.length() + 1);

        if ("resourceType".equals(name)) {
          try {
            resourceType = ResourceTypeUtils.fromString(value);
          } catch (IllegalArgumentException e) {
            return RestUtil.R.badRequest("Invalid value for 'resourceType'");
          }
        } else {
          logger.warn("Unknown filter criteria {}", name);
          return RestUtil.R.badRequest("Unknown filter");
        }
      }
    }

    Set<StatisticsProvider> providers;
    if (resourceType != null) {
      providers = statisticsService.getProviders(resourceType);
    } else {
      providers = statisticsService.getProviders();
    }

    JSONArray result = new JSONArray();
    providers.stream().map(p -> StatisticsProviderUtils.toJson(p, withParameters)).forEach(result::add);

    return ApiResponses.Json.ok(acceptHeader, result.toJSONString());
  }

  @GET
  @Path("providers/{providerId}")
  @RestQuery(
    name = "getprovider",
    description = "Returns the statistics provider with the specified id",
    returnDescription = "The requested statistics provider",
    pathParameters = {
      @RestParameter(
        name = "providerId", description = "The identifier of the statistics provider",
        isRequired = true, type = RestParameter.Type.STRING)
    },
    restParameters = {
      @RestParameter(
        name = "withparameters", isRequired = false,
        description = "Whether the parameters should be included in the response.",
        type = RestParameter.Type.BOOLEAN)
    },
    reponses = {
      @RestResponse(
        description = "Returns the requested statistics provider as JSON",
        responseCode = HttpServletResponse.SC_OK)
    })
  public Response getProvider(@HeaderParam("Accept") String acceptHeader, @PathParam("providerId") String id,
        @QueryParam("withparameters") Boolean withParameters) {

    if (StringUtils.isNotBlank(id)) {
      Optional<StatisticsProvider> provider = statisticsService.getProvider(id);
      if (provider.isPresent()) {
        return ApiResponses.Json.ok(acceptHeader, StatisticsProviderUtils.toJson(provider.get(),
            withParameters).toJSONString());
      } else {
        return ApiResponses.notFound("Cannot find a statistics provider with id '%s'.", id);
      }
    } else {
      return RestUtil.R.badRequest("Invalid value for providerId");
    }
  }

  @POST
  @Path("data/query")
  @RestQuery(
    name = "getstatistics",
    description = "Returns the statistical data based on the query posted",
    returnDescription = "The statistical data as JSON array",
    restParameters = {
      @RestParameter(
        name = "data", description = "An JSON array describing the queries to be executed",
        isRequired = true, type = RestParameter.Type.TEXT)
    },
    reponses = {
      @RestResponse(
        description = "Returns the statistical data as requested by the query as JSON array",
        responseCode = HttpServletResponse.SC_OK),
      @RestResponse(
        description = "If the current user is not authorized to perform this action",
        responseCode = HttpServletResponse.SC_UNAUTHORIZED)
    })
  public Response getStatistics(@HeaderParam("Accept") String acceptHeader, @FormParam("data") String data) {

    List<QueryUtils.Query> queries = null;
    try {
      queries = QueryUtils.parse(data, statisticsService);
    } catch (Exception e) {
      logger.debug("Unable to parse form parameter 'data' {}, exception: {}", data, e);
      return RestUtil.R.badRequest("Unable to parse form parameter 'data': " + e.getMessage());
    }

    JSONArray result = new JSONArray();
    queries.stream()
      .peek(query -> checkAccess(query.getParameters().getResourceId(), query.getProvider().getResourceType()))
      .map(query -> QueryUtils.execute(query))
      .forEach(result::add);

    return ApiResponses.Json.ok(acceptHeader, result.toJSONString());
  }

  private void checkAccess(final String resourceId, final ResourceType resourceType) {
    try {
      switch (resourceType) {
        case EPISODE:
          checkMediapackageAccess(resourceId);
          break;
        case SERIES:
          checkSeriesAccess(resourceId);
          break;
        case ORGANIZATION:
          checkOrganizationAccess(resourceId);
          break;
        default:
          break;
      }
    } catch (UnauthorizedException | SearchIndexException e) {
      chuck(e);
    }
  }

  private void checkMediapackageAccess(final String mpId) throws UnauthorizedException, SearchIndexException {
    final Opt<Event> event = indexService.getEvent(mpId, externalIndex);
    if (event.isNone()) {
      // IndexService checks permissions and returns None if user is unauthorized
      throw new UnauthorizedException(securityService.getUser(), "read");
    }
  }

  private void checkSeriesAccess(final String seriesId) throws UnauthorizedException, SearchIndexException {
    final Opt<Series> series = indexService.getSeries(seriesId, externalIndex);
    if (series.isNone()) {
      // IndexService checks permissions and returns None if user is unauthorized
      throw new UnauthorizedException(securityService.getUser(), "read");
    }
  }

  private void checkOrganizationAccess(final String orgId) throws UnauthorizedException {
    final User currentUser = securityService.getUser();
    final Organization currentOrg = securityService.getOrganization();
    final String currentOrgAdminRole = currentOrg.getAdminRole();
    final String currentOrgId = currentOrg.getId();

    boolean authorized = currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            || (currentUser.hasRole(currentOrgAdminRole) && currentOrgId.equals(orgId));

    if (!authorized) {
      throw new UnauthorizedException(currentUser, "read");
    }
  }

}
