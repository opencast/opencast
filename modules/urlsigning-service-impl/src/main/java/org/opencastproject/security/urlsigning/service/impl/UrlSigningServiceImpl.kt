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
package org.opencastproject.security.urlsigning.service.impl

import java.util.Objects.requireNonNull
import org.opencastproject.security.urlsigning.exception.UrlSigningException.urlNotSupported

import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.urlsigning.common.Policy

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.DateTimeZone
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class UrlSigningServiceImpl : UrlSigningService {

    /** List of registered signing providers  */
    private val signingProviders = CopyOnWriteArrayList<UrlSigningProvider>()

    /** OSGi callback for registering [UrlSigningProvider]  */
    internal fun registerSigningProvider(provider: UrlSigningProvider) {
        signingProviders.add(provider)
        logger.info("{} registered", provider)
    }

    /** OSGi callback for unregistering [UrlSigningProvider]  */
    internal fun unregisterSigningProvider(provider: UrlSigningProvider) {
        signingProviders.remove(provider)
        logger.info("{} unregistered", provider)
    }

    override fun accepts(baseUrl: String): Boolean {
        for (provider in signingProviders) {
            if (provider.accepts(baseUrl)) {
                logger.debug("{} accepted to sign base URL '{}'", provider, baseUrl)
                return true
            }
        }

        logger.debug("No provider accepted to sign the URL '{}'", baseUrl)
        return false
    }

    @Throws(UrlSigningException::class)
    override fun sign(baseUrl: String, validUntilDuration: Long?, validFromDuration: Long?,
                      ipAddr: String): String {
        requireNonNull<Long>(validUntilDuration)
        val validUntil = DateTime(DateTimeZone.UTC).plus(validUntilDuration!! * DateTimeConstants.MILLIS_PER_SECOND)
        val validFrom = if (validFromDuration == null)
            null
        else
            DateTime(DateTimeZone.UTC).plus(validFromDuration * DateTimeConstants.MILLIS_PER_SECOND)
        return sign(baseUrl, validUntil, validFrom!!, ipAddr)
    }

    @Throws(UrlSigningException::class)
    override fun sign(baseUrl: String, validUntil: DateTime, validFrom: DateTime, ipAddr: String): String {
        requireNonNull(baseUrl)
        requireNonNull(validUntil)

        val policy = Policy.mkPolicyValidFromWithIP(baseUrl, validUntil, validFrom, ipAddr)

        for (provider in signingProviders) {
            if (provider.accepts(baseUrl)) {
                logger.debug("{} accepted to sign base URL '{}'", provider, baseUrl)
                return provider.sign(policy)
            }
        }

        logger.warn("No signing provider accepted to sign URL '{}'", baseUrl)
        throw urlNotSupported()
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(UrlSigningServiceImpl::class.java)
    }

}
