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


package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.track.TrackImpl;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * This interface describes methods and fields for audio and video tracks as part of a media package.
 */
@XmlJavaTypeAdapter(TrackImpl.Adapter.class)
public interface Track extends MediaPackageElement {

  /**
   * Media package element type.
   */
  Type TYPE = Type.Track;

  /**
   * Return the streams that make up the track. Tracks consist of at least one stream.
   */
  Stream[] getStreams();

  /**
   * Returns <code>true</code> if the track features an audio stream.
   *
   * @return <code>true</code> if the track has an audio stream
   */
  boolean hasAudio();

  /**
   * Returns <code>true</code> if the track features a video stream.
   *
   * @return <code>true</code> if the track has a video stream
   */
  boolean hasVideo();

  /**
   * Returns the track duration in milliseconds or <code>null</code> if the duration is not available.
   *
   * @return the track duration
   */
  Long getDuration();

  /**
   * Returns the track's description with details about framerate, codecs etc.
   *
   * @return the track description.
   */
  String getDescription();

  /**
   * Returns <code>true</code> if the track is a live track.
   *
   * @return true if live track; false otherwise
   */
  boolean isLive();
}
