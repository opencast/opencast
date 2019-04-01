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
package org.opencastproject.publication.configurable.remote

import java.nio.charset.StandardCharsets.UTF_8

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.publication.api.ConfigurablePublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.serviceregistry.api.RemoteBase

import com.google.gson.Gson

import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.Charset
import java.util.ArrayList

/**
 * A remote publication service invoker.
 */
class ConfigurablePublicationServiceRemoteImpl : RemoteBase(JOB_TYPE), ConfigurablePublicationService {

    /* Gson is thread-safe so we use a single instance */
    private val gson = Gson()

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun replace(mediaPackage: MediaPackage, channelId: String,
                         addElements: Collection<MediaPackageElement>, retractElementIds: Set<String>): Job {

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
        params.add(BasicNameValuePair("channel", channelId))
        params.add(BasicNameValuePair("addElements", MediaPackageElementParser.getArrayAsXml(addElements)))
        params.add(BasicNameValuePair("retractElements", gson.toJson(retractElementIds)))

        val post = HttpPost("/replace")
        var response: HttpResponse? = null
        try {
            post.entity = UrlEncodedFormEntity(params, UTF_8)
            response = getResponse(post)
            if (response != null) {
                logger.info("Publishing media package {} to channel {} using a remote publication service",
                        mediaPackage, channelId)
                try {
                    return JobParser.parseJob(response.entity.content)
                } catch (e: Exception) {
                    throw PublicationException(
                            "Unable to publish media package '$mediaPackage' using a remote publication service", e)
                }

            }
        } catch (e: Exception) {
            throw PublicationException(
                    "Unable to publish media package $mediaPackage using a remote publication service.", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(
                "Unable to publish mediapackage $mediaPackage using a remote publication service.")
    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun replaceSync(
            mediaPackage: MediaPackage, channelId: String, addElements: Collection<MediaPackageElement>,
            retractElementIds: Set<String>): Publication {
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
        params.add(BasicNameValuePair("channel", channelId))
        params.add(BasicNameValuePair("addElements", MediaPackageElementParser.getArrayAsXml(addElements)))
        params.add(BasicNameValuePair("retractElements", gson.toJson(retractElementIds)))

        val post = HttpPost("/replacesync")
        var response: HttpResponse? = null
        try {
            post.entity = UrlEncodedFormEntity(params, UTF_8)
            response = getResponse(post)
            if (response != null) {
                logger.info("Publishing media package {} to channel {} using a remote publication service",
                        mediaPackage, channelId)
                try {
                    val xml = IOUtils.toString(response.entity.content, Charset.forName("utf-8"))
                    return MediaPackageElementParser.getFromXml(xml) as Publication
                } catch (e: Exception) {
                    throw PublicationException(
                            "Unable to publish media package '$mediaPackage' using a remote publication service", e)
                }

            }
        } catch (e: Exception) {
            throw PublicationException(
                    "Unable to publish media package $mediaPackage using a remote publication service.", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(
                "Unable to publish mediapackage $mediaPackage using a remote publication service.")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(ConfigurablePublicationServiceRemoteImpl::class.java)
    }
}
