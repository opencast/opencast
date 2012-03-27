/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
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
import org.opencastproject.caption.util.TimeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Converter engine for SubRip srt caption format. It does not support advanced SubRip format (SubRip format with
 * annotations). Advanced format will be parsed but all annotations will be stripped off.
 * 
 */
public class SubRipCaptionConverter implements CaptionConverter {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SubRipCaptionConverter.class);

  private static final String EXTENSION = "srt";

  /** line ending used in srt - windows native in specification */
  private static final String LINE_ENDING = "\r\n";

  /**
   * {@inheritDoc} Since srt does not store information about language, language parameter is ignored.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.io.InputStream, java.lang.String)
   */
  @Override
  public List<Caption> importCaption(InputStream in, String language) throws CaptionConverterException {

    List<Caption> collection = new ArrayList<Caption>();

    // initialize scanner object
    Scanner scanner = new Scanner(in, "UTF-8");
    scanner.useDelimiter("[\n(\r\n)]{2}");

    // create initial time
    Time time = null;
    try {
      time = new TimeImpl(0, 0, 0, 0);
    } catch (IllegalTimeFormatException e1) {
    }

    while (scanner.hasNext()) {
      String captionString = scanner.next();
      // convert line endings to \n
      captionString = captionString.replace("\r\n", "\n");

      // split to number, time and caption
      String[] captionParts = captionString.split("\n", 3);
      // check for table length
      if (captionParts.length != 3) {
        throw new CaptionConverterException("Invalid caption for SubRip format: " + captionString);
      }

      // get time part
      String[] timePart = captionParts[1].split("-->");

      // parse time
      Time inTime;
      Time outTime;
      try {
        inTime = TimeUtil.importSrt(timePart[0].trim());
        outTime = TimeUtil.importSrt(timePart[1].trim());
      } catch (IllegalTimeFormatException e) {
        throw new CaptionConverterException(e.getMessage());
      }

      // check for time validity
      if (inTime.compareTo(time) < 0 || outTime.compareTo(inTime) <= 0) {
        logger.warn("Caption with invalid time encountered. Skipping...");
        continue;
      }
      time = outTime;

      // get text captions
      String[] captionLines = createCaptionLines(captionParts[2]);
      if (captionLines == null) {
        throw new CaptionConverterException("Caption does not contain any caption text: " + captionString);
      }

      // create caption object and add to caption collection
      Caption caption = new CaptionImpl(inTime, outTime, captionLines);
      collection.add(caption);
    }

    return collection;
  }

  /**
   * {@inheritDoc} Since srt does not store information about language, language parameter is ignored.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#exportCaption(java.io.OutputStream, java.lang.String)
   */
  @Override
  public void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException {

    if (language != null) {
      logger.debug("SubRip format does not include language information. Ignoring language attribute.");
    }

    // initialize stream writer
    OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
    BufferedWriter bw = new BufferedWriter(osw);

    // initialize counter
    int counter = 1;
    for (Caption caption : captions) {
      String captionString = String.format("%2$d%1$s%3$s --> %4$s%1$s%5$s%1$s%1$s", LINE_ENDING, counter,
              TimeUtil.exportToSrt(caption.getStartTime()), TimeUtil.exportToSrt(caption.getStopTime()),
              createCaptionText(caption.getCaption()));
      bw.append(captionString);
      counter++;
    }

    bw.flush();
    bw.close();
    osw.close();
  }

  /**
   * Helper function that creates caption text.
   * 
   * @param captionLines
   *          array containing caption lines
   * @return string representation of caption text
   */
  private String createCaptionText(String[] captionLines) {
    StringBuilder builder = new StringBuilder(captionLines[0]);
    for (int i = 1; i < captionLines.length; i++) {
      builder.append(LINE_ENDING);
      builder.append(captionLines[i]);
    }
    return builder.toString();
  }

  /**
   * Helper function that splits text into lines and remove any style annotation
   * 
   * @param captionText
   * @return array of caption's text lines
   */
  private String[] createCaptionLines(String captionText) {
    String[] captionLines = captionText.split("\n");
    if (captionLines.length == 0) {
      return null;
    }
    for (int i = 0; i < captionLines.length; i++) {
      captionLines[i] = captionLines[i].replaceAll("(<\\s*.\\s*>)|(</\\s*.\\s*>)", "").trim();
    }
    return captionLines;
  }

  /**
   * {@inheritDoc} Returns empty list since srt format does not store any information about language.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getLanguageList(java.io.InputStream)
   */
  @Override
  public String[] getLanguageList(InputStream input) throws CaptionConverterException {
    return new String[0];
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getExtension()
   */
  @Override
  public String getExtension() {
    return EXTENSION;
  }
}
