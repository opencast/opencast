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

package org.opencastproject.graphql.type.output;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(GqlTrack.TYPE_NAME)
public class GqlTrack implements GqlMediaPackageElement {

  public static final String TYPE_NAME = "Track";

  private final Track track;

  private VideoStream[] videoStreams;

  public GqlTrack(Track track) {
    this.track = track;
  }

  @GraphQLField
  public boolean isLive() {
    return this.track.isLive();
  }

  @GraphQLField
  public String logicalName() {
    return this.track.getLogicalName();
  }

  @GraphQLField
  public Integer width() {
    VideoStream videoStream = getVideoStream();
    if (videoStream == null) {
      return null;
    }
    return videoStream.getFrameWidth();
  }

  @GraphQLField
  public Integer height() {
    VideoStream videoStream = getVideoStream();
    if (videoStream == null) {
      return null;
    }
    return videoStream.getFrameHeight();
  }

  @GraphQLField
  public double frameRate() {
    VideoStream videoStream = getVideoStream();
    if (videoStream == null) {
      return 0f;
    }
    return videoStream.getFrameRate();
  }

  @Override
  public MediaPackageElement getElement() {
    return track;
  }

  private VideoStream getVideoStream() {
    if (videoStreams == null) {
      videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
    }
    if (videoStreams.length == 0) {
      return null;
    }
    return this.videoStreams[0];
  }

}
