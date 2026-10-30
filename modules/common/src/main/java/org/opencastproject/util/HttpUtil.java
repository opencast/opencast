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

package org.opencastproject.util;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Prelude.sleep;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Tuple;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Functions to support Apache httpcomponents and HTTP related operations in general. */
public final class HttpUtil {
  private HttpUtil() {
  }

  public static HttpPost post(NameValuePair... formParams) {
    final HttpPost post = new HttpPost("");
    setFormParams(post, formParams);
    return post;
  }

  public static HttpPost post(String uri, NameValuePair... formParams) {
    final HttpPost post = new HttpPost(uri);
    setFormParams(post, formParams);
    return post;
  }

  public static HttpGet get(String path, Tuple<String, String>... queryParams) {
    String queryString = Arrays.stream(queryParams)
        .map(a -> a.getA() + "=" + URLEncoder.encode(a.getB(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    String url = path + (queryString.isEmpty() ? "" : "?" + queryString);
    return new HttpGet(url);
  }

  private static void setFormParams(ClassicHttpRequest r, NameValuePair[] formParams) {
    r.setEntity(new UrlEncodedFormEntity(Arrays.asList(formParams), StandardCharsets.UTF_8));
  }

  public static NameValuePair param(String name, String value) {
    return new BasicNameValuePair(name, value);
  }

  public static boolean isOk(ClassicHttpResponse res) {
    return res.getCode() == HttpStatus.SC_OK;
  }

  /**
   * Creates a builder for a plain http client using the given timeouts.
   *
   * @param connectTimeoutMs
   *          the time in milliseconds to wait for a connection to be established
   * @param socketTimeoutMs
   *          the maximum time in milliseconds of inactivity while waiting for response data
   * @return the builder, which can be configured further before calling {@link HttpClientBuilder#build()}
   */
  public static HttpClientBuilder clientBuilder(int connectTimeoutMs, int socketTimeoutMs) {
    final Timeout connectTimeout = Timeout.ofMilliseconds(connectTimeoutMs);
    final Timeout socketTimeout = Timeout.ofMilliseconds(socketTimeoutMs);
    return HttpClientBuilder.create()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            .useSystemProperties()
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build())
            .build())
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(connectTimeout)
            .setResponseTimeout(socketTimeout)
            .build());
  }

  /**
   * Wait for a certain status of a resource.
   *
   * @return either an exception or the status code of the last http response
   */
  public static Either<Exception, Integer> waitForResource(final TrustedHttpClient http, final URI resourceUri,
          final int expectedStatus, final long timeout, final long pollingInterval) {
    long now = 0L;
    while (true) {
      final HttpHead head = new HttpHead(resourceUri);
      ClassicHttpResponse response = null;
      final int status;
      try {
        try {
          response = http.execute(head);
          status = response.getCode();
        } finally {
          http.close(response);
        }
        if (status == expectedStatus || now >= timeout) {
          return right(status);
        } else {
          if (!sleep(pollingInterval)) {
            return left(new Exception("Interrupted"));
          } else {
            now = now + pollingInterval;
          }
        }
      } catch (Exception e) {
        return left(e);
      }
    }
  }
}
