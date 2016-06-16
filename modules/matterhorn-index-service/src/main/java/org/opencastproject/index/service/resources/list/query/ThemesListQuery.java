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

package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.provider.UsersListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.util.data.Option;

/**
 * Query for the themes list.
 *
 * The following filters can be used:
 * <ul>
 * <li>creator</li>
 * </ul>
 */
public class ThemesListQuery extends ResourceListQueryImpl {

  public static final String FILTER_CREATOR_NAME = "Creator";
  private static final String FILTER_CREATOR_LABEL = "FILTERS.THEMES.CREATOR.LABEL";

  public static final String FILTER_TEXT_NAME = "textFilter";

  public ThemesListQuery() {
    super();
    this.availableFilters.add(createCreatorFilter(Option.<String> none()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given creator
   *
   * @param creator
   *          the creator to filter for
   */
  public void withCreator(String creator) {
    this.addFilter(createCreatorFilter(Option.option(creator)));
  }

  /**
   * Returns an {@link Option} containing the creator used to filter if set
   *
   * @return an {@link Option} containing the creator or none.
   */
  public Option<String> getCreator() {
    return getFilterValue(FILTER_CREATOR_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on a creator
   *
   * @param creator
   *          the creator to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a creator based query
   */
  public static ResourceListFilter<String> createCreatorFilter(Option<String> creator) {
    return FiltersUtils.generateFilter(creator, FILTER_CREATOR_NAME, FILTER_CREATOR_LABEL, SourceType.SELECT,
            Option.some(UsersListProvider.NAME));
  }

}
