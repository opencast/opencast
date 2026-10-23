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
package org.opencastproject.basicstatisticsaggregation.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseException;
import org.opencastproject.basicstatisticsaggregation.AggregatedEvent;
import org.opencastproject.basicstatisticsaggregation.AggregatedTotal;
import org.opencastproject.basicstatisticsaggregation.BasicStatisticsAggregationService;
import org.opencastproject.basicstatisticsaggregation.persistence.BasicStatisticsAggregationDatabaseException;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component(
    property = {
        "service.description=Basic Statistics Aggregation REST Endpoint",
        "opencast.service.type=org.opencastproject.basicstatisticsaggregation",
        "opencast.service.path=/basicstatistics-aggregation",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = BasicStatisticsAggregationRestEndpoint.class
)
@Path("/basicstatistics-aggregation")
@RestService(
    name = "BasicStatisticsAggregationEndpoint",
    title = "Basic Statistics Aggregation Endpoint",
    abstractText = "Read access to aggregated basic statistics (views/watchtime)",
    notes = { "" }
)
@JaxrsResource
public class BasicStatisticsAggregationRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsAggregationRestEndpoint.class);

  private static final Gson GSON = new Gson();

  /** The service */
  protected BasicStatisticsAggregationService basicStatisticsAggregationService;

  protected SecurityService securityService;

  @GET
  @Path("{itemType}/total")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getTotal",
      description = "Get the all-time view/watchtime total for an item.",
      returnDescription = "A JSON object with the item's all-time views and watchtime (in seconds).",
      pathParameters = {
          @RestParameter(name = "itemType", isRequired = true, type = STRING, defaultValue = "video",
              description = "The item's type, e.g. video."),
      },
      restParameters = {
          @RestParameter(name = "itemId", isRequired = true, type = STRING,
              description = "The item's id."),
      },
      responses = {
          @RestResponse(description = "Returns the total.", responseCode = SC_OK),
          @RestResponse(description = "itemType is unknown.", responseCode = SC_BAD_REQUEST),
          @RestResponse(description = "Could not fetch the total.", responseCode = SC_INTERNAL_SERVER_ERROR),
      })
  public Response getTotal(@PathParam("itemType") String itemTypeParam, @QueryParam("itemId") String itemId) {
    ItemType itemType = parseItemType(itemTypeParam);
    if (itemType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Unknown itemType '" + itemTypeParam + "'")
          .build();
    }

    try {
      AggregatedTotal total = basicStatisticsAggregationService.getTotal(itemType, itemId);

      TotalDto dto = new TotalDto();
      dto.setItemType(itemType.toString());
      dto.setItemId(itemId);
      dto.setViews(total.getTotalViews());
      dto.setWatchtime(total.getTotalWatchtime());

      return Response.ok(GSON.toJson(dto), MediaType.APPLICATION_JSON).build();
    } catch (BasicStatisticsAggregationDatabaseException e) {
      logger.warn("Could not fetch total for item {}", itemId, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("{itemType}/daily")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getDailyStats",
      description = "Get the hourly view/watchtime buckets for an item over a range of days. Only days with at "
          + "least one recorded view are included.",
      returnDescription = "A JSON array of per-day objects, each with 24 hourly view counts and 24 hourly "
          + "watchtime values (in seconds).",
      pathParameters = {
          @RestParameter(name = "itemType", isRequired = true, type = STRING, defaultValue = "video",
              description = "The item's type, e.g. video."),
      },
      restParameters = {
          @RestParameter(name = "itemId", isRequired = true, type = STRING,
              description = "The item's id."),
          @RestParameter(name = "from", isRequired = true, type = STRING,
              description = "The first day to include, as an ISO date, e.g. 2026-01-01."),
          @RestParameter(name = "to", isRequired = true, type = STRING,
              description = "The last day to include (inclusive), as an ISO date, e.g. 2026-01-31."),
      },
      responses = {
          @RestResponse(description = "Returns the daily stats.", responseCode = SC_OK),
          @RestResponse(description = "A parameter is missing or malformed.", responseCode = SC_BAD_REQUEST),
          @RestResponse(description = "Could not fetch the daily stats.", responseCode = SC_INTERNAL_SERVER_ERROR),
      })
  public Response getDailyStats(@PathParam("itemType") String itemTypeParam, @QueryParam("itemId") String itemId,
      @QueryParam("from") String fromParam, @QueryParam("to") String toParam) {
    ItemType itemType = parseItemType(itemTypeParam);
    if (itemType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Unknown itemType '" + itemTypeParam + "'")
          .build();
    }

    LocalDate from;
    LocalDate to;
    try {
      from = LocalDate.parse(fromParam);
      to = LocalDate.parse(toParam);
    } catch (DateTimeParseException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("from/to must be ISO dates, e.g. 2026-01-01")
          .build();
    }

    try {
      List<AggregatedEvent> events = basicStatisticsAggregationService.getAggregatedEvents(itemType, itemId, from,
          to);

      List<DayStatsDto> dtos = new ArrayList<>();
      for (AggregatedEvent event : events) {
        dtos.add(new DayStatsDto(event.getDay().toString(), event.getViews(), event.getWatchtime()));
      }

      return Response.ok(GSON.toJson(dtos), MediaType.APPLICATION_JSON).build();
    } catch (BasicStatisticsAggregationDatabaseException e) {
      logger.warn("Could not fetch daily stats for item {}", itemId, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("reaggregate")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "reaggregate",
      description = "Re-aggregate view/watchtime data for one video over a range of days (or, if from/to are "
          + "omitted, all recorded history). Admin only.",
      returnDescription = "A JSON object with the number of days recomputed.",
      restParameters = {
          @RestParameter(name = "itemId", isRequired = true, type = STRING,
              description = "The video's id."),
          @RestParameter(name = "from", isRequired = false, type = STRING,
              description = "The first day to re-aggregate, as an ISO date, e.g. 2026-01-01. If omitted, starts "
                  + "from the beginning of recorded history."),
          @RestParameter(name = "to", isRequired = false, type = STRING,
              description = "The last day to re-aggregate (inclusive), as an ISO date, e.g. 2026-01-31. If "
                  + "omitted, re-aggregates up through today."),
      },
      responses = {
          @RestResponse(description = "Re-aggregation completed.", responseCode = SC_OK),
          @RestResponse(description = "from/to is malformed.", responseCode = SC_BAD_REQUEST),
          @RestResponse(description = "The current user is not an organization or system administrator.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(description = "Re-aggregation failed.", responseCode = SC_INTERNAL_SERVER_ERROR),
      })
  public Response reaggregate(@QueryParam("itemId") String itemId, @QueryParam("from") String fromParam,
      @QueryParam("to") String toParam) {
    if (!currentUserIsAdmin()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    LocalDate from = null;
    LocalDate to = null;
    try {
      if (fromParam != null) {
        from = LocalDate.parse(fromParam);
      }
      if (toParam != null) {
        to = LocalDate.parse(toParam);
      }
    } catch (DateTimeParseException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("from/to must be ISO dates, e.g. 2026-01-01")
          .build();
    }

    try {
      int itemDaysReaggregated = basicStatisticsAggregationService.reaggregate(itemId, from, to);
      return Response.ok(GSON.toJson(new ReaggregationResultDto(itemDaysReaggregated)), MediaType.APPLICATION_JSON)
          .build();
    } catch (BasicStatisticsDatabaseException | BasicStatisticsAggregationDatabaseException e) {
      logger.warn("Could not re-aggregate itemId={}", itemId, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean currentUserIsAdmin() {
    User user = securityService.getUser();
    String orgAdminRole = securityService.getOrganization().getAdminRole();
    return user.hasRole(SecurityConstants.GLOBAL_ADMIN_ROLE) || user.hasRole(orgAdminRole);
  }

  private record ReaggregationResultDto(int itemDaysReaggregated) {
  }

  /**
   * @return the parsed {@link ItemType}, or {@code null} if {@code itemTypeParam} doesn't match one
   */
  private static ItemType parseItemType(String itemTypeParam) {
    if (itemTypeParam == null) {
      return null;
    }
    try {
      return ItemType.valueOf(itemTypeParam.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Reference
  public void setBasicStatisticsAggregationService(BasicStatisticsAggregationService service) {
    this.basicStatisticsAggregationService = service;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
