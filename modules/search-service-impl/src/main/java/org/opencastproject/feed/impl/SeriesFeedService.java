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
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.search.api.MediaSegment;
import org.opencastproject.search.api.MediaSegmentImpl;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItemImpl;
import org.opencastproject.util.data.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * This feed generator implements a feed for series. The series argument is taken from the first url parameter after the
 * feed type and version, and {@link #accept(String[])} returns <code>true</code> if the search service returns a result
 * for that series identifier.
 */
public class SeriesFeedService extends AbstractFeedService implements FeedGenerator {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesFeedService.class);

  /** The series identifier */
  protected ThreadLocal<String> series = new ThreadLocal<String>();

  /** The series data */
  protected ThreadLocal<SearchResult> seriesData = new ThreadLocal<SearchResult>();

  /** Number of milliseconds to cache the recordings (1h) */
  private static final long SERIES_CACHE_TIME = 60L * 60L * 1000L;

  /** A token to store in the miss cache */
  private Object nullToken = new Object();

  private final CacheLoader<String, Object> seriesLoader = new CacheLoader<String, Object>() {
    @Override
    public Object load(String id) {
      SearchResult result = loadSeries.apply(id);
      return result == null ? nullToken : result;
    }
  };

  /** The series metadata, cached for up to the indicated amount of time */
  private final LoadingCache<String, Object> seriesCache = CacheBuilder.newBuilder()
          .expireAfterWrite(SERIES_CACHE_TIME, TimeUnit.MILLISECONDS).maximumSize(500).build(seriesLoader);

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  @Override
  public boolean accept(String[] query) {
    boolean generalChecksPassed = super.accept(query);
    if (!generalChecksPassed) {
      return false;
    }

    // Build the series id, first parameter is the selector. Note that if the series identifier
    // contained slashes (e. g. in the case of a handle or doi), we need to reassemble the
    // identifier
    StringBuffer sId = new StringBuffer();
    int idparts = query.length - 1;
    if (idparts < 1) {
      return false;
    }
    for (int i = 1; i <= idparts; i++) {
      if (sId.length() > 0) {
        sId.append("/");
      }
      sId.append(query[i]);
    }

    // Remember the series id
    final String seriesId = sId.toString();
    series.set(seriesId);

    try {
      // To check if we can accept the query it is enough to query for just one result
      // Check the series service to see if the series exists
      // but has not yet had anything published from it
      Object result = seriesCache.getUnchecked(seriesId);
      if (result == nullToken) {
        return false;
      }

      SearchResult searchResult = (SearchResult) result;
      seriesData.set(searchResult);
      return searchResult.size() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getIdentifier()
   */
  @Override
  public String getIdentifier() {
    return series.get() != null ? series.get() : super.getIdentifier();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getName()
   */
  @Override
  public String getName() {
    SearchResult rs = seriesData.get();
    return (rs != null) ? rs.getItems()[0].getDcTitle() : super.getName();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getDescription()
   */
  @Override
  public String getDescription() {
    String dcAbstract = null;
    SearchResult rs = seriesData.get();
    if (rs != null) {
      dcAbstract = rs.getItems()[0].getDcAbstract();
    }
    return dcAbstract == null ? super.getDescription() : dcAbstract;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  @Override
  protected SearchResult loadFeedData(Type type, String[] query, int limit, int offset) {
    SearchQuery q = createBaseQuery(type, limit, offset);
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withSeriesId(series.get());
    return searchService.getByQuery(q);
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

  /**
   * Find if a series exists in the seriesService and return it's dublinCore as a search result. This call should be
   * used when searchService returns null as a series may exist but not have had any episodes yet published.
   *
   * @param id
   *          the series to lookup
   * @return search result, null if series not found
   */
  private final Function<String, SearchResult> loadSeries = new Function<String, SearchResult>() {

    @Override
    public SearchResult apply(String id) {

      // Try to look up the series from the search service
      SearchQuery q = new SearchQuery();
      q.includeEpisodes(false);
      q.includeSeries(true);
      q.withId(id);
      SearchResult result = searchService.getByQuery(q);
      if (result.getItems().length > 0) {
        logger.trace("Metadata for series {} loaded from search service", id);
        return result;
      }

      // There is nothing in the search index, let's ask the series service
      try {

        logger.debug("Loading metadata for series {} from series service", id);

        final DublinCoreCatalog seriesDublinCore = seriesService.getSeries(id);
        SearchResultImpl artificialResult = new SearchResultImpl();

        // Response either finds the one series or nothing at all
        artificialResult.setLimit(1);
        artificialResult.setOffset(0);
        artificialResult.setTotal(1);

        SearchResultItemImpl item = SearchResultItemImpl.fill(new SearchResultItem() {

          @Override
          public String getId() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_IDENTIFIER);
          }

          @Override
          public String getOrganization() {
            return null;
          }

          @Override
          public MediaPackage getMediaPackage() {
            return null;
          }

          @Override
          public long getDcExtent() {
            return -1;
          }

          @Override
          public String getDcTitle() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE);
          }

          @Override
          public String getDcSubject() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_SUBJECT);
          }

          @Override
          public String getDcDescription() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_DESCRIPTION);
          }

          @Override
          public String getDcCreator() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_CREATOR);
          }

          @Override
          public String getDcPublisher() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_PUBLISHER);
          }

          @Override
          public String getDcContributor() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_CONTRIBUTOR);
          }

          @Override
          public String getDcAbstract() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_ABSTRACT);
          }

          @Override
          public Date getDcCreated() {
            String date = seriesDublinCore.getFirst(DublinCore.PROPERTY_CREATED);
            if (date != null) {
              return EncodingSchemeUtils.decodeDate(date);
            }

            return null;
          }

          @Override
          public Date getDcAvailableFrom() {
            return null;
          }

          @Override
          public Date getDcAvailableTo() {
            return null;
          }

          @Override
          public String getDcLanguage() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_LANGUAGE);
          }

          @Override
          public String getDcRightsHolder() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER);
          }

          @Override
          public String getDcSpatial() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_SPATIAL);
          }

          @Override
          public String getDcTemporal() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_TEMPORAL);
          }

          @Override
          public String getDcIsPartOf() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_IS_PART_OF);
          }

          @Override
          public String getDcReplaces() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_REPLACES);
          }

          @Override
          public String getDcType() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_TYPE);
          }

          @Override
          public String getDcAccessRights() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS);
          }

          @Override
          public String getDcLicense() {
            return seriesDublinCore.getFirst(DublinCore.PROPERTY_LICENSE);
          }

          @Override
          public String getOcMediapackage() {
            return null;
          }

          @Override
          public SearchResultItem.SearchResultItemType getType() {
            return SearchResultItemType.Series;
          }

          @Override
          public String[] getKeywords() {
            return new String[0];
          }

          @Override
          public String getCover() {
            return null;
          }

          @Override
          public Date getModified() {
            return null;
          }

          @Override
          public Date getDeletionDate() {
            return null;
          }

          @Override
          public double getScore() {
            return 0.0;
          }

          @Override
          public MediaSegment[] getSegments() {
            return new MediaSegmentImpl[0];
          }
        });

        artificialResult.addItem(item);
        return artificialResult;
      } catch (Exception e) {
        return null;
      }
    }
  };

}
