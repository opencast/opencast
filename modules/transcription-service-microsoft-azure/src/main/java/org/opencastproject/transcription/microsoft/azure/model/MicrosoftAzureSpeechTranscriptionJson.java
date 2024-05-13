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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MicrosoftAzureSpeechTranscriptionJson {

  // CHECKSTYLE:OFF checkstyle:LineLength

  // Documentation:
  // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_ListFiles

  // CHECKSTYLE:ON checkstyle:LineLength
  // CHECKSTYLE:OFF checkstyle:VisibilityModifier

  public String source;
  public String timestamp;
  public long durationInTicks;
  public String duration;
  public List<Map<String, Object>> combinedRecognizedPhrases;
  public List<MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases> recognizedPhrases;

  // CHECKSTYLE:ON checkstyle:VisibilityModifier

  public MicrosoftAzureSpeechTranscriptionJson() { }

  public String toSrt(float minConfidence, int maxCueLength) {
    StringBuilder sb = new StringBuilder();
    long segmentIndex = 1;
    for (MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases phrase : recognizedPhrases) {
      String[] cues = phrase.toSrt(minConfidence, maxCueLength);
      if (cues != null) {
        for (String cue : cues) {
          sb.append(String.format("\n%d\n", segmentIndex++)).append(cue);
        }
      }
    }
    return sb.toString();
  }

  public String toWebVtt(float minConfidence, int maxCueLength) {
    StringBuilder sb = new StringBuilder();
    sb.append("WEBVTT\n");
    for (MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases phrase : recognizedPhrases) {
      String[] cues = phrase.toWebVtt(minConfidence, maxCueLength);
      if (cues != null) {
        for (String cue : cues) {
          if (StringUtils.isNotBlank(cue)) {
            sb.append("\n");
            sb.append(cue);
          }
        }
      }
    }
    return sb.toString();
  }

  public Map<String, Float> getRecognizedLocales() {
    Map<String, Long> localeDurations = new HashMap<>();
    for (MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases recognizedPhrase : recognizedPhrases) {
      if (StringUtils.isNotBlank(recognizedPhrase.locale)) {
        localeDurations.put(recognizedPhrase.locale,
            localeDurations.getOrDefault(recognizedPhrase.locale, 0L) + recognizedPhrase.durationInTicks);
      }
    }
    Map<String, Float> relativeLocaleDurations = new HashMap<>();
    for (Map.Entry<String, Long> localeDuration : localeDurations.entrySet()) {
      relativeLocaleDurations.put(localeDuration.getKey(),
          localeDuration.getValue().floatValue() / Long.valueOf(durationInTicks).floatValue());
    }
    return relativeLocaleDurations;
  }

  public String getRecognizedLocale() {
    Optional<Map.Entry<String, Float>> localeOpt = getRecognizedLocales().entrySet().stream()
        .max(Map.Entry.comparingByValue());
    return localeOpt.isPresent() ? localeOpt.get().getKey() : "";
  }
}
