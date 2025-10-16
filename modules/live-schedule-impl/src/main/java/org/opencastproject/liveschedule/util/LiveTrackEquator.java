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
package org.opencastproject.liveschedule.util;

import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;

import org.apache.commons.collections4.Equator;

import java.util.Objects;

public class LiveTrackEquator implements Equator<Track> {
  @Override
  public boolean equate(Track track1, Track track2) {
    // we can safely assume that each live track has exactly one video stream since we generated that ourselves
    VideoStream videostream1 = (VideoStream) track1.getStreams()[0];
    VideoStream videostream2 = (VideoStream) track2.getStreams()[0];

    return Objects.equals(track1.getURI(), track2.getURI()) && Objects.equals(track1.getFlavor(), track2.getFlavor())
        && Objects.equals(track1.getMimeType(), track2.getMimeType()) && Objects.equals(track1.getDuration(),
        track2.getDuration()) && Objects.equals(videostream1.getFrameWidth(), videostream2.getFrameWidth())
        && Objects.equals(videostream1.getFrameHeight(), videostream2.getFrameHeight());
  }

  @Override
  public int hash(Track track) {
    VideoStream videostream = (VideoStream) track.getStreams()[0];
    return Objects.hash(track.getURI(), track.getFlavor(), track.getMimeType(), track.getDuration(),
        videostream.getFrameWidth(), videostream.getFrameHeight());
  }
};



