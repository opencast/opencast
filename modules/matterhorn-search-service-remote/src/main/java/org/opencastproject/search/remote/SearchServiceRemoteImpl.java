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

package org.opencastproject.search.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy to a remote search service.
 */
public class SearchServiceRemoteImpl extends RemoteBase implements SearchService {
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceRemoteImpl.class);

  public SearchServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public Job add(MediaPackage mediaPackage) throws SearchException {
    HttpPost post = new HttpPost("/add");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new SearchException("Unable to assemble a remote search request for mediapackage " + mediaPackage, e);
    }

    HttpResponse response = getResponse(post);
    try {
      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Publishing mediapackage '{}' using a remote search service", mediaPackage.getIdentifier());
        return job;
      }
    } catch (Exception e) {
      throw new SearchException("Unable to publish " + mediaPackage.getIdentifier() + " using a remote search service",
              e);
    } finally {
      closeConnection(response);
    }

    throw new SearchException("Unable to publish " + mediaPackage.getIdentifier() + " using a remote search service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  @Override
  public Job delete(String mediaPackageId) throws SearchException {
    HttpDelete del = new HttpDelete(mediaPackageId);
    HttpResponse response = getResponse(del);
    try {
      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Removing mediapackage '{}' from a remote search service", mediaPackageId);
        return job;
      }
    } catch (Exception e) {
      throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search service", e);
    } finally {
      closeConnection(response);
    }

    throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search service");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    HttpGet get = new HttpGet(getSearchUrl(q, false));
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return SearchResultImpl.valueOf(response.getEntity().getContent());
    } catch (Exception e) {
      throw new SearchException("Unable to parse results of a getByQuery request from remote search index: ", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getByQuery from remote search index");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#getForAdministrativeRead(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getForAdministrativeRead(SearchQuery q) throws SearchException, UnauthorizedException {
    HttpGet get = new HttpGet(getSearchUrl(q, true));
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return SearchResultImpl.valueOf(response.getEntity().getContent());
    } catch (Exception e) {
      throw new SearchException(
              "Unable to parse results of a getForAdministrativeRead request from remote search index: ", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getForAdministrativeRead from remote search index");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  @Override
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("q", query));
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(limit)));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(offset)));
    queryStringParams.add(new BasicNameValuePair("admin", Boolean.TRUE.toString()));
    HttpGet get = new HttpGet("/lucene.xml?" + URLEncodedUtils.format(queryStringParams, "UTF-8"));
    logger.debug("Sending remote query '{}'", get.getRequestLine().toString());
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return SearchResultImpl.valueOf(response.getEntity().getContent());
    } catch (Exception e) {
      throw new SearchException("Unable to parse getByQuery response from remote search index", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getByQuery from remote search index");
  }

  /**
   * Builds the a search URL.
   *
   * @param q
   *          the search query
   * @param admin
   *          whether this is for an administrative read
   * @return the search URL
   */
  private String getSearchUrl(SearchQuery q, boolean admin) {
    StringBuilder url = new StringBuilder();
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();

    // MH-10216, Choose "/expisode.xml" endpoint when querying by mediapackage id (i.e. episode id ) to recieve full mp data
    if (q.getId() != null || q.getSeriesId() != null || q.getElementFlavors() != null || q.getElementTags() != null) {
      url.append("/episode.xml?");

      if (q.getSeriesId() != null)
        queryStringParams.add(new BasicNameValuePair("sid", q.getSeriesId()));

      if (q.getElementFlavors() != null) {
        for (MediaPackageElementFlavor f : q.getElementFlavors()) {
          queryStringParams.add(new BasicNameValuePair("flavor", f.toString()));
        }
      }

      if (q.getElementTags() != null) {
        for (String t : q.getElementTags()) {
          queryStringParams.add(new BasicNameValuePair("tag", t));
        }
      }
    } else {
      url.append("/series.xml?");
      queryStringParams.add(new BasicNameValuePair("series", Boolean.toString(q.isIncludeSeries())));
      queryStringParams.add(new BasicNameValuePair("episodes", Boolean.toString(q.isIncludeEpisodes())));
    }

    // General query parameters
    if (q.getText() != null)
      queryStringParams.add(new BasicNameValuePair("q", q.getText()));

    if (q.getId() != null)
      queryStringParams.add(new BasicNameValuePair("id", q.getId()));

    if (admin) {
      queryStringParams.add(new BasicNameValuePair("admin", Boolean.TRUE.toString()));
    } else {
      queryStringParams.add(new BasicNameValuePair("admin", Boolean.FALSE.toString()));
    }

    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(q.getLimit())));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(q.getOffset())));

    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
    return url.toString();
  }

}
