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

import org.opencastproject.usertracking.api.UserSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
 * A JAXB-annotated implementation of {@link UserSummary}
 */
@Entity(name = "UserSummary")
@Table(name = "mh_user_action")
@NamedQueries({ 
 @NamedQuery(name = "userSummaryByMediapackageByType", query = "SELECT a.userId, COUNT(distinct a.sessionId), COUNT(distinct a.mediapackageId), SUM(a.length), MAX(a.created) FROM UserAction a WHERE a.type = :type AND a.mediapackageId = :mediapackageId GROUP BY a.userId") })
@XmlType(name = "summary", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "summary", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class UserSummaryImpl implements UserSummary {
  @Id
  @Column(name = "user", length = 65535)
  @XmlElement(name = "userId")
  private String userId = "Empty UserId";
  
  @Column(name = "session")
  @XmlElement(name = "sessionCount")
  private long sessionCount = 0;
  
  @Column(name = "mediapackage", length = 128)
  @XmlElement(name = "uniqueMediapackages")
  private long uniqueMediapackages = 0;
  
  @Column(name = "length")
  @XmlElement(name = "length")
  private long length = 0;
  
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "last")
  private Date last = new Date();

//The logger
 private Logger logger = LoggerFactory.getLogger(UserSummaryImpl.class);
  
  public UserSummaryImpl() {
  }
  
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public long getSessionCount() {
    return sessionCount;
  }

  public void setSessionCount(long sessionCount) {
    this.sessionCount = sessionCount;
  }

  public long getUniqueMediapackages() {
    return uniqueMediapackages;
  }

  public void setUniqueMediapackages(long uniqueMediapackages) {
    this.uniqueMediapackages = uniqueMediapackages;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }

  public Date getLast() {
    return last;
  }

  public void setLast(Date last) {
    this.last = last;
  }

  public void combine(UserSummary other) {
    this.setSessionCount(this.getSessionCount() + other.getSessionCount());
    this.setUniqueMediapackages(this.getUniqueMediapackages() + other.getUniqueMediapackages());
    this.setLength(this.getLength() + other.getLength());
    if (this.getLast().before(other.getLast())) {
      this.setLast(other.getLast());
    }
  }
  
  public void ingest(Object[] properties) {
    if (properties.length != 5) {
      return;
    }
    if (properties[0] instanceof String) {
      setUserId((String) properties[0]);
    } else if (properties[0] != null) {
      logger.debug("Unable to set user id to " + properties[0]);
    }
    if (properties[1] instanceof Long) {
      setSessionCount((Long) properties[1]);
    } else if (properties[1] != null) {
      logger.debug("Unable to set session count to " + properties[1]);
    }
    if (properties[2] instanceof Long) {
      setUniqueMediapackages((Long) properties[2]);
    } else if (properties[2] != null) {
      logger.debug("Unable to set unique mediapackage count to " + properties[2]);
    }
    if (properties[3] instanceof Long) {
      setLength((Long) properties[3]);
    } else if (properties[3] != null) {
      logger.debug("Unable to set length to " + properties[3]);
    }
    if (properties[4] instanceof Date) {
      setLast((Date) properties[4]);
    } else if (properties[4] != null) {
      logger.debug("Unable to set last to " + properties[4]);
    }
  }
}
