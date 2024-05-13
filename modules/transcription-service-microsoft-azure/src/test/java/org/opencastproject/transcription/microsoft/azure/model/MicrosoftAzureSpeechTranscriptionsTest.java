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
package org.opencastproject.transcription.microsoft.azure.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class MicrosoftAzureSpeechTranscriptionsTest {

  private final String testResourcePath;
  private Gson gson;

  public MicrosoftAzureSpeechTranscriptionsTest(String testResourcePath) {
    this.testResourcePath = testResourcePath;
  }

  @Before
  public void setUp() {
    gson = new GsonBuilder().create();
  }

  @Parameterized.Parameters()
  public static List<String> data() {
    return Arrays.asList("/transcriptions.json");
  }

  @Test
  public void deserialize() throws URISyntaxException, IOException {
    String transcriptionStr = FileUtils.readFileToString(new File(MicrosoftAzureSpeechTranscriptionsTest.class
            .getResource(testResourcePath).toURI()), StandardCharsets.UTF_8);
    MicrosoftAzureSpeechTranscriptions transcriptions = gson.fromJson(transcriptionStr,
        MicrosoftAzureSpeechTranscriptions.class);
    Assert.assertNotNull(transcriptions);
  }
}
