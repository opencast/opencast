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
package org.opencastproject.basicstatisticsaggregation;

import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatistics.VideoWatchedParameters;

import com.google.gson.Gson;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Turns one item's raw events for a single day into hourly view/watchtime buckets.
 *
 * A view is a video:play event, video:watched events summing to at least
 * {@code min(30s, 70% of video length)}, and at least one file-fetch event, all sharing the same session
 * hash. A video:watched event is attributed to the closest preceding video:play event; a
 * file-fetch event may match a video:play up to 30s in the future, to tolerate clock skew. A
 * session is capped at 4 counted views per item per day.
 */
public final class ViewCorrelator {

  /** Minimum watch time (seconds) required for a view, regardless of video length. */
  static final long MIN_VIEW_SECONDS = 30;

  /** Fraction of the video's total length that alone is enough to count as a view. */
  static final double VIEW_LENGTH_FRACTION = 0.7;

  /** How far a file-fetch event may precede its matching play event and still count (clock skew tolerance). */
  static final Duration ALLOWED_CLOCK_SKEW = Duration.ofSeconds(30);

  /** Limits how much watch time a single "video:watched" event may contribute, relative to real elapsed time. */
  static final double WATCHED_EVENT_RATE_CAP = 2.5;

  /** Maximum number of views a single session can generate for one item per day. */
  static final int MAX_VIEWS_PER_SESSION_PER_DAY = 4;

  private static final Gson gson = new Gson();

  private ViewCorrelator() {
  }

  public static HourlyBuckets correlate(List<RawEvent> dayEvents, String organization, String itemId,
      DurationLookup durationLookup) {
    int[] views = new int[24];
    long[] watchtime = new long[24];

    long viewThreshold = durationLookup.getDurationSeconds(organization, itemId)
        .map(duration -> Math.min(MIN_VIEW_SECONDS, (long) (duration * VIEW_LENGTH_FRACTION)))
        .orElse(MIN_VIEW_SECONDS);

    Map<String, List<RawEvent>> bySession = dayEvents.stream()
        .collect(Collectors.groupingBy(RawEvent::getSession));

    for (List<RawEvent> sessionEvents : bySession.values()) {
      correlateSession(sessionEvents, viewThreshold, views, watchtime);
    }

    return new HourlyBuckets(views, watchtime);
  }

  private static void correlateSession(List<RawEvent> sessionEvents, long viewThreshold, int[] views,
      long[] watchtime) {
    // Order ascending by playTimestamp, since events are processed in chronological order.
    List<RawEvent> sorted = sessionEvents.stream()
        .sorted(Comparator.comparing(RawEvent::getTimestamp))
        .collect(Collectors.toList());

    List<PlayGroup> groups = new ArrayList<>();

    for (RawEvent event : sorted) {
      if (event.getEventType() == EventType.VIDEO_PLAY) {
        groups.add(new PlayGroup(event.getTimestamp()));
      } else if (event.getEventType() == EventType.VIDEO_WATCHED) {
        findClosestGroupAtOrBefore(groups, event.getTimestamp())
            .ifPresent(group -> applyWatched(group, event));
      } else if (event.getEventType() == EventType.FETCH_FILE) {
        findClosestGroupAtOrBefore(groups, event.getTimestamp().plus(ALLOWED_CLOCK_SKEW))
            .ifPresent(group -> group.hasFetch = true);
      }
    }

    int countedViews = 0;
    for (PlayGroup group : groups) {
      boolean qualifiesAsView = group.hasFetch && group.watchedSeconds >= viewThreshold;
      if (qualifiesAsView && countedViews < MAX_VIEWS_PER_SESSION_PER_DAY) {
        views[hourOf(group.playTimestamp)]++;
        countedViews++;
      }
      // Watchtime counts for every play/fetch-associated group, independent of the per-day view cap.
      if (group.hasFetch) {
        for (int hour = 0; hour < 24; hour++) {
          watchtime[hour] += group.watchtimeByHour[hour];
        }
      }
    }
  }

  private static void applyWatched(PlayGroup group, RawEvent event) {
    VideoWatchedParameters payload = gson.fromJson(event.getEventPayload(), VideoWatchedParameters.class);
    // from/to are video_timestamps: milliseconds since the video's start, not seconds.
    long claimedSeconds = Math.max(0, (payload.getTo() - payload.getFrom()) / 1000);

    Instant since = group.lastWatchedTimestamp != null ? group.lastWatchedTimestamp : group.playTimestamp;
    long elapsedSeconds = Math.max(0, Duration.between(since, event.getTimestamp()).getSeconds());
    long cappedSeconds = Math.min(claimedSeconds, (long) (elapsedSeconds * WATCHED_EVENT_RATE_CAP));

    group.watchedSeconds += cappedSeconds;
    group.watchtimeByHour[hourOf(event.getTimestamp())] += cappedSeconds;
    group.lastWatchedTimestamp = event.getTimestamp();
  }

  /**
   * The play group with the largest {@code playTimestamp <= time}, i.e. the closest preceding play. {@code groups}
   * must be sorted ascending by {@code playTimestamp}.
   */
  private static Optional<PlayGroup> findClosestGroupAtOrBefore(List<PlayGroup> groups, Instant time) {
    PlayGroup match = null;
    for (PlayGroup group : groups) {
      if (!group.playTimestamp.isAfter(time)) {
        match = group;
      } else {
        break;
      }
    }
    return Optional.ofNullable(match);
  }

  private static int hourOf(Instant timestamp) {
    return timestamp.atZone(ZoneOffset.UTC).getHour();
  }

  private static final class PlayGroup {
    private final Instant playTimestamp;
    private long watchedSeconds;
    private Instant lastWatchedTimestamp;
    private boolean hasFetch;
    private final long[] watchtimeByHour = new long[24];

    private PlayGroup(Instant playTimestamp) {
      this.playTimestamp = playTimestamp;
    }
  }
}
