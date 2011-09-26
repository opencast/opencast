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
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.impl.solr.Schema;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * This feed generator creates a feed for the latest episodes across a set of series as specified by the service
 * property <code>feed.series</code>.
 * <p>
 * The service will answer requests matching the service property <code>feed.selector</code> as the first query argument
 * passed to the {@link #accept(String[])} method.
 */
public class AggregationFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(AggregationFeedService.class);

  /** Property key for the series */
  private static final String PROP_SERIES = "feed.series";

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
      return searchService.getByQuery(solrQuery, limit, offset);
    } catch (Exception e) {
      logger.error("Cannot retrieve result for aggregated feed", e);
      return null;
    }
  }

  /**
   * Sets the series that are to be aggregated when creating the feed.
   * 
   * @param series
   *          the series identifier
   */
  public void setSeries(String[] series) {
    if (series == null || series.length == 0)
      throw new IllegalArgumentException("Series cannot be null or empty");

    // Create the solr query for the series
    StringBuffer q = new StringBuffer();
    q.append(Schema.DC_IS_PART_OF);
    q.append(":(");
    for (String s : series) {
      q.append(s).append(" ");
    }
    q.append(")");

    // Store the query
    solrQuery = q.toString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#initialize(java.util.Properties)
   */
  @Override
  public void initialize(Properties properties) {
    String series = (String) properties.get(PROP_SERIES);
    if (series != null && !"".equals(series)) {
      String[] seriesIds = series.split(",");
      for (int i = 0; i < seriesIds.length; i++) {
        seriesIds[i] = StringUtils.trim(seriesIds[i]);
      }
      setSeries(seriesIds);
      logger.debug("Configuring aggregation feed with series {}", series);
    }
    super.initialize(properties);
  }
}
