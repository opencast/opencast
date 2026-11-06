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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Limits how much data a client can send to the client push endpoint, as anyone can send data there. The limits are on
 * the size of the requests: of a single request, and of all the requests of one IP address within a minute and within
 * an hour. A limit of 0 means that there is none.
 *
 * Everything is kept in memory. Restarting a node resets the limits, and several nodes count independently of each
 * other. IP addresses are not kept, only hashes of them, which are made using a random key that is not stored.
 */
public class ClientPushRateLimiter {

  private final long maxRequestBytes;
  private final long maxMinuteBytes;
  private final long maxHourBytes;

  private final byte[] key = new byte[32];

  // The size is limited, so that many different addresses cannot use up all the memory
  private final Cache<Long, Usage> usages = CacheBuilder.newBuilder()
      .maximumSize(100_000)
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build();

  /**
   * @param maxRequestBytes the size of the largest request that is allowed
   * @param maxMinuteBytes how much one IP address can send in a minute
   * @param maxHourBytes how much one IP address can send in an hour
   */
  public ClientPushRateLimiter(long maxRequestBytes, long maxMinuteBytes, long maxHourBytes) {
    this.maxRequestBytes = maxRequestBytes;
    this.maxMinuteBytes = maxMinuteBytes;
    this.maxHourBytes = maxHourBytes;
    new SecureRandom().nextBytes(key);
  }

  /**
   * @return the size of the largest request that is allowed
   */
  public long getMaxRequestBytes() {
    return maxRequestBytes > 0 ? maxRequestBytes : Long.MAX_VALUE;
  }

  /**
   * Check if a request is within the limits, and count it if it is. A request that is not is not counted.
   *
   * @param client the IP address of the client that sent the request
   * @param bytes the size of the request
   * @return false if the request has to be ignored
   */
  public boolean allow(InetAddress client, long bytes) {
    return allow(client, bytes, System.currentTimeMillis());
  }

  boolean allow(InetAddress client, long bytes, long nowMillis) {
    if (bytes > getMaxRequestBytes()) {
      return false;
    }
    if (maxMinuteBytes <= 0 && maxHourBytes <= 0) {
      return true;
    }
    long hash = ByteBuffer.wrap(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmac(client.getAddress())).getLong();
    return usages.asMap().computeIfAbsent(hash, h -> new Usage()).add(bytes, nowMillis);
  }

  /** What one client sent in the current minute and the current hour */
  private class Usage {
    private long minuteStart;
    private long minuteBytes;
    private long hourStart;
    private long hourBytes;

    synchronized boolean add(long bytes, long nowMillis) {
      if (nowMillis - minuteStart >= TimeUnit.MINUTES.toMillis(1)) {
        minuteStart = nowMillis;
        minuteBytes = 0;
      }
      if (nowMillis - hourStart >= TimeUnit.HOURS.toMillis(1)) {
        hourStart = nowMillis;
        hourBytes = 0;
      }
      if ((maxMinuteBytes > 0 && minuteBytes + bytes > maxMinuteBytes)
          || (maxHourBytes > 0 && hourBytes + bytes > maxHourBytes)) {
        return false;
      }
      minuteBytes += bytes;
      hourBytes += bytes;
      return true;
    }
  }
}
