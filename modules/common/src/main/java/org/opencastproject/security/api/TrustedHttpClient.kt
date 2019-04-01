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

package org.opencastproject.security.api

import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpUriRequest

/**
 * Provides secured http client components to access to protected resources.
 */
interface TrustedHttpClient {
    /**
     * Request runner encapsulating request resource management.
     */
    interface RequestRunner<A> {
        /**
         * Run the request and apply function `f` to its response.
         * The function may throw an exception which then gets returned as the left of the Either.
         *
         * @return the processed response of type `A` or any exception that might
         * have occurred, either in the underlying request execution code or in the given function `f`
         */
        fun run(f: Function<HttpResponse, A>): Either<Exception, A>
    }

    /**
     * Create a function that takes a function to handle the response of the given request returning
     * either a result or any occurred exception.
     * @param httpUriRequest
     * The http request
     * @return `HttpUriRequest -> (HttpResponse -> A) -> Either Exception A`
     */
    @Deprecated("use {@link #runner(org.apache.http.client.methods.HttpUriRequest)}")
    fun <A> run(httpUriRequest: HttpUriRequest): Function<Function<HttpResponse, A>, Either<Exception, A>>

    /**
     * Create a request runner to execute the request.
     *
     *
     * This method replaces [.run] to avoid
     * type annotation mess. To actually run the request call the runners
     * [run][org.opencastproject.security.api.TrustedHttpClient.RequestRunner.run]
     * method.
     */
    fun <A> runner(req: HttpUriRequest): RequestRunner<A>

    /**
     * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
     *
     * @param httpUriRequest
     * The http request
     * @return the http response returned by the server
     */
    @Throws(TrustedHttpClientException::class)
    fun execute(httpUriRequest: HttpUriRequest): HttpResponse

    /**
     * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
     * timeouts.
     *
     * @param httpUriRequest
     * The http request
     * @param connectionTimeout
     * the wait time in milliseconds at which a connection attempt will throw
     * @param socketTimeout
     * the maximum time in milliseconds allowed between packets before this method will throw
     * @return the http response returned by the server
     */
    @Throws(TrustedHttpClientException::class)
    fun execute(httpUriRequest: HttpUriRequest, connectionTimeout: Int, socketTimeout: Int): HttpResponse

    /**
     * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
     *
     * @param httpUriRequest
     * The http request
     * @return the http response returned by the server
     */
    @Throws(TrustedHttpClientException::class)
    fun <T> execute(httpUriRequest: HttpUriRequest, responseHandler: ResponseHandler<T>): T

    /**
     * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
     * timeouts.
     *
     * @param httpUriRequest
     * The http request
     * @param responseHandler
     * the response handler
     * @param connectionTimeout
     * the wait time in milliseconds at which a connection attempt will throw
     * @param socketTimeout
     * the maximum time in milliseconds allowed between packets before this method will throw
     *
     * @return the http response returned by the server
     */
    @Throws(TrustedHttpClientException::class)
    fun <T> execute(httpUriRequest: HttpUriRequest, responseHandler: ResponseHandler<T>, connectionTimeout: Int,
                    socketTimeout: Int): T

    /**
     * Closes this response. The caller must call close() once finished reading the response.
     *
     * @param response
     * The response to close
     */
    fun close(response: HttpResponse)
}
