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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

/**
 * Provides secured http client components to access to protected resources.
 */
public interface TrustedHttpClient {

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
   * Closes this response. The caller must call close() once finished reading the response.
   *
   * @param response
   *          The response to close
   */
  void close(HttpResponse response) throws IOException;
}
