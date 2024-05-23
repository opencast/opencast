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

import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscription;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionFile;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionFiles;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptionJson;
import org.opencastproject.transcription.microsoft.azure.model.MicrosoftAzureSpeechTranscriptions;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MicrosoftAzureSpeechServicesClient {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureSpeechServicesClient.class);
  private static final String WORKSPACE_COLLECTION = "azure-speech-services";
  private static final String DEFAULT_TRANSCRIPTION_TIME_TO_LIVE = "P14D";
  private final String azureSpeechServicesEndpoint;
  private final String azureCognitiveServicesSubscriptionKey;

  public MicrosoftAzureSpeechServicesClient(String azureSpeechServicesEndpoint,
      String azureCognitiveServicesSubscriptionKey) {
    this.azureSpeechServicesEndpoint = StringUtils.trimToEmpty(azureSpeechServicesEndpoint);
    this.azureCognitiveServicesSubscriptionKey = StringUtils.trimToEmpty(azureCognitiveServicesSubscriptionKey);
  }

  public List<MicrosoftAzureSpeechTranscription> getTranscriptions(int skip, int top)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    return getTranscriptions(skip, top, null);
  }

  public List<MicrosoftAzureSpeechTranscription> getTranscriptions(int skip, int top, String filter)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    // Documentation:
    // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_List
    StringBuilder url = new StringBuilder(azureSpeechServicesEndpoint + "/speechtotext/v3.1/transcriptions");
    StringBuilder params = new StringBuilder();
    if (skip > 0) {
      params.append("skip=" + skip);
    }
    if (top > 0) {
      params.append("top=" + top);
    }
    if (StringUtils.isNotBlank(filter)) {
      params.append("filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8));
    }
    if (params.length() > 0) {
      url.append("?");
      url.append(params);
    }
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpGet httpGet = new HttpGet(url.toString());
      httpGet.addHeader("Ocp-Apim-Subscription-Key", azureCognitiveServicesSubscriptionKey);
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to get transcriptions. "
                + "Microsoft Azure Speech Services response: %s", responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Getting transcriptions failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services response: %s", code, responseString));
        }
        Gson gson = new GsonBuilder().create();
        MicrosoftAzureSpeechTranscriptions transcriptions = gson.fromJson(responseString,
            MicrosoftAzureSpeechTranscriptions.class);
        return transcriptions.values;
      }
    }
  }

  public MicrosoftAzureSpeechTranscription getTranscriptionById(String transcriptionId)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    String transcriptionUrl = azureSpeechServicesEndpoint + "/speechtotext/v3.1/transcriptions/"
        + StringUtils.trimToEmpty(transcriptionId);
    return getTranscription(transcriptionUrl);
  }

  public MicrosoftAzureSpeechTranscription getTranscription(String transcriptionUrl)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    if (StringUtils.isBlank(transcriptionUrl)) {
      throw new IllegalArgumentException("Transcription URL not set.");
    }
    // Documentation:
    // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_Get
    String url = StringUtils.trimToEmpty(transcriptionUrl);
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Ocp-Apim-Subscription-Key", azureCognitiveServicesSubscriptionKey);
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to get transcription '%s'. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionUrl, responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Getting transcription '%s' failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services  response: %s", transcriptionUrl, code, responseString));
        }
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(responseString, MicrosoftAzureSpeechTranscription.class);
      }
    }
  }

  public MicrosoftAzureSpeechTranscription createTranscription(List<String> contentUrls, String destinationContainerUrl,
      String displayName , String locale, List<String> candidateLocales, String timeToLive,
      Map<String, Object> properties)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    // CHECKSTYLE:OFF checkstyle:LineLength
    // Documentation:
    // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_Create
    // https://learn.microsoft.com/en-us/azure/cognitive-services/speech-service/batch-transcription-create?pivots=rest-api
    // CHECKSTYLE:ON checkstyle:LineLength
    String url = azureSpeechServicesEndpoint  + "/speechtotext/v3.1/transcriptions";
    MicrosoftAzureSpeechTranscription requestTranscription = new MicrosoftAzureSpeechTranscription();
    // required properties
    requestTranscription.displayName = displayName;
    requestTranscription.locale = locale;
    requestTranscription.contentUrls = contentUrls;
    // optional properties
    requestTranscription.properties = new HashMap<>();
    if (properties != null && !properties.isEmpty()) {
      requestTranscription.properties.putAll(properties);
    }
    // TODO produces InvalidUri: Could not access the results container
//    if (StringUtils.isNotEmpty(destinationContainerUrl)) {
//      requestTranscription.properties.put("destinationContainerUrl", destinationContainerUrl);
//    }
    if (candidateLocales != null && !candidateLocales.isEmpty()) {
      Map<String, Object> languageIdentification = new HashMap<>();
      languageIdentification.put("candidateLocales", candidateLocales);
      requestTranscription.properties.put("languageIdentification",languageIdentification);
    }
    if (StringUtils.isNotEmpty(timeToLive)) {
      requestTranscription.properties.put("timeToLive", timeToLive);
    } else {
      requestTranscription.properties.put("timeToLive", DEFAULT_TRANSCRIPTION_TIME_TO_LIVE);
    }
    Gson gson = new GsonBuilder().create();
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpPost httpPost = new HttpPost(url);
      httpPost.addHeader("Ocp-Apim-Subscription-Key", azureCognitiveServicesSubscriptionKey);
      httpPost.setEntity(new StringEntity(gson.toJson(requestTranscription), ContentType.APPLICATION_JSON));
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
          case HttpStatus.SC_CREATED: // 201
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format(
                "Not allowed to create transcription '%s'. Microsoft Azure Speech Services response: %s",
                displayName, responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Creating transcription '%s' failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services response: %s", displayName, code, responseString));
        }
        return gson.fromJson(responseString, MicrosoftAzureSpeechTranscription.class);
      }
    }
  }

  public MicrosoftAzureSpeechTranscriptionFiles getTranscriptionFilesById(String transcriptionId)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException,
          MicrosoftAzureNotFoundException {
    String transcriptionUrl = String.format("%s/speechtotext/v3.1/transcriptions/%s/files", azureSpeechServicesEndpoint,
        StringUtils.trimToEmpty(transcriptionId));
    return getTranscriptionFiles(transcriptionUrl);
  }

  public MicrosoftAzureSpeechTranscriptionFiles getTranscriptionFiles(String transcriptionFilesUrl)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException,
          MicrosoftAzureNotFoundException {
    if (StringUtils.isBlank(transcriptionFilesUrl)) {
      throw new IllegalArgumentException("Transcription files URL not set.");
    }
    // Documentation:
    // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_Get
    String url = StringUtils.trimToEmpty(transcriptionFilesUrl);
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpGet httpGet = new HttpGet(url);
      httpGet.addHeader("Ocp-Apim-Subscription-Key", azureCognitiveServicesSubscriptionKey);
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to get transcription files '%s'. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionFilesUrl, responseString));
          case HttpStatus.SC_NOT_FOUND: // 404
            throw new MicrosoftAzureNotFoundException(String.format("Transcription files '%s' not found. "
                + "Microsoft Azure Speech Services response: %s", transcriptionFilesUrl, responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Getting transcription files '%s' failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionFilesUrl, code, responseString));
        }
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(responseString, MicrosoftAzureSpeechTranscriptionFiles.class);
      }
    }
  }

  public static MicrosoftAzureSpeechTranscriptionJson getTranscriptionJson(
      MicrosoftAzureSpeechTranscriptionFile transcriptionFile)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException,
          MicrosoftAzureNotFoundException {
    String transcriptionUrl = transcriptionFile.links.contentUrl;
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpGet httpGet = new HttpGet(transcriptionUrl);
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to get transcription file '%s'. "
                    + "Microsoft Azure Speech Services response: %s",
                transcriptionUrl, responseString));
          case HttpStatus.SC_NOT_FOUND: // 404
            throw new MicrosoftAzureNotFoundException(String.format("Transcription file '%s' not found. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionUrl, responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Getting transcription file '%s' failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionUrl, code, responseString));
        }
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(responseString, MicrosoftAzureSpeechTranscriptionJson.class);
      }
    }
  }

  public static URI writeTranscriptionFile(MicrosoftAzureSpeechTranscriptionJson transcriptionJson,
      Workspace workspace, String format, float minConfidence, int maxCueLength) throws IOException {
    boolean formatIsWebVtt;
    switch (StringUtils.lowerCase(format)) {
      case "vtt":
        formatIsWebVtt = true;
        break;
      case "srt":
        formatIsWebVtt = false;
        break;
      default:
        throw new IllegalArgumentException("format should be srt or vtt");
    }
    String content;
    if (formatIsWebVtt) {
      content = transcriptionJson.toWebVtt(minConfidence, maxCueLength);
    } else {
      content = transcriptionJson.toSrt(minConfidence, maxCueLength);
    }
    String fileName = UUID.randomUUID().toString();
    if (formatIsWebVtt) {
      fileName += ".vtt";
    } else {
      fileName += ".srt";
    }
    try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
      return workspace.putInCollection(WORKSPACE_COLLECTION, fileName, is);
    }
  }

  public void deleteTranscription(String transcriptionId)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureSpeechClientException {
    String transcriptionDeleteUrl = azureSpeechServicesEndpoint + "/speechtotext/v3.1/transcriptions/"
        + StringUtils.trimToEmpty(transcriptionId);
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpDelete httpDelete = new HttpDelete(transcriptionDeleteUrl);
      httpDelete.addHeader("Ocp-Apim-Subscription-Key", azureCognitiveServicesSubscriptionKey);
      try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_OK: // 200
          case HttpStatus.SC_NO_CONTENT: // 204
          case HttpStatus.SC_NOT_FOUND: // 404
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to delete transcription '%s'. "
                    + "Microsoft Azure Speech Services response: %s",
                transcriptionId, responseString));
          default:
            throw new MicrosoftAzureSpeechClientException(String.format(
                "Deleting transcription '%s' failed with HTTP response code %d. "
                    + "Microsoft Azure Speech Services response: %s", transcriptionId, code, responseString));
        }
      }
    }
  }
}
