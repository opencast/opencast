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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class MicrosoftAzureSpeechTranscriptionFile {

  // CHECKSTYLE:OFF checkstyle:LineLength

  // Documentation:
  // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_ListFiles

  // CHECKSTYLE:ON checkstyle:LineLength
  // CHECKSTYLE:OFF checkstyle:VisibilityModifier

  public String self;
  public String name;

  /** Possible values:
   * DatasetReport,
   * Audio,
   * LanguageData,
   * PronunciationData,
   * AcousticDataArchive,
   * AcousticDataTranscriptionV2,
   * Transcription,
   * TranscriptionReport,
   * EvaluationDetails,
   * ModelReport
   */
  public String kind;
  public class TranscriptionFileLinks {
    public String contentUrl;
  }
  public TranscriptionFileLinks links;
  public String createdDateTime;
  public Map<String, Object> properties;

  // CHECKSTYLE:ON checkstyle:VisibilityModifier

  public MicrosoftAzureSpeechTranscriptionFile() { }

  public boolean isTranscriptionFile() {
    return StringUtils.equalsIgnoreCase("Transcription", kind);
  }
}
