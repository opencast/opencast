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

import java.awt.Rectangle;

/**
 * Decomposition type for space and time.
 */
public interface SpatioTemporalDecomposition extends XmlElement {

  /**
   * Set the <code>hasGap</code> property indicating that there may be gaps in between the segments.
   * 
   * @param hasGap
   *          <code>true</code> if there are gaps
   */
  void setGap(boolean hasGap);

  /**
   * Returns <code>true</code> if the segment has a gap.
   * 
   * @return <code>true</code> if the segment has a gap
   */
  boolean hasGap();

  /**
   * Set the <code>isOverlapping</code> property indicating that some segments may be overlapping.
   * 
   * @param isOverlapping
   *          <code>true</code> if elements are overlapping
   */
  void setOverlapping(boolean isOverlapping);

  /**
   * Returns <code>true</code> if the segment overlaps with another one.
   * 
   * @return <code>true</code> if the segment overlaps
   */
  boolean isOverlapping();

  /**
   * Adds the specified text as a <code>VideoText</code> element to the decomposition.
   * 
   * @param text
   *          the text
   * @param boundary
   *          the text's bounding box
   * @param time
   *          the time and duration
   * 
   * @return the new video text element
   */
  VideoText addVideoText(Textual text, Rectangle boundary, MediaTime time);

  /**
   * Adds the <code>VideoText</code> element to the decomposition.
   * 
   * @param videoText
   *          the video text
   */
  void addVideoText(VideoText videoText);

  /**
   * Returns all the video text elements from this decomposition.
   * 
   * @return the video text elements
   */
  VideoText[] getVideoText();

  /**
   * Returns the <code>VideoText</code> element with the given identifier or <code>null</code> if there is no such
   * element.
   * 
   * @param id
   *          the video text id
   * @return the video text element
   */
  VideoText getVideoText(String id);

}
