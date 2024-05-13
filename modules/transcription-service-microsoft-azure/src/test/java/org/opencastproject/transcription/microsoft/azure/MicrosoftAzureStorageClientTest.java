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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class MicrosoftAzureStorageClientTest {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureStorageClientTest.class);

  /**
   * Provide the Microsoft Azure storage account name as value here or
   * put the value into AZURE_STORAGE_ACCOUNT_NAME environment variable.
   */
  private String azureStorageAccountName = "mmssubtitlestorage";
  /**
   * Provide the Microsoft Azure account access key as value here or
   * put the value into AZURE_ACCOUNT_ACCESS_KEY environment variable.
   */
  private String azureAccountAccessKey = "";
  private boolean enabled;   // will be set in setUp according the values of
                             // azureStorageAccountName and azureAccountAccessKey

  private MicrosoftAzureStorageClient azureStorageClient;

  @Before
  public void setUp() throws MicrosoftAzureStorageClientException {
//    if (StringUtils.isBlank(azureAccountAccessKey)) {
//      String envValue = System.getProperty("AZURE_STORAGE_ACCOUNT_NAME");
//      if (StringUtils.isNotBlank(envValue)) {
//        azureAccountAccessKey = envValue;
//      }
//    }
//    if (StringUtils.isBlank(azureAccountAccessKey)) {
//      String envValue = System.getProperty("AZURE_ACCOUNT_ACCESS_KEY");
//      if (StringUtils.isNotBlank(envValue)) {
//        azureAccountAccessKey = envValue;
//      }
//    }
    enabled = StringUtils.isNotBlank(azureStorageAccountName)
        && StringUtils.isNotBlank(azureAccountAccessKey);
    if (!enabled) {
      return;
    }
    azureStorageClient = new MicrosoftAzureStorageClient(new MicrosoftAzureAuthorization(azureStorageAccountName,
        azureAccountAccessKey));
  }

  @Test
  public void containerExists()
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException {
    if (!enabled) {
      return;
    }
    boolean containerExists = azureStorageClient.containerExists("opencast-transcriptions");
    Assert.assertTrue(containerExists);
  }

  @Test
  public void getContainerProperties()
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotFoundException,
          MicrosoftAzureNotAllowedException {
    if (!enabled) {
      return;
    }
    Map<String, String> containerProps = azureStorageClient.getContainerProperties("opencast-transcriptions");
    Assert.assertNotNull(containerProps);
    Assert.assertFalse(containerProps.isEmpty());
    logger.info("Container properties: {}", containerProps);
  }

  @Test
  public void createContainer()
          throws MicrosoftAzureStorageClientException, IOException, MicrosoftAzureNotAllowedException {
    if (!enabled) {
      return;
    }
    azureStorageClient.createContainer("opencast-transcriptions2");
  }

  @Test
  public void uploadFile()
          throws MicrosoftAzureStorageClientException, MicrosoftAzureNotAllowedException, IOException,
          URISyntaxException {
    if (!enabled) {
      return;
    }
    URL testFileUrl = MicrosoftAzureStorageClientTest.class.getResource("/test.txt");
    File testFile = new File(testFileUrl.toURI());
    String blobUrl = azureStorageClient.uploadFile(testFile, "opencast-transcriptions", null, "test.txt");
    Assert.assertTrue("Azure storage blob URL should end with /opencast-transcriptions/test.txt",
        StringUtils.endsWithIgnoreCase(blobUrl, "/opencast-transcriptions/test.txt"));
  }

  @Test
  public void uploadFile2()
          throws MicrosoftAzureStorageClientException, MicrosoftAzureNotAllowedException, IOException,
          URISyntaxException {
    if (!enabled) {
      return;
    }
    URL testFileUrl = MicrosoftAzureStorageClientTest.class.getResource("/test.ogg");
    File testFile = new File(testFileUrl.toURI());
    String blobUrl = azureStorageClient.uploadFile(testFile, "opencast-transcriptions", null, "test.ogg");
    Assert.assertTrue("Azure storage blob URL should end with /opencast-transcriptions/test.txt",
        StringUtils.endsWithIgnoreCase(blobUrl, "/opencast-transcriptions/test.ogg"));
  }

  @Test
  public void deleteFile()
          throws MicrosoftAzureStorageClientException, MicrosoftAzureNotAllowedException, IOException,
          URISyntaxException {
    if (!enabled) {
      return;
    }
    URL testFileUrl = MicrosoftAzureStorageClientTest.class.getResource("/test.txt");
    File testFile = new File(testFileUrl.toURI());
    String blobUrl = azureStorageClient.uploadFile(testFile, "opencast-transcriptions", null, "test.txt");
    Assert.assertNotNull(blobUrl);
    azureStorageClient.deleteFile(new URL(blobUrl));
  }
}
