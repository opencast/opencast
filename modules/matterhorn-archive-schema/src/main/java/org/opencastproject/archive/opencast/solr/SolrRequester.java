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

package org.opencastproject.archive.opencast.solr;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.opencast.OpencastQuery;
import org.opencastproject.archive.opencast.OpencastResultItem;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.schema.OcDublinCore;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;
import org.opencastproject.util.data.Tuple;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility. */
public class SolrRequester {
  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(SolrRequester.class);

  /** The connection to the solr database */
  private final SolrServer solrServer;

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
   * Query the Solr index.
   * 
   * @param q
   *          the search query
   */
  public OpencastResultSet find(OpencastQuery q) throws SolrServerException {
    return runQuery(createQuery(q));
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
  private OpencastResultSet runQuery(SolrQuery query) throws SolrServerException {
    final QueryResponse res = solrServer.query(query);
    final List<OpencastResultItem> items = mlist(res.getResults()).map(solrToItem).value();
    final String queryString = query.getQuery();
    final long totalSize = res.getResults().getNumFound();
    final long limit = res.getResults().size();
    final long offset = res.getResults().getStart();
    final long searchTime = res.getQTime();
    return new OpencastResultSet() {
      @Override
      public List<OpencastResultItem> getItems() {
        return items;
      }

      @Override
      public String getQuery() {
        return queryString;
      }

      @Override
      public long getTotalSize() {
        return totalSize;
      }

      @Override
      public long getLimit() {
        return limit;
      }

      @Override
      public long getOffset() {
        return offset;
      }

      @Override
      public long getSearchTime() {
        return searchTime;
      }
    };
  }

  private static final Function<SolrDocument, OpencastResultItem> solrToItem = new Function.X<SolrDocument, OpencastResultItem>() {
    @Override
    public OpencastResultItem xapply(final SolrDocument source) throws Exception {
      final String mediaPackageId = Schema.getDcId(source);
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(Schema.getOcMediapackage(source));
      final Option<String> seriesId = option(Schema.getDcIsPartOf(source));
      final AccessControlList acl = AccessControlParser.parseAcl(Schema.getOcAcl(source));
      final Version version = Schema.getOcVersion(source);
      final boolean latestVersion = Schema.isOcLatestVersion(source);
      final String organizationId = Schema.getOrganization(source);
      final OcDublinCore dublinCore = Schema.getDublinCore(source);
      final Option<OcDublinCore> seriesDublinCore = Schema.getSeriesDublinCore(source);

      return new OpencastResultItem() {
        @Override
        public String getMediaPackageId() {
          return mediaPackageId;
        }

        @Override
        public Option<OcDublinCore> getSeriesDublinCore() {
          return seriesDublinCore;
        }

        @Override
        public MediaPackage getMediaPackage() {
          return mediaPackage;
        }

        @Override
        public Option<String> getSeriesId() {
          return seriesId;
        }

        @Override
        public AccessControlList getAcl() {
          return acl;
        }

        @Override
        public Version getVersion() {
          return version;
        }

        @Override
        public String getOrganizationId() {
          return organizationId;
        }

        @Override
        public OcDublinCore getDublinCore() {
          return dublinCore;
        }

        @Override
        public boolean isLatestVersion() {
          return latestVersion;
        }

      };
    }
  };

  /** Converts the query object into a solr query. */
  // todo rewrite using solr DSL
  private SolrQuery createQuery(OpencastQuery q) throws SolrServerException {
    final StringBuilder sb = new StringBuilder();
    append(sb, Schema.DC_ID, q.getMediaPackageId());

    // full text query with boost
    for (String solrTextRequest : q.getFullText()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(").append(createBoostedFullTextQuery(solrTextRequest)).append(")");
    }

    appendFuzzy(sb, Schema.DC_CREATOR_SUM, q.getDcCreator());
    appendFuzzy(sb, Schema.DC_CONTRIBUTOR_SUM, q.getDcContributor());
    append(sb, Schema.DC_LANGUAGE, q.getDcLanguage());
    appendFuzzy(sb, Schema.DC_LICENSE_SUM, q.getDcLicense());
    appendFuzzy(sb, Schema.DC_TITLE_SUM, q.getDcTitle());
    appendFuzzy(sb, Schema.S_DC_TITLE_SUM, q.getSeriesTitle());
    append(sb, Schema.DC_IS_PART_OF, q.getSeriesId());
    append(sb, Schema.OC_ORGANIZATION, q.getOrganizationId());

    if (q.getDeletedAfter().isSome() || q.getDeletedBefore().isSome()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_DELETED).append(":true AND ").append(Schema.OC_TIMESTAMP).append(":")
              .append(SolrUtils.serializeDateRange(q.getDeletedAfter(), q.getDeletedBefore()));
    }
    if (!q.isIncludeDeleted()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_DELETED + ":false");
    }

    if (q.isOnlyLastVersion()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_LATEST_VERSION + ":true");
    }

    if (q.getArchivedAfter().isSome() || q.getArchivedBefore().isSome()) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append(Schema.OC_TIMESTAMP).append(":")
              .append(SolrUtils.serializeDateRange(q.getArchivedAfter(), q.getArchivedBefore()));
    }

    if (sb.length() == 0)
      sb.append("*:*");

    final SolrQuery solr = new SolrQuery(sb.toString());
    // limit & offset
    solr.setRows(q.getLimit().getOrElse(Integer.MAX_VALUE));
    solr.setStart(q.getOffset().getOrElse(0));

    // ordering
    for (OpencastQuery.Order o : q.getOrder()) {
      final SolrQuery.ORDER order = q.isOrderAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
      solr.addSortField(getSortField(o), order);
    }

    solr.setFields("* score");
    return solr;
  }

  private static String getSortField(OpencastQuery.Order order) {
    switch (order) {
      case Title:
        return Schema.DC_TITLE_SORT;
      case Created:
        return Schema.DC_CREATED;
      case Creator:
        return Schema.DC_CREATOR_SORT;
      case SeriesTitle:
        return Schema.S_DC_TITLE_SORT;
      default:
        return Schema.DC_CREATED;
    }
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
   * Return the value with undefined language or the first available value if there is no such value or none if there
   * are no values.
   */
  private static Option<String> getLangUndefOrFirst(final List<DField<String>> fields) {
    return mlist(fields).find(isLanguageUndefined).map(getValue).orElse(Schema.<String> getFirst().curry(fields));
  }

  private static Function<DField<String>, Boolean> isLanguageUndefined = new Predicate<DField<String>>() {
    @Override
    public Boolean apply(DField<String> f) {
      return f.getSuffix().equals(Schema.LANGUAGE_UNDEFINED);
    }
  };

  private static Function<DField<String>, String> getValue = new Function<DField<String>, String>() {
    @Override
    public String apply(DField<String> f) {
      return f.getValue();
    }
  };
}
