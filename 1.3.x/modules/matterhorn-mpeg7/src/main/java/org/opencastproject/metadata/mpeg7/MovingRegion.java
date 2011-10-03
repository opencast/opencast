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
 * A <code>MovingRegion</code> describes the location, the movement and contents of elements on a video segment:
 * 
 * <pre>
 * &lt;complexType name="MovingRegionType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="mpeg7:SegmentType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice minOccurs="0"&gt;
 *           &lt;element name="SpatioTemporalLocator" type="mpeg7:SpatioTemporalLocatorType"/&gt;
 *           &lt;element name="SpatioTemporalMask" type="mpeg7:SpatioTemporalMaskType"/&gt;
 *         &lt;/choice&gt;
 *         &lt;choice minOccurs="0" maxOccurs="unbounded"&gt;
 *           &lt;element name="VisualDescriptor" type="mpeg7:VisualDType"/&gt;
 *           &lt;element name="VisualDescriptionScheme" type="mpeg7:VisualDSType"/&gt;
 *           &lt;element name="VisualTimeSeriesDescriptor" type="mpeg7:VisualTimeSeriesType"/&gt;
 *           &lt;element name="GofGopFeature" type="mpeg7:GofGopFeatureType"/&gt;
 *         &lt;/choice&gt;
 *         &lt;element name="MultipleView" type="mpeg7:MultipleViewType" minOccurs="0"/&gt;
 *         &lt;choice minOccurs="0" maxOccurs="unbounded"&gt;
 *           &lt;element name="SpatialDecomposition" type="mpeg7:MovingRegionSpatialDecompositionType"/&gt;
 *           &lt;element name="TemporalDecomposition" type="mpeg7:MovingRegionTemporalDecompositionType"/&gt;
 *           &lt;element name="SpatioTemporalDecomposition" type="mpeg7:MovingRegionSpatioTemporalDecompositionType"/&gt;
 *           &lt;element name="MediaSourceDecomposition" type="mpeg7:MovingRegionMediaSourceDecompositionType"/&gt;
 *         &lt;/choice&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface MovingRegion extends XmlElement {

  /**
   * Sets the spatio temporal locator.
   * 
   * @param locator
   *          the region locator
   */
  void setSpatioTemporalLocator(SpatioTemporalLocator locator);

  /**
   * Returns the spatio temporal locator.
   * 
   * @return the locator
   */
  SpatioTemporalLocator getSpatioTemporalLocator();

}
