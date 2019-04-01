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
package org.opencastproject.transcription.workflowoperation

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.transcription.api.TranscriptionService
import org.opencastproject.transcription.api.TranscriptionServiceException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StartTranscriptionOperationHandler : AbstractWorkflowOperationHandler() {

    /** The transcription service  */
    private var service: TranscriptionService? = null

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
        val operation = workflowInstance.currentOperation

        val skipOption = StringUtils.trimToNull(operation.getConfiguration(SKIP_IF_FLAVOR_EXISTS))
        if (skipOption != null) {
            val mpes = mediaPackage.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor(skipOption))
            if (mpes != null && mpes.size > 0) {
                logger.info(
                        "Start transcription operation will be skipped because flavor {} already exists in the media package",
                        skipOption)
                return createResult(Action.SKIP)
            }
        }

        logger.debug("Start transcription for mediapackage {} started", mediaPackage)

        // Check which tags have been configured
        val sourceTagOption = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAG))
        val sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR))

        val elementSelector = TrackSelector()

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceTagOption) && StringUtils.isBlank(sourceFlavorOption))
            throw WorkflowOperationException("No source tag or flavor have been specified!")

        if (StringUtils.isNotBlank(sourceFlavorOption)) {
            val flavor = StringUtils.trim(sourceFlavorOption)
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Source flavor '$flavor' is malformed")
            }

        }
        if (sourceTagOption != null)
            elementSelector.addTag(sourceTagOption)

        val elements = elementSelector.select(mediaPackage, false)
        var job: Job? = null
        for (track in elements) {
            if (track.hasVideo()) {
                logger.info("Skipping track {} since it contains a video stream", track)
                continue
            }
            try {
                job = service!!.startTranscription(mediaPackage.identifier.compact(), track)
                // Only one job per media package
                break
            } catch (e: TranscriptionServiceException) {
                throw WorkflowOperationException(e)
            }

        }

        if (job == null) {
            logger.info("No matching tracks found")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Wait for the jobs to return
        if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException("Transcription job did not complete successfully")
        }
        // Return OK means that the ibm watson job was created, but not finished yet

        logger.debug("External transcription job for mediapackage {} was created", mediaPackage)

        // Results are empty, we should get a callback when transcription is done
        return createResult(Action.CONTINUE)
    }

    fun setTranscriptionService(service: TranscriptionService) {
        this.service = service
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(StartTranscriptionOperationHandler::class.java)

        /** Workflow configuration option keys  */
        internal val SOURCE_FLAVOR = "source-flavor"
        internal val SOURCE_TAG = "source-tag"
        internal val SKIP_IF_FLAVOR_EXISTS = "skip-if-flavor-exists"
    }

}
