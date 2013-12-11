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

package org.opencastproject.feed.api;

import org.opencastproject.search.api.SearchService;
import org.opencastproject.series.api.SeriesService;

import java.util.Properties;

/**
 * A <code>FeedGenerator</code> is able to create an xml feed of the requested type, based on a query string.
 * <p>
 * The implementation must either return a valid feed node or <code>null</code> if it cannot satisfy the query.
 */
public interface FeedGenerator {

  /**
   * Returns the feed identifier.
   * 
   * @return the feed identifier
   */
  String getIdentifier();

  /**
   * Return the feed name.
   * 
   * @return the feed name
   */
  String getName();

  /**
   * Return the feed description
   */
  String getDescription();

  /**
   * Return the feed link.
   * 
   * @return the feed link
   */
  String getFeedLink();

  /**
   * Returns <code>true</code> if the generator is able to satisfy the request for a feed described by the query. The
   * query consists of all the elements that are found in the request, separated by a slash.
   * 
   * @return <code>true</code> if the generator can handle the query
   */
  boolean accept(String[] query);

  /**
   * Returns <code>null</code> if the generator cannot deal with the request. Otherwise it must returns a valid xml
   * feed.
   * 
   * @param type
   *          the feed type
   * @param query
   *          the request
   * @param size
   *          the requested size of the feed
   * @return the feed or <code>null</code>
   */
  Feed createFeed(Feed.Type type, String[] query, int size);

  /**
   * Returns the copyright for the feed.
   * 
   * @return the feed
   */
  String getCopyright();

  /**
   * Returns the url to the cover art.
   * 
   * @return the cover
   */
  String getCover();

  /**
   * Initializes the feed generator using the following properties:
   * <ul>
   * <li><code>feed.uri</code> - the feed uri</li>
   * <li><code>feed.selector</code> the pattern that is used to determine if the feed implementation wants to handle a
   * request, e. g. the selector {{latest}} in {{http://<servername>/feeds/atom/0.3/latest}} maps the latest feed
   * handler to urls containing that selector</li>
   * <li><code>feed.name</code> - name of this feed</li>
   * <li><code>feed.description</code> - an abstract of this feed</li>
   * <li><code>feed.copyright</code> - the feed copyright note</li>
   * <li><code>feed.home</code> - url of the feed's home page</li>
   * <li><code>feed.cover</code> - url of the feed's cover image</li>
   * <li><code>feed.entry</code> - template to create a link to a feed entry</li>
   * <li><code>feed.rssflavor</code> - media package flavor identifying rss feed media package elements</li>
   * <li><code>feed.atomflavors</code> - comma separated list of flavors identifying atom feed media package elements</li>
   * <li><code>feed.rsstags</code> - tags identifying rss feed media package elements</li>
   * <li><code>feed.atomtags</code> - comma separated list of tags identifying atom feed media package elements</li>
   * <li><code>org.opencastproject.server.url</code> - this server's base URL</li>
   * </ul>
   * 
   * @param properties
   *          used to initialize the feed
   */
  void initialize(Properties properties);

  /**
   * Sets the search service for this feed generator. FIXME: This shouldn't be exposed in the API, but must be present
   * for the FeedRegistrationScanner to function.
   * 
   * @param searchService
   *          The search service to use in finding data to expose in the feed
   */
  void setSearchService(SearchService searchService);

  /**
   * Sets the series service for this feed generator. FIXME: This shouldn't be exposed in the API, but must be present
   * for the FeedRegistrationScanner to function.
   * 
   * @param seriesService
   *          The series service to use in finding data to expose in the feed
   */
  void setSeriesService(SeriesService seriesService);
  
}
