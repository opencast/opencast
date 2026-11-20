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
package org.opencastproject.basicstatisticssecret.impl;

import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;

import org.osgi.service.component.annotations.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * A service for the rotating daily secret used in sessions hashes for Opencast basic statistics
 *
 * The secret is replaced the first time it is asked for after 4am.
 *
 * Must only run on a single node in the cluster.
 */
@Component(
    property = {
        "service.description=Basic Statistics Secret Service",
    },
    immediate = true,
    service = BasicStatisticsSecretService.class
)
public class BasicStatisticsSecretServiceImpl implements BasicStatisticsSecretService {

  private final SecureRandom secureRandom = new SecureRandom();

  private byte[] currentSecret;

  /** The time from which on the current secret must not be used anymore */
  private Instant validUntil = Instant.MIN;

  @Override
  public synchronized byte[] getCurrentSecret() {
    Instant now = Instant.now();
    if (!now.isBefore(validUntil)) {
      currentSecret = new byte[32];
      secureRandom.nextBytes(currentSecret);
      validUntil = BasicStatisticsSecretService.nextRotation(now);
    }
    return currentSecret;
  }
}
