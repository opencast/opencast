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
package org.opencastproject.waveform.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.waveform.api.WaveformService
import org.opencastproject.waveform.api.WaveformServiceException

import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList


/**
 * This is a remote waveform service that will call the waveform service implementation on a remote host.
 */
/** The default constructor.  */
class WaveformServiceRemote : RemoteBase(JOB_TYPE), WaveformService {

    /**
     * Takes the given track and returns the job that will create an waveform image using a remote service.
     *
     * @param sourceTrack the track to create waveform image from
     * @param pixelsPerMinute the width of the waveform image in pixels per minute
     * @param minWidth the minimum width of the waveform image
     * @param maxWidth the maximum width of the waveform image
     * @param height the height of the waveform image
     * @return a job that will create a waveform image
     * @throws MediaPackageException if the serialization of the given track fails
     * @throws WaveformServiceException if the job can't be created for any reason
     */
    @Throws(MediaPackageException::class, WaveformServiceException::class)
    override fun createWaveformImage(sourceTrack: Track, pixelsPerMinute: Int, minWidth: Int, maxWidth: Int, height: Int, color: String): Job {
        val post = HttpPost("/create")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("track", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("pixelsPerMinute", Integer.toString(pixelsPerMinute)))
            params.add(BasicNameValuePair("minWidth", Integer.toString(minWidth)))
            params.add(BasicNameValuePair("maxWidth", Integer.toString(maxWidth)))
            params.add(BasicNameValuePair("height", Integer.toString(height)))
            params.add(BasicNameValuePair("color", color))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw WaveformServiceException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                try {
                    val receipt = JobParser.parseJob(response.entity.content)
                    logger.info("Create waveform image from {}", sourceTrack)
                    return receipt
                } catch (e: Exception) {
                    throw WaveformServiceException(
                            "Unable to create waveform image from $sourceTrack using a remote service", e)
                }

            }
        } finally {
            closeConnection(response)
        }
        throw WaveformServiceException("Unable to create waveform image from $sourceTrack using a remote service")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WaveformServiceRemote::class.java)
    }

}
