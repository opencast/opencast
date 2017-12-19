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

/**
 * A video segment represents a temporal decomposition of the video stream that may have properties like text
 * annotations attached to it.
 *
 * <pre>
 * &lt;complexType name=&quot;VideoSegmentType&quot;&gt;
 *   &lt;complexContent&gt;
 *       &lt;extension base=&quot;mpeg7:SegmentType&quot;&gt;
 *           &lt;sequence&gt;
 *               &lt;choice minOccurs=&quot;0&quot;&gt;
 *                   &lt;element name=&quot;MediaTime&quot; type=&quot;mpeg7:MediaTimeType&quot;/&gt;
 *                   &lt;element name=&quot;TemporalMask&quot; type=&quot;mpeg7:TemporalMaskType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;choice minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *                   &lt;element name=&quot;VisualDescriptor&quot; type=&quot;mpeg7:VisualDType&quot;/&gt;
 *                   &lt;element name=&quot;VisualDescriptionScheme&quot; type=&quot;mpeg7:VisualDSType&quot;/&gt;
 *                   &lt;element name=&quot;VisualTimeSeriesDescriptor&quot; type=&quot;mpeg7:VisualTimeSeriesType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name=&quot;MultipleView&quot; type=&quot;mpeg7:MultipleViewType&quot; minOccurs=&quot;0&quot;/&gt;
 *               &lt;element name=&quot;Mosaic&quot; type=&quot;mpeg7:MosaicType&quot; minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;/&gt;
 *               &lt;choice minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *                   &lt;element name=&quot;SpatialDecomposition&quot; type=&quot;mpeg7:VideoSegmentSpatialDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;TemporalDecomposition&quot; type=&quot;mpeg7:VideoSegmentTemporalDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;SpatioTemporalDecomposition&quot; type=&quot;mpeg7:VideoSegmentSpatioTemporalDecompositionType&quot;/&gt;
 *                   &lt;element name=&quot;MediaSourceDecomposition&quot; type=&quot;mpeg7:VideoSegmentMediaSourceDecompositionType&quot;/&gt;
 *               &lt;/choice&gt;
 *           &lt;/sequence&gt;
 *       &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface VideoSegment extends Segment {

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
