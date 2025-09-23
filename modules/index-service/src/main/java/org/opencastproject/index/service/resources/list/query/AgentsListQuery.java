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

package org.opencastproject.index.service.resources.list.query;

import org.opencastproject.index.service.resources.list.provider.AgentsListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListFilter.SourceType;
import org.opencastproject.list.impl.ResourceListQueryImpl;

import java.util.Optional;

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

  public static final String FILTER_TEXT_NAME = "textFilter";

  public AgentsListQuery() {
    super();
    this.availableFilters.add(createStatusFilter(Optional.<String> empty()));
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given name
   *
   * @param name
   *          the name to filter for
   */
  public void withName(String name) {
    this.addFilter(createNameFilter(Optional.ofNullable(name)));
  }

  /**
   * Returns an {@link Optional} containing the name used to filter if set
   *
   * @return an {@link Optional} containing the name or none.
   */
  public Optional<String> getName() {
    return this.getFilterValue(FILTER_NAME_NAME);
  }

  /**
   * Add a {@link ResourceListFilter} filter to the query with the given status
   *
   * @param status
   *          the status to filter for
   */
  public void withStatus(String status) {
    this.addFilter(createStatusFilter(Optional.ofNullable(status)));
  }

  /**
   * Returns an {@link Optional} containing the status used to filter if set
   *
   * @return an {@link Optional} containing the status or none.
   */
  public Optional<String> getStatus() {
    return this.getFilterValue(FILTER_STATUS_NAME);
  }

  /**
   * Create a new {@link ResourceListFilter} based on a name
   *
   * @param name
   *          the name to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a name based query
   */
  public static ResourceListFilter<String> createNameFilter(Optional<String> name) {
    return FiltersUtils.generateFilter(name, FILTER_NAME_NAME, FILTER_NAME_LABEL, SourceType.SELECT,
            Optional.of(AgentsListProvider.NAME));
  }

  /**
   * Create a new {@link ResourceListFilter} based on a state
   *
   * @param status
   *          the status to filter on wrapped in an {@link Optional} or {@link Optional#empty()}
   * @return a new {@link ResourceListFilter} for a status based query
   */
  public static ResourceListFilter<String> createStatusFilter(Optional<String> status) {
    return FiltersUtils.generateFilter(status, FILTER_STATUS_NAME, FILTER_STATUS_LABEL, SourceType.SELECT,
            Optional.of(AgentsListProvider.STATUS));
  }
}
