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
 * This locator specifies elements in time and space.
 * 
 * <pre>
 * &lt;complexType name="SpatioTemporalLocatorType" final="#all"&gt;
 *   &lt;sequence&gt;
 *     &lt;element name="CoordRef" minOccurs="0"&gt;
 *       &lt;complexType&gt;
 *         &lt;attribute name="ref" type="IDREF" use="required"/&gt;
 *         &lt;attribute name="spatialRef" type="boolean" use="required"/&gt;
 *       &lt;/complexType&gt;
 *     &lt;/element&gt;
 *     &lt;choice maxOccurs="unbounded"&gt;
 *       &lt;element name="FigureTrajectory" type="mpeg7:FigureTrajectoryType"/&gt;
 *       &lt;element name="ParameterTrajectory" type="mpeg7:ParameterTrajectoryType"/&gt;
 *       &lt;element name="MediaTime" type="mpeg7:MediaTimeType"/&gt;
 *     &lt;/choice&gt;
 *   &lt;/sequence&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public interface SpatioTemporalLocator extends XmlElement {

  /**
   * Sets the media time.
   * 
   * @param time
   *          the time
   */
  void setMediaTime(MediaTime time);

  /**
   * Returns the locator's time and duration.
   * 
   * @return the media time
   */
  MediaTime getMediaTime();

}
