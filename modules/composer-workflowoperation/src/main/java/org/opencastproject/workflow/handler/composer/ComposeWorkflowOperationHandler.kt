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
import org.opencastproject.composer.api.EncodingProfile.MediaType
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
import java.util.ArrayList
import java.util.HashMap

/**
 * The workflow definition for handling "compose" operations
 */
class ComposeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
        logger.debug("Running compose workflow operation on workflow {}", workflowInstance.id)

        try {
            return encode(workflowInstance.mediaPackage, workflowInstance.currentOperation)
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
    private fun encode(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage

        // Check which tags have been configured
        val sourceTagsOption = StringUtils.trimToNull(operation.getConfiguration("source-tags"))
        val targetTagsOption = StringUtils.trimToNull(operation.getConfiguration("target-tags"))
        val sourceFlavorOption = StringUtils.trimToNull(operation.getConfiguration("source-flavor"))
        val sourceFlavorsOption = StringUtils.trimToNull(operation.getConfiguration("source-flavors"))
        val targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration("target-flavor"))
        val tagsAndFlavorsOption = java.lang.Boolean
                .parseBoolean(StringUtils.trimToNull(operation.getConfiguration("tags-and-flavors")))

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

        // Find the encoding profile
        val profilesOption = StringUtils.trimToNull(operation.getConfiguration("encoding-profiles"))
        val profiles = ArrayList<EncodingProfile>()
        for (profileName in asList(profilesOption)) {
            val profile = composerService!!.getProfile(profileName)
                    ?: throw WorkflowOperationException("Encoding profile '$profileName' was not found")
            profiles.add(profile)
        }

        // Support legacy "encoding-profile" option
        val profileOption = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"))
        if (StringUtils.isNotBlank(profileOption)) {
            val profileId = StringUtils.trim(profileOption)
            val profile = composerService!!.getProfile(profileId)
                    ?: throw WorkflowOperationException("Encoding profile '$profileId' was not found")
            profiles.add(profile)
        }

        // Make sure there is at least one profile
        if (profiles.isEmpty())
            throw WorkflowOperationException("No encoding profile was specified")

        // Audio / Video only?
        val audioOnlyConfig = StringUtils.trimToNull(operation.getConfiguration("audio-only"))
        val videoOnlyConfig = StringUtils.trimToNull(operation.getConfiguration("video-only"))
        val audioOnly = audioOnlyConfig != null && java.lang.Boolean.parseBoolean(audioOnlyConfig)
        val videoOnly = videoOnlyConfig != null && java.lang.Boolean.parseBoolean(videoOnlyConfig)

        // Target tags
        val targetTags = asList(targetTagsOption)

        // Target flavor
        var targetFlavor: MediaPackageElementFlavor? = null
        if (StringUtils.isNotBlank(targetFlavorOption)) {
            try {
                targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Target flavor '$targetFlavorOption' is malformed")
            }

        }

        // Look for elements matching the tag
        val elements = elementSelector.select(mediaPackage, tagsAndFlavorsOption)

        val processOnlyOneConfig = StringUtils.trimToNull(operation.getConfiguration("process-first-match-only"))
        val processOnlyOne = processOnlyOneConfig != null && java.lang.Boolean.parseBoolean(processOnlyOneConfig)

        // Encode all tracks found
        var totalTimeInQueue: Long = 0
        val encodingJobs = HashMap<Job, JobInformation>()
        for (track in elements) {

            // Skip audio/video only mismatches
            if (audioOnly && track.hasVideo()) {
                logger.info("Skipping encoding of '{}', since it contains a video stream", track)
                continue
            } else if (videoOnly && track.hasAudio()) {
                logger.info("Skipping encoding of '{}', since it containsa an audio stream", track)
                continue
            }

            // Encode the track with all profiles
            for (profile in profiles) {

                // Check if the track supports the output type of the profile
                val outputType = profile.outputType
                if (outputType == MediaType.Audio && !track.hasAudio()) {
                    logger.info("Skipping encoding of '{}', since it lacks an audio stream", track)
                    continue
                } else if (outputType == MediaType.Visual && !track.hasVideo()) {
                    logger.info("Skipping encoding of '{}', since it lacks a video stream", track)
                    continue
                }

                logger.info("Encoding track {} using encoding profile '{}'", track, profile)

                // Start encoding and wait for the result
                encodingJobs[composerService!!.encode(track, profile.identifier)] = JobInformation(track, profile)

                if (processOnlyOne)
                    break
            }
        }

        if (encodingJobs.isEmpty()) {
            logger.info("No matching tracks found")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Wait for the jobs to return
        if (!waitForStatus(*encodingJobs.keys.toTypedArray()).isSuccess) {
            throw WorkflowOperationException("One of the encoding jobs did not complete successfully")
        }

        // Process the result
        for ((job, value) in encodingJobs) {
            val track = value.track

            // add this receipt's queue time to the total
            totalTimeInQueue += job.queueTime!!
            // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
            if (job.payload.length > 0) {
                val composedTrack = MediaPackageElementParser.getFromXml(job.payload) as Track

                // Adjust the target tags
                for (tag in targetTags) {
                    logger.trace("Tagging composed track with '{}'", tag)
                    composedTrack.addTag(tag)
                }

                // Adjust the target flavor. Make sure to account for partial updates
                if (targetFlavor != null) {
                    var flavorType = targetFlavor.type
                    var flavorSubtype = targetFlavor.subtype
                    if ("*" == flavorType)
                        flavorType = track!!.flavor.type
                    if ("*" == flavorSubtype)
                        flavorSubtype = track!!.flavor.subtype
                    composedTrack.flavor = MediaPackageElementFlavor(flavorType!!, flavorSubtype!!)
                    logger.debug("Composed track has flavor '{}'", composedTrack.flavor)
                }

                // store new tracks to mediaPackage
                mediaPackage.addDerived(composedTrack, track!!)
                val fileName = getFileNameFromElements(track, composedTrack)
                composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
                        composedTrack.identifier, fileName))
            }
        }

        val result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
        logger.debug("Compose operation completed")
        return result
    }

    /**
     * This class is used to store context information for the jobs.
     */
    private class JobInformation internal constructor(track: Track, profile: EncodingProfile) {

        /**
         * Returns the track.
         *
         * @return the track
         */
        val track: Track? = null
        /**
         * Returns the profile.
         *
         * @return the profile
         */
        val profile: EncodingProfile? = null

        init {
            this.track = track
            this.profile = profile
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler::class.java)
    }

}
