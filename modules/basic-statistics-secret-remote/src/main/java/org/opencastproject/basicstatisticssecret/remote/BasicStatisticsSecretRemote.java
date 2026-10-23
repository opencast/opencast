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
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

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

  /** The default constructor. */
  public BasicStatisticsSecretRemote() {
    super(JOB_TYPE);
  }

  private static final long CACHE_SECONDS = 600;

  /** A secret, and the time before it was fetched */
  private record CachedSecret(byte[] secret, Instant fetched) { }

  private volatile CachedSecret cached = null;

  @Override
  public byte[] getCurrentSecret() throws BasicStatisticsSecretServiceException {
    Instant now = Instant.now();
    CachedSecret cache = cached;
    // The secret is replaced at a fixed time every day, which the cache must not hold on to
    if (cache != null
        && now.isBefore(cache.fetched().plusSeconds(CACHE_SECONDS))
        && now.isBefore(BasicStatisticsSecretService.nextRotation(cache.fetched()))) {
      return cache.secret();
    }

    // Without a secret service, getResponse waits for one to show up, and that can be forever. This is called by
    // threads that serve requests, so it is better to fail.
    try {
      if (remoteServiceManager.getServiceRegistrationsByLoad(JOB_TYPE).isEmpty()) {
        throw new BasicStatisticsSecretServiceException("There is no basic statistics secret service");
      }
    } catch (ServiceRegistryException e) {
      throw new BasicStatisticsSecretServiceException(e);
    }

    HttpGet get = new HttpGet("/daily-secret");
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        String secret = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
        byte[] decoded = Base64.getDecoder().decode(secret);
        // The time from before the request is used, as the secret might have been replaced while it was running
        cached = new CachedSecret(decoded, now);
        return decoded;
      }
    } catch (IOException | IllegalArgumentException e) {
      throw new BasicStatisticsSecretServiceException(e);
    } finally {
      closeConnection(response);
    }
    throw new BasicStatisticsSecretServiceException("Unable to get the secret from the remote service");
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
