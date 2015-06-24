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

package org.opencastproject.themes.persistence;

import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.themes.Theme;
import org.opencastproject.util.data.Option;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/** Entity object for themes. */
@Entity(name = "Themes")
@Table(name = "mh_themes")
@NamedQueries({
        @NamedQuery(name = "Themes.count", query = "SELECT COUNT(t) FROM Themes t WHERE t.organization = :org"),
        @NamedQuery(name = "Themes.findById", query = "SELECT t FROM Themes t WHERE t.id = :id AND t.organization = :org"),
        @NamedQuery(name = "Themes.findByOrg", query = "SELECT t FROM Themes t WHERE t.organization = :org"),
        @NamedQuery(name = "Themes.findByUserName", query = "SELECT t FROM Themes t WHERE t.username = :username AND t.organization = :org"),
        @NamedQuery(name = "Themes.clear", query = "DELETE FROM Themes t WHERE t.organization = :org") })
public class ThemeDto {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private long id;

  @Column(name = "organization", nullable = false)
  private String organization;

  @Column(name = "creation_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;

  @Column(name = "isDefault", nullable = false)
  private boolean isDefault = false;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "bumper_active", nullable = false)
  private boolean bumperActive = false;

  @Column(name = "bumper_file")
  private String bumperFile;

  @Column(name = "trailer_active", nullable = false)
  private boolean trailerActive = false;

  @Column(name = "trailer_file")
  private String trailerFile;

  @Column(name = "title_slide_active", nullable = false)
  private boolean titleSlideActive = false;

  @Column(name = "title_slide_metadata")
  private String titleSlideMetadata;

  @Column(name = "title_slide_background")
  private String titleSlideBackground;

  @Column(name = "license_slide_active", nullable = false)
  private boolean licenseSlideActive = false;

  @Column(name = "license_slide_background")
  private String licenseSlideBackground;

  @Column(name = "license_slide_description")
  private String licenseSlideDescription;

  @Column(name = "watermark_active", nullable = false)
  private boolean watermarkActive = false;

  @Column(name = "watermark_file")
  private String watermarkFile;

  @Column(name = "watermark_position")
  private String watermarkPosition;

  /** Default constructor */
  public ThemeDto() {
  }

  /**
   * @return the business object model of this theme
   */
  public Theme toTheme(UserDirectoryService userDirectoryService) {
    User creator = userDirectoryService.loadUser(username);
    return new Theme(Option.some(id), creationDate, isDefault, creator, name, description, bumperActive, bumperFile,
            trailerActive, trailerFile, titleSlideActive, titleSlideMetadata, titleSlideBackground, licenseSlideActive,
            licenseSlideBackground, licenseSlideDescription, watermarkActive, watermarkFile, watermarkPosition);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isBumperActive() {
    return bumperActive;
  }

  public void setBumperActive(boolean bumperActive) {
    this.bumperActive = bumperActive;
  }

  public String getBumperFile() {
    return bumperFile;
  }

  public void setBumperFile(String bumperFile) {
    this.bumperFile = bumperFile;
  }

  public boolean isTrailerActive() {
    return trailerActive;
  }

  public void setTrailerActive(boolean trailerActive) {
    this.trailerActive = trailerActive;
  }

  public String getTrailerFile() {
    return trailerFile;
  }

  public void setTrailerFile(String trailerFile) {
    this.trailerFile = trailerFile;
  }

  public boolean isTitleSlideActive() {
    return titleSlideActive;
  }

  public void setTitleSlideActive(boolean titleSlideActive) {
    this.titleSlideActive = titleSlideActive;
  }

  public String getTitleSlideBackground() {
    return titleSlideBackground;
  }

  public void setTitleSlideBackground(String titleSlideBackground) {
    this.titleSlideBackground = titleSlideBackground;
  }

  public String getTitleSlideMetadata() {
    return titleSlideMetadata;
  }

  public void setTitleSlideMetadata(String titleSlideMetadata) {
    this.titleSlideMetadata = titleSlideMetadata;
  }

  public boolean isLicenseSlideActive() {
    return licenseSlideActive;
  }

  public void setLicenseSlideActive(boolean licenseSlideActive) {
    this.licenseSlideActive = licenseSlideActive;
  }

  public String getLicenseSlideBackground() {
    return licenseSlideBackground;
  }

  public void setLicenseSlideBackground(String licenseSlideBackground) {
    this.licenseSlideBackground = licenseSlideBackground;
  }

  public String getLicenseSlideDescription() {
    return licenseSlideDescription;
  }

  public void setLicenseSlideDescription(String licenseSlideDescription) {
    this.licenseSlideDescription = licenseSlideDescription;
  }

  public boolean isWatermarkActive() {
    return watermarkActive;
  }

  public void setWatermarkActive(boolean watermarkActive) {
    this.watermarkActive = watermarkActive;
  }

  public String getWatermarkFile() {
    return watermarkFile;
  }

  public void setWatermarkFile(String watermarkFile) {
    this.watermarkFile = watermarkFile;
  }

  public String getWatermarkPosition() {
    return watermarkPosition;
  }

  public void setWatermarkPosition(String watermarkPosition) {
    this.watermarkPosition = watermarkPosition;
  }

}
