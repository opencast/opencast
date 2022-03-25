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
package org.opencastproject.editor.api;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.List;

/**
 * Wrapper for additional data that may be received by a post request from the editor
 */
public final class PostEditingData extends EditingData {

  public final class Subtitle {
    private final MediaPackageElementFlavor flavor;
    private final String subtitle;

    private Subtitle(MediaPackageElementFlavor flavor, String subtitle, String trackId) {
      this.flavor = flavor;
      this.subtitle = subtitle;
    }

    public MediaPackageElementFlavor getFlavor() {
      return flavor;
    }

    public String getSubtitle() {
      return subtitle;
    }
  }

  private final List<Subtitle> subtitles;

  private PostEditingData(List<SegmentData> segments, List<TrackData> tracks, List<WorkflowData> workflows,
          Long duration, String title, String recordingStartDate, String seriesId, String seriesName,
          Boolean workflowActive, List<Subtitle> subtitles) {
    super(segments, tracks, workflows, duration, title, recordingStartDate, seriesId, seriesName, workflowActive);
    this.subtitles = subtitles;
  }

  public List<Subtitle> getSubtitles() {
    return subtitles;
  }
}
