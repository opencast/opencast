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

package org.opencastproject.adopter.registration;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Contains methods for sending statistic data via rest.
 */
public class AdopterRegistrationSender {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AdopterRegistrationSender.class);

  /** The base URL for the external server where the data will be send to */
  private String baseUrl;

  // The suffixes for the base statistic server URL
  // they determine to which REST endpoint the data will be sent
  private static final String GENERAL_DATA_URL_SUFFIX = "api/1.0/adopter";
  private static final String STATISTIC_URL_SUFFIX = "api/1.0/statistic";

  private static final String EXTRA_URL_INFIX = "api/1.0";

  //================================================================================
  // Constructor
  //================================================================================

  /**
   * Simple Constructor that requires the URL of the statistic server.
   * @param statisticServerBaseUrl The URL prefix of the statistic server.
   */
  public AdopterRegistrationSender(String statisticServerBaseUrl) {
    if (!statisticServerBaseUrl.endsWith("/")) {
      statisticServerBaseUrl += "/";
    }
    this.baseUrl = statisticServerBaseUrl;
  }

  //================================================================================
  // Methods
  //================================================================================

  /**
   * Executes the 'send' method with the proper REST URL suffix.
   * @param json The data which shall be sent.
   * @throws IOException General exception that can occur while sending the data.
   */
  public void sendGeneralData(String json) throws IOException  {
    send(json, GENERAL_DATA_URL_SUFFIX);
  }

  /**
   * Deletes the adopter data
   * @param json The data which shall be sent.
   * @throws IOException General exception that can occur while sending the data.
   */
  public void deleteGeneralData(String json) throws IOException  {
    send(json, GENERAL_DATA_URL_SUFFIX, "DELETE");
  }

  /**
   * Executes the 'send' method with the proper REST URL suffix.
   * @param json The data which shall be sent.
   * @throws IOException General exception that can occur while sending the data.
   */
  public void sendStatistics(String json) throws IOException {
    send(json, STATISTIC_URL_SUFFIX);
  }

  /**
   * Executes the 'send' method with the proper REST URL suffix.
   * @param key The key for the extra data.
   * @param json The data which shall be sent.
   * @throws IOException General exception that can occur while sending the data.
   */
  public void sendExtraData(String key, String json) throws IOException {
    send(json, EXTRA_URL_INFIX + "/" + key);
  }

  /**
   * Deletes the statistics data
   * @param json The data which shall be sent.
   * @throws IOException General exception that can occur while sending the data.
   */
  public void deleteStatistics(String json) throws IOException {
    send(json, STATISTIC_URL_SUFFIX, "DELETE");
  }


  /**
   * Sends the JSON string via post request.
   * @param json The JSON string that has to be send.
   * @param urlSuffix The url suffix determines to which rest endpoint the data will be send.
   * @throws IOException General exception that can occur while processing the POST request.
   */
  private void send(String json, String urlSuffix) throws IOException {
    send(json, urlSuffix, "GET");
  }

  /**
   * Sends the JSON string via post request.
   * @param json The JSON string that has to be send.
   * @param urlSuffix The url suffix determines to which rest endpoint the data will be send.
   * @param method The HTTP method to send to the server with.  Hint: Try DELETE
   * @throws IOException General exception that can occur while processing the POST request.
   */
  private void send(String json, String urlSuffix, String method) throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().useSystemProperties().build()) {
      String url;
      try {
        url = URI.create(baseUrl + urlSuffix).toURL().toString();
      } catch (IllegalArgumentException e) {
        throw new MalformedURLException(baseUrl + urlSuffix + " is not a valid URL");
      }
      HttpEntityEnclosingRequestBase request = null;
      if ("DELETE".equals(method)) {
        request = new HttpDeleteWithEntity(url);
      } else {
        request = new HttpPost(url);
      }
      request.addHeader("Content-Type", "application/json; utf-8");
      request.addHeader("Accept", "application/json");
      request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

      HttpResponse resp = client.execute(request);
      int httpStatus = resp.getStatusLine().getStatusCode();
      boolean errorOccurred = httpStatus < 200 || httpStatus > 299;
      InputStream responseStream = resp.getEntity().getContent();

      try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
          response.append(responseLine.trim());
        }
        if (errorOccurred) {
          String errorMessage = String.format("HttpStatus: %s, HttpResponse: %s", httpStatus, response);
          throw new RuntimeException(errorMessage);
        }
      }
    }
  }

  //This custom class is here because HttpDelete doesn't accept entities, which is needed by the server
  static class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {

    public static final String METHOD_NAME = "DELETE";

    HttpDeleteWithEntity(final String uri) {
      super();
      setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
      return METHOD_NAME;
    }
  }

}
