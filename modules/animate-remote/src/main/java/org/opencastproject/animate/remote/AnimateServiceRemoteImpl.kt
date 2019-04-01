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

package org.opencastproject.animate.remote

import org.opencastproject.animate.api.AnimateService
import org.opencastproject.animate.api.AnimateServiceException
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.serviceregistry.api.RemoteBase

import com.google.gson.Gson

import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.opencastproject.animate.api.AnimateService.Companion.JOB_TYPE
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI
import java.util.ArrayList

/** Create video animations using Synfig  */
/** Creates a new animate service instance.  */
class AnimateServiceRemoteImpl : RemoteBase(JOB_TYPE), AnimateService {

    @Throws(AnimateServiceException::class)
    override fun animate(animation: URI, metadata: Map<String, String>, options: List<String>): Job {

        // serialize arguments and metadata
        val metadataJson = gson.toJson(metadata)
        val optionJson = gson.toJson(options)

        // Build form parameters
        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("animation", animation.toString()))
        params.add(BasicNameValuePair("arguments", optionJson))
        params.add(BasicNameValuePair("metadata", metadataJson))

        logger.info("Animating {}", animation)
        var response: HttpResponse? = null
        try {
            val post = HttpPost("/animate")
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
            response = getResponse(post)
            if (response == null) {
                throw AnimateServiceException("No response from service")
            }
            val receipt = JobParser.parseJob(response.entity.content)
            logger.info("Completed animating {}", animation)
            return receipt
        } catch (e: IOException) {
            throw AnimateServiceException("Failed building service request", e)
        } finally {
            closeConnection(response)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AnimateServiceRemoteImpl::class.java)

        private val gson = Gson()
    }
}
