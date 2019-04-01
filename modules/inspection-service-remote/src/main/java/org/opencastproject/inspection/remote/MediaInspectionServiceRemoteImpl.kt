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

package org.opencastproject.inspection.remote

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.inspection.api.util.Options
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.ArrayList

/**
 * Proxies a remote media inspection service for use as a JVM-local service.
 */
/**
 * Constructs a new remote media inspection service proxy
 */
class MediaInspectionServiceRemoteImpl : RemoteBase(JOB_TYPE), MediaInspectionService {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.inspect
     */
    @Throws(MediaInspectionException::class)
    override fun inspect(uri: URI): Job {
        return inspect(uri, Options.NO_OPTION)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.inspection.api.MediaInspectionService.inspect
     */
    @Throws(MediaInspectionException::class)
    override fun inspect(uri: URI, options: Map<String, String>): Job {
        assert(options != null)
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("uri", uri.toString()))
        params.add(BasicNameValuePair("options", Options.toJson(options)))
        val url = "/inspect?" + URLEncodedUtils.format(params, "UTF-8")
        logger.info("Inspecting media file at {} using a remote media inspection service", uri)
        var response: HttpResponse? = null
        try {
            val get = HttpGet(url)
            response = getResponse(get)
            if (response != null) {
                val job = JobParser.parseJob(response.entity.content)
                logger.info("Completing inspection of media file at {} using a remote media inspection service", uri)
                return job
            }
        } catch (e: Exception) {
            throw MediaInspectionException("Unable to inspect $uri using a remote inspection service", e)
        } finally {
            closeConnection(response)
        }
        throw MediaInspectionException("Unable to inspect $uri using a remote inspection service")
    }

    /**
     * {@inheritDoc}
     */
    @Throws(MediaInspectionException::class)
    override fun enrich(original: MediaPackageElement, override: Boolean): Job {
        return enrich(original, override, Options.NO_OPTION)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(MediaInspectionException::class)
    override fun enrich(original: MediaPackageElement, override: Boolean, options: Map<String, String>): Job {
        assert(options != null)
        val params = ArrayList<NameValuePair>()
        try {
            params.add(BasicNameValuePair("mediaPackageElement", MediaPackageElementParser.getAsXml(original)))
            params.add(BasicNameValuePair("override", override.toString()))
            params.add(BasicNameValuePair("options", Options.toJson(options)))
        } catch (e: Exception) {
            throw MediaInspectionException(e)
        }

        logger.info("Enriching {} using a remote media inspection service", original)
        var response: HttpResponse? = null
        try {
            val post = HttpPost("/enrich")
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
            response = getResponse(post)
            if (response != null) {
                val receipt = JobParser.parseJob(response.entity.content)
                logger.info("Completing inspection of media file at {} using a remote media inspection service",
                        original.getURI())
                return receipt
            }
        } catch (e: Exception) {
            throw MediaInspectionException("Unable to enrich $original using a remote inspection service", e)
        } finally {
            closeConnection(response)
        }
        throw MediaInspectionException("Unable to enrich $original using a remote inspection service")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(MediaInspectionServiceRemoteImpl::class.java)
    }

}
