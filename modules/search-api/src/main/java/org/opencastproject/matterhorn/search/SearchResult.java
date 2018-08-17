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


package org.opencastproject.matterhorn.search;

/**
 * A result as returned by a search operation.
 */
public interface SearchResult<T> {

  /**
   * Returns the individual items of this search result.
   * 
   * @return the search result items
   */
  SearchResultItem<T>[] getItems();

  /**
   * Returns the total number of appearances of the search criteria in the search result, spread over
   * <code>getDocumentCount</code> number of documents.
   * 
   * @return the overall number of hits
   */
  long getHitCount();

  /**
   * Returns the total number of items in the search result.
   * <p>
   * Note that this number might not match the size of the array as returned by {@link #getItems()}, which is likely to
   * be limited by the value returned by {@link #getLimit()}.
   * 
   * @return the number of documents containing the hits
   */
  long getDocumentCount();

  /**
   * Returns the number of items in this search result, possibly limited with respect to the total number of result
   * items by <code>offset</code> and <code>limit</code>.
   * 
   * @return the total number of hits.
   * @see #getOffset()
   * @see #getLimit()
   */
  long getPageSize();

  /**
   * Get the offset within the search result or <code>-1</code> if no limit has been specified.
   * 
   * @return the offset
   */
  long getOffset();

  /**
   * Returns the limit of this search results or <code>-1</code> if no limit has been specified.
   * 
   * @return the limit
   */
  long getLimit();

  /**
   * Returns the page of the current result items within the complete search result. This number is influenced by the
   * <code>offset</code> and the page size <code>limit</code>.
   * <p>
   * Note that the page size is one-based
   * 
   * @return the page number
   */
  long getPage();

  /**
   * Returns the search time in milliseconds.
   * 
   * @return the time
   */
  long getSearchTime();

}
