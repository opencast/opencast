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
