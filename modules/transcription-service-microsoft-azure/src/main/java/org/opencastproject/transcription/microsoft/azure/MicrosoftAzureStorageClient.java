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
package org.opencastproject.transcription.microsoft.azure;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MicrosoftAzureStorageClient {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureStorageClient.class);

  private MicrosoftAzureAuthorization azureAuthorization;

  public MicrosoftAzureStorageClient(MicrosoftAzureAuthorization azureAuthorization) {
    this.azureAuthorization = azureAuthorization;
  }

  public String getContainerUrl(String azureContainerName) {
    return String.format("https://%s.%s/%s", azureAuthorization.getAzureStorageAccountName(),
        MicrosoftAzureAuthorization.AZURE_BLOB_STORE_URL_SUFFIX,
        StringUtils.trimToEmpty(azureContainerName));
  }

  public boolean containerExists(String azureContainerName)
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException {
    try {
      Map<String, String> containerProperties = getContainerProperties(azureContainerName);
      return containerProperties.containsKey("x-ms-blob-public-access") && StringUtils.equalsIgnoreCase("unlocked",
          containerProperties.getOrDefault("x-ms-lease-status", "INVALID"));
    } catch (MicrosoftAzureNotFoundException ex) {
      return false;
    }
  }

  public Map<String, String> getContainerProperties(String azureContainerName)
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException,
          MicrosoftAzureNotFoundException {
    String containerUrl = String.format("%s?%s", getContainerUrl(azureContainerName), "restype=container");
    String sasToken = azureAuthorization.generateAccountSASToken("r", "c",
        null, null, null, null);
    containerUrl = containerUrl + "&" + sasToken;

    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpGet httpGet = new HttpGet(containerUrl);
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
        int code = response.getStatusLine().getStatusCode();
        Map<String, String> headersMap = Arrays.stream(response.getAllHeaders())
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        switch (code) {
          case HttpStatus.SC_OK: // 200
            EntityUtils.consume(response.getEntity());
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(HttpUtils.formatResponseErrorString(response, String.format(
                "Not allowed to read Azure storage container properties for container %s.",
                azureContainerName)));
          case HttpStatus.SC_NOT_FOUND: // 404
            throw new MicrosoftAzureNotFoundException(HttpUtils.formatResponseErrorString(response, String.format(
                "Azure storage container %s does not exists.", azureContainerName)));
          default:
            throw new MicrosoftAzureStorageClientException(HttpUtils.formatResponseErrorString(response, String.format(
                "Getting Azure storage container metadata failed with HTTP response code %d for container %s.",
                code, azureContainerName)));
        }
        return headersMap;
      }
    }
  }

  public void createContainer(String azureContainerName)
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException {
    if (containerExists(azureContainerName)) {
      return;
    }
    String containerUrl = String.format("%s?%s", getContainerUrl(azureContainerName), "restype=container");
    String sasToken = azureAuthorization.generateAccountSASToken("w", "c", null, null, null, null);
    containerUrl = containerUrl + "&" + sasToken;
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpPut httpPut = new HttpPut(containerUrl);
      httpPut.addHeader("x-ms-blob-public-access", "blob");
      try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
        int code = response.getStatusLine().getStatusCode();
        switch (code) {
          case HttpStatus.SC_CREATED: // 201
            EntityUtils.consume(response.getEntity());
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(HttpUtils.formatResponseErrorString(response, String.format(
                "Not allowed to read Azure storage container properties for container %s.", azureContainerName)));

          default:
            throw new MicrosoftAzureStorageClientException(HttpUtils.formatResponseErrorString(response, String.format(
                "Creating Azure storage container %s failed with HTTP response code %d.", azureContainerName, code)));
        }
      }
    }
  }

  public String uploadFile(File trackFile, String azureContainerName, String azureBlobPath, String azureBlobName)
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException {
    String containerUrl = getContainerUrl(azureContainerName);
    String blobPath = Paths.get(StringUtils.trimToEmpty(azureBlobPath), StringUtils.trimToEmpty(azureBlobName))
        .normalize().toString();
    URL blobUrl = new URL(containerUrl + "/" + blobPath);
    int blockSize = 100000000; // 100MB
    String sasToken = azureAuthorization.generateServiceSasToken("w", null, null, blobUrl.getPath(), "b");
    try (FileInputStream trackStream = new FileInputStream(trackFile)) {
      try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
        List<String> blockIds = new ArrayList<>();
        // put blocks (file chunks)
        for (int iteration = 0; iteration * blockSize < trackFile.length(); iteration++) {
          String blockId = Base64.encodeBase64String(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
          String putBlockUrl = blobUrl + "?comp=block&blockid="
              + URLEncoder.encode(blockId, StandardCharsets.UTF_8) + "&" + sasToken;
          HttpPut httpPut = new HttpPut(putBlockUrl);
          byte[] blockData = trackStream.readNBytes(blockSize);
          httpPut.setEntity(new ByteArrayEntity(blockData, ContentType.APPLICATION_OCTET_STREAM));
          try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            int code = response.getStatusLine().getStatusCode();
            switch (code) {
              case HttpStatus.SC_CREATED: // 201
                blockIds.add(blockId);
                EntityUtils.consume(response.getEntity());
                break;
              case HttpStatus.SC_FORBIDDEN: // 403
                throw new MicrosoftAzureNotAllowedException(HttpUtils.formatResponseErrorString(response, String.format(
                    "Not allowed to put block to Azure storage container %s.", azureContainerName)));
              default:
                throw new MicrosoftAzureStorageClientException(HttpUtils.formatResponseErrorString(response,
                    String.format("Putting block to Azure storage container %s failed with HTTP response code %d. ",
                        azureContainerName, code)));
            }
          }
        }
        // commit block list
        StringBuffer blockList = new StringBuffer();
        blockList.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        blockList.append("<BlockList>");
        for (String blockId : blockIds) {
          blockList.append("<Uncommitted>");
          blockList.append(blockId);
          blockList.append("</Uncommitted>");
        }
        blockList.append("</BlockList>");
        String putBlockListUrl = blobUrl + "?comp=blocklist&" + sasToken;
        HttpPut httpPut = new HttpPut(putBlockListUrl);
        httpPut.setEntity(new StringEntity(blockList.toString(),
            ContentType.create("application/xml", StandardCharsets.UTF_8)));
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
          int code = response.getStatusLine().getStatusCode();
          switch (code) {
            case HttpStatus.SC_CREATED: // 201
              EntityUtils.consume(response.getEntity());
              break;
            case HttpStatus.SC_FORBIDDEN: // 403
              throw new MicrosoftAzureNotAllowedException(HttpUtils.formatResponseErrorString(response, String.format(
                  "Not allowed to put block list to Azure storage container %s.", azureContainerName)));
            default:
              throw new MicrosoftAzureStorageClientException(HttpUtils.formatResponseErrorString(response,
                  String.format("Putting block list to Azure storage container %s failed with HTTP response code %d.",
                      azureContainerName, code)));
          }
        }
      }
    }
    return blobUrl.toString();
  }

  public void deleteFile(URL fileUrl)
          throws IOException, MicrosoftAzureNotAllowedException, MicrosoftAzureStorageClientException {
    String sasToken = azureAuthorization.generateServiceSasToken("dy", null, null, fileUrl.getPath(), "b");
    String deleteUrl = String.format("https://%s%s?%s", fileUrl.getHost(), fileUrl.getPath(), sasToken);
    try (CloseableHttpClient httpClient = HttpUtils.makeHttpClient()) {
      HttpDelete httpDelete = new HttpDelete(deleteUrl);
      try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
        int code = response.getStatusLine().getStatusCode();
        String responseString = "";
        if (response.getEntity() != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
        switch (code) {
          case HttpStatus.SC_ACCEPTED:  // 202
          case HttpStatus.SC_NOT_FOUND: // 404
            break;
          case HttpStatus.SC_FORBIDDEN: // 403
            throw new MicrosoftAzureNotAllowedException(String.format("Not allowed to delete storage blob %s. "
                + "Microsoft Azure Storage Service response: %s", httpDelete.getURI().toASCIIString(), responseString));
          default:
            throw new MicrosoftAzureStorageClientException(String.format("Deleting Azure storage blob '%s' failed "
                    + "with HTTP response code %d. Microsoft Azure Storage Service response: %s",
                httpDelete.getURI().toASCIIString(), code, responseString));
        }
      }
    }
  }
}
