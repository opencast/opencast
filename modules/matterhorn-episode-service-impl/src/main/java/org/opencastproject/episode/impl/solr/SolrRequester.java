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
package org.opencastproject.episode.impl.solr;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.JaxbSearchResult;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.impl.Convert;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.util.data.functions.Strings;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/** Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility. */
public class SolrRequester {
  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(SolrRequester.class);

  /** The connection to the solr database */
  private SolrServer solrServer = null;

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   * 
   * @param connection
   *          the solr connection
   */
  public SolrRequester(SolrServer connection) {
    if (connection == null)
      throw new IllegalStateException("Unable to run queries on null connection");
    this.solrServer = connection;
  }

  /**
   * Creates a search result from a given solr response.
   * 
   * @param query
   *          The solr query.
   * @return The search result.
   * @throws SolrServerException
   *           if the solr server is not working as expected
   */
  private SearchResult createSearchResult(final SolrQuery query) throws SolrServerException {
    // Execute the query and try to get hold of a query response
    QueryResponse solrResponse = null;
    try {
      solrResponse = solrServer.query(query);
    } catch (Exception e) {
      throw new SolrServerException(e);
    }

    // Create and configure the query result
    final JaxbSearchResult result = new JaxbSearchResult(query.getQuery());
    result.setSearchTime(solrResponse.getQTime());
    result.setOffset(solrResponse.getResults().getStart());
    result.setLimit(solrResponse.getResults().size());
    result.setTotalSize(solrResponse.getResults().getNumFound());

    // Walk through response and create new items with title, creator, etc:
    for (final SolrDocument doc : solrResponse.getResults()) {
      // Add the item to the result set
      result.addItem(Convert.convert(doc, query));
    }
    return result;
  }

  private static final List<Tuple<String, Float>> fullTextQueryFields = list(
          tuple(Schema.DC_TITLE_SUM, Schema.DC_TITLE_BOOST), tuple(Schema.S_DC_TITLE_SUM, Schema.S_DC_TITLE_BOOST),
          tuple(Schema.DC_IS_PART_OF, Schema.DC_IS_PART_OF_BOOST),
          tuple(Schema.DC_CREATOR_SUM, Schema.DC_CREATOR_BOOST), tuple(Schema.DC_SUBJECT_SUM, Schema.DC_SUBJECT_BOOST),
          tuple(Schema.DC_PUBLISHER_SUM, Schema.DC_PUBLISHER_BOOST),
          tuple(Schema.DC_CONTRIBUTOR_SUM, Schema.DC_CONTRIBUTOR_BOOST),
          tuple(Schema.DC_ABSTRACT_SUM, Schema.DC_ABSTRACT_BOOST),
          tuple(Schema.DC_DESCRIPTION_SUM, Schema.DC_DESCRIPTION_BOOST), tuple(Schema.FULLTEXT, 1.0f));

  /**
   * Modifies the query such that certain fields are being boosted (meaning they gain some weight).
   * 
   * @param query
   *          The user query.
   * @return The boosted query
   */
  public StringBuilder createBoostedFullTextQuery(String query) {
    final StringBuilder sb = new StringBuilder();
    for (Tuple<String, Float> f : fullTextQueryFields) {
      appendFuzzyBoosted(sb, f.getA(), some(query), some(f.getB()), "OR");
    }
    return sb;
  }

  /**
   * Simple helper method to avoid null strings.
   * 
   * @param f
   *          object which implements <code>toString()</code> method.
   * @return The input object or empty string.
   */
  private static String mkString(Object f) {
    if (f != null)
      return f.toString();
    else
      return "";
  }

  /**
   * Converts the query object into a solr query and returns the results.
   * 
   * @param q
   *          the query
   * @return the search results
   */
  private SolrQuery createQuery(EpisodeQuery q) throws SolrServerException {
    final StringBuilder sb = new StringBuilder();
    for (String solrQueryRequest : q.getQuery())
      sb.append(solrQueryRequest);
    append(sb, Schema.DC_ID, q.getId());

    // full text query with boost
    for (String solrTextRequest : q.getText()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(").append(createBoostedFullTextQuery(solrTextRequest)).append(")");
    }

    appendFuzzy(sb, Schema.DC_CREATOR_SUM, q.getCreator());
    appendFuzzy(sb, Schema.DC_CONTRIBUTOR_SUM, q.getContributor());
    append(sb, Schema.DC_LANGUAGE, q.getLanguage());
    appendFuzzy(sb, Schema.DC_LICENSE_SUM, q.getLicense());
    appendFuzzy(sb, Schema.DC_TITLE_SUM, q.getTitle());
    appendFuzzy(sb, Schema.S_DC_TITLE_SUM, q.getSeriesTitle());
    append(sb, Schema.DC_IS_PART_OF, q.getSeriesId());
    append(sb, Schema.OC_ORGANIZATION, q.getOrganization());

    if (q.getElementTags().size() > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      StringBuilder tagBuilder = new StringBuilder();
      for (String tag : mlist(q.getElementTags()).bind(Options.<String> asList().o(SolrUtils.clean))) {
        if (tagBuilder.length() == 0) {
          tagBuilder.append("(");
        } else {
          tagBuilder.append(" OR ");
        }
        tagBuilder.append(Schema.OC_ELEMENTTAGS);
        tagBuilder.append(":");
        tagBuilder.append(SolrUtils.clean(tag));
      }
      if (tagBuilder.length() > 0) {
        tagBuilder.append(") ");
        sb.append(tagBuilder);
      }
    }
    if (q.getElementFlavors().size() > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      StringBuilder flavorBuilder = new StringBuilder();
      for (String flavor : mlist(q.getElementFlavors()).bind(
              Options.<String> asList().o(SolrUtils.clean).o(Strings.<MediaPackageElementFlavor> asStringNull()))) {
        if (flavorBuilder.length() == 0) {
          flavorBuilder.append("(");
        } else {
          flavorBuilder.append(" OR ");
        }
        flavorBuilder.append(Schema.OC_ELEMENTFLAVORS);
        flavorBuilder.append(":");
        flavorBuilder.append(ClientUtils.escapeQueryChars(flavor));
      }
      if (flavorBuilder.length() > 0) {
        flavorBuilder.append(") ");
        sb.append(flavorBuilder);
      }
    }
    for (Date deleted : q.getDeletedDate()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_DELETED).append(":")
              .append(SolrUtils.serializeDateRange(option(deleted), Option.<Date> none()));
    }
    if (!q.getIncludeDeleted()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("-" + Schema.OC_DELETED + ":[* TO *]");
    }

    if (q.getOnlyLastVersion()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_LATEST_VERSION + ":true");
    }

    // only episodes
    if (sb.length() > 0)
      sb.append(" AND ");
    sb.append(Schema.OC_MEDIATYPE + ":" + SearchResultItem.SearchResultItemType.AudioVisual);

    // only add date range if at least on criteria is set
    if (q.getAddedBefore().isSome() || q.getAddedAfter().isSome()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_TIMESTAMP + ":[" + q.getAddedAfter().map(SolrUtils.serializeDate).getOrElse("*") + " TO "
              + q.getAddedBefore().map(SolrUtils.serializeDate).getOrElse("*") + "]");
    }

    if (sb.length() == 0)
      sb.append("*:*");

    final SolrQuery solr = new SolrQuery(sb.toString());
    // limit & offset
    solr.setRows(q.getLimit());
    solr.setStart(q.getOffset());

    // sorting
    final SolrQuery.ORDER order = q.getSortAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    solr.addSortField(getSortField(q.getSort()), order);

    solr.setFields("* score");

    return solr;
  }

  private static String getSortField(EpisodeQuery.Sort sort) {
    switch (sort) {
      case TITLE:
        return Schema.DC_TITLE_SORT;
      case DATE_CREATED:
        return Schema.DC_CREATED;
      case CREATOR:
        return Schema.DC_CREATOR_SORT;
      case SERIES_TITLE:
        return Schema.S_DC_TITLE_SORT;
      default:
        return Schema.DC_CREATED;
    }
  }

  /**
   * Appends query parameters to a solr query
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, Option<String> value) {
    for (String val : value) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(").append(key).append(":").append(SolrUtils.clean(val)).append(")");
    }
    return sb;
  }

  /**
   * Appends query parameters to a solr query in a way that they are found even though they are not treated as a full
   * word in solr.
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder appendFuzzy(StringBuilder sb, String key, Option<String> value) {
    return appendFuzzyBoosted(sb, key, value, none(0.0f), "AND");
  }

  private StringBuilder appendFuzzyBoosted(StringBuilder sb, String key, Option<String> value, Option<Float> boost,
          String join) {
    for (String val : value) {
      if (sb.length() > 0) {
        sb.append(" ").append(join).append(" ");
      }
      final String boostSuffix = boost.map(mkBoost).getOrElse("");
      sb.append("(");
      sb.append(key).append(":(").append(SolrUtils.clean(val)).append(")").append(boostSuffix);
      sb.append(" OR ");
      sb.append(key).append(":(*").append(SolrUtils.clean(val)).append("*)").append(boostSuffix);
      sb.append(")");
    }
    return sb;
  }

  private static final Function<Float, String> mkBoost = new Function<Float, String>() {
    @Override
    public String apply(Float boost) {
      return "^" + Float.toString(boost);
    }
  };

  /**
   * Query the Solr index.
   * 
   * @param q
   *          the search query
   */
  public SearchResult find(EpisodeQuery q) throws SolrServerException {
    SolrQuery query = createQuery(q);
    return createSearchResult(query);
  }
}
