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

import org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID

import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.Publication
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.HashSet

/**
 * Workflow operation for retracting a media package from the engage player.
 */
class RetractEngageWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The streaming distribution service  */
    private var streamingDistributionService: DistributionService? = null

    /** The download distribution service  */
    private var downloadDistributionService: DownloadDistributionService? = null

    /** The search service  */
    private var searchService: SearchService? = null

    /** Whether to distribute to streaming server  */
    private var distributeStreaming = false

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param streamingDistributionService
     * the streaming distribution service
     */
    protected fun setStreamingDistributionService(streamingDistributionService: DistributionService) {
        this.streamingDistributionService = streamingDistributionService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param downloadDistributionService
     * the download distribution service
     */
    protected fun setDownloadDistributionService(downloadDistributionService: DownloadDistributionService) {
        this.downloadDistributionService = downloadDistributionService
    }

    /**
     * Callback for declarative services configuration that will introduce us to the search service. Implementation
     * assumes that the reference is configured as being static.
     *
     * @param searchService
     * an instance of the search service
     */
    protected fun setSearchService(searchService: SearchService) {
        this.searchService = searchService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.activate
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        val bundleContext = cc.bundleContext

        if (StringUtils.isNotBlank(bundleContext.getProperty(STREAMING_URL_PROPERTY)))
            distributeStreaming = true
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage
        try {
            val jobs = ArrayList<Job>()

            val query = SearchQuery().withId(mediaPackage.identifier.toString())
            val result = searchService!!.getByQuery(query)
            if (result.size() == 0L) {
                logger.info("The search service doesn't know mediapackage {}", mediaPackage)
                return createResult(mediaPackage, Action.SKIP)
            } else if (result.size() > 1) {
                logger.warn("More than one mediapackage with id {} returned from search service", mediaPackage.identifier)
                throw WorkflowOperationException("More than one mediapackage with id " + mediaPackage.identifier
                        + " found")
            } else {
                val retractElementIds = HashSet<String>()
                val searchMediaPackage = result.items[0].mediaPackage
                logger.info("Retracting media package {} from download/streaming distribution channel", searchMediaPackage)
                for (element in searchMediaPackage.elements) {
                    retractElementIds.add(element.identifier)
                }
                if (retractElementIds.size > 0) {
                    val retractDownloadDistributionJob = downloadDistributionService!!.retract(CHANNEL_ID, searchMediaPackage, retractElementIds)
                    if (retractDownloadDistributionJob != null) {
                        jobs.add(retractDownloadDistributionJob)
                    }
                }
                if (distributeStreaming) {
                    for (element in searchMediaPackage.elements) {
                        if (distributeStreaming) {
                            val retractStreamingJob = streamingDistributionService!!.retract(CHANNEL_ID, searchMediaPackage,
                                    element.identifier)
                            if (retractStreamingJob != null) {
                                jobs.add(retractStreamingJob)
                            }
                        }
                    }
                }
            }

            // Wait for retraction to finish
            if (!waitForStatus(*jobs.toTypedArray()).isSuccess) {
                throw WorkflowOperationException("One of the download/streaming retract job did not complete successfully")
            }

            logger.debug("Retraction operation complete")

            logger.info("Removing media package {} from the search index", mediaPackage)
            val deleteFromSearch = searchService!!.delete(mediaPackage.identifier.toString())
            if (!waitForStatus(deleteFromSearch).isSuccess)
                throw WorkflowOperationException("Removing media package from search did not complete successfully")

            logger.debug("Remove from search operation complete")

            // Remove publication element
            logger.info("Removing engage publication element from media package {}", mediaPackage)
            val publications = mediaPackage.publications
            for (publication in publications) {
                if (CHANNEL_ID == publication.channel) {
                    mediaPackage.remove(publication)
                    logger.debug("Remove engage publication element '{}' complete", publication)
                }
            }

            return createResult(mediaPackage, Action.CONTINUE)
        } catch (t: Throwable) {
            throw WorkflowOperationException(t)
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(RetractEngageWorkflowOperationHandler::class.java)

        /** Configuration property id  */
        private val STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url"
    }

}
