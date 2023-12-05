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

import org.opencastproject.mediapackage.XmlElement;

/**
 * Base interface for either of <code>AudioVisual</code>, <code>Audio</code> or <code>Video</code>.
 */
public interface MultimediaContentType extends XmlElement {

  /**
   * Multimedia content element type definitions.
   */
  enum Type {
    Audio, Video, AudioVisual
  };

  /**
   * Returns the element identifier.
   *
   * @return the identifier
   */
  String getId();

  /**
   * Returns the track's media location.
   *
   * @return the media location
   */
  MediaLocator getMediaLocator();

  /**
   * Sets the media locator for this multimedia content element.
   *
   * @param locator
   *          the media locator
   */
  void setMediaLocator(MediaLocator locator);

  /**
   * Returns the track's time constraints. Usually, the media time point will be <code>T00:00:00</code>, while the
   * duration will reflect the duration of the whole video clip.
   *
   * @return the track's time constraints
   */
  MediaTime getMediaTime();

  /**
   * Sets the media time constraints for this multimedia content element.
   *
   * @param time
   *          the media time constraints
   */
  void setMediaTime(MediaTime time);

  /**
   * Returns the temmporal decomposition (segments) of the content element.
   *
   * @return the temporal decomposition
   */
  TemporalDecomposition<? extends Segment> getTemporalDecomposition();

}
