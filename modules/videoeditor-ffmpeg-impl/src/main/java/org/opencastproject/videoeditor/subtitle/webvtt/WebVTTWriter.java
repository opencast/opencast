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
package org.opencastproject.videoeditor.subtitle.webvtt;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WebVTTWriter {
  private Charset charset; // Charset used to encode file

  public WebVTTWriter() {
    this.charset = StandardCharsets.UTF_8;
  }

  public WebVTTWriter(Charset charset) {
    this.charset = charset;
  }

  public void write(WebVTTSubtitle subtitleObject, OutputStream os) throws IOException {
    try {
      // Write header
      List<String> headerLines = subtitleObject.getHeaderLines();
      for (int i = 0; i < headerLines.size() ; i++) {
        // Ensure valid header
        if (i == 0 && !headerLines.get(i).startsWith("WEBVTT")) {
          os.write(("WEBVTT " +  headerLines.get(i) + "\n").getBytes(this.charset));
          continue;
        }
        os.write((headerLines.get(i) + "\n").getBytes(this.charset));
      }
      // Ensure valid header
      if (headerLines.size() == 0) {
        os.write(("WEBVTT" + "\n").getBytes(this.charset));
      }
      os.write("\n".getBytes(this.charset));

      // Write region blocks
      for (WebVTTSubtitleRegion region : subtitleObject.getRegions()) {
        for (String regionLine : region.getLines()) {
          os.write((regionLine + "\n").getBytes(this.charset));
        }
        os.write("\n".getBytes(this.charset));
      }

      // Write style blocks
      for (WebVTTSubtitleStyle style : subtitleObject.getStyle()) {
        for (String styleLine : style.getLines()) {
          os.write((styleLine + "\n").getBytes(this.charset));
        }
        os.write("\n".getBytes(this.charset));
      }

      // Write cues
      for (WebVTTSubtitleCue cue : subtitleObject.getCues()) {
        if (cue.getId() != null) {
          // Write id
          String number = String.format("%s\n", cue.getId());
          os.write(number.getBytes(this.charset));
        }

        // Write start and end time
        String startToEnd = String.format("%s --> %s %s\n",
                this.formatTimeCode(cue.getStartTime()),
                this.formatTimeCode(cue.getEndTime()),
                ofNullable(cue.getCueSettingsList()).orElse(""));
        os.write(startToEnd.getBytes(this.charset));

        // Write text
        String text = String.format("%s\n", cue.getText());
        os.write(text.getBytes(this.charset));

        // Write empty line
        os.write("\n".getBytes(this.charset));
      }
    } catch (UnsupportedEncodingException e) {
      throw new IOException("Encoding error in input subtitle");
    }
  }

  private String formatTimeCode(long ms) {
    long milliseconds = (ms % 1000);
    long seconds = (long)Math.floor((ms / 1000) % 60);
    long minutes = (long)Math.floor((ms / (1000 * 60)) % 60);
    long hours = (long)Math.floor((ms / (1000 * 60 * 60)));

    return String.format("%02d:%02d:%02d.%03d",
            hours,
            minutes,
            seconds,
            milliseconds);
  }
}
