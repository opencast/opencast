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

package org.opencastproject.annotation.impl;

import org.opencastproject.annotation.api.Annotation;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
@Access(AccessType.FIELD)
@Table(name = "oc_annotation", indexes = {
    @Index(name = "IX_oc_annotation_created", columnList = "created"),
    @Index(name = "IX_oc_annotation_inpoint", columnList = "inpoint"),
    @Index(name = "IX_oc_annotation_outpoint", columnList = "outpoint"),
    @Index(name = "IX_oc_annotation_mediapackage", columnList = "mediapackage"),
    @Index(name = "IX_oc_annotation_private", columnList = "private"),
    @Index(name = "IX_oc_annotation_user", columnList = "user_id"),
    @Index(name = "IX_oc_annotation_session", columnList = "session"),
    @Index(name = "IX_oc_annotation_type", columnList = "type")
})
@NamedQueries({
    @NamedQuery(
        name = "findAnnotations",
        query = "SELECT a FROM Annotation a "
            + "WHERE (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findAnnotationsByMediapackageId",
        query = "SELECT a FROM Annotation a "
            + "WHERE a.mediapackageId = :mediapackageId "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findAnnotationsByType",
        query = "SELECT a FROM Annotation a "
            + "WHERE a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findAnnotationsByTypeAndMediapackageId",
        query = "SELECT a FROM Annotation a "
            + "WHERE a.mediapackageId = :mediapackageId "
            + "AND a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findAnnotationsByTypeAndMediapackageIdOrderByOutpointDESC",
        query = "SELECT a FROM Annotation a "
            + "WHERE a.mediapackageId = :mediapackageId "
            + "AND a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE)) "
            + "ORDER BY a.outpoint DESC"
    ),
    @NamedQuery(
        name = "findAnnotationsByIntervall",
        query = "SELECT a FROM Annotation a "
            + "WHERE :begin <= a.created "
            + "AND a.created <= :end "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findAnnotationsByTypeAndIntervall",
        query = "SELECT a FROM Annotation a "
            + "WHERE :begin <= a.created "
            + "AND a.created <= :end "
            + "AND a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotal",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotalByMediapackageId",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE a.mediapackageId = :mediapackageId "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotalByType",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotalByTypeAndMediapackageId",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE a.mediapackageId = :mediapackageId "
            + "AND a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotalByIntervall",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE :begin <= a.created "
            + "AND a.created <= :end "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findDistinctEpisodeIdTotalByIntervall",
        query = "SELECT COUNT(distinct a.mediapackageId) FROM Annotation a "
            + "WHERE :begin <= a.created "
            + "AND a.created <= :end "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "findTotalByTypeAndIntervall",
        query = "SELECT COUNT(a) FROM Annotation a "
            + "WHERE :begin <= a.created "
            + "AND a.created <= :end "
            + "AND a.type = :type "
            + "AND (a.privateAnnotation = FALSE OR (a.userId = :userId AND a.privateAnnotation = TRUE))"
    ),
    @NamedQuery(
        name = "updateAnnotation",
        query = "UPDATE Annotation a SET a.value = :value "
            + "WHERE a.annotationId = :annotationId"
    ),
})
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

  @Column(name = "user_id")
  @XmlElement(name = "userId")
  private String userId;

  @Column(name = "session", length = 128)
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

  @Override
  public Long getAnnotationId() {
    return annotationId;
  }

  @Override
  public void setAnnotationId(Long annotationId) {
    this.annotationId = annotationId;
  }

  @Override
  public String getMediapackageId() {
    return mediapackageId;
  }

  @Override
  public void setMediapackageId(String mediapackageId) {
    this.mediapackageId = mediapackageId;
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
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public int getInpoint() {
    return inpoint;
  }

  @Override
  public void setInpoint(int inpoint) {
    this.inpoint = inpoint;
    updateLength();
  }

  @Override
  public int getOutpoint() {
    return outpoint;
  }

  @Override
  public void setOutpoint(int outpoint) {
    this.outpoint = outpoint;
    updateLength();
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public Date getCreated() {
    return created;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  private void updateLength() {
    this.length = this.outpoint - this.inpoint;
  }

  @Override
  public Boolean getPrivate() {
    return this.privateAnnotation;
  }

  @Override
  public void setPrivate(Boolean isPrivate) {
    this.privateAnnotation = isPrivate;
  }

}
