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
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Convenience implementation that is intended to serve as a base implementation for feed generator services. It handles
 * service activation, reads a default set of properties (see below) and can be configured to track the opencast
 * {@link SearchService} by using {@link #setSearchService(SearchService)}, {@link SeriesService} by using
 * {@link #setSeriesService(SeriesService)} and {@link SecurityService} by using
 * {@link #setSecurityService(SecurityService)}.
 * <p>
 * By using this implementation as the basis for feed services, only the two methods accept and loadFeedData need to be
 * implemented by subclasses.
 * <p>
 * The following properties are being read from the component properties:
 * <ul>
 * <li><code>feed.uri</code> - the feed uri</li>
 * <li><code>feed.selector</code> the pattern that is used to determine if the feed implementation wants to handle a
 * request, e. g. the selector {{latest}} in {{http://<servername>/feeds/atom/0.3/latest}} maps the latest feed handler
 * to urls containing that selector</li>
 * <li><code>feed.name</code> - name of this feed</li>
 * <li><code>feed.description</code> - an abstract of this feed</li>
 * <li><code>feed.copyright</code> - the feed copyright note</li>
 * <li><code>feed.home</code> - url of the feed's home page</li>
 * <li><code>feed.cover</code> - url of the feed's cover image</li>
 * <li><code>feed.entry</code> - template to create a link to a feed entry</li>
 * <li><code>feed.rssflavor</code> - flavor identifying rss feed media package elements</li>
 * <li><code>feed.atomflavors</code> - comma separated list of flavors identifying atom feed media package elements</li>
 * <li><code>feed.rsstags</code> - tags identifying rss feed media package elements</li>
 * <li><code>feed.atomtags</code> - comma separated list of tags identifying atom feed media package elements</li>
 * </ul>
 */
public abstract class AbstractFeedService extends AbstractFeedGenerator {

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(AbstractFeedService.class);

  /** The selector used to match urls */
  private String selector = null;

  /** The search service */
  protected SearchService searchService = null;

  /** The search service */
  protected SeriesService seriesService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * Creates a new abstract feed generator.
   * <p>
   * <b>Note:</b> Subclasses using this constructor need to set required member variables prior to calling
   * {@link #createFeed(org.opencastproject.feed.api.Feed.Type, String[], int)} for the first time.
   */
  protected AbstractFeedService() {
    super();
  }

  /**
   * Creates a new abstract feed generator.
   *
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          the feed's home url
   * @param rssFlavors
   *          the flavors identifying rss tracks
   * @param atomFlavors
   *          the flavors identifying tracks to be included in atom feeds
   * @param entryLinkTemplate
   *          the link template
   */
  public AbstractFeedService(String uri, String feedHome, MediaPackageElementFlavor[] rssFlavors,
          String[] rssMediaTypes, MediaPackageElementFlavor[] atomFlavors, String entryLinkTemplate) {
    super(uri, feedHome, rssFlavors, rssMediaTypes, atomFlavors, entryLinkTemplate);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    String feedURI = getURI();

    if (searchService == null) {
      logger.warn("{} denies to handle request for {} due to missing search service", this, query);
      return false;
    } else if (feedURI == null) {
      logger.warn("{} denies to handle request for {} since no uri is defined", this);
      return false;
    } else if (query.length == 0) {
      logger.debug("{} denies to handle unknown request", this);
      return false;
    }

    // truncate uri, as it had to be and real uri not an id

    String id = extractId(feedURI);

    // Check the uri
    if (!query[0].equalsIgnoreCase(id)) {
      logger.debug("{} denies to handle request for {}", this, query);
      return false;
    }

    // Check the selector
    if (selector != null && (query.length < 2 || !query[1].equalsIgnoreCase(selector))) {
      return false;
    }

    logger.debug("{} accepts to handle request for {}", this, query);
    return true;
  }

  protected String extractId(String uri) {
    String id = uri.substring(uri.lastIndexOf("/") + 1);
    if (id == null)
      return uri;
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected abstract SearchResult loadFeedData(Type type, String[] query, int limit, int offset);

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#initialize(java.util.Properties)
   */
  @Override
  public void initialize(Properties properties) {
    super.initialize(properties);

    selector = (String) properties.get(PROP_SELECTOR);
  }

  /**
   * Returns the feed query.
   *
   * @return the query
   */
  protected String getSelector() {
    return selector;
  }

  /**
   * Sets the selector.
   *
   * @param selector
   *          the new selector
   */
  protected void setSelector(String selector) {
    this.selector = selector;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbstractFeedService) || selector == null)
      return super.equals(o);
    return super.equals(o) && selector.equals(((AbstractFeedService) o).selector);
  }

  /**
   * Sets the search service.
   *
   * @param searchService
   *          the search service
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * Returns the search service.
   *
   * @return the search services
   */
  protected SearchService getSearchService() {
    return searchService;
  }

  /**
   * Sets the series service.
   *
   * @param seriesService
   *          the series service
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Returns the series service.
   *
   * @return the series services
   */
  protected SeriesService getSeriesService() {
    return seriesService;
  }

  /**
   * Sets the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Returns the security service.
   *
   * @return the security services
   */
  protected SecurityService getSecurityService() {
    return securityService;
  }

}
