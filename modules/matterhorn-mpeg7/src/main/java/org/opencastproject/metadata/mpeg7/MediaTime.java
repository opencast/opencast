/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.metadata.mpeg7;

import org.opencastproject.mediapackage.XmlElement;

/**
 * This interface defines the time constraints (start and duration) of a multimedia content element or segment.
 */
public interface MediaTime extends XmlElement {

  /**
   * Returns the media time point, i. e. the starting time of the vidoe segement.
   *
   * @return the media time point
   */
  MediaTimePoint getMediaTimePoint();

  /**
   * Returns the media duration.
   *
   * @return the media duration
   */
  MediaDuration getMediaDuration();

}
