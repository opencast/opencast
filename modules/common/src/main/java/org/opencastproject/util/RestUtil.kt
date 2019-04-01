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

import org.opencastproject.util.Jsons.obj
import org.opencastproject.util.Jsons.p
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Tuple.tuple
import org.opencastproject.util.data.functions.Strings.split
import org.opencastproject.util.data.functions.Strings.trimToNil

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.rest.ErrorCodeException
import org.opencastproject.rest.RestConstants
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.Jsons.Obj
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Monadics
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.regex.Pattern

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/** Utility functions for REST endpoints.  */
object RestUtil {

    private val CSV_SPLIT = split(Pattern.compile(","))

    /**
     * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
     * ComponentContext.
     *
     * @param cc
     * ComponentContext to get configuration from
     * @param serverUrlKey
     * Configuration key for the server URL
     * @param servicePathKey
     * Configuration key for the service path
     * @return (serverUrl, servicePath)
     * @throws Error
     * if the service path is not configured for this component
     */
    @JvmOverloads
    fun getEndpointUrl(cc: ComponentContext, serverUrlKey: String = OpencastConstants.SERVER_URL_PROPERTY, servicePathKey: String = RestConstants.SERVICE_PATH_PROPERTY): Tuple<String, String> {
        val serverUrl = option(cc.bundleContext.getProperty(serverUrlKey)).getOrElse(
                UrlSupport.DEFAULT_BASE_URL)
        val servicePath = option(cc.properties.get(servicePathKey) as String).getOrElse(
                Option.error(RestConstants.SERVICE_PATH_PROPERTY + " property not configured"))
        return tuple(serverUrl, servicePath)
    }

    /** Create a file response.  */
    fun fileResponse(f: File, contentType: String, fileName: Option<String>): Response.ResponseBuilder {
        val b = Response.ok(f).header("Content-Type", contentType)
                .header("Content-Length", f.length())
        for (fn in fileName)
            b.header("Content-Disposition", "attachment; filename=$fn")
        return b
    }

    /** Create a file response.  */
    fun fileResponse(f: File, contentType: Option<String>, fileName: Option<String>): Response.ResponseBuilder {
        val b = Response.ok(f).header("Content-Length", f.length())
        for (t in contentType)
            b.header("Content-Type", t)
        for (fn in fileName)
            b.header("Content-Disposition", "attachment; filename=$fn")
        return b
    }

    /**
     * create a partial file response
     *
     * @param f
     * the requested file
     * @param contentType
     * the contentType to send
     * @param fileName
     * the filename to send
     * @param rangeHeader
     * the range header
     * @return the Responsebuilder
     * @throws IOException
     * if something goes wrong
     */
    @Throws(IOException::class)
    fun partialFileResponse(f: File, contentType: String, fileName: Option<String>,
                            rangeHeader: String): Response.ResponseBuilder {

        val rangeValue = rangeHeader.trim { it <= ' ' }.substring("bytes=".length)
        val fileLength = f.length()
        val start: Long
        var end: Long
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1
            start = fileLength - 1 - java.lang.Long.parseLong(rangeValue.substring("-".length))
        } else {
            val range = rangeValue.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            start = java.lang.Long.parseLong(range[0])
            end = if (range.size > 1) java.lang.Long.parseLong(range[1]) else fileLength - 1
        }
        if (end > fileLength - 1) {
            end = fileLength - 1
        }

        // send partial response status code
        val response = Response.status(206)

        if (start <= end) {
            val contentLength = end - start + 1
            response.header("Accept-Ranges", "bytes")
            response.header("Connection", "Close")
            response.header("Content-Length", contentLength.toString() + "")
            response.header("Content-Range", "bytes $start-$end/$fileLength")
            response.header("Content-Type", contentType)
            response.entity(ChunkedFileInputStream(f, start, end))
        }

        return response
    }

    /**
     * Create a stream response.
     *
     */
    @Deprecated("use\n" +
            "                {@link org.opencastproject.util.RestUtil.R#ok(java.io.InputStream, String, org.opencastproject.util.data.Option, org.opencastproject.util.data.Option)}\n" +
            "                instead")
    fun streamResponse(`in`: InputStream, contentType: String, streamLength: Option<Long>,
                       fileName: Option<String>): Response.ResponseBuilder {
        val b = Response.ok(`in`).header("Content-Type", contentType)
        for (l in streamLength)
            b.header("Content-Length", l)
        for (fn in fileName)
            b.header("Content-Disposition", "attachment; filename=$fn")
        return b
    }

    /**
     * Return JSON if `format` == json, XML else.
     *
     */
    @Deprecated("use {@link #getResponseType(String)}")
    fun getResponseFormat(format: String): MediaType {
        return if ("json".equals(format, ignoreCase = true)) MediaType.APPLICATION_JSON_TYPE else MediaType.APPLICATION_XML_TYPE
    }

    /** Return JSON if `type` == json, XML else.  */
    fun getResponseType(type: String): MediaType {
        return if ("json".equals(type, ignoreCase = true)) MediaType.APPLICATION_JSON_TYPE else MediaType.APPLICATION_XML_TYPE
    }

    /**
     * Split a comma separated request param into a list of trimmed strings discarding any blank parts.
     *
     *
     * x=comma,separated,,%20value -&gt; ["comma", "separated", "value"]
     */
    fun splitCommaSeparatedParam(param: Option<String>): Monadics.ListMonadic<String> {
        for (p in param)
            return mlist(*CSV_SPLIT.apply(p)).bind(trimToNil)
        return mlist()
    }

    fun generateErrorResponse(e: ErrorCodeException): String {
        val json = obj(p("error", obj(p("code", e.errorCode), p("message", StringUtils.trimToEmpty(e.message)))))
        return json.toJson()
    }

    /** Response builder functions.  */
    object R {

        fun ok(): Response {
            return Response.ok().build()
        }

        fun ok(entity: Any): Response {
            return Response.ok().entity(entity).build()
        }

        fun ok(entity: Boolean): Response {
            return Response.ok().entity(java.lang.Boolean.toString(entity)).build()
        }

        fun ok(json: Jsons.Obj): Response {
            return Response.ok().entity(json.toJson()).type(MediaType.APPLICATION_JSON_TYPE).build()
        }

        fun ok(job: Job): Response {
            return Response.ok().entity(JaxbJob(job)).build()
        }

        fun ok(type: MediaType, entity: Any): Response {
            return Response.ok(entity, type).build()
        }

        /**
         * Create a response with status OK from a stream.
         *
         * @param in
         * the input stream to read from
         * @param contentType
         * the content type to set the Content-Type response header to
         * @param streamLength
         * an optional value for the Content-Length response header
         * @param fileName
         * an optional file name for the Content-Disposition response header
         */
        fun ok(`in`: InputStream, contentType: String, streamLength: Option<Long>, fileName: Option<String>): Response {
            return ok(`in`, option(contentType), streamLength, fileName)
        }

        /**
         * Create a response with status OK from a stream.
         *
         * @param in
         * the input stream to read from
         * @param contentType
         * the content type to set the Content-Type response header to
         * @param streamLength
         * an optional value for the Content-Length response header
         * @param fileName
         * an optional file name for the Content-Disposition response header
         */
        fun ok(`in`: InputStream, contentType: Option<String>, streamLength: Option<Long>,
               fileName: Option<String>): Response {
            val b = Response.ok(`in`)
            for (t in contentType)
                b.header("Content-Type", t)
            for (l in streamLength)
                b.header("Content-Length", l)
            for (fn in fileName)
                b.header("Content-Disposition", "attachment; filename=$fn")
            return b.build()
        }

        fun created(location: URI): Response {
            return Response.created(location).build()
        }

        fun notFound(): Response {
            return Response.status(Response.Status.NOT_FOUND).build()
        }

        fun notFound(entity: Any): Response {
            return Response.status(Response.Status.NOT_FOUND).entity(entity).build()
        }

        fun notFound(entity: Any, type: MediaType): Response {
            return Response.status(Response.Status.NOT_FOUND).entity(entity).type(type).build()
        }

        fun locked(): Response {
            return Response.status(423).build()
        }

        fun serverError(): Response {
            return Response.serverError().build()
        }

        fun conflict(): Response {
            return Response.status(Response.Status.CONFLICT).build()
        }

        fun noContent(): Response {
            return Response.noContent().build()
        }

        fun badRequest(): Response {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        fun badRequest(msg: String): Response {
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }

        fun forbidden(): Response {
            return Response.status(Response.Status.FORBIDDEN).build()
        }

        fun forbidden(msg: String): Response {
            return Response.status(Response.Status.FORBIDDEN).entity(msg).build()
        }

        fun conflict(msg: String): Response {
            return Response.status(Response.Status.CONFLICT).entity(msg).build()
        }

        /**
         * create a partial file response
         *
         * @param f
         * the requested file
         * @param contentType
         * the contentType to send
         * @param fileName
         * the filename to send
         * @param rangeHeader
         * the range header
         * @return the Responsebuilder
         * @throws IOException
         * if something goes wrong
         */

        /**
         * Creates a precondition failed status response
         *
         * @return a precondition failed status response
         */
        fun preconditionFailed(): Response {
            return Response.status(Response.Status.PRECONDITION_FAILED).build()
        }

        /**
         * Creates a precondition failed status response with a message
         *
         * @param message
         * The message body
         * @return a precondition failed status response with a message
         */
        fun preconditionFailed(message: String): Response {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(message).build()
        }

    }
}
/**
 * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
 * ComponentContext.
 *
 * @param cc
 * ComponentContext to get configuration from
 * @return (serverUrl, servicePath)
 * @throws Error
 * if the service path is not configured for this component
 */
