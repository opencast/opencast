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

package org.opencastproject.matterhorn.search.impl;

import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for a search result.
 */
public class SearchResultImpl<T> implements SearchResult<T> {

  /** The query that led to this search result */
  protected SearchQuery query = null;

  /** The search offset */
  protected long offset = 0;

  /** The search limit */
  protected long limit = 0;

  /** The total number of appearances of the search criteria */
  protected long hitCount = 0;

  /** The total size of the search result set */
  protected long documentCount = 0;

  /** The time it took to do the search in ms */
  protected long time = 0;

  /** Flag to indicate the sorted state */
  protected boolean sorted = true;

  /** The search result */
  protected List<SearchResultItem<T>> result = null;

  /**
   * Creates a search result that was created using the given query. Note that <code>hits</code> indicates the overall
   * number of appearances of the search term, while size is equal to the number of documents that contain those
   * <code>hits</code> hits.
   * 
   * @param query
   *          the query
   * @param hitCount
   *          the number of hits
   * @param documentCount
   *          the total size of the result set
   */
  public SearchResultImpl(SearchQuery query, long hitCount, long documentCount) {
    this.query = query;
    this.offset = query.getOffset();
    this.limit = query.getLimit();
    this.hitCount = hitCount;
    this.documentCount = documentCount;
  }

  /**
   * Adds the given search result item to the result set.
   * 
   * @param item
   *          the result item
   */
  public void addResultItem(SearchResultItem<T> item) {
    if (result == null)
      result = new ArrayList<SearchResultItem<T>>();
    result.add(item);
    sorted = false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getItems()
   */
  @SuppressWarnings("unchecked")
  public SearchResultItem<T>[] getItems() {
    if (result == null)
      return new SearchResultItem[] {};
    if (!sorted) {
      Collections.sort(result);
      sorted = true;
    }
    return result.toArray(new SearchResultItem[result.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getLimit()
   */
  public long getLimit() {
    return limit;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getOffset()
   */
  public long getOffset() {
    return offset;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getPage()
   */
  public long getPage() {
    if (offset == 0 || limit == 0)
      return 1;
    return (long) Math.floor(offset / limit) + 1;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getPageSize()
   */
  public long getPageSize() {
    return result != null ? result.size() : 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getQuery()
   */
  public SearchQuery getQuery() {
    return query;
  }

  /**
   * Sets the search time in milliseconds.
   * 
   * @param time
   *          the time
   */
  public void setSearchTime(long time) {
    this.time = time;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getSearchTime()
   */
  public long getSearchTime() {
    return time;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getHitCount()
   */
  public long getHitCount() {
    return hitCount;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchResult#getDocumentCount()
   */
  public long getDocumentCount() {
    return result != null ? result.size() : documentCount;
  }

  /**
   * Sets the document count.
   * 
   * @param count
   *          the number of documents in this search result
   */
  public void setDocumentCount(long count) {
    this.documentCount = count;
  }

}
