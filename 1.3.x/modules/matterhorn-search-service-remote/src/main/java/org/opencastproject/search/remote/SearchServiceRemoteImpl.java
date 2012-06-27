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
package org.opencastproject.search.remote;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
  public void add(MediaPackage mediaPackage) throws SearchException {
    HttpPost post = new HttpPost("/add");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new SearchException("Unable to assemble a remote search request for mediapackage " + mediaPackage, e);
    }

    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT);
    if (response == null) {
      throw new SearchException("Unable to add mediapackage " + mediaPackage + " using the remote search services");
    } else {
      closeConnection(response);
    }
    logger.info("Successfully added {} to the search service", mediaPackage);
    return;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  @Override
  public boolean delete(String mediaPackageId) throws SearchException {
    HttpDelete del = new HttpDelete(mediaPackageId);
    HttpResponse response = null;
    try {
      response = getResponse(del, HttpStatus.SC_NO_CONTENT);
      if (response == null) {
        throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search index");
      }
      int status = response.getStatusLine().getStatusCode();
      if (status == HttpStatus.SC_NO_CONTENT) {
        logger.info("Successfully deleted {} from the remote search index", mediaPackageId);
        return true;
      } else if (status == HttpStatus.SC_NOT_FOUND) {
        logger.info("Mediapackage {} not found in remote search index", mediaPackageId);
        return false;
      } else {
        throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search index, http status="
                + status);
      }
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    HttpGet get = new HttpGet(getSearchUrl(q, false));
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        return SearchResultImpl.valueOf(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new SearchException("Unable to parse results of a getByQuery request from remote search index: ", e);
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
    if (!q.isIncludeEpisodes() && q.isIncludeSeries()) {
      url.append("/series?");
    } else if (q.isIncludeEpisodes() && !q.isIncludeSeries()) {
      url.append("/episode?");
    } else {
      url.append("/?");
    }

    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if (q.getText() != null) {
      queryStringParams.add(new BasicNameValuePair("q", q.getText()));
    }
    if (admin) {
      queryStringParams.add(new BasicNameValuePair("admin", Boolean.TRUE.toString()));
    }
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(q.getLimit())));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(q.getOffset())));
    
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));

    return url.toString();
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

    StringBuilder url = new StringBuilder();
    url.append("/lucene?");
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));

    HttpGet get = new HttpGet(url.toString());
    logger.debug("Sending remote query '{}'", get.getRequestLine().toString());
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        return SearchResultImpl.valueOf(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new SearchException("Unable to parse getByQuery response from remote search index", e);
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
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        return SearchResultImpl.valueOf(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new SearchException(
              "Unable to parse results of a getForAdministrativeRead request from remote search index: ", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getForAdministrativeRead from remote search index");
  }

}
