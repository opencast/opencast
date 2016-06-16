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
  protected String id;
  protected String seriesId;
  protected String text;
  protected String query;
  protected int limit = 0;
  protected int offset = 0;
  protected String[] tags = null;
  protected MediaPackageElementFlavor[] flavors = null;
  protected Date deletedDate = null;
  protected Sort sort = Sort.DATE_CREATED;
  protected boolean sortAscending = true;

  public enum Sort {
    DATE_CREATED, DATE_PUBLISHED, TITLE, SERIES_ID, MEDIA_PACKAGE_ID, CREATOR, CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT, DESCRIPTION, PUBLISHER
  }

  public SearchQuery includeEpisodes(boolean includeEpisode) {
    this.includeEpisode = includeEpisode;
    return this;
  }

  public SearchQuery includeSeries(boolean includeSeries) {
    this.includeSeries = includeSeries;
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

  public boolean isIncludeEpisodes() {
    return includeEpisode;
  }

  public boolean isIncludeSeries() {
    return includeSeries;
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
  public boolean isSortAscending() {
    return sortAscending;
  }
}
