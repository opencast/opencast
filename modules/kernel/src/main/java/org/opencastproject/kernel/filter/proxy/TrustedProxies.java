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

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keeps track of which hosts are allowed to send <code>X-Forwarded-*</code> headers.
 * <p>
 * Forwarding headers are trivially forged by any client, so they may only be honored if the request was actually
 * received from a reverse proxy the administrator has declared trustworthy. This component holds that list and
 * implements the address resolution based on it.
 */
@Component(
    service = TrustedProxies.class,
    immediate = true,
    property = {
        "service.description=Trusted Proxy Configuration",
    },
    configurationPid = "org.opencastproject.kernel.filter.proxy.TrustedProxies"
)
public class TrustedProxies {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TrustedProxies.class);

  /** Configuration key holding the comma separated list of trusted proxy addresses or CIDR ranges */
  public static final String TRUSTED_PROXIES_KEY = "trusted.proxies";

  /**
   * Addresses trusted if nothing is configured: loopback and the private ranges a reverse proxy running next to
   * Opencast realistically uses. Public addresses are deliberately not part of this.
   */
  public static final String DEFAULT_TRUSTED_PROXIES
      = "127.0.0.0/8, ::1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fc00::/7";

  /** Matches a dotted quad with all four octets in range */
  private static final Pattern IPV4 = Pattern.compile(
      "((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])");

  /** The compiled configuration. Replaced as a whole on reconfiguration, hence volatile rather than synchronized. */
  private volatile List<IpAddressMatcher> matchers = Collections.emptyList();

  @Activate
  @Modified
  public void modified(Map<String, Object> properties) {
    final Object configured = properties == null ? null : properties.get(TRUSTED_PROXIES_KEY);
    final String value = configured == null ? DEFAULT_TRUSTED_PROXIES : configured.toString();

    final List<IpAddressMatcher> parsed = new ArrayList<>();
    final List<String> accepted = new ArrayList<>();
    for (String configuredEntry : StringUtils.split(value, ',')) {
      final String entry = StringUtils.trimToEmpty(configuredEntry);
      if (entry.isEmpty()) {
        continue;
      }
      if (!isValidRange(entry)) {
        logger.warn("Ignoring invalid entry '{}' in {}. Expected an IP address or a CIDR range.",
                entry, TRUSTED_PROXIES_KEY);
        continue;
      }
      try {
        parsed.add(new IpAddressMatcher(entry));
        accepted.add(entry);
      } catch (IllegalArgumentException e) {
        logger.warn("Ignoring unparsable entry '{}' in {}: {}", entry, TRUSTED_PROXIES_KEY, e.getMessage());
      }
    }

    matchers = Collections.unmodifiableList(parsed);
    if (accepted.isEmpty()) {
      logger.info("No trusted proxies configured. X-Forwarded-* headers will be ignored on every request.");
    } else {
      logger.info("Honoring X-Forwarded-* headers from these proxies: {}", String.join(", ", accepted));
    }
  }

  /**
   * Checks if the given address belongs to a configured reverse proxy.
   *
   * @param address
   *          the address to check, which must be an IP address literal
   * @return whether headers coming from this address may be trusted
   */
  public boolean isTrusted(String address) {
    if (!isIpLiteral(address)) {
      return false;
    }
    for (IpAddressMatcher matcher : matchers) {
      // Never throws: isIpLiteral() guarantees a parsable literal, so no name resolution can happen here
      if (matcher.matches(address)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines the address of the client which originally sent the request.
   * <p>
   * The <code>X-Forwarded-For</code> header is a list of addresses which grows as the request passes through proxies,
   * the leftmost entries being the ones added first. Since every proxy may only be trusted for the address it added
   * itself, the list is walked from the right, dropping known proxies, and the first address which is not one of our
   * proxies is the client. Anything to the left of it was supplied by that client and must not be believed.
   *
   * @param peer
   *          the address the request was actually received from
   * @param forwardedFor
   *          the value of the <code>X-Forwarded-For</code> header, may be <code>null</code>
   * @return the client address, falling back to <code>peer</code> whenever the header cannot be trusted
   */
  public String resolveClientAddress(String peer, String forwardedFor) {
    if (!isTrusted(peer) || StringUtils.isBlank(forwardedFor)) {
      return peer;
    }

    final String[] hops = StringUtils.split(forwardedFor, ',');
    for (int i = hops.length - 1; i >= 0; i--) {
      final String hop = normalize(hops[i]);
      if (hop == null) {
        // A malformed entry means we cannot tell which of the remaining ones the client supplied
        logger.debug("Ignoring X-Forwarded-For header with malformed entry '{}'", hops[i]);
        return peer;
      }
      if (!isTrusted(hop)) {
        return hop;
      }
    }

    // Every hop was a proxy of ours, so there is nothing better to report than the immediate peer
    return peer;
  }

  /**
   * Strips the decoration an address may carry in an <code>X-Forwarded-For</code> header.
   *
   * @return the bare address literal or <code>null</code> if the entry is not one
   */
  private static String normalize(String hop) {
    String address = StringUtils.trimToEmpty(hop);

    // An IPv6 address with a port is bracketed, e.g. [::1]:8080
    if (address.startsWith("[")) {
      final int end = address.indexOf(']');
      address = end < 0 ? "" : address.substring(1, end);
    } else if (StringUtils.countMatches(address, ':') == 1) {
      // A single colon cannot be IPv6, so this is an IPv4 address with a port
      address = StringUtils.substringBefore(address, ":");
    }

    return isIpLiteral(address) ? address : null;
  }

  /**
   * Checks that the given string is an IP address literal. Everything handed to {@link IpAddressMatcher} has to pass
   * this first, since that class resolves names via DNS, which we must never do for attacker controlled input.
   */
  private static boolean isIpLiteral(String address) {
    if (StringUtils.isBlank(address)) {
      return false;
    }

    if (address.indexOf(':') < 0) {
      return IPV4.matcher(address).matches();
    }

    // Host names cannot contain a colon, so the only thing left to rule out are IPv6 zone identifiers
    for (char c : address.toCharArray()) {
      final boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
      if (!hex && c != ':' && c != '.') {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks that a configured entry is an address literal with an optional prefix length.
   */
  private static boolean isValidRange(String entry) {
    final String address = StringUtils.substringBefore(entry, "/");
    if (!isIpLiteral(address)) {
      return false;
    }

    if (entry.indexOf('/') < 0) {
      return true;
    }

    final int maximum = address.indexOf(':') < 0 ? 32 : 128;
    try {
      final int bits = Integer.parseInt(StringUtils.substringAfter(entry, "/"));
      return bits >= 0 && bits <= maximum;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}
