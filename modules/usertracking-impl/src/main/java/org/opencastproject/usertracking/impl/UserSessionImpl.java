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

package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserSession;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@Entity(name = "UserSession")
@Access(AccessType.FIELD)
@Table(name = "oc_user_session")
@NamedQueries({
        @NamedQuery(name = "findUserSessionBySessionId", query = "SELECT s FROM UserSession s WHERE s.sessionId = :sessionId") })
@XmlType(name = "session", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "session", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserSessionImpl implements UserSession {

  @Id
  @Column(name = "session_id", length = 50)
  @XmlElement(name = "sessionId")
  private String sessionId;

  @Lob
  @Column(name = "user_id", length = 255)
  @XmlElement(name = "userId")
  private String userId;

  @Lob
  @Column(name = "user_ip", length = 255)
  @XmlElement(name = "userIp")
  private String userIp;

  @Lob
  @Column(name = "user_agent", length = 255)
  @XmlElement(name = "userAgent")
  private String userAgent;

  /**
   * No Arg Constructor for JAXB
   */
  public UserSessionImpl() {

  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String getUserIp() {
    return userIp;
  }

  @Override
  public void setUserIp(String userIp) {
    this.userIp = userIp;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public String getUserAgent() {
    return userAgent;
  }

  @Override
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
