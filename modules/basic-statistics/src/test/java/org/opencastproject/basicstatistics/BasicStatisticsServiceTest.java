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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test class for the basic statistics service
 */
public class BasicStatisticsServiceTest {

  private static final byte[] SECRET = "secret".getBytes(StandardCharsets.UTF_8);

  private BasicStatisticsService service;
  private InetAddress ip;

  @Before
  public void setUp() throws Exception {
    service = new BasicStatisticsService();
    ip = InetAddress.getByAddress(new byte[]{1, 2, 3, 4});
  }

  @Test
  public void testSessionHashIsTruncatedHmac() throws Exception {
    // first 8 bytes of HMAC-SHA256(secret, itemId || ip bytes || user agent), as big-endian number
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
    mac.update("item".getBytes(StandardCharsets.UTF_8));
    mac.update(ip.getAddress());
    mac.update("ua".getBytes(StandardCharsets.UTF_8));
    long expected = ByteBuffer.wrap(mac.doFinal()).getLong();

    long hash = service.generateSessionHash(SECRET, "item", ip, "ua");

    assertEquals(expected, hash);
    // fixed value, so that the definition of the hash cannot change by accident: a session must keep its hash
    // within a day
    assertEquals(-5311488531004393648L, hash);
  }

  @Test
  public void testSessionHashIsStableAndSeparatesSessions() throws Exception {
    long hash = service.generateSessionHash(SECRET, "item", ip, "ua");

    assertEquals(hash, service.generateSessionHash(SECRET, "item", ip, "ua"));

    assertNotEquals(hash, service.generateSessionHash(SECRET, "other-item", ip, "ua"));
    assertNotEquals(hash, service.generateSessionHash(SECRET, "item", InetAddress.getByName("1.2.3.5"), "ua"));
    assertNotEquals(hash, service.generateSessionHash(SECRET, "item", ip, "other-ua"));
    // the secret changes every day, which is what makes the sessions of different days unrelated
    assertNotEquals(hash,
        service.generateSessionHash("other".getBytes(StandardCharsets.UTF_8), "item", ip, "ua"));
  }

  @Test
  public void testSessionHashWithIpv6() throws Exception {
    long v6 = service.generateSessionHash(SECRET, "item", InetAddress.getByName("2001:db8::1"), "ua");
    assertNotEquals(v6, service.generateSessionHash(SECRET, "item", ip, "ua"));
  }
}
