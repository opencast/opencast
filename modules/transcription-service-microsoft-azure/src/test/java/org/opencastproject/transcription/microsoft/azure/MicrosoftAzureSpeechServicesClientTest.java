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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class MicrosoftAzureSpeechServicesClientTest {

  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAzureSpeechServicesClientTest.class);

  private boolean enabled;
  /**
   * Provide the Microsoft Azure Speech services endpoint as value here or
   * put the value into AZURE_SPEECH_SERVICES_ENDPOINT environment variable.
   */
  private String azureSpeechServicesEndpoint;
  /**
   * Provide the Microsoft Azure Speech services subscription kex as value here or
   * put the value into AZURE_SPEECH_SERVICES_SUBSCRIPTION_KEY environment variable.
   */
  private String azureCognitiveServicesSubscriptionKey;
  private MicrosoftAzureSpeechServicesClient azureSpeechClient;


  @Before
  public void setUp() {
    azureSpeechServicesEndpoint = System.getProperty("AZURE_SPEECH_SERVICES_ENDPOINT", "");
    azureCognitiveServicesSubscriptionKey = System.getProperty("AZURE_SPEECH_SERVICES_SUBSCRIPTION_KEY", "");
    enabled = StringUtils.isNotBlank(azureSpeechServicesEndpoint)
        && StringUtils.isNotBlank(azureCognitiveServicesSubscriptionKey);
    if (enabled) {
      logger.debug("Run tests on Azure endpoint {}", azureSpeechServicesEndpoint);
    } else {
      logger.debug("Skip tests.");
    }
    azureSpeechClient = new MicrosoftAzureSpeechServicesClient(azureSpeechServicesEndpoint,
        azureCognitiveServicesSubscriptionKey);
  }

  @Test
  public void getTranscriptions()
          throws MicrosoftAzureNotAllowedException, IOException, MicrosoftAzureSpeechClientException {
    if (!enabled) {
      return;
    }
    List<MicrosoftAzureSpeechTranscription> transcriptions = azureSpeechClient.getTranscriptions(0, 0, null);
    Assert.assertNotNull(transcriptions);
  }

  @Test
  public void getTranscription()
          throws MicrosoftAzureNotAllowedException, IOException, MicrosoftAzureSpeechClientException {
    if (!enabled) {
      return;
    }
    String transcriptionId = "09c3892c-4819-46b3-9161-9716679bdf01";
    MicrosoftAzureSpeechTranscription transcription = azureSpeechClient.getTranscriptionById(
        transcriptionId);
    Assert.assertNotNull(transcription);
    Assert.assertTrue(transcription.self.contains(transcriptionId));
  }

  @Test
  public void createTranscription()
          throws URISyntaxException, MicrosoftAzureNotAllowedException, IOException,
          MicrosoftAzureSpeechClientException, MicrosoftAzureStorageClientException {
    if (!enabled) {
      return;
    }
    String contentUrl = "https://storage.blob.core.windows.net/opencast-transcriptions/test.ogg";
    String destContainerUrl = "https://storage.blob.core.windows.net/opencast-transcriptions";
    String azureStorageAccountName = "storage";
    String azureAccountAccessKey = "access_key";
    MicrosoftAzureAuthorization azureAuthorization = new MicrosoftAzureAuthorization(azureStorageAccountName,
        azureAccountAccessKey);
    //String sasToken = azureAuthorization.generateServiceSasToken("clw", null, null, "/opencast-transcriptions", null,
    //    null, null, "c", null, null, null, null, null, null, null, null);
    //String sasToken = azureAuthorization.generateServiceSasToken("clw", null, null, "/opencast-transcriptions", "c");
    //String sasToken = azureAuthorization.generateUserDelegationSASToken("clw", null, null, "/opencast-transcriptions",
    //    null, null, null, null, null, null, null, null, null, null, null, "c", null, null, null, null, null, null,
    //    null, null);

    //String sasToken = azureAuthorization.generateUserDelegationSASToken("cw", null, null, "/opencast-transcriptions",
    //    "c");
    String sasToken = azureAuthorization.generateServiceSasToken("cw", null, null, "/opencast-transcriptions","c");

    MicrosoftAzureSpeechTranscription transcription = azureSpeechClient.createTranscription(Arrays.asList(contentUrl),
        destContainerUrl + "?" + sasToken, "Test createTranscription", "de-DE", null, "PT1H", null);
    Assert.assertNotNull(transcription);

  }
}
