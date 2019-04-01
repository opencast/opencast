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
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.api.WorkflowOperationTagUtil
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException

/**
 * The <tt>prepare media</tt> operation will make sure that media where audio and video track come in separate files
 * will be muxed prior to further processing.
 */
class PrepareAVWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
    protected fun setComposerService(composerService: ComposerService) {
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
        logger.debug("Running a/v muxing workflow operation on workflow {}", workflowInstance.id)
        try {
            return mux(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Merges audio and video track of the selected flavor and adds it to the media package. If there is nothing to mux, a
     * new track with the target flavor is created (pointing to the original url).
     *
     * @param src
     * The source media package
     * @param operation
     * the mux workflow operation
     * @return the operation result containing the updated mediapackage
     * @throws EncoderException
     * if encoding fails
     * @throws IOException
     * if read/write operations from and to the workspace fail
     * @throws NotFoundException
     * if the workspace does not contain the requested element
     */
    @Throws(EncoderException::class, WorkflowOperationException::class, NotFoundException::class, MediaPackageException::class, IOException::class)
    private fun mux(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage

        // Read the configuration properties
        val sourceFlavorName = StringUtils.trimToNull(operation.getConfiguration("source-flavor"))
        val targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"))
        val targetTrackFlavorName = StringUtils.trimToNull(operation.getConfiguration("target-flavor"))
        var muxEncodingProfileName: String? = StringUtils.trimToNull(operation.getConfiguration("mux-encoding-profile"))
        var audioVideoEncodingProfileName: String? = StringUtils.trimToNull(operation.getConfiguration("audio-video-encoding-profile"))
        var videoOnlyEncodingProfileName: String? = StringUtils.trimToNull(operation.getConfiguration("video-encoding-profile"))
        var audioOnlyEncodingProfileName: String? = StringUtils.trimToNull(operation.getConfiguration("audio-encoding-profile"))

        val tagDiff = WorkflowOperationTagUtil.createTagDiff(targetTrackTags)

        // Make sure the source flavor is properly set
        if (sourceFlavorName == null)
            throw IllegalStateException("Source flavor must be specified")
        val sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName)

        // Make sure the target flavor is properly set
        if (targetTrackFlavorName == null)
            throw IllegalStateException("Target flavor must be specified")
        val targetFlavor = MediaPackageElementFlavor.parseFlavor(targetTrackFlavorName)

        // Reencode when there is no need for muxing?
        var rewrite = true
        if (StringUtils.trimToNull(operation.getConfiguration(OPT_REWRITE)) != null) {
            rewrite = java.lang.Boolean.parseBoolean(operation.getConfiguration(OPT_REWRITE))
        }

        val audioMuxingSourceFlavors = StringUtils.trimToNull(operation.getConfiguration(OPT_AUDIO_MUXING_SOURCE_FLAVORS))

        // Select those tracks that have matching flavors
        val tracks = mediaPackage.getTracks(sourceFlavor)

        var audioTrack: Track? = null
        var videoTrack: Track? = null

        when (tracks.size) {
            0 -> {
                logger.info("No audio/video tracks with flavor '{}' found to prepare", sourceFlavor)
                return createResult(mediaPackage, Action.CONTINUE)
            }
            1 -> {
                videoTrack = tracks[0]
                if (!tracks[0].hasAudio() && tracks[0].hasVideo() && audioMuxingSourceFlavors != null) {
                    audioTrack = findAudioTrack(tracks[0], mediaPackage, audioMuxingSourceFlavors)
                } else {
                    audioTrack = tracks[0]
                }
            }
            2 -> for (track in tracks) {
                if (track.hasAudio() && !track.hasVideo()) {
                    audioTrack = track
                } else if (!track.hasAudio() && track.hasVideo()) {
                    videoTrack = track
                } else {
                    throw WorkflowOperationException("Multiple tracks with competing audio/video streams and flavor '"
                            + sourceFlavor + "' found")
                }
            }
            else -> {
                logger.error("More than two tracks with flavor {} found. No idea what we should be doing", sourceFlavor)
                throw WorkflowOperationException("More than two tracks with flavor '$sourceFlavor' found")
            }
        }

        var job: Job? = null
        var composedTrack: Track? = null

        // Make sure we have a matching combination
        if (audioTrack == null && videoTrack != null) {
            if (rewrite) {
                logger.info("Encoding video only track {} to work version", videoTrack)
                if (videoOnlyEncodingProfileName == null)
                    videoOnlyEncodingProfileName = PREPARE_VONLY_PROFILE
                // Find the encoding profile to make sure the given profile exists
                val profile = composerService!!.getProfile(videoOnlyEncodingProfileName)
                        ?: throw IllegalStateException("Encoding profile '$videoOnlyEncodingProfileName' was not found")
                composedTrack = prepare(videoTrack, mediaPackage, videoOnlyEncodingProfileName)
            } else {
                composedTrack = videoTrack.clone() as Track
                composedTrack.identifier = null
                mediaPackage.add(composedTrack)
            }
        } else if (videoTrack == null && audioTrack != null) {
            if (rewrite) {
                logger.info("Encoding audio only track {} to work version", audioTrack)
                if (audioOnlyEncodingProfileName == null)
                    audioOnlyEncodingProfileName = PREPARE_AONLY_PROFILE
                // Find the encoding profile to make sure the given profile exists
                val profile = composerService!!.getProfile(audioOnlyEncodingProfileName)
                        ?: throw IllegalStateException("Encoding profile '$audioOnlyEncodingProfileName' was not found")
                composedTrack = prepare(audioTrack, mediaPackage, audioOnlyEncodingProfileName)
            } else {
                composedTrack = audioTrack.clone() as Track
                composedTrack.identifier = null
                mediaPackage.add(composedTrack)
            }
        } else if (audioTrack === videoTrack) {
            if (rewrite) {
                logger.info("Encoding audiovisual track {} to work version", videoTrack)
                if (audioVideoEncodingProfileName == null)
                    audioVideoEncodingProfileName = PREPARE_AV_PROFILE
                // Find the encoding profile to make sure the given profile exists
                val profile = composerService!!.getProfile(audioVideoEncodingProfileName)
                        ?: throw IllegalStateException("Encoding profile '$audioVideoEncodingProfileName' was not found")
                composedTrack = prepare(videoTrack, mediaPackage, audioVideoEncodingProfileName)
            } else {
                composedTrack = videoTrack!!.clone() as Track
                composedTrack.identifier = null
                mediaPackage.add(composedTrack)
            }
        } else {
            logger.info("Muxing audio and video only track {} to work version", videoTrack)

            if (audioTrack!!.hasVideo()) {
                logger.info("Stripping video from track {}", audioTrack)
                audioTrack = prepare(audioTrack, null, PREPARE_AONLY_PROFILE)
            }

            if (muxEncodingProfileName == null)
                muxEncodingProfileName = MUX_AV_PROFILE

            // Find the encoding profile
            val profile = composerService!!.getProfile(muxEncodingProfileName)
                    ?: throw IllegalStateException("Encoding profile '$muxEncodingProfileName' was not found")

            job = composerService!!.mux(videoTrack, audioTrack, profile.identifier)
            if (!waitForStatus(job).isSuccess) {
                throw WorkflowOperationException("Muxing video track " + videoTrack + " and audio track " + audioTrack
                        + " failed")
            }
            composedTrack = MediaPackageElementParser.getFromXml(job!!.payload) as Track
            mediaPackage.add(composedTrack)
            val fileName = getFileNameFromElements(videoTrack, composedTrack)
            composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
                    composedTrack.identifier, fileName))
        }

        var timeInQueue: Long = 0
        if (job != null) {
            // add this receipt's queue time to the total
            timeInQueue = job.queueTime!!
        }

        // Update the track's flavor
        composedTrack.flavor = targetFlavor
        logger.debug("Composed track has flavor '{}'", composedTrack.flavor)

        WorkflowOperationTagUtil.applyTagDiff(tagDiff, composedTrack)
        return createResult(mediaPackage, Action.CONTINUE, timeInQueue)
    }

    /**
     * Prepares a video track. If the mediapackage is specified, the prepared track will be added to it.
     *
     * @param videoTrack
     * the track containing the video
     * @param mediaPackage
     * the mediapackage
     * @return the rewritten track
     * @throws WorkflowOperationException
     * @throws NotFoundException
     * @throws IOException
     * @throws EncoderException
     * @throws MediaPackageException
     */
    @Throws(WorkflowOperationException::class, NotFoundException::class, IOException::class, EncoderException::class, MediaPackageException::class)
    private fun prepare(videoTrack: Track, mediaPackage: MediaPackage?, encodingProfile: String): Track {
        var composedTrack: Track? = null
        logger.info("Encoding video only track {} to work version", videoTrack)
        val job = composerService!!.encode(videoTrack, encodingProfile)
        if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException("Rewriting container for video track $videoTrack failed")
        }
        composedTrack = MediaPackageElementParser.getFromXml(job.payload) as Track
        if (mediaPackage != null) {
            mediaPackage.add(composedTrack)
            val fileName = getFileNameFromElements(videoTrack, composedTrack)

            // Note that the composed track must have an ID before being moved to the mediapackage in the working file
            // repository. This ID is generated when the track is added to the mediapackage. So the track must be added
            // to the mediapackage before attempting to move the file.
            composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
                    composedTrack.identifier, fileName))
        }
        return composedTrack
    }

    /**
     * Finds a suitable audio track from the mediapackage by scanning a source flavor sequence
     *
     * @param videoTrack
     * the video track
     * @param mediaPackage
     * the mediapackage
     * @param audioMuxingSourceFlavors
     * sequence of source flavors where an audio track should be searched for
     * @return the found audio track
     */
    private fun findAudioTrack(videoTrack: Track, mediaPackage: MediaPackage, audioMuxingSourceFlavors: String?): Track? {

        if (audioMuxingSourceFlavors != null) {
            var type: String?
            var subtype: String?
            for (flavorStr in audioMuxingSourceFlavors.split("[\\s,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (!flavorStr.isEmpty()) {
                    var flavor: MediaPackageElementFlavor? = null
                    try {
                        flavor = MediaPackageElementFlavor.parseFlavor(flavorStr)
                    } catch (e: IllegalArgumentException) {
                        logger.error("The parameter {} contains an invalid flavor: {}", OPT_AUDIO_MUXING_SOURCE_FLAVORS, flavorStr)
                        throw e
                    }

                    type = if (QUESTION_MARK == flavor!!.type) videoTrack.flavor.type else flavor.type
                    subtype = if (QUESTION_MARK == flavor.subtype) videoTrack.flavor.subtype else flavor.subtype
                    // Recreate the (possibly) modified flavor
                    flavor = MediaPackageElementFlavor(type!!, subtype!!)
                    for (track in mediaPackage.getTracks(flavor)) {
                        if (track.hasAudio()) {
                            logger.info("Audio muxing found audio source {} with flavor {}", track, track.flavor)
                            return track
                        }
                    }
                }
            }
        }
        return null
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler::class.java)
        private val QUESTION_MARK = "?"

        /** Name of the 'encode to a/v work copy' encoding profile  */
        val PREPARE_AV_PROFILE = "av.work"

        /** Name of the muxing encoding profile  */
        val MUX_AV_PROFILE = "mux-av.work"

        /** Name of the 'encode to audio only work copy' encoding profile  */
        val PREPARE_AONLY_PROFILE = "audio-only.work"

        /** Name of the 'encode to video only work copy' encoding profile  */
        val PREPARE_VONLY_PROFILE = "video-only.work"

        /** Name of the 'rewrite' configuration key  */
        val OPT_REWRITE = "rewrite"

        /** Name of audio muxing configuration key  */
        val OPT_AUDIO_MUXING_SOURCE_FLAVORS = "audio-muxing-source-flavors"
    }

}
