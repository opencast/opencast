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

import org.opencastproject.util.doc.rest.RestResponse

import javax.servlet.http.HttpServletResponse

/**
 * Represents a possible status result for an endpoint
 */
class StatusData
/**
 * A constructor that takes a HTTP response code and a description for this response, and an XML schema for the
 * response and constructs a StatusData object. A reference of response code constants can be found in [javax.servlet.http.HttpServletResponse](http://download.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html).
 *
 * @param code
 * a HTTP response code
 * @param description
 * a description of the response
 * @throws IllegalArgumentException
 * if code is out of range (e.g. &lt;100 or &gt;1100)
 */
@Throws(IllegalArgumentException::class)
@JvmOverloads constructor(
        /**
         * The HTTP response code.
         */
        /**
         * @return the response code of this status
         */
        val code: Int, description: String,
        /**
         * The XML schema for the response, if applicable.
         */
        /**
         * @return the xmlSchema
         */
        /**
         * @param xmlSchema
         * the xmlSchema to set
         */
        var xmlSchema: String? = null) {

    /**
     * The name of this status.
     */
    /**
     * @return a string name of this status
     */
    val name: String

    /**
     * The description for this HTTP response.
     */
    /**
     * @return a description of this status
     */
    var description: String? = null
        private set

    /**
     * A constructor that takes a RestResponse annotation type object and constructs a StatusData object.
     *
     * @param restResponse
     * a RestResponse annotation type object that stores a response code and its description
     * @throws IllegalArgumentException
     * if the response code is out of range (e.g. &lt;100 or &gt;1100)
     */
    @Throws(IllegalArgumentException::class)
    constructor(restResponse: RestResponse) : this(restResponse.responseCode, restResponse.description) {
    }

    init {
        if (code < 100 || code > 1100) {
            throw IllegalArgumentException("Code ($code) is outside of the valid range: 100-1100.")
        }
        name = findName(code)
        if (description.isEmpty()) {
            this.description = null
        } else {
            this.description = description
        }
    }

    override
            /**
             * @return a string representation of this object
             */
    fun toString(): String {
        return "SC:$code:$name"
    }

    companion object {

        /**
         * This will resolve a human readable name for all known status codes.
         *
         * @param code
         * the status code
         * @return the name OR UNKNOWN if none found
         * @throws IllegalArgumentException
         * if the code is outside the valid range
         */
        @Throws(IllegalArgumentException::class)
        fun findName(code: Int): String {
            if (code < 100 || code > 1100) {
                throw IllegalArgumentException("Code ($code) is outside of the valid range: 100-1100.")
            }

            // list from http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
            val result: String
            when (code) {
                // 1xx Informational
                HttpServletResponse.SC_CONTINUE // 100
                -> result = "Continue"
                HttpServletResponse.SC_SWITCHING_PROTOCOLS // 101
                -> result = "Switching Protocols"
                // 2xx Success
                HttpServletResponse.SC_OK // 200
                -> result = "OK"
                HttpServletResponse.SC_CREATED // 201
                -> result = "Created"
                HttpServletResponse.SC_ACCEPTED // 202
                -> result = "Accepted"
                HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION // 203
                -> result = "Non-Authoritative Information"
                HttpServletResponse.SC_NO_CONTENT // 204
                -> result = "No Content"
                HttpServletResponse.SC_RESET_CONTENT // 205
                -> result = "Reset Content"
                HttpServletResponse.SC_PARTIAL_CONTENT // 206
                -> result = "Partial Content"
                // 3xx Redirection
                HttpServletResponse.SC_MULTIPLE_CHOICES // 300
                -> result = "Multiple Choices"
                HttpServletResponse.SC_MOVED_PERMANENTLY // 301
                -> result = "Moved Permanently"
                HttpServletResponse.SC_MOVED_TEMPORARILY // 302
                -> result = "Found"
                HttpServletResponse.SC_SEE_OTHER // 303
                -> result = "See Other"
                HttpServletResponse.SC_NOT_MODIFIED // 304
                -> result = "Not Modified"
                HttpServletResponse.SC_USE_PROXY // 305
                -> result = "Use Proxy"
                HttpServletResponse.SC_TEMPORARY_REDIRECT // 307
                -> result = "Temporary Redirect"
                // 4xx Client Error
                HttpServletResponse.SC_BAD_REQUEST // 400
                -> result = "Bad Request"
                HttpServletResponse.SC_UNAUTHORIZED // 401
                -> result = "Unauthorized"
                HttpServletResponse.SC_PAYMENT_REQUIRED // 402
                -> result = "Payment Required"
                HttpServletResponse.SC_FORBIDDEN // 403
                -> result = "Forbidden"
                HttpServletResponse.SC_NOT_FOUND // 404
                -> result = "Not Found"
                HttpServletResponse.SC_METHOD_NOT_ALLOWED // 405
                -> result = "Method Not Allowed"
                HttpServletResponse.SC_NOT_ACCEPTABLE // 406
                -> result = "Not Acceptable"
                HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED // 407
                -> result = "Proxy Authentication Required"
                HttpServletResponse.SC_REQUEST_TIMEOUT // 408
                -> result = "Request Timeout"
                HttpServletResponse.SC_CONFLICT // 409
                -> result = "Conflict"
                HttpServletResponse.SC_GONE // 410
                -> result = "Gone"
                HttpServletResponse.SC_LENGTH_REQUIRED // 411
                -> result = "Length Required"
                HttpServletResponse.SC_PRECONDITION_FAILED // 412
                -> result = "Precondition Failed"
                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE // 413
                -> result = "Request Entity Too Large"
                HttpServletResponse.SC_REQUEST_URI_TOO_LONG // 414
                -> result = "Request URI Too Long"
                HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE // 415
                -> result = "Unsupported Media Type"
                HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE // 416
                -> result = "Requested Range Not Satisfiable"
                HttpServletResponse.SC_EXPECTATION_FAILED // 417
                -> result = "Expectation Failed"
                // 5xx Server Error
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR // 500
                -> result = "Internal Server Error"
                HttpServletResponse.SC_NOT_IMPLEMENTED // 501
                -> result = "Not Implemented"
                HttpServletResponse.SC_BAD_GATEWAY // 502
                -> result = "Bad Gateway"
                HttpServletResponse.SC_SERVICE_UNAVAILABLE // 503
                -> result = "Service Unavailable"
                HttpServletResponse.SC_GATEWAY_TIMEOUT // 504
                -> result = "Gateway Timeout"
                HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED // 505
                -> result = "Version Not Supported"
                else -> result = "UNKNOWN"
            }
            return result
        }
    }
}
/**
 * A constructor that takes a HTTP response code and a description for this response and constructs a StatusData
 * object. A reference of response code constants can be found in [javax.servlet.http.HttpServletResponse](http://download.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html).
 *
 * @param code
 * a HTTP response code
 * @param description
 * a description of the response
 * @throws IllegalArgumentException
 * if code is out of range (e.g. &lt;100 or &gt; 1100)
 */
