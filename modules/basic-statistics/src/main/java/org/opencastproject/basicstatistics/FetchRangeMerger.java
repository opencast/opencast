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

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Merges connecting (overlapping or directly adjacent) byte-range fetches of the same file, by the same session,
 * into a single reported fetch instead of one per HTTP (range) request.
 *
 * This is what keeps adaptive streaming (which fetches one file in many small byte-range requests, one per
 * segment) from reporting one fetch per segment: segments of the same rendition are consecutive, non-overlapping
 * byte ranges of one underlying file, so a run of them collapses into one report.
 *
 * A run is reported to the {@link FlushListener} once a gap appears in it (e.g. from a seek) or it has gone idle
 * for {@code idleSeconds}.
 *
 * {@link #record} and the periodic idle check both run on the single {@code executor} passed to the constructor.
 * That is not for data-structure safety (the underlying cache is itself safe for concurrent access) but to
 * preserve the order fetches actually arrived in, which decides which one is "first" for a merged run's
 * timestamp; the caller must only ever call {@link #record} from that same executor.
 */
public class FetchRangeMerger {

  /** Bounds memory use. Each entry is one (session, item, element) with an in-progress run of byte-range fetches. */
  private static final long MAX_PENDING = 10_000;

  /** How often pending runs are checked for having gone idle. */
  private static final long SWEEP_INTERVAL_SECONDS = 10;

  /** Identifies one in-progress run of connecting byte-range fetches: a file element, fetched within one session. */
  public record Key(long session, String itemId, String elementId) {
  }

  /** Reports one run of connecting fetches once it is done. */
  public interface FlushListener {
    /**
     * @param timestamp of the first fetch in the run
     * @param from start of the merged byte range
     * @param to end of the merged byte range, or null if any fetch in the run had no specified end
     */
    void flush(Key key, Instant timestamp, long from, Long to, Organization organization, User user);
  }

  private final Executor executor;
  private final FlushListener listener;
  private final Cache<Key, Range> pending;
  private final ScheduledExecutorService sweeper;

  public FetchRangeMerger(Executor executor, FlushListener listener, long idleSeconds) {
    this.executor = executor;
    this.listener = listener;
    this.pending = CacheBuilder.newBuilder()
        .maximumSize(MAX_PENDING)
        .expireAfterWrite(idleSeconds, TimeUnit.SECONDS)
        .removalListener(notification -> {
          // REPLACED means a merge/gap was already handled (and flushed, if needed) inside record() itself.
          // Anything else (expired, evicted for size, or explicitly invalidated by stop()) still needs flushing.
          if (notification.getCause() != RemovalCause.REPLACED) {
            flush((Key) notification.getKey(), (Range) notification.getValue());
          }
        })
        .build();
    // Guava only expires entries while the cache is being read or written, so idle runs need a periodic nudge.
    this.sweeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "fetch-range-merger-sweep");
      thread.setDaemon(true);
      return thread;
    });
    sweeper.scheduleWithFixedDelay(() -> executor.execute(pending::cleanUp),
        SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  /** Flushes every run still held, then stops the periodic idle check. Call on shutdown. */
  public void stop() {
    sweeper.shutdownNow();
    executor.execute(pending::invalidateAll);
  }

  /** Records one fetch. Must be called on the {@code executor} given to the constructor. */
  public void record(Key key, Instant timestamp, long from, Long to, Organization organization, User user) {
    Range incoming = new Range(timestamp, from, to, organization, user);
    pending.asMap().merge(key, incoming, (held, newRange) -> {
      if (connects(held.from, held.to, newRange.from, newRange.to)) {
        held.from = Math.min(held.from, newRange.from);
        held.to = (held.to == null || newRange.to == null) ? null : Math.max(held.to, newRange.to);
        return held;
      }
      // A real gap, e.g. from a seek: the run that was held so far is done.
      flush(key, held);
      return newRange;
    });
  }

  /** Whether two byte ranges overlap or are directly adjacent, i.e. can be represented as a single fetch. */
  private static boolean connects(long aFrom, Long aTo, long bFrom, Long bTo) {
    if (aTo != null && aTo + 1 < bFrom) {
      return false;
    }
    if (bTo != null && bTo + 1 < aFrom) {
      return false;
    }
    return true;
  }

  private void flush(Key key, Range range) {
    listener.flush(key, range.timestamp, range.from, range.to, range.organization, range.user);
  }

  private static final class Range {
    private final Instant timestamp;
    private long from;
    private Long to;
    private final Organization organization;
    private final User user;

    Range(Instant timestamp, long from, Long to, Organization organization, User user) {
      this.timestamp = timestamp;
      this.from = from;
      this.to = to;
      this.organization = organization;
      this.user = user;
    }
  }
}
