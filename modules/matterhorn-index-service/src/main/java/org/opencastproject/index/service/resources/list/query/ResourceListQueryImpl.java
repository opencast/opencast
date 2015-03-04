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
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListQueryImpl implements ResourceListQuery {

  protected final List<ResourceListFilter<?>> availableFilters = new ArrayList<ResourceListFilter<?>>();
  private final Map<String, ResourceListFilter<?>> filters = new HashMap<String, ResourceListFilter<?>>();
  private Option<Integer> limit;
  private Option<Integer> offset;
  protected Option<String> sortBy;

  public ResourceListQueryImpl() {
    limit = Option.none();
    offset = Option.none();
    sortBy = Option.none();
  }

  public void addFilter(ResourceListFilter<?> filter) {
    this.filters.put(filter.getName(), filter);
  }

  public void removeFilter(ResourceListFilter<?> filter) {
    this.filters.remove(filter.getName());
  }

  public void setLimit(Integer limit) {
    this.limit = Option.<Integer> option(limit);
  }

  public void setOffset(Integer offset) {
    this.offset = Option.<Integer> option(offset);
  }

  @Override
  public List<ResourceListFilter<?>> getFilters() {
    return new ArrayList<ResourceListFilter<?>>(filters.values());
  }

  @Override
  public ResourceListFilter<?> getFilter(String name) {
    return filters.get(name);
  }

  @Override
  public Option<Integer> getLimit() {
    return limit;
  }

  @Override
  public Option<Integer> getOffset() {
    return offset;
  }

  @Override
  public Option<String> getSortBy() {
    return sortBy;
  }

  @Override
  public Boolean hasFilter(String name) {
    return filters.containsKey(name);
  }

  @Override
  public List<ResourceListFilter<?>> getAvailableFilters() {
    return availableFilters;
  }

  /**
   * Returns the filter value wrapped in an {@link Option} or none if the filter is not existing or has no value.
   *
   * @param name
   *          the filter name
   * @return an {@link Option} wrapping the value or none.
   */
  public <A> Option<A> getFilterValue(String name) {
    if (this.hasFilter(name)) {
      return (Option<A>) this.getFilter(name).getValue();
    }

    return Option.<A> none();
  }

}
