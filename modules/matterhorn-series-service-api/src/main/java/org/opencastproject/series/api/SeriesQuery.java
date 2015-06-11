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

package org.opencastproject.series.api;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * Query object used for storing search parameters.
 *
 */
public class SeriesQuery {

  /** Maximum number of results returned */
  protected int count;
  /** Start page number */
  protected int startPage;
  /** Free text search */
  protected String text;
  /** Series id search */
  protected String seriesId;
  /** Series title search */
  protected String seriesTitle;
  /** Creator search */
  protected String creator;
  /** Contributor search */
  protected String contributor;
  /** Language search */
  protected String language;
  /** License search */
  protected String license;
  /** Subject search */
  protected String subject;
  /** Publisher search */
  protected String publisher;
  /** Abstract search */
  protected String seriesAbstract;
  /** Description search */
  protected String description;
  /** Created from search */
  protected Date createdFrom;
  /** Created to search */
  protected Date createdTo;
  /** Available from search */
  protected Date availableFrom;
  /** Available to search */
  protected Date availableTo;
  /** Rights holder search */
  protected String rightsHolder;
  /**
   * Show only series for which the current user may edit the series (true), otherwise show series for which the current
   * user can contribute content toward the series
   */
  protected boolean edit;
  /** Sort by field */
  protected Sort sort = Sort.TITLE;
  /** Sort order */
  protected boolean sortAscending = true;

  /** Sort fields */
  public enum Sort {
    TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, TEMPORAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE
  }

  /**
   * Set search by license
   *
   * @param license
   * @return
   */
  public SeriesQuery setLicense(String license) {
    if (StringUtils.isNotBlank(license)) {
      this.license = license.trim();
    }
    return this;
  }

  /**
   * Set search by abstract
   *
   * @param seriesAbstract
   * @return
   */
  public SeriesQuery setSeriesAbstract(String seriesAbstract) {
    if (StringUtils.isNotBlank(seriesAbstract)) {
      this.seriesAbstract = seriesAbstract.trim();
    }
    return this;
  }

  /**
   * Set maximum number of results
   *
   * @param count
   * @return
   */
  public SeriesQuery setCount(int count) {
    this.count = count;
    return this;
  }

  /**
   * Set start page
   *
   * @param startPage
   * @return
   */
  public SeriesQuery setStartPage(int startPage) {
    this.startPage = startPage;
    return this;
  }

  /**
   * Set search by created from
   *
   * @param createdFrom
   * @return
   */
  public SeriesQuery setCreatedFrom(Date createdFrom) {
    this.createdFrom = createdFrom;
    return this;
  }

  /**
   * Set search by created to
   *
   * @param createdTo
   * @return
   */
  public SeriesQuery setCreatedTo(Date createdTo) {
    this.createdTo = createdTo;
    return this;
  }

  /**
   * Set search over all text fields
   *
   * @param text
   * @return
   */
  public SeriesQuery setText(String text) {
    if (StringUtils.isNotBlank(text)) {
      this.text = text.trim();
    }
    return this;
  }

  /**
   * Set search by series id
   *
   * @param seriesId
   * @return
   */
  public SeriesQuery setSeriesId(String seriesId) {
    if (StringUtils.isNotBlank(seriesId)) {
      this.seriesId = seriesId.trim();
    }
    return this;
  }

  /**
   * Set search by series title
   *
   * @param seriesTitle
   * @return
   */
  public SeriesQuery setSeriesTitle(String seriesTitle) {
    if (StringUtils.isNotBlank(seriesTitle)) {
      this.seriesTitle = seriesTitle.trim();
    }
    return this;
  }

  /**
   * Set search by creator
   *
   * @param creator
   * @return
   */
  public SeriesQuery setCreator(String creator) {
    if (StringUtils.isNotBlank(creator)) {
      this.creator = creator.trim();
    }
    return this;
  }

  /**
   * Set search by contributor
   *
   * @param contributor
   * @return
   */
  public SeriesQuery setContributor(String contributor) {
    if (StringUtils.isNotBlank(contributor)) {
      this.contributor = contributor.trim();
    }
    return this;
  }

  /**
   * Set search by language
   *
   * @param language
   * @return
   */
  public SeriesQuery setLanguage(String language) {
    if (StringUtils.isNotBlank(language)) {
      this.language = language.trim();
    }
    return this;
  }

  /**
   * Set search by subject
   *
   * @param subject
   * @return
   */
  public SeriesQuery setSubject(String subject) {
    if (StringUtils.isNotBlank(subject)) {
      this.subject = subject.trim();
    }
    return this;
  }

  /**
   * Set search by publisher
   *
   * @param publisher
   * @return
   */
  public SeriesQuery setPublisher(String publisher) {
    if (StringUtils.isNotBlank(subject)) {
      this.publisher = publisher.trim();
    }
    return this;
  }

  /**
   * Set search by description
   *
   * @param description
   * @return
   */
  public SeriesQuery setDescription(String description) {
    if (StringUtils.isNotBlank(subject)) {
      this.description = description.trim();
    }
    return this;
  }

  /**
   * Set search by available from
   *
   * @param availableFrom
   * @return
   */
  public SeriesQuery setAvailableFrom(Date availableFrom) {
    this.availableFrom = availableFrom;
    return this;
  }

  /**
   * Set search by available to
   *
   * @param availableTo
   * @return
   */
  public SeriesQuery setAvailableTo(Date availableTo) {
    this.availableTo = availableTo;
    return this;
  }

  /**
   * Set search by rights holder
   *
   * @param rightsHolder
   * @return
   */
  public SeriesQuery setRightsHolder(String rightsHolder) {
    if (StringUtils.isNotBlank(subject)) {
      this.rightsHolder = rightsHolder.trim();
    }
    return this;
  }

  /**
   * Set sort field with ascending order
   *
   * @param sort
   * @return
   */
  public SeriesQuery withSort(Sort sort) {
    return withSort(sort, true);
  }

  /**
   * Set sort field with sort order
   *
   * @param sort
   * @param ascending
   * @return
   */
  public SeriesQuery withSort(Sort sort, boolean ascending) {
    this.sort = sort;
    this.sortAscending = ascending;
    return this;
  }

  /**
   * Get sort field
   *
   * @return
   */
  public Sort getSort() {
    return sort;
  }

  /**
   * Whether sort order is ascending
   *
   * @return
   */
  public boolean isSortAscending() {
    return sortAscending;
  }

  /**
   * Get result count
   *
   * @return
   */
  public int getCount() {
    return count;
  }

  /**
   * Get start page
   *
   * @return
   */
  public int getStartPage() {
    return startPage;
  }

  /**
   * Get text
   *
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * Get series id
   *
   * @return
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Get series title
   *
   * @return
   */
  public String getSeriesTitle() {
    return seriesTitle;
  }

  /**
   * Get creator
   *
   * @return
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Get contributor
   *
   * @return
   */
  public String getContributor() {
    return contributor;
  }

  /**
   * Get language
   *
   * @return
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Get license
   *
   * @return
   */
  public String getLicense() {
    return license;
  }

  /**
   * Get subject
   *
   * @return
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Get publisher
   *
   * @return
   */
  public String getPublisher() {
    return publisher;
  }

  /**
   * Get abstract
   *
   * @return
   */
  public String getAbstract() {
    return seriesAbstract;
  }

  /**
   * Get description
   *
   * @return
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get created from
   *
   * @return
   */
  public Date getCreatedFrom() {
    return createdFrom;
  }

  /**
   * Get created to
   *
   * @return
   */
  public Date getCreatedTo() {
    return createdTo;
  }

  /**
   * Get available from
   *
   * @return
   */
  public Date getAvailableFrom() {
    return availableFrom;
  }

  /**
   * Get available to
   *
   * @return
   */
  public Date getAvailableTo() {
    return availableTo;
  }

  /**
   * Get rights holder
   *
   * @return
   */
  public String getRightsHolder() {
    return rightsHolder;
  }

  /**
   * Whether the results for this query should contain only series that the current user can edit.
   *
   * @return
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * Set the edit flag.
   *
   * @param edit
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }

}
