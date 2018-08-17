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
import org.opencastproject.caption.util.TimeUtil;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class WebVttCaptionConverter implements CaptionConverter {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(WebVttCaptionConverter.class);

  private static final String EXTENSION = "vtt";

  /**
   * {@inheritDoc} Language parameter is ignored.
   *
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.io.InputStream, java.lang.String)
   */
  @Override
  public List<Caption> importCaption(InputStream in, String language) throws CaptionConverterException {
    // TODO
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc} Language parameter is ignored.
   */
  @Override
  public void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException {

    OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
    BufferedWriter bw = new BufferedWriter(osw);

    bw.append("WEBVTT\n\n");

    for (Caption caption : captions) {
      String captionString = String.format("%s --> %s\n%s\n\n", TimeUtil.exportToVtt(caption.getStartTime()),
              TimeUtil.exportToVtt(caption.getStopTime()), createCaptionText(caption.getCaption()));
      bw.append(captionString);
      logger.trace(captionString);
    }

    bw.flush();
    bw.close();
    osw.close();
  }

  private String createCaptionText(String[] captionLines) {
    StringBuilder builder = new StringBuilder(captionLines[0]);
    for (int i = 1; i < captionLines.length; i++) {
      builder.append("\n");
      builder.append(captionLines[i]);
    }
    return builder.toString();
  }

  @Override
  public String[] getLanguageList(InputStream input) throws CaptionConverterException {
    return new String[0];
  }
  @Override
  public String getExtension() {
    return EXTENSION;
  }

  @Override
  public Type getElementType() {
    return MediaPackageElement.Type.Attachment;
  }

}
