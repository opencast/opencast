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

import org.opencastproject.basicstatistics.EventType;
import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.basicstatistics.VideoWatchedParameters;

import com.google.gson.Gson;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Covers two main behaviors:
 * - a normal play/watch/fetch session becomes exactly one view with matching watchtime
 * - a session replaying the same item several times in a day is capped at 4 counted views.
 * Deliberately does not test implementation details like skew tolerance.
 */
public class ViewCorrelatorTest {

  private static final String ORGANIZATION = "mh_default_org";
  private static final String ITEM_ID = "item-1";
  private static final Gson gson = new Gson();
  private static final DurationLookup DURATION_LOOKUP = new DurationLookup(null, null, null);

  @Test
  public void normalSessionCountsAsOneView() {
    String session = "session-1";
    Instant play = Instant.parse("2026-01-15T10:00:00Z");

    List<RawEvent> events = new ArrayList<>();
    events.add(event(session, EventType.VIDEO_PLAY, play, null));
    events.add(event(session, EventType.FETCH_FILE, play.plusSeconds(5), null));
    // 20s claimed, 10s after play: within the 2.5x rate cap (25s), so fully counted.
    events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(10), watchedPayload(0, 20_000)));
    // 25s claimed, 25s after the previous watched event: within the rate cap (62s), so fully counted.
    events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(35), watchedPayload(20_000, 45_000)));

    HourlyBuckets result = ViewCorrelator.correlate(events, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);

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

  @Test
  public void sessionRewatchingSameDayIsCappedAtFourViews() {
    String session = "session-1";
    List<RawEvent> events = new ArrayList<>();

    // Five separate plays of the same item, same session, same day, each independently qualifying as a view.
    for (int hour = 0; hour < 5; hour++) {
      Instant play = Instant.parse(String.format("2026-01-15T%02d:00:00Z", hour));
      events.add(event(session, EventType.VIDEO_PLAY, play, null));
      events.add(event(session, EventType.FETCH_FILE, play.plusSeconds(5), null));
      events.add(event(session, EventType.VIDEO_WATCHED, play.plusSeconds(40), watchedPayload(0, 35_000)));
    }

    HourlyBuckets result = ViewCorrelator.correlate(events, ORGANIZATION, ITEM_ID, DURATION_LOOKUP);

    // Only the first four plays (chronologically) count as views ...
    assertEquals(4, Arrays.stream(result.getViews()).sum());
    assertEquals(0, result.getViews()[4]);
    // ... but watchtime keeps accumulating for the fifth, uncounted play too.
    assertEquals(35, result.getWatchtime()[4]);
    assertEquals(5 * 35L, Arrays.stream(result.getWatchtime()).sum());
  }

  private static RawEvent event(String session, EventType eventType, Instant timestamp, String payload) {
    return new RawEvent(UUID.randomUUID().toString(), ORGANIZATION, timestamp, session, ItemType.VIDEO, ITEM_ID,
        eventType, payload);
  }

  private static String watchedPayload(long fromMillis, long toMillis) {
    VideoWatchedParameters params = new VideoWatchedParameters();
    params.setFrom(fromMillis);
    params.setTo(toMillis);
    return gson.toJson(params);
  }
}
