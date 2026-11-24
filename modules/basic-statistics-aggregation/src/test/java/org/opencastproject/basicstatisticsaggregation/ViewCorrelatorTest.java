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

import static org.junit.Assert.assertEquals;

import org.opencastproject.basicstatistics.EventPayload;
import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Covers:
 * - a normal play/watch/fetch session becomes exactly one view with matching watchtime
 * - a session replaying the same item several times in a day is capped at 4 counted views
 * - a fetch preceding its play within the allowed clock skew still counts
 * - a session spanning midnight splits its view and watchtime correctly across both days.
 */
public class ViewCorrelatorTest {

  private static final String ORGANIZATION = "mh_default_org";
  private static final String ITEM_ID = "item-1";
  private static final DurationLookup DURATION_LOOKUP = new DurationLookup(null, null, null);
  private static final Instant DAY_START = Instant.parse("2026-01-15T00:00:00Z");

  @Test
  public void normalSessionCountsAsOneView() {
    long session = 1L;
    Instant play = Instant.parse("2026-01-15T10:00:00Z");

    List<RawEvent> events = new ArrayList<>();
    events.add(event(session, EventType.VIDEO_PLAY, play, null));
    events.add(event(session, EventType.FETCH_FILE, play.plusSeconds(5), null));
    // 20s claimed, 10s after play: within the 2.5x rate cap (25s), so fully counted.
    events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(10), watchedPayload(0, 20_000)));
    // 25s claimed, 25s after the previous watched event: within the rate cap (62s), so fully counted.
    events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(35), watchedPayload(20_000, 45_000)));

    HourlyBuckets result = ViewCorrelator.correlate(events, DAY_START, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);

    int[] expectedViews = new int[24];
    expectedViews[10] = 1;
    long[] expectedWatchtime = new long[24];
    expectedWatchtime[10] = 45;

    assertEquals(Arrays.stream(expectedViews).sum(), Arrays.stream(result.getViews()).sum());
    for (int hour = 0; hour < 24; hour++) {
      assertEquals("views[" + hour + "]", expectedViews[hour], result.getViews()[hour]);
      assertEquals("watchtime[" + hour + "]", expectedWatchtime[hour], result.getWatchtime()[hour]);
    }
  }

  /** A fetch may precede its play by up to ALLOWED_CLOCK_SKEW; it must still count toward that play's group. */
  @Test
  public void fetchPrecedingItsPlayWithinClockSkewStillCounts() {
    long session = 1L;
    Instant play = Instant.parse("2026-01-15T10:00:05Z");

    List<RawEvent> events = new ArrayList<>();
    events.add(event(session, EventType.FETCH_FILE, play.minusSeconds(5), null));
    events.add(event(session, EventType.VIDEO_PLAY, play, null));
    events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(40), watchedPayload(0, 35_000)));

    HourlyBuckets result = ViewCorrelator.correlate(events, DAY_START, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);

    assertEquals(1, Arrays.stream(result.getViews()).sum());
  }

  /**
   * A play just before midnight, watched into the next day: the view belongs to the play's day, and watchtime
   * splits across both days' hours, with neither day double-counting the other's slice.
   */
  @Test
  public void sessionSpanningMidnightSplitsAcrossBothDays() {
    long session = 1L;
    Instant play = Instant.parse("2026-01-15T23:58:00Z");
    Instant fetch = play.plusSeconds(2);
    // 20s before midnight, 40s after: 60s total, well past the 30s view threshold.
    Instant watchedBeforeMidnight = play.plusSeconds(20);
    Instant watchedAfterMidnight = Instant.parse("2026-01-16T00:00:40Z");

    List<RawEvent> events = new ArrayList<>();
    events.add(event(session, EventType.VIDEO_PLAY, play, null));
    events.add(event(session, EventType.FETCH_FILE, fetch, null));
    events.add(event(session, EventType.VIDEO_WATCHED, watchedBeforeMidnight, watchedPayload(0, 20_000)));
    events.add(event(session, EventType.VIDEO_WATCHED, watchedAfterMidnight, watchedPayload(20_000, 60_000)));

    HourlyBuckets day1 = ViewCorrelator.correlate(events, DAY_START, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);
    HourlyBuckets day2 = ViewCorrelator.correlate(events, DAY_START.plusSeconds(24 * 3600), ORGANIZATION, ITEM_ID,
        DURATION_LOOKUP);

    // The view belongs entirely to the play's day.
    assertEquals(1, day1.getViews()[23]);
    assertEquals(0, Arrays.stream(day2.getViews()).sum());
    // Watchtime splits: 20s in day 1's last hour, 40s in day 2's first hour.
    assertEquals(20, day1.getWatchtime()[23]);
    assertEquals(40, day2.getWatchtime()[0]);
    assertEquals(0, Arrays.stream(day1.getWatchtime()).sum() - 20);
    assertEquals(0, Arrays.stream(day2.getWatchtime()).sum() - 40);
  }

  @Test
  public void sessionRewatchingSameDayIsCappedAtFourViews() {
    long session = 1L;
    List<RawEvent> events = new ArrayList<>();

    // Five separate plays of the same item, same session, same day, each independently qualifying as a view.
    for (int hour = 0; hour < 5; hour++) {
      Instant play = Instant.parse(String.format("2026-01-15T%02d:00:00Z", hour));
      events.add(event(session, EventType.VIDEO_PLAY, play, null));
      events.add(event(session, EventType.FETCH_FILE, play.plusSeconds(5), null));
      events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(40), watchedPayload(0, 35_000)));
    }

    HourlyBuckets result = ViewCorrelator.correlate(events, DAY_START, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);

    // Only the first four plays (chronologically) count as views ...
    assertEquals(4, Arrays.stream(result.getViews()).sum());
    assertEquals(0, result.getViews()[4]);
    // ... but watchtime keeps accumulating for the fifth, uncounted play too.
    assertEquals(35, result.getWatchtime()[4]);
    assertEquals(5 * 35L, Arrays.stream(result.getWatchtime()).sum());
  }

  private static RawEvent event(long session, EventType eventType, Instant timestamp, String payload) {
    return new RawEvent(ORGANIZATION, timestamp, session, ItemType.VIDEO, ITEM_ID, eventType, payload);
  }

  private static String watchedPayload(long fromMillis, long toMillis) {
    return RawEvent.serializePayload(new EventPayload.VideoWatched(fromMillis, toMillis));
  }
}
