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

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.Properties

/**
 * See this JAXB bug for the full explanation: https://jaxb.dev.java.net/issues/show_bug.cgi?id=223
 */
class LocalHashMap {

    /** The internal backing map  */
    protected var map: MutableMap<String, String> = HashMap()

    /** Returns the internal map storing the properties  */
    fun getMap(): Map<String, String> {
        return map
    }

    /** No-arg constructor needed by JAXB  */
    constructor() {}

    /**
     * Constructs this map from a properties list, expressed as a string:
     *
     * `
     * foo=bar
     * this=that
    ` *
     *
     * @param in
     * The properties list
     * @throws IOException
     * if parsing the string fails
     */
    @Throws(IOException::class)
    constructor(`in`: String) {
        val properties = Properties()
        properties.load(IOUtils.toInputStream(`in`, "UTF-8"))
        for ((key, value) in properties) {
            map[key] = value as String
        }
    }
}
