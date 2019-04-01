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
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Workflow operation for retracting a media package from OAI-PMH publication repository.
 */
class RetractOaiPmhWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The OAI-PMH publication service  */
    private var publicationService: OaiPmhPublicationService? = null

    /**
     * OSGi declarative service configuration callback.
     *
     * @param publicationService
     * the publication service
     */
    fun setPublicationService(publicationService: OaiPmhPublicationService) {
        this.publicationService = publicationService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.activate
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage

        val repository = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(REPOSITORY))
                ?: throw IllegalArgumentException("No repository has been specified")

        try {
            logger.info("Retracting media package {} publication from OAI-PMH repository {}", mediaPackage, repository)

            // Wait for OAI-PMH retraction to finish
            val retractJob = publicationService!!.retract(mediaPackage, repository)
            if (!waitForStatus(retractJob).isSuccess)
                throw WorkflowOperationException("The OAI-PMH retract job did not complete successfully")

            logger.debug("Retraction from OAI-PMH operation complete")

            // Remove the retracted elements from the mediapackage
            val job = serviceRegistry.getJob(retractJob.id)
            if (job.payload != null) {
                logger.info("Removing OAI-PMH publication element from media package {}", mediaPackage)
                val retractedElement = MediaPackageElementParser.getFromXml(job.payload) as Publication
                mediaPackage.remove(retractedElement)
                logger.debug("Remove OAI-PMH publication element '{}' complete", retractedElement)
            } else {
                logger.info("No OAI-PMH publication found to retract in mediapackage {}!", mediaPackage)
                return createResult(mediaPackage, Action.CONTINUE)
            }
            return createResult(mediaPackage, Action.CONTINUE)
        } catch (t: Throwable) {
            throw WorkflowOperationException(t)
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(RetractOaiPmhWorkflowOperationHandler::class.java)

        /** Workflow configuration option keys  */
        private val REPOSITORY = "repository"
    }
}
