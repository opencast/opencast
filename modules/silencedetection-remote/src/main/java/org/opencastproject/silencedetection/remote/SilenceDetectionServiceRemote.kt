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

package org.opencastproject.silencedetection.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.api.SilenceDetectionService

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays

/**
 * Silence dedection service proxy for use as a JVM local service.
 */
class SilenceDetectionServiceRemote : RemoteBase(SilenceDetectionService.JOB_TYPE), SilenceDetectionService {

    @Throws(SilenceDetectionFailedException::class)
    override fun detect(sourceTrack: Track): Job {
        return detect(sourceTrack, null)
    }

    @Throws(SilenceDetectionFailedException::class)
    override fun detect(sourceTrack: Track, referencedTracks: Array<Track>?): Job {
        val post = HttpPost("/detect")
        val params = ArrayList<BasicNameValuePair>()
        try {
            params.add(BasicNameValuePair("track", MediaPackageElementParser.getAsXml(sourceTrack)))
            if (referencedTracks != null && referencedTracks.size > 0) {
                val referencedTracksXml = MediaPackageElementParser.getArrayAsXml(Arrays.asList(*referencedTracks))
                params.add(BasicNameValuePair("referenceTracks", referencedTracksXml))
            }
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw SilenceDetectionFailedException(
                    "Unable to assemble a remote silence detection request for track " + sourceTrack.identifier)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val entity = EntityUtils.toString(response.entity)
                if (StringUtils.isNotEmpty(entity)) {
                    val resultJob = JobParser.parseJob(entity)
                    logger.info(
                            "Start silence detection for track '{}' on remote silence detection service",
                            sourceTrack.identifier)
                    return resultJob
                }
            }
        } catch (e: Exception) {
            throw SilenceDetectionFailedException("Unable to run silence detection for track "
                    + sourceTrack.identifier + " on remote silence detection service", e)
        } finally {
            closeConnection(response)
        }
        throw SilenceDetectionFailedException("Unable to run silence detection for track "
                + sourceTrack.identifier + " on remote silence detection service")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SilenceDetectionServiceRemote::class.java)
    }
}
