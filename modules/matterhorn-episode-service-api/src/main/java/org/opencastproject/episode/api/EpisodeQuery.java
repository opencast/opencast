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
package org.opencastproject.episode.api;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.Date;

/**
 * Represents a query to find search results.
 */
public class EpisodeQuery {
  private boolean includeLocked = false;
  private String id;
  private String text;
  private String seriesId;
  private String seriesTitle;
  private String creator;
  private String contributor;
  private String language;
  private String license;
  private String title;
  private String query;
  private int limit = -1;
  private int offset = -1;
  private String[] tags = null;
  private MediaPackageElementFlavor[] flavors = null;
  private Date deletedDate = null;
  private Sort sort = Sort.DATE_CREATED;
  private boolean sortAscending = true;

  public enum Sort {
    DATE_CREATED, TITLE, CREATOR, LANGUAGE, LICENSE, SUBJECT, MEDIA_PACKAGE_ID
  }

  public EpisodeQuery includeLocked(boolean includeLocked) {
    this.includeLocked = includeLocked;
    return this;
  }

  public EpisodeQuery withId(String id) {
    this.id = id;
    return this;
  }

  public EpisodeQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public EpisodeQuery withOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public EpisodeQuery withQuery(String q) {
    this.query = q;
    return this;
  }

  public EpisodeQuery withText(String text) {
    this.text = text;
    return this;
  }

  public EpisodeQuery withSeriesId(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  public EpisodeQuery withSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
    return this;
  }

  public EpisodeQuery withCreator(String creator) {
    this.creator = creator;
    return this;
  }

  public EpisodeQuery withContributor(String contributor) {
    this.contributor = contributor;
    return this;
  }

  public EpisodeQuery withLanguage(String language) {
    this.language = language;
    return this;
  }

  public EpisodeQuery withLicense(String license) {
    this.license = license;
    return this;
  }

  public EpisodeQuery withTitle(String title) {
    this.title = title;
    return this;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public String getSeriesTitle() {
    return seriesTitle;
  }

  public String getCreator() {
    return creator;
  }

  public String getContributor() {
    return contributor;
  }

  public String getLanguage() {
    return language;
  }

  public String getLicense() {
    return license;
  }

  public String getTitle() {
    return title;
  }

  public String getId() {
    return id;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }

  public String getQuery() {
    return query;
  }

  public String getText() {
    return text;
  }

  /**
   * Sort the results by the specified field, either ascending or descending.
   *
   * @param sort the sort field
   * @param ascending whether to sort ascending (true) or descending (false)
   */
  public EpisodeQuery withSort(Sort sort, boolean ascending) {
    this.sort = sort;
    this.sortAscending = ascending;
    return this;
  }

  /**
   * Return the field to use in sorting the results of the query.
   *
   * @return the sort field
   */
  public Sort getSort() {
    return sort;
  }

  /**
   * Return whether to sort the results in ascending order.
   *
   * @return whether the search results should be sorted in ascending order
   */
  public boolean isSortAscending() {
    return sortAscending;
  }

  public MediaPackageElementFlavor[] getElementFlavors() {
    return flavors;
  }

  public String[] getElementTags() {
    return tags;
  }

  public EpisodeQuery withElementFlavors(MediaPackageElementFlavor[] flavors) {
    this.flavors = flavors;
    return this;
  }

  public EpisodeQuery withElementTags(String[] tags) {
    this.tags = tags;
    return this;
  }

  public EpisodeQuery withDeletedSince(Date date) {
    this.deletedDate = date;
    return this;
  }

  public Date getDeletedDate() {
    return deletedDate;
  }
}
