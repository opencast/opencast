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

package org.opencastproject.workflow.handler.composer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.HashMap

/**
 * The workflow definition for handling demux operations.
 */
class DemuxWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the local composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /**
     * Callback for declarative services configuration that will introduce us to the local workspace service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param workspace
     * an instance of the workspace
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
        logger.debug("Running demux workflow operation on workflow {}", workflowInstance.id)

        try {
            return demux(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
     *
     * @param src
     * The source media package
     * @param operation
     * the current workflow operation
     * @return the operation result containing the updated media package
     * @throws EncoderException
     * if encoding fails
     * @throws WorkflowOperationException
     * if errors occur during processing
     * @throws IOException
     * if the workspace operations fail
     * @throws NotFoundException
     * if the workspace doesn't contain the requested file
     */
    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun demux(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage
        val sectionSeparator = ";"
        // Check which tags have been configured
        val sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"))
        var sourceFlavorsOption: String? = StringUtils.trimToNull(operation.getConfiguration("source-flavors"))
        if (sourceFlavorsOption == null)
            sourceFlavorsOption = StringUtils.trimToEmpty(operation.getConfiguration("source-flavor"))
        var targetFlavorsOption: String? = StringUtils.trimToNull(operation.getConfiguration("target-flavors"))
        if (targetFlavorsOption == null)
            targetFlavorsOption = StringUtils.trimToEmpty(operation.getConfiguration("target-flavor"))
        val targetTagsOption = StringUtils.trimToNull(operation.getConfiguration("target-tags"))
        val encodingProfile = StringUtils.trimToEmpty(operation.getConfiguration("encoding-profile"))

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorsOption)) {
            logger.info("No source tags or flavors have been specified, not matching anything")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        val targetFlavors = asList(targetFlavorsOption)
        val targetTags = StringUtils.split(targetTagsOption, sectionSeparator)
        val elementSelector = TrackSelector()

        // Select the source flavors
        for (flavor in asList(sourceFlavorsOption)) {
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException(String.format("Source flavor '%s' is malformed", flavor))
            }

        }

        // Select the source tags
        for (tag in asList(sourceTagsOption)) {
            elementSelector.addTag(tag)
        }

        // Find the encoding profile - should only be one
        val profile = composerService!!.getProfile(encodingProfile)
                ?: throw WorkflowOperationException(String.format("Encoding profile '%s' was not found", encodingProfile))
// Look for elements matching the tag
        val sourceTracks = elementSelector.select(mediaPackage, false)
        if (sourceTracks.isEmpty()) {
            logger.info("No matching tracks found")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        var totalTimeInQueue: Long = 0
        val encodingJobs = HashMap<Job, Track>()
        for (track in sourceTracks) {
            logger.info("Demuxing track {} using encoding profile '{}'", track, profile)
            // Start encoding and wait for the result
            encodingJobs[composerService!!.demux(track, profile.identifier)] = track
        }

        // Wait for the jobs to return
        if (!waitForStatus(*encodingJobs.keys.toTypedArray()).isSuccess) {
            throw WorkflowOperationException("One of the encoding jobs did not complete successfully")
        }

        // Process the result
        for ((job, sourceTrack) in encodingJobs) {

            // add this receipt's queue time to the total
            totalTimeInQueue += job.queueTime!!

            // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
            if (job.payload.length <= 0) {
                logger.warn("No output from Demux operation")
                continue
            }

            val composedTracks = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
            if (composedTracks.size != targetFlavors.size && targetFlavors.size != 1) {
                throw WorkflowOperationException(String.format("Number of target flavors (%d) and output tracks (%d) do " + "not match", targetFlavors.size, composedTracks.size))
            }
            if (composedTracks.size != targetTags.size && targetTags.size != 1 && targetTags.size != 0) {
                throw WorkflowOperationException(String.format("Number of target tag groups (%d) and output tracks (%d) " + "do not match", targetTags.size, composedTracks.size))
            }

            // Flavor each track in the order read
            var flavorIndex = 0
            var tagsIndex = 0
            for (composedTrack in composedTracks) {
                // set flavor to the matching flavor in the order listed
                composedTrack.flavor = newFlavor(sourceTrack, targetFlavors[flavorIndex])
                if (targetFlavors.size > 1) {
                    flavorIndex++
                }
                if (targetTags.size > 0) {
                    asList(targetTags[tagsIndex]).forEach(Consumer<String> { composedTrack.addTag(it) })
                    logger.trace("Tagging composed track with '{}'", targetTags[tagsIndex])
                    if (targetTags.size > 1) {
                        tagsIndex++
                    }
                }
                // store new tracks to mediaPackage
                val fileName = getFileNameFromElements(sourceTrack, composedTrack)
                composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
                        composedTrack.identifier, fileName))
                mediaPackage.addDerived(composedTrack, sourceTrack)
            }
        }

        logger.debug("Demux operation completed")
        return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
    }

    @Throws(WorkflowOperationException::class)
    private fun newFlavor(track: Track, flavor: String): MediaPackageElementFlavor {
        val targetFlavor: MediaPackageElementFlavor

        try {
            targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor)
            var flavorType = targetFlavor.type
            var flavorSubtype = targetFlavor.subtype
            // Adjust the target flavor. Make sure to account for partial updates
            if ("*" == flavorType)
                flavorType = track.flavor.type
            if ("*" == flavorSubtype)
                flavorSubtype = track.flavor.subtype
            return MediaPackageElementFlavor(flavorType!!, flavorSubtype!!)
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException(String.format("Target flavor '%s' is malformed", flavor))
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(DemuxWorkflowOperationHandler::class.java)
    }

}
