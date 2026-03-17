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

package org.opencastproject.list.common.query;

import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListFilter.SourceType;
import org.opencastproject.list.util.FiltersUtils;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.Optional;

/**
 * Query for the playlists list.
 *
 * The following filters can be used:
 * <ul>
 * <li>textFilter (free text search across title, description, creator)</li>
 * <li>creator</li>
 * <li>updated (date period)</li>
 * </ul>
 */
public class PlaylistsListQuery extends DefaultResourceListQuery {

  public static final String FILTER_TEXT_NAME = "textFilter";

  public static final String FILTER_CREATOR_NAME = "creator";
  private static final String FILTER_CREATOR_LABEL = "FILTERS.PLAYLISTS.CREATOR.LABEL";
  private static final String FILTER_CREATOR_LIST_PROVIDER = "PLAYLISTS.CREATORS";

  public static final String FILTER_UPDATED_NAME = "Updated";
  private static final String FILTER_UPDATED_LABEL = "FILTERS.PLAYLISTS.UPDATED.LABEL";

  public PlaylistsListQuery() {
    super();
    this.availableFilters.add(createCreatorFilter(Optional.empty()));
    this.availableFilters.add(createUpdatedFilter(Optional.empty()));
  }

  public void withTextFilter(String text) {
    this.addFilter(createTextFilter(Optional.ofNullable(text)));
  }

  public Optional<String> getTextFilter() {
    return this.getFilterValue(FILTER_TEXT_NAME);
  }

  public void withCreator(String creator) {
    this.addFilter(createCreatorFilter(Optional.ofNullable(creator)));
  }

  public Optional<String> getCreator() {
    return this.getFilterValue(FILTER_CREATOR_NAME);
  }


  public void withUpdated(Tuple<Date, Date> period) {
    this.addFilter(createUpdatedFilter(Optional.ofNullable(period)));
  }


  public Optional<Tuple<Date, Date>> getUpdated() {
    return this.getFilterValue(FILTER_UPDATED_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on a free text
   *
   * @param value
   *          the free text to filter with or none
   * @return a new {@link ResourceListFilter} for a free text
   */
  public static ResourceListFilter<String> createTextFilter(Optional<String> value) {
    return FiltersUtils.generateFilter(value, FILTER_TEXT_NAME, "", SourceType.FREETEXT, Optional.empty());
  }

  /**
   * Create a new {@link ResourceListFilter} based on a creator
   *
   * @param creator
   *          the creator to filter with or none
   * @return a new {@link ResourceListFilter} for a creator
   */
  public static ResourceListFilter<String> createCreatorFilter(Optional<String> creator) {
    return FiltersUtils.generateFilter(creator, FILTER_CREATOR_NAME, FILTER_CREATOR_LABEL, SourceType.SELECT,
            Optional.of(FILTER_CREATOR_LIST_PROVIDER));
  }

  /**
   * Create a new {@link ResourceListFilter} based on an updated date period
   *
   * @param period
   *          the updated date period as {@link Tuple} with two {@link Date} or none
   * @return a new {@link ResourceListFilter} for an updated date period
   */
  public static ResourceListFilter<Tuple<Date, Date>> createUpdatedFilter(Optional<Tuple<Date, Date>> period) {
    return FiltersUtils.generateFilter(period, FILTER_UPDATED_NAME, FILTER_UPDATED_LABEL, SourceType.PERIOD,
            Optional.empty());
  }

}
