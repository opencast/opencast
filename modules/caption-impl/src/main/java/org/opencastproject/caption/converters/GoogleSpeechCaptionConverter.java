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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

public class GoogleSpeechCaptionConverter implements CaptionConverter {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechCaptionConverter.class);

  // Default transcription text line size
  private static final int LINE_SIZE = 100;

  @Override
  public List<Caption> importCaption(InputStream inputStream, String languageLineSize) throws CaptionConverterException {
    List<Caption> captionList = new ArrayList<Caption>();
    JSONParser jsonParser = new JSONParser();
    int transcriptionLineSize = 0;
    try {
      // No language to specify so define size of a transcripts line
      transcriptionLineSize = Integer.parseInt(languageLineSize.trim());
      logger.info("Transcripts line size {} used", transcriptionLineSize);
    } catch (NumberFormatException nfe) {
      transcriptionLineSize = LINE_SIZE;
      logger.info("Default transcripts line size {} used", transcriptionLineSize);
    }

    try {
      JSONObject outputObj = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream));
      String jobId = "Unknown";
      if (outputObj.get("name") != null) {
        jobId = (String) outputObj.get("name");
      }

      JSONObject responseObj = (JSONObject) outputObj.get("response");
      JSONArray resultsArray = (JSONArray) responseObj.get("results");

      resultsLoop:
      for (int i = 0; i < resultsArray.size(); i++) {
        JSONObject resultElement = (JSONObject) resultsArray.get(i);
        JSONArray alternativesArray = (JSONArray) resultElement.get("alternatives");
        if (alternativesArray != null && alternativesArray.size() > 0) {
          JSONObject alternativeElement = (JSONObject) alternativesArray.get(0);
          // remove trailing space in order to have correct transcript length
          String transcript = ((String) alternativeElement.get("transcript")).trim();
          if (transcript != null) {
            JSONArray timestampsArray = (JSONArray) alternativeElement.get("words");
            if (timestampsArray == null || timestampsArray.isEmpty()) {
              logger.warn("Could not build caption object for job {}, result index {}: timestamp data not found",
                      jobId, i);
              continue;
            }
            // Force a maximum line size of transcriptionLineSize + one word
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
              if (line.length() >= transcriptionLineSize || j == words.length - 1) {
                indexLast = j;
                // Create a caption
                double start = -1;
                double end = -1;
                if (indexLast < timestampsArray.size()) {
                  // Get start time of first element
                  JSONObject wordTSList = (JSONObject) timestampsArray.get(indexFirst);
                  if (wordTSList.size() == 3) {
                    // Remove 's' at the end
                    Number startNumber = NumberFormat.getInstance(Locale.US).parse(removeEndCharacter((wordTSList.get("startTime").toString()), "s"));
                    start = startNumber.doubleValue();
                  }
                  // Get end time of last element
                  wordTSList = (JSONObject) timestampsArray.get(indexLast);
                  if (wordTSList.size() == 3) {
                    Number endNumber = NumberFormat.getInstance(Locale.US).parse(removeEndCharacter((wordTSList.get("endTime").toString()), "s"));
                    end = endNumber.doubleValue();
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
      logger.warn("Error when parsing Google transcriptions result: {}" + e.getMessage());
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
