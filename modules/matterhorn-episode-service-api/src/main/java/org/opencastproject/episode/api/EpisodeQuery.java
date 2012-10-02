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

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;

import java.util.Date;
import java.util.List;

/**
 * Represents a query to find search results. Note that none of the methods takes null as argument.
 */
public final class EpisodeQuery {
  private Option<String> id = none();
  private Option<String> text = none();
  private Option<String> seriesId = none();
  private Option<String> seriesTitle = none();
  private Option<String> creator = none();
  private Option<String> contributor = none();
  private Option<String> language = none();
  private Option<String> license = none();
  private Option<String> title = none();
  private Option<String> query = none();
  private Option<Integer> limit = some(-1);
  private Option<Integer> offset = some(-1);
  private Option<String> organization = none();
  private List<String> tags = nil();
  private List<MediaPackageElementFlavor> flavors = nil();
  private Option<Date> addedAfter = none();
  private Option<Date> addedBefore = none();
  private Option<Date> deletedDate = none();
  private Sort sort = Sort.DATE_CREATED;
  private boolean sortAscending = true;
  private boolean onlyLastVersion = false;
  private boolean includeDeleted = false;

  public enum Sort {
    DATE_CREATED, TITLE, CREATOR, LANGUAGE, LICENSE, SUBJECT, MEDIA_PACKAGE_ID
  }

  private EpisodeQuery() {
  }

  /** Create a new user query. This query has the organization set automatically. */
  public static EpisodeQuery query(SecurityService sec) {
    return new EpisodeQuery().organization(sec.getOrganization().toString());
  }

  /** Create a new system query with no restrictions. */
  public static EpisodeQuery systemQuery() {
    return new EpisodeQuery();
  }

  public EpisodeQuery id(String id) {
    this.id = some(id);
    return this;
  }

  public EpisodeQuery limit(int limit) {
    this.limit = some(limit);
    return this;
  }

  public EpisodeQuery offset(int offset) {
    this.offset = some(offset);
    return this;
  }

  public EpisodeQuery query(String q) {
    this.query = some(q);
    return this;
  }

  public EpisodeQuery text(String text) {
    this.text = some(text);
    return this;
  }

  public EpisodeQuery organization(String organization) {
    this.organization = some(organization);
    return this;
  }

  public Option<String> getOrganization() {
    return organization;
  }

  public EpisodeQuery seriesId(String seriesId) {
    this.seriesId = some(seriesId);
    return this;
  }

  public EpisodeQuery seriesTitle(String seriesTitle) {
    this.seriesTitle = some(seriesTitle);
    return this;
  }

  public EpisodeQuery creator(String creator) {
    this.creator = some(creator);
    return this;
  }

  public EpisodeQuery contributor(String contributor) {
    this.contributor = some(contributor);
    return this;
  }

  public EpisodeQuery language(String language) {
    this.language = some(language);
    return this;
  }

  public EpisodeQuery license(String license) {
    this.license = some(license);
    return this;
  }

  public EpisodeQuery title(String title) {
    this.title = some(title);
    return this;
  }

  /** Find episodes added after or on the given date. */
  public EpisodeQuery addedAfter(Date date) {
    this.addedAfter = some(date);
    return this;
  }

  public Option<Date> getAddedAfter() {
    return addedAfter;
  }

  /** Find episodes added before or on the given date. */
  public EpisodeQuery addedBefore(Date date) {
    this.addedBefore = some(date);
    return this;
  }

  public Option<Date> getAddedBefore() {
    return addedBefore;
  }

  public Option<String> getSeriesId() {
    return seriesId;
  }

  public Option<String> getSeriesTitle() {
    return seriesTitle;
  }

  public Option<String> getCreator() {
    return creator;
  }

  public Option<String> getContributor() {
    return contributor;
  }

  public Option<String> getLanguage() {
    return language;
  }

  public Option<String> getLicense() {
    return license;
  }

  public Option<String> getTitle() {
    return title;
  }

  public Option<String> getId() {
    return id;
  }

  public Option<Integer> getLimit() {
    return limit;
  }

  public Option<Integer> getOffset() {
    return offset;
  }

  public Option<String> getQuery() {
    return query;
  }

  public Option<String> getText() {
    return text;
  }

  /**
   * Sort the results by the specified field, either ascending or descending.
   * 
   * @param sort
   *          the sort field
   * @param ascending
   *          whether to sort ascending (true) or descending (false)
   */
  public EpisodeQuery sort(Sort sort, boolean ascending) {
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

  public EpisodeQuery onlyLastVersion() {
    this.onlyLastVersion = true;
    return this;
  }

  /**
   * Return whether to sort the results in ascending order.
   * 
   * @return whether the search results should be sorted in ascending order
   */
  public boolean getSortAscending() {
    return sortAscending;
  }

  public boolean getOnlyLastVersion() {
    return onlyLastVersion;
  }

  public List<MediaPackageElementFlavor> getElementFlavors() {
    return flavors;
  }

  public EpisodeQuery elementFlavors(List<MediaPackageElementFlavor> flavors) {
    this.flavors = flavors;
    return this;
  }

  public EpisodeQuery elementFlavors(MediaPackageElementFlavor... flavors) {
    this.flavors = list(flavors);
    return this;
  }

  public EpisodeQuery elementTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public EpisodeQuery elementTags(String... tags) {
    this.tags = list(tags);
    return this;
  }

  public List<String> getElementTags() {
    return tags;
  }

  /** Only return items that have been deleted since the given date. Overrides {@link #includeDeleted(boolean)}. */
  public EpisodeQuery deletedSince(Date date) {
    this.deletedDate = some(date);
    return this;
  }

  public Option<Date> getDeletedDate() {
    return deletedDate;
  }

  /** Include deleted items in the response. {@link #deletedSince} always takes precedence. */
  public EpisodeQuery includeDeleted(boolean include) {
    this.includeDeleted = include;
    return this;
  }

  public boolean getIncludeDeleted() {
    return includeDeleted || deletedDate.isSome();
  }
}
