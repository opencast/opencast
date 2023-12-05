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

import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Jsons;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/** Contains the actual harvesting logic.  */
final class Harvest {
  /** Private constructor for utility class with static methods */
  private Harvest() { }

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

  private static final Logger logger = LoggerFactory.getLogger(Harvest.class);

  static Jsons.Obj harvest(
      int preferredAmount,
      Date since,
      SearchService searchService,
      SeriesService seriesService,
      Workspace workspace
  ) throws UnauthorizedException, SeriesException {
    // Retrieve episodes from index.
    //
    // We actually fetch `preferredAmount + 1` to get some useful extra information: whether there
    // are more events and if so, what timestamp that extra event was modified at.
    final var q = new SearchQuery()
        .withUpdatedSince(since)
        .withSort(SearchQuery.Sort.DATE_MODIFIED)
        .includeDeleted(true)
        .withLimit(preferredAmount + 1);
    final var rawEvents = searchService.getForAdministrativeRead(q).getItems();
    final var hasMoreEvents = rawEvents.length == preferredAmount + 1;
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
    final var rawSeries = seriesService.getAllForAdministrativeRead(
        since,
        seriesRangeEnd,
        preferredAmount + 1
    );
    final var hasMoreSeriesInRange = rawSeries.size() == preferredAmount + 1;
    logger.debug("Retrieved {} series from the database during harvest", rawSeries.size());


    // Convert events and series into JSON representation. We limit both to `preferredAmount` here
    // again, because we fetched `preferredAmount + 1` above.
    final var eventItems = Arrays.stream(rawEvents)
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

          final var lastSeriesModifiedDate = rawSeries.get(rawSeries.size() - 1).getModifiedDate();
          return !event.getModified().after(lastSeriesModifiedDate);
        })
        .map(event -> {
          try {
            return new Item(event, workspace);
          } catch (Exception e) {
            var id = event == null ? null : event.getId();
            logger.error("Error reading event '{}' (skipping...)", id, e);
            return null;
          }
        })
        .filter(item -> item != null);

    final var seriesItems = rawSeries.stream()
        .limit(preferredAmount)
        .map(series -> {
          try {
            return new Item(series);
          } catch (Exception e) {
            var id = series == null ? null : series.getId();
            logger.error("Error reading series '{}' (skipping...)", id, e);
            return null;
          }
        })
        .filter(item -> item != null);


    // Combine series and events into one combined list and sort it.
    //
    // The sorting is, again, not for correctness, because consumers of this API need to be
    // able to deal with that. However, sorting this here will result in fewer temporary objects
    // or invalid states in the consumer.
    final var items = Stream.concat(eventItems, seriesItems)
        .collect(Collectors.toCollection(ArrayList::new));
    items.sort(Comparator.comparing(item -> item.getModifiedDate()));


    // Obtain information to allow Tobira to plan the next harvesting request.
    final var hasMore = hasMoreEvents || hasMoreSeriesInRange;
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
    final var includesItemsUntil = Math.min(
        includesItemsUntilRaw.getTime(),
        new Date().getTime() - TIME_BUFFER_SIZE
    );


    // Assembly full response.
    final var outItems = items.stream()
        .map(item -> item.getJson())
        .collect(Collectors.toCollection(ArrayList::new));
    final var json = Jsons.obj(
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

    return json;
  }
}
