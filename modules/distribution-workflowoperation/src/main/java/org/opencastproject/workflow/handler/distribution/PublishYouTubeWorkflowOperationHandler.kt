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

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.publication.api.YouTubePublicationService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The workflow definition for handling "publish" operations
 */
class PublishYouTubeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The publication service  */
    private var publicationService: YouTubePublicationService? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param publicationService
     * the publication service
     */
    fun setPublicationService(publicationService: YouTubePublicationService) {
        this.publicationService = publicationService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running youtube publication workflow operation")

        val mediaPackage = workflowInstance.mediaPackage

        // Check which tags have been configured
        val sourceTags = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration("source-tags"))
        val sourceFlavors = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                "source-flavors"))

        val elementSelector: AbstractMediaPackageElementSelector<MediaPackageElement>

        if (sourceTags == null && sourceFlavors == null) {
            logger.warn("No tags or flavor have been specified")
            return createResult(mediaPackage, Action.CONTINUE)
        }
        elementSelector = SimpleElementSelector()

        if (sourceFlavors != null) {
            for (flavor in asList(sourceFlavors)) {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            }
        }
        if (sourceTags != null) {
            for (tag in asList(sourceTags)) {
                elementSelector.addTag(tag)
            }
        }

        try {
            // Look for elements matching the tag
            val elements = elementSelector.select(mediaPackage, true)
            if (elements.size > 1) {
                logger.warn("More than one element has been found for publishing to youtube: {}", elements)
                return createResult(mediaPackage, Action.SKIP)
            }

            if (elements.size < 1) {
                logger.info("No mediapackage element was found for publishing")
                return createResult(mediaPackage, Action.CONTINUE)
            }

            val youtubeJob: Job
            try {
                val track = mediaPackage.getTrack(elements.iterator().next().identifier)
                youtubeJob = publicationService!!.publish(mediaPackage, track)
            } catch (e: PublicationException) {
                throw WorkflowOperationException(e)
            }

            // Wait until the youtube publication job has returned
            if (!waitForStatus(youtubeJob).isSuccess)
                throw WorkflowOperationException("The youtube publication jobs did not complete successfully")

            // All the jobs have passed
            val job = serviceRegistry.getJob(youtubeJob.id)

            // If there is no payload, then the item has not been published.
            if (job.payload == null) {
                logger.warn("Publish to youtube failed, no payload from publication job: {}", job)
                return createResult(mediaPackage, Action.CONTINUE)
            }

            var newElement: Publication? = null
            try {
                newElement = MediaPackageElementParser.getFromXml(job.payload) as Publication
            } catch (e: MediaPackageException) {
                throw WorkflowOperationException(e)
            }

            if (newElement == null) {
                logger.warn(
                        "Publication to youtube failed, unable to parse the payload '{}' from job '{}' to a mediapackage element",
                        job.payload, job)
                return createResult(mediaPackage, Action.CONTINUE)
            }
            mediaPackage.add(newElement)

            logger.debug("Publication to youtube operation completed")
        } catch (e: Exception) {
            if (e is WorkflowOperationException) {
                throw e
            } else {
                throw WorkflowOperationException(e)
            }
        }

        return createResult(mediaPackage, Action.CONTINUE)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(PublishYouTubeWorkflowOperationHandler::class.java)
    }
}
