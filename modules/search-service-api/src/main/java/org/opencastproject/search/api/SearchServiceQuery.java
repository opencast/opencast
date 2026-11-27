/*
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

import static java.util.Objects.requireNonNull;

import org.opencastproject.util.requests.SortCriterion.Order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A query against the search service index. All criteria are combined with AND.
 */
public class SearchServiceQuery {

  /** Sort field for the modification date of an index entry */
  public static final String SORT_MODIFIED = SearchResult.MODIFIED_DATE;

  private final String organization;
  private final SearchService.IndexEntryType type;
  private boolean includeDeleted = false;
  private Date modifiedSince = null;
  private String id = null;
  private final List<String> seriesIds = new ArrayList<>();
  private String seriesTitle = null;
  private Boolean live = null;
  private List<String> readRoles = null;
  private String text = null;
  private final Map<String, Order> sortOrders = new LinkedHashMap<>();
  private int offset = -1;
  private int limit = -1;

  /**
   * Creates a query for index entries of the given type in the given organization. Deleted entries are excluded.
   *
   * @param organization
   *          the organization identifier
   * @param type
   *          the type of index entries
   */
  public SearchServiceQuery(String organization, SearchService.IndexEntryType type) {
    this.organization = requireNonNull(organization);
    this.type = requireNonNull(type);
  }

  /** Also return entries which have been marked as deleted. */
  public SearchServiceQuery withDeleted() {
    this.includeDeleted = true;
    return this;
  }

  /** Only return entries modified at or after the given date. */
  public SearchServiceQuery withModifiedSince(Date since) {
    this.modifiedSince = since;
    return this;
  }

  /** Only return the entry with the given identifier. */
  public SearchServiceQuery withId(String id) {
    this.id = id;
    return this;
  }

  /** Only return episodes belonging to one of the given series. */
  public SearchServiceQuery withSeriesIds(Collection<String> seriesIds) {
    this.seriesIds.addAll(seriesIds);
    return this;
  }

  /** Only return entries with exactly this title. */
  public SearchServiceQuery withSeriesTitle(String title) {
    this.seriesTitle = title;
    return this;
  }

  /** Only return live (true) or non-live (false) episodes. */
  public SearchServiceQuery withLive(boolean live) {
    this.live = live;
    return this;
  }

  /** Only return entries which at least one of the given roles may read. */
  public SearchServiceQuery withReadRoles(Collection<String> roles) {
    this.readRoles = new ArrayList<>(roles);
    return this;
  }

  /** Only return entries matching this free text query. */
  public SearchServiceQuery withText(String text) {
    this.text = text;
    return this;
  }

  /**
   * Sort by the given index field. Can be called multiple times, earlier calls take precedence. Results with equal
   * values are sorted by relevance.
   *
   * @param field
   *          the index field, e.g. {@link #SORT_MODIFIED} or {@code dc.title}
   * @param order
   *          the sort order
   */
  public SearchServiceQuery sortBy(String field, Order order) {
    this.sortOrders.put(requireNonNull(field), requireNonNull(order));
    return this;
  }

  /** Skip the first {@code offset} results. */
  public SearchServiceQuery withOffset(int offset) {
    this.offset = offset;
    return this;
  }

  /** Return at most {@code limit} results. */
  public SearchServiceQuery withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public SearchService.IndexEntryType getType() {
    return type;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public Optional<Date> getModifiedSince() {
    return Optional.ofNullable(modifiedSince);
  }

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public List<String> getSeriesIds() {
    return Collections.unmodifiableList(seriesIds);
  }

  public Optional<String> getSeriesTitle() {
    return Optional.ofNullable(seriesTitle);
  }

  public Optional<Boolean> getLive() {
    return Optional.ofNullable(live);
  }

  /** The roles of which at least one must be allowed to read an entry, or empty if there is no restriction. */
  public Optional<List<String>> getReadRoles() {
    return Optional.ofNullable(readRoles).map(Collections::unmodifiableList);
  }

  public Optional<String> getText() {
    return Optional.ofNullable(text);
  }

  public Map<String, Order> getSortOrders() {
    return Collections.unmodifiableMap(sortOrders);
  }

  /** The offset, or a negative value if not set. */
  public int getOffset() {
    return offset;
  }

  /** The limit, or a negative value if not set. */
  public int getLimit() {
    return limit;
  }
}
