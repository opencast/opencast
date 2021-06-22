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
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.util.doc.rest.RestParameter.Type;

import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.series.api.Series;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  /**
   * A duration to allow some leeway for certain database operations to be slow.
   *
   * The correct usage of the harvesting API depends on the correct adjustment of the {@code since}
   * parameter between different requests. If `since` is increased too much, some event/series
   * modifications could be missed. One fact which makes this harder is that the modified date of
   * events/series can be significantly lower/before the time those changes are written to the
   * database or index. That's because the "now" timestamp is created in Java and written to the
   * database afterwards. Unfortunately, we are not just talking about milliseconds, but up to many
   * seconds. This can have different reasons, but since Opencast can be distributed, network plays
   * a big role here.
   *
   * But to not completely give up on an incremental harvesting API, we define an arbitrary "buffer
   * period". Writes that take longer (i.e. the time between `new Date()` and the serialization in
   * the DB/index) than this buffer could lead to missed updates with this harvesting API.
   */
  private static final long TIME_BUFFER_SIZE = 3 * 60 * 1000;

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
      // Retrieve episodes from index.
      //
      // We actually fetch `limit + 1` to get some useful extra information: whether there are more
      // events and if so, what timestamp that extra event was modified at.
      final SearchQuery q = new SearchQuery()
          .withUpdatedSince(new Date(since))
          .withSort(SearchQuery.Sort.DATE_MODIFIED)
          .includeDeleted(true)
          .withLimit(limit + 1);
      final SearchResultItem[] rawEvents = searchService.getForAdministrativeRead(q).getItems();
      final boolean hasMoreEvents = rawEvents.length == limit + 1;
      logger.debug("Retrieved {} events from the index during harvest", rawEvents.length);


      // Retrieve series from DB.
      //
      // Here we optimize a bit to avoid transfering some items twice. If the events were limited by
      // `limit` (i.e. there are more than we will return), we only fetch series that were modified
      // before the modification date of the last event we will return. Consider that
      // `includesItemsUntil` can be at most that event's modification date. So if we were to now
      // return any series that are modified after that timestamp, they will be returned by the
      // next request of the client as well, since the next request's `since` parameter is the
      // `includesItemsUntil` value of the current response.
      //
      // We also fetch `limit + 1` here to be able to know whether there are more series in the
      // given time range, which allows us to figure out `hasMore` and `includesItemsUntil` more
      // precisely.
      final Optional<Date> seriesRangeEnd = hasMoreEvents
          ? Optional.of(rawEvents[rawEvents.length - 2].getModified())
          : Optional.empty();
      final List<Series> rawSeries = seriesService.getAllForAdministrativeRead(
          new Date(since),
          seriesRangeEnd,
          limit + 1
      );
      final boolean hasMoreSeriesInRange = rawSeries.size() == limit + 1;
      logger.debug("Retrieved {} series from the database during harvest", rawSeries.size());


      // Convert events and series into JSON representation. We limit both to `limit` here again,
      // because we fetched `limit + 1` above.
      final Stream<Item> eventItems = Arrays.stream(rawEvents)
          .limit(limit)
          .filter(event -> {
            // Here, we potentially filter out some events. Compare to above: when loading series
            // from the DB, we used the modified date of the last event as upper bound of the series
            // modified date. That is, IF we had more events than `limit`. The same reasoning
            // applies the other way: if we have more series than `limit`, then all events with
            // modified dates after the modified date of the last series we return will be included
            // in the next request anyway. So we might as well filter them out here to save a bit
            // on the size of this request.
            //
            // The reason we first filter series, then events, is because it is likely that an
            // Opencast instance contains way more events than series. This means that
            // `hasMoreSeriesInRange` being true is actually very uncommon.
            if (!hasMoreSeriesInRange) {
              return true;
            }

            final Date lastSeriesModifiedDate = rawSeries.get(rawSeries.size() - 2)
                .getModifiedDate();
            return !event.getModified().after(lastSeriesModifiedDate);
          })
          .map(event -> new Item(event));

      final Stream<Item> seriesItems = rawSeries.stream()
          .limit(limit)
          .map(series -> new Item(series));


      // Combine series and events into one combined list and sort it.
      //
      // The sorting is, again, not for correctness, because consumers of this API need to be
      // able to deal with that. However, sorting this here will result in fewer temporary objects
      // or invalid states in the consumer.
      final ArrayList<Item> items = Stream.concat(eventItems, seriesItems)
          .collect(Collectors.toCollection(ArrayList::new));
      items.sort(Comparator.comparing(item -> item.getModifiedDate()));


      // Obtain information to allow Tobira to plan the next harvesting request.
      //
      // The timestamp that can be used as next `since` parameter depends on whether we have more
      // items. If that's not the case, we basically just return the current timestamp (minus a
      // buffer, see docs for `TIME_BUFFER_SIZE`). If we have more items, we can use the timestamp
      // of the last item in `items`, thanks to our filtering and upper limit on modified date
      // above. However, it must also be at least `TIME_BUFFER_SIZE` in the past.
      boolean hasMore = hasMoreEvents || hasMoreSeriesInRange;
      final long timeBuffer = new Date().getTime() - TIME_BUFFER_SIZE;
      final long includesItemsUntil = hasMore
          ? Math.min(items.get(items.size() - 1).getModifiedDate().getTime(), timeBuffer)
          : timeBuffer;


      // Assembly full response.
      final List<Jsons.Val> outItems = items.stream()
          .map(item -> item.getJson())
          .collect(Collectors.toCollection(ArrayList::new));
      final Jsons.Obj json = Jsons.obj(
          Jsons.p("includesItemsUntil", includesItemsUntil),
          Jsons.p("hasMore", hasMore),
          Jsons.p("items", Jsons.arr(outItems))
      );
      logger.debug(
          "Returning {} items from harvesting (hasMore = {}, includesItemsUntil = {})",
          items.size(),
          hasMore,
          new Date(includesItemsUntil)
      );

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


  /**
   * A item of the harvesting API, basically as a JSON object. Can be "event", "series",
   * "event-deleted" or "series-deleted". Also contains the modified date, used for sorting.
   */
  private class Item {
    private Date modifiedDate;
    private Jsons.Val obj;

    /** Converts a series into the corresponding JSON representation */
    Item(SearchResultItem event) {
      this.modifiedDate = event.getModified();

      if (event.getDeletionDate() != null) {
        this.obj = Jsons.obj(
            Jsons.p("kind", "event-deleted"),
            Jsons.p("id", event.getId()),
            Jsons.p("updated", event.getModified().getTime())
        );
      } else {
        this.obj = Jsons.obj(
            Jsons.p("kind", "event"),
            Jsons.p("id", event.getId()),
            Jsons.p("title", event.getDcTitle()),
            Jsons.p("partOf", event.getDcIsPartOf()),
            Jsons.p("description", event.getDcDescription()),
            Jsons.p("updated", event.getModified().getTime())
        );
      }
    }

    /** Converts a series into the corresponding JSON representation */
    Item(Series series) {
      this.modifiedDate = series.getModifiedDate();

      if (series.isDeleted()) {
        this.obj = Jsons.obj(
          Jsons.p("kind", "series-deleted"),
          Jsons.p("id", series.getId()),
          Jsons.p("updated", series.getModifiedDate().getTime())
        );
      } else {
        this.obj = Jsons.obj(
          Jsons.p("kind", "series"),
          Jsons.p("id", series.getId()),
          Jsons.p("title", series.getDublinCore().getFirst(PROPERTY_TITLE)),
          Jsons.p("description", series.getDublinCore().getFirst(PROPERTY_DESCRIPTION)),
          Jsons.p("updated", series.getModifiedDate().getTime())
        );
      }
    }

    Date getModifiedDate() {
      return this.modifiedDate;
    }

    Jsons.Val getJson() {
      return this.obj;
    }
  }
}
