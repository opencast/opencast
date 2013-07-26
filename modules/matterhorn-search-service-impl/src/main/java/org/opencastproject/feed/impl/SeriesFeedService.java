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
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.search.api.MediaSegment;
import org.opencastproject.search.api.MediaSegmentImpl;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItemImpl;

import java.util.Properties;
import java.util.Date;


/**
 * This feed generator implements a feed for series. The series argument is taken from the first url parameter after the
 * feed type and version, and {@link #accept(String[])} returns <code>true</code> if the search service returns a result
 * for that series identifier.
 */
public class SeriesFeedService extends AbstractFeedService implements FeedGenerator {

  /** The series identifier */
  protected ThreadLocal<String> series = new ThreadLocal<String>();

  /** The series data */
  protected ThreadLocal<SearchResult> seriesData = new ThreadLocal<SearchResult>();

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    boolean generalChecksPassed = super.accept(query);
    if (!generalChecksPassed)
      return false;

    // Build the series id, first parameter is the selector. Note that if the series identifier
    // contained slashes (e. g. in the case of a handle or doi), we need to reassemble the
    // identifier
    StringBuffer id = new StringBuffer();
    int idparts = query.length - 1;
    if (idparts < 1)
      return false;
    for (int i = 1; i <= idparts; i++) {
      if (id.length() > 0)
        id.append("/");
      id.append(query[i]);
    }

    try {
      // To check if we can accept the query it is enough to query for just one result
      SearchQuery q = new SearchQuery();
      q.includeEpisodes(true);
      q.includeSeries(true);
      q.withId(id.toString());
      q.withLimit(size);
      q.withCreationDateSort(true);
      SearchResult result = searchService.getByQuery(q);
      if (result != null && result.size() > 0) {
        series.set(id.toString());
        seriesData.set(result);
        return true;
      } else {
        // Check the series service to see if the series exists
        // but has not yet had anything published from it
        result = findSeries(id.toString());
        if (result != null && result.size() > 0) {
          series.set(id.toString());
          seriesData.set(result);
          return true;
        }
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getIdentifier()
   */
  public String getIdentifier() {
    return series.get() != null ? series.get() : super.getIdentifier();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getName()
   */
  public String getName() {
    SearchResult rs = seriesData.get();
    return (rs != null) ? rs.getItems()[0].getDcTitle() : super.getName();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getDescription()
   */
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
  protected SearchResult loadFeedData(Type type, String[] query, int limit, int offset) {
    return seriesData.get();
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
    selector = null;
  }
  
  /**
   * Find if a series exists in the seriesService and return it's dublinCore as a 
   * search result. This call should be used when searchService returns null as a
   * series may exist but not have had any episodes yet published.
   * 
   * @param id
   *          the series to lookup
   * @return search result, null if series not found 
   */
  private SearchResult findSeries(String id) {
    try {
      final DublinCoreCatalog seriesDublinCore = seriesService.getSeries(id);
      SearchResultImpl result = new SearchResultImpl();
      
      // Response either finds the one series or nothing at all
      result.setLimit(1);
      result.setOffset(0);
      result.setTotal(1);

      SearchResultItemImpl item = new SearchResultItemImpl().fill(new SearchResultItem() {
        private final String dfltString = null;

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
        public double getScore() {
          return 0.0;
        }

        @Override
        public MediaSegment[] getSegments() {
          return new MediaSegmentImpl[0];
        }
      });


      result.addItem(item);
      return result;
    } catch (Exception e) {
      return null;
    }
  }
}
