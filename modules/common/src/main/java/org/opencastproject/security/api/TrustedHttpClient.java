/*
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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.security.api;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;

/**
 * Provides secured http client components to access to protected resources.
 */
public interface TrustedHttpClient {

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
   * <p>
   * The caller must call {@link #close(ClassicHttpResponse)} (or {@link ClassicHttpResponse#close()}) once finished
   * reading the response. Consider {@link #execute(ClassicHttpRequest, HttpClientResponseHandler)} instead, which
   * takes care of this.
   *
   * @param request
   *          The http request
   * @return the http response returned by the server
   */
  ClassicHttpResponse execute(ClassicHttpRequest request) throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
   * timeouts.
   *
   * @param request
   *          The http request
   * @param connectionTimeout
   *          the wait time in milliseconds at which a connection attempt will throw
   * @param socketTimeout
   *          the maximum time in milliseconds allowed between packets before this method will throw
   * @return the http response returned by the server
   */
  ClassicHttpResponse execute(ClassicHttpRequest request, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection and hands the response to
   * the given handler. The response is closed once the handler returns.
   *
   * @param request
   *          The http request
   * @param handler
   *          The handler processing the response
   * @return whatever the handler returned
   */
  <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<? extends T> handler)
          throws TrustedHttpClientException;

  /**
   * Closes this response. The caller must call close() once finished reading the response.
   *
   * @param response
   *          The response to close
   */
  void close(ClassicHttpResponse response) throws IOException;
}
