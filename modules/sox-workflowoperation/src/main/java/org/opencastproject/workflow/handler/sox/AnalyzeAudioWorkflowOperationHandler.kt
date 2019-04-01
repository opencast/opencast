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

package org.opencastproject.workflow.handler.sox

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.sox.api.SoxException
import org.opencastproject.sox.api.SoxService
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.HashMap

/**
 * The workflow definition for handling "sox" operations
 */
class AnalyzeAudioWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The SoX service  */
    private var soxService: SoxService? = null

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param soxService
     * the SoX service
     */
    fun setSoxService(soxService: SoxService) {
        this.soxService = soxService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running analyze audio workflow operation on workflow {}", workflowInstance.id)

        try {
            return analyze(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    @Throws(SoxException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class, EncoderException::class)
    private fun analyze(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage

        // Check which tags have been configured
        val sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"))
        val sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration("source-flavor"))
        val sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("source-flavors"))
        val forceTranscode = BooleanUtils.toBoolean(operation.getConfiguration("force-transcode"))

        val elementSelector = TrackSelector()

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorOption)
                && StringUtils.isBlank(sourceFlavorsOption)) {
            logger.info("No source tags or flavors have been specified, not matching anything")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Select the source flavors
        for (flavor in asList(sourceFlavorsOption)) {
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Source flavor '$flavor' is malformed")
            }

        }

        // Support legacy "source-flavor" option
        if (StringUtils.isNotBlank(sourceFlavorOption)) {
            val flavor = StringUtils.trim(sourceFlavorOption)
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Source flavor '$flavor' is malformed")
            }

        }

        // Select the source tags
        for (tag in asList(sourceTagsOption)) {
            elementSelector.addTag(tag)
        }

        // Look for elements matching the tag
        val elements = elementSelector.select(mediaPackage, false)

        // Analyze audio for all tracks found
        var totalTimeInQueue: Long = 0
        val cleanupURIs = ArrayList<URI>()
        val analyzeJobs = HashMap<Job, Track>()
        try {
            for (track in elements) {

                var audioTrack = track as TrackImpl
                // Skip video only mismatches
                if (!track.hasAudio()) {
                    logger.info("Skipping audio analysis of '{}', since it contains no audio stream", track)
                    continue
                } else if (track.hasVideo() || forceTranscode) {
                    audioTrack = extractAudioTrack(track) as TrackImpl
                    audioTrack.setAudio(track.getAudio()!!)
                    cleanupURIs.add(audioTrack.getURI())
                }

                analyzeJobs[soxService!!.analyze(audioTrack)] = track
            }

            if (analyzeJobs.isEmpty()) {
                logger.info("No matching tracks found")
                return createResult(mediaPackage, Action.CONTINUE)
            }

            // Wait for the jobs to return
            if (!waitForStatus(*analyzeJobs.keys.toTypedArray()).isSuccess)
                throw WorkflowOperationException("One of the analyze jobs did not complete successfully")

            // Process the result
            for ((job, value) in analyzeJobs) {
                val origTrack = value as TrackImpl

                // add this receipt's queue time to the total
                totalTimeInQueue += job.queueTime!!

                if (job.payload.length > 0) {
                    val analyzed = MediaPackageElementParser.getFromXml(job.payload) as TrackImpl

                    // Set metadata on track
                    origTrack.setAudio(analyzed.getAudio()!!)
                } else {
                    logger.warn("Analyze audio job {} for track {} has no result!", job, origTrack)
                }
            }
        } finally {
            // Clean up temporary audio files from workspace
            for (uri in cleanupURIs) {
                workspace!!.delete(uri)
            }
        }

        val result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
        logger.debug("Analyze audio operation completed")
        return result
    }

    /**
     * Extract the audio track from the given video track.
     *
     * @param videoTrack
     * the track containing the audio
     * @return the extracted audio track
     * @throws WorkflowOperationException
     * @throws NotFoundException
     * @throws EncoderException
     * @throws MediaPackageException
     */
    @Throws(WorkflowOperationException::class, EncoderException::class, MediaPackageException::class)
    private fun extractAudioTrack(videoTrack: Track): Track {
        logger.info("Extract audio stream from track {}", videoTrack)
        val job = composerService!!.encode(videoTrack, SOX_AONLY_PROFILE)
        if (!waitForStatus(job).isSuccess)
            throw WorkflowOperationException("Extracting audio track from video track $videoTrack failed")

        return MediaPackageElementParser.getFromXml(job.payload) as Track
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(AnalyzeAudioWorkflowOperationHandler::class.java)

        /** Name of the 'encode to SoX audio only work copy' encoding profile  */
        val SOX_AONLY_PROFILE = "sox-audio-only.work"
    }

}
