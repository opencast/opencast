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

import org.opencastproject.util.doc.DocData

import java.util.Collections
import java.util.Vector

/**
 * Represents a group of endpoints.
 */
class RestEndpointHolderData
/**
 * @param name
 * name of this endpoint holder
 * @param title
 * title of this endpoint holder (to be shown on the documentation page)
 * @throws IllegalArgumentException
 * if name is null, name is not alphanumeric or title is null
 */
@Throws(IllegalArgumentException::class)
constructor(
        /**
         * Name of this group of endpoints.
         */
        /**
         * Gets the name of this endpoint holder.
         *
         * @return the name of this endpoint holder
         */
        val name: String,
        /**
         * Title of this group of endpoints to be shown on the documentation page.
         */
        /**
         * Gets the title of this endpoint holder.
         *
         * @return the title of this endpoint holder
         */
        val title: String?) {

    /**
     * List of endpoints in this group.
     */
    private var endpoints: MutableList<RestEndpointData>? = null

    init {
        if (!DocData.isValidName(name)) {
            throw IllegalArgumentException("Name must not be null and must be alphanumeric.")
        }
        if (title == null) {
            throw IllegalArgumentException("Title must not be null.")
        }
    }

    /**
     * Add an endpoint to this holder and make sure the endpoints are sorted by their names.
     *
     * @param endpoint
     * an endpoint to be added to this holder
     */
    fun addEndPoint(endpoint: RestEndpointData?) {
        if (endpoint != null) {
            if (endpoints == null) {
                endpoints = Vector()
            }
            endpoints!!.add(endpoint)
            Collections.sort(endpoints!!)
        }
    }

    /**
     * Return a string representation of this RestEndpointHolderData object.
     *
     * @return a string representation of this RestEndpointHolderData object
     */
    override fun toString(): String {
        return "HOLD:$name:$endpoints"
    }

    /**
     * Returns a copy of this RestEndpointHolderData object.
     *
     * @return a copy of this RestEndpointHolderData object
     */
    fun duplicate(): RestEndpointHolderData {
        return RestEndpointHolderData(name, title)
    }

    /**
     * Returns a copy of this RestEndpointHolderData object.
     *
     * @return a copy of this RestEndpointHolderData object
     */
    protected fun clone(): Any {
        return duplicate()
    }

    /**
     * Gets the list of endpoints in this endpoint holder.
     *
     * @return the list of endpoints in this endpoint holder
     */
    fun getEndpoints(): List<RestEndpointData> {
        if (endpoints == null) {
            endpoints = Vector(0)
        }
        return endpoints
    }

}
