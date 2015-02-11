/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.security.api;

import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Provides secured http client components to access to protected resources.
 */
public interface TrustedHttpClient {
  /**
   * Request runner encapsulating request resource management.
   */
  interface RequestRunner<A> {
    /**
     * Run the request and apply function <code>f</code> to its response.
     * The function may throw an exception which then gets returned as the left of the Either.
     *
     * @return the processed response of type <code>A</code> or any exception that might
     *   have occurred, either in the underlying request execution code or in the given function <code>f</code>
     */
    Either<Exception, A> run(Function<HttpResponse, A> f);
  }

  /**
   * Create a function that takes a function to handle the response of the given request returning
   * either a result or any occurred exception.
   * @param httpUriRequest
   *          The http request
   * @return <code>HttpUriRequest -> (HttpResponse -> A) -> Either Exception A</code>
   * @deprecated use {@link #runner(org.apache.http.client.methods.HttpUriRequest)}
   */
  <A> Function<Function<HttpResponse, A>, Either<Exception, A>> run(HttpUriRequest httpUriRequest);

  /**
   * Create a request runner to execute the request.
   * <p/>
   * This method replaces {@link #run(org.apache.http.client.methods.HttpUriRequest)} to avoid
   * type annotation mess. To actually run the request call the runners
   * {@linkplain org.opencastproject.security.api.TrustedHttpClient.RequestRunner#run(org.opencastproject.util.data.Function) run}
   * method.
   */
  <A> RequestRunner<A> runner(HttpUriRequest req);

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
   *
   * @param httpUriRequest
   *          The http request
   * @return the http response returned by the server
   */
  HttpResponse execute(HttpUriRequest httpUriRequest) throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
   * timeouts.
   *
   * @param httpUriRequest
   *          The http request
   * @param connectionTimeout
   *          the wait time in milliseconds at which a connection attempt will throw
   * @param socketTimeout
   *          the maximum time in milliseconds allowed between packets before this method will throw
   * @return the http response returned by the server
   */
  HttpResponse execute(HttpUriRequest httpUriRequest, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
   *
   * @param httpUriRequest
   *          The http request
   * @return the http response returned by the server
   */
  <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler) throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
   * timeouts.
   *
   * @param httpUriRequest
   *          The http request
   * @param responseHandler
   *          the response handler
   * @param connectionTimeout
   *          the wait time in milliseconds at which a connection attempt will throw
   * @param socketTimeout
   *          the maximum time in milliseconds allowed between packets before this method will throw
   *
   * @return the http response returned by the server
   */
  <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler, int connectionTimeout,
          int socketTimeout) throws TrustedHttpClientException;

  /**
   * Closes this response. The caller must call close() once finished reading the response.
   *
   * @param response
   *          The response to close
   */
  void close(HttpResponse response);
}
