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
package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter.SourceType;
import org.opencastproject.index.service.resources.list.provider.GroupsListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.util.data.Option;

/**
 * Query for the users list.
 *
 * The following filters can be used:
 * <ul>
 * <li>name</li>
 * <li>role</li>
 * <li>provider</li>
 * </ul>
 */
public class GroupsListQuery extends ResourceListQueryImpl {

  public static final String FILTER_NAME_NAME = "Name";
  private static final String FILTER_NAME_LABEL = "FILTERS.USERS.NAME.LABEL";

  public GroupsListQuery() {
    super();
    this.availableFilters.add(createNameFilter(Option.<String> none()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given name
   *
   * @param name
   *          the name to filter for
   */
  public void withName(String name) {
    this.addFilter(createNameFilter(Option.option(name)));
  }

  /**
   * Returns an {@link Option} containing the name used to filter if set
   *
   * @return an {@link Option} containing the name or none.
   */
  public Option<String> getName() {
    return this.getFilterValue(FILTER_NAME_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on a name
   *
   * @param name
   *          the name to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a name based query
   */
  public static ResourceListFilter<String> createNameFilter(Option<String> name) {
    return FiltersUtils.generateFilter(name, FILTER_NAME_NAME, FILTER_NAME_LABEL, SourceType.SELECT,
            Option.some(GroupsListProvider.NAME));
  }

}
