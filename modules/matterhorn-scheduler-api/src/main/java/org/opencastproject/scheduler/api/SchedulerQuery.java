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
package org.opencastproject.scheduler.api;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Query object used for storing search parameters.
 *
 */
public class SchedulerQuery {

  /** Free text search */
  protected String text;
  /** Identifier search */
  protected String identifier;
  /** ID Set */
  protected List<Long> idList;
  /** Title search */
  protected String title;
  /** Series id search */
  protected String seriesId;
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
  /** Spatial search */
  protected String spatial;
  /** Publisher search */
  protected String publisher;
  /** Abstract search */
  protected String eventAbstract;
  /** Description search */
  protected String description;
  /** Created from search */
  protected Date createdFrom;
  /** Created to search */
  protected Date createdTo;
  /** Starts from search */
  protected Date startsFrom;
  /** Starts to search */
  protected Date startsTo;
  /** Ends from search */
  protected Date endsFrom;
  /** Ends to search */
  protected Date endsTo;
  /** Rights holder search */
  protected String rightsHolder;
  /** Whether the event is opted out */
  protected boolean optOut = false;
  /** Whether the event is blacklisted */
  protected boolean blacklisted = false;
  /** Sort by field */
  protected Sort sort = Sort.EVENT_START;
  /** Sort order */
  protected boolean sortAscending = true;

  /** Sort fields */
  public enum Sort {
    TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE, EVENT_START
  }

  /**
   * Set search by license
   *
   * @param license
   * @return
   */
  public SchedulerQuery setLicense(String license) {
    if (StringUtils.isNotBlank(license)) {
      this.license = license.toLowerCase();
    }
    return this;
  }

  /**
   * Set search by abstract
   *
   * @param eventAbstract
   * @return
   */
  public SchedulerQuery setEventAbstract(String eventAbstract) {
    if (StringUtils.isNotBlank(eventAbstract)) {
      this.eventAbstract = eventAbstract.toLowerCase();
    }
    return this;
  }

  /**
   * Set search by created from
   *
   * @param createdFrom
   * @return
   */
  public SchedulerQuery setCreatedFrom(Date createdFrom) {
    this.createdFrom = createdFrom;
    return this;
  }

  /**
   * Set search by created to
   *
   * @param createdTo
   * @return
   */
  public SchedulerQuery setCreatedTo(Date createdTo) {
    this.createdTo = createdTo;
    return this;
  }

  /**
   * Set search over all text fields
   *
   * @param text
   * @return
   */
  public SchedulerQuery setText(String text) {
    if (StringUtils.isNotBlank(text)) {
      this.text = text;
    }
    return this;
  }

  /**
   * Set search by series id
   *
   * @param seriesId
   * @return
   */
  public SchedulerQuery setSeriesId(String seriesId) {
    if (StringUtils.isNotBlank(seriesId)) {
      this.seriesId = seriesId;
    }
    return this;
  }

  /**
   * Set search by creator
   *
   * @param creator
   * @return
   */
  public SchedulerQuery setCreator(String creator) {
    if (StringUtils.isNotBlank(creator)) {
      this.creator = creator;
    }
    return this;
  }

  /**
   * Set search by contributor
   *
   * @param contributor
   * @return
   */
  public SchedulerQuery setContributor(String contributor) {
    if (StringUtils.isNotBlank(contributor)) {
      this.contributor = contributor;
    }
    return this;
  }

  /**
   * Set search by language
   *
   * @param language
   * @return
   */
  public SchedulerQuery setLanguage(String language) {
    if (StringUtils.isNotBlank(language)) {
      this.language = language;
    }
    return this;
  }

  /**
   * Set search by subject
   *
   * @param subject
   * @return
   */
  public SchedulerQuery setSubject(String subject) {
    if (StringUtils.isNotBlank(subject)) {
      this.subject = subject;
    }
    return this;
  }

  /**
   * Set search by publisher
   *
   * @param publisher
   * @return
   */
  public SchedulerQuery setPublisher(String publisher) {
    if (StringUtils.isNotBlank(subject)) {
      this.publisher = publisher;
    }
    return this;
  }

  /**
   * Set search by description
   *
   * @param description
   * @return
   */
  public SchedulerQuery setDescription(String description) {
    if (StringUtils.isNotBlank(subject)) {
      this.description = description;
    }
    return this;
  }

  /**
   * Set search by spatial
   *
   * @param spatial
   * @return
   */
  public SchedulerQuery setSpatial(String spatial) {
    if (StringUtils.isNotBlank(spatial)) {
      this.spatial = spatial;
    }
    return this;
  }

  /**
   * Set search by start date of event
   *
   * @param startsFrom
   * @return
   */
  public SchedulerQuery setStartsFrom(Date startsFrom) {
    this.startsFrom = startsFrom;
    return this;
  }

  /**
   * Set search by start date of event
   *
   * @param startsTo
   * @return
   */
  public SchedulerQuery setStartsTo(Date startsTo) {
    this.startsTo = startsTo;
    return this;
  }

  /**
   * Set search by end date of event.
   *
   * @param endsFrom
   * @return
   */
  public SchedulerQuery setEndsFrom(Date endsFrom) {
    this.endsFrom = endsFrom;
    return this;
  }

  /**
   * Set search by end date of event.
   *
   * @param endsTo
   * @return
   */
  public SchedulerQuery setEndsTo(Date endsTo) {
    this.endsTo = endsTo;
    return this;
  }

  /**
   * Set search by rights holder
   *
   * @param rightsHolder
   * @return
   */
  public SchedulerQuery setRightsHolder(String rightsHolder) {
    if (StringUtils.isNotBlank(subject)) {
      this.rightsHolder = rightsHolder;
    }
    return this;
  }

  /**
   * Set search by opt out status
   *
   * @param optOut
   * @return
   */
  public SchedulerQuery setOptOut(boolean optOut) {
    this.optOut = optOut;
    return this;
  }

  /**
   * Set search by blacklist status
   *
   * @param blacklisted
   * @return
   */
  public SchedulerQuery setBlacklisted(boolean blacklisted) {
    this.blacklisted = blacklisted;
    return this;
  }

  /**
   * Set search by title.
   *
   * @param title
   * @return
   */
  public SchedulerQuery setTitle(String title) {
    if (StringUtils.isNotBlank(title)) {
      this.title = title;
    }
    return this;
  }

  /**
   * Set search by identifier.
   *
   * @param identifier
   * @return
   */
  public SchedulerQuery setIdentifier(String identifier) {
    if (StringUtils.isNotBlank(identifier)) {
      this.identifier = identifier;
    }
    return this;
  }

  /**
   * Set a list of identifiers
   *
   * @param ids
   * @return
   */
  public SchedulerQuery withIdInList(List<Long> ids) {
    if (!ids.isEmpty()) {
      this.idList = ids;
    }
    return this;
  }

  /**
   * Set sort field with ascending order
   *
   * @param sort
   * @return
   */
  public SchedulerQuery withSort(Sort sort) {
    return withSort(sort, true);
  }

  /**
   * Set sort field with sort order
   *
   * @param sort
   * @param ascending
   * @return
   */
  public SchedulerQuery withSort(Sort sort, boolean ascending) {
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
    return eventAbstract;
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
   * Get spatial
   *
   * @return
   */
  public String getSpatial() {
    return spatial;
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
   * Get starts from
   *
   * @return
   */
  public Date getStartsFrom() {
    return startsFrom;
  }

  /**
   * Get starts to
   *
   * @return
   */
  public Date getStartsTo() {
    return startsTo;
  }

  /**
   * Get ends from
   *
   * @return
   */
  public Date getEndsFrom() {
    return endsFrom;
  }

  public Date getEndsTo() {
    return endsTo;
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
   * Get opt out status
   *
   * @return
   */
  public boolean isOptOut() {
    return optOut;
  }

  /**
   * Get blacklist status
   *
   * @return
   */
  public boolean isBlacklisted() {
    return blacklisted;
  }

  public String getIdentifier() {
    return identifier;
  }

  public List<Long> getIdsList() {
    return idList;
  }

  public String getTitle() {
    return title;
  }

}
