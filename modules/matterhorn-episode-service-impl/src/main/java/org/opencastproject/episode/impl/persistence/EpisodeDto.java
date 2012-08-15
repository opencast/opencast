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
package org.opencastproject.episode.impl.persistence;

import org.opencastproject.episode.api.Version;
import org.opencastproject.util.data.Option;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;

/**
 * Entity object for storing episodes in persistence storage. Media package id, version and organization are stored as
 * primary key.
 */
@Entity(name = "Episode")
@Table(name = "episode_episode")
@NamedQueries({
        @NamedQuery(name = "Episode.findAll", query = "SELECT e FROM Episode e"),
        @NamedQuery(name = "Episode.findByIdAndVersion", query = "SELECT e FROM Episode e WHERE e.mediaPackageId=:mediaPackageId AND e.version=:version"),
        @NamedQuery(name = "Episode.findLatestById", query = "SELECT e FROM Episode e WHERE e.mediaPackageId=:mediaPackageId "
                + "AND e.version = (SELECT MAX(e2.version) FROM Episode e2 WHERE e2.mediaPackageId =:mediaPackageId)"),
        @NamedQuery(name = "Episode.findAllById", query = "SELECT e FROM Episode e WHERE e.mediaPackageId=:mediaPackageId") })
public final class EpisodeDto {

  /** Episode ID, primary key */
  @Id
  @Column(name = "mediapackage_id", length = 128)
  private String mediaPackageId;

  /** Episode ID, primary key */
  @Id
  @Column(name = "version")
  private long version;

  /** The organization id */
  @Column(name = "organization_id", length = 128)
  private String organization;

  /** The archive latest version */
  @Column(name = "latest_version")
  private boolean latestVersion;

  /** The media package locking */
  @Column(name = "locked")
  private boolean locked;

  /** The media package deleted */
  @Column(name = "deletion_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date deletionDate;

  /** The media package deleted */
  @Column(name = "modification_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  /** Serialized access control */
  @Lob
  @Column(name = "access_control", length = 65535)
  private String accessControl;

  /** Serialized media package */
  @Lob
  @Column(name = "mediapackage", length = 65535)
  private String mediaPackageXML;

  /**
   * Default constructor without any import.
   */
  public EpisodeDto() {
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
   * @return the serialized access control list
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
   * @return media package id
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * Sets media package id
   * 
   * @param media
   *          package id
   */
  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  /**
   * @return the serialized media package
   */
  public String getMediaPackageXML() {
    return mediaPackageXML;
  }

  /**
   * Sets the serialized media package
   * 
   * @param mediaPackageXML
   *          the serialized media package
   */
  public void setMediaPackageXML(String mediaPackageXML) {
    this.mediaPackageXML = mediaPackageXML;
  }

  /**
   * @return the archive version
   */
  public long getVersion() {
    return version;
  }

  /**
   * Sets the archive version
   * 
   * @param version
   *          the archive version
   */
  public void setVersion(long version) {
    this.version = version;
  }

  /**
   * @return latest version
   */
  public boolean isLatestVersion() {
    return latestVersion;
  }

  /**
   * Sets the latest version
   * 
   * @param latestVersion
   *          the latest version
   */
  public void setLatestVersion(boolean latestVersion) {
    this.latestVersion = latestVersion;
  }

  /**
   * @return the lock state
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * Sets the lock state
   * 
   * @param locked
   *          the lock state
   */
  public void setLocked(boolean locked) {
    this.locked = locked;
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

  public static Option<EpisodeDto> findByIdAndVersion(EntityManager em, String mediaPackageId, Version version) {
    return runSingleResultQuery(em, "Episode.findByIdAndVersion",
                                tuple("version", version.value()),
                                tuple("mediaPackageId", mediaPackageId));
  }
}
