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

package org.opencastproject.adopter.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Contains methods for sending statistic data via rest.
 */
public class Sender {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Sender.class);

  /** The base URL for the external server where the data will be send to */
  private String baseUrl;

  // The suffixes for the base statistic server URL
  // they determine to which REST endpoint the data will be sent
  private static final String GENERAL_DATA_URL_SUFFIX = "adopter";
  private static final String STATISTIC_URL_SUFFIX = "statistic";

  //================================================================================
  // Constructor
  //================================================================================

  /**
   * Simple Constructor that requires the URL of the statistic server.
   * @param statisticServerBaseUrl The URL prefix of the statistic server.
   */
  public Sender(String statisticServerBaseUrl) {
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
   * @throws Exception General exception that can occur while sending the data.
   */
  public void sendGeneralData(String json) throws Exception {
    send(json, GENERAL_DATA_URL_SUFFIX);
  }

  /**
   * Executes the 'send' method with the proper REST URL suffix.
   * @param json The data which shall be sent.
   * @throws Exception General exception that can occur while sending the data.
   */
  public void sendStatistics(String json) throws Exception {
    send(json, STATISTIC_URL_SUFFIX);
  }

  /**
   * Sends the JSON string via post request.
   * @param json The JSON string that has to be send.
   * @param urlSuffix The url suffix determines to which rest endpoint the data will be send.
   * @throws Exception General exception that can occur while processing the POST request.
   */
  private void send(String json, String urlSuffix) throws Exception {
    try {
      URL url = new URL(baseUrl + urlSuffix);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/json; utf-8");
      con.setRequestProperty("Accept", "application/json");
      con.setDoOutput(true);

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = json.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      String httpStatus = con.getResponseCode() + "";
      boolean errorOccurred = !httpStatus.startsWith("2");
      InputStream responseStream;

      if (errorOccurred) {
        responseStream = con.getErrorStream();
      } else {
        responseStream = con.getInputStream();
      }

      try (BufferedReader br = new BufferedReader(
              new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
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

    } catch (Exception e) {
      logger.error("Error while sending JSON via POST request. The json string: {}", json, e);
      throw e;
    }
  }

}
