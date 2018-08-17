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
package org.opencastproject.oaipmh.persistence.impl;

import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;

import java.util.List;

public class SearchResultImpl implements SearchResult {

  private final long offset;
  private final long limit;
  private final List<SearchResultItem> items;

  public SearchResultImpl(long offset, long limit, List<SearchResultItem> items) {
    this.offset = offset;
    this.limit = limit;
    this.items = items;
  }

  @Override
  public List<SearchResultItem> getItems() {
    return items;
  }

  @Override
  public long size() {
    return items.size();
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public long getLimit() {
    return limit;
  }

}
