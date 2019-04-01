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
 * A result as returned by a search operation.
 */
interface SearchResult<T> {

    /**
     * Returns the individual items of this search result.
     *
     * @return the search result items
     */
    val items: Array<SearchResultItem<T>>

    /**
     * Returns the total number of appearances of the search criteria in the search result, spread over
     * `getDocumentCount` number of documents.
     *
     * @return the overall number of hits
     */
    val hitCount: Long

    /**
     * Returns the total number of items in the search result.
     *
     *
     * Note that this number might not match the size of the array as returned by [.getItems], which is likely to
     * be limited by the value returned by [.getLimit].
     *
     * @return the number of documents containing the hits
     */
    val documentCount: Long

    /**
     * Returns the number of items in this search result, possibly limited with respect to the total number of result
     * items by `offset` and `limit`.
     *
     * @return the total number of hits.
     * @see .getOffset
     * @see .getLimit
     */
    val pageSize: Long

    /**
     * Get the offset within the search result or `-1` if no limit has been specified.
     *
     * @return the offset
     */
    val offset: Long

    /**
     * Returns the limit of this search results or `-1` if no limit has been specified.
     *
     * @return the limit
     */
    val limit: Long

    /**
     * Returns the page of the current result items within the complete search result. This number is influenced by the
     * `offset` and the page size `limit`.
     *
     *
     * Note that the page size is one-based
     *
     * @return the page number
     */
    val page: Long

    /**
     * Returns the search time in milliseconds.
     *
     * @return the time
     */
    val searchTime: Long

}
