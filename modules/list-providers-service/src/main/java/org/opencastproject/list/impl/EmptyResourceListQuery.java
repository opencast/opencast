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

package org.opencastproject.list.impl;

import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.util.data.Option;

import java.util.Collections;
import java.util.List;

/**
 * ResourceListQuery that should result in an empty list after execution.
 */
public class EmptyResourceListQuery implements ResourceListQuery {
  @Override
  public List<ResourceListFilter<?>> getFilters() {
    return Collections.emptyList();
  }

  @Override
  public List<ResourceListFilter<?>> getAvailableFilters() {
    return Collections.emptyList();
  }

  @Override
  public ResourceListFilter<?> getFilter(String name) {
    return null;
  }

  @Override
  public Option<Integer> getLimit() {
    return Option.some(0);
  }

  @Override
  public Option<Integer> getOffset() {
    return Option.none();
  }

  @Override
  public Option<String> getSortBy() {
    return Option.none();
  }

  @Override
  public Boolean hasFilter(String name) {
    return false;
  }
}
