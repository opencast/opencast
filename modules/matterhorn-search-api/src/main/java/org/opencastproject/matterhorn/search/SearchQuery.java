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

import java.util.Collection;
import java.util.Map;

/**
 * Interface for the implementation of search queries.
 */
public interface SearchQuery {

  /**
   * Sort order definitions.
   */
  public enum Order {
    None, Ascending, Descending
  }

  /**
   * Return documents of the given types.
   *
   * @param types
   *          the resource types to look up
   * @return the query extended by this criterion
   */
  SearchQuery withTypes(String... types);

  /**
   * Returns the document types or or an empty array if no types were specified.
   *
   * @return the type
   */
  String[] getTypes();

  /**
   * Sets the number of results that are returned.
   *
   * @param limit
   *          the number of results
   * @return the search query
   */
  SearchQuery withLimit(int limit);

  /**
   * Returns the number of results that are returned, starting at the offset returned by <code>getOffset()</code>. If no
   * limit was specified, this method returns <code>-1</code>.
   *
   * @return the maximum number of results
   */
  int getLimit();

  /**
   * Sets the starting offset. Search results will be returned starting at that offset and until the limit is reached,
   * as specified by <code>getLimit()</code>.
   *
   * @param offset
   *          the starting offset
   * @return the search query
   * @see
   */
  SearchQuery withOffset(int offset);

  /**
   * Returns the starting offset within the search result or <code>0</code> if no offset was specified.
   *
   * @return the offset
   */
  int getOffset();

  /**
   * Returns documents that contain the given text.
   *
   * @param text
   *          the text to look up
   * @return the query extended by this criterion
   */
  SearchQuery withText(String text);

  /**
   * Returns documents that contain the given text.
   *
   * @param wildcardSearch
   *          <code>True</code> to perform a (much slower) wildcard search
   * @param text
   *          the text to look up
   *
   * @return the query extended by this criterion
   */
  SearchQuery withText(boolean wildcardSearch, String text);

  /**
   * Returns documents that contain the given text.
   * <p>
   * Depending on the quantifier, either documents are returned that contain at least one of the terms are only
   * documents containing all of the terms.
   *
   * @param text
   *          the text to look up
   * @param quantifier
   *          whether all or some of the terms need to be matched
   * @param fuzzy
   *          <code>true</code> to perform a fuzzy search
   * @return the query extended by this criterion
   */
  SearchQuery withText(boolean fuzzy, SearchTerms.Quantifier quantifier, String... text);

  /**
   * Returns the search terms or an empty collection if no terms were specified.
   *
   * @return the terms
   */
  Collection<SearchTerms<String>> getTerms();

  /**
   * Returns the search text or <code>null</code> if no text was specified.
   *
   * @return the text
   */
  String getQueryString();

  /**
   * Returns <code>true</code> if the current search operation should be performed using fuzzy searching.
   *
   * @return <code>true</code> if fuzzy search should be used
   */
  boolean isFuzzySearch();

  /**
   * Returns documents that match the search query <i>and</i> the text filter.
   *
   * @param filter
   *          the filter text
   * @return the search query
   */
  SearchQuery withFilter(String filter);

  /**
   * Returns the filter expression.
   *
   * @return the filter
   */
  String getFilter();

  /**
   * Returns the fields that should be returned by the query. If all fields should be returned, the method will return
   * an empty array.
   *
   * @return the names of the fields to return
   */
  String[] getFields();

  /**
   * Adds a field that needs to be returned by the query. If no fields are being set, all fields will be returned.
   *
   * @param field
   *          the field name
   * @return the query
   */
  SearchQuery withField(String field);

  /**
   * Adds the fields that need to be returned by the query. If no fields are being set, all fields will be returned.
   *
   * @param fields
   *          the field names
   * @return the query
   */
  SearchQuery withFields(String... fields);

  /**
   * Sort the result set by the field and the given order. The insertion-order is kept.
   * 
   * @param field
   *          the field name, must not be {@code null}
   * @param order
   *          the order direction, must not be {@code null}
   * @return the updated search query
   */
  SearchQuery withSortOrder(String field, Order order);

  /**
   * Returns all the known sort orders. The insertion-order is kept.
   * 
   * @return a map with all known sort orders
   */
  Map<String, Order> getSortOrders();

  /**
   * Returns the sort order of a field. If no sort order has been set for the given field, {@link Order#None} is
   * returned.
   * 
   * @param field
   *          the field name, must not be {@code null}
   * @return the sort order
   */
  Order getSortOrder(String field);

}
