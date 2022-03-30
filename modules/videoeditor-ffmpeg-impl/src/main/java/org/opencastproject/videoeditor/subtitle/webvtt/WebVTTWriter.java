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

import org.opencastproject.videoeditor.subtitle.base.Subtitle;
import org.opencastproject.videoeditor.subtitle.base.SubtitleCue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class WebVTTWriter {
  private Charset charset; // Charset used to encode file

  public WebVTTWriter() {
    this.charset = StandardCharsets.UTF_8;
  }

  public WebVTTWriter(Charset charset) {
    this.charset = charset;
  }

  public void write(Subtitle subtitleObject, OutputStream os) throws IOException {
    try {
      // Write header
      os.write(new String("WEBVTT\n\n").getBytes(this.charset));

      // Write cues
      for (SubtitleCue cue : subtitleObject.getCues()) {
        if (cue.getId() != null) {
          // Write id
          String number = String.format("%s\n", cue.getId());
          os.write(number.getBytes(this.charset));
        }

        // Write start and end time
        String startToEnd = String.format("%s --> %s \n",
                this.formatTimeCode(cue.getStartTime()),
                this.formatTimeCode(cue.getEndTime()));
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
