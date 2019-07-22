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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.functions.Functions.chuck;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.impl.ProviderQuery;
import org.opencastproject.adminui.impl.RawProviderQuery;
import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.export.api.StatisticsExportService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/")
@RestService(name = "statistics", title = "statistics façade service",
  abstractText = "Provides statistics",
  notes = {"This service provides statistics."
    + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
    + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
    + "DO NOT use this for integration of third-party applications.<em>"})
public class StatisticsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsEndpoint.class);
  private static final String TIME_SERIES_PROVIDER_TYPE = "timeSeries";
  private static final String STATISTICS_ORGANIZATION_UI_ROLE = "ROLE_UI_STATISTICS_ORGANIZATION_VIEW";

  private SecurityService securityService;
  private IndexService indexService;
  private AdminUISearchIndex searchIndex;
  private StatisticsService statisticsService;
  private StatisticsExportService statisticsExportService;

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  public void setSearchIndex(AdminUISearchIndex searchIndex) {
    this.searchIndex = searchIndex;
  }

  public void setStatisticsService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  public void setStatisticsExportService(StatisticsExportService statisticsExportService) {
    this.statisticsExportService = statisticsExportService;
  }

  @GET
  @Path("providers.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getprovidersbyresourcetype", description = "Returns the available statistics providers for an (optional) resource type", returnDescription = "The available statistics providers as JSON", restParameters = {
    @RestParameter(name = "resourceType", description = "The resource type: either 'episode', 'series' or 'organization'", isRequired = false, type = STRING)},
    reponses = {
      @RestResponse(description = "Returns the providers for the given resource type as JSON, or all, if the resource type is missing", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED)
    })
  public Response getProviders(
    @QueryParam("resourceType") final String resourceTypeStr) {
    ResourceType resourceType;
    try {
      if (resourceTypeStr == null) {
        resourceType = null;
      } else {
        resourceType = Enum.valueOf(ResourceType.class, resourceTypeStr.toUpperCase());
      }
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest("invalid value for 'resourceType'");
    }

    JSONArray result = new JSONArray();
    statisticsService
      .getProviders(resourceType)
      .stream()
      .map(this::providerToJson)
      .forEach(result::add);
    return Response.ok(result.toJSONString()).build();
  }

  private static String providerTypeString(StatisticsProvider provider) {
    if (provider instanceof TimeSeriesProvider) {
      return TIME_SERIES_PROVIDER_TYPE;
    }
    return "unknown";
  }

  private JSONObject providerToJson(StatisticsProvider provider) {
    final JSONObject providerObj = new JSONObject();
    providerObj.put("providerId", provider.getId());
    providerObj.put("providerType", providerTypeString(provider));
    providerObj.put("title", provider.getTitle());
    if (provider instanceof TimeSeriesProvider) {
      providerObj.put("dataResolutions", resolutionsToJson(((TimeSeriesProvider) provider).getDataResolutions()));
    }
    providerObj.put("description", provider.getDescription());
    return providerObj;
  }

  private JSONArray resolutionsToJson(Set<DataResolution> resolutions) {
    JSONArray result = new JSONArray();
    for (DataResolution dataResolution : resolutions) {
      result.add(dataResolutionToJson(dataResolution));
    }
    return result;
  }

  private String dataResolutionToJson(DataResolution dataResolution) {
    return dataResolution.toString().toLowerCase();
  }

  @POST
  @Path("data.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getproviderdata", description = "Returns the statistical data for a specific provider and a specific resource", returnDescription = "The statistical data as JSON", restParameters = {
    @RestParameter(name = "data", isRequired = true, description = "A list of statistical data requests, containing a provider id, from, to, the resource id and a resolution - all as JSON", type = RestParameter.Type.TEXT) },
    reponses = {
      @RestResponse(description = "Returns the statistical data for the given resource type as JSON", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED)
    })
  public Response getProviderData(@FormParam("data") String data) {
    if (StringUtils.isBlank(data)) {
      return RestUtil.R.badRequest("No data set");
    }
    Gson gson = new Gson();
    RawProviderQuery[] dataJson;
    try {
      dataJson = gson.fromJson(data, RawProviderQuery[].class);
    } catch (Exception e) {
      logger.warn("Unable to parse data {}", data);
      return RestUtil.R.badRequest("Unable to parse data");
    }

    JSONArray result = new JSONArray();
    try {
      Arrays
        .stream(dataJson)
        .map(ProviderQuery::new)
        .flatMap(q ->
          statisticsService
            .getProvider(q.getProviderId())
            .map(Stream::of).orElseGet(Stream::empty)
            .peek(p -> checkAccess(q.getResourceId(), p.getResourceType()))
            .map(p -> timeSeriesToJson(
              p.getId(),
              statisticsService.getTimeSeriesData(p, q.getResourceId(), q.getFrom(), q.getTo(), q.getDataResolution(),
                ZoneId.systemDefault()))))
        .forEach(result::add);
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    }
    return Response.ok(result.toJSONString()).build();
  }

  @GET
  @Path("export.csv")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "getcsvdata", description = "Returns the statistical data for a specific provider and a specific resource as CSV.", returnDescription = "The statistical data as CSV", restParameters = {
    @RestParameter(name = "providerId", isRequired = true, description = "The provider id", type = RestParameter.Type.TEXT),
    @RestParameter(name = "resourceId", isRequired = true, description = "The resource id", type = RestParameter.Type.TEXT),
    @RestParameter(name = "from", isRequired = true, description = "The from date in iso 8601 UTC notation", type = RestParameter.Type.TEXT),
    @RestParameter(name = "to", isRequired = true, description = "The to date in iso 8601 UTC notation", type = RestParameter.Type.TEXT),
    @RestParameter(name = "dataResolution", isRequired = true, description = "The data resolution. Valid values are 'HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', and 'YEARLY'", type = RestParameter.Type.TEXT)},
    reponses = {
      @RestResponse(description = "Returns the statistical data for the given resource type as csv", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "If the current user is not authorized to perform this action", responseCode = HttpServletResponse.SC_UNAUTHORIZED)
    })
  public Response getCSVData(
    @QueryParam("providerId") String providerId,
    @QueryParam("resourceId") String resourceId,
    @QueryParam("from") String fromStr,
    @QueryParam("to") String toStr,
    @QueryParam("dataResolution") String dataResolutionStr) {
    try {
      final ProviderQuery q = new ProviderQuery(providerId, fromStr, toStr, dataResolutionStr, resourceId);
      final StatisticsProvider p = statisticsService
        .getProvider(providerId).orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + providerId));
      checkAccess(q.getResourceId(), p.getResourceType());
      final String csv = statisticsExportService.getCSV(p, q.getResourceId(), q.getFrom(), q.getTo(), q.getDataResolution(),
        searchIndex, ZoneId.systemDefault());
      return Response.ok().entity(csv).build();
    } catch (IllegalArgumentException e) {
      return RestUtil.R.badRequest(e.getMessage());
    } catch (SearchIndexException e) {
      return RestUtil.R.serverError();
    } catch (NotFoundException e) {
      return RestUtil.R.notFound(resourceId);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
  }

  private JSONObject timeSeriesToJson(String providerId, TimeSeries timeSeriesData) {
    final JSONObject result = new JSONObject();
    result.put("providerId", providerId);
    result.put("providerType", "timeSeries");
    result.put("labels", timeSeriesData.getLabels());
    result.put("values", timeSeriesData.getValues());
    if (timeSeriesData.getTotal().isPresent()) {
      result.put("total", timeSeriesData.getTotal().getAsDouble());
    }
    return result;
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
        // Thanks CheckStyle, very sensible
        default:
          break;
      }
    } catch (UnauthorizedException | SearchIndexException e) {
      chuck(e);
    }
  }

  private void checkMediapackageAccess(final String mpId) throws UnauthorizedException, SearchIndexException {
    final Opt<Event> event = indexService.getEvent(mpId, searchIndex);
    if (event.isNone()) {
      // IndexService checks permissions and returns None if user is unauthorized
      throw new UnauthorizedException(securityService.getUser(), "read");
    }
  }

  private void checkSeriesAccess(final String seriesId) throws UnauthorizedException, SearchIndexException {
    final Opt<Series> series = indexService.getSeries(seriesId, searchIndex);
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

    final boolean userIsInOrg = currentOrgId.equals(orgId);

    boolean userIsAdmin = currentUser.hasRole(GLOBAL_ADMIN_ROLE)
      || (currentUser.hasRole(currentOrgAdminRole) && userIsInOrg);

    boolean userIsAuthorized = currentUser.hasRole(STATISTICS_ORGANIZATION_UI_ROLE) && userIsInOrg;

    if (!userIsAdmin && !userIsAuthorized) {
      throw new UnauthorizedException(currentUser, "read");
    }
  }

}
