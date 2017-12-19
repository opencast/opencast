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


package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * This feed generator creates a feed for the latest episodes across all series.
 */
public class LatestFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(LatestFeedService.class);

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String[] query, int limit, int offset) {
    logger.debug("Loading {} latest feed data starting from {}.", limit, offset);
    try {
      SearchQuery q = createBaseQuery(type, limit, offset);
      return searchService.getByQuery(q);
    } catch (Exception e) {
      logger.error("Cannot retrieve result for feed 'recent episodes'", e);
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
    super.initialize(properties);
    // Clear the selector, since super.accept() relies on the fact that it's not set
    setSelector(null);
  }

}
