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

package org.opencastproject.transcription.microsoft.azure.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class MicrosoftAzureSpeechTranscription {

  // Documentation:
  // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_Get

  // CHECKSTYLE:OFF checkstyle:VisibilityModifier
  public class TranscriptionFiles {
    public String files;
  }
  public class TranscriptionSelf {
    public String self;
  }
  public String self;
  public TranscriptionSelf model;
  public TranscriptionFiles links;
  public Map<String, Object> properties;
  public TranscriptionSelf project;
  public TranscriptionSelf dataset;
  public List<String> contentUrls;
  public String contentContainerUrl;
  public String locale;
  public String displayName;
  public String description;
  public Map<String, Object> customProperties;
  public String lastActionDateTime;
  public String status;
  public String createdDateTime;

  // CHECKSTYLE:ON checkstyle:VisibilityModifier

  /** Default constructor. */
  public MicrosoftAzureSpeechTranscription() { }

  public String getID() {
    if (StringUtils.isEmpty(self)) {
      return null;
    }
    URI selfUriPath = URI.create(URI.create(self).getPath());
    String speechServiceEndpointPath = "/speechtotext/v3.1/transcriptions/";
    if (Strings.CI.startsWith(selfUriPath.getPath(), speechServiceEndpointPath)) {
      String id = StringUtils.substringAfter(selfUriPath.getPath(), speechServiceEndpointPath);
      if (!Strings.CS.contains(id, "/")) {
        return id;
      }
      return StringUtils.substringBefore(id, "/");
    }
    return null;
  }

  public boolean isFailed() {
    return Strings.CI.equals("Failed", status);
  }

  public boolean isRunning() {
    return Strings.CI.equals("Running", status);
  }

  public boolean isSucceeded() {
    return Strings.CI.equals("Succeeded", status);
  }
}
