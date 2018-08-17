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


package org.opencastproject.silencedetection.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * This class holds the start and stop position of a media file in milliseconds.
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MediaSegment {

  /**
   * Start position.
   */
  @XmlAttribute(name = "start", required = true)
  private final long segmentStart;

  /**
   * Stop position.
   */
  @XmlAttribute(name = "stop", required = true)
  private final long segmentStop;

  public MediaSegment() {
    this(-1, -1);
  }

  public MediaSegment(long segmentStart, long segmentStop) {
    this.segmentStart = segmentStart;
    this.segmentStop = segmentStop;
  }

  /**
   * Returns segment start position.
   * @return start position
   */
  public long getSegmentStart() {
    return segmentStart;
  }

  /**
   * Returns segment stop position.
   * @return stop position
   */
  public long getSegmentStop() {
    return segmentStop;
  }
}
