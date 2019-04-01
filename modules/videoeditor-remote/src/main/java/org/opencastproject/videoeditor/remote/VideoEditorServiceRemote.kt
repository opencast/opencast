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

package org.opencastproject.videoeditor.remote

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.videoeditor.api.ProcessFailedException
import org.opencastproject.videoeditor.api.VideoEditorService

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.LinkedList

/**
 * Video editor service proxy for use as a JVM local service.
 */
class VideoEditorServiceRemote : RemoteBase(JOB_TYPE), VideoEditorService {

    @Throws(ProcessFailedException::class)
    override fun processSmil(smil: Smil): List<Job> {
        val post = HttpPost("/process-smil")
        val params = ArrayList<BasicNameValuePair>()
        try {
            params.add(BasicNameValuePair("smil", smil.toXML()))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw ProcessFailedException(
                    "Unable to assemble a remote videoeditor request for smil " + smil.id)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val entity = EntityUtils.toString(response.entity)
                if (StringUtils.isNotEmpty(entity)) {
                    val jobs = LinkedList<Job>()
                    for (job in JobParser.parseJobList(entity).getJobs()) {
                        jobs.add(job.toJob())
                    }
                    logger.info(
                            "Start proccessing smil '{}' on remote videoeditor service", smil.id)
                    return jobs
                }
            }
        } catch (e: Exception) {
            throw ProcessFailedException("Unable to proccess smil "
                    + smil.id + " using a remote videoeditor service", e)
        } finally {
            closeConnection(response)
        }
        throw ProcessFailedException("Unable to proccess smil "
                + smil.id + " using a remote videoeditor service.")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VideoEditorServiceRemote::class.java)
    }
}

