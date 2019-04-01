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

import org.opencastproject.caption.api.CaptionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.transcription.api.TranscriptionService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AttachTranscriptionOperationHandler : AbstractWorkflowOperationHandler() {

    /** The transcription service  */
    private var service: TranscriptionService? = null
    private var workspace: Workspace? = null
    private var captionService: CaptionService? = null

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

        logger.debug("Attach transcription for mediapackage {} started", mediaPackage)

        // Get job id.
        val jobId = StringUtils.trimToNull(operation.getConfiguration(TRANSCRIPTION_JOB_ID))
                ?: throw WorkflowOperationException("$TRANSCRIPTION_JOB_ID missing")

        // Check which tags/flavors have been configured
        val targetTagOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAG))
        val targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR))
        val captionFormatOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_CAPTION_FORMAT))
        // Target flavor is mandatory if target-caption-format was NOT informed and no conversion is done
        if (targetFlavorOption == null && captionFormatOption == null)
            throw WorkflowOperationException("$TARGET_FLAVOR missing")
        // Target flavor is optional if target-caption-format was informed because the default flavor
        // will be "captions/<format>". If informed, will override the default.
        var flavor: MediaPackageElementFlavor? = null
        if (targetFlavorOption != null)
            flavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)

        try {
            // Get transcription file from the service
            val original = service!!.getGeneratedTranscription(mediaPackage.identifier.compact(), jobId)
            var transcription = original

            // If caption format passed, convert to desired format
            if (captionFormatOption != null) {
                val job = captionService!!.convert(transcription, "ibm-watson", captionFormatOption, service!!.language)
                if (!waitForStatus(job).isSuccess) {
                    throw WorkflowOperationException("Transcription format conversion job did not complete successfully")
                }
                transcription = MediaPackageElementParser.getFromXml(job.payload)
            }

            // Set the target flavor if informed
            if (flavor != null)
                transcription.flavor = flavor

            // Add tags
            if (targetTagOption != null) {
                for (tag in asList(targetTagOption)) {
                    if (StringUtils.trimToNull(tag) != null)
                        transcription.addTag(tag)
                }
            }

            // Add to media package
            mediaPackage.add(transcription)

            val uri = transcription.getURI().toString()
            val ext = uri.substring(uri.lastIndexOf("."))
            transcription.setURI(workspace!!.moveTo(transcription.getURI(), mediaPackage.identifier.toString(),
                    transcription.identifier, "captions.$ext"))
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

        return createResult(mediaPackage, Action.CONTINUE)
    }

    fun setTranscriptionService(service: TranscriptionService) {
        this.service = service
    }

    fun setWorkspace(service: Workspace) {
        this.workspace = service
    }

    fun setCaptionService(service: CaptionService) {
        this.captionService = service
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(AttachTranscriptionOperationHandler::class.java)

        /** Workflow configuration option keys  */
        internal val TRANSCRIPTION_JOB_ID = "transcription-job-id"
        internal val TARGET_FLAVOR = "target-flavor"
        internal val TARGET_TAG = "target-tag"
        internal val TARGET_CAPTION_FORMAT = "target-caption-format"
    }

}
