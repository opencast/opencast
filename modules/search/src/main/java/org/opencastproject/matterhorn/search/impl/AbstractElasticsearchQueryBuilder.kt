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

import org.opencastproject.matterhorn.search.impl.IndexSchema.TEXT
import org.opencastproject.matterhorn.search.impl.IndexSchema.TEXT_FUZZY

import org.opencastproject.matterhorn.search.SearchQuery
import org.opencastproject.util.DateTimeSupport

import org.apache.commons.lang3.StringUtils
import org.apache.lucene.search.Query
import org.elasticsearch.common.io.stream.StreamOutput
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchAllQueryBuilder
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryRewriteContext
import org.elasticsearch.index.query.QueryShardContext
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

/**
 * Opencast implementation of the elastic search query builder.
 */
abstract class AbstractElasticsearchQueryBuilder<T : SearchQuery>
/**
 * Creates a new elastic search query based on the raw query.
 *
 * @param query
 * the search query
 */
(
        /** The original search query  */
        /**
         * Returns the original search query.
         *
         * @return the search query
         */
        val query: T) : QueryBuilder {

    /** Term queries on fields  */
    private var searchTerms: MutableMap<String, Set<Any>>? = null

    /** Fields that need to match all values  */
    protected var groups: List<ValueGroup>? = null

    /** Fields that query a date range  */
    private var dateRanges: MutableSet<DateRange>? = null

    /** Filter expression  */
    protected var filter: String? = null

    /** Text query  */
    protected var text: String? = null

    /** Fuzzy text query  */
    protected var fuzzyText: String? = null

    /** The boolean query  */
    private var queryBuilder: QueryBuilder? = null

    init {
        buildQuery(query)
        createQuery()
    }

    /**
     * {@inheritDoc}
     */
    abstract fun buildQuery(query: T)

    /**
     * Create the actual query. We start with a query that matches everything, then move to the boolean conditions,
     * finally add filter queries.
     */
    private fun createQuery() {

        queryBuilder = MatchAllQueryBuilder()

        // The boolean query builder
        val booleanQuery = BoolQueryBuilder()

        // Terms
        if (searchTerms != null) {
            for ((key, value) in searchTerms!!) {
                booleanQuery.must(TermsQueryBuilder(key, *value.toTypedArray()))
            }
            this.queryBuilder = booleanQuery
        }

        // Date ranges
        if (dateRanges != null) {
            for (dr in dateRanges!!) {
                booleanQuery.must(dr.queryBuilder)
            }
            this.queryBuilder = booleanQuery
        }

        // Text
        if (text != null) {
            val queryBuilder = QueryBuilders.queryStringQuery(text!!).field(TEXT)
            booleanQuery.must(queryBuilder)
            this.queryBuilder = booleanQuery
        }

        // Fuzzy text
        if (fuzzyText != null) {
            val moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(
                    arrayOf(TEXT_FUZZY),
                    arrayOf<String>(fuzzyText), null)
            booleanQuery.must(moreLikeThisQueryBuilder)
            this.queryBuilder = booleanQuery
        }

        val filters = ArrayList<QueryBuilder>()

        // Add filtering for AND terms
        if (groups != null) {
            for (group in groups!!) {
                filters.addAll(group.filterBuilders)
            }
        }

        // Filter expressions
        if (filter != null) {
            filters.add(QueryBuilders.termQuery(IndexSchema.TEXT, filter!!))
        }

        // Apply the filters
        if (!filters.isEmpty()) {
            for (filter in filters) {
                booleanQuery.filter(filter)
            }
            this.queryBuilder = booleanQuery
        }

    }

    /**
     * Stores `fieldValue` as a search term on the `fieldName` field.
     *
     * @param fieldName
     * the field name
     * @param fieldValues
     * the field value
     */
    protected fun and(fieldName: String, vararg fieldValues: Any) {
        var fieldName = fieldName

        // Make sure the data structures are set up accordingly
        if (searchTerms == null) {
            searchTerms = HashMap()
        }

        // Fix the field name, just in case
        fieldName = StringUtils.trim(fieldName)

        // insert value
        (searchTerms as java.util.Map<String, Set<Any>>).computeIfAbsent(fieldName) { k -> HashSet() }
                .addAll(Arrays.asList(*fieldValues))
    }

    /**
     * Stores `fieldValue` as a search term on the `fieldName` field.
     *
     * @param fieldName
     * the field name
     * @param startDate
     * the start date
     * @param endDate
     * the end date
     */
    protected fun and(fieldName: String, startDate: Date, endDate: Date) {
        var fieldName = fieldName

        // Fix the field name, just in case
        fieldName = StringUtils.trim(fieldName)

        // Make sure the data structures are set up accordingly
        if (dateRanges == null)
            dateRanges = HashSet()

        // Add the term
        dateRanges!!.add(DateRange(fieldName, startDate, endDate))
    }

    /**
     * {@inheritDoc}
     *
     * @see org.elasticsearch.common.xcontent.ToXContent.toXContent
     */
    @Throws(IOException::class)
    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return queryBuilder!!.toXContent(builder, params)
    }

    @Throws(IOException::class)
    override fun toQuery(context: QueryShardContext): Query {
        return queryBuilder!!.toQuery(context)
    }

    @Throws(IOException::class)
    override fun toFilter(context: QueryShardContext): Query {
        return queryBuilder!!.toFilter(context)
    }

    override fun queryName(queryName: String): QueryBuilder {
        return queryBuilder!!.queryName(queryName)
    }

    override fun queryName(): String {
        return queryBuilder!!.queryName()
    }

    override fun boost(): Float {
        return queryBuilder!!.boost()
    }

    override fun boost(boost: Float): QueryBuilder {
        return queryBuilder!!.boost(boost)
    }

    override fun getName(): String {
        return queryBuilder!!.name
    }

    override fun getWriteableName(): String {
        return queryBuilder!!.writeableName
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        queryBuilder!!.writeTo(out)
    }

    @Throws(IOException::class)
    override fun rewrite(queryShardContext: QueryRewriteContext?): QueryBuilder {
        return queryBuilder!!.rewrite(queryShardContext)
    }

    override fun isFragment(): Boolean {
        return queryBuilder!!.isFragment
    }

    /**
     * Utility class to hold date range specifications and turn them into elastic search queries.
     */
    class DateRange
    /**
     * Creates a new date range specification with the given field name, start and end dates. `null` may be
     * passed in for start or end dates that should remain unspecified.
     *
     * @param field
     * the field name
     * @param start
     * the start date
     * @param end
     * the end date
     */
    internal constructor(
            /** The field name  */
            private val field: String,
            /** The start date  */
            private val startDate: Date?,
            /** The end date  */
            private val endDate: Date?) {

        /**
         * Returns the range query that is represented by this date range.
         *
         * @return the range query builder
         */
        internal val queryBuilder: QueryBuilder
            get() {
                val rqb = RangeQueryBuilder(field)
                if (startDate != null)
                    rqb.from(DateTimeSupport.toUTC(startDate.time))
                if (endDate != null)
                    rqb.to(DateTimeSupport.toUTC(endDate.time))
                return rqb
            }

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Object.equals
         */
        override fun equals(obj: Any?): Boolean {
            return obj is DateRange && obj.field == field
        }

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Object.hashCode
         */
        override fun hashCode(): Int {
            return field.hashCode()
        }

    }

    /**
     * Stores a group of values which will later be added to the query using AND.
     */
    class ValueGroup
    /**
     * Creates a new value group for the given field and values.
     *
     * @param field
     * the field name
     * @param values
     * the values
     */
    (
            /** The field name  */
            private val field: String, vararg values: Any) {

        /** The values to store  */
        private val values: Array<Any>

        /**
         * Returns the filter that will make sure only documents are returned that match all of the values at once.
         *
         * @return the filter builder
         */
        internal val filterBuilders: List<QueryBuilder>
            get() = Arrays.stream(values)
                    .map<TermQueryBuilder> { v -> QueryBuilders.termQuery(field, v.toString()) }
                    .collect<List<QueryBuilder>, Any>(Collectors.toList())

        init {
            this.values = values
        }

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Object.equals
         */
        override fun equals(obj: Any?): Boolean {
            return obj is ValueGroup && obj.field == field
        }

        /**
         * {@inheritDoc}
         *
         * @see java.lang.Object.hashCode
         */
        override fun hashCode(): Int {
            return field.hashCode()
        }

    }

}
