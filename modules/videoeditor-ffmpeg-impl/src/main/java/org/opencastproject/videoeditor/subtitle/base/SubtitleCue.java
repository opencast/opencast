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

public abstract class SubtitleCue {
  private String id;            // Id of cue. 1 or c1
  private long startTime;       // Start time in ms
  private long endTime;         // Stop time in ms
  private List<String> lines;   // Lines that make up the text

  protected SubtitleCue(SubtitleCue cue) {
    this.id = cue.getId();
    this.startTime = cue.getStartTime();
    this.endTime = cue.getEndTime();
    this.lines = cue.getLines();
  }

  protected SubtitleCue() {
    this.lines = new ArrayList<>();
  }

  protected SubtitleCue(long startTime, long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.lines = new ArrayList<>();
  }

  protected SubtitleCue(long startTime, long endTime, List<String> lines) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.lines = lines;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getStartTime() {
    return this.startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return this.endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public List<String> getLines() {
    return this.lines;
  }

  public void setLines(List<String> lines) {
    this.lines = lines;
  }

  public void addLine(String line) {
    this.lines.add(line);
  }

  public String getText() {
    String[] lineArray = lines.toArray(new String[lines.size()]);
    return String.join("\n", lineArray);
  }

  @Override
  public String toString() {
    return this.getText();
  }
}
