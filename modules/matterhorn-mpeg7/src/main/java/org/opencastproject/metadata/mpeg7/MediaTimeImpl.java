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


package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * TODO: Comment me!
 */
public class MediaTimeImpl implements MediaTime {

  /** The media time point */
  protected MediaTimePoint mediaTimePoint = null;

  /** The media duration */
  protected MediaDuration mediaDuration = null;

  /**
   * Creates a media time object representing the given timepoint and duration.
   *
   * @param timePoint
   *          the time point
   * @param duration
   *          the duration
   */
  public MediaTimeImpl(MediaTimePoint timePoint, MediaDuration duration) {
    mediaTimePoint = timePoint;
    mediaDuration = duration;
  }

  /**
   * Creates a media time object from the given long values for timepoint and duration.
   *
   * @param time
   *          the time in milliseconds
   * @param duration
   *          the duration in milliseconds
   */
  public MediaTimeImpl(long time, long duration) {
    mediaTimePoint = new MediaTimePointImpl(time);
    mediaDuration = new MediaDurationImpl(duration);
  }

  /**
   * Creates a media time object from the given string representations for timepoint and duration.
   *
   * @param time
   *          the timepoint string representation
   * @param duration
   *          the duration string representation
   */
  MediaTimeImpl(String time, String duration) {
    mediaTimePoint = MediaTimePointImpl.parseTimePoint(time);
    mediaDuration = MediaDurationImpl.parseDuration(duration);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTime#getMediaDuration()
   */
  public MediaDuration getMediaDuration() {
    return mediaDuration;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.MediaTime#getMediaTimePoint()
   */
  public MediaTimePoint getMediaTimePoint() {
    return mediaTimePoint;
  }

  /**
   * Parses a media time and duration.
   *
   * @param time
   * @param duration
   * @return the
   */
  public static MediaTime parse(String time, String duration) {
    MediaTime mediaTime = new MediaTimeImpl(time, duration);
    return mediaTime;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("MediaTime");
    if (mediaTimePoint != null)
      node.appendChild(mediaTimePoint.toXml(document));
    if (mediaDuration != null)
      node.appendChild(mediaDuration.toXml(document));
    return node;
  }

}
