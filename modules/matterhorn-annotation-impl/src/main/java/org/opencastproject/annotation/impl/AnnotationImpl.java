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
package org.opencastproject.annotation.impl;

import org.opencastproject.annotation.api.Annotation;

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
 * A JAXB-annotated implementation of {@link Annotation}
 */
@Entity(name = "Annotation")
@Table(name = "mh_annotation")
@NamedQueries({
        @NamedQuery(name = "findAnnotations", query = "SELECT a FROM Annotation a WHERE a.userId = :userId"),
	@NamedQuery(name = "findAnnotationsByMediapackageId", query = "SELECT a FROM Annotation a WHERE a.mediapackageId = :mediapackageId AND a.userId = :userId"),
        @NamedQuery(name = "findAnnotationsByType", query = "SELECT a FROM Annotation a WHERE a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "findAnnotationsByTypeAndMediapackageId", query = "SELECT a FROM Annotation a WHERE a.mediapackageId = :mediapackageId AND a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "findAnnotationsByTypeAndMediapackageIdOrderByOutpointDESC", query = "SELECT a FROM Annotation a WHERE a.mediapackageId = :mediapackageId AND a.type = :type AND a.userId = :userId ORDER BY a.outpoint DESC"),
        @NamedQuery(name = "findAnnotationsByIntervall", query = "SELECT a FROM Annotation a WHERE :begin <= a.created AND a.created <= :end AND a.userId = :userId"),
        @NamedQuery(name = "findAnnotationsByTypeAndIntervall", query = "SELECT a FROM Annotation a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "findTotal", query = "SELECT COUNT(a) FROM Annotation a WHERE a.userId = :userId"),
        @NamedQuery(name = "findTotalByMediapackageId", query = "SELECT COUNT(a) FROM Annotation a WHERE a.mediapackageId = :mediapackageId AND a.userId = :userId"),
        @NamedQuery(name = "findTotalByType", query = "SELECT COUNT(a) FROM Annotation a WHERE a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "findTotalByTypeAndMediapackageId", query = "SELECT COUNT(a) FROM Annotation a WHERE a.mediapackageId = :mediapackageId AND a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "findTotalByIntervall", query = "SELECT COUNT(a) FROM Annotation a WHERE :begin <= a.created AND a.created <= :end AND a.userId = :userId"),
        @NamedQuery(name = "findDistinctEpisodeIdTotalByIntervall", query = "SELECT COUNT(distinct a.mediapackageId) FROM Annotation a WHERE :begin <= a.created AND a.created <= :end AND a.userId = :userId"),
        @NamedQuery(name = "findTotalByTypeAndIntervall", query = "SELECT COUNT(a) FROM Annotation a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type AND a.userId = :userId"),
        @NamedQuery(name = "updateAnnotation", query = "UPDATE Annotation a SET a.value = :value WHERE a.annotationId = :annotationId") })
@XmlType(name = "annotation", namespace = "http://annotation.opencastproject.org")
@XmlRootElement(name = "annotation", namespace = "http://annotation.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnnotationImpl implements Annotation {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.AUTO)
  @XmlElement(name = "annotationId")
  private Long annotationId;

  @Column(name = "mediapackage", length = 128)
  @XmlElement(name = "mediapackageId")
  private String mediapackageId;

  @Column(name = "user_id", length = 255)
  @XmlElement(name = "userId")
  private String userId;

  @Column(name = "session", length = 50)
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

  @Column(name = "type", length = 128)
  @XmlElement(name = "type")
  private String type;

  @Column(name = "private")
  @XmlElement(name = "isPrivate")
  private Boolean privateAnnotation = false;

  @Lob
  @Column(name = "value", length = 65535)
  @XmlElement(name = "value")
  private String value;

  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "created")
  private Date created = new Date();

  /**
   * A no-arg constructor needed by JAXB
   */
  public AnnotationImpl() {
  }

  public Long getAnnotationId() {
    return annotationId;
  }

  public void setAnnotationId(Long annotationId) {
    this.annotationId = annotationId;
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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
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
