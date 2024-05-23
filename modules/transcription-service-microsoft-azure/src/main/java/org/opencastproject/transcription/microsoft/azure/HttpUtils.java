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

package org.opencastproject.transcription.microsoft.azure;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public final class HttpUtils {
  private static final int CONNECTION_TIMEOUT = 1000 * 60;
  private static final int SOCKET_TIMEOUT = 1000 * 300;

  private HttpUtils() { }
  public static CloseableHttpClient makeHttpClient() {
    return makeHttpClient(CONNECTION_TIMEOUT, SOCKET_TIMEOUT, CONNECTION_TIMEOUT);
  }

  public static CloseableHttpClient makeHttpClient(int conectionTimeout, int socketTimeout,
      int connectionRequestTimeout) {
    RequestConfig reqConfig = RequestConfig.custom().setConnectTimeout(conectionTimeout)
        .setSocketTimeout(socketTimeout)
        .setConnectionRequestTimeout(connectionRequestTimeout)
        .build();
    CloseableHttpClient httpClient = HttpClientBuilder.create()
        .useSystemProperties()
        .setDefaultRequestConfig(reqConfig)
        .build();
    return httpClient;
  }

  public static String formatResponseErrorString(HttpResponse response, String message) throws IOException {
    String responseString = "";
    if (response.getEntity() != null) {
      responseString = EntityUtils.toString(response.getEntity());
    }
    if (StringUtils.isNotBlank(message)) {
      return String.format("%s Microsoft response: %s", message, responseString);
    } else {
      return String.format("Microsoft response: %s", responseString);
    }
  }
}
