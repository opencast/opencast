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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Convenience implementation that is intended to serve as a base implementation for feed generator services. It handles
 * service activation, reads a default set of properties (see below) and can be configured to track the opencast
 * {@link SearchService} by using {@link #setSearchService(SearchService)}.
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

  /** Property key for the feed uri */
  public static final String PROP_URI = "feed.uri";

  /** Property key for the number of feed entries */
  public static final String PROP_SIZE = "feed.size";

  /** Property key for the feed selector pattern */
  public static final String PROP_SELECTOR = "feed.selector";

  /** Property key for the feed name */
  public static final String PROP_NAME = "feed.name";

  /** Property key for the feed description */
  public static final String PROP_DESCRIPTION = "feed.description";

  /** Property key for the feed copyright note */
  public static final String PROP_COPYRIGHT = "feed.copyright";

  /** Property key for the feed home url */
  public static final String PROP_HOME = "feed.home";

  /** Property key for the feed cover url */
  public static final String PROP_COVER = "feed.cover";

  /** Property key for the feed entry link template */
  public static final String PROP_ENTRY = "feed.entry";
  
  /** Property key for the feed entry rel=self link template */
  public static final String PROP_SELF = "feed.self";

  /** Property key for the feed rss media element flavor */
  public static final String PROP_RSSFLAVORS = "feed.rssflavors";

  /** Property key for the feed atom media element flavor */
  public static final String PROP_ATOMFLAVORS = "feed.atomflavors";

  /** Property key for the feed rss media element flavor */
  public static final String PROP_RSSTAGS = "feed.rsstags";

  /** Property key for the feed rss media type */
  public static final String PROP_RSS_MEDIA_TYPE = "feed.rssmediatype";

  /** Property key for the feed atom media element flavor */
  public static final String PROP_ATOMTAGS = "feed.atomtags";

  /** The selector used to match urls */
  protected String selector = null;

  /** The search service */
  protected SearchService searchService = null;

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
    if (searchService == null) {
      logger.warn("{} denies to handle request for {} due to missing search service", this, query);
      return false;
    } else if (uri == null) {
      logger.warn("{} denies to handle request for {} since no uri is defined", this);
      return false;
    } else if (query.length == 0) {
      logger.debug("{} denies to handle unknown request", this);
      return false;
    }
    
    //truncate uri, as it had to be and real uri not an id
    
    String id = extractId(uri);
    
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
	if (id == null) return uri;
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
    serverUrl = (String) properties.get("org.opencastproject.engage.ui.url");
    if (serverUrl == null) serverUrl = (String) properties.get("org.opencastproject.server.url"); 
    
    uri = generateFeedUri((String) properties.get(PROP_URI)); 
    

    String sizeAsString = (String) properties.get(PROP_SIZE);
    try {
      if (StringUtils.isNotBlank(sizeAsString)) {
        size = Integer.parseInt(sizeAsString);
        if (size == 0)
          size = Integer.MAX_VALUE;
      }
    } catch (NumberFormatException e) {
      logger.warn("Unable to set the size of the feed to {}", sizeAsString);
    }
    selector = (String) properties.get(PROP_SELECTOR);
    name = (String) properties.get(PROP_NAME);
    description = (String) properties.get(PROP_DESCRIPTION);
    copyright = (String) properties.get(PROP_COPYRIGHT);
    home = ensureUrl(((String) properties.get(PROP_HOME)), serverUrl);
    // feed.cover can be unset if no branding is required
    if (StringUtils.isBlank((String)properties.get(PROP_COVER))) {
      cover = null;
    } else {
      cover = ensureUrl((String) properties.get(PROP_COVER), serverUrl);
    }
    linkTemplate = ensureUrl((String) properties.get(PROP_ENTRY), serverUrl);
    if (properties.get(PROP_SELF) != null) 
       linkSelf = ensureUrl((String) properties.get(PROP_SELF), serverUrl);
    String rssFlavors = (String) properties.get(PROP_RSSFLAVORS);
    if (rssFlavors != null) {
      StringTokenizer tok = new StringTokenizer(rssFlavors, " ,;");
      while (tok.hasMoreTokens()) {
        addRssTrackFlavor(MediaPackageElementFlavor.parseFlavor(tok.nextToken()));
      }
    }
    String rssMediaTypes = (String) properties.get(PROP_RSS_MEDIA_TYPE);
    if (rssFlavors == null) {
      this.rssMediaTypes.add(PROP_RSS_MEDIA_TYPE_DEFAULT);
    } else {
      StringTokenizer tok = new StringTokenizer(rssMediaTypes, " ,;");
      while (tok.hasMoreTokens()) {
        this.rssMediaTypes.add(tok.nextToken());
      }
    }
    String atomFlavors = (String) properties.get(PROP_ATOMFLAVORS);
    if (atomFlavors != null) {
      StringTokenizer tok = new StringTokenizer(atomFlavors, " ,;");
      while (tok.hasMoreTokens()) {
        addAtomTrackFlavor(MediaPackageElementFlavor.parseFlavor(tok.nextToken()));
      }
    }
    String rssTags = (String) properties.get(PROP_RSSTAGS);
    if (rssTags != null) {
      for (String tag : rssTags.split("\\W")) {
        addRSSTag(tag);
      }
    }
    String atomTags = (String) properties.get(PROP_ATOMTAGS);
    if (atomTags != null) {
      for (String tag : atomTags.split("\\W")) {
        addAtomTag(tag);
      }
    }
  }
  
  protected String generateFeedUri(String feedId) {
    return ensureUrl(feedId, serverUrl);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbstractFeedService) || selector == null)
      return super.equals(o);
    return super.equals(o) && selector.equals(((AbstractFeedService)o).selector);
  }
  
  /**
   * Ensures that this string is an absolute URL. If not, prepend the local serverUrl to the string.
   * 
   * @param string
   *          The absolute or relative URL
   * @param baseUrl
   *          The base URL to prepend
   * @return An absolute URL
   */
  protected String ensureUrl(String string, String baseUrl) {
    try {
      new URL(string);
      return string;
    } catch (MalformedURLException e) {
      if (baseUrl.endsWith("/") || string.startsWith("/")) return baseUrl + string;
      else return baseUrl + "/" + string;
    }
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

}
