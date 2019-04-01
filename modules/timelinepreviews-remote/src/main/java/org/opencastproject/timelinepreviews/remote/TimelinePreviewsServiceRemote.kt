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
package org.opencastproject.timelinepreviews.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService

import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList


/**
 * This is a remote timeline previews service that will call the timeline previews service implementation on a
 * remote host.
 */
/** The default constructor.  */
class TimelinePreviewsServiceRemote : RemoteBase(JOB_TYPE), TimelinePreviewsService {

    /**
     * Takes the given track and returns the job that will create timeline preview images using a remote service.
     *
     * @param sourceTrack the track to create preview images from
     * @param imageCount number of preview images that will be generated
     * @return a job that will create timeline preview images
     * @throws MediaPackageException if the serialization of the given track fails
     * @throws TimelinePreviewsException if the job can't be created for any reason
     */
    @Throws(MediaPackageException::class, TimelinePreviewsException::class)
    override fun createTimelinePreviewImages(sourceTrack: Track, imageCount: Int): Job {
        val post = HttpPost("/create")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("track", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("imageCount", Integer.toString(imageCount)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw TimelinePreviewsException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                try {
                    val receipt = JobParser.parseJob(response.entity.content)
                    logger.info("Create timeline preview images from {}", sourceTrack)
                    return receipt
                } catch (e: Exception) {
                    throw TimelinePreviewsException(
                            "Unable to create timeline preview images from $sourceTrack using a remote service", e)
                }

            }
        } finally {
            closeConnection(response)
        }
        throw TimelinePreviewsException("Unable to create timeline preview images from " + sourceTrack
                + " using a remote service")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimelinePreviewsServiceRemote::class.java)
    }

}
