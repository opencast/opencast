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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class ClientPushRateLimiterTest {

  private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
  private static final long HOUR = TimeUnit.HOURS.toMillis(1);

  private static InetAddress client(int lastByte) throws Exception {
    return InetAddress.getByAddress(new byte[]{1, 2, 3, (byte) lastByte});
  }

  @Test
  public void testLimitOfSingleRequest() throws Exception {
    ClientPushRateLimiter limiter = new ClientPushRateLimiter(100, 0, 0);

    assertTrue(limiter.allow(client(1), 100, 0));
    assertFalse(limiter.allow(client(1), 101, 0));
  }

  @Test
  public void testZeroMeansNoLimit() throws Exception {
    ClientPushRateLimiter limiter = new ClientPushRateLimiter(0, 0, 0);

    for (int i = 0; i < 3; i++) {
      assertTrue(limiter.allow(client(1), 1_000_000_000L, i));
    }
  }

  @Test
  public void testLimitsPerMinuteAndPerHour() throws Exception {
    ClientPushRateLimiter limiter = new ClientPushRateLimiter(100, 250, 400);
    InetAddress client = client(1);

    assertTrue(limiter.allow(client, 100, 0));
    assertTrue(limiter.allow(client, 100, 1000));
    // too much for this minute, and requests that are ignored are not counted
    assertFalse(limiter.allow(client, 100, 2000));
    assertFalse(limiter.allow(client, 100, 3000));

    // a new minute
    assertTrue(limiter.allow(client, 100, MINUTE));
    assertTrue(limiter.allow(client, 100, MINUTE + 1000));
    // not too much for this minute, but for this hour
    assertFalse(limiter.allow(client, 100, 2 * MINUTE));

    // a new hour
    assertTrue(limiter.allow(client, 100, HOUR));
  }

  @Test
  public void testEachClientIsCountedOnItsOwn() throws Exception {
    ClientPushRateLimiter limiter = new ClientPushRateLimiter(100, 100, 0);

    assertTrue(limiter.allow(client(1), 100, 0));
    assertFalse(limiter.allow(client(1), 1, 0));
    assertTrue(limiter.allow(client(2), 100, 0));
  }
}
