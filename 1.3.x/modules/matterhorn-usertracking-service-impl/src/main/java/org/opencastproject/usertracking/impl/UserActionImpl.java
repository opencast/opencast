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
package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserAction;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB-annotated implementation of {@link UserAction}
 */
@Entity(name = "UserAction")
@Table(name = "user_action")
@NamedQueries({
        @NamedQuery(name = "findUserActions", query = "SELECT a FROM UserAction a"),
        @NamedQuery(name = "countSessionsGroupByMediapackage", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM UserAction a GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsGroupByMediapackageByIntervall", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsOfMediapackage", query = "SELECT COUNT(distinct a.sessionId) FROM UserAction a WHERE a.mediapackageId = :mediapackageId"),
        @NamedQuery(name = "findLastUserFootprintOfSession", query = "SELECT a FROM UserAction a  WHERE a.sessionId = :sessionId AND a.type = \'FOOTPRINT\'  ORDER BY a.created DESC"),
        @NamedQuery(name = "findLastUserActionsOfSession", query = "SELECT a FROM UserAction a  WHERE a.sessionId = :sessionId ORDER BY a.created DESC"),
        @NamedQuery(name = "findUserActionsByType", query = "SELECT a FROM UserAction a WHERE a.type = :type"),
        @NamedQuery(name = "findUserActionsByTypeAndMediapackageId", query = "SELECT a FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type"),
        @NamedQuery(name = "findUserActionsByTypeAndMediapackageIdOrderByOutpointDESC", query = "SELECT a FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type ORDER BY a.outpoint DESC"),
        @NamedQuery(name = "findUserActionsByIntervall", query = "SELECT a FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findUserActionsByTypeAndIntervall", query = "SELECT a FROM UserAction a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type"),
        @NamedQuery(name = "findTotal", query = "SELECT COUNT(a) FROM UserAction a"),
        @NamedQuery(name = "findTotalByType", query = "SELECT COUNT(a) FROM UserAction a WHERE a.type = :type"),
        @NamedQuery(name = "findTotalByTypeAndMediapackageId", query = "SELECT COUNT(a) FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type"),
        @NamedQuery(name = "findTotalByIntervall", query = "SELECT COUNT(a) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findDistinctEpisodeIdTotalByIntervall", query = "SELECT COUNT(distinct a.mediapackageId) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findTotalByTypeAndIntervall", query = "SELECT COUNT(a) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type") })
@XmlType(name = "action", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "action", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserActionImpl implements UserAction {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.AUTO)
  @XmlElement(name = "id")
  private Long id;

  @Lob
  //@Index
  @Column(name = "mediapackage_id", length = 65535)
  @XmlElement(name = "mediapackageId")
  private String mediapackageId;

  @Lob
  //@Index
  @Column(name = "user_id", length = 65535)
  @XmlElement(name = "userId")
  private String userId;

  @Lob
  @Column(name = "user_ip", length = 65535)
  @XmlElement(name = "userIp")
  private String userIp;

  @Lob
  @Column(name = "session_id", length = 65535)
  @XmlElement(name = "sessionId")
  private String sessionId;

  @Column(name = "inpoint")
  @XmlElement(name = "inpoint")
  private int inpoint;

  @Column(name = "outpoint")
  @XmlElement(name = "outpoint")
  private int outpoint;

  @Column(name = "length")
  @XmlElement(name = "length")
  private int length;

  @Lob
  @Column(name = "type", length = 65535)
  @XmlElement(name = "type")
  private String type;

  @Column(name = "is_playing")
  @XmlElement(name = "isPlaying")
  private boolean isPlaying;

  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "created")
  private Date created = new Date();

  /**
   * A no-arg constructor needed by JAXB
   */
  public UserActionImpl() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMediapackageId() {
    return mediapackageId;
  }

  public void setMediapackageId(String mediapackageId) {
    this.mediapackageId = mediapackageId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserIp() {
    return userIp;
  }

  public void setUserIp(String userIp) {
    this.userIp = userIp;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public int getInpoint() {
    return inpoint;
  }

  public void setInpoint(int inpoint) {
    this.inpoint = inpoint;
    updateLength();
  }

  public int getOutpoint() {
    return outpoint;
  }

  public void setOutpoint(int outpoint) {
    this.outpoint = outpoint;
    updateLength();
  }

  public int getLength() {
    return length;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean getIsPlaying() {
    return isPlaying;
  }

  public void setIsPlaying(boolean isPlaying) {
    this.isPlaying = isPlaying;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  private void updateLength() {
    this.length = this.outpoint - this.inpoint;
  }
}
