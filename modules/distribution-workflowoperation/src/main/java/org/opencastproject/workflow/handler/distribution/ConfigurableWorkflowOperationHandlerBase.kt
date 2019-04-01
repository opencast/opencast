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
package org.opencastproject.workflow.handler.distribution

import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowOperationException

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashSet

/**
 * Abstract base class of ConfigurablePublishWorkflerOperationHandler and ConfigurableRectractWorkflowOperationHandler.
 *
 * Both the ConfigurablePublishWorkflowOperationHandler and ConfigurableRetractWorkflowOperationHanlder are capable of
 * retracting publications created by the ConfigurablePublishWorkflowOperationHandler.
 * To avoid code duplication, this commonly used functionaly has been factored out into this class.
 */
abstract class ConfigurableWorkflowOperationHandlerBase : AbstractWorkflowOperationHandler() {

    internal abstract val distributionService: DownloadDistributionService

    /**
     * Adds all of the [Publication]'s [MediaPackageElement]s that would normally have not been in the
     * [MediaPackage].
     *
     * @param publication
     * The [Publication] with the [MediaPackageElement]s to add.
     * @param mp
     * The [MediaPackage] to add the [MediaPackageElement]s to.
     */
    private fun addPublicationElementsToMediaPackage(publication: Publication, mp: MediaPackage?) {
        assert(publication != null && mp != null)
        for (attachment in publication.attachments) {
            mp!!.add(attachment)
        }
        for (catalog in publication.catalogs) {
            mp!!.add(catalog)
        }
        for (track in publication.tracks) {
            mp!!.add(track)
        }
    }

    /**
     * Remove the [Publication]'s [MediaPackageElement]s from a given channel.
     *
     * @param channelId
     * The channel to remove the [MediaPackageElement]s from.
     * @param publication
     * The [Publication] that is being removed.
     * @param mp
     * The [MediaPackage] that the [Publication] is part of.
     * @return the number of [MediaPackageElement]s that have been retracted
     * @throws WorkflowOperationException
     * Thrown if unable to retract the [MediaPackageElement]s.
     */
    @Throws(WorkflowOperationException::class)
    private fun retractPublicationElements(channelId: String?, publication: Publication?, mp: MediaPackage): Int {
        assert(channelId != null && publication != null && mp != null)
        val mediapackageWithPublicationElements = mp.clone() as MediaPackage

        // Add the publications to the mediapackage so that we can use the standard retract
        addPublicationElementsToMediaPackage(publication!!, mediapackageWithPublicationElements)

        val elementIds = HashSet<String>()

        for (attachment in publication.attachments) {
            elementIds.add(attachment.identifier)
        }
        for (catalog in publication.catalogs) {
            elementIds.add(catalog.identifier)
        }
        for (track in publication.tracks) {
            elementIds.add(track.identifier)
        }

        if (elementIds.size > 0) {
            logger.info("Retracting {} elements of media package {} from publication channel {}", elementIds.size, mp,
                    channelId)
            var job: Job? = null
            try {
                job = distributionService.retract(channelId!!, mediapackageWithPublicationElements, elementIds)
            } catch (e: DistributionException) {
                logger.error("Error while retracting '{}' elements from channel '{}' of distribution '{}': {}",
                        elementIds.size, channelId, distributionService, getStackTrace(e))
                throw WorkflowOperationException("The retraction job did not complete successfully")
            }

            if (!waitForStatus(job).isSuccess) {
                throw WorkflowOperationException("The retraction job did not complete successfully")
            }
        } else {
            logger.debug("No publication elements were found for retraction")
        }

        return elementIds.size
    }

    fun getPublications(mp: MediaPackage?, channelId: String?): List<Publication> {
        assert(mp != null && channelId != null)
        return Stream.mk(*mp!!.publications).filter(object : Fn<Publication, Boolean>() {
            override fun apply(a: Publication): Boolean? {
                return channelId == a.channel
            }
        }).toList()!!
    }

    @Throws(WorkflowOperationException::class)
    fun retract(mp: MediaPackage?, channelId: String?) {
        assert(mp != null && channelId != null)

        val publications = getPublications(mp, channelId)

        if (publications.size > 0) {
            var retractedElementsCount = 0
            for (publication in publications) {
                retractedElementsCount += retractPublicationElements(channelId, publication, mp!!)
                mp.remove(publication)
            }
            logger.info("Successfully retracted {} publications and retracted {} elements from publication channel '{}'",
                    publications.size, retractedElementsCount, channelId)
        } else {
            logger.info("No publications for channel {} found for media package {}", channelId, mp!!.identifier)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConfigurableWorkflowOperationHandlerBase::class.java)
    }

}
