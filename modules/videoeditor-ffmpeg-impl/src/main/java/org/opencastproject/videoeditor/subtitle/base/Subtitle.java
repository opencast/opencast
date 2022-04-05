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
package org.opencastproject.videoeditor.subtitle.base;

import java.util.ArrayList;
import java.util.List;

public abstract class Subtitle<T extends SubtitleCue> {
  private List<String> headerLines;
  private List<T> cues;

  public Subtitle() {
    this.headerLines = new ArrayList<String>();
    this.cues = new ArrayList<T>();
  }

  public void addHeaderLine(String headerLine) {
    this.headerLines.add(headerLine);
  }
  public List<String> getHeaderLines() {
    return headerLines;
  }
  public void setHeaderLines(List<String> headerLines) {
    this.headerLines = headerLines;
  }

  public void addCue(T cue) {
    this.cues.add(cue);
  }
  public List<T> getCues() {
    return this.cues;
  }
  public void setCues(List<T> cues) {
    this.cues = cues;
  }
}
