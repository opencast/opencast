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

package org.opencastproject.textanalyzer.remote

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.textanalyzer.api.TextAnalyzerException
import org.opencastproject.textanalyzer.api.TextAnalyzerService

import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

class TextAnalysisRemoteImpl : RemoteBase(JOB_TYPE), TextAnalyzerService {

    /**
     * {@inheritDoc}
     */
    @Throws(TextAnalyzerException::class)
    override fun extract(image: Attachment): Job {
        val post = HttpPost()
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("image", MediaPackageElementParser.getAsXml(image)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw TextAnalyzerException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                try {
                    val receipt = JobParser.parseJob(response.entity.content)
                    logger.info("Analyzing {} on a remote analysis server", image)
                    return receipt
                } catch (e: Exception) {
                    throw TextAnalyzerException("Unable to analyze element '$image' using a remote analysis service",
                            e)
                }

            }
        } finally {
            closeConnection(response)
        }
        throw TextAnalyzerException("Unable to analyze element '$image' using a remote analysis service")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(TextAnalysisRemoteImpl::class.java)
    }

}
