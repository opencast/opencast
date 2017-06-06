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
package org.opencastproject.security.urlsigning.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public final class UrlSigningServiceOsgiUtil {
  private static final Logger logger = LoggerFactory.getLogger(UrlSigningServiceOsgiUtil.class);

  /** The default key in the OSGI service configuration for when signed URLs will expire. */
  public static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /** The default key in the OSGI service configuration for whether to use client IP in the signature. */
  public static final String URL_SIGNING_USE_CLIENT_IP = "url.signing.use.client.ip";

  /** The default time before a piece of signed content expires. 2 Hours. */
  public static final long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60;

  /** The default for whether to use the client IP in the signature.*/
  public static final Boolean DEFAULT_SIGN_WITH_CLIENT_IP = false;

  private UrlSigningServiceOsgiUtil() {

  }

  /**
   * Get the amount of seconds before a signed URL should expire from a {@link Dictionary}. Uses the
   * {@link UrlSigningServiceOsgiUtil}'s default value and default key name.
   *
   * @param properties
   *          The {@link Dictionary} to look through to get the expire duration.
   * @param className
   *          The name of the class that is getting the expire duration for logging.
   * @return The duration that URLs expire from the properties if present, the default if it isn't.
   */
  public static long getUpdatedSigningExpiration(@SuppressWarnings("rawtypes") Dictionary properties, String className) {
    return getUpdatedSigningExpiration(properties, className, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY,
            DEFAULT_URL_SIGNING_EXPIRE_DURATION);
  }


  /**
   * Get the amount of seconds before a signed URL should expire from a {@link Dictionary}.
   *
   * @param properties
   *          The {@link Dictionary} to look through to get the expire duration.
   * @param className
   *          The name of the class that is getting the expire duration for logging.
   * @param key
   *          The key in the dictionary that should contain the expire duration.
   * @param defaultExpiry
   *          The expire duration to use if one is not found in the {@link Dictionary}
   * @return The duration that URLs expire from the properties if present, the default if it isn't.
   */
  public static long getUpdatedSigningExpiration(@SuppressWarnings("rawtypes") Dictionary properties, String className,
          String key, long defaultExpiry) {
    long expireSeconds = defaultExpiry;
    Object dictionaryValue = properties.get(URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY);
    if (dictionaryValue != null) {
      try {
        expireSeconds = Long.parseLong(dictionaryValue.toString());
        logger.info("For the class {} the property '{}' has been configured to expire signed URLs in {} seconds.",
                new Object[] { className, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, expireSeconds });
      } catch (NumberFormatException e) {
        logger.warn(
                "For the class {} unable to parse when a stream should expire from '{}' so using default '{}' because: {}",
                new Object[] { className, dictionaryValue, defaultExpiry,
                        ExceptionUtils.getStackTrace(e) });
        expireSeconds = defaultExpiry;
      }
    } else {
      logger.debug(
              "For the class {} the property '{}' has not been configured, so the default is being used to expire signed URLs in {} seconds.",
              new Object[] { className, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, expireSeconds });
    }
    return expireSeconds;
  }


  /**
   * Get whether a signed URL should contain the client's IP from a {@link Dictionary}. Uses the
   * {@link UrlSigningServiceOsgiUtil}'s default value and default key name.
   *
   * @param properties
   *          The {@link Dictionary} to look through for whether the IP should be included.
   * @param className
   *          The name of the class that is getting the value for logging.
   * @return Whether the URLs that are signed should contain the client's IP address
   */
  public static boolean getUpdatedSignWithClientIP(@SuppressWarnings("rawtypes") Dictionary properties, String className) {
    return getUpdatedSignWithClientIP(properties, className, URL_SIGNING_USE_CLIENT_IP, DEFAULT_SIGN_WITH_CLIENT_IP);
  }

  /**
   * Get whether a signed URL should contain the client's IP from a {@link Dictionary}.
   *
   * @param properties
   *          The {@link Dictionary} to look through for whether the IP should be included.
   * @param className
   *          The name of the class that is getting the value for logging.
   * @param key
   *          The key in the dictionary that should contain the value.
   * @param defaultSignWithIP
   *          The default to use if the value is not found in the {@link Dictionary}
   * @return true if signed URLs should contain the client IP, false if not.
   */
  public static boolean getUpdatedSignWithClientIP(@SuppressWarnings("rawtypes") Dictionary properties,
          String className, String key, boolean defaultSignWithIP) {
    boolean signWithClientIP = defaultSignWithIP;
    Object dictionaryValue = properties.get(URL_SIGNING_USE_CLIENT_IP);
    if (dictionaryValue != null) {
        signWithClientIP = Boolean.parseBoolean(dictionaryValue.toString());
        if (signWithClientIP) {
          logger.info("For the class {} the property '{}' has been configured to sign urls with the client IP.", className,
                  URL_SIGNING_USE_CLIENT_IP);
        } else {
          logger.info("For the class {} the property '{}' has been configured to not sign urls with the client IP.",
                  className, URL_SIGNING_USE_CLIENT_IP);
        }
    } else {
      logger.debug(
              "For the class {} the property '{}' has not been configured, so the default of signing urls with the client ip is {}.",
              new Object[] { className, URL_SIGNING_USE_CLIENT_IP, signWithClientIP });
    }
    return signWithClientIP;
  }
}
