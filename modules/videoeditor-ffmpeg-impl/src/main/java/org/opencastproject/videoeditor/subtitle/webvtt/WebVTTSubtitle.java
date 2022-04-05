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

import java.util.ArrayList;
import java.util.List;

public class WebVTTSubtitle extends Subtitle<WebVTTSubtitleCue> {
  private List<WebVTTSubtitleCue> cues;
  private List<WebVTTSubtitleRegion> regions;
  private List<WebVTTSubtitleStyle> style;

  public WebVTTSubtitle() {
    this.cues = new ArrayList<>();
    this.regions = new ArrayList<>();
    this.style = new ArrayList<>();
  }

  public void addCue(WebVTTSubtitleCue cue) {
    this.cues.add(cue);
  }
  public List<WebVTTSubtitleCue> getCues() {
    return this.cues;
  }
  public void setCues(List<WebVTTSubtitleCue> cues) {
    this.cues = cues;
  }

  public void addRegion(WebVTTSubtitleRegion region) {
    this.regions.add(region);
  }
  public List<WebVTTSubtitleRegion> getRegions() {
    return regions;
  }

  public void addStyle(WebVTTSubtitleStyle style) {
    this.style.add(style);
  }
  public List<WebVTTSubtitleStyle> getStyle() {
    return style;
  }
}
