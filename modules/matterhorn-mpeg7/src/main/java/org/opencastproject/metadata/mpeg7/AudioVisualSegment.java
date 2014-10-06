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

/**
 * An audiovisual segment represents a temporal decomposition of the audiovisual stream that may have properties like
 * text annotations attached to it.
 *
 * <pre>
 * &lt;complexType name=&quot;AudioVisualSegmentType&quot;&gt;
 *   &lt;complexContent&gt;
 *       &lt;extension base=&quot;mpeg7:SegmentType&quot;&gt;
 *           &lt;sequence&gt;
 *               &lt;choice minOccurs=&quot;0&quot;&gt;
 *                   &lt;element name=&quot;MediaTime&quot; type=&quot;mpeg7:MediaTimeType&quot;/&gt;
 *                   &lt;element name=&quot;TemporalMask&quot; type=&quot;mpeg7:TemporalMaskType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;choice minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *                   &lt;element name=&quot;SpatialDecomposition&quot;
 *                       type=&quot;mpeg7:AudioVisualSegmentSpatialDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;TemporalDecomposition&quot;
 *                       type=&quot;mpeg7:AudioVisualSegmentTemporalDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;SpatioTemporalDecomposition&quot;
 *                       type=&quot;mpeg7:AudioVisualSegmentSpatioTemporalDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;MediaSourceDecomposition&quot;
 *                       type=&quot;mpeg7:AudioVisualSegmentMediaSourceDecompositionType&quot;/&gt;
 *               &lt;/choice&gt;
 *           &lt;/sequence&gt;
 *       &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface AudioVisualSegment extends Segment {

  /**
   * Returns <code>true</code> if there is a spatio temporal decomposition as part of this segment.
   *
   * @return <code>true</code> if there is a spatio temporal decomposition
   */
  boolean hasSpatioTemporalDecomposition();

  /**
   * Creates a spatio temporal decomposition in this video segment.
   *
   * @param gap
   *          <code>true</code> if there may be gap between the elements
   * @param overlap
   *          <code>true</code> if elements may overlap
   * @return the new decomposition
   * @throws IllegalStateException
   *           if there is already such a decomposition
   */
  SpatioTemporalDecomposition createSpatioTemporalDecomposition(boolean gap, boolean overlap)
          throws IllegalStateException;

  /**
   * Returns the spatio temporal decomposition that contains information about elements on the segment such as text
   * elements.
   *
   * @return the spatio temporal decomposition
   */
  SpatioTemporalDecomposition getSpatioTemporalDecomposition();

}
