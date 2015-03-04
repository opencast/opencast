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

import org.opencastproject.matterhorn.search.SearchResultItem;

/**
 * Default implementation of a {@link SearchResultItem}.
 */
public class SearchResultItemImpl<T> implements SearchResultItem<T> {

  /** Source of the search result */
  protected T source = null;

  /** Score within the search result */
  protected double score = 0.0d;

  /**
   * Creates a new search result item with the given uri. The <code>source</code> is the object that created the item,
   * usually, this will be the site itself but it could very well be a module that added to a search result.
   * 
   * @param relevance
   *          the score inside the search result
   * @param source
   *          the object that produced the result item
   */
  public SearchResultItemImpl(double relevance, T source) {
    this.source = source;
    this.score = relevance;
  }

  /**
   * Sets the relevance value, representing the score in the search result.
   * 
   * @param relevance
   *          the relevance value
   */
  public void setRelevance(double relevance) {
    this.score = relevance;
  }

  /**
   * @see org.opencastproject.matterhorn.search.SearchResultItem#getRelevance()
   */
  public double getRelevance() {
    return score;
  }

  /**
   * Returns the search result's source.
   * 
   * @see org.opencastproject.matterhorn.search.SearchResultItem#getSource()
   */
  public T getSource() {
    return source;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(SearchResultItem<T> sr) {
    if (score < sr.getRelevance())
      return 1;
    else if (score > sr.getRelevance())
      return -1;
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Double.toString(score).hashCode();
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return source.toString();
  }

}
