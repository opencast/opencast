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

package org.opencastproject.runtimeinfo.rest

import javax.ws.rs.core.MediaType

/**
 * Represents an output format for a REST endpoint.
 */
class RestFormatData
/**
 * Constructor that accepts a format name and finds the format's corresponding default URL and description. Currently
 * only JSON and XML have a default URL.
 *
 * @param name
 * the format name, the value should be a constant from [javax.ws.rs.core.MediaType](http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html) or ExtendedMediaType
 * (org.opencastproject.util.doc.rest.ExtendedMediaType).
 *
 * @throws IllegalArgumentException
 * when name is null
 */
@Throws(IllegalArgumentException::class)
constructor(
        /**
         * Name of the format, the value should be a constant from [javax.ws.rs.core.MediaType](http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html) or ExtendedMediaType (org.opencastproject.util.doc.rest.ExtendedMediaType).
         */
        /**
         * Return the name of this format.
         *
         * @return the name of this format
         */
        val name: String?) {

    /**
     * URL to a page providing more information of the format. Currently only JSON and XML have a default URL.
     */
    /**
     * Return the default URL of this format.
     *
     * @return the default URL of this format.
     */
    var url: String? = null
        private set

    init {
        if (name == null) {
            throw IllegalArgumentException("Name must not be null.")
        }
        if (name.equals(MediaType.TEXT_XML, ignoreCase = true) || name.equals(MediaType.APPLICATION_XML, ignoreCase = true)) {
            url = XML_URL
        } else if (name.equals(MediaType.APPLICATION_JSON, ignoreCase = true)) {
            url = JSON_URL
        }
    }

    /**
     * Return a string representation of this object.
     *
     * @return a string representation of this object
     */
    override fun toString(): String {
        return "$name:($url)"
    }

    companion object {
        /**
         * Default URL for the JSON format.
         */
        val JSON_URL = "http://www.json.org/"

        /**
         * Default URL for the XML format.
         */
        val XML_URL = "http://www.w3.org/XML/"
    }

}
