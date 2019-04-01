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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.security.urlsigning.utils

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Dictionary

object UrlSigningServiceOsgiUtil {
    private val logger = LoggerFactory.getLogger(UrlSigningServiceOsgiUtil::class.java)

    /** The default key in the OSGI service configuration for when signed URLs will expire.  */
    val URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds"

    /** The default key in the OSGI service configuration for whether to use client IP in the signature.  */
    val URL_SIGNING_USE_CLIENT_IP = "url.signing.use.client.ip"

    /** The default time before a piece of signed content expires. 2 Hours.  */
    val DEFAULT_URL_SIGNING_EXPIRE_DURATION = (2 * 60 * 60).toLong()

    /** The default for whether to use the client IP in the signature.  */
    val DEFAULT_SIGN_WITH_CLIENT_IP = false

    /**
     * Get the amount of seconds before a signed URL should expire from a [Dictionary].
     *
     * @param properties
     * The [Dictionary] to look through to get the expire duration.
     * @param className
     * The name of the class that is getting the expire duration for logging.
     * @param key
     * The key in the dictionary that should contain the expire duration.
     * @param defaultExpiry
     * The expire duration to use if one is not found in the [Dictionary]
     * @return The duration that URLs expire from the properties if present, the default if it isn't.
     */
    @JvmOverloads
    fun getUpdatedSigningExpiration(properties: Dictionary<*, *>, className: String,
                                    key: String = URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, defaultExpiry: Long = DEFAULT_URL_SIGNING_EXPIRE_DURATION): Long {
        var expireSeconds = defaultExpiry
        val dictionaryValue = properties.get(URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY)
        if (dictionaryValue != null) {
            try {
                expireSeconds = java.lang.Long.parseLong(dictionaryValue.toString())
                logger.info("For the class {} the property '{}' has been configured to expire signed URLs in {} seconds.",
                        className, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, expireSeconds)
            } catch (e: NumberFormatException) {
                logger.warn(
                        "For the class {} unable to parse when a stream should expire from '{}' so using default '{}' because: {}",
                        className, dictionaryValue, defaultExpiry, ExceptionUtils.getStackTrace(e))
                expireSeconds = defaultExpiry
            }

        } else {
            logger.debug(
                    "For the class {} the property '{}' has not been configured, so the default is being used to expire signed URLs in {} seconds.",
                    className, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, expireSeconds)
        }
        return expireSeconds
    }

    /**
     * Get whether a signed URL should contain the client's IP from a [Dictionary].
     *
     * @param properties
     * The [Dictionary] to look through for whether the IP should be included.
     * @param className
     * The name of the class that is getting the value for logging.
     * @param key
     * The key in the dictionary that should contain the value.
     * @param defaultSignWithIP
     * The default to use if the value is not found in the [Dictionary]
     * @return true if signed URLs should contain the client IP, false if not.
     */
    @JvmOverloads
    fun getUpdatedSignWithClientIP(properties: Dictionary<*, *>,
                                   className: String, key: String = URL_SIGNING_USE_CLIENT_IP, defaultSignWithIP: Boolean = DEFAULT_SIGN_WITH_CLIENT_IP): Boolean {
        var signWithClientIP = defaultSignWithIP
        val dictionaryValue = properties.get(URL_SIGNING_USE_CLIENT_IP)
        if (dictionaryValue != null) {
            signWithClientIP = java.lang.Boolean.parseBoolean(dictionaryValue.toString())
            if (signWithClientIP) {
                logger.info("For the class {} the property '{}' has been configured to sign urls with the client IP.",
                        className, URL_SIGNING_USE_CLIENT_IP)
            } else {
                logger.info("For the class {} the property '{}' has been configured to not sign urls with the client IP.",
                        className, URL_SIGNING_USE_CLIENT_IP)
            }
        } else {
            logger.debug(
                    "For the class {} the property '{}' has not been configured, so the default of signing urls with the client ip is {}.",
                    className, URL_SIGNING_USE_CLIENT_IP, signWithClientIP)
        }
        return signWithClientIP
    }
}
/**
 * Get the amount of seconds before a signed URL should expire from a [Dictionary]. Uses the
 * [UrlSigningServiceOsgiUtil]'s default value and default key name.
 *
 * @param properties
 * The [Dictionary] to look through to get the expire duration.
 * @param className
 * The name of the class that is getting the expire duration for logging.
 * @return The duration that URLs expire from the properties if present, the default if it isn't.
 */
/**
 * Get whether a signed URL should contain the client's IP from a [Dictionary]. Uses the
 * [UrlSigningServiceOsgiUtil]'s default value and default key name.
 *
 * @param properties
 * The [Dictionary] to look through for whether the IP should be included.
 * @param className
 * The name of the class that is getting the value for logging.
 * @return Whether the URLs that are signed should contain the client's IP address
 */
