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
import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

/**
 * A simple tutorial class to learn about Opencast Services
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

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /** Config key for an administrator-set override of the data-completeness threshold date (an ISO date). */
  private static final String COMPLETENESS_THRESHOLD_DATE_KEY = "completeness.threshold.date";

  private LocalDate configuredCompletenessThreshold;

  /** Limits what clients can send to the client push endpoint. Until it is configured, the defaults are used. */
  private volatile ClientPushRateLimiter clientPushRateLimiter = createClientPushRateLimiter(Map.of());

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
  }

  @Modified
  void modified(Map<String, Object> properties) {
    configuredCompletenessThreshold = parseCompletenessThreshold(properties);
    clientPushRateLimiter = createClientPushRateLimiter(properties);
  }

  @Deactivate
  void deactivate() {
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
    try {
      Instant version1 = persistence.getVersion1Timestamp();
      return version1 != null ? version1.atZone(ZoneOffset.UTC).toLocalDate() : null;
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
    if (!contentType.startsWith("video/")
        && !contentType.startsWith(MimeTypes.HLS.toString())
        && !contentType.startsWith(MimeTypes.DASH.toString())) {
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

    RawEvent event = new RawEvent();
    event.setTimestamp(timestamp);
    event.setItemType(ItemType.VIDEO);
    event.setItemId(mediaPackageId);
    event.setEventType(EventType.FETCH_FILE);
    event.setEventPayload(RawEvent.serializePayload(new EventPayload.FetchFile(elementId, from, to)));

    InetAddress ip;
    String ipString;
    if (StringUtils.isNotBlank(request.getHeader(X_FORWARDED_FOR))) {
      ipString = request.getHeader(X_FORWARDED_FOR);
      ipString = ipString.split(",")[0].trim();
    } else {
      ipString = request.getRemoteAddr();
    }
    // Only parse IP addresses. Looking up host names would let clients trigger DNS lookups through the header.
    ip = InetAddresses.forString(ipString);

    // Parse user agent
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      throw new IllegalArgumentException("User Agent missing from request header " + request);
    }

    // The security context belongs to the thread of the request, so the background thread has to be given it
    Organization organization = securityService.getOrganization();
    User user = securityService.getUser();
    fileFetchExecutor.execute(() -> SecurityUtil.runAs(securityService, organization, user,
        () -> recordFileFetch(event, ip, userAgent)));
  }

  /** Runs in the background, see {@link #fileFetched} */
  private void recordFileFetch(RawEvent event, InetAddress ip, String userAgent) {
    try {
      event.setSession(generateSessionHash(secretService.getCurrentSecret(), event.getItemId(), ip, userAgent));
      create(List.of(event));
    } catch (BasicStatisticsSecretServiceException | RuntimeException | InternalError e) {
      logger.warn("Could not record file fetch event", e);
    }
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
