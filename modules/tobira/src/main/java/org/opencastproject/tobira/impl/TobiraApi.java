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

import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
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
      // Retrieve episodes from index.
      //
      // We actually fetch `preferredAmount + 1` to get some useful extra information: whether there
      // are more events and if so, what timestamp that extra event was modified at.
      final SearchQuery q = new SearchQuery()
          .withUpdatedSince(new Date(since))
          .withSort(SearchQuery.Sort.DATE_MODIFIED)
          .includeDeleted(true)
          .withLimit(preferredAmount + 1);
      final SearchResultItem[] rawEvents = searchService.getForAdministrativeRead(q).getItems();
      final boolean hasMoreEvents = rawEvents.length == preferredAmount + 1;
      logger.debug("Retrieved {} events from the index during harvest", rawEvents.length);


      // Retrieve series from DB.
      //
      // Here we optimize a bit to avoid transfering some items twice. If the events were limited by
      // `preferredAmount` (i.e. there are more than we will return), we only fetch series that
      // were modified before the modification date of the last event we will return. Consider that
      // `includesItemsUntil` can be at most that event's modification date. So if we were to now
      // return any series that are modified after that timestamp, they will be returned by the
      // next request of the client as well, since the next request's `since` parameter is the
      // `includesItemsUntil` value of the current response.
      //
      // We also fetch `preferredAmount + 1` here to be able to know whether there are more series
      // in the given time range, which allows us to figure out `hasMore` and `includesItemsUntil`
      // more precisely.
      final Optional<Date> seriesRangeEnd = hasMoreEvents
          ? Optional.of(rawEvents[rawEvents.length - 1].getModified())
          : Optional.empty();
      final List<Series> rawSeries = seriesService.getAllForAdministrativeRead(
          new Date(since),
          seriesRangeEnd,
          preferredAmount + 1
      );
      final boolean hasMoreSeriesInRange = rawSeries.size() == preferredAmount + 1;
      logger.debug("Retrieved {} series from the database during harvest", rawSeries.size());


      // Convert events and series into JSON representation. We limit both to `preferredAmount` here
      // again, because we fetched `preferredAmount + 1` above.
      final Stream<Item> eventItems = Arrays.stream(rawEvents)
          .limit(preferredAmount)
          .filter(event -> {
            // Here, we potentially filter out some events. Compare to above: when loading series
            // from the DB, we used the modified date of the last event as upper bound of the
            // series modified date. That is, IF we had more events than `preferredAmount`. The
            // same reasoning applies the other way: if we have more series than `preferredAmount`,
            // then all events with modified dates after the modified date of the last series we
            // return will be included in the next request anyway. So we might as well filter them
            // out here to save a bit on the size of this request.
            //
            // The reason we first filter series, then events, is because it is likely that an
            // Opencast instance contains way more events than series. This means that
            // `hasMoreSeriesInRange` being true is actually very uncommon.
            if (!hasMoreSeriesInRange) {
              return true;
            }

            final Date lastSeriesModifiedDate = rawSeries.get(rawSeries.size() - 1)
                .getModifiedDate();
            return !event.getModified().after(lastSeriesModifiedDate);
          })
          .map(event -> new Item(event));

      final Stream<Item> seriesItems = rawSeries.stream()
          .limit(preferredAmount)
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
      boolean hasMore = hasMoreEvents || hasMoreSeriesInRange;
      final Date includesItemsUntilRaw;
      if (!hasMoreEvents && !hasMoreSeriesInRange) {
        // All events and series up to now have been harvested.
        includesItemsUntilRaw = new Date();
      } else if (!hasMoreEvents && hasMoreSeriesInRange) {
        // `rawEvents` contains all events that currently exist. Our response won't contain the ones
        // that have a modified date after the one of the last raw series. But that means that we
        // will return all series and events until the last raw series.
        includesItemsUntilRaw = rawSeries.get(rawSeries.size() - 1).getModifiedDate();
      } else if (hasMoreEvents && !hasMoreSeriesInRange) {
        // There are more events, but no additional series in the range from `since` to the modified
        // date of the last raw event. So we know there are no other events or series before the
        // last raw events.
        includesItemsUntilRaw = rawEvents[rawEvents.length - 1].getModified();
      } else {
        // There are more events and more series in the given range. In theory, this would be
        // `Math.min()` of the last raw event and last raw series. However, since `hasMoreEvents`
        // is set, we only loaded series with modified dates smaller than the last raw event's
        // modified date. That means, that the modified date of the last raw series is always
        // smaller than that of the last raw event.
        includesItemsUntilRaw = rawSeries.get(rawSeries.size() - 1).getModifiedDate();
      }

      // The `includesItemsUntil` we return has to be at least `TIME_BUFFER_SIZE` in the past. See
      // the constant's documentation for more information on that.
      final long includesItemsUntil = Math.min(
          includesItemsUntilRaw.getTime(),
          new Date().getTime() - TIME_BUFFER_SIZE
      );


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
        final List<Jsons.Val> tracks = Arrays.stream(event.getMediaPackage().getTracks())
            .map(track -> {
              VideoStream[] videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
              Jsons.Val resolution = null;
              if (videoStreams.length > 0) {
                final VideoStream stream = videoStreams[0];
                resolution = Jsons.arr(Jsons.v(stream.getFrameWidth()), Jsons.v(stream.getFrameHeight()));

                if (videoStreams.length > 1) {
                  logger.warn(
                      "Track of event {} has more than one video stream; we will ignore all but the first",
                      event.getId()
                  );
                }
              }

              return Jsons.obj(
                  Jsons.p("uri", track.getURI().toString()),
                  Jsons.p("mimetype", track.getMimeType().toString()),
                  Jsons.p("flavor", track.getFlavor().toString()),
                  Jsons.p("resolution", resolution)
              );
            })
            .collect(Collectors.toCollection(ArrayList::new));

        this.obj = Jsons.obj(
            Jsons.p("kind", "event"),
            Jsons.p("id", event.getId()),
            Jsons.p("title", event.getDcTitle()),
            Jsons.p("partOf", event.getDcIsPartOf()),
            Jsons.p("description", event.getDcDescription()),
            Jsons.p("created", event.getDcCreated().getTime()),
            Jsons.p("creator", event.getDcCreator()),
            Jsons.p("duration", event.getDcExtent() < 0 ? null : event.getDcExtent()),
            Jsons.p("tracks", Jsons.arr(tracks)),
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
