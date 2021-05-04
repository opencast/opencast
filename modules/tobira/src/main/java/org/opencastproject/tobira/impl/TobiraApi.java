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

import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

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

  @Activate
  public void activate(BundleContext bundleContext) {
    logger.info("Activated Tobira API");
  }

  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  @GET
  @Path("/harvest")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "harvest",
      description = "Harvesting API to get incremental updates about series and events.",
      restParameters = {
          @RestParameter(
              name = "limit",
              isRequired = true,
              description = "The maximum number of items to return. Has to be positive.",
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
      @QueryParam("limit") Integer limit,
      @QueryParam("since") Long since
  ) {
    // Parameter error handling
    if (since == null) {
      return badRequest("Required parameter 'since' not specified");
    }
    if (limit == null) {
      return badRequest("Required parameter 'limit' not specified");
    }
    if (since < 0) {
      return badRequest("Parameter 'since' < 0, but it has to be positive or 0");
    }
    if (limit <= 0) {
      return badRequest("Parameter 'limit' <= 0, but it has to be positive");
    }

    logger.debug("Request to '/harvest' with limit={} and since={}", limit, since);

    try {
      final SearchQuery q = new SearchQuery()
          .withUpdatedSince(new Date(since))
          .withSort(SearchQuery.Sort.DATE_MODIFIED)
          .includeDeleted(true)
          // We fetch one item more to know the timestamp of that item. See below how this is used.
          .withLimit(limit + 1);
      final SearchResultItem[] results = searchService.getByQuery(q).getItems();
      logger.debug("Retrieved {} events from the index during harvest", results.length);

      // Obtain information to allow Tobira to plan the next harvesting request. The `since`
      // timestamp that needs to be used next is always the timestamp of the last item. In case we
      // got `limit + 1` items, it's exactly the timestamp of the next new item. Otherwise, it's the
      // timestamp of an item that Tobira already received. But Tobira has to be able to deal with
      // duplicate items anyway.
      boolean hasMore = results.length == limit + 1;
      long includesItemsUntil = results.length > 0
          ? results[results.length - 1].getModified().getTime()
          : since; // TODO: this could maybe be `now()` or `now() - 5 min`

      final ArrayList<Jsons.Val> items = Arrays.stream(results)
          // We fetched up to `limit + 1` items above, so we limit here again to the actual number
          // of items.
          .limit(limit)
          .map(item -> {
            long modified = item.getModified().getTime();

            // TODO: does `deleted != null` really imply the event is deleted?
            if (item.getDeletionDate() == null) {
              return Jsons.obj(
                  Jsons.p("kind", "event"),
                  Jsons.p("id", item.getId()),
                  Jsons.p("title", item.getDcTitle()),
                  Jsons.p("partOf", item.getDcIsPartOf()),
                  Jsons.p("description", item.getDcDescription()),
                  Jsons.p("updated", modified)
              );
            } else {
              return Jsons.obj(
                  Jsons.p("kind", "event-deleted"),
                  Jsons.p("id", item.getId()),
                  Jsons.p("updated", modified)
              );
            }
          })
          .collect(Collectors.toCollection(ArrayList::new));

      // Assembly full response
      final Jsons.Obj json = Jsons.obj(
          Jsons.p("includesItemsUntil", includesItemsUntil),
          Jsons.p("hasMore", hasMore),
          Jsons.p("items", Jsons.arr(items))
      );
      logger.debug("Returning {} items from harvesting (hasMore={})", items.size(), hasMore);

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
