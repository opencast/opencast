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


package org.opencastproject.matterhorn.search.impl

import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SearchResult
import org.opencastproject.matterhorn.search.SearchResultItem

import java.util.ArrayList

/**
 * Base implementation for a search result.
 */
class SearchResultImpl<T>
/**
 * Creates a search result that was created using the given query. Note that `hits` indicates the overall
 * number of appearances of the search term, while size is equal to the number of documents that contain those
 * `hits` hits.
 *
 * @param query
 * the query
 * @param hitCount
 * the number of hits
 * @param documentCount
 * the total size of the result set
 */
(query: SearchQuery, hitCount: Long, documentCount: Long) : SearchResult<T> {

    /** The query that led to this search result  */
    /**
     * {@inheritDoc}
     */
    var query: SearchQuery? = null
        protected set

    /** The search offset  */
    protected var offset: Long = 0

    /** The search limit  */
    protected var limit: Long = 0

    /** The total number of appearances of the search criteria  */
    protected var hitCount: Long = 0

    /** The total size of the search result set  */
    protected var documentCount: Long = 0

    /** The time it took to do the search in ms  */
    protected var time: Long = 0

    /** The search result  */
    protected var result: MutableList<SearchResultItem<T>>? = null

    init {
        this.query = query
        this.offset = query.offset.toLong()
        this.limit = query.limit.toLong()
        this.hitCount = hitCount
        this.documentCount = documentCount
    }

    /**
     * Adds the given search result item to the result set.
     *
     * @param item
     * the result item
     */
    fun addResultItem(item: SearchResultItem<T>) {
        if (result == null)
            result = ArrayList()
        result!!.add(item)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getItems
     */
    override fun getItems(): Array<SearchResultItem<T>> {
        return if (result == null) arrayOf<SearchResultItem<*>>() else result!!.toTypedArray<SearchResultItem<*>>()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getLimit
     */
    override fun getLimit(): Long {
        return limit
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getOffset
     */
    override fun getOffset(): Long {
        return offset
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getPage
     */
    override fun getPage(): Long {
        return if (offset == 0L || limit == 0L) 1 else Math.floor((offset / limit).toDouble()).toLong() + 1
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getPageSize
     */
    override fun getPageSize(): Long {
        return (if (result != null) result!!.size else 0).toLong()
    }

    /**
     * Sets the search time in milliseconds.
     *
     * @param time
     * the time
     */
    fun setSearchTime(time: Long) {
        this.time = time
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getSearchTime
     */
    override fun getSearchTime(): Long {
        return time
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getHitCount
     */
    override fun getHitCount(): Long {
        return hitCount
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchResult.getDocumentCount
     */
    override fun getDocumentCount(): Long {
        return if (result != null) result!!.size else documentCount
    }

    /**
     * Sets the document count.
     *
     * @param count
     * the number of documents in this search result
     */
    fun setDocumentCount(count: Long) {
        this.documentCount = count
    }

}
