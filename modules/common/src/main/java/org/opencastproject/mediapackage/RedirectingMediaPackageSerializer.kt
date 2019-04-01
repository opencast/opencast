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
package org.opencastproject.mediapackage

import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Collections
import java.util.Dictionary
import java.util.HashMap

/**
 * Implementation of a [MediaPackageSerializer] that will rewrite urls of a Mediapackage.
 */
class RedirectingMediaPackageSerializer : MediaPackageSerializer, ManagedService {

    /** Map containing source and destination prefixes  */
    private val redirects = HashMap<URI, URI>()

    /**
     * Returns the current set of redirects.
     *
     * @return the redirects
     */
    val redirections: Map<URI, URI>
        get() = Collections.unmodifiableMap(redirects)

    override val ranking: Int
        get() = RANKING

    /**
     * Creates a new and unconfigured package serializer that will not be able to perform any redirecting.
     */
    constructor() {}

    /**
     * Creates a new package serializer that enables rewriting of urls starting with `sourcePrefix` to strart
     * with `destintionPrefix`.
     *
     * @param sourcePrefix
     * the original url prefix
     * @param destinationPrefix
     * the new url prefix
     */
    constructor(sourcePrefix: URI, destinationPrefix: URI) {
        addRedirect(sourcePrefix, destinationPrefix)
    }

    /**
     * Adds a redirect to the set of configured redirections.
     *
     * @param sourcePrefix
     * the source prefix
     * @param destinationPrefix
     * the destination prefix
     * @throws IllegalArgumentException
     * if `sourcePrefix` or `destinationPrefix` is `null`
     * @throws IllegalStateException
     * if a redirect for `sourcePrefix` has already been configured
     */
    fun addRedirect(sourcePrefix: URI?, destinationPrefix: URI?) {
        if (sourcePrefix == null)
            throw IllegalArgumentException("Source prefix must not be null")
        if (destinationPrefix == null)
            throw IllegalArgumentException("Destination prefix must not be null")
        if (redirects.containsKey(sourcePrefix))
            throw IllegalStateException("Source prefix '$sourcePrefix' already registered")
        redirects[sourcePrefix] = destinationPrefix
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.encodeURI
     */
    @Throws(URISyntaxException::class)
    override fun encodeURI(uri: URI?): URI {
        if (uri == null)
            throw IllegalArgumentException("Argument uri is null")
        return rewrite(uri, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.decodeURI
     */
    @Throws(URISyntaxException::class)
    override fun decodeURI(uri: URI?): URI {
        if (uri == null)
            throw IllegalArgumentException("Argument uri is null")
        return rewrite(uri, true)
    }

    /**
     * This method is rewriting the URI with regards to its prefix.
     *
     * @param uri
     * the URI to rewrite
     * @param reverse
     * whether to decode instead of encode the URI
     *
     * @return the rewritten URI
     * @throws URISyntaxException
     * if the rewritten URI contains syntax errors
     */
    @Throws(URISyntaxException::class)
    private fun rewrite(uri: URI, reverse: Boolean): URI {
        var path = uri.toString()
        val variations = ArrayList<String>()
        var changed = true
        while (changed) {
            changed = false

            // Make sure we are not getting into an endless loop
            if (variations.contains(path))
                throw IllegalStateException("Rewriting of mediapackage element '$uri' experienced an endless loop")
            variations.add(path)

            // Loop over all configured redirects
            for ((key, value) in redirects) {
                val oldPrefix = if (reverse) value else key
                val newPrefix = if (reverse) key else value

                // Does the URI match the source prefix?
                val sourcePrefixString = oldPrefix.toString()
                if (!path.startsWith(sourcePrefixString))
                    continue

                // Cut off the source prefix
                path = path.substring(sourcePrefixString.length)

                // Prepend the destination prefix
                path = StringBuilder(newPrefix.toString()).append(path).toString()

                changed = true
                break
            }
        }
        return URI(path)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedService.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        if (properties == null) {
            logger.warn("Mediapackage serializer is unconfigured")
            return
        }

        // Clear the current set of redirects
        redirects.clear()

        var sourceKey: String? = null
        var destinationKey: String? = null

        var i = 1
        while (true) {

            // Create the configuration keys
            sourceKey = StringBuilder(OPT_SOURCE_PREFIX).append(".").append(i).toString()
            destinationKey = StringBuilder(OPT_DESINATION_PREFIX).append(".").append(i).toString()

            logger.debug("Looking for configuration of {} and {}", sourceKey, destinationKey)

            // Read the source and destination prefixes
            val sourcePrefixOpt = StringUtils.trimToNull(properties.get(sourceKey) as String)
            val destinationPrefixOpt = StringUtils.trimToNull(properties.get(destinationKey) as String)

            // Has the rewriter been fully configured
            if (sourcePrefixOpt == null || destinationPrefixOpt == null) {
                logger.info("Mediapackage serializer configured to transparent mode")
                break
            }

            var sourcePrefix: URI? = null
            var destinationPrefix: URI? = null

            try {
                sourcePrefix = URI(sourcePrefixOpt)
            } catch (e: URISyntaxException) {
                throw ConfigurationException(sourceKey, e.message)
            }

            // Read the source prefix
            try {
                destinationPrefix = URI(destinationPrefixOpt)
            } catch (e: URISyntaxException) {
                throw ConfigurationException(destinationKey, e.message)
            }

            // Store the redirect
            try {
                addRedirect(destinationPrefix, sourcePrefix)
                logger.info("Mediapackage serializer will rewrite element uris from starting with '{}' to start with '{}'", destinationPrefix, sourcePrefix)
            } catch (e: IllegalStateException) {
                throw ConfigurationException(sourceKey, e.message)
            }

            i++
        }

        // Has the rewriter been fully configured
        if (redirects.size == 0) {
            logger.info("Mediapackage serializer configured to transparent mode")
            return
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(RedirectingMediaPackageSerializer::class.java!!)

        /** The redirect serializer should be invoked after the default serializer  */
        val RANKING = 100

        /** Configuration option for the source prefix  */
        val OPT_SOURCE_PREFIX = "source"

        /** Configuration option for the destination prefix  */
        val OPT_DESINATION_PREFIX = "destination"
    }

}
