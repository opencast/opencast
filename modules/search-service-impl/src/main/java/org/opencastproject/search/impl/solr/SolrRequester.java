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


package org.opencastproject.search.impl.solr;

import static org.opencastproject.security.api.Permissions.Action.READ;
import static org.opencastproject.security.api.Permissions.Action.WRITE;
import static org.opencastproject.util.data.Collections.filter;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.search.api.MediaSegment;
import org.opencastproject.search.api.MediaSegmentImpl;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.search.api.SearchResultItemImpl;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility.
 */
public class SolrRequester {

  private static final int QUERY_MAX_ROWS = 2000;

  /**
   * Logging facility
   */
  private static Logger logger = LoggerFactory.getLogger(SolrRequester.class);

  /**
   * The connection to the solr database
   */
  private SolrServer solrServer = null;

  /**
   * The security service
   */
  private SecurityService securityService;

  /**
   * The optional serializer
   */
  private MediaPackageSerializer serializer = null;

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   *
   * @param connection
   *          the solr connection
   * @param securityService
   *          the security service
   */
  public SolrRequester(SolrServer connection, SecurityService securityService) {
    this(connection, securityService, null);
  }

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   *
   * @param connection
   *          the solr connection
   * @param securityService
   *          the security service
   * @param serializer
   *          the optional mediapackage serializer
   */
  public SolrRequester(SolrServer connection, SecurityService securityService, MediaPackageSerializer serializer) {
    if (connection == null) {
      throw new IllegalStateException("Unable to run queries on null connection");
    }
    this.solrServer = connection;
    this.securityService = securityService;
    this.serializer = serializer;
  }

  /**
   * Returns the search results for a solr query string with read access for the current user.
   *
   * @param q
   *          the query
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @return the search results
   * @throws SolrServerException
   */
  public SearchResult getByQuery(String q, int limit, int offset) throws SolrServerException {
    SearchQuery q1 = new SearchQuery();
    q1.withQuery(q).withLimit(limit).withOffset(offset);
    return getForRead(q1);
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
  private SearchResult createSearchResult(final SolrQuery query, final boolean signed) throws SolrServerException {

    // Execute the query and try to get hold of a query response
    QueryResponse solrResponse = null;
    try {
      solrResponse = solrServer.query(query);
    } catch (Exception e) {
      throw new SolrServerException(e);
    }

    // Create and configure the query result
    final SearchResultImpl result = new SearchResultImpl(query.getQuery());
    result.setSearchTime(solrResponse.getQTime());
    result.setOffset(solrResponse.getResults().getStart());
    result.setLimit(solrResponse.getResults().size());
    result.setTotal(solrResponse.getResults().getNumFound());

    // Walk through response and create new items with title, creator, etc:
    for (final SolrDocument doc : solrResponse.getResults()) {
      final SearchResultItemImpl item = SearchResultItemImpl.fill(new SearchResultItem() {
        private final String dfltString = null;

        @Override
        public String getId() {
          return Schema.getId(doc);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.opencastproject.search.api.SearchResultItem#getOrganization()
         */
        @Override
        public String getOrganization() {
          return Schema.getOrganization(doc);
        }

        @Override
        public MediaPackage getMediaPackage() {
          MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
          if (signed && serializer != null) {
            builder.setSerializer(serializer);
          }
          String mediaPackageFieldValue = Schema.getOcMediapackage(doc);
          if (mediaPackageFieldValue != null) {
            try {
              return builder.loadFromXml(mediaPackageFieldValue);
            } catch (Exception e) {
              logger.warn("Unable to read media package from search result", e);
            }
          }
          return null;
        }

        @Override
        public long getDcExtent() {
          if (getType().equals(SearchResultItemType.AudioVisual)) {
            Long extent = Schema.getDcExtent(doc);
            if (extent != null) {
              return extent;
            }
          }
          return -1;
        }

        @Override
        public String getDcTitle() {
          final List<DField<String>> titles = Schema.getDcTitle(doc);
          // try to return the first title without any language information first...
          return head(filter(titles, new Predicate<DField<String>>() {
            @Override
            public Boolean apply(DField<String> f) {
              return f.getSuffix().equals(Schema.LANGUAGE_UNDEFINED);
            }
          })).map(new Function<DField<String>, String>() {
            @Override
            public String apply(DField<String> f) {
              return f.getValue();
            }
          }).getOrElse(new Function0<String>() {
            @Override
            public String apply() {
              // ... since none is present return the first arbitrary title
              return Schema.getFirst(titles, dfltString);
            }
          });
        }

        @Override
        public String getDcSubject() {
          return Schema.getFirst(Schema.getDcSubject(doc), dfltString);
        }

        @Override
        public String getDcDescription() {
          return Schema.getFirst(Schema.getDcDescription(doc), dfltString);
        }

        @Override
        public String getDcCreator() {
          return Schema.getFirst(Schema.getDcCreator(doc), dfltString);
        }

        @Override
        public String getDcPublisher() {
          return Schema.getFirst(Schema.getDcPublisher(doc), dfltString);
        }

        @Override
        public String getDcContributor() {
          return Schema.getFirst(Schema.getDcContributor(doc), dfltString);
        }

        @Override
        public String getDcAbstract() {
          return null;
        }

        @Override
        public Date getDcCreated() {
          return Schema.getDcCreated(doc);
        }

        @Override
        public Date getDcAvailableFrom() {
          return Schema.getDcAvailableFrom(doc);
        }

        @Override
        public Date getDcAvailableTo() {
          return Schema.getDcAvailableTo(doc);
        }

        @Override
        public String getDcLanguage() {
          return Schema.getDcLanguage(doc);
        }

        @Override
        public String getDcRightsHolder() {
          return Schema.getFirst(Schema.getDcRightsHolder(doc), dfltString);
        }

        @Override
        public String getDcSpatial() {
          return Schema.getFirst(Schema.getDcSpatial(doc), dfltString);
        }

        @Override
        public String getDcTemporal() {
          return null;
        }

        @Override
        public String getDcIsPartOf() {
          return Schema.getDcIsPartOf(doc);
        }

        @Override
        public String getDcReplaces() {
          return Schema.getDcReplaces(doc);
        }

        @Override
        public String getDcType() {
          return Schema.getDcType(doc);
        }

        @Override
        public String getDcAccessRights() {
          return Schema.getFirst(Schema.getDcAccessRights(doc), dfltString);
        }

        @Override
        public String getDcLicense() {
          return Schema.getFirst(Schema.getDcLicense(doc), dfltString);
        }

        @Override
        public String getOcMediapackage() {
          return Schema.getOcMediapackage(doc);
        }

        @Override
        public SearchResultItemType getType() {
          String t = Schema.getOcMediatype(doc);
          return t != null ? SearchResultItemType.valueOf(t) : null;
        }

        @Override
        public String[] getKeywords() {
          if (getType().equals(SearchResultItemType.AudioVisual)) {
            String k = Schema.getOcKeywords(doc);
            return k != null ? k.split(" ") : new String[0];
          } else {
            return new String[0];
          }
        }

        @Override
        public String getCover() {
          return Schema.getOcCover(doc);
        }

        @Override
        public Date getModified() {
          return Schema.getOcModified(doc);
        }

        @Override
        public Date getDeletionDate() {
          return Schema.getOcDeleted(doc);
        }

        @Override
        public double getScore() {
          return Schema.getScore(doc);
        }

        @Override
        public MediaSegment[] getSegments() {
          if (SearchResultItemType.AudioVisual.equals(getType())) {
            return createSearchResultSegments(doc, query).toArray(new MediaSegmentImpl[0]);
          } else {
            return new MediaSegmentImpl[0];
          }
        }
      });

      // Add the item to the result set
      result.addItem(item);
    }

    return result;
  }

  /**
   * Creates a list of <code>MediaSegment</code>s from the given result document.
   *
   * @param doc
   *          the result document
   * @param query
   *          the original query
   */
  private List<MediaSegmentImpl> createSearchResultSegments(SolrDocument doc, SolrQuery query) {
    List<MediaSegmentImpl> segments = new ArrayList<MediaSegmentImpl>();

    // The maximum number of hits in a segment
    int maxHits = 0;

    // Loop over every segment
    for (String fieldName : doc.getFieldNames()) {
      if (!fieldName.startsWith(Schema.SEGMENT_TEXT_PREFIX)) {
        continue;
      }

      // Ceate a new segment
      int segmentId = Integer.parseInt(fieldName.substring(Schema.SEGMENT_TEXT_PREFIX.length()));
      MediaSegmentImpl segment = new MediaSegmentImpl(segmentId);
      segment.setText(mkString(doc.getFieldValue(fieldName)));

      // Read the hints for this segment
      Properties segmentHints = new Properties();
      try {
        String hintFieldName = Schema.SEGMENT_HINT_PREFIX + segment.getIndex();
        Object hintFieldValue = doc.getFieldValue(hintFieldName);
        segmentHints.load(new ByteArrayInputStream(hintFieldValue.toString().getBytes()));
      } catch (IOException e) {
        logger.warn("Cannot load hint properties.");
      }

      // get segment time
      String segmentTime = segmentHints.getProperty("time");
      if (segmentTime == null) {
        throw new IllegalStateException("Found segment without time hint");
      }
      segment.setTime(Long.parseLong(segmentTime));

      // get segment duration
      String segmentDuration = segmentHints.getProperty("duration");
      if (segmentDuration == null) {
        throw new IllegalStateException("Found segment without duration hint");
      }
      segment.setDuration(Long.parseLong(segmentDuration));

      // get preview urls
      for (Entry<Object, Object> entry : segmentHints.entrySet()) {
        if (entry.getKey().toString().startsWith("preview.")) {
          String[] parts = entry.getKey().toString().split("\\.");
          segment.addPreview(entry.getValue().toString(), parts[1]);
        }
      }

      // calculate the segment's relevance with respect to the query
      String queryText = query.getQuery();
      String segmentText = segment.getText();
      if (!StringUtils.isBlank(queryText) && !StringUtils.isBlank(segmentText)) {
        segmentText = segmentText.toLowerCase();
        Pattern p = Pattern.compile(".*fulltext:\\(([^)]*)\\).*");
        Matcher m = p.matcher(queryText);
        if (m.matches()) {
          String[] queryTerms = StringUtils.split(m.group(1).toLowerCase());
          int segmentHits = 0;
          int textLength = segmentText.length();
          for (String t : queryTerms) {
            String strippedTerm = StringUtils.strip(t, "*");
            if (StringUtils.isBlank(strippedTerm)) {
              continue;
            }
            int startIndex = 0;
            while (startIndex < textLength - 1) {
              int foundAt = segmentText.indexOf(strippedTerm, startIndex);
              if (foundAt < 0) {
                break;
              }
              segmentHits++;
              startIndex = foundAt + strippedTerm.length();
            }
          }

          // for now, just store the number of hits, but keep track of the maximum hit count
          if (segmentHits > 0) {
            segment.setHit(true);
            segment.setRelevance(segmentHits);
          }
          if (segmentHits > maxHits) {
            maxHits = segmentHits;
          }
        }
      }

      segments.add(segment);
    }

    for (MediaSegmentImpl segment : segments) {
      int hitsInSegment = segment.getRelevance();
      if (hitsInSegment > 0) {
        segment.setRelevance((int) ((100 * hitsInSegment) / maxHits));
      }
    }

    return segments;
  }

  /**
   * Modifies the query such that certain fields are being boosted (meaning they gain some weight).
   *
   * @param query
   *          The user query.
   * @return The boosted query
   */
  public StringBuffer boost(String query) {
    String uq = SolrUtils.clean(query);
    StringBuffer sb = new StringBuffer();

    sb.append("(");

    sb.append(Schema.DC_TITLE_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_TITLE_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_CREATOR_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_CREATOR_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_SUBJECT_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_SUBJECT_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_PUBLISHER_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_PUBLISHER_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_CONTRIBUTOR_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_CONTRIBUTOR_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_ABSTRACT_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_ABSTRACT_BOOST);
    sb.append(" ");

    sb.append(Schema.DC_DESCRIPTION_PREFIX);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(Schema.DC_DESCRIPTION_BOOST);
    sb.append(" ");

    sb.append(Schema.FULLTEXT);
    sb.append(":(");
    sb.append(uq);
    sb.append(") ");

    // see http://wiki.apache.org/lucene-java/LuceneFAQ#Are_Wildcard.2C_Prefix.2C_and_Fuzzy_queries_case_sensitive.3F
    // for an explanation why .toLowerCase() is used here. This behaviour is tracked in SOLR-219.
    // It's also important not to stem when using wildcard queries. Please adjust the schema.xml accordingly.
    sb.append(Schema.FULLTEXT);
    sb.append(":(*");
    sb.append(uq.toLowerCase());
    sb.append("*) ");

    sb.append(")");

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
    if (f != null) {
      return f.toString();
    } else {
      return "";
    }
  }

  /**
   * Converts the query object into a solr query and returns the results.
   *
   * @param q
   *          the query
   * @param action
   *          one of {@link org.opencastproject.search.api.SearchService#READ_PERMISSION},
   *          {@link org.opencastproject.search.api.SearchService#WRITE_PERMISSION}
   * @param applyPermissions
   *          whether to apply the permissions to the query. Set to false for administrative queries.
   * @return the search results
   */
  private SolrQuery getForAction(SearchQuery q, String action, boolean applyPermissions) throws SolrServerException {
    StringBuilder sb = new StringBuilder();

    if (StringUtils.isNotBlank(q.getQuery())) {
      sb.append(q.getQuery());
    }

    String solrIdRequest = StringUtils.trimToNull(q.getId());
    if (solrIdRequest != null) {
      String cleanSolrIdRequest = SolrUtils.clean(solrIdRequest);
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("(");
      sb.append(Schema.ID);
      sb.append(":");
      sb.append(cleanSolrIdRequest);
      if (q.willIncludeEpisodes() && q.willIncludeSeries()) {
        sb.append(" OR ");
        sb.append(Schema.DC_IS_PART_OF);
        sb.append(":");
        sb.append(cleanSolrIdRequest);
      }
      sb.append(")");
    }

    String solrSeriesIdRequest = StringUtils.trimToNull(q.getSeriesId());
    if (solrSeriesIdRequest != null) {
      String cleanSolrSeriesIdRequest = SolrUtils.clean(solrSeriesIdRequest);
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("(");
      sb.append(Schema.DC_IS_PART_OF);
      sb.append(":");
      sb.append(cleanSolrSeriesIdRequest);
      sb.append(")");
    }

    String solrTextRequest = StringUtils.trimToNull(q.getText());
    if (solrTextRequest != null) {
      String cleanSolrTextRequest = SolrUtils.clean(q.getText());
      if (StringUtils.isNotEmpty(cleanSolrTextRequest)) {
        if (sb.length() > 0) {
          sb.append(" AND ");
        }
        sb.append("( *:");
        sb.append(boost(cleanSolrTextRequest));
        sb.append(" OR (");
        sb.append(Schema.ID);
        sb.append(":");
        sb.append(cleanSolrTextRequest);
        sb.append(") )");
      }
    }

    if (q.getElementTags() != null && q.getElementTags().length > 0) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      StringBuilder tagBuilder = new StringBuilder();
      for (int i = 0; i < q.getElementTags().length; i++) {
        String tag = SolrUtils.clean(q.getElementTags()[i]);
        if (StringUtils.isEmpty(tag)) {
          continue;
        }
        if (tagBuilder.length() == 0) {
          tagBuilder.append("(");
        } else {
          tagBuilder.append(" OR ");
        }
        tagBuilder.append(Schema.OC_ELEMENTTAGS);
        tagBuilder.append(":");
        tagBuilder.append(tag);
      }
      if (tagBuilder.length() > 0) {
        tagBuilder.append(") ");
        sb.append(tagBuilder);
      }
    }

    if (q.getElementFlavors() != null && q.getElementFlavors().length > 0) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      StringBuilder flavorBuilder = new StringBuilder();
      for (int i = 0; i < q.getElementFlavors().length; i++) {
        String flavor = SolrUtils.clean(q.getElementFlavors()[i].toString());
        if (StringUtils.isEmpty(flavor)) {
          continue;
        }
        if (flavorBuilder.length() == 0) {
          flavorBuilder.append("(");
        } else {
          flavorBuilder.append(" OR ");
        }
        flavorBuilder.append(Schema.OC_ELEMENTFLAVORS);
        flavorBuilder.append(":");
        flavorBuilder.append(flavor);
      }
      if (flavorBuilder.length() > 0) {
        flavorBuilder.append(") ");
        sb.append(flavorBuilder);
      }
    }

    if (q.getDeletedDate() != null) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(Schema.OC_DELETED + ":"
              + SolrUtils.serializeDateRange(option(q.getDeletedDate()), Option.none()));
    }

    if (q.getUpdatedSince() != null) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(Schema.OC_MODIFIED)
          .append(":")
          .append(SolrUtils.serializeDateRange(option(q.getUpdatedSince()), Option.none()));
    }

    if (sb.length() == 0) {
      sb.append("*:*");
    }

    if (applyPermissions) {
      sb.append(" AND ").append(Schema.OC_ORGANIZATION).append(":")
              .append(SolrUtils.clean(securityService.getOrganization().getId()));
      User user = securityService.getUser();
      Set<Role> roles = user.getRoles();
      boolean userHasAnonymousRole = false;
      if (roles.size() > 0) {
        sb.append(" AND (");
        StringBuilder roleList = new StringBuilder();
        for (Role role : roles) {
          if (roleList.length() > 0) {
            roleList.append(" OR ");
          }
          roleList.append(Schema.OC_ACL_PREFIX).append(action).append(":").append(SolrUtils.clean(role.getName()));
          if (role.getName().equalsIgnoreCase(securityService.getOrganization().getAnonymousRole())) {
            userHasAnonymousRole = true;
          }
        }
        if (!userHasAnonymousRole) {
          if (roleList.length() > 0) {
            roleList.append(" OR ");
          }
          roleList.append(Schema.OC_ACL_PREFIX).append(action).append(":")
                  .append(SolrUtils.clean(securityService.getOrganization().getAnonymousRole()));
        }

        sb.append(roleList.toString());
        sb.append(")");
      }
    }

    if (!q.willIncludeEpisodes()) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("-" + Schema.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual);
    }

    if (!q.willIncludeSeries()) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("-" + Schema.OC_MEDIATYPE + ":" + SearchResultItemType.Series);
    }

    if (!q.willIncludeDeleted()) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append("-" + Schema.OC_DELETED + ":[* TO *]");
    }

    SolrQuery query = new SolrQuery(sb.toString());

    if ((q.getLimit() > 0) && (q.getLimit() < QUERY_MAX_ROWS)) {
      query.setRows(q.getLimit());
    } else {
      query.setRows(QUERY_MAX_ROWS);
    }

    if (q.getOffset() > 0) {
      query.setStart(q.getOffset());
    }

    if (q.getSort() != null) {
      ORDER order = q.willSortAscending() ? ORDER.asc : ORDER.desc;
      query.addSortField(getSortField(q.getSort()), order);
    }

    if (!SearchQuery.Sort.DATE_CREATED.equals(q.getSort())) {
      query.addSortField(getSortField(SearchQuery.Sort.DATE_CREATED), ORDER.desc);
    }

    query.setFields("* score");
    return query;
  }

  /**
   * Returns the search results, regardless of permissions. This should be used for maintenance purposes only.
   *
   * @param q
   *          the search query
   * @return the readable search result
   * @throws SolrServerException
   */
  public SearchResult getForAdministrativeRead(SearchQuery q) throws SolrServerException {
    SolrQuery query = getForAction(q, READ.toString(), false);
    return createSearchResult(query, q.willSignURLs());
  }

  /**
   * Returns the search results that are accessible for read by the current user.
   *
   * @param q
   *          the search query
   * @return the readable search result
   * @throws SolrServerException
   */
  public SearchResult getForRead(SearchQuery q) throws SolrServerException {
    SolrQuery query = getForAction(q, READ.toString(), true);
    return createSearchResult(query, q.willSignURLs());
  }

  /**
   * Returns the search results that are accessible for write by the current user.
   *
   * @param q
   *          the search query
   * @return the writable search result
   * @throws SolrServerException
   */
  public SearchResult getForWrite(SearchQuery q) throws SolrServerException {
    SolrQuery query = getForAction(q, WRITE.toString(), true);
    return createSearchResult(query, q.willSignURLs());
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the optional Mediapackage Serializer.
   *
   * @param serializer
   *          the serializer
   */
  public void setMediaPackageSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * Returns the search index' field name that corresponds to the sort field.
   *
   * @param sort
   *          the sort field
   * @return the field name in the search index
   */
  protected String getSortField(SearchQuery.Sort sort) {
    switch (sort) {
      case TITLE:
        return Schema.DC_TITLE_SORT;
      case CONTRIBUTOR:
        return Schema.DC_CONTRIBUTOR_SORT;
      case DATE_CREATED:
        return Schema.DC_CREATED;
      case DATE_MODIFIED:
        return Schema.OC_MODIFIED;
      case CREATOR:
        return Schema.DC_CREATOR_SORT;
      case LANGUAGE:
        return Schema.DC_LANGUAGE;
      case LICENSE:
        return Schema.DC_LICENSE_SORT;
      case MEDIA_PACKAGE_ID:
        return Schema.ID;
      case SERIES_ID:
        return Schema.DC_IS_PART_OF;
      case SUBJECT:
        return Schema.DC_SUBJECT_SORT;
      case DESCRIPTION:
        return Schema.DC_DESCRIPTION_SORT;
      case PUBLISHER:
        return Schema.DC_PUBLISHER_SORT;
      default:
        throw new IllegalArgumentException("No mapping found between sort field and index");
    }
  }

}
