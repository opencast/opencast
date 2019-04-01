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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.matterhorn.search

/**
 * Interface for the implementation of search queries.
 */
interface SearchQuery {

    /**
     * Returns the document types or or an empty array if no types were specified.
     *
     * @return the type
     */
    val types: Array<String>

    /**
     * Returns the number of results that are returned, starting at the offset returned by `getOffset()`. If no
     * limit was specified, this method returns `-1`.
     *
     * @return the maximum number of results
     */
    val limit: Int

    /**
     * Returns the starting offset within the search result or `0` if no offset was specified.
     *
     * @return the offset
     */
    val offset: Int

    /**
     * Returns the search terms or an empty collection if no terms were specified.
     *
     * @return the terms
     */
    val terms: Collection<SearchTerms<String>>

    /**
     * Returns the search text or `null` if no text was specified.
     *
     * @return the text
     */
    val queryString: String

    /**
     * Returns `true` if the current search operation should be performed using fuzzy searching.
     *
     * @return `true` if fuzzy search should be used
     */
    val isFuzzySearch: Boolean

    /**
     * Returns the filter expression.
     *
     * @return the filter
     */
    val filter: String

    /**
     * Returns the fields that should be returned by the query. If all fields should be returned, the method will return
     * an empty array.
     *
     * @return the names of the fields to return
     */
    val fields: Array<String>

    /**
     * Returns all the known sort orders. The insertion-order is kept.
     *
     * @return a map with all known sort orders
     */
    val sortOrders: Map<String, Order>

    /**
     * Sort order definitions.
     */
    enum class Order {
        None, Ascending, Descending
    }

    /**
     * Return documents of the given types.
     *
     * @param types
     * the resource types to look up
     * @return the query extended by this criterion
     */
    fun withTypes(vararg types: String): SearchQuery

    /**
     * Sets the number of results that are returned.
     *
     * @param limit
     * the number of results
     * @return the search query
     */
    fun withLimit(limit: Int): SearchQuery

    /**
     * Sets the starting offset. Search results will be returned starting at that offset and until the limit is reached,
     * as specified by `getLimit()`.
     *
     * @param offset
     * the starting offset
     * @return the search query
     */
    fun withOffset(offset: Int): SearchQuery

    /**
     * Returns documents that contain the given text.
     *
     * @param text
     * the text to look up
     * @return the query extended by this criterion
     */
    fun withText(text: String): SearchQuery

    /**
     * Returns documents that contain the given text.
     *
     * @param wildcardSearch
     * `True` to perform a (much slower) wildcard search
     * @param text
     * the text to look up
     *
     * @return the query extended by this criterion
     */
    fun withText(wildcardSearch: Boolean, text: String): SearchQuery

    /**
     * Returns documents that contain the given text.
     *
     *
     * Depending on the quantifier, either documents are returned that contain at least one of the terms are only
     * documents containing all of the terms.
     *
     * @param text
     * the text to look up
     * @param quantifier
     * whether all or some of the terms need to be matched
     * @param fuzzy
     * `true` to perform a fuzzy search
     * @return the query extended by this criterion
     */
    fun withText(fuzzy: Boolean, quantifier: SearchTerms.Quantifier, vararg text: String): SearchQuery

    /**
     * Returns documents that match the search query *and* the text filter.
     *
     * @param filter
     * the filter text
     * @return the search query
     */
    fun withFilter(filter: String): SearchQuery

    /**
     * Adds a field that needs to be returned by the query. If no fields are being set, all fields will be returned.
     *
     * @param field
     * the field name
     * @return the query
     */
    fun withField(field: String): SearchQuery

    /**
     * Adds the fields that need to be returned by the query. If no fields are being set, all fields will be returned.
     *
     * @param fields
     * the field names
     * @return the query
     */
    fun withFields(vararg fields: String): SearchQuery

    /**
     * Sort the result set by the field and the given order. The insertion-order is kept.
     *
     * @param field
     * the field name, must not be `null`
     * @param order
     * the order direction, must not be `null`
     * @return the updated search query
     */
    fun withSortOrder(field: String, order: Order): SearchQuery

    /**
     * Returns the sort order of a field. If no sort order has been set for the given field, [Order.None] is
     * returned.
     *
     * @param field
     * the field name, must not be `null`
     * @return the sort order
     */
    fun getSortOrder(field: String): Order

}
