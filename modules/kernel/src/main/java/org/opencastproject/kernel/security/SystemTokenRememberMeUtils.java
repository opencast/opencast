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
import org.springframework.security.crypto.codec.Hex;

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
 *
 */
public final class SystemTokenRememberMeUtils {

  private static final Logger logger = LoggerFactory.getLogger(SystemTokenRememberMeUtils.class);

  /** This is the default cookie key, that is configured in Opencast **/
  private static final String defaultCookieKey = "opencast";

  private SystemTokenRememberMeUtils() {
  }

  public static String augmentKey(String key) {

    if (!defaultCookieKey.equals(key)) {
      logger.debug("The default cookie key 'opencast' is not in use. The given key won't be augmented.");
      return key;
    }

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
    return key;
  }
}
