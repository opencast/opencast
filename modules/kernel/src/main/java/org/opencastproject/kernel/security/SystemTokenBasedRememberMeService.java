/**
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

package org.opencastproject.kernel.security;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * This implements a zero-configuration version Spring Security's token based remember-me service. While the key can
 * still be augmented by configuration, it is generally generated based on seldom changing but unique system
 * properties like hostname, IP address, file system information and Linux kernel.
 */
public class SystemTokenBasedRememberMeService extends TokenBasedRememberMeServices {
  private Logger logger = LoggerFactory.getLogger(SystemTokenBasedRememberMeService.class);
  private String key;

  @Deprecated
  public SystemTokenBasedRememberMeService() {
    super();
    setKey(null);
  }

  public SystemTokenBasedRememberMeService(String key, UserDetailsService userDetailsService) {
    super(key, userDetailsService);
    setKey(key);
  }

  /**
   * Set a new key to be used when generating remember-me tokens.
   *
   * Note that the key passed to this method will be augmented by seldom changing but generally unique system
   * properties like hostname, IP address, file system information and Linux kernel. Hence, even setting no custom
   * key should be save.
   */
  @Override
  public void setKey(String key) {
    // Start with a user key if provided
    StringBuilder keyBuilder = new StringBuilder(Objects.toString(key, ""));

    // This will give us the hostname and IP address as something which should be unique per system.
    // For example: lk.elan-ev.de/10.10.10.31
    try {
      keyBuilder.append(InetAddress.getLocalHost());
    } catch (UnknownHostException e) {
      // silently ignore this
    }

    // Gather additional system properties as key
    // This requires a proc-fs which should generally be available under Linux.
    // But even without, we have fallbacks above and below.
    for (String procFile: Arrays.asList("/proc/version", "/proc/partitions")) {
      try (FileInputStream fileInputStream = new FileInputStream(new File(procFile))) {
        keyBuilder.append(IOUtils.toString(fileInputStream, StandardCharsets.UTF_8));
      } catch (IOException e) {
        // ignore this
      }
    }

    // If we still have no proper key, just generate a random one.
    // This will work just fine with the single drawback that restarting Opencast invalidates all remember-me tokens.
    // But it should be a sufficiently good fallback.
    key = keyBuilder.toString();
    if (key.isEmpty()) {
      logger.warn("Could not generate semi-persistent remember-me key. Will generate a non-persistent random one.");
      key = Double.toString(Math.random());
    }
    logger.debug("Remember me key before hashing: {}", key);

    // Use a SHA-512 hash as key to have a more sane key.
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-512");
      key = new String(Hex.encode(digest.digest(key.getBytes())));
    } catch (NoSuchAlgorithmException e) {
      logger.warn("No SHA-512 algorithm available!");
    }
    logger.debug("Calculated remember me key: {}", key);
    this.key = key;
    super.setKey(key);
  }

  @Override
  public String getKey() {
    return this.key;
  }

  /**
   * Calculates the digital signature to be put in the cookie. Default value is
   * SHA-512 ("username:tokenExpiryTime:password:key")
   */
  @Override
  protected String makeTokenSignature(long tokenExpiryTime, String username, String password) {
    String data = username + ":" + tokenExpiryTime + ":" + password + ":" + getKey();
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No SHA-512 algorithm available!");
    }

    return new String(Hex.encode(digest.digest(data.getBytes())));
  }
}
