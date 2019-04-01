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

package org.opencastproject.dictionary.regexp

import org.opencastproject.util.ReadinessIndicator.ARTIFACT

import org.opencastproject.dictionary.api.DictionaryService
import org.opencastproject.metadata.mpeg7.Textual
import org.opencastproject.metadata.mpeg7.TextualImpl
import org.opencastproject.util.ReadinessIndicator

import org.osgi.framework.BundleContext
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.UnsupportedEncodingException
import java.util.Dictionary
import java.util.Hashtable
import java.util.LinkedList
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * This dictionary service implementation applies a pattern
 * to an input string - as many times as it matches - and
 * returns the matches, separated by a space character.
 */
class DictionaryServiceImpl : DictionaryService, ManagedService {

    /* The regular expression to use for string matching */
    var pattern = "\\w+"
        set(p) = try {
            compilesPattern = Pattern.compile(p)
            field = p
        } catch (e: RuntimeException) {
            logger.error("Failed to compile pattern '{}'", p)
        }

    /* The compiles pattern to use for matching */
    private var compilesPattern = Pattern.compile(this.pattern)

    /**
     * Load configuration
     */
    @Synchronized
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties != null && properties.get(PATTERN_CONFIG_KEY) != null) {
            var pattern = properties.get(PATTERN_CONFIG_KEY).toString()
            /* Fix special characters */
            try {
                pattern = String(pattern.toByteArray(charset("ISO-8859-1")), "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                logger.warn("Error decoding pattern string")
            }

            logger.info("Setting pattern for regexp based DictionaryService to '{}'", pattern)
            pattern = pattern
        }
    }

    /**
     * OSGi callback on component activation.
     *
     * @param  ctx  the bundle context
     */
    internal fun activate(ctx: BundleContext) {
        logger.info("Activating regexp based DictionaryService")
        val properties = Hashtable<String, String>()
        properties[ARTIFACT] = "dictionary"
        ctx.registerService(ReadinessIndicator::class.java.name,
                ReadinessIndicator(), properties)
    }

    /**
     * Filter the text according to the rules defined by the dictionary
     * implementation used. This implementation uses a regular expression to find
     * matching terms.
     *
     * @return filtered text
     */
    override fun cleanUpText(text: String): Textual {

        logger.debug("Text input: “{}”", text)
        val words = LinkedList<String>()
        val matcher = compilesPattern.matcher(text)
        while (matcher.find()) {
            words.add(matcher.group())
        }
        val result = org.apache.commons.lang3.StringUtils.join(words, " ")
        logger.debug("Resulting text: “{}”", result)
        return if ("" == result) {
            null
        } else TextualImpl(result)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(DictionaryServiceImpl::class.java)

        val PATTERN_CONFIG_KEY = "pattern"
    }

}
