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
package org.opencastproject.kernel.filter.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

/**
 * Tests the {@link TrustedProxies}
 */
public class TrustedProxiesTest {

  private static final String PROXY = "10.0.0.1";

  private static final String CLIENT = "93.184.216.34";

  private TrustedProxies configure(String trustedProxies) {
    TrustedProxies trusted = new TrustedProxies();
    trusted.modified(trustedProxies == null
        ? Map.of()
        : Map.of(TrustedProxies.TRUSTED_PROXIES_KEY, trustedProxies));
    return trusted;
  }

  @Test
  public void testDefaultConfiguration() {
    TrustedProxies trusted = configure(null);
    assertTrue(trusted.isTrusted("127.0.0.1"));
    assertTrue(trusted.isTrusted("::1"));
    assertTrue(trusted.isTrusted("10.0.0.1"));
    assertTrue(trusted.isTrusted("172.16.5.5"));
    assertTrue(trusted.isTrusted("192.168.1.1"));
    assertTrue(trusted.isTrusted("fd12::1"));
    assertFalse(trusted.isTrusted(CLIENT));
    assertFalse(trusted.isTrusted("172.32.0.1"));
    assertFalse(trusted.isTrusted("2001:db8::1"));
  }

  @Test
  public void testEmptyConfigurationTrustsNothing() {
    TrustedProxies trusted = configure("");
    assertFalse(trusted.isTrusted("127.0.0.1"));
    assertFalse(trusted.isTrusted(CLIENT));
  }

  @Test
  public void testSingleAddressAndCidr() {
    TrustedProxies trusted = configure("192.0.2.7, 198.51.100.0/24");
    assertTrue(trusted.isTrusted("192.0.2.7"));
    assertFalse(trusted.isTrusted("192.0.2.8"));
    assertTrue(trusted.isTrusted("198.51.100.255"));
    assertFalse(trusted.isTrusted("198.51.101.1"));
  }

  @Test
  public void testInvalidEntriesAreSkipped() {
    TrustedProxies trusted = configure("not-an-address, 10.0.0.0/99, 300.1.1.1, proxy.example.org, 192.0.2.7");
    assertTrue("valid entries are still applied", trusted.isTrusted("192.0.2.7"));
    assertFalse(trusted.isTrusted("10.0.0.1"));
  }

  @Test
  public void testHostnamesAreNeverTrusted() {
    // A host name must not even be handed to the matcher, since resolving it would mean a DNS lookup
    TrustedProxies trusted = configure("127.0.0.0/8");
    assertFalse(trusted.isTrusted("localhost"));
    assertFalse(trusted.isTrusted(null));
    assertFalse(trusted.isTrusted(""));
  }

  @Test
  public void testUntrustedPeerKeepsItsAddress() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(CLIENT, trusted.resolveClientAddress(CLIENT, "192.0.2.7"));
  }

  @Test
  public void testMissingHeaderKeepsPeerAddress() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(PROXY, trusted.resolveClientAddress(PROXY, null));
    assertEquals(PROXY, trusted.resolveClientAddress(PROXY, "  "));
  }

  @Test
  public void testSingleForwardedAddress() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, CLIENT));
  }

  @Test
  public void testChainIsWalkedFromTheRight() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    // This is what nginx produces with $proxy_add_x_forwarded_for: the client supplied the first entry itself
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, "192.0.2.7, " + CLIENT));
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, CLIENT + ", 10.0.0.9, 10.0.0.8"));
  }

  @Test
  public void testSpoofedChainCannotReachPastTheClient() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    // An attacker claiming to be a proxy of ours does not get to pick the address behind it
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, "192.0.2.7, 10.0.0.5, " + CLIENT));
  }

  @Test
  public void testAllHopsTrustedKeepsPeerAddress() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(PROXY, trusted.resolveClientAddress(PROXY, "10.0.0.5, 10.0.0.6"));
  }

  @Test
  public void testMalformedEntryIsNotBelieved() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(PROXY, trusted.resolveClientAddress(PROXY, "evil.example.org"));
    assertEquals(PROXY, trusted.resolveClientAddress(PROXY, CLIENT + ", garbage"));
  }

  @Test
  public void testWhitespaceAndPortsAreStripped() {
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, "   " + CLIENT + "   "));
    assertEquals(CLIENT, trusted.resolveClientAddress(PROXY, CLIENT + ":51234"));
    assertEquals("2001:db8::1", trusted.resolveClientAddress(PROXY, "[2001:db8::1]:51234"));
  }

  @Test
  public void testIpv6Client() {
    TrustedProxies trusted = configure("::1");
    assertEquals("2001:db8::1", trusted.resolveClientAddress("::1", "2001:db8::1"));
  }

  @Test
  public void testMixedAddressFamilies() {
    // An IPv4 range must never match an IPv6 address and vice versa
    TrustedProxies trusted = configure("10.0.0.0/8");
    assertFalse(trusted.isTrusted("::1"));
    assertEquals("::1", trusted.resolveClientAddress(PROXY, "::1"));
  }

}
