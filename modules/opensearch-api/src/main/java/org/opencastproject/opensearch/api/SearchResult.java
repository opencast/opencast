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

package org.opencastproject.opensearch.api;

import java.util.Collections;
import java.util.List;

/**
 * One page of results as returned by a search operation.
 *
 * @param <T>
 *          the type of the returned documents
 */
public final class SearchResult<T> {

  private final List<T> items;
  private final long hitCount;
  private final long offset;
  private final long limit;

  /**
   * Creates a search result.
   *
   * @param items
   *          the documents on this page
   * @param hitCount
   *          the total number of documents matching the query, independent of offset and limit
   * @param offset
   *          the offset of the query
   * @param limit
   *          the limit of the query
   */
  public SearchResult(List<T> items, long hitCount, long offset, long limit) {
    this.items = Collections.unmodifiableList(items);
    this.hitCount = hitCount;
    this.offset = offset;
    this.limit = limit;
  }

  /**
   * Creates an empty search result.
   */
  public static <T> SearchResult<T> empty() {
    return new SearchResult<>(List.of(), 0, 0, 0);
  }

  /**
   * Returns the documents on this page of the search result.
   *
   * @return the documents
   */
  public List<T> getItems() {
    return items;
  }

  /**
   * Returns the total number of documents matching the query, independent of offset and limit.
   *
   * @return the number of hits
   */
  public long getHitCount() {
    return hitCount;
  }

  /**
   * Returns the number of documents on this page.
   *
   * @return the page size
   */
  public long getPageSize() {
    return items.size();
  }

  /**
   * Returns the offset of the query.
   *
   * @return the offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Returns the limit of the query.
   *
   * @return the limit
   */
  public long getLimit() {
    return limit;
  }

}
