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

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.mediapackage.MediaPackageSupport.Filters.ofChannel
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.functions.Strings.toBool
import org.opencastproject.util.data.functions.Strings.trimToNone

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import com.entwinemedia.fn.data.Opt

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.HashSet
import java.util.UUID

/**
 * The workflow definition for handling "publish" operations
 */
class PublishOaiPmhWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The publication service  */
    private var publicationService: OaiPmhPublicationService? = null

    private var distributeStreaming = false

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param publicationService
     * the publication service
     */
    fun setPublicationService(publicationService: OaiPmhPublicationService) {
        this.publicationService = publicationService
    }

    /** OSGi component activation.  */
    public override fun activate(cc: ComponentContext) {
        if (StringUtils.isNotBlank(cc.bundleContext.getProperty(STREAMING_URL_PROPERTY)))
            distributeStreaming = true
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running distribution workflow operation")

        val mediaPackage = workflowInstance.mediaPackage

        // Check which tags have been configured
        val downloadTags = StringUtils
                .trimToEmpty(workflowInstance.currentOperation.getConfiguration(DOWNLOAD_TAGS))
        val downloadFlavors = StringUtils
                .trimToEmpty(workflowInstance.currentOperation.getConfiguration(DOWNLOAD_FLAVORS))
        val streamingTags = StringUtils
                .trimToEmpty(workflowInstance.currentOperation.getConfiguration(STREAMING_TAGS))
        val streamingFlavors = StringUtils
                .trimToEmpty(workflowInstance.currentOperation.getConfiguration(STREAMING_FLAVORS))
        val checkAvailability = option(workflowInstance.currentOperation.getConfiguration(CHECK_AVAILABILITY))
                .bind(trimToNone).map(toBool).getOrElse(true)
        val repository = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(REPOSITORY))

        val externalChannel = getOptConfig(workflowInstance.currentOperation, EXTERNAL_CHANNEL_NAME)
        val externalTempalte = getOptConfig(workflowInstance.currentOperation, EXTERNAL_TEMPLATE)
        val externalMimetype = getOptConfig(workflowInstance.currentOperation, EXTERNAL_MIME_TYPE)
                .bind(MimeTypes.toMimeType)

        if (repository == null)
            throw IllegalArgumentException("No repository has been specified")

        val sourceDownloadTags = StringUtils.split(downloadTags, ",")
        val sourceDownloadFlavors = StringUtils.split(downloadFlavors, ",")
        val sourceStreamingTags = StringUtils.split(streamingTags, ",")
        val sourceStreamingFlavors = StringUtils.split(streamingFlavors, ",")

        if (sourceDownloadTags.size == 0 && sourceDownloadFlavors.size == 0 && sourceStreamingTags.size == 0
                && sourceStreamingFlavors.size == 0) {
            logger.warn("No tags or flavors have been specified, so nothing will be published to the engage")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        val downloadElementSelector = SimpleElementSelector()
        for (flavor in sourceDownloadFlavors) {
            downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
        for (tag in sourceDownloadTags) {
            downloadElementSelector.addTag(tag)
        }
        val downloadElements = downloadElementSelector.select(mediaPackage, false)

        val streamingElements: Collection<MediaPackageElement>
        if (distributeStreaming) {
            val streamingElementSelector = SimpleElementSelector()
            for (flavor in sourceStreamingFlavors) {
                streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            }
            for (tag in sourceStreamingTags) {
                streamingElementSelector.addTag(tag)
            }
            streamingElements = streamingElementSelector.select(mediaPackage, false)
        } else {
            streamingElements = list()
        }

        try {
            val downloadElementIds = HashSet<String>()
            val streamingElementIds = HashSet<String>()

            // Look for elements matching the tag
            for (elem in downloadElements) {
                downloadElementIds.add(elem.identifier)
            }
            for (elem in streamingElements) {
                streamingElementIds.add(elem.identifier)
            }

            var publishJob: Job? = null
            try {
                publishJob = publicationService!!.publish(mediaPackage, repository, downloadElementIds, streamingElementIds,
                        checkAvailability)
            } catch (e: MediaPackageException) {
                throw WorkflowOperationException("Error parsing media package", e)
            } catch (e: PublicationException) {
                throw WorkflowOperationException("Error parsing media package", e)
            }

            // Wait until the publication job has returned
            if (!waitForStatus(publishJob).isSuccess)
                throw WorkflowOperationException("Mediapackage " + mediaPackage.identifier
                        + " could not be published to OAI-PMH repository " + repository)

            // The job has passed
            val job = serviceRegistry.getJob(publishJob!!.id)

            // If there is no payload, then the item has not been published.
            if (job.payload == null) {
                logger.warn("Publish to OAI-PMH repository '{}' failed, no payload from publication job: {}", repository, job)
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
                        "Publication to OAI-PMH repository '{}' failed, unable to parse the payload '{}' from job '{}' to a mediapackage element",
                        repository, job.payload, job.toString())
                return createResult(mediaPackage, Action.CONTINUE)
            }

            for (existingPublication in `$`(*mediaPackage.publications)
                    .find(ofChannel(newElement.channel).toFn())) {
                mediaPackage.remove(existingPublication)
            }
            mediaPackage.add(newElement)

            if (externalChannel.isSome && externalMimetype.isSome && externalTempalte.isSome) {
                var template = externalTempalte.get().replace("{event}", mediaPackage.identifier.compact())
                if (StringUtils.isNotBlank(mediaPackage.series))
                    template = template.replace("{series}", mediaPackage.series)

                val externalElement = PublicationImpl.publication(UUID.randomUUID().toString(), externalChannel.get(),
                        URI.create(template), externalMimetype.get())
                for (existingPublication in `$`(*mediaPackage.publications)
                        .find(ofChannel(externalChannel.get()).toFn())) {
                    mediaPackage.remove(existingPublication)
                }
                mediaPackage.add(externalElement)
            }

            logger.debug("Publication to OAI-PMH repository '{}' operation completed", repository)
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
        private val logger = LoggerFactory.getLogger(PublishOaiPmhWorkflowOperationHandler::class.java)

        private val STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url"

        /** Workflow configuration option keys  */
        private val DOWNLOAD_FLAVORS = "download-flavors"
        private val DOWNLOAD_TAGS = "download-tags"
        private val STREAMING_TAGS = "streaming-tags"
        private val STREAMING_FLAVORS = "streaming-flavors"
        private val CHECK_AVAILABILITY = "check-availability"
        private val REPOSITORY = "repository"
        private val EXTERNAL_TEMPLATE = "external-template"
        private val EXTERNAL_CHANNEL_NAME = "external-channel"
        private val EXTERNAL_MIME_TYPE = "external-mime-type"
    }
}
