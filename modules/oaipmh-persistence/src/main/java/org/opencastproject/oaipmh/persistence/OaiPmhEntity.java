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

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity(name = "OaiPmhEntity")
@IdClass(OaiPmhEntityId.class)
@Table(name = "oc_oaipmh", uniqueConstraints = @UniqueConstraint(columnNames = { "modification_date" }))
@NamedQueries({ @NamedQuery(name = "OaiPmh.findById",
        query = "SELECT o FROM OaiPmhEntity o "
                + "WHERE o.mediaPackageId=:mediaPackageId"
                + " AND o.repositoryId=:repository"
                + " AND o.organization=:organization") })
public class OaiPmhEntity {

  /** media package id, primary key */
  @Id
  @Column(name = "mp_id", length = 64)
  private String mediaPackageId;

  /** Organization id */
  @Id
  @Column(name = "organization", length = 96)
  protected String organization;

  /** Repository id */
  @Id
  @Column(name = "repo_id", length = 12)
  private String repositoryId;

  /** Series id */
  @Column(name = "series_id",length = 128)
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
  @Column(name = "mediapackage_xml", length = 65535, nullable = false)
  private String mediaPackageXML;

  /** List of serialized media package element entities */
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumns({
    @JoinColumn(name = "mp_id", referencedColumnName = "mp_id", nullable = false, table = "oc_oaipmh_elements", insertable = false, updatable = false),
    @JoinColumn(name = "organization", referencedColumnName = "organization", nullable = false, table = "oc_oaipmh_elements", insertable = false, updatable = false),
    @JoinColumn(name = "repo_id", referencedColumnName = "repo_id", nullable = false, table = "oc_oaipmh_elements", insertable = false, updatable = false)
  })
  private List<OaiPmhElementEntity> mediaPackageElements = new ArrayList<>();

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
   * @return serialized media package attachment entities
   */
  public List<OaiPmhElementEntity> getAttachments() {
    return getMediaPackageElementsOfType(Attachment.TYPE.name());
  }

  /**
   * @return serialized media package catalog entities
   */
  public List<OaiPmhElementEntity> getCatalogs() {
    return getMediaPackageElementsOfType(Catalog.TYPE.name());
  }

  /**
   * A list of serialized media package element entities, filtered by given type
   *
   * @param elementType type of media package element to filter on
   * @return serialized media package elements of given type
   */
  private List<OaiPmhElementEntity> getMediaPackageElementsOfType(String elementType) {
    // as we do not expect to many media package elements per media package, we can filter them in java
    List<OaiPmhElementEntity> filteredElements = new ArrayList<>();
    for (OaiPmhElementEntity element : mediaPackageElements) {
      if (StringUtils.equals(elementType, element.getElementType()))
        filteredElements.add(element);
    }
    return filteredElements;
  }

  /**
   * @return all serialized media package element entities
   */
  public List<OaiPmhElementEntity> getMediaPackageElements() {
    return mediaPackageElements;
  }

  /**
   * Add an serialized media package element
   *
   * @param mediaPackageElementEntity serialized media package element to add
   */
  public void addMediaPackageElement(OaiPmhElementEntity mediaPackageElementEntity) {
    mediaPackageElements.add(mediaPackageElementEntity);
    mediaPackageElementEntity.setOaiPmhEntity(this);
  }

  /**
   * Remove media package element entity from the list of elements
   *
   * @param mediaPackageElementEntity serialized media package element entity to remove
   */
  public void removeMediaPackageElement(OaiPmhElementEntity mediaPackageElementEntity) {
    mediaPackageElements.remove(mediaPackageElementEntity);
  }

  /**
   * Clear the list of media package element entities
   */
  public void removeAllMediaPackageElements() {
    mediaPackageElements.clear();
  }
}
