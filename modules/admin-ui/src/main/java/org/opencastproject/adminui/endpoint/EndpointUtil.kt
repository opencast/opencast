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

package org.opencastproject.adminui.endpoint

import java.lang.String.format

import org.opencastproject.adminui.exception.JsonCreationException
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl
import org.opencastproject.index.service.resources.list.query.StringListFilter
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils
import org.opencastproject.metadata.dublincore.Precision

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fx
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.SimpleSerializer

import org.json.simple.JSONArray
import org.json.simple.JSONObject

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.Date
import kotlin.collections.Map.Entry

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.StreamingOutput

object EndpointUtil {
    private val serializer = SimpleSerializer()

    val fnDay: Fn<Date, String> = object : Fn<Date, String>() {
        override fun apply(date: Date): String {
            return dateDay(date)
        }
    }

    val fnSecond: Fn<Date, String> = object : Fn<Date, String>() {
        override fun apply(date: Date): String {
            return dateSecond(date)
        }
    }

    /**
     * Create a streaming response entity. Pass it as an entity parameter to one of the response builder methods like
     * [org.opencastproject.util.RestUtil.R.ok].
     */
    fun stream(out: Fx<OutputStream>): StreamingOutput {
        return { s -> BufferedOutputStream(s).use { bs -> out.apply(bs) } }
    }

    fun ok(json: JObject): Response {
        return Response.ok(stream(serializer.fn.toJson(json)), MediaType.APPLICATION_JSON_TYPE).build()
    }

    fun notFound(msg: String, vararg args: Any): Response {
        return Response.status(Status.NOT_FOUND).entity(format(msg, *args)).type(MediaType.TEXT_PLAIN_TYPE).build()
    }

    fun dateDay(date: Date): String {
        return EncodingSchemeUtils.formatDate(date, Precision.Day)
    }

    fun dateSecond(date: Date): String {
        return EncodingSchemeUtils.formatDate(date, Precision.Second)
    }

    /**
     * Returns a generated JSON object with key-value from given list.
     *
     * Note that JSONObject (and JSON in general) does not preserve key ordering,
     * so while the Map passed to this function may have ordered keys, the resulting
     * JSONObject is not ordered.
     *
     * @param list
     * The source list for the JSON object
     * @return a JSON object containing the all the key-value as parameter
     * @throws JsonCreationException
     */
    @Throws(JsonCreationException::class)
    fun <T> generateJSONObject(list: Map<String, T>?): JSONObject {

        if (list == null) {
            throw JsonCreationException("List is null")
        }

        val jsonList = JSONObject()

        for ((key, value) in list) {
            if (value is String) {
                jsonList[key] = value
            } else if (value is JSONObject) {
                jsonList[key] = value
            } else if (value is List<*>) {
                val collection = value as Collection<*>
                val jsonArray = JSONArray()
                jsonArray.addAll(collection)
                jsonList[key] = jsonArray
            } else {
                throw JsonCreationException("Could not deal with $value")
            }
        }

        return jsonList
    }

    /**
     * Add the string based filters to the given list query.
     *
     * @param filterString
     * The string based filters
     * @param query
     * The query to update with the filters
     */
    fun addRequestFiltersToQuery(filterString: String?, query: ResourceListQueryImpl) {
        if (filterString != null) {
            val filters = filterString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (filter in filters) {
                val splitFilter = filter.split(":".toRegex(), 2).toTypedArray()
                if (splitFilter != null && splitFilter.size == 2) {
                    val key = splitFilter[0].trim { it <= ' ' }
                    val value = splitFilter[1].trim { it <= ' ' }
                    query.addFilter(StringListFilter(key, value))
                }
            }
        }
    }
}
