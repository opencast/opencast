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

package org.opencastproject.solr

import org.apache.lucene.queryParser.ParseException
import org.apache.lucene.search.Query
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.request.SolrQueryRequest
import org.apache.solr.search.QParser
import org.apache.solr.search.QParserPlugin
import org.apache.solr.search.SolrQueryParser

/**
 * Parser plugin that creates a parser with support for full wildcard searches.
 */
class WildcardQueryParserPlugin : QParserPlugin() {

    /**
     * {@inheritDoc}
     *
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin.init
     */
    override fun init(args: NamedList<*>) {}

    /**
     * {@inheritDoc}
     *
     * @see org.apache.solr.search.QParserPlugin.createParser
     */
    override fun createParser(qstr: String, localParams: SolrParams, params: SolrParams, req: SolrQueryRequest): QParser {
        return WildcardQueryParser(qstr, localParams, params, req)
    }

    /**
     * Parser that allows for wildcard queries.
     */
    private class WildcardQueryParser
    /**
     * Creates a new wildcard query parser.
     *
     * @param qstr
     * the query string
     * @param localParams
     * local parameters
     * @param params
     * additional parameters
     * @param req
     * the query request
     */
    internal constructor(qstr: String, localParams: SolrParams, params: SolrParams, req: SolrQueryRequest) : QParser(qstr, localParams, params, req) {

        /**
         * {@inheritDoc}
         *
         * @see org.apache.solr.search.QParser.parse
         */
        @Throws(ParseException::class)
        override fun parse(): Query {
            val solrParser = SolrQueryParser(this, DEFAULT_FIELD)
            solrParser.allowLeadingWildcard = true
            return solrParser.parse(super.qstr)
        }

    }

    companion object {

        /** Default field name  */
        private val DEFAULT_FIELD = "fulltext"
    }

}
