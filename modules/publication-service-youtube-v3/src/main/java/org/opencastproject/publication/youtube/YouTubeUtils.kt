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

package org.opencastproject.publication.youtube

import org.opencastproject.util.XProperties

import org.apache.commons.lang3.StringUtils

import java.util.Dictionary

/**
 * Supports YouTube property management.
 */
object YouTubeUtils {

    val keyPrefix = "org.opencastproject.publication.youtube."

    /**
     * Disciplined way of getting required properties.
     *
     * @param dictionary may not be `null`
     * @param key  may not be `null`
     * @param required when true, and property result is null, we throw [java.lang.IllegalArgumentException]
     * @return associated value or null
     */
    @JvmOverloads
    internal operator fun get(dictionary: XProperties, key: YouTubeKey, required: Boolean = true): String? {
        val trimmed = StringUtils.trimToNull(dictionary.getProperty(keyPrefix + key.name))
        if (required && trimmed == null) {
            throw IllegalArgumentException("Null or blank value for YouTube-related property: " + keyPrefix + key.name)
        }
        return trimmed
    }

    /**
     * Disciplined way of setting properties.
     *
     * @param dictionary may not be `null`
     * @param key  may not be `null`
     * @param value may not be `null`
     */
    internal fun put(dictionary: Dictionary<*, *>, key: YouTubeKey, value: Any) {
        dictionary.put(keyPrefix + key.name, value)
    }
}
/**
 * Disciplined way of getting properties.
 *
 * @param dictionary may not be `null`
 * @param key  may not be `null`
 * @return associated value or null
 */
