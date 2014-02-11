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
package org.opencastproject.analytics.impl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "view-item", namespace = "http://analytics.opencastproject.org")
@XmlRootElement(name = "view-item", namespace = "http://analytics.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class ViewItem {

  @XmlElement(name = "episode-id")
  private String id = "";
  @XmlElement(name = "views")
  private String views = "";
  @XmlElement(name = "played")
  private String played = "";
  @XmlElement(name = "start")
  private String start = "";
  @XmlElement(name = "end")
  private String end = "";

  /**
   * A no-arg constructor needed by JAXB
   */
  public ViewItem() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getViews() {
    return views;
  }

  public void setViews(String views) {
    this.views = views;
  }

  public String getPlayed() {
    return played;
  }

  public void setPlayed(String played) {
    this.played = played;
  }

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }
}
