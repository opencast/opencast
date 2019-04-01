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

package org.opencastproject.serviceregistry.api

import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.StatusLine
import org.apache.http.client.methods.HttpRequestBase
import org.joda.time.DateTimeConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays

/**
 * Base class serving as a convenience implementation for remote services.
 */
open class RemoteBase
/**
 * Creates a remote implementation for the given type of service.
 *
 * @param type
 * the service type
 */
protected constructor(type: String?) {

    /** The service type, used to look up remote implementations  */
    protected var serviceType: String? = null

    /** The http client to use when connecting to remote servers  */
    protected var client: TrustedHttpClient? = null

    /** The http client  */
    protected var remoteServiceManager: ServiceRegistry? = null

    init {
        if (type == null)
            throw IllegalArgumentException("Service type must not be null")
        this.serviceType = type
    }

    /**
     * Sets the trusted http client
     *
     * @param client
     */
    fun setTrustedHttpClient(client: TrustedHttpClient) {
        this.client = client
    }

    /**
     * Sets the remote service manager.
     *
     * @param remoteServiceManager
     */
    fun setRemoteServiceManager(remoteServiceManager: ServiceRegistry) {
        this.remoteServiceManager = remoteServiceManager
    }

    protected fun <A> runRequest(req: HttpRequestBase, f: Function<HttpResponse, A>): Option<A> {
        var res: HttpResponse? = null
        try {
            res = getResponse(req)
            return if (res != null) some(f.apply(res)) else Option.none()
        } finally {
            closeConnection(res)
        }
    }

    /**
     * Makes a request to all available remote services and returns the response as soon as the first of them returns the
     * expected http status code.
     *
     * @param httpRequest
     * the http request. If the URI is specified, it should include only the path beyond the service endpoint.
     * For example, a request intended for http://{host}/{service}/extra/path/info.xml should include the URI
     * "/extra/path/info.xml".
     * @param expectedHttpStatus
     * any expected status codes to include in the return.
     * @return the response object, or null if we can not connect to any services
     */
    @JvmOverloads
    protected fun getResponse(httpRequest: HttpRequestBase, vararg expectedHttpStatus: Int = HttpStatus.SC_OK): HttpResponse? {

        val maxWaitTimeMillis = System.currentTimeMillis() + DateTimeConstants.MILLIS_PER_DAY
        var warnedUnavailability = false

        // Try forever
        while (true) {

            var remoteServices: List<ServiceRegistration>? = null
            val servicesInWarningState = ArrayList<String>()
            val servicesInKnownState = ArrayList<String>()

            // Find available services
            var warned = false
            while (remoteServices == null || remoteServices.size == 0) {
                try {
                    remoteServices = remoteServiceManager!!.getServiceRegistrationsByLoad(serviceType)
                    if (remoteServices == null || remoteServices.size == 0) {
                        if (!warned) {
                            logger.warn("No services of type '{}' found, waiting...", serviceType)
                            warned = true
                        }
                        logger.debug("Still no services of type '{}' found, waiting...", serviceType)
                        try {
                            Thread.sleep(TIMEOUT.toLong())
                        } catch (e: InterruptedException) {
                            logger.warn("Interrupted while waiting for remote service of type '{}'", serviceType)
                            return null
                        }

                    }
                } catch (e: ServiceRegistryException) {
                    logger.warn("Unable to obtain a list of remote services", e)
                    return null
                }

            }

            val originalUri = httpRequest.uri
            var uriSuffix: String? = null
            if (originalUri != null && StringUtils.isNotBlank(originalUri.toString())) {
                uriSuffix = originalUri.toString()
            }

            // Try each available service
            var fullUrl: String? = null
            for (remoteService in remoteServices) {
                var response: HttpResponse? = null
                try {
                    if (uriSuffix == null) {
                        fullUrl = UrlSupport.concat(remoteService.host, remoteService.path)
                    } else {
                        fullUrl = UrlSupport.concat(*arrayOf(remoteService.host, remoteService.path, uriSuffix))
                    }

                    logger.debug("Connecting to remote service of type '{}' at {}", serviceType, fullUrl)

                    val uri = URI(fullUrl!!)
                    httpRequest.uri = uri
                    response = client!!.execute(httpRequest)
                    val status = response!!.statusLine
                    if (Arrays.asList<Int>(*expectedHttpStatus).contains(status.statusCode)) {
                        if (servicesInWarningState.contains(fullUrl) || servicesInKnownState.contains(fullUrl)) {
                            logger.warn("Service at {} is back to normal with expected status code {}", fullUrl,
                                    status.statusCode)
                        }
                        return response
                    } else {
                        if (!knownHttpStatuses.contains(status.statusCode) && !servicesInWarningState.contains(fullUrl)) {
                            logger.warn("Service at {} returned unexpected response code {}", fullUrl, status.statusCode)
                            servicesInWarningState.add(fullUrl)
                            servicesInKnownState.remove(fullUrl)
                        } else if (knownHttpStatuses.contains(status.statusCode) && !servicesInKnownState.contains(fullUrl)) {
                            logger.info("Service at {} returned known response code {}", fullUrl, status.statusCode)
                            servicesInKnownState.add(fullUrl)
                            servicesInWarningState.remove(fullUrl)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Exception while trying to dispatch job to {}: {}", fullUrl, e)
                    servicesInWarningState.add(fullUrl)
                }

                closeConnection(response)
            }

            if (servicesInKnownState.isEmpty()) {
                logger.warn("All services of type '{}' are in unknown state, abort remote call {}", serviceType, originalUri)
                return null
            }

            // Reset Original URI
            httpRequest.uri = originalUri

            // If none of them accepted the request, let's wait and retry
            if (!warnedUnavailability) {
                logger.warn("No service of type '{}' is currently readily available", serviceType)
                warnedUnavailability = true
            } else {
                logger.debug("All services of type '{}' are still unavailable", serviceType)
            }

            try {
                if (System.currentTimeMillis() > maxWaitTimeMillis) {
                    logger.warn(
                            "Still no service of type '{}' available while waiting for more than one day, abort remote call {}",
                            serviceType, originalUri)
                    return null
                }
                Thread.sleep(TIMEOUT.toLong())
            } catch (e: InterruptedException) {
                logger.warn("Interrupted while waiting for remote service of type '{}'", serviceType)
                return null
            }

        }
    }

    /**
     * Closes any http connections kept open by this http response.
     */
    protected fun closeConnection(response: HttpResponse?) {
        if (response != null)
            client!!.close(response)
    }

    /**
     * A stream wrapper that closes the http response when the stream is closed. If a remote service proxy returns an
     * inputstream, this implementation should be used to ensure that the http connection is closed properly.
     */
    inner class HttpClientClosingInputStream
    /**
     * Constructs an HttpClientClosingInputStream from a source stream and an http response.
     *
     * @throws IOException
     * @throws IllegalStateException
     */
    @Throws(IllegalStateException::class, IOException::class)
    constructor(resp: HttpResponse) : InputStream() {

        /** The input stream delivering the actual data  */
        protected var delegateStream: InputStream? = null

        /** The http response to close when the stream is closed  */
        protected var httpResponse: HttpResponse? = null

        init {
            this.delegateStream = resp.entity.content
            this.httpResponse = resp
        }

        /**
         * {@inheritDoc}
         *
         * @see java.io.InputStream.read
         */
        @Throws(IOException::class)
        override fun read(): Int {
            return delegateStream!!.read()
        }

        /**
         * {@inheritDoc}
         *
         * @see java.io.InputStream.available
         */
        @Throws(IOException::class)
        override fun available(): Int {
            return delegateStream!!.available()
        }

        /**
         * @throws IOException
         * @see java.io.InputStream.close
         */
        @Throws(IOException::class)
        override fun close() {
            delegateStream!!.close()
            closeConnection(httpResponse)
        }

        /**
         * @param readlimit
         * @see java.io.InputStream.mark
         */
        override fun mark(readlimit: Int) {
            delegateStream!!.mark(readlimit)
        }

        /**
         * @return whether this stream supports marking
         * @see java.io.InputStream.markSupported
         */
        override fun markSupported(): Boolean {
            return delegateStream!!.markSupported()
        }

        /**
         * @param b
         * the buffer into which the data is read.
         * @param off
         * the start offset in array `b` at which the data is written.
         * @param len
         * the maximum number of bytes to read.
         * @return the total number of bytes read into the buffer, or `-1` if there is no more data because the
         * end of the stream has been reached.
         * @exception IOException
         * If the first byte cannot be read for any reason other than end of file, or if the input stream has
         * been closed, or if some other I/O error occurs.
         * @exception NullPointerException
         * If `b` is `null`.
         * @exception IndexOutOfBoundsException
         * If `off` is negative, `len` is negative, or `len` is greater than
         * `b.length - off`
         * @see java.io.InputStream.read
         */
        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return delegateStream!!.read(b, off, len)
        }

        /**
         * @param b
         * the buffer into which the data is read.
         * @return the total number of bytes read into the buffer, or `-1` is there is no more data because the
         * end of the stream has been reached.
         * @exception IOException
         * If the first byte cannot be read for any reason other than the end of the file, if the input stream
         * has been closed, or if some other I/O error occurs.
         * @exception NullPointerException
         * if `b` is `null`.
         * @see java.io.InputStream.read
         */
        @Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            return delegateStream!!.read(b)
        }

        /**
         * @throws IOException
         * @see java.io.InputStream.reset
         */
        @Throws(IOException::class)
        override fun reset() {
            delegateStream!!.reset()
        }

        /**
         * @param n
         * the number of bytes to be skipped.
         * @return the actual number of bytes skipped.
         * @exception IOException
         * if the stream does not support seek, or if some other I/O error occurs.
         * @see java.io.InputStream.skip
         */
        @Throws(IOException::class)
        override fun skip(n: Long): Long {
            return delegateStream!!.skip(n)
        }

        /**
         * @return
         * @see java.lang.Object.toString
         */
        override fun toString(): String {
            return javaClass.getName() + " : " + delegateStream!!.toString()
        }
    }

    companion object {

        private val TIMEOUT = 10000

        /** The logger  */
        private val logger = LoggerFactory.getLogger(RemoteBase::class.java!!)

        /** A list of known http statuses  */
        private val knownHttpStatuses = Arrays.asList(HttpStatus.SC_SERVICE_UNAVAILABLE)


        val elementsFromHttpResponse: Function<HttpResponse, Option<List<MediaPackageElement>>> = object : Function<HttpResponse, Option<List<MediaPackageElement>>>() {
            override fun apply(response: HttpResponse): Option<List<MediaPackageElement>> {
                try {
                    val xml = IOUtils.toString(response.entity.content, Charset.forName("utf-8"))
                    val result = ArrayList(MediaPackageElementParser.getArrayFromXml(xml))
                    return some(result)
                } catch (e: Exception) {
                    logger.error("Error parsing Job from HTTP response", e)
                    return none()
                }

            }
        }
    }

}
/**
 * Makes a request to all available remote services and returns the response as soon as the first of them returns the
 * [HttpStatus.SC_OK] as the status code.
 *
 * @param httpRequest
 * the http request. If the URI is specified, it should include only the path beyond the service endpoint.
 * For example, a request intended for http://{host}/{service}/extra/path/info.xml should include the URI
 * "/extra/path/info.xml".
 * @return the response object, or null if we can not connect to any services
 */
