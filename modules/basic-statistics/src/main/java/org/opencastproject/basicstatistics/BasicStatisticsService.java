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

import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseException;
import org.opencastproject.basicstatistics.persistence.BasicStatisticsDatabaseService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretServiceException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.FileFetchListener;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.requests.SortCriterion;

import com.google.common.net.InetAddresses;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.Strings;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

/**
 * Collects raw statistics events, e.g. file fetches and client-pushed playback events, and persists them.
 */
@Component(
    property = {
        "service.description=Basic Statistics Service",
        "service.pid=org.opencastproject.basicstatistics.BasicStatisticsService"
    },
    immediate = true,
    service = { BasicStatisticsService.class, FileFetchListener.class }
)
public class BasicStatisticsService implements FileFetchListener {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsService.class);

  /** Config key for an administrator-set override of the data-completeness threshold date (an ISO date). */
  private static final String COMPLETENESS_THRESHOLD_DATE_KEY = "completeness.threshold.date";

  /** Config key for how many days of raw events to keep. 0 disables retention, keeping events forever. */
  private static final String RAW_EVENT_RETENTION_DAYS_KEY = "raw.events.retention.days";

  /** How often the raw event retention sweep runs. Cheap once the table is actually bounded by retention. */
  private static final long RETENTION_SWEEP_INTERVAL_HOURS = 1;

  private volatile LocalDate configuredCompletenessThreshold;

  /**
   * Cached result of {@link #getCompletenessThreshold()} once derived from the database, since the date of the
   * first ever recorded event cannot change once it is known.
   */
  private volatile LocalDate cachedCompletenessThreshold;

  /** Limits what clients can send to the client push endpoint. Until it is configured, the defaults are used. */
  private volatile ClientPushRateLimiter clientPushRateLimiter = createClientPushRateLimiter(Map.of());

  /**
   * How many days of raw events to keep. Also used by the REST endpoint's {@code trustedPush} to bound how old a
   * trusted push may be, since nothing older than retention could ever be aggregated before deletion.
   */
  private volatile long rawEventRetentionDays = parseLimit(Map.of(), RAW_EVENT_RETENTION_DAYS_KEY, 90);

  /** Persistent storage */
  protected BasicStatisticsDatabaseService persistence;

  /** The security service */
  protected SecurityService securityService;

  /** Daily secret service */
  protected BasicStatisticsSecretService secretService;

  /**
   * Processes file fetch events in the background, so that the file server never has to wait for the secret service
   * or the database. A single thread keeps the events in order and the load on the database low.
   */
  private volatile ThreadPoolExecutor fileFetchExecutor;

  private final AtomicLong droppedFileFetches = new AtomicLong();

  /** For throttling the warning in {@link #recordFileFetch}: a down DB or secret service must not spam the log. */
  private final AtomicLong failedFileFetches = new AtomicLong();

  /**
   * How long a run of fetches for the same (session, item, element) may go without a new, connecting byte range
   * before it is persisted as-is. Chosen to match {@code ViewCorrelator}'s own fetch/play matching tolerance, so it
   * is generous enough to bridge ordinary gaps between adaptive-streaming segment requests without inventing a
   * second magic number.
   */
  private static final long FETCH_MERGE_IDLE_SECONDS = 30;

  /**
   * Combines fetches of the same file element, by the same session, into a single raw event as long as their byte
   * ranges connect, instead of storing one event per HTTP (range) request. This is what keeps adaptive streaming
   * (which fetches one file in many small byte-range requests, one per segment) from storing one row per segment.
   * See {@link #flushMergedFetch} for how a merged run becomes a {@link RawEvent}.
   */
  private FetchRangeMerger fetchRangeMerger;

  /** Periodically deletes raw events older than {@link #rawEventRetentionDays}. */
  private ScheduledExecutorService retentionSweeper;

  /** Callback to set the basic statistics database */
  @Reference(name = "basicstatistics-persistence")
  public void setPersistence(BasicStatisticsDatabaseService persistence) {
    this.persistence = persistence;
  }

  /** OSGi callback to set the security service */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to set the secret service */
  @Reference(name = "basicstatistics-secret")
  public void setBasicStatisticsSecretService(BasicStatisticsSecretService secretService) {
    this.secretService = secretService;
  }

  @Activate
  void activate(Map<String, Object> properties) {
    logger.info("Activating Basic Statistics Service");
    modified(properties);
    // Deliberately read once here and not kept up to date by modified(): unlike the other settings, a mistake in
    // the config file must not be able to take effect immediately and silently delete raw events. Changing this
    // requires an explicit restart. Must happen before retentionSweeper exists, so the first sweep it runs below
    // never sees anything but this value.
    rawEventRetentionDays = parseLimit(properties, RAW_EVENT_RETENTION_DAYS_KEY, 90);
    // Up to 10000 events can wait to be processed, further events are dropped
    fileFetchExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(10_000),
        runnable -> {
          Thread thread = new Thread(runnable, "basic-statistics-file-fetch");
          thread.setDaemon(true);
          return thread;
        },
        (runnable, executor) -> {
          // Never block the caller. Losing an event is better than slowing down the file server.
          long dropped = droppedFileFetches.incrementAndGet();
          if (dropped == 1 || dropped % 1000 == 0) {
            logger.warn("File fetch events are not processed fast enough, dropped {} so far", dropped);
          }
        });

    fetchRangeMerger = new FetchRangeMerger(fileFetchExecutor, this::flushMergedFetch, FETCH_MERGE_IDLE_SECONDS);

    retentionSweeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "basic-statistics-retention-sweep");
      thread.setDaemon(true);
      return thread;
    });
    retentionSweeper.scheduleWithFixedDelay(this::sweepRawEventRetention, 0, RETENTION_SWEEP_INTERVAL_HOURS,
        TimeUnit.HOURS);
  }

  @Modified
  void modified(Map<String, Object> properties) {
    configuredCompletenessThreshold = parseCompletenessThreshold(properties);
    clientPushRateLimiter = createClientPushRateLimiter(properties);
  }

  @Deactivate
  void deactivate() {
    retentionSweeper.shutdownNow();
    // Flush whatever is still merged in memory before shutting down; stop() queues that onto fileFetchExecutor
    // ahead of the shutdown() below, so it still runs during the graceful drain.
    fetchRangeMerger.stop();
    fileFetchExecutor.shutdown();
    try {
      // Give the pending events a few seconds to be processed
      if (!fileFetchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.warn("Dropping {} file fetch events on shutdown", fileFetchExecutor.shutdownNow().size());
      }
    } catch (InterruptedException e) {
      fileFetchExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public ClientPushRateLimiter getClientPushRateLimiter() {
    return clientPushRateLimiter;
  }

  public long getRawEventRetentionDays() {
    return rawEventRetentionDays;
  }

  /** Deletes raw events older than {@link #rawEventRetentionDays}, if retention is enabled. Runs periodically. */
  private void sweepRawEventRetention() {
    long retentionDays = rawEventRetentionDays;
    if (retentionDays <= 0) {
      return;
    }
    // Day-aligned, so this only ever deletes whole days: aggregation always recomputes a full day at a time, and
    // must never see one with some but not all of its raw events.
    Instant cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(retentionDays).atStartOfDay(ZoneOffset.UTC).toInstant();
    try {
      long deleted = persistence.deleteRawEventsBefore(cutoff);
      if (deleted > 0) {
        logger.info("Deleted {} raw events older than {}", deleted, cutoff);
      }
    } catch (BasicStatisticsDatabaseException e) {
      logger.warn("Could not delete raw events older than {}", cutoff, e);
    }
  }

  private ClientPushRateLimiter createClientPushRateLimiter(Map<String, Object> properties) {
    // By default: 8 KiB per request, 512 KiB per minute and 8 MiB per hour
    return new ClientPushRateLimiter(
        parseLimit(properties, "ratelimit.request.bytes", 8 * 1024),
        parseLimit(properties, "ratelimit.minute.bytes", 512 * 1024),
        parseLimit(properties, "ratelimit.hour.bytes", 8 * 1024 * 1024));
  }

  private long parseLimit(Map<String, Object> properties, String key, long defaultValue) {
    Object value = properties.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      long limit = Long.parseLong(value.toString().trim());
      if (limit >= 0) {
        return limit;
      }
    } catch (NumberFormatException e) {
      // Handled like a negative number
    }
    logger.warn("Invalid {} '{}', using {}", key, value, defaultValue);
    return defaultValue;
  }

  private LocalDate parseCompletenessThreshold(Map<String, Object> properties) {
    Object value = properties.get(COMPLETENESS_THRESHOLD_DATE_KEY);
    if (value == null) {
      return null;
    }
    try {
      return LocalDate.parse(value.toString());
    } catch (DateTimeParseException e) {
      logger.warn("Invalid {} '{}', ignoring", COMPLETENESS_THRESHOLD_DATE_KEY, value);
      return null;
    }
  }

  /**
   * The date before which statistical data may be incomplete or missing entirely
   * — either an administrator-configured override, or, by default, when statistics tracking first started
   * recording any data at all. {@code null} if neither is available yet.
   */
  public LocalDate getCompletenessThreshold() {
    if (configuredCompletenessThreshold != null) {
      return configuredCompletenessThreshold;
    }
    if (cachedCompletenessThreshold != null) {
      return cachedCompletenessThreshold;
    }
    try {
      Instant version1 = persistence.getVersion1Timestamp();
      if (version1 == null) {
        return null;
      }
      cachedCompletenessThreshold = version1.atZone(ZoneOffset.UTC).toLocalDate();
      return cachedCompletenessThreshold;
    } catch (BasicStatisticsDatabaseException e) {
      logger.warn("Could not determine the statistics completeness threshold", e);
      return null;
    }
  }

  /**
   * Get multiple raw events from the database
   * This is purely for testing purposes at the moment
   * @param limit The maximum amount of raw events to get with one request.
   * @param offset The index of the first result to return.
   * @return A list of {@link RawEvent}s
   * @throws IllegalStateException If something went wrong in the database service
   */
  public List<RawEvent> getRawEvents(int limit, int offset) throws IllegalStateException {
    return getRawEvents(limit, offset, new SortCriterion("", SortCriterion.Order.None));
  }

  public List<RawEvent> getRawEvents(int limit, int offset, SortCriterion sortCriterion)
          throws IllegalStateException {
    try {
      String organizationId = securityService.getOrganization().getId();
      List<RawEvent> events = persistence.getRawEvents(organizationId, limit, offset, sortCriterion);
      return events;
    } catch (BasicStatisticsDatabaseException e) {
      throw new IllegalStateException("Could not get raw events from database", e);
    }
  }

  /**
   * Persist one or multiple events in the database
   * @param events The events to persist
   */
  public void create(List<RawEvent> events) {
    for (RawEvent event : events) {
      if (event.getSession() == null || event.getTimestamp() == null || event.getItemId() == null
          || event.getItemType() == null || event.getEventType() == null) {
        throw new IllegalStateException("Required field missing in event " + event);
      }
      event.setOrganization(securityService.getOrganization().getId());
    }

    try {
      persistence.createRawEvents(events);
    } catch (BasicStatisticsDatabaseException e) {
      throw new IllegalStateException("Could not persist raw events", e);
    }
  }

  /**
   * Record that a file was fetched, e.g. by a media player. Only video files are recorded.
   * <p>
   * This only does what is cheap on the calling thread. The event is then processed in the background, and dropped if
   * too many events are waiting.
   *
   * @throws IllegalArgumentException if the client IP or the user agent of the request are missing or invalid
   */
  @Override
  public void fileFetched(String path, String contentType, Instant timestamp, long from, Long to,
          HttpServletRequest request) {
    // We only care about request that fetch "video" files for now
    // Media types are case-insensitive (RFC 2045/7231), and MimeTypes.xml declares HLS as "application/x-mpegURL"
    // while the MimeTypes.HLS constant uses "application/X-mpegURL", so a case-sensitive comparison here would miss
    // HLS playlist requests entirely.
    if (!Strings.CI.startsWith(contentType, "video/")
        && !Strings.CI.startsWith(contentType, MimeTypes.HLS.toString())
        && !Strings.CI.startsWith(contentType, MimeTypes.DASH.toString())) {
      return;
    }

    String[] parts = path.split("/");

    // TODO: Find a less brittle way to determine these
    if (parts.length < 5) {
      logger.warn("Cannot determine media package and element id from path '{}', skipping statistics", path);
      return;
    }
    String mediaPackageId = parts[3];
    String elementId = parts[4];

    // Behind a trusted reverse proxy, Opencast's own request filter already validates X-Forwarded-For and
    // rewrites getRemoteAddr() to the real client IP; the header itself is left untouched, so reading it here
    // directly would use an unvalidated, client-controlled value instead.
    // Only parse IP addresses. Looking up host names would let clients trigger DNS lookups through the header.
    InetAddress ip = InetAddresses.forString(request.getRemoteAddr());

    // Parse user agent
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      throw new IllegalArgumentException("User Agent missing from request header " + request);
    }

    // The security context belongs to the thread of the request, so the background thread has to be given it.
    // The user's identity is never actually read downstream (create() only needs the organization), so an
    // anonymous placeholder is used instead of holding onto the real user, which can otherwise sit in
    // FetchRangeMerger's pending ranges for a while.
    Organization organization = securityService.getOrganization();
    User user = SecurityUtil.createAnonymousUser(organization);
    fileFetchExecutor.execute(() -> SecurityUtil.runAs(securityService, organization, user,
        () -> recordFileFetch(mediaPackageId, elementId, timestamp, from, to, ip, userAgent, organization, user)));
  }

  /** Runs in the background, see {@link #fileFetched}. Hands the fetch to {@link #fetchRangeMerger}. */
  private void recordFileFetch(String itemId, String elementId, Instant timestamp, long from, Long to,
          InetAddress ip, String userAgent, Organization organization, User user) {
    try {
      long session = generateSessionHash(secretService.getCurrentSecret(), itemId, ip, userAgent);
      FetchRangeMerger.Key key = new FetchRangeMerger.Key(session, itemId, elementId);
      fetchRangeMerger.record(key, timestamp, from, to, organization, user);
    } catch (BasicStatisticsSecretServiceException | RuntimeException | InternalError e) {
      // A down DB or secret service would otherwise log this, with a stacktrace, for every single file fetch.
      long failed = failedFileFetches.incrementAndGet();
      if (failed == 1 || failed % 1000 == 0) {
        logger.warn("Could not record file fetch event, {} have failed so far", failed, e);
      }
    }
  }

  /**
   * Turns one merged run of fetches (see {@link #fetchRangeMerger}) into a {@link RawEvent} and persists it.
   * Only ever called on {@link #fileFetchExecutor}.
   */
  private void flushMergedFetch(FetchRangeMerger.Key key, Instant timestamp, long from, Long to,
          Organization organization, User user) {
    RawEvent event = new RawEvent();
    event.setTimestamp(timestamp);
    event.setSession(key.session());
    event.setItemType(ItemType.VIDEO);
    event.setItemId(key.itemId());
    event.setEventType(EventType.FETCH_FILE);
    event.setEventPayload(RawEvent.serializePayload(new EventPayload.FetchFile(key.elementId(), from, to)));
    SecurityUtil.runAs(securityService, organization, user, () -> create(List.of(event)));
  }

  /**
   * The session hash is defined as the first 8 bytes of HMAC_SHA256(dailySecret, itemId || IP || UA).
   * The hash is truncated to save space. 64 bits are plenty, since only sessions within one day can collide,
   * and a collision merely makes two sessions count as one.
   * @param dailySecret is a random secret, rotated/regenerated daily
   * @param itemId is the ID of the item the event involves (e.g. video UUID)
   * @param ip is the IP address of the user
   * @param userAgent is the user agent string of the user
   * @return the session hash
   */
  public long generateSessionHash(
      byte[] dailySecret,
      String itemId,
      InetAddress ip,
      String userAgent) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    message.writeBytes(itemId.getBytes(StandardCharsets.UTF_8));
    message.writeBytes(ip.getAddress());          // raw IPv4/IPv6 bytes
    message.writeBytes(userAgent.getBytes(StandardCharsets.UTF_8));

    byte[] hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, dailySecret).hmac(message.toByteArray());
    return ByteBuffer.wrap(hmac).getLong();
  }
}
