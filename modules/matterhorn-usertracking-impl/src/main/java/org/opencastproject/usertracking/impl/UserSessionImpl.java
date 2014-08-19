package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserSession;

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
@Table(name = "mh_user_session")
@NamedQueries({
        @NamedQuery(name = "findUserActionsByMediaPackageAndTypeDescendingByDate", query = "SELECT a FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type ORDER BY a.created DESC") })
@XmlType(name = "session", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "session", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserSessionImpl implements UserSession {

  @Id
  @Lob
  @Column(name = "session", length = 50)
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

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {{
    this.userAgent = userAgent;
  }
}
