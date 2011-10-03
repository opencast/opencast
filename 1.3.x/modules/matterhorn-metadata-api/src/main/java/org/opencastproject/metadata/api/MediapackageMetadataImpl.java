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
package org.opencastproject.metadata.api;

import java.util.Date;

/**
 * Provides metadata for a {@link MediaPackageMetadata}.
 */
public class MediapackageMetadataImpl implements MediaPackageMetadata {

  protected String title;
  protected String seriesTitle;
  protected String seriesIdentifier;
  protected String[] creators;
  protected String[] contributors;
  protected String[] subjects;
  protected String language;
  protected String license;
  protected Date date;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the media package's title.
   * 
   * @param title
   *          the title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getSeriesTitle()
   */
  public String getSeriesTitle() {
    return seriesTitle;
  }

  /**
   * Sets the series title.
   * 
   * @param seriesTitle
   *          the series title
   */
  public void setSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getSeriesIdentifier()
   */
  public String getSeriesIdentifier() {
    return seriesIdentifier;
  }

  /**
   * Sets the series identifier
   * 
   * @param seriesIdentifier
   *          the series identifier
   */
  public void setSeriesIdentifier(String seriesIdentifier) {
    this.seriesIdentifier = seriesIdentifier;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getCreators()
   */
  public String[] getCreators() {
    if (creators == null)
      return new String[] {};
    return creators;
  }

  /**
   * Sets the list of creators.
   * 
   * @param creators
   *          the creators
   */
  public void setCreators(String[] creators) {
    this.creators = creators;
  }

  public String[] getContributors() {
    if (contributors == null)
      return new String[] {};
    return contributors;
  }

  /**
   * Sets the mediapackage's contributors.
   * 
   * @param contributors
   *          the contributors
   */
  public void setContributors(String[] contributors) {
    this.contributors = contributors;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getSubjects()
   */
  public String[] getSubjects() {
    if (subjects == null)
      return new String[] {};
    return subjects;
  }

  /**
   * Sets the mediapackage's subjects.
   * 
   * @param subjects
   *          the subjects
   */
  public void setSubjects(String[] subjects) {
    this.subjects = subjects;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getLanguage()
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Sets the mediapackage's language.
   * 
   * @param language
   *          the language
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getLicense()
   */
  public String getLicense() {
    return license;
  }

  /**
   * Sets the mediapackage license.
   * 
   * @param license
   *          the license
   */
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageMetadata#getDate()
   */
  public Date getDate() {
    return date;
  }

  /**
   * Sets the mediapackage's creation date.
   * 
   * @param date
   *          the creation date
   */
  public void setDate(Date date) {
    this.date = date;
  }

}
