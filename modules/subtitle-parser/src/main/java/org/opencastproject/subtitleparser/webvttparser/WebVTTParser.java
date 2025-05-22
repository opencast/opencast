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
package org.opencastproject.subtitleparser.webvttparser;

import org.opencastproject.subtitleparser.SubtitleParsingException;

import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses WebVTT from a file into a datastructure to allow for easy modification.
 * Throws exceptions if the read WebVTT is invalid.
 *
 * TODO: Comments are currently ignored and discarded. Find a good way to keep comments
 *  without compromising easy editing.
 */
public class WebVTTParser {

  private static final String WEBVTT_METADATA_HEADER_STRING = "\\S*[:=]\\S*";
  private static final Pattern WEBVTT_METADATA_HEADER =
          Pattern.compile(WEBVTT_METADATA_HEADER_STRING);

  // Regex checks if not a time interval
  private static final String WEBVTT_CUE_IDENTIFIER_STRING = "^(?!.*(-->)).*$";
  private static final Pattern WEBVTT_CUE_IDENTIFIER =
          Pattern.compile(WEBVTT_CUE_IDENTIFIER_STRING);

  // Timestamp from time interval
  private static final String WEBVTT_TIMESTAMP_STRING = "(\\d+:)?[0-5]\\d:[0-5]\\d\\.\\d{3}";
  private static final Pattern WEBVTT_TIMESTAMP = Pattern.compile(WEBVTT_TIMESTAMP_STRING);

  private Charset charset; // Charset of the input files

  public WebVTTParser() {
    this.charset = StandardCharsets.UTF_8;
  }

  public WebVTTParser(Charset charset) {
    this.charset = charset;
  }

  public WebVTTSubtitle parse(InputStream is) throws IOException, SubtitleParsingException {
    // Wrap input stream into Apache Commons IO BOMInputStream
    BOMInputStream bomIn = new BOMInputStream(is, false);

    // Create subtitle object
    WebVTTSubtitle subtitle = new WebVTTSubtitle();

    // Read each line
    BufferedReader webvttReader = new BufferedReader(new InputStreamReader(bomIn, this.charset));
    String line = "";

    // File should start with "WEBVTT" on the first line
    line = webvttReader.readLine();
    if (line == null) {
      throw new SubtitleParsingException("WEBVTT Header line is null");
    }

    if (!line.startsWith("WEBVTT")) {
      throw new SubtitleParsingException("Header line did not start with WEBVTT. Got " + line);
    }

    subtitle.addHeaderLine(line);

    // While this is not mentioned in the W3C specs, it seems to be common practice to have additional lines after
    // the header containing metadata information on the file.
    while ((line = webvttReader.readLine()) != null && !line.isEmpty()) {
      subtitle.addHeaderLine(line);
    }

    // Process the cues
    while ((line = webvttReader.readLine()) != null) {
      WebVTTSubtitleCue cue = new WebVTTSubtitleCue();

      // Skip additional newlines
      if (line.isEmpty()) {
        continue;
      }

      if (line.startsWith("REGION")) {
        WebVTTSubtitleRegion region = new WebVTTSubtitleRegion();
        region.addLine(line);
        while ((line = webvttReader.readLine()) != null && !line.isEmpty()) {
          region.addLine(line);
        }
        subtitle.addRegion(region);
        continue;
      }

      if (line.startsWith("STYLE")) {
        WebVTTSubtitleStyle style = new WebVTTSubtitleStyle();
        style.addLine(line);
        while ((line = webvttReader.readLine()) != null && !line.isEmpty()) {
          style.addLine(line);
        }
        subtitle.addStyle(style);
        continue;
      }

      if (line.startsWith("NOTE")) {
        while ((line = webvttReader.readLine()) != null && !line.isEmpty()) {
          // do nothing
        }
        continue;
      }

      // Parse the cue identifier (if present)
      Matcher matcher = WEBVTT_CUE_IDENTIFIER.matcher(line);
      if (matcher.find()) {
        cue.setId(line);
        line = webvttReader.readLine();
      }

      // Parse the cue timestamps
      matcher = WEBVTT_TIMESTAMP.matcher(line);

      // Parse start timestamp
      if (!matcher.find()) {
        throw new SubtitleParsingException("Expected cue start time: " + line);
      } else {
        cue.setStartTime(parseTimestamp(matcher.group()));
      }

      // Parse end timestamp
      if (!matcher.find()) {
        throw new SubtitleParsingException("Expected cue end time: " + line);
      } else {
        cue.setEndTime(parseTimestamp(matcher.group()));
      }

      // Parse cue settings list
      String cueSettings = line.substring(matcher.end()).trim();
      if (!cueSettings.isEmpty()) {
        cue.setCueSettingsList(cueSettings);
      }


      // Parse text
      while (((line = webvttReader.readLine()) != null) && (!line.isEmpty())) {
        cue.addLine(line);
      }

      subtitle.addCue(cue);
    }

    webvttReader.close();
    is.close();

    return subtitle;
  }

  private static long parseTimestamp(String s) throws NumberFormatException {
    if (!s.matches(WEBVTT_TIMESTAMP_STRING)) {
      throw new NumberFormatException("has invalid format");
    }

    String[] parts = s.split("\\.", 2);
    long value = 0;
    for (String group : parts[0].split(":")) {
      value = value * 60 + Long.parseLong(group);
    }
    return (value * 1000 + Long.parseLong(parts[1]));
  }
}
