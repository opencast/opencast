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

package org.opencastproject.publication.youtube.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.publication.api.YouTubePublicationService
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

/**
 * A remote youtube service invoker.
 */
class YouTubePublicationServiceRemoteImpl : RemoteBase(JOB_TYPE), YouTubePublicationService {

    @Throws(PublicationException::class)
    override fun publish(mediaPackage: MediaPackage, track: Track): Job {
        val trackId = track.identifier
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
        params.add(BasicNameValuePair("elementId", trackId))
        val post = HttpPost()
        var response: HttpResponse? = null
        try {
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
            response = getResponse(post)
            if (response != null) {
                logger.info("Publishing {} to youtube", trackId)
                return JobParser.parseJob(response.entity.content)
            }
        } catch (e: Exception) {
            throw PublicationException("Unable to publish track " + trackId + " from mediapackage "
                    + mediaPackage + " using a remote youtube publication service", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException("Unable to publish track " + trackId + " from mediapackage "
                + mediaPackage + " using a remote youtube publication service")
    }

    @Throws(PublicationException::class)
    override fun retract(mediaPackage: MediaPackage): Job {
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
        val post = HttpPost("/retract")
        var response: HttpResponse? = null
        try {
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
            response = getResponse(post)
            if (response != null) {
                logger.info("Retracting {} from youtube", mediaPackage)
                return JobParser.parseJob(response.entity.content)
            }
        } catch (e: Exception) {
            throw PublicationException("Unable to retract mediapackage " + mediaPackage
                    + " using a remote youtube publication service", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException("Unable to retract mediapackage " + mediaPackage
                + " using a remote youtube publication service")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(YouTubePublicationServiceRemoteImpl::class.java)
    }

}
