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

package org.opencastproject.basicstatisticssecret.remote;

import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretServiceException;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.UrlSupport;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a remote basic statistics secret service that gets the daily secret from the secret service implementation
 * on a remote host. The secret is cached for a while.
 */
@Component(
    immediate = true,
    service = BasicStatisticsSecretService.class,
    property = {
        "service.description=Basic Statistics Internal Service Proxy"
    }
)
public class BasicStatisticsSecretRemote extends RemoteBase implements BasicStatisticsSecretService {

  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsSecretRemote.class);

  /** The default constructor. */
  public BasicStatisticsSecretRemote() {
    super(JOB_TYPE);
  }

  private static final long CACHE_SECONDS = 600;
  /** Connection and socket timeout used to refresh the secret. Requests that serve clients wait on this. */
  private static final int REQUEST_TIMEOUT_MILLIS = 5000;

  /** A secret, and the time before it was fetched */
  private record CachedSecret(byte[] secret, Instant fetched) { }

  private volatile CachedSecret cached = null;
  /** Ensures only one thread at a time refreshes the secret over the network; the others wait for it instead of
   *  each blocking a request thread on their own HTTP call. */
  private final ReentrantLock refreshLock = new ReentrantLock();

  @Override
  public byte[] getCurrentSecret() throws BasicStatisticsSecretServiceException {
    Instant now = Instant.now();
    CachedSecret cache = cached;
    if (isUsable(cache, now)) {
      return cache.secret();
    }

    refreshLock.lock();
    try {
      // The cache may have just been refreshed by whoever held the lock before us
      cache = cached;
      return isUsable(cache, now) ? cache.secret() : refreshSecret(now);
    } finally {
      refreshLock.unlock();
    }
  }

  /** The secret is replaced at a fixed time every day, which the cache must not hold on to. */
  private static boolean isUsable(CachedSecret cache, Instant now) {
    return cache != null
        && now.isBefore(cache.fetched().plusSeconds(CACHE_SECONDS))
        && now.isBefore(BasicStatisticsSecretService.nextRotation(cache.fetched()));
  }

  /**
   * Fetches the secret from the first basic statistics secret service that answers, trying each one in turn with a
   * short timeout, as opposed to {@link RemoteBase#getResponse} which can retry a single unresponsive admin node for
   * up to a day. This is called by threads that serve requests, so it must not block them for long.
   */
  private byte[] refreshSecret(Instant requestStarted) throws BasicStatisticsSecretServiceException {
    List<ServiceRegistration> services;
    try {
      services = remoteServiceManager.getServiceRegistrationsByLoad(JOB_TYPE);
    } catch (ServiceRegistryException e) {
      throw new BasicStatisticsSecretServiceException(e);
    }
    if (services.isEmpty()) {
      throw new BasicStatisticsSecretServiceException("There is no basic statistics secret service");
    }

    for (ServiceRegistration service : services) {
      HttpGet get = new HttpGet(UrlSupport.concat(service.getHost(), service.getPath(), "/daily-secret"));
      ClassicHttpResponse response = null;
      try {
        response = client.execute(get, REQUEST_TIMEOUT_MILLIS, REQUEST_TIMEOUT_MILLIS);
        if (response.getCode() == HttpStatus.SC_OK) {
          String secret = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
          byte[] decoded = Base64.getDecoder().decode(secret);
          // The time from before the request is used, as the secret might have been replaced while it was running
          cached = new CachedSecret(decoded, requestStarted);
          return decoded;
        }
        logger.warn("Service at {} returned unexpected response code {} for the daily secret", service.getHost(),
            response.getCode());
      } catch (IOException | ParseException | IllegalArgumentException e) {
        logger.warn("Could not get the daily secret from {}: {}", service.getHost(), e.getMessage());
      } finally {
        closeConnection(response);
      }
    }
    throw new BasicStatisticsSecretServiceException("Unable to get the secret from any basic statistics secret "
        + "service");
  }

  @Reference
  @Override
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    super.setTrustedHttpClient(trustedHttpClient);
  }

  @Reference
  @Override
  public void setRemoteServiceManager(ServiceRegistry serviceRegistry) {
    super.setRemoteServiceManager(serviceRegistry);
  }
}
