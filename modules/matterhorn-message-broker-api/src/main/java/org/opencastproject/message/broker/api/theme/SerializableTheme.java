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
package org.opencastproject.message.broker.api.theme;

import java.io.Serializable;
import java.util.Date;

/**
 * Business object of themes class
 */
public class SerializableTheme implements Serializable {

  private static final long serialVersionUID = 618342361307578393L;

  private final long id;
  private final Date creationDate;
  private final boolean isDefault;
  private final String creator;
  private final String name;

  private final String description;
  private final boolean bumperActive;
  private final String bumperFile;
  private final boolean trailerActive;
  private final String trailerFile;
  private final boolean titleSlideActive;
  private final String titleSlideMetadata;
  private final String titleSlideBackground;
  private final boolean licenseSlideActive;
  private final String licenseSlideBackground;
  private final String licenseSlideDescription;
  private final boolean watermarkActive;
  private final String watermarkFile;
  private final String watermarkPosition;

  public SerializableTheme(Long id, Date creationDate, boolean isDefault, String creator, String name,
          String description, boolean bumperActive, String bumperFile, boolean trailerActive, String trailerFile,
          boolean titleSlideActive, String titleSlideMetadata, String titleSlideBackground, boolean licenseSlideActive,
          String licenseSlideBackground, String licenseSlideDescription, boolean watermarkActive, String watermarkFile,
          String watermarkPosition) {
    this.id = id;
    this.creationDate = creationDate;
    this.isDefault = isDefault;
    this.creator = creator;
    this.name = name;
    this.description = description;
    this.bumperActive = bumperActive;
    this.bumperFile = bumperFile;
    this.trailerActive = trailerActive;
    this.trailerFile = trailerFile;
    this.titleSlideActive = titleSlideActive;
    this.titleSlideMetadata = titleSlideMetadata;
    this.titleSlideBackground = titleSlideBackground;
    this.licenseSlideActive = licenseSlideActive;
    this.licenseSlideBackground = licenseSlideBackground;
    this.licenseSlideDescription = licenseSlideDescription;
    this.watermarkActive = watermarkActive;
    this.watermarkFile = watermarkFile;
    this.watermarkPosition = watermarkPosition;
  }

  public long getId() {
    return id;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public String getCreator() {
    return creator;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isBumperActive() {
    return bumperActive;
  }

  public String getBumperFile() {
    return bumperFile;
  }

  public boolean isTrailerActive() {
    return trailerActive;
  }

  public String getTrailerFile() {
    return trailerFile;
  }

  public boolean isTitleSlideActive() {
    return titleSlideActive;
  }

  public String getTitleSlideMetadata() {
    return titleSlideMetadata;
  }

  public String getTitleSlideBackground() {
    return titleSlideBackground;
  }

  public boolean isLicenseSlideActive() {
    return licenseSlideActive;
  }

  public String getLicenseSlideBackground() {
    return licenseSlideBackground;
  }

  public String getLicenseSlideDescription() {
    return licenseSlideDescription;
  }

  public boolean isWatermarkActive() {
    return watermarkActive;
  }

  public String getWatermarkFile() {
    return watermarkFile;
  }

  public String getWatermarkPosition() {
    return watermarkPosition;
  }

  @Override
  public String toString() {
    return new StringBuilder(Long.toString(id)).append(":").append(name).toString();
  }

}
