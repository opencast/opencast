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

package org.opencastproject.rest

import org.apache.commons.io.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.IOException
import java.io.InputStream
import java.io.StringWriter

/**
 * This class is used to store the results of a bulk operation on an endpoint and to easily return those results.
 */
class BulkOperationResult {

    var oks = JSONArray()
        private set
    var badRequests = JSONArray()
        private set
    var unauthorized = JSONArray()
        private set
    var notFound = JSONArray()
        private set
    var serverError = JSONArray()
        private set

    fun addOk(id: String) {
        oks.add(id)
    }

    fun addBadRequest(id: String) {
        badRequests.add(id)
    }

    fun addNotFound(id: String) {
        notFound.add(id)
    }

    fun addServerError(id: String) {
        serverError.add(id)
    }

    fun addOk(id: Long?) {
        addOk(java.lang.Long.toString(id!!))
    }

    fun addBadRequest(id: Long?) {
        addBadRequest(java.lang.Long.toString(id!!))
    }

    fun addUnauthorized(id: String) {
        unauthorized.add(id)
    }

    fun addNotFound(id: Long?) {
        addNotFound(java.lang.Long.toString(id!!))
    }

    fun addServerError(id: Long?) {
        addServerError(java.lang.Long.toString(id!!))
    }

    fun toJson(): String {
        val bulkOperationResult = JSONObject()
        bulkOperationResult[OK_KEY] = oks
        bulkOperationResult[BAD_REQUEST_KEY] = badRequests
        bulkOperationResult[NOT_FOUND_KEY] = notFound
        bulkOperationResult[UNAUTHORIZED_KEY] = unauthorized
        bulkOperationResult[ERROR_KEY] = serverError
        return bulkOperationResult.toJSONString()
    }

    @Throws(IOException::class, ParseException::class)
    fun fromJson(jsonContent: InputStream) {
        val writer = StringWriter()
        IOUtils.copy(jsonContent, writer)
        val parser = JSONParser()
        val result = parser.parse(writer.toString()) as JSONObject
        this.oks = result[OK_KEY] as JSONArray
        this.badRequests = result[BAD_REQUEST_KEY] as JSONArray
        this.notFound = result[NOT_FOUND_KEY] as JSONArray
        this.unauthorized = result[UNAUTHORIZED_KEY] as JSONArray
        this.serverError = result[ERROR_KEY] as JSONArray
    }

    companion object {
        val OK_KEY = "ok"
        val BAD_REQUEST_KEY = "badRequest"
        val UNAUTHORIZED_KEY = "unauthorized"
        val NOT_FOUND_KEY = "notFound"
        val ERROR_KEY = "error"
    }

}
