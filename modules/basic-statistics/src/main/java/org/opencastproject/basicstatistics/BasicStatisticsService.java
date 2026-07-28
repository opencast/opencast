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
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    service = BasicStatisticsService.class
)
public class BasicStatisticsService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsService.class);

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  /** Persistent storage */
  protected BasicStatisticsDatabaseService persistence;

  /** The security service */
  protected SecurityService securityService;

  /** Daily secret service */
  protected BasicStatisticsSecretService secretService;

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
  @Modified
  void activate(Map<String, Object> properties) {
    logger.info("Activating Basic Statistics Service");
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
      List<RawEvent> events = persistence.getRawEvents(limit, offset, sortCriterion);
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
      throw new IllegalStateException();
    }
  }

  /**
   *
   * @param event
   * @param request
   * @throws UnknownHostException
   */
  public void recordFileFetched(RawEvent event, HttpServletRequest request)
          throws UnknownHostException {
    InetAddress ip;
    String ipString;
    if (StringUtils.isNotBlank(request.getHeader(X_FORWARDED_FOR))) {
      ipString = request.getHeader(X_FORWARDED_FOR);
      ipString = ipString.split(",")[0].trim();
    } else {
      ipString = request.getRemoteAddr();
    }
    ip = InetAddress.getByName(ipString);

    // Parse user agent
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      throw new IllegalArgumentException("User Agent missing from request header " + request);
    }

    try {
      event.setSession(generateSessionHash(secretService.getCurrentSecret(), event.getItemId(), ip, userAgent));
    } catch (BasicStatisticsSecretServiceException e) {
      throw new InternalError("Error in the secret service", e);
    }

    List events = new ArrayList();
    events.add(event);
    create(events);
  }

  /**
   * The session hash is defined as HMAC_SHA256(daily_secret, item_id || IP || UA) where:
   * @param dailySecret is a random secret, rotated/regenerated daily
   * @param itemId is the ID of the item the event involves (e.g. video UUID)
   * @param ip is the IP address of the user
   * @param userAgent is the user agent string of the user
   * @return the session hash
   */
  public String generateSessionHash(
      byte[] dailySecret,
      String itemId,
      InetAddress ip,
      String userAgent) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    message.writeBytes(itemId.getBytes(StandardCharsets.UTF_8));
    message.writeBytes(ip.getAddress());          // raw IPv4/IPv6 bytes
    message.writeBytes(userAgent.getBytes(StandardCharsets.UTF_8));

    return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, dailySecret)
        .hmacHex(message.toByteArray());
  }
}
