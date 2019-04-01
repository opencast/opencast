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

package org.opencastproject.coverimage.remote

import org.opencastproject.coverimage.CoverImageException
import org.opencastproject.coverimage.CoverImageService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

/**
 * Remote implementation for [CoverImageService]
 */
/**
 * Default constructor
 */
class CoverImageServiceRemoteImpl : RemoteBase(JOB_TYPE), CoverImageService {

    @Throws(CoverImageException::class)
    override fun generateCoverImage(xml: String, xsl: String, width: String, height: String, posterImageUri: String,
                                    targetFlavor: String): Job {
        val post = HttpPost("/generate")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("xml", xml))
            params.add(BasicNameValuePair("xsl", xsl))
            params.add(BasicNameValuePair("width", width))
            params.add(BasicNameValuePair("height", height))
            params.add(BasicNameValuePair("posterimage", posterImageUri))
            params.add(BasicNameValuePair("targetflavor", targetFlavor))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw CoverImageException("Unable to assemble a remote cover image request", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity, "UTF-8")
                val r = JobParser.parseJob(content)
                logger.info("Cover image generation job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw CoverImageException("Unable to generate cover image using a remote generation service", e)
        } finally {
            closeConnection(response)
        }
        throw CoverImageException("Unable to generate cover image using a remote generation service")
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CoverImageServiceRemoteImpl::class.java)
    }

}
