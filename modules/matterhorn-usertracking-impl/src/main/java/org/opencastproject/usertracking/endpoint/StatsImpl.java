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
package org.opencastproject.usertracking.endpoint;

import org.opencastproject.usertracking.api.Stats;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB-annotated implementation of {@link Stats}
 */
@XmlType(name = "stats", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "stats", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class StatsImpl implements Stats {

  @XmlAttribute(name = "id")
  private String mediapackageId;

  @XmlElement(name = "views")
  private int views;

  /**
   * A no-arg constructor needed by JAXB
   */
  public StatsImpl() {
  }

  @Override
  public String getMediapackageId() {
    return mediapackageId;
  }

  @Override
  public int getViews() {
    return views;
  }

  @Override
  public void setMediapackageId(String mediapackageId) {
    this.mediapackageId = mediapackageId;
    
  }

  @Override
  public void setViews(int views) {
    this.views = views;
    
  }
}
