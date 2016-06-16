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

package org.opencastproject.index.service.resources.list.api;

import org.opencastproject.util.data.Option;

import java.util.List;

/**
 * Query for the resource list
 */
public interface ResourceListQuery {

  /**
   * Returns all the {@link ResourceListFilter} set for the query.
   * 
   * @return all the query filters
   */
  List<ResourceListFilter<?>> getFilters();

  /**
   * Returns all the available {@link ResourceListFilter} filters with this query. The objects returned in the list are
   * simple instance without any filter value set.
   * 
   * @return all the available filters
   */
  List<ResourceListFilter<?>> getAvailableFilters();

  /**
   * Returns the filter with the given name
   * 
   * @param <A>
   * 
   * @return the query filter or null if the filter does not exist
   */
  ResourceListFilter<?> getFilter(String name);

  /**
   * Returns the limit for the resource query
   * 
   * @return the list limit
   */
  Option<Integer> getLimit();

  /**
   * Returns the offset for the resource query
   * 
   * @return the list offset
   */
  Option<Integer> getOffset();

  /**
   * Returns the name of the field by which the list should be sorted
   * 
   * @return the name of the field to use to sort the list
   */
  Option<String> getSortBy();

  /**
   * Returns if the given filter is or not set
   * 
   * @param name
   *          the filter name
   * @return true if set
   */
  Boolean hasFilter(String name);

}
