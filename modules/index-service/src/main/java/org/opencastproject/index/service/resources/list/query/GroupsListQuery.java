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

import org.opencastproject.index.service.resources.list.provider.GroupsListProvider;
import org.opencastproject.index.service.util.FiltersUtils;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListFilter.SourceType;
import org.opencastproject.list.impl.ResourceListQueryImpl;

import java.util.Optional;

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
  public static final String FILTER_ROLE_NAME = "Role";
  private static final String FILTER_NAME_LABEL = "FILTERS.USERS.NAME.LABEL";
  private static final String FILTER_ROLE_LABEL = "FILTERS.USERS.ROLE.LABEL";

  public static final String FILTER_TEXT_NAME = "textFilter";

  public GroupsListQuery() {
    super();
    this.availableFilters.add(createRoleFilter(Optional.empty()));
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
   * Add a {@link ResourceListFilter} filter to the query with the given role
   *
   * @param role
   *          the role to filter for
   */
  public void withRole(String role) {
    this.addFilter(createRoleFilter(Optional.ofNullable(role)));
  }

  /**
   * Returns an {@link Optional} containing the role used to filter if set
   *
   * @return an {@link Optional} containing the role or none.
   */
  public Optional<String> getRole() {
    return this.getFilterValue(FILTER_ROLE_NAME);
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
            Optional.of(GroupsListProvider.NAME));
  }

  public static ResourceListFilter<String> createRoleFilter(Optional<String> role) {
    return FiltersUtils.generateFilter(role, FILTER_ROLE_NAME, FILTER_ROLE_LABEL, SourceType.SELECT,
        Optional.of(GroupsListProvider.ROLE_ONLY));
  }

}
