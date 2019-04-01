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
package org.opencastproject.security.urlsigning.provider.impl

import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.provider.UrlSigningProvider
import org.opencastproject.urlsigning.common.Policy
import org.opencastproject.urlsigning.common.ResourceStrategy
import org.opencastproject.urlsigning.utils.ResourceRequestUtil

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger

import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Dictionary
import java.util.Enumeration
import java.util.HashMap
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

abstract class AbstractUrlSigningProvider : UrlSigningProvider, ManagedService {

    /** The security service  */
    protected var securityService: SecurityService

    /**
     * @return The method that an implementation class will convert base urls to resource urls.
     */
    abstract val resourceStrategy: ResourceStrategy

    /**
     * @return The logger to use for this signing provider.
     */
    abstract val logger: Logger

    /** The map to contain the list of keys, their ids and the urls they match.  */
    private var keys: Map<String, KeyEntry> = HashMap()

    /**
     * @return The current set of url beginnings this signing provider is looking for.
     */
    val uris: Set<String>
        get() = keys.values.stream()
                .map<String> { keyEntry -> keyEntry.url }
                .collect(Collectors.collectingAndThen<String, Any, Set<String>, Set<String>>(
                        Collectors.toSet(),
                        Function<Set<String>, Set<String>> { Collections.unmodifiableSet(it) }))

    /**
     * A class to contain the necessary key entries for url signing.
     */
    private class KeyEntry {
        private val key: String? = null
        private val url: String? = null
        private val organization = ANY_ORGANIZATION
    }

    /**
     * @param securityService
     * the securityService to set
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * If available get a [KeyEntry] if there is a matching Url matcher.
     *
     * @param baseUrl
     * The url to check against the possible matchers.
     * @return The [KeyEntry] if it is available.
     */
    private fun getKeyEntry(baseUrl: String): Optional<Entry<String, KeyEntry>> {
        return keys.entries.stream()
                .filter { entry -> baseUrl.startsWith(entry.value.url!!) }
                .findAny()
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        logger.info("Updating {}", toString())
        if (properties == null) {
            logger.warn("{} is unconfigured", toString())
            return
        }

        // Collect configuration in a new map so we don't partially override the old one in case of error
        var keys: MutableMap<String, KeyEntry> = HashMap()

        val propertyKeys = properties.keys()
        while (propertyKeys.hasMoreElements()) {
            val propertyKey = propertyKeys.nextElement()

            val keyEntryProperty = StringUtils.removeStart(propertyKey, "$KEY_ENTRY_PREFIX.")
            if (keyEntryProperty === propertyKey) continue

            val parts = Arrays.stream(keyEntryProperty.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    .map<String>(Function<String, String> { it.trim({ it <= ' ' }) })
                    .toArray<String>(String[]::new  /* Currently unsupported in Kotlin */)
            if (parts.size != 2) {
                throw ConfigurationException(propertyKey, "wrong property key format")
            }

            val id = parts[0]
            val currentKeyEntry = (keys as java.util.Map<String, KeyEntry>).computeIfAbsent(id) { __ -> KeyEntry() }

            val attribute = parts[1]
            val propertyValue = StringUtils.trimToNull(Objects.toString(properties.get(propertyKey), null))
                    ?: throw ConfigurationException(propertyKey, "can't be null or empty")
            when (attribute) {
                ORGANIZATION -> currentKeyEntry.organization = propertyValue
                URL -> {
                    if (keys.values.stream()
                                    .map<String> { keyEntry -> keyEntry.url }
                                    .filter(Predicate<String> { Objects.nonNull(it) })
                                    .anyMatch { url -> propertyValue.startsWith(url!!) || url != null && url.startsWith(propertyValue) }) {
                        throw ConfigurationException(propertyKey,
                                "there is already a key configuration for a URL with the prefix $propertyValue")
                    }
                    currentKeyEntry.url = propertyValue
                }
                SECRET -> currentKeyEntry.key = propertyValue
                else -> throw ConfigurationException(propertyKey, "unknown attribute $attribute for key $id")
            }
        }

        keys = keys.entries.stream()
                .filter { entry -> entry.value.key != null && entry.value.url != null }
                .collect<Map<String, KeyEntry>, Any>(Collectors.toMap<Entry<String, KeyEntry>, String, KeyEntry>(Function<Entry<String, KeyEntry>, String> { it.key }, Function<Entry<String, KeyEntry>, KeyEntry> { it.value }))

        // Has the rewriter been fully configured
        if (keys.size == 0) {
            logger.info("{} configured to not sign any urls.", toString())
        }

        this.keys = keys
    }

    override fun accepts(baseUrl: String): Boolean {
        try {
            URI(baseUrl)
        } catch (e: URISyntaxException) {
            logger.debug("Unable to support url {} because", baseUrl, e)
            return false
        }

        // Don't accept URLs without an organization context
        // (for example from the ServiceRegistry JobProducerHeartbeat)
        if (securityService.organization == null)
            return false

        val orgId = securityService.organization.id

        val keyEntry = getKeyEntry(baseUrl)
        return keyEntry
                .map<String> { entry -> entry.value.organization }
                .map { organization -> organization == ANY_ORGANIZATION || organization == orgId }
                .orElse(false)
    }

    @Throws(UrlSigningException::class)
    override fun sign(policy: Policy): String {
        if (!accepts(policy.baseUrl)) {
            throw UrlSigningException.urlNotSupported()
        }

        policy.setResourceStrategy(resourceStrategy)

        try {
            // Get the key that matches this URI since there must be one that matches as the base url has been accepted.
            val keyEntry = getKeyEntry(policy.baseUrl).get()
            val uri = URI(policy.baseUrl)
            var queryStringParameters: MutableList<NameValuePair> = ArrayList()
            if (uri.query != null) {
                queryStringParameters = URLEncodedUtils.parse(URI(policy.baseUrl).query, StandardCharsets.UTF_8)
            }
            queryStringParameters.addAll(URLEncodedUtils.parse(
                    ResourceRequestUtil.policyToResourceRequestQueryString(policy, keyEntry.key, keyEntry.value.key!!),
                    StandardCharsets.UTF_8))
            return URI(uri.scheme, null, uri.host, uri.port, uri.path,
                    URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8), null).toString()
        } catch (e: Exception) {
            logger.error("Unable to create signed URL because {}", ExceptionUtils.getStackTrace(e))
            throw UrlSigningException(e)
        }

    }

    companion object {
        /** The prefix for key entry configuration keys  */
        val KEY_ENTRY_PREFIX = "key"
        /** The postfix in the configuration file to define the encryption key.  */
        val SECRET = "secret"
        /** The postfix in the configuration file to define the matching url.  */
        val URL = "url"
        /** The postfix in the configuration file to define the organization owning the key.  */
        val ORGANIZATION = "organization"

        /** Value indicating that the key can be used by any organization  */
        val ANY_ORGANIZATION = "*"
    }
}
