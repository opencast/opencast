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

import java.util.Objects.requireNonNull
import org.opencastproject.matterhorn.search.SearchTerms.Quantifier.Any

import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.matterhorn.search.SearchTerms
import org.opencastproject.matterhorn.search.SearchTerms.Quantifier

import org.apache.commons.lang3.StringUtils

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Stack

/**
 * Base implementation for search queries.
 */
open class AbstractSearchQuery
/**
 * Creates a search query that is executed on all document types.
 */
protected constructor()// Nothing to be done atm.
    : SearchQuery {

    /** The document types  */
    protected var types: MutableList<String> = ArrayList()

    /** The list of fields to return  */
    protected var fields: MutableList<String>? = null

    /** Query configuration stack  */
    protected var stack = Stack<Any>()

    /** The object that needs to show up next  */
    protected var expectation: Class<*>? = null

    /** True if the search text should be matched using wildcards  */
    protected var fuzzySearch = true

    /** Query terms  */
    protected var text: MutableList<SearchTerms<String>>? = null

    /** Filter terms  */
    protected var filter: String? = null

    /** The query offset  */
    protected var offset = -1

    /** The query limit  */
    protected var limit = -1

    /** The map with the sort orders  */
    private val sortOrders = LinkedHashMap<String, SearchQuery.Order>()

    /** The last method called  */
    protected var lastMethod: String? = null

    /**
     * Creates a search query that is executed on the given document type.
     *
     * @param documentType
     * the document type
     */
    constructor(documentType: String) : this() {
        if (StringUtils.isNotBlank(documentType))
            this.types.add(documentType)
    }

    /**
     * {@inheritDoc}
     */
    override fun withTypes(vararg types: String): SearchQuery {
        this.types.addAll(Arrays.asList(*types))
        return this
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.matterhorn.search.SearchQuery.getTypes
     */
    override fun getTypes(): Array<String> {
        return types.toTypedArray()
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withField
     */
    override fun withField(field: String): AbstractSearchQuery {
        if (fields == null)
            fields = ArrayList()
        fields!!.add(field)
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withFields
     */
    override fun withFields(vararg fields: String): AbstractSearchQuery {
        for (field in fields) {
            withField(field)
        }
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getFields
     */
    override fun getFields(): Array<String> {
        return if (fields == null) arrayOf() else fields!!.toTypedArray()
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withLimit
     */
    override fun withLimit(limit: Int): SearchQuery {
        this.limit = limit
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getLimit
     */
    override fun getLimit(): Int {
        return limit
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withOffset
     */
    override fun withOffset(offset: Int): SearchQuery {
        this.offset = Math.max(0, offset)
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getOffset
     */
    override fun getOffset(): Int {
        return offset
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withText
     */
    override fun withText(text: String): SearchQuery {
        return withText(false, Any, text)
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withText
     */
    override fun withText(wildcardSearch: Boolean, text: String): SearchQuery {
        return withText(wildcardSearch, Any, text)
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withText
     */
    override fun withText(wildcardSearch: Boolean, quantifier: Quantifier?, vararg text: String): SearchQuery {
        if (quantifier == null)
            throw IllegalArgumentException("Quantifier must not be null")
        if (text == null)
            throw IllegalArgumentException("Text must not be null")

        // Make sure the collection is initialized
        if (this.text == null)
            this.text = ArrayList()

        // Add the text to the search terms
        clearExpectations()
        this.fuzzySearch = wildcardSearch

        // Handle any quantifier
        if (text.size == 1 || Any == quantifier) {
            var terms: SearchTerms<String>? = null

            // Check if there is a default terms collection
            for (t in this.text!!) {
                if (Quantifier.Any == t.quantifier) {
                    terms = t
                    break
                }
            }

            // Has there been a default terms collection?
            if (terms == null) {
                terms = SearchTermsImpl(Quantifier.Any, *text)
                this.text!!.add(terms)
            }

            // All quantifier
        } else {
            this.text!!.add(SearchTermsImpl(quantifier, *text))
        }
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getTerms
     */
    override fun getTerms(): Collection<SearchTerms<String>> {
        return if (text == null) emptyList() else text
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getQueryString
     */
    override fun getQueryString(): String? {
        if (text == null)
            return null
        val query = StringBuilder()
        for (s in text!!) {
            for (t in s.terms) {
                if (query.length == 0)
                    query.append(" ")
                query.append(t)
            }
        }
        return query.toString()
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.isFuzzySearch
     */
    override fun isFuzzySearch(): Boolean {
        return fuzzySearch
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.withFilter
     */
    override fun withFilter(filter: String): SearchQuery {
        clearExpectations()
        this.filter = filter
        return this
    }

    /**
     * @see org.opencastproject.matterhorn.search.SearchQuery.getFilter
     */
    override fun getFilter(): String? {
        return filter
    }

    override fun withSortOrder(field: String, order: SearchQuery.Order): SearchQuery {
        sortOrders[requireNonNull(field)] = requireNonNull<Order>(order)
        return this
    }

    override fun getSortOrders(): Map<String, SearchQuery.Order> {
        return Collections.unmodifiableMap(sortOrders)
    }

    override fun getSortOrder(field: String): SearchQuery.Order {
        return if (!sortOrders.containsKey(field)) SearchQuery.Order.None else sortOrders[field]

    }

    /**
     * Pushes the configuration object onto the stack.
     *
     * @param object
     * the object
     */
    protected fun configure(`object`: Any) {
        stack.push(`object`)
    }

    /**
     * Sets the expectation to `c`, making sure that the next configuration object will either match
     * `c` in terms of class of throw an `IllegalStateException` if it doesn't.
     *
     * @param c
     * the class type
     */
    protected fun expect(c: Class<*>) {
        lastMethod = Thread.currentThread().stackTrace[2].methodName
        this.expectation = c
    }

    /**
     * This method is called if nothing should be expected by anyone. If this is not the case (e. g. some unfinished query
     * configuration is still in place) we throw an `IllegalStateException`.
     *
     * @throws IllegalStateException
     * if some object is expected
     */
    @Throws(IllegalStateException::class)
    protected fun clearExpectations() {
        if (expectation != null)
            throw IllegalStateException("Query configuration expects " + expectation!!.javaClass.name)
        stack.clear()
    }

}
