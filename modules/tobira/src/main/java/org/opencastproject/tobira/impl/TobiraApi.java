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

package org.opencastproject.tobira.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.opencastproject.util.doc.rest.RestParameter.Type;

import org.opencastproject.search.api.SearchService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Tobira API Endpoint
 */
@Path("")
@RestService(
    name = "TobiraApiEndpoint",
    title = "Tobira API Endpoint",
    abstractText = "Opencast Tobira API endpoint.",
    notes = { "This provides API endpoint used by Tobira to harvest media metadata" }
)
public class TobiraApi {
  private static final Logger logger = LoggerFactory.getLogger(TobiraApi.class);

  private SearchService searchService;
  private SeriesService seriesService;

  @Activate
  public void activate(BundleContext bundleContext) {
    logger.info("Activated Tobira API");
  }

  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  public void setSeriesService(SeriesService service) {
    this.seriesService = service;
  }

  @GET
  @Path("/harvest")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "harvest",
      description = "Harvesting API to get incremental updates about series and events.",
      restParameters = {
          @RestParameter(
              name = "preferredAmount",
              isRequired = true,
              description = "A preferred number of items the request should return. This is "
                  + "merely a rough guideline and the API might return more or fewer items than "
                  + "this parameter. You cannot rely on an exact number of returned items! "
                  + "In practice this API usually returns between 0 and twice this parameter "
                  + "number of items.",
              type = Type.INTEGER
          ),
          @RestParameter(
              name = "since",
              isRequired = true,
              description = "Only return items that changed after or at this timestamp. "
                  + "Specified in milliseconds since 1970-01-01T00:00:00Z.",
              type = Type.INTEGER
          ),
      },
      responses = {
          @RestResponse(description = "Event and Series Data", responseCode = HttpServletResponse.SC_OK)
      },
      returnDescription = "Event and Series Data changed after the given timestamp"
  )
  public Response harvest(
      @QueryParam("preferredAmount") Integer preferredAmount,
      @QueryParam("since") Long since
  ) {
    // Parameter error handling
    if (since == null) {
      return badRequest("Required parameter 'since' not specified");
    }
    if (preferredAmount == null) {
      return badRequest("Required parameter 'preferredAmount' not specified");
    }
    if (since < 0) {
      return badRequest("Parameter 'since' < 0, but it has to be positive or 0");
    }
    if (preferredAmount <= 0) {
      return badRequest("Parameter 'preferredAmount' <= 0, but it has to be positive");
    }

    logger.debug("Request to '/harvest' with preferredAmount={} since={}", preferredAmount, since);

    try {
      Jsons.Obj json = Harvest.harvest(
          preferredAmount, new Date(since), searchService, seriesService);

      return Response.ok()
          .type(APPLICATION_JSON_TYPE)
          // TODO: encoding
          .entity(json.toJson())
          .build();
    } catch (Exception e) {
      logger.error("Unexpected exception in tobira/harvest", e);
      return Response.serverError().build();
    }
  }

  private static Response badRequest(String msg) {
    logger.warn("Bad request to tobira/harvest: {}", msg);
    return Response.status(BAD_REQUEST).entity(msg).build();
  }
}
