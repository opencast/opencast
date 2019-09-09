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

package org.opencastproject.statistics.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Statistics Service.
 *
 */
@Path("/")
@RestService(name = "statisticsservice", title = "Statistics Service", abstractText = "This service provides statistics.", notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"})
public class StatisticsRestService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(StatisticsRestService.class);

  /** Statistics Service */
  private StatisticsService statisticsService;

  /**
   * OSGi callback for setting statistics service.
   *
   * @param statisticsService
   */
  public void setService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  /**
   * Activates REST service (needed by OSGi)
   *
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("providers.json")
  @RestQuery(name = "getAllAsJson", description = "Returns active providers", returnDescription = "Returns the active providers JSON document",
      reponses = {
          @RestResponse(responseCode = SC_OK, description = "The active providers.")
      })
  public Response getProviders() {
    try {
      return Response.ok(providersToJson(statisticsService.getProviders()).toJSONString()).build();
    } catch (Exception e) {
      logger.error("Could not retrieve providers: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("timeseries/{providerId:.+}.json")
  @RestQuery(name = "getTimeSeriesAsJson", description = "Returns the time series data for the given providerId", returnDescription = "Returns the time series data JSON document",
      pathParameters = {@RestParameter(name = "providerId", isRequired = true, description = "The provider identifier", type = STRING)},
      restParameters = {
          @RestParameter(name = "resourceId", description = "The id of the resource to get the data for. E.g. episode id.", isRequired = true, type = STRING),
          @RestParameter(name = "from", description = "Start of the time series as ISO 8601 UTC date string", isRequired = true, type = STRING),
          @RestParameter(name = "to", description = "End of the time series as ISO 8601 UTC date string", isRequired = true, type = STRING),
          @RestParameter(name = "resolution", description = "Data aggregation level. Must be one of 'daily', 'weekly', 'monthly', 'yearly'", isRequired = true, type = STRING),
          @RestParameter(name = "zoneId", description = "The time zone id to use for calculations", isRequired = true, type = STRING),
      },
      reponses = {
          @RestResponse(responseCode = SC_OK, description = "The time series data.")
      })
  public Response getTimeSeriesData(
      @PathParam("providerId") final String providerId,
      @QueryParam("resourceId") final String resourceId,
      @QueryParam("from") final String from,
      @QueryParam("to") final String to,
      @QueryParam("resolution") final String resolution,
      @QueryParam("zoneId") final String zoneId
  ) {
    try {
      final TimeSeries timeSeries = statisticsService.getTimeSeriesData(
          statisticsService.getProvider(providerId).orElseThrow(NotFoundException::new),
          resourceId,
          Instant.parse(from),
          Instant.parse(to),
          DataResolution.fromString(resolution),
          ZoneId.of(zoneId)
      );
      return Response.ok(timeSeriesToJson(timeSeries).toJSONString()).build();
    } catch (Exception e) {
      logger.error("Could not retrieve time series data for provider {}: {}", providerId, e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @SuppressWarnings("unchecked")
  private static JSONObject timeSeriesToJson(final TimeSeries timeSeries) {
    final JSONObject json = new JSONObject();
    final JSONArray jsonLabels = new JSONArray();
    final JSONArray jsonValues = new JSONArray();
    for (int i = 0; i < timeSeries.getValues().size(); i++) {
      jsonLabels.add(timeSeries.getLabels().get(i));
      jsonValues.add(timeSeries.getValues().get(i));
    }
    json.put("labels", jsonLabels);
    json.put("values", jsonValues);
    if (timeSeries.getTotal().isPresent()) {
      json.put("total", timeSeries.getTotal().getAsDouble());
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  private static JSONArray providersToJson(final Set<StatisticsProvider> providers) {
    final JSONArray json = new JSONArray();
    for (StatisticsProvider provider : providers) {
      final JSONObject jsonProvider = new JSONObject();
      final JSONArray jsonResolutions = new JSONArray();
      jsonProvider.put("id", provider.getId());
      jsonProvider.put("title", provider.getTitle());
      jsonProvider.put("description", provider.getDescription());
      jsonProvider.put("resourceType", provider.getResourceType().name());
      if (provider instanceof TimeSeriesProvider) {
        for (DataResolution resolution : ((TimeSeriesProvider) provider).getDataResolutions()) {
          jsonResolutions.add(resolution.name());
        }
        jsonProvider.put("dataResolutions", jsonResolutions);
      }
      json.add(jsonProvider);
    }
    return json;
  }
}
