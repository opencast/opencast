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
import org.opencastproject.index.service.resources.list.provider.AgentsListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.util.data.Option;

/**
 * Query for the capture-agents list.
 *
 * The following filters can be used:
 * <ul>
 * <li>name</li>
 * <li>status</li>
 * </ul>
 */
public class AgentsListQuery extends ResourceListQueryImpl {

  public static final String FILTER_NAME_NAME = "Name";
  private static final String FILTER_NAME_LABEL = "FILTERS.AGENTS.NAME.LABEL";

  public static final String FILTER_STATUS_NAME = "Status";
  private static final String FILTER_STATUS_LABEL = "FILTERS.AGENTS.STATUS.LABEL";

  public static final String FILTER_LAST_UPDATED = "LastUpdated";

  public AgentsListQuery() {
    super();
    this.availableFilters.add(createNameFilter(Option.<String> none()));
    this.availableFilters.add(createStatusFilter(Option.<String> none()));
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
   * Add a {@link ResourceListFilter} filter to the query with the given status
   *
   * @param name
   *          the status to filter for
   */
  public void withStatus(String status) {
    this.addFilter(createStatusFilter(Option.option(status)));
  }

  /**
   * Returns an {@link Option} containing the status used to filter if set
   *
   * @return an {@link Option} containing the status or none.
   */
  public Option<String> getStatus() {
    return this.getFilterValue(FILTER_STATUS_NAME);
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
            Option.some(AgentsListProvider.NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a state
   *
   * @param status
   *          the status to filter on wrapped in an {@link Option} or {@link Option#none()}
   * @return a new {@link ResourceListFilter} for a status based query
   */
  public static ResourceListFilter<String> createStatusFilter(Option<String> status) {
    return FiltersUtils.generateFilter(status, FILTER_STATUS_NAME, FILTER_STATUS_LABEL, SourceType.SELECT,
            Option.some(AgentsListProvider.STATUS));
  }
}
