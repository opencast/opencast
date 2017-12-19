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

package org.opencastproject.search.impl.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for storing search in persistence storage. Media package id is stored as primary key.
 */
@Entity(name = "SearchEntity")
@Table(name = "mh_search")
@NamedQueries({
        @NamedQuery(name = "Search.findAll", query = "SELECT s FROM SearchEntity s"),
        @NamedQuery(name = "Search.getCount", query = "SELECT COUNT(s) FROM SearchEntity s"),
        @NamedQuery(name = "Search.findById", query = "SELECT s FROM SearchEntity s WHERE s.mediaPackageId=:mediaPackageId"),
        @NamedQuery(name = "Search.findBySeriesId", query = "SELECT s FROM SearchEntity s WHERE s.seriesId=:seriesId"),
        @NamedQuery(name = "Search.getNoSeries", query = "SELECT s FROM SearchEntity s WHERE s.seriesId IS NULL")})
public class SearchEntity {

  /** media package id, primary key */
  @Id
  @Column(name = "id", length = 128)
  private String mediaPackageId;

  @Column(name = "series_id", length = 128)
  protected String seriesId;

  /** Organization id */
  @Column(name = "organization", length = 128)
  protected String organization;

  /** The media package deleted */
  @Column(name = "deletion_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date deletionDate;

  /** The media package deleted */
  @Column(name = "modification_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  /** Serialized media package */
  @Lob
  @Column(name = "mediapackage_xml", length = 65535)
  private String mediaPackageXML;

  /** Serialized access control */
  @Lob
  @Column(name = "access_control", length = 65535)
  protected String accessControl;

  /**
   * Default constructor without any import.
   */
  public SearchEntity() {
  }

  /**
   * Returns media package id.
   *
   * @return media package id
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * Sets media package id. Id length limit is 128 charachters.
   *
   * @param mediaPackageId
   */
  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  /**
   * Returns serialized media package.
   *
   * @return serialized media package
   */
  public String getMediaPackageXML() {
    return mediaPackageXML;
  }

  /**
   * Sets serialized media package
   *
   * @param mediaPackageXML
   */
  public void setMediaPackageXML(String mediaPackageXML) {
    this.mediaPackageXML = mediaPackageXML;
  }

  /**
   * Returns serialized access control
   *
   * @return serialized access control
   */
  public String getAccessControl() {
    return accessControl;
  }

  /**
   * Sets serialized access control.
   *
   * @param accessControl
   *          serialized access control
   */
  public void setAccessControl(String accessControl) {
    this.accessControl = accessControl;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization
   *          the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * @return the deletion date
   */
  public Date getDeletionDate() {
    return deletionDate;
  }

  /**
   * Sets the deletion date
   *
   * @param deletionDate
   *          the deletion date
   */
  public void setDeletionDate(Date deletionDate) {
    this.deletionDate = deletionDate;
  }

  /**
   * @return the modification date
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * Sets the modification date
   *
   * @param modificationDate
   *          the modification date
   */
  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  /**
   * @return the series Id for this search entry
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Sets the series ID
   *
   * @param seriesId
   *          the series ID
   */
  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }
}
