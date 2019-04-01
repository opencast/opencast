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

package org.opencastproject.util

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Dictionary
import java.util.Enumeration
import java.util.Properties
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * An extension to the `Properties` class which performs the following when you call getProperty:
 *
 * 1. Checks to see if there are any variables in the property 2. If so it replaces the variable with with the first
 * match it finds in the following order - The java properties (java -Dkey=value) - The component context properties
 * (set using setBundleContext) - The properties set in the object itself - The container's environment variables This
 * class operates identically to a standard Properties object in all other respects.
 */
class XProperties : Properties() {

    /** The `BundleContext` for this properties object  */
    /**
     * Return the current `BundleContext` that's in use by this object.
     *
     * @return The current `BundleContext`
     */
    /**
     * Sets the `BundleContext` for this object. Set this to null if you wish to skip checking the context for a
     * property.
     *
     * @param ctx
     * The `BundleContext` for this instance.
     */
    @Transient
    var bundleContext: BundleContext? = null
        set(ctx) {
            field = ctx
            if (ctx != null) {
                bundle = ctx.bundle
            }
        }

    /** The [Bundle] that loaded this object  */
    @Transient
    private var bundle: Bundle? = null

    /**
     * {@inheritDoc} See the class description for more details.
     *
     * @see java.util.Properties.getProperty
     */
    override fun getProperty(key: String): String? {
        var prop: String? = getUninterpretedProperty(key)
        if (prop != null) {
            var start = prop.indexOf(START_REPLACEMENT)
            while (start != -1) {
                val end = prop!!.indexOf(END_REPLACEMENT)
                val next = prop.indexOf(START_REPLACEMENT, start + START_REPLACEMENT.length)
                if (next > 0 && next <= end) {
                    log.error("Start of next subkey before end of last subkey, unable to resolve replacements for key {}!", key)
                    return null
                }
                val subkey = prop.substring(start + START_REPLACEMENT.length, end)
                prop = findReplacement(prop, subkey)
                if (prop == null) {
                    log.error("Unable to find replacement for subkey {} in key {}, returning null!", subkey, key)
                    return null
                }
                start = prop.indexOf(START_REPLACEMENT)
            }
        }
        return prop
    }

    /**
     * Wrapper around the actual search and replace functionality. This function will value with all of the instances of
     * subkey replaced.
     *
     * @param value
     * The original string you wish to replace.
     * @param subkey
     * The substring you wish to replace. This must be the substring rather than the full variable - M2_REPO
     * rather than ${M2_REPO}
     * @return The value string with all instances of subkey replaced, or null in the case of an error.
     */
    private fun findReplacement(value: String, subkey: String?): String? {
        if (subkey == null) {
            return null
        }
        val p = Pattern.compile(START_REPLACEMENT + subkey + END_REPLACEMENT, Pattern.LITERAL)
        var replacement: String? = null

        if (System.getProperty(subkey) != null) {
            replacement = System.getProperty(subkey)
        } else if (this.getProperty(subkey) != null) {
            replacement = this.getProperty(subkey)
        } else if (this.bundleContext != null && this.bundle != null && this.bundle!!.state == Bundle.ACTIVE
                && this.bundleContext!!.getProperty(subkey) != null) {
            replacement = this.bundleContext!!.getProperty(subkey)
        } else if (System.getenv(subkey) != null) {
            replacement = System.getenv(subkey)
        }

        return if (replacement != null)
            p.matcher(value).replaceAll(Matcher.quoteReplacement(replacement))
        else
            null
    }

    /**
     * This goes through the customString provided by the client and checks for any properties that they may wish
     * substituted by ConfigurationManager properties such as ${capture.filesystem.cache.capture.url} would be replaced by
     * the actual location of the capture cache.
     *
     * @param customString
     * The String that you want to substitute the properties into
     * @return The customString with all of the ${property} it was able to substitute.
     */
    fun replacePropertiesInCustomString(customString: String?): String? {
        if (customString != null) {
            // Find all properties defined by ${someProperty} looking specifically for 1$ followed by 1{ then 1 or more not }
            // and finally 1 }
            val regEx = "\\$\\{[^\\}]+\\}"
            var workingString = String(customString)
            val pattern = Pattern.compile(regEx)
            val matcher = pattern.matcher(workingString)
            while (matcher.find()) {
                val propertyKey = matcher.group()
                // Strip off the ${} from the actual property key.
                val strippedPropertyKey = propertyKey.substring(2, propertyKey.length - 1)
                if (strippedPropertyKey != null && !StringUtils.isEmpty(strippedPropertyKey)
                        && get(strippedPropertyKey) != null) {
                    // Get the property from the XProperties collection
                    val result = get(strippedPropertyKey).toString()
                    // Replace the key with the value.
                    workingString = workingString.replace(propertyKey, result)
                }
            }
            return workingString
        } else {
            return null
        }
    }

    /**
     * Returns the value of a variable with the same priority replacement scheme as getProperty.
     *
     * @param variable
     * The variable you need the replacement for.
     * @return The value for variable.
     * @see org.opencastproject.util.XProperties.getProperty
     */
    fun expandVariable(variable: String): String? {
        return findReplacement(START_REPLACEMENT + variable + END_REPLACEMENT, variable)
    }

    /**
     * A wrapper around the old getProperty behaviour, this method does not do any variable expansion.
     *
     * @param key
     * The key of the property
     * @return The property exactly as it appears in the properties list without any variable expansion
     */
    fun getUninterpretedProperty(key: String): String {
        return super.getProperty(key)
    }

    /**
     * Merges the properties from p into this properties object
     *
     * @param p
     * The `Dictionary` you wish to add to this object
     */
    fun merge(p: Dictionary<String, String>) {
        val keys = p.keys()
        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            this[key] = p.get(key)
        }
    }

    companion object {

        private val serialVersionUID = -7497116948581078334L

        val START_REPLACEMENT = "\${"

        val END_REPLACEMENT = "}"

        /** Logging facility provided by log4j  */
        private val log = LoggerFactory.getLogger(XProperties::class.java!!)
    }
}
