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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.impl.TimeImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component(
    immediate = true,
    service = { CaptionConverter.class },
    property = {
        "service.description=IBM Watson caption converter",
        "caption.format=ibm-watson"
    }
)
public class IBMWatsonCaptionConverter implements CaptionConverter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IBMWatsonCaptionConverter.class);

  private static final int LINE_SIZE = 100;

  @Override
  public List<Caption> importCaption(InputStream inputStream, String language) throws CaptionConverterException {
    List<Caption> captionList = new ArrayList<Caption>();

    try {
      JsonObject resultsObj = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
      String jobId = "Unknown";
      if (resultsObj.get("id") != null) {
        jobId = resultsObj.get("id").getAsString();
      }

      // Log warnings
      if (resultsObj.get("warnings") != null) {
        JsonArray warningsArray = resultsObj.getAsJsonArray("warnings");
        for (JsonElement w : warningsArray) {
          logger.warn("Warning from Speech-To-Text service: {}", asText(w));
        }
      }

      JsonArray outerResultsArray = resultsObj.getAsJsonArray("results");
      JsonObject obj = outerResultsArray.get(0).getAsJsonObject();
      JsonArray resultsArray = obj.getAsJsonArray("results");

      resultsLoop:
      for (int i = 0; i < resultsArray.size(); i++) {
        JsonObject resultElement = resultsArray.get(i).getAsJsonObject();
        // Ignore results that are not final
        if (!resultElement.get("final").getAsBoolean()) {
          continue;
        }

        JsonArray alternativesArray = resultElement.getAsJsonArray("alternatives");
        if (alternativesArray != null && alternativesArray.size() > 0) {
          JsonObject alternativeElement = alternativesArray.get(0).getAsJsonObject();
          String transcript = alternativeElement.has("transcript")
              ? alternativeElement.get("transcript").getAsString()
              : null;
          if (transcript != null) {
            JsonArray timestampsArray = alternativeElement.getAsJsonArray("timestamps");
            if (timestampsArray == null || timestampsArray.size() == 0) {
              logger.warn("Could not build caption object for job {}, result index {}: timestamp data not found",
                      jobId, i);
              continue;
            }
            // Force a maximum line size of LINE_SIZE + one word
            String[] words = transcript.split("\\s+");
            StringBuffer line = new StringBuffer();
            int indexFirst = -1;
            int indexLast = -1;
            for (int j = 0; j < words.length; j++) {
              if (indexFirst == -1) {
                indexFirst = j;
              }
              line.append(words[j]);
              line.append(" ");
              if (line.length() >= LINE_SIZE || j == words.length - 1) {
                indexLast = j;
                // Create a caption
                double start = -1;
                double end = -1;
                if (indexLast < timestampsArray.size()) {
                  // Get start time of first element
                  JsonArray wordTsArray = timestampsArray.get(indexFirst).getAsJsonArray();
                  if (wordTsArray.size() == 3) {
                    start = NumberFormat.getInstance(Locale.US)
                        .parse(removeEndCharacter(asText(wordTsArray.get(1)), "s")).doubleValue();
                  }
                  // Get end time of last element
                  wordTsArray = timestampsArray.get(indexLast).getAsJsonArray();
                  if (wordTsArray.size() == 3) {
                    end = NumberFormat.getInstance(Locale.US)
                        .parse(removeEndCharacter(asText(wordTsArray.get(2)), "s")).doubleValue();
                  }
                }
                if (start == -1 || end == -1) {
                  logger.warn("Could not build caption object for job {}, result index {}: start/end times not found",
                          jobId, i);
                  continue resultsLoop;
                }

                String[] captionLines = new String[1];
                captionLines[0] = line.toString().replace("%HESITATION", "...");
                captionList.add(new CaptionImpl(buildTime((long) (start * 1000)), buildTime((long) (end * 1000)),
                        captionLines));
                indexFirst = -1;
                indexLast = -1;
                line.setLength(0);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Error when parsing IBM Watson transcriptions result", e);
      throw new CaptionConverterException(e);
    }

    return captionList;
  }

  @Override
  public void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getLanguageList(InputStream inputStream) throws CaptionConverterException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getExtension() {
    return "json";
  }

  @Override
  public Type getElementType() {
    return MediaPackageElement.Type.Attachment;
  }

  /** Render a value as plain text: the raw string for primitives, JSON otherwise. */
  private static String asText(JsonElement value) {
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }

  private Time buildTime(long ms) throws IllegalTimeFormatException {
    int h = (int) (ms / 3600000L);
    int m = (int) ((ms % 3600000L) / 60000L);
    int s = (int) ((ms % 60000L) / 1000L);
    ms = (int) (ms % 1000);

    return new TimeImpl(h, m, s, (int) ms);
  }

  private String removeEndCharacter(String str, String end) {
    if (str.endsWith(end)) {
      str = str.replace(end, "");
    }
    return str;
  }

}
