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
package org.opencastproject.publication.api

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.util.NotFoundException

/**
 * Publishes elements from MediaPackages to OAI-PMH.
 */
interface OaiPmhPublicationService {

    /**
     * Publishes some media package elements.
     *
     * @param mediaPackage
     * the media package
     * @param repository
     * the OAI-PMH repository
     * @param downloadElementIds
     * the download element ids to publish
     * @param streamingElementIds
     * the streaming element ids to publish
     * @param checkAvailability
     * whether to check the distributed download artifacts are available at its URL
     * @return The job
     * @throws PublicationException
     * if there was a problem publishing the media
     * @throws MediaPackageException
     * if there was a problem with the mediapackage element
     */
    @Throws(PublicationException::class, MediaPackageException::class)
    fun publish(mediaPackage: MediaPackage, repository: String, downloadElementIds: Set<String>,
                streamingElementIds: Set<String>, checkAvailability: Boolean): Job

    /**
     * Updates the given media package in the Oai-Pmh storage incrementally, i.e. without retracting the whole media
     * package.
     *
     * @param mediaPackage
     * The media package to publish the element for
     * @param repository
     * The OAI-PMH repository
     * @param downloadElements
     * the download elements to publish
     * @param streamingElements
     * the streaming elements to publish
     * @param retractDownloadFlavors
     * flavors to use to search for download elements to retract.
     * @param retractStreamingFlavors
     * flavors to use to search for streaming elements to retract.
     * @param publications
     * the publications to update
     * @param checkAvailability
     * whether to check the distributed download artifacts are available at their URLs
     *
     * @return The job which performs the operation (The job payload will hold the publication with the updated media
     * package).
     *
     * @throws PublicationException
     * if the job could not be created.
     */
    @Throws(PublicationException::class)
    fun replace(mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
                streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
                checkAvailability: Boolean): Job

    /**
     * Synchronously updates the given media package in the Oai-Pmh storage incrementally, i.e. without retracting the whole media
     * package.
     *
     * @param mediaPackage
     * The media package to publish the element for
     * @param repository
     * The OAI-PMH repository
     * @param downloadElements
     * the download elements to publish
     * @param streamingElements
     * the streaming elements to publish
     * @param retractDownloadFlavors
     * flavors to use to search for download elements to retract.
     * @param retractStreamingFlavors
     * flavors to use to search for streaming elements to retract.
     * @param publications
     * the publications to update
     * @param checkAvailability
     * whether to check the distributed download artifacts are available at their URLs
     *
     * @return The publication with the updated media package.
     *
     * @throws PublicationException
     * if the job could not be created.
     * @throws MediaPackageException
     * if distribution failed.
     */
    @Throws(PublicationException::class, MediaPackageException::class)
    fun replaceSync(mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
                    streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                    retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
                    checkAvailability: Boolean): Publication

    /**
     * Retract a media package from the publication channel.
     *
     * @param mediaPackage
     * the media package
     * @param repository
     * the OAI-PMH repository
     * @throws NotFoundException
     * if there was no mediapackage to retract from this channel
     * @throws PublicationException
     * if there was a problem retracting the mediapackage
     */
    @Throws(PublicationException::class, NotFoundException::class)
    fun retract(mediaPackage: MediaPackage, repository: String): Job

    /**
     * Update all media package elements that match the flavors and tags. Also update the media package in the given
     * OAI-PMH repository.
     *
     * @param mediaPackage media package with updated elements
     * @param repository OAI-PMH repository where to update the media package
     * @param flavors updated media package element flavors
     * @param tags updated media package element tags
     * @param checkAvailability whether to check the distributed download artifacts are available at its URL
     * @return The job to update the media package
     * @throws PublicationException if there was a problem publishing the media
     * @throws MediaPackageException if there was a problem with the media package element
     */
    @Throws(PublicationException::class, MediaPackageException::class)
    fun updateMetadata(mediaPackage: MediaPackage, repository: String, flavors: Set<String>, tags: Set<String>,
                       checkAvailability: Boolean): Job

    companion object {

        /**
         * Identifier for service registration and location
         */
        val JOB_TYPE = "org.opencastproject.publication.oaipmh"

        /**
         * The Opencast publication channel id is created from the OAI-PMH channel name prefixed by the
         * [.PUBLICATION_CHANNEL_PREFIX].
         */
        val PUBLICATION_CHANNEL_PREFIX = "oaipmh-"

        /**
         * Separator used to separate strings when serializing arrays of strings.
         */
        val SEPARATOR = ";;"
    }
}
