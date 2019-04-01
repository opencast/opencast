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

import org.opencastproject.matterhorn.search.SearchResultItem

/**
 * Default implementation of a [SearchResultItem].
 */
class SearchResultItemImpl<T>
/**
 * Creates a new search result item with the given uri. The `source` is the object that created the item,
 * usually, this will be the site itself but it could very well be a module that added to a search result.
 *
 * @param relevance
 * the score inside the search result
 * @param source
 * the object that produced the result item
 */
(relevance: Double, source: T) : SearchResultItem<T> {

    /** Source of the search result  */
    protected var source: T? = null

    /** Score within the search result  */
    protected var score = 0.0

    init {
        this.source = source
        this.score = relevance
    }

    /**
     * Sets the relevance value, representing the score in the search result.
     *
     * @param relevance
     * the relevance value
     */
    fun setRelevance(relevance: Double) {
        this.score = relevance
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchResultItem.getRelevance
     */
    override fun getRelevance(): Double {
        return score
    }

    /**
     * Returns the search result's source.
     *
     * @see org.opencastproject.matterhorn.search.SearchResultItem.getSource
     */
    override fun getSource(): T? {
        return source
    }

    /**
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(sr: SearchResultItem<T>): Int {
        if (score < sr.relevance)
            return 1
        else if (score > sr.relevance)
            return -1
        return 0
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        return this === obj
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return java.lang.Double.toString(score).hashCode()
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return source!!.toString()
    }

}
