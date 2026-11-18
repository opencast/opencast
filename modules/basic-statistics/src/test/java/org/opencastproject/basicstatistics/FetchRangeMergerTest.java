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

package org.opencastproject.basicstatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the byte-range merging algorithm in isolation, without any of {@link BasicStatisticsService}'s security
 * context or persistence plumbing.
 */
public class FetchRangeMergerTest {

  private static final FetchRangeMerger.Key KEY = new FetchRangeMerger.Key(1, "item", "element");

  /** One run as reported by a {@link FetchRangeMerger.FlushListener}. */
  private record Flushed(FetchRangeMerger.Key key, Instant timestamp, long from, Long to) {
  }

  /** Runs the merger's housekeeping synchronously on the calling thread, which is all these tests need. */
  private static FetchRangeMerger newMerger(List<Flushed> flushed) {
    return new FetchRangeMerger(Runnable::run,
        (key, timestamp, from, to, organization, user) -> flushed.add(new Flushed(key, timestamp, from, to)),
        30);
  }

  /**
   * Adaptive streaming fetches one file through many small, consecutive byte-range requests (one per segment).
   * Reporting one fetch per request would make the raw event table dominated by these, so connecting ranges are
   * merged into a single fetch instead.
   */
  @Test
  public void testConnectingRangesAreMergedIntoOne() {
    List<Flushed> flushed = new ArrayList<>();
    FetchRangeMerger merger = newMerger(flushed);
    Instant first = Instant.parse("2026-10-20T10:00:00Z");
    Instant second = Instant.parse("2026-10-20T10:00:06Z");

    merger.record(KEY, first, 0, 99L, null, null);
    // directly adjoining the previous range, as consecutive HLS/DASH segments of the same file would be
    merger.record(KEY, second, 100, 199L, null, null);
    merger.stop();

    assertEquals(1, flushed.size());
    assertEquals(0L, flushed.get(0).from());
    assertEquals(199L, flushed.get(0).to().longValue());
    // the documented semantics: the timestamp of a merged fetch-file event is that of its first request
    assertEquals(first, flushed.get(0).timestamp());
  }

  /** A gap between two byte ranges (e.g. a seek) must not be merged away, each is its own fetch. */
  @Test
  public void testGapBetweenRangesIsNotMerged() {
    List<Flushed> flushed = new ArrayList<>();
    FetchRangeMerger merger = newMerger(flushed);
    Instant first = Instant.parse("2026-10-20T10:00:00Z");
    Instant second = Instant.parse("2026-10-20T10:00:30Z");

    merger.record(KEY, first, 0, 99L, null, null);
    merger.record(KEY, second, 500, 599L, null, null);
    merger.stop();

    assertEquals(2, flushed.size());
    assertEquals(0L, flushed.get(0).from());
    assertEquals(99L, flushed.get(0).to().longValue());
    assertEquals(500L, flushed.get(1).from());
    assertEquals(599L, flushed.get(1).to().longValue());
  }

  /** Fetches of different sessions must never be merged into one, even for the same file and byte ranges. */
  @Test
  public void testDifferentSessionsAreNotMerged() {
    List<Flushed> flushed = new ArrayList<>();
    FetchRangeMerger merger = newMerger(flushed);
    Instant timestamp = Instant.parse("2026-10-20T10:00:00Z");

    merger.record(new FetchRangeMerger.Key(1, "item", "element"), timestamp, 0, 99L, null, null);
    merger.record(new FetchRangeMerger.Key(2, "item", "element"), timestamp, 100, 199L, null, null);
    merger.stop();

    assertEquals(2, flushed.size());
  }

  /**
   * A fetch without a specified end means "to the end of the file", which is implicitly larger than any explicit
   * end a previous fetch in the same run may have had, and must not be narrowed by merging.
   */
  @Test
  public void testOpenEndedRangeDominatesMerge() {
    List<Flushed> flushed = new ArrayList<>();
    FetchRangeMerger merger = newMerger(flushed);
    Instant first = Instant.parse("2026-10-20T10:00:00Z");
    Instant second = Instant.parse("2026-10-20T10:00:06Z");

    merger.record(KEY, first, 0, 99L, null, null);
    merger.record(KEY, second, 100, null, null, null);
    merger.stop();

    assertEquals(1, flushed.size());
    assertEquals(0L, flushed.get(0).from());
    assertNull(flushed.get(0).to());
  }
}
