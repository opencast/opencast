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

import com.google.gson.annotations.SerializedName;

public final class TrackData {
  public static final String AUDIO_STREAM = "audio_stream";
  public static final String VIDEO_STREAM = "video_stream";

  @SerializedName(AUDIO_STREAM)
  private final TrackSubData audio;

  @SerializedName(VIDEO_STREAM)
  private final TrackSubData video;

  private final MediaPackageElementFlavor flavor;
  private final String uri;
  private final String id;

  public MediaPackageElementFlavor getFlavor() {
    if (flavor == null) {
      return null;
    }
    return new MediaPackageElementFlavor(flavor.getType(), flavor.getSubtype());
  }

  public TrackData(final String flavorType, final String flavorSubtype, final TrackSubData audio,
          final TrackSubData video, String uri, String id) {
    this.flavor = new MediaPackageElementFlavor(flavorType, flavorSubtype);
    this.audio = audio;
    this.video = video;
    this.uri = uri;
    this.id = id;
  }

  public TrackSubData getAudio() {
    return this.audio;
  }

  public TrackSubData getVideo() {
    return this.video;
  }

  public String getId() {
    return this.id;
  }
}
