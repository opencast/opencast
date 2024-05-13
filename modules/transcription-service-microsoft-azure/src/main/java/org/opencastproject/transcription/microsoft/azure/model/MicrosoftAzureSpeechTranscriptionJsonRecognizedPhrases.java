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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

public class MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases {

  // CHECKSTYLE:OFF checkstyle:LineLength

  // Documentation:
  // https://eastus.dev.cognitive.microsoft.com/docs/services/speech-to-text-api-v3-1/operations/Transcriptions_ListFiles

  // CHECKSTYLE:ON checkstyle:LineLength
  // CHECKSTYLE:OFF checkstyle:VisibilityModifier

  public String recognitionStatus;
  public int channel;
  public String offset;
  public String duration;
  public long offsetInTicks;
  public long durationInTicks;
  public List<MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrase> nBest;
  public String locale;

  // CHECKSTYLE:ON checkstyle:VisibilityModifier

  public MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrases() { }

  public String[] toSrt(float minConfidence, int maxCueLength) {
    String text = getBestRecognizedText(minConfidence);
    String[] cueText = splitCueText(text, maxCueLength);
    return timestampCues(false, cueText);
  }

  public String[] toWebVtt(float minConfidence, int maxCueLength) {
    String text = getBestRecognizedText(minConfidence);
    String[] cueText = splitCueText(text, maxCueLength);
    return timestampCues(true, cueText);
  }

  String[] timestampCues(boolean formatWebVtt, String[] cueText) {
    long ticksPerMillisecond = 10000;
    String format;
    if (formatWebVtt) {
      format = "HH:mm:ss.SSS";
    } else {
      // SRT format requires ',' as decimal separator rather than '.'.
      format = "HH:mm:ss,SSS";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    // If we don't do this, the time is adjusted for our local time zone, which we don't want.
    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    int cueTextLength = 0;
    int[] cuesTextLenth = new int[cueText.length];
    for (int i = 0; i < cueText.length; i++) {
      cuesTextLenth[i] = StringUtils.length(cueText[i]);
      cueTextLength += cuesTextLenth[i];
    }
    String[] result = new String[cueText.length];
    long cueOffsetInTicks = 0;
    for (int i = 0; i < cueText.length; i++) {
      long cueLengthInTicks = (long)Math.ceil((double)durationInTicks * (double)cuesTextLenth[i]
          / (double)cueTextLength);

      Date startTime = new Date((offsetInTicks + cueOffsetInTicks) / ticksPerMillisecond);
      Date endTime = new Date((offsetInTicks  + cueOffsetInTicks + cueLengthInTicks) / ticksPerMillisecond);
      cueOffsetInTicks += cueLengthInTicks;
      result[i] = String.format("%s --> %s\n%s\n", formatter.format(startTime), formatter.format(endTime), cueText[i]);
    }
    return result;
  }

  public String getBestRecognizedText(float minConfidence) {
    if (nBest == null) {
      return null;
    }
    Optional<MicrosoftAzureSpeechTranscriptionJsonRecognizedPhrase> bestPhrase;
    if (minConfidence >= 0 && minConfidence < 1) {
      bestPhrase = nBest.stream()
          .filter(phrase -> phrase.confidence >= minConfidence)
          .sorted((t1, t2) -> Float.compare(t2.confidence, t1.confidence))  // descendant order
          .findFirst();
    } else if (minConfidence >= 1) {
      bestPhrase = nBest.stream().findFirst();
    } else {
      bestPhrase = nBest.stream()
          .sorted((t1, t2) -> Float.compare(t2.confidence, t1.confidence))  // descendant order
          .findFirst();
    }
    return bestPhrase.isPresent() ? bestPhrase.get().display : "";
  }

  public static String[] splitCueText(String text, int maxCueLength) {
    int textLength = StringUtils.length(text);
    if (textLength == 0) {
      return new String[0];
    } else if (textLength <= maxCueLength) {
      return new String[] { text };
    }
    List<String> result = new ArrayList<>();
    int start = 0;
    do {
      if (textLength - start <= maxCueLength) {
        result.add(StringUtils.trimToEmpty(StringUtils.substring(text, start, textLength)));
        break;
      }
      int end = StringUtils.lastIndexOf(text, " ", start + maxCueLength);
      if (start >= end) {
        end = Math.min(textLength, start + maxCueLength);
      }
      result.add(StringUtils.trimToEmpty(StringUtils.substring(text, start, end)));
      start = end;
    } while (start < textLength);
    return result.toArray(new String[0]);
  }
}
