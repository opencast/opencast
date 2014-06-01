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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Properties;

/**
 * This feed generator creates a feed for the episodes returned by the query specified by the service property
 * <code>feed.query</code>. Additional arguments will be passed to the query by means of
 * {@link MessageFormat#format(String, Object...)}.
 * <p>
 * The service will answer requests matching the service property <code>feed.selector</code> as the first query argument
 * passed to the {@link #accept(String[])} method.
 */
public class CustomFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(CustomFeedService.class);

  /** Property key for the query */
  private static final String PROP_QUERY = "feed.query";

  /** The solr query */
  private String solrQuery = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    if (solrQuery == null) {
      logger.warn("{} denies to handle request for {} since query is still undefined", this, query);
      return false;
    }
    return super.accept(query);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String[] query, int limit, int offset) {
    try {
      String q = solrQuery;
      if (query != null && query.length > 1) {
        Object[] args = new Object[query.length - 1];
        for (int i = 1; i < query.length; i++)
          args[i - 1] = query[i];
        q = MessageFormat.format(solrQuery, args);
      }

      // Make sure there are no remaining arguments
      if (q.matches(".*\\{[\\d]+\\}.*")) {
        logger.warn("Feed has been called with an insufficient number of parameters");
        return null;
      }

      // Create the query
      SearchQuery searchQuery = createBaseQuery(type, limit, offset);
      searchQuery.includeEpisodes(true);
      searchQuery.includeSeries(false);
      searchQuery.withQuery(q);
      searchService.getByQuery(searchQuery);

      return searchService.getByQuery(searchQuery);
    } catch (Exception e) {
      logger.error("Cannot retrieve result for aggregated feed", e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedService#initialize(java.util.Properties)
   */
  @Override
  public void initialize(Properties properties) {
    String query = (String) properties.get(PROP_QUERY);
    if (query != null && !"".equals(query)) {
      solrQuery = query;
      logger.debug("Configuring custom feed with query '{}'", query);
    }
    super.initialize(properties);
    // Clear the selector, since super.accept() relies on the fact that it's not set
    selector = null;
  }

}
