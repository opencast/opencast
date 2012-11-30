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

import org.opencastproject.usertracking.api.ReportItem;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB-annotated implementation of ReportItem
 */
@XmlType(name = "report-item", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "report-item", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReportItemImpl implements ReportItem {

  @XmlElement(name = "episode-id")
  private String episodeId;

  @XmlElement(name = "views")
  private long views;

  @XmlElement(name = "played")
  private long played;

  /**
   * A no-arg constructor needed by JAXB
   */
  public ReportItemImpl() {
  }

  public String getEpisodeId() {
    return episodeId;
  }

  public void setEpisodeId(String episodeId) {
    this.episodeId = episodeId;
  }

  public long getViews() {
    return views;
  }

  public void setViews(long views) {
    this.views = views;
  }

  public long getPlayed() {
    return played;
  }

  public void setPlayed(long played) {
    this.played = played;
  }

}
