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

package org.opencastproject.solr;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrQueryParser;

/**
 * Parser plugin that creates a parser with support for full wildcard searches.
 */
public class WildcardQueryParserPlugin extends QParserPlugin {

  /** Default field name */
  private static final String DEFAULT_FIELD = "fulltext";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void init(NamedList args) {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String, org.apache.solr.common.params.SolrParams,
   *      org.apache.solr.common.params.SolrParams, org.apache.solr.request.SolrQueryRequest)
   */
  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new WildcardQueryParser(qstr, localParams, params, req);
  }

  /**
   * Parser that allows for wildcard queries.
   */
  private static class WildcardQueryParser extends QParser {

    /**
     * Creates a new wildcard query parser.
     *
     * @param qstr
     *          the query string
     * @param localParams
     *          local parameters
     * @param params
     *          additional parameters
     * @param req
     *          the query request
     */
    public WildcardQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      super(qstr, localParams, params, req);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.solr.search.QParser#parse()
     */
    @Override
    public Query parse() throws ParseException {
      SolrQueryParser solrParser = new SolrQueryParser(this, DEFAULT_FIELD);
      solrParser.setAllowLeadingWildcard(true);
      return solrParser.parse(super.qstr);
    }

  }

}
