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
package org.opencastproject.publication.oaipmh.remote

import java.lang.String.format
import java.nio.charset.StandardCharsets.UTF_8

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays

/**
 * A remote publication service invoker.
 */
class OaiPmhPublicationServiceRemoteImpl : RemoteBase(JOB_TYPE), OaiPmhPublicationService {

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun publish(mediaPackage: MediaPackage, repository: String, downloadIds: Set<String>, streamingIds: Set<String>,
                         checkAvailability: Boolean): Job {
        val mediapackageXml = MediaPackageParser.getAsXml(mediaPackage)
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", mediapackageXml))
        params.add(BasicNameValuePair("channel", repository))
        params.add(BasicNameValuePair("downloadElementIds", StringUtils.join<Set<String>>(downloadIds, SEPARATOR)))
        params.add(BasicNameValuePair("streamingElementIds", StringUtils.join<Set<String>>(streamingIds, SEPARATOR)))
        params.add(BasicNameValuePair("checkAvailability", java.lang.Boolean.toString(checkAvailability)))
        val post = HttpPost()
        var response: HttpResponse? = null
        try {
            val entity = UrlEncodedFormEntity(params, UTF_8)
            entity.setContentEncoding(UTF_8.toString())
            post.entity = entity
            response = getResponse(post)
            if (response != null) {
                logger.info("Publishing media package {} to OAI-PMH channel {} using a remote publication service",
                        mediaPackage, repository)
                try {
                    return JobParser.parseJob(response.entity.content)
                } catch (e: Exception) {
                    throw PublicationException(
                            "Unable to publish media package '$mediaPackage' using a remote OAI-PMH publication service",
                            e)
                }

            }
        } catch (e: Exception) {
            throw PublicationException(
                    "Unable to publish media package $mediaPackage using a remote OAI-PMH publication service.", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(
                "Unable to publish mediapackage $mediaPackage using a remote OAI-PMH publication service.")
    }

    @Throws(PublicationException::class)
    override fun replace(mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
                         streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                         retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
                         checkAvailability: Boolean): Job {
        var response: HttpResponse? = null
        try {
            val mediapackageXml = MediaPackageParser.getAsXml(mediaPackage)
            val downloadElementsXml = MediaPackageElementParser.getArrayAsXml(downloadElements)
            val streamingElementsXml = MediaPackageElementParser.getArrayAsXml(streamingElements)
            val retractDownloadFlavorsString = StringUtils.join<Set<MediaPackageElementFlavor>>(retractDownloadFlavors, SEPARATOR)
            val retractStreamingFlavorsString = StringUtils.join<Set<MediaPackageElementFlavor>>(retractStreamingFlavors, SEPARATOR)
            val publicationsXml = MediaPackageElementParser.getArrayAsXml(publications)
            val params = Arrays.asList(
                    BasicNameValuePair("mediapackage", mediapackageXml),
                    BasicNameValuePair("channel", repository),
                    BasicNameValuePair("downloadElements", downloadElementsXml),
                    BasicNameValuePair("streamingElements", streamingElementsXml),
                    BasicNameValuePair("retractDownloadFlavors", retractDownloadFlavorsString),
                    BasicNameValuePair("retractStreamingFlavors", retractStreamingFlavorsString),
                    BasicNameValuePair("publications", publicationsXml),
                    BasicNameValuePair("checkAvailability", java.lang.Boolean.toString(checkAvailability)))
            val post = HttpPost("replace")
            post.entity = UrlEncodedFormEntity(params, UTF_8)
            response = getResponse(post)
            if (response != null) {
                logger.info("Replace media package {} in OAI-PMH channel {} using a remote publication service", mediaPackage,
                        repository)

                return JobParser.parseJob(response.entity.content)
            }
        } catch (e: Exception) {
            throw PublicationException(
                    "Unable to replace media package $mediaPackage using a remote OAI-PMH publication service.", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(
                "Unable to replace media package $mediaPackage using a remote OAI-PMH publication service.")
    }

    @Throws(PublicationException::class)
    override fun replaceSync(
            mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
            streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
            retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
            checkAvailability: Boolean): Publication {
        var response: HttpResponse? = null
        try {
            val mediapackageXml = MediaPackageParser.getAsXml(mediaPackage)
            val downloadElementsXml = MediaPackageElementParser.getArrayAsXml(downloadElements)
            val streamingElementsXml = MediaPackageElementParser.getArrayAsXml(streamingElements)
            val retractDownloadFlavorsString = StringUtils.join<Set<MediaPackageElementFlavor>>(retractDownloadFlavors, SEPARATOR)
            val retractStreamingFlavorsString = StringUtils.join<Set<MediaPackageElementFlavor>>(retractStreamingFlavors, SEPARATOR)
            val publicationsXml = MediaPackageElementParser.getArrayAsXml(publications)
            val params = Arrays.asList(
                    BasicNameValuePair("mediapackage", mediapackageXml),
                    BasicNameValuePair("channel", repository),
                    BasicNameValuePair("downloadElements", downloadElementsXml),
                    BasicNameValuePair("streamingElements", streamingElementsXml),
                    BasicNameValuePair("retractDownloadFlavors", retractDownloadFlavorsString),
                    BasicNameValuePair("retractStreamingFlavors", retractStreamingFlavorsString),
                    BasicNameValuePair("publications", publicationsXml),
                    BasicNameValuePair("checkAvailability", java.lang.Boolean.toString(checkAvailability)))
            val post = HttpPost("replacesync")
            post.entity = UrlEncodedFormEntity(params, UTF_8)
            response = getResponse(post)
            if (response != null) {
                logger.info("Replace media package {} in OAI-PMH channel {} using a remote publication service", mediaPackage,
                        repository)

                val xml = IOUtils.toString(response.entity.content, Charset.forName("utf-8"))
                return MediaPackageElementParser.getFromXml(xml) as Publication
            }
        } catch (e: Exception) {
            throw PublicationException(
                    "Unable to replace media package $mediaPackage using a remote OAI-PMH publication service.", e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(
                "Unable to replace media package $mediaPackage using a remote OAI-PMH publication service.")
    }

    @Throws(PublicationException::class)
    override fun retract(mediaPackage: MediaPackage, repository: String): Job {
        val mediapackageXml = MediaPackageParser.getAsXml(mediaPackage)
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", mediapackageXml))
        params.add(BasicNameValuePair("channel", repository))
        val post = HttpPost("/retract")
        var response: HttpResponse? = null
        val entity = UrlEncodedFormEntity(params, UTF_8)
        entity.setContentEncoding(UTF_8.toString())
        post.entity = entity
        try {
            response = getResponse(post)
            var receipt: Job? = null
            if (response != null) {
                logger.info("Retracting {} from OAI-PMH channel {} using a remote publication service",
                        mediaPackage.identifier.compact(), repository)
                try {
                    receipt = JobParser.parseJob(response.entity.content)
                    return receipt
                } catch (e: Exception) {
                    throw PublicationException(format(
                            "Unable to retract media package %s from OAI-PMH channel %s using a remote publication service",
                            mediaPackage.identifier.compact(), repository), e)
                }

            }
        } finally {
            closeConnection(response)
        }
        throw PublicationException(format(
                "Unable to retract media package %s from OAI-PMH channel %s using a remote publication service",
                mediaPackage.identifier.compact(), repository))
    }

    @Throws(PublicationException::class)
    override fun updateMetadata(mediaPackage: MediaPackage, repository: String, flavors: Set<String>, tags: Set<String>,
                                checkAvailability: Boolean): Job {
        val mediapackageXml = MediaPackageParser.getAsXml(mediaPackage)
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("mediapackage", mediapackageXml))
        params.add(BasicNameValuePair("channel", repository))
        params.add(BasicNameValuePair("flavors", StringUtils.join<Set<String>>(flavors, SEPARATOR)))
        params.add(BasicNameValuePair("tags", StringUtils.join<Set<String>>(tags, SEPARATOR)))
        params.add(BasicNameValuePair("checkAvailability", java.lang.Boolean.toString(checkAvailability)))
        val post = HttpPost("/updateMetadata")
        var response: HttpResponse? = null
        try {
            post.entity = UrlEncodedFormEntity(params, UTF_8)
            response = getResponse(post)
            if (response != null) {
                logger.info("Update media package {} metadata in OAI-PMH channel {} using a remote publication service",
                        mediaPackage.identifier.compact(), repository)
                return JobParser.parseJob(response.entity.content)
            }
        } catch (e: Exception) {
            throw PublicationException(format(
                    "Unable to update media package %s metadata in OAI-PMH repository %s using a remote publication service.",
                    mediaPackage.identifier.compact(), repository), e)
        } finally {
            closeConnection(response)
        }
        throw PublicationException(format(
                "Unable to update media package %s metadata in OAI-PMH repository %s using a remote publication service.",
                mediaPackage.identifier.compact(), repository))
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(OaiPmhPublicationServiceRemoteImpl::class.java)
    }
}
