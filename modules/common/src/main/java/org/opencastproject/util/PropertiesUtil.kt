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

import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable
import kotlin.collections.Map.Entry
import java.util.Properties

/** Contains general purpose [Properties] utility functions.  */
object PropertiesUtil {

    /**
     * Convert the given [Properties] to a [Dictionary] of strings
     *
     * @param properties
     * the properties
     * @return the [Dictionary] of strings
     */
    fun toDictionary(properties: Properties): Dictionary<String, String> {
        val dictionary = Hashtable<String, String>()
        for ((key, value) in properties) {
            dictionary[key.toString()] = value.toString()
        }
        return dictionary
    }

    /**
     * Convert the given [Properties] to a [Map] of strings
     *
     * @param properties
     * the properties
     * @return the [Map] of strings
     */
    fun toMap(properties: Properties): Map<String, String> {
        val map = HashMap<String, String>()
        for (name in properties.stringPropertyNames())
            map[name] = properties.getProperty(name)
        return map
    }

}
