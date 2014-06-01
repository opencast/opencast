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

import java.util.Iterator;

/**
 * This interface describes that basis for a temporal decomposition of an audio, video or audiovisual content element.
 */
public interface TemporalDecomposition<T extends Segment> extends XmlElement {

  /**
   * Criteria of decomposition.
   */
  enum DecompositionCriteria {
    Temporal
  };

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
   * Sets the decomposition criteria.
   *
   * @param criteria
   *          the criteria
   */
  void setCriteria(DecompositionCriteria criteria);

  /**
   * Returns the decomposition criteria.
   *
   * @return the criteria
   */
  DecompositionCriteria getCriteria();

  /**
   * Creates a new segment and returns it.
   *
   * @param id
   *          the segment identifier
   * @return the new segment
   */
  T createSegment(String id);

  /**
   * Returns <code>true</code> if the composition actually contains segments.
   *
   * @return <code>true</code> if there are segments
   */
  boolean hasSegments();

  /**
   * Returns an iteration of the video's segments.
   *
   * @return the video segments
   */
  Iterator<T> segments();

  /**
   * Returns the segment with the given identifier or <code>null</code> if the segment does not exist.
   *
   * @param segmentId
   *          the segment identifier
   * @return the segment
   */
  T getSegmentById(String segmentId);

}
