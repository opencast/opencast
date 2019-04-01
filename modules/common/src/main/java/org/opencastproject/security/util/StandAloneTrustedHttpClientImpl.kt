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

package org.opencastproject.security.util

import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right

import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClientException
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import org.apache.http.Header
import org.apache.http.HeaderElement
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.auth.DigestScheme
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreConnectionPNames
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

/**
 * An http client that executes secure (though not necessarily encrypted) http requests. Unlike the original
 * TrustedHttpClientImpl this version is not bound to an OSGi environment.
 */
class StandAloneTrustedHttpClientImpl(
        /** The configured username to send as part of the digest authenticated request  */
        private val user: String,
        /** The configured password to send as part of the digest authenticated request  */
        private val pass: String, nonceTimeoutRetries: Option<Int>,
        retryBaseDelay: Option<Int>, retryMaximumVariableTime: Option<Int>) : TrustedHttpClient {

    /** The number of times to retry a request after a nonce timeout.  */
    private val nonceTimeoutRetries: Int

    /** The map of open responses to their http clients, which need to be closed after we are finished with the response  */
    private val responseMap = ConcurrentHashMap<HttpResponse, HttpClient>()

    /** Used to add a random amount of time up to retryMaximumVariableTime to retry a request after a nonce timeout.  */
    private val generator = Random()

    /** The amount of time in seconds to wait until trying the request again.  */
    private val retryBaseDelay: Int

    /** The maximum amount of time in seconds to wait in addition to the RETRY_BASE_DELAY.  */
    private val retryMaximumVariableTime: Int

    init {
        this.nonceTimeoutRetries = nonceTimeoutRetries.getOrElse(DEFAULT_NONCE_TIMEOUT_RETRIES)
        this.retryBaseDelay = retryBaseDelay.getOrElse(DEFAULT_RETRY_BASE_DELAY)
        this.retryMaximumVariableTime = retryMaximumVariableTime.getOrElse(DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME)
    }

    override fun <A> run(httpUriRequest: HttpUriRequest): Function<Function<HttpResponse, A>, Either<Exception, A>> {
        return run(this, httpUriRequest)
    }

    override fun <A> runner(req: HttpUriRequest): TrustedHttpClient.RequestRunner<A> {
        return runner(this, req)
    }

    @Throws(TrustedHttpClientException::class)
    override fun execute(httpUriRequest: HttpUriRequest): HttpResponse {
        return execute(httpUriRequest, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT)
    }

    @Throws(TrustedHttpClientException::class)
    override fun execute(httpUriRequest: HttpUriRequest, connectionTimeout: Int, socketTimeout: Int): HttpResponse {
        val httpClient = DefaultHttpClient()
        httpClient.params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout)
        // Add the request header to elicit a digest auth response
        httpUriRequest.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH)

        // if (serviceRegistry != null && serviceRegistry.getCurrentJob() != null)
        // httpUriRequest.setHeader(CURRENT_JOB_HEADER, Long.toString(serviceRegistry.getCurrentJob().getId()));

        if ("GET".equals(httpUriRequest.method, ignoreCase = true) || "HEAD".equals(httpUriRequest.method, ignoreCase = true)) {
            // Set the user/pass
            val creds = UsernamePasswordCredentials(user, pass)
            httpClient.credentialsProvider.setCredentials(AuthScope.ANY, creds)

            // Run the request (the http client handles the multiple back-and-forth requests)
            var response: HttpResponse? = null
            try {
                response = HttpResponseWrapper(httpClient.execute(httpUriRequest))
                responseMap[response] = httpClient
                return response
            } catch (e: IOException) {
                // close the http connection(s)
                httpClient.connectionManager.shutdown()
                throw TrustedHttpClientException(e)
            }

        }

        // HttpClient doesn't handle the request dynamics for other verbs (especially when sending a streamed multipart
        // request), so we need to handle the details of the digest auth back-and-forth manually
        manuallyHandleDigestAuthentication(httpUriRequest, httpClient)

        var response: HttpResponse? = null
        try {
            response = HttpResponseWrapper(httpClient.execute(httpUriRequest))
            if (nonceTimeoutRetries > 0 && hadNonceTimeoutResponse(response)) {
                httpClient.connectionManager.shutdown()
                response = retryAuthAndRequestAfterNonceTimeout(httpUriRequest, response)
            }
            responseMap[response] = httpClient
            return response
        } catch (e: Exception) {
            // if we have a response, remove it from the map
            if (response != null) {
                responseMap.remove(response)
            }
            // close the http connection(s)
            httpClient.connectionManager.shutdown()
            throw TrustedHttpClientException(e)
        }

    }

    /**
     * Retries a request if the nonce timed out during the request.
     *
     * @param httpUriRequest
     * The request to be made that isn't a GET, those are handled automatically.
     * @param response
     * The response with the bad nonce timeout in it.
     * @return A new response for the request if it was successful without the nonce timing out again or just the same
     * response it got if it ran out of attempts.
     * @throws org.opencastproject.security.api.TrustedHttpClientException
     * @throws java.io.IOException
     * @throws org.apache.http.client.ClientProtocolException
     */
    @Throws(TrustedHttpClientException::class, IOException::class, ClientProtocolException::class)
    private fun retryAuthAndRequestAfterNonceTimeout(httpUriRequest: HttpUriRequest, response: HttpResponse): HttpResponse {
        var response = response
        // Get rid of old security headers with the old nonce.
        httpUriRequest.removeHeaders(AUTHORIZATION_HEADER_NAME)

        for (i in 0 until nonceTimeoutRetries) {
            val httpClient = DefaultHttpClient()
            var variableDelay = 0
            // Make sure that we have a variable delay greater than 0.
            if (retryMaximumVariableTime > 0) {
                variableDelay = generator.nextInt(retryMaximumVariableTime * MILLISECONDS_IN_SECONDS)
            }

            val totalDelay = (retryBaseDelay * MILLISECONDS_IN_SECONDS + variableDelay).toLong()
            if (totalDelay > 0) {
                logger.info("Sleeping " + totalDelay + "ms before trying request " + httpUriRequest.uri
                        + " again due to a " + response.statusLine)
                try {
                    Thread.sleep(totalDelay)
                } catch (e: InterruptedException) {
                    logger.error("Suffered InteruptedException while trying to sleep until next retry.", e)
                }

            }
            manuallyHandleDigestAuthentication(httpUriRequest, httpClient)
            response = HttpResponseWrapper(httpClient.execute(httpUriRequest))
            if (!hadNonceTimeoutResponse(response)) {
                responseMap[response] = httpClient
                break
            }
            httpClient.connectionManager.shutdown()
        }
        return response
    }

    /**
     * Determines if the nonce has timed out before a request could be performed.
     *
     * @param response
     * The response to test to see if it has timed out.
     * @return true if it has time out, false if it hasn't
     */
    private fun hadNonceTimeoutResponse(response: HttpResponse): Boolean {
        return 401 == response.statusLine.statusCode && "Nonce has expired/timed out" == response.statusLine.reasonPhrase
    }

    /**
     * Handles the necessary handshake for digest authenticaion in the case where it isn't a GET operation.
     *
     * @param httpUriRequest
     * The request location to get the digest authentication for.
     * @param httpClient
     * The client to send the request through.
     * @throws org.opencastproject.security.api.TrustedHttpClientException
     * Thrown if the client cannot be shutdown.
     */
    @Throws(TrustedHttpClientException::class)
    private fun manuallyHandleDigestAuthentication(httpUriRequest: HttpUriRequest, httpClient: HttpClient) {
        val digestRequest: HttpRequestBase
        try {
            digestRequest = httpUriRequest.javaClass.newInstance()
        } catch (e: Exception) {
            throw IllegalStateException("Can not create a new " + httpUriRequest.javaClass.getName())
        }

        digestRequest.uri = httpUriRequest.uri
        digestRequest.setHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH)
        val realmAndNonce = getRealmAndNonce(digestRequest)

        if (realmAndNonce != null) {
            // Set the user/pass
            val creds = UsernamePasswordCredentials(user, pass)

            // Set up the digest authentication with the required values
            val digestAuth = DigestScheme()
            digestAuth.overrideParamter("realm", realmAndNonce[0])
            digestAuth.overrideParamter("nonce", realmAndNonce[1])

            // Add the authentication header
            try {
                httpUriRequest.setHeader(digestAuth.authenticate(creds, httpUriRequest))
            } catch (e: Exception) {
                // close the http connection(s)
                httpClient.connectionManager.shutdown()
                throw TrustedHttpClientException(e)
            }

        }
    }

    @Throws(TrustedHttpClientException::class)
    override fun <T> execute(httpUriRequest: HttpUriRequest, responseHandler: ResponseHandler<T>, connectionTimeout: Int,
                             socketTimeout: Int): T {
        try {
            return responseHandler.handleResponse(execute(httpUriRequest, connectionTimeout, socketTimeout))
        } catch (e: IOException) {
            throw TrustedHttpClientException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.TrustedHttpClient.close
     */
    override fun close(response: HttpResponse?) {
        if (response == null) {
            logger.debug("Can not close a null response")
        } else {
            val httpClient = responseMap.remove(response)
            httpClient?.connectionManager?.shutdown()
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.security.api.TrustedHttpClient.execute
     */
    @Throws(TrustedHttpClientException::class)
    override fun <T> execute(httpUriRequest: HttpUriRequest, responseHandler: ResponseHandler<T>): T {
        return execute(httpUriRequest, responseHandler, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT)
    }

    /**
     * Perform a request, and extract the realm and nonce values
     *
     * @param request
     * The request to execute in order to obtain the realm and nonce
     * @return A String[] containing the {realm, nonce}
     */
    @Throws(TrustedHttpClientException::class)
    private fun getRealmAndNonce(request: HttpRequestBase): Array<String>? {
        val httpClient = DefaultHttpClient()
        val response: HttpResponse
        try {
            response = HttpResponseWrapper(httpClient.execute(request))
        } catch (e: IOException) {
            httpClient.connectionManager.shutdown()
            throw TrustedHttpClientException(e)
        }

        val headers = response.getHeaders("WWW-Authenticate")
        if (headers == null || headers.size == 0) {
            logger.warn("URI {} does not support digest authentication", request.uri)
            httpClient.connectionManager.shutdown()
            return null
        }
        val authRequiredResponseHeader = headers[0]
        var nonce: String? = null
        var realm: String? = null
        for (element in authRequiredResponseHeader.elements) {
            if ("nonce" == element.name) {
                nonce = element.value
            } else if ("Digest realm" == element.name) {
                realm = element.value
            }
        }
        httpClient.connectionManager.shutdown()
        return arrayOf<String>(realm, nonce)
    }

    companion object {
        /** The logger  */
        private val logger = LoggerFactory.getLogger(StandAloneTrustedHttpClientImpl::class.java!!)

        /** Header name used to request a new nonce from a server a request is sent to.  */
        val AUTHORIZATION_HEADER_NAME = "Authorization"

        val REQUESTED_AUTH_HEADER = "X-Requested-Auth"
        val DIGEST_AUTH = "Digest"

        /** The default time until a connection attempt fails  */
        val DEFAULT_CONNECTION_TIMEOUT = 60 * 1000

        /** The default time between packets that causes a connection to fail  */
        val DEFAULT_SOCKET_TIMEOUT = DEFAULT_CONNECTION_TIMEOUT

        /** The default number of times to attempt a request after it has failed due to a nonce expiring.  */
        val DEFAULT_NONCE_TIMEOUT_RETRIES = 3

        /** The number of milliseconds in a single second.  */
        private val MILLISECONDS_IN_SECONDS = 1000

        /** The default amount of time to wait after a nonce timeout.  */
        val DEFAULT_RETRY_BASE_DELAY = 300

        /** Default maximum amount of time in a random range between 0 and this value to add to the base time.  */
        val DEFAULT_RETRY_MAXIMUM_VARIABLE_TIME = 300

        fun <A> run(client: TrustedHttpClient,
                    httpUriRequest: HttpUriRequest): Function<Function<HttpResponse, A>, Either<Exception, A>> {
            return object : Function<Function<HttpResponse, A>, Either<Exception, A>>() {
                override fun apply(responseHandler: Function<HttpResponse, A>): Either<Exception, A> {
                    var response: HttpResponse? = null
                    try {
                        response = client.execute(httpUriRequest)
                        return right(responseHandler.apply(response))
                    } catch (e: Exception) {
                        return left(e)
                    } finally {
                        if (response != null) {
                            client.close(response)
                        }
                    }
                }
            }
        }

        fun <A> runner(client: TrustedHttpClient, req: HttpUriRequest): TrustedHttpClient.RequestRunner<A> {
            return TrustedHttpClient.RequestRunner { f ->
                var response: HttpResponse? = null
                try {
                    response = client.execute(req)
                    return@RequestRunner right(f.apply(response))
                } catch (e: Exception) {
                    return@RequestRunner left(e)
                } finally {
                    if (response != null) {
                        client.close(response)
                    }
                }
            }
        }
    }
}
