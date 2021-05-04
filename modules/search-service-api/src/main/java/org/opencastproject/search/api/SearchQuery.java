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

package org.opencastproject.search.api;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.Date;

/**
 * Represents a query to find search results
 */
public class SearchQuery {
  protected boolean includeEpisode = true;
  protected boolean includeSeries = false;
  protected boolean includeDeleted = false;
  protected String id;
  protected String seriesId;
  protected String text;
  protected String query;
  protected int limit = 0;
  protected int offset = 0;
  protected String[] tags = null;
  protected MediaPackageElementFlavor[] flavors = null;
  protected Date deletedDate = null;
  protected Date updatedSince = null;
  protected Sort sort = Sort.DATE_CREATED;
  protected boolean sortAscending = true;
  protected boolean signURL = false;

  public enum Sort {
    DATE_CREATED,
    DATE_MODIFIED,
    TITLE,
    SERIES_ID,
    MEDIA_PACKAGE_ID,
    CREATOR,
    CONTRIBUTOR,
    LANGUAGE,
    LICENSE,
    SUBJECT,
    DESCRIPTION,
    PUBLISHER
  }

  public SearchQuery signURLs(final boolean sign) {
    this.signURL = sign;
    return this;
  }

  public SearchQuery includeEpisodes(boolean includeEpisode) {
    this.includeEpisode = includeEpisode;
    return this;
  }

  public SearchQuery includeSeries(boolean includeSeries) {
    this.includeSeries = includeSeries;
    return this;
  }

  /**
   * Specifies whether deleted events should be included in the results.
   *
   * Deleted events are automatically included if <code>deleteDate</code> is set. Otherwise,
   * this defaults to <code>false</code>.
   */
  public SearchQuery includeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public SearchQuery withId(String id) {
    this.id = id;
    return this;
  }

  public SearchQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public SearchQuery withOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public SearchQuery withQuery(String q) {
    this.query = q;
    return this;
  }

  public SearchQuery withText(String text) {
    this.text = text;
    return this;
  }

  public SearchQuery withSeriesId(String seriesId) {
    this.seriesId = seriesId;
    return this;
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

  public String getSeriesId() {
    return seriesId;
  }

  public boolean willSignURLs() {
    return signURL;
  }

  public boolean willIncludeEpisodes() {
    return includeEpisode;
  }

  public boolean willIncludeSeries() {
    return includeSeries;
  }

  /**
   * Returns true if <code>includeDeleted</code> was set to `true` or <code>withDeletedDate</code>
   * was set. In those cases, the results of the query will include deleted elements.
   */
  public boolean willIncludeDeleted() {
    return includeDeleted || this.deletedDate != null;
  }

  public MediaPackageElementFlavor[] getElementFlavors() {
    return flavors;
  }

  public String[] getElementTags() {
    return tags;
  }

  public SearchQuery withElementFlavors(MediaPackageElementFlavor[] flavors) {
    this.flavors = flavors;
    return this;
  }

  public SearchQuery withElementTags(String[] tags) {
    this.tags = tags;
    return this;
  }

  public SearchQuery withDeletedSince(Date date) {
    this.deletedDate = date;
    return this;
  }

  public Date getDeletedDate() {
    return deletedDate;
  }

  /**
   * Adds a filter to only retrieve results that have been published or deleted since
   * the given date.
   */
  public SearchQuery withUpdatedSince(Date date) {
    this.updatedSince = date;
    return this;
  }

  public Date getUpdatedSince() {
    return updatedSince;
  }

   /**
   * Sort the results by the specified field in ascending order.
   *
   * @param sort
   *          the sort field
   */
  public SearchQuery withSort(Sort sort) {
    return withSort(sort, true);
  }

  /**
   * Sort the results by the specified field, either ascending or descending.
   *
   * @param sort
   *          the sort field
   * @param ascending
   *          whether to sort ascending (true) or descending (false)
   */
  public SearchQuery withSort(Sort sort, boolean ascending) {
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
  public boolean willSortAscending() {
    return sortAscending;
  }
}
