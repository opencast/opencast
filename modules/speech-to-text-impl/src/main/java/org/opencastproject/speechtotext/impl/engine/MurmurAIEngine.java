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

package org.opencastproject.speechtotext.impl.engine;

import org.opencastproject.speechtotext.api.SpeechToTextEngine;
import org.opencastproject.speechtotext.api.SpeechToTextEngineException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** MurmurAI implementation of the Speech-to-text engine interface. */
@Component(
    property = {
        "service.description=MurmurAI implementation of the SpeechToTextEngine interface",
        "enginetype=murmurai"
    }
)
public class MurmurAIEngine implements SpeechToTextEngine {

  private static final Logger logger = LoggerFactory.getLogger(MurmurAIEngine.class);

  /** Name of the engine. */
  private static final String engineName = "MurmurAI";

  /** Config key for setting the MurmurAI server URL. */
  private static final String MURMURAI_SERVER_URL_CONFIG_KEY = "murmurai.server.url";

  /** Currently used MurmurAI server URL. */
  private String murmuraiServerUrl = null;

  /** Config key for setting the MurmurAI API key. */
  private static final String MURMURAI_API_KEY_CONFIG_KEY = "murmurai.api.key";

  /** Currently used MurmurAI API key. */
  private String murmuraiApiKey = null;

  /** Config key for setting the polling interval in seconds. */
  private static final String MURMURAI_POLLING_INTERVAL_CONFIG_KEY = "murmurai.polling.interval";

  /** Default polling interval in seconds. */
  public static final int MURMURAI_POLLING_INTERVAL_DEFAULT = 10;

  /** Currently used polling interval in milliseconds. */
  private int pollingInterval = MURMURAI_POLLING_INTERVAL_DEFAULT * 1000;

  /** Config key for setting the maximum wait time in seconds. */
  private static final String MURMURAI_MAX_WAIT_TIME_CONFIG_KEY = "murmurai.max.wait.time";

  /** Default maximum wait time in seconds (2h = 120m = 7200s) */
  public static final int MURMURAI_MAX_WAIT_TIME_DEFAULT = 7200;

  /** Currently used maximum wait time. */
  private int maxWaitTime = MURMURAI_MAX_WAIT_TIME_DEFAULT;


  /** Configuration for HTTP requests with timeouts set to 30s */
  private final RequestConfig httpRequestConfig = RequestConfig.custom()
      .setConnectTimeout(30000)
      .setConnectionRequestTimeout(30000)
      .setSocketTimeout(30000)
      .build();

  /** Gson instance for parsing JSON. */
  private final Gson gson = new Gson();

  /** Map type for JSON parsing */
  private final Type jsonType = new TypeToken<Map<String, Object>>() { }.getType();

  @Override
  public String getEngineName() {
    return engineName;
  }

  @Activate
  @Modified
  public void activate(ComponentContext cc) {
    logger.debug("Activated/Modified MurmurAI engine service");
    var properties = cc.getProperties();
    murmuraiServerUrl = Objects.toString(properties.get(MURMURAI_SERVER_URL_CONFIG_KEY), null);
    logger.debug("Set MurmurAI server URL to {}", murmuraiServerUrl);

    murmuraiApiKey = Objects.toString(properties.get(MURMURAI_API_KEY_CONFIG_KEY), null);
    logger.debug("Set MurmurAI API key to {}", murmuraiApiKey);

    pollingInterval = NumberUtils.toInt(
        (String) properties.get(MURMURAI_POLLING_INTERVAL_CONFIG_KEY),
        MURMURAI_POLLING_INTERVAL_DEFAULT) * 1000;
    logger.debug("Set MurmurAI polling interval to {} ms", pollingInterval);

    maxWaitTime = NumberUtils.toInt(
        (String) properties.get(MURMURAI_MAX_WAIT_TIME_CONFIG_KEY),
        MURMURAI_MAX_WAIT_TIME_DEFAULT);
    logger.debug("Set MurmurAI maximum wait time to {} seconds", maxWaitTime);
    logger.debug("Finished activating/updating speech-to-text service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(File, File, String, Boolean)
   */
  @Override
  public Result generateSubtitlesFile(File mediaFile, File workingDirectory,
      String language, Boolean translate)
          throws SpeechToTextEngineException {

    if (!language.isBlank()) {
      logger.debug("MurmurAI doesn't support supplying the language. It will use auto-detection.");
    }
    if (translate) {
      logger.warn("MurmurAI doesn't support translation of subtitle files");
    }

    // Upload audio file to MurmurAI server
    String jobId;
    try {
      logger.info("Uploading audio file to MurmurAI server: {}", mediaFile.getName());
      jobId = uploadAudioFile(mediaFile);

      logger.info("Waiting for transcription job {} to complete", jobId);
      language = waitForCompletion(jobId);

      logger.info("Downloading result as WebVTT from MurmurAI server");
      var resultFile = downloadResultFile(jobId, workingDirectory);

      return new Result(language, resultFile);
    } catch (IOException | InterruptedException e) {
      throw new SpeechToTextEngineException("Transcription via MurmurAI server failed", e);
    }
  }

  /**
   * Uploads the audio file to MurmurAI server.
   *
   * @param mediaFile The audio file to upload
   * @return The job ID of the upload
   * @throws IOException If there's an error during upload
   * @throws InterruptedException If the upload is interrupted
   */
  private String uploadAudioFile(File mediaFile)
          throws IOException, InterruptedException {
    // Build the upload URL
    var uploadUrl = StringUtils.stripEnd(murmuraiServerUrl, "/") + "/v1/transcript";

    try (var httpClient = HttpClientBuilder.create().setDefaultRequestConfig(httpRequestConfig).build()) {
      var request = new HttpPost(uploadUrl);
      request.setHeader("Authorization", "Bearer " + murmuraiApiKey);
      request.setEntity(MultipartEntityBuilder.create().addBinaryBody("file", mediaFile).build());

      // Executing the POST request
      var response = httpClient.execute(request);

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Failed to upload file. Status code: " + statusCode);
      }

      // Processing the response
      String responseBody = EntityUtils.toString(response.getEntity());

      // Parse the response to extract job ID using Gson
      Map<String, Object> responseMap = gson.fromJson(responseBody, jsonType);
      if (responseMap == null || !responseMap.containsKey("id")) {
        throw new IOException("Invalid response from MurmurAI server: " + responseBody);
      }

      // return job id
      return Objects.toString(responseMap.get("id"));
    }
  }

  /**
   * Waits for the transcription job to complete.
   *
   * @param jobId The job ID to wait for
   * @return The language code of the transcription
   * @throws IOException If there's an error waiting for completion
   * @throws InterruptedException If the wait is interrupted
   */
  private String waitForCompletion(String jobId) throws IOException, InterruptedException {
    var url = StringUtils.stripEnd(murmuraiServerUrl, "/") + "/v1/transcript/" + jobId;
    var startTime = System.currentTimeMillis();
    var maxWaitTimeMs = maxWaitTime * 1000L;

    try (var httpClient = HttpClientBuilder.create().setDefaultRequestConfig(httpRequestConfig).build()) {
      while (true) {
        // Executing the GET request to get job status
        var request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + murmuraiApiKey);
        var response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Failed to get job status. Status code: " + statusCode);
        }

        // Processing the response
        var responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

        Map<String, Object> responseMap = gson.fromJson(responseBody, jsonType);
        if (responseMap == null || !responseMap.containsKey("status")) {
          throw new IOException("Invalid response from MurmurAI server: " + responseBody);
        }

        var status = (String) responseMap.get("status");
        if ("completed".equals(status)) {
          return Objects.toString(responseMap.get("language_code"), "unknown");
        }

        // Stop if in any error state
        if (!List.of("pending", "processing").contains(status)) {
          throw new IOException("Transcription job failed for job ID " + jobId + " with status: " + status);
        }

        // Check if we've exceeded maximum wait time
        if (System.currentTimeMillis() - startTime > maxWaitTimeMs) {
          throw new IOException("Transcription job timed out after " + maxWaitTime + " seconds for job ID: " + jobId);
        }

        // Wait before polling again
        Thread.sleep(pollingInterval);
      }
    }
  }

  /**
   * Downloads the result file from MurmurAI server.
   *
   * @param jobId The job ID to download the result for
   * @param workingDirectory The working directory to save the file
   * @return The downloaded result file
   * @throws IOException If there's an error during download
   */
  private File downloadResultFile(String jobId, File workingDirectory)
          throws IOException {
    var url = StringUtils.stripEnd(murmuraiServerUrl, "/") + "/v1/transcript/" + jobId + "/vtt";
    var outputFileName = "murmurai-" + jobId + ".vtt";
    var outputFile = new File(workingDirectory, outputFileName);

    // Download the result file
    try (var httpClient = HttpClientBuilder.create().setDefaultRequestConfig(httpRequestConfig).build()) {
      var request = new HttpGet(url);
      request.setHeader("Authorization", "Bearer " + murmuraiApiKey);
      var response = httpClient.execute(request);

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Failed to download WebVTT. Status code: " + statusCode);
      }

      // Write the response to the output file
      try (InputStream inputStream = response.getEntity().getContent()) {
        java.nio.file.Files.copy(inputStream, outputFile.toPath());
      }

      logger.info("Subtitle file obtained successfully: {}", outputFile);
      return outputFile;
    }
  }
}
