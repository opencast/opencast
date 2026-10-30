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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * This is a remote waveform service that will call the waveform service implementation on a remote host.
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

  private long lastUpdated = 0;
  private byte[] cachedSecret = null;
  private static final long CACHE_SECONDS = 600;

  @Override
  public byte[] getCurrentSecret() throws BasicStatisticsSecretServiceException {

    if (lastUpdated + CACHE_SECONDS > Instant.now().getEpochSecond()) {
      return cachedSecret;
    }


    HttpGet get = new HttpGet("/daily-secret");
    HttpResponse response = getResponse(get);

    int status = response.getStatusLine().getStatusCode();
    if (status != HttpStatus.SC_OK) {
      throw new BasicStatisticsSecretServiceException("Failed to fetch secret: HTTP " + status);
    }

    String secret;
    try {
      secret = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      throw new BasicStatisticsSecretServiceException(e);
    }
    cachedSecret = Base64.getDecoder().decode(secret);
    lastUpdated = Instant.now().getEpochSecond();

    return cachedSecret;
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
