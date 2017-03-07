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
package org.opencastproject.oaipmh.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity(name = "OaiPmhEntity") @IdClass(OaiPmhEntityId.class)
@Table(name = "mh_oaipmh", uniqueConstraints = @UniqueConstraint(columnNames = { "modification_date" }))
@NamedQueries({ @NamedQuery(name = "OaiPmh.findById", query = "SELECT o FROM OaiPmhEntity o WHERE o.mediaPackageId=:mediaPackageId AND o.repositoryId=:repository AND o.organization=:organization") })
public class OaiPmhEntity {

  /** media package id, primary key */
  @Id
  @Column(name = "id", length = 128)
  private String mediaPackageId;

  /** Organization id */
  @Id
  @Column(name = "organization", length = 128)
  protected String organization;

  /** Repository id */
  @Id
  @Column(name = "repo_id")
  private String repositoryId;

  /** Series id */
  @Column(name = "series_id")
  private String series;

  /** Flag indicating deletion. */
  @Column(name = "deleted")
  private boolean deleted = false;

  /** The last modification date */
  @Column(
      name = "modification_date",
      insertable = false,
      updatable = false,
      // this is H2 syntax - Opencast uses a dedicated database dependent schema in production
      columnDefinition = "TIMESTAMP AS CURRENT_TIMESTAMP()")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  /** Serialized media package */
  @Lob
  @Column(name = "mediapackage_xml", length = 65535)
  private String mediaPackageXML;

  /** Serialized series dublincore */
  @Lob
  @Column(name = "series_dublincore_xml", length = 65535)
  private String seriesDublinCoreXML;

  /** Serialized series ACL XML */
  @Lob
  @Column(name = "series_acl_xml", length = 65535)
  private String seriesAclXML;

  /** Serialized episode dublincore */
  @Lob
  @Column(name = "episode_dublincore_xml", length = 65535)
  private String episodeDublinCoreXML;

  /**
   * Default constructor without any import.
   */
  public OaiPmhEntity() {
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
   * @return the deletion flag
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * Sets the deletion flag
   * 
   * @param deleted
   *          the deletion flag
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * @return the series identifier
   */
  public String getSeries() {
    return series;
  }

  /**
   * Sets the series identifier
   * 
   * @param series
   *          the series identifier
   */
  public void setSeries(String series) {
    this.series = series;
  }

  /**
   * @return the modification date
   */
  public Date getModificationDate() {
    return modificationDate;
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
   * @return the repository id
   */
  public String getRepositoryId() {
    return repositoryId;
  }

  /**
   * Sets the repository id
   * 
   * @param repositoryId
   */
  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  /**
   * @return the serialized series dublincore
   */
  public String getSeriesDublinCoreXML() {
    return seriesDublinCoreXML;
  }

  /**
   * Sets the serialized series dublincore
   * 
   * @param seriesDublinCoreXML
   */
  public void setSeriesDublinCoreXML(String seriesDublinCoreXML) {
    this.seriesDublinCoreXML = seriesDublinCoreXML;
  }

  /**
   * @return the serialized episode dublincore
   */
  public String getEpisodeDublinCoreXML() {
    return episodeDublinCoreXML;
  }

  /**
   * Sets the serialized episode dublincore
   * 
   * @param episodeDublinCoreXML
   */
  public void setEpisodeDublinCoreXML(String episodeDublinCoreXML) {
    this.episodeDublinCoreXML = episodeDublinCoreXML;
  }

  public String getSeriesAclXML() {
    return seriesAclXML;
  }

  public void setSeriesAclXML(String seriesAclXML) {
    this.seriesAclXML = seriesAclXML;
  }
}
