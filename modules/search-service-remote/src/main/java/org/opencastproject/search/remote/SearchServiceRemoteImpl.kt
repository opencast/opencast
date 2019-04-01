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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.search.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.search.api.SearchException
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchResultImpl
import org.opencastproject.search.api.SearchService
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

/**
 * A proxy to a remote search service.
 */
class SearchServiceRemoteImpl : RemoteBase(SearchService.JOB_TYPE), SearchService {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.search.api.SearchService.add
     */
    @Throws(SearchException::class)
    override fun add(mediaPackage: MediaPackage): Job {
        val post = HttpPost("/add")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw SearchException("Unable to assemble a remote search request for mediapackage $mediaPackage", e)
        }

        val response = getResponse(post)
        try {
            if (response != null) {
                val job = JobParser.parseJob(response.entity.content)
                logger.info("Publishing mediapackage '{}' using a remote search service", mediaPackage.identifier)
                return job
            }
        } catch (e: Exception) {
            throw SearchException("Unable to publish " + mediaPackage.identifier + " using a remote search service",
                    e)
        } finally {
            closeConnection(response)
        }

        throw SearchException("Unable to publish " + mediaPackage.identifier + " using a remote search service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.search.api.SearchService.delete
     */
    @Throws(SearchException::class)
    override fun delete(mediaPackageId: String): Job {
        val del = HttpDelete(mediaPackageId)
        val response = getResponse(del)
        try {
            if (response != null) {
                val job = JobParser.parseJob(response.entity.content)
                logger.info("Removing mediapackage '{}' from a remote search service", mediaPackageId)
                return job
            }
        } catch (e: Exception) {
            throw SearchException("Unable to remove $mediaPackageId from a remote search service", e)
        } finally {
            closeConnection(response)
        }

        throw SearchException("Unable to remove $mediaPackageId from a remote search service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.search.api.SearchService.getByQuery
     */
    @Throws(SearchException::class)
    override fun getByQuery(q: SearchQuery): SearchResult {
        val get = HttpGet(getSearchUrl(q, false))
        val response = getResponse(get)
        try {
            if (response != null)
                return SearchResultImpl.valueOf(response.entity.content)
        } catch (e: Exception) {
            throw SearchException("Unable to parse results of a getByQuery request from remote search index: ", e)
        } finally {
            closeConnection(response)
        }
        throw SearchException("Unable to perform getByQuery from remote search index")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.search.api.SearchService.getForAdministrativeRead
     */
    @Throws(SearchException::class, UnauthorizedException::class)
    override fun getForAdministrativeRead(q: SearchQuery): SearchResult {
        val get = HttpGet(getSearchUrl(q, true))
        val response = getResponse(get)
        try {
            if (response != null)
                return SearchResultImpl.valueOf(response.entity.content)
        } catch (e: Exception) {
            throw SearchException(
                    "Unable to parse results of a getForAdministrativeRead request from remote search index: ", e)
        } finally {
            closeConnection(response)
        }
        throw SearchException("Unable to perform getForAdministrativeRead from remote search index")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.search.api.SearchService.getByQuery
     */
    @Throws(SearchException::class)
    override fun getByQuery(query: String, limit: Int, offset: Int): SearchResult {
        val queryStringParams = ArrayList<NameValuePair>()
        queryStringParams.add(BasicNameValuePair("q", query))
        queryStringParams.add(BasicNameValuePair("limit", Integer.toString(limit)))
        queryStringParams.add(BasicNameValuePair("offset", Integer.toString(offset)))
        queryStringParams.add(BasicNameValuePair("admin", java.lang.Boolean.TRUE.toString()))
        val get = HttpGet("/lucene.xml?" + URLEncodedUtils.format(queryStringParams, "UTF-8"))
        logger.debug("Sending remote query '{}'", get.requestLine.toString())
        val response = getResponse(get)
        try {
            if (response != null)
                return SearchResultImpl.valueOf(response.entity.content)
        } catch (e: Exception) {
            throw SearchException("Unable to parse getByQuery response from remote search index", e)
        } finally {
            closeConnection(response)
        }
        throw SearchException("Unable to perform getByQuery from remote search index")
    }

    /**
     * Builds the a search URL.
     *
     * @param q
     * the search query
     * @param admin
     * whether this is for an administrative read
     * @return the search URL
     */
    private fun getSearchUrl(q: SearchQuery, admin: Boolean): String {
        val url = StringBuilder()
        val queryStringParams = ArrayList<NameValuePair>()

        // MH-10216, Choose "/expisode.xml" endpoint when querying by mediapackage id (i.e. episode id ) to recieve full mp data
        if (q.id != null || q.seriesId != null || q.elementFlavors != null || q.elementTags != null) {
            url.append("/episode.xml?")

            if (q.seriesId != null)
                queryStringParams.add(BasicNameValuePair("sid", q.seriesId))

            if (q.elementFlavors != null) {
                for (f in q.elementFlavors) {
                    queryStringParams.add(BasicNameValuePair("flavor", f.toString()))
                }
            }

            if (q.elementTags != null) {
                for (t in q.elementTags) {
                    queryStringParams.add(BasicNameValuePair("tag", t))
                }
            }
        } else {
            url.append("/series.xml?")
            queryStringParams.add(BasicNameValuePair("series", java.lang.Boolean.toString(q.isIncludeSeries)))
            queryStringParams.add(BasicNameValuePair("episodes", java.lang.Boolean.toString(q.isIncludeEpisodes)))
        }

        // General query parameters
        if (q.text != null)
            queryStringParams.add(BasicNameValuePair("q", q.text))

        if (q.id != null)
            queryStringParams.add(BasicNameValuePair("id", q.id))

        if (admin) {
            queryStringParams.add(BasicNameValuePair("admin", java.lang.Boolean.TRUE.toString()))
        } else {
            queryStringParams.add(BasicNameValuePair("admin", java.lang.Boolean.FALSE.toString()))
        }

        queryStringParams.add(BasicNameValuePair("limit", Integer.toString(q.limit)))
        queryStringParams.add(BasicNameValuePair("offset", Integer.toString(q.offset)))

        url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"))
        return url.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchServiceRemoteImpl::class.java)
    }

}
