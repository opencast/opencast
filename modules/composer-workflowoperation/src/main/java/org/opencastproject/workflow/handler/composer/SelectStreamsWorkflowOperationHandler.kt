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
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationTagUtil
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.String.Companion

class SelectStreamsWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    private enum class AudioMuxing {
        NONE, FORCE, DUPLICATE;

        override fun toString(): String {
            return super.toString().toLowerCase()
        }

        companion object {

            internal fun fromConfigurationString(s: String): AudioMuxing {
                return AudioMuxing.valueOf(s.toUpperCase())
            }
        }
    }

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

    @Throws(WorkflowOperationException::class)
    private fun getProfile(identifier: String): EncodingProfile {
        return composerService!!.getProfile(identifier)
                ?: throw WorkflowOperationException(String.format("couldn't find encoding profile \"%s\"", identifier))
    }

    private enum class SubTrack {
        AUDIO, VIDEO
    }

    /**
     * During our operations, we accumulate new tracks and wait times, for which we have this nice helper class
     */
    private class MuxResult private constructor(private var queueTime: Long, private val tracks: MutableCollection<Track>) {

        internal fun forEachTrack(trackConsumer: Consumer<Track>) {
            tracks.forEach(trackConsumer)
        }

        fun add(jobResult: TrackJobResult) {
            this.queueTime += jobResult.waitTime
            this.tracks.add(jobResult.track)
        }

        fun add(muxResult: MuxResult) {
            this.queueTime += muxResult.queueTime
            this.tracks.addAll(muxResult.tracks)
        }

        companion object {

            internal fun empty(): MuxResult {
                return MuxResult(0L, ArrayList(0))
            }
        }
    }

    private class AugmentedTrack private constructor(private val track: Track, private val hideAudio: Boolean, private val hideVideo: Boolean) {

        internal val flavorType: String?
            get() = track.flavor.type

        internal fun has(t: SubTrack): Boolean {
            return if (t == SubTrack.AUDIO) {
                hasAudio()
            } else {
                hasVideo()
            }
        }

        internal fun hide(t: SubTrack): Boolean {
            return if (t == SubTrack.AUDIO) {
                hideAudio
            } else {
                hideVideo
            }
        }

        internal fun hasAudio(): Boolean {
            return track.hasAudio()
        }

        internal fun hasVideo(): Boolean {
            return track.hasVideo()
        }

        override fun toString(): String {
            return String.format("ID: %s, Flavor: %s [hasAudio %s, hideAudio %s, hasVideo %s, hideVideo: %s]",
                    track.identifier, track.flavor,
                    hasAudio(), hide(SubTrack.AUDIO),
                    hasVideo(), hide(SubTrack.VIDEO)
            )
        }
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        try {
            return doStart(workflowInstance)
        } catch (e: EncoderException) {
            throw WorkflowOperationException(e)
        } catch (e: MediaPackageException) {
            throw WorkflowOperationException(e)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException(e)
        }

    }

    @Throws(WorkflowOperationException::class, EncoderException::class, MediaPackageException::class, NotFoundException::class, IOException::class)
    private fun doStart(workflowInstance: WorkflowInstance): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage

        val sourceFlavor = getConfiguration(workflowInstance, "source-flavor")
                .map(Function<String, Any> { parseFlavor() })
                .orElseThrow({ IllegalStateException("Source flavor must be specified") })

        val targetTrackFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.trimToNull(
                getConfiguration(workflowInstance, "target-flavor")
                        .orElseThrow { IllegalStateException("Target flavor not specified") }))

        val tracks = mediaPackage.getTracks(sourceFlavor)

        if (tracks.size == 0) {
            logger.info("No audio/video tracks with flavor '{}' found to prepare", sourceFlavor)
            return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)
        }

        val augmentedTracks = createAugmentedTracks(tracks, workflowInstance)

        val result = MuxResult.empty()
        // Note that the logic below currently supports at most two input tracks

        if (allNonHidden(augmentedTracks, SubTrack.VIDEO)) {
            // Case 1: We have only tracks with non-hidden video streams. So we keep them all and possibly cut away audio.

            val audioMuxing = getConfiguration(workflowInstance, CONFIG_AUDIO_MUXING)
                    .map<AudioMuxing>(Function<String, AudioMuxing> { AudioMuxing.fromConfigurationString(it) }).orElse(AudioMuxing.NONE)
            val singleAudioTrackOpt = findSingleAudioTrack(augmentedTracks)
            val multipleVideo = augmentedTracks.size > 1

            if (multipleVideo && audioMuxing == AudioMuxing.DUPLICATE && singleAudioTrackOpt.isPresent) {
                /* Case 1.1: We have more than one track, all tracks have non-hidden video streams and exactly one track has
           an audio stream. Audio muxing is set to DUPLICATE, so duplicate the single audio stream into all tracks */
                logger.debug("Duplicate the audio stream of track {} into all tracks", singleAudioTrackOpt.get())
                val singleAudioTrack = singleAudioTrackOpt.get()
                for (t in augmentedTracks) {
                    if (t.track !== singleAudioTrack.track) {
                        val jobResult = mux(t.track, singleAudioTrack.track, mediaPackage)
                        result.add(jobResult)
                    } else {
                        result.add(copyTrack(t.track))
                    }
                }
            } else if (multipleVideo && audioMuxing == AudioMuxing.FORCE && singleAudioTrackOpt.isPresent) {
                /* Case 1.2: We have more than one track, all tracks have non-hidden video streams and exactly one track has
           an audio stream. Audio muxing is set to FORCE, so we enforce that the audio stream is moved to the track
           specified by CONFIG_FORCE_TARGET if not already there */
                logger.debug("Enforce audio stream to be present in track {} only", singleAudioTrackOpt.get())
                val singleAudioTrack = singleAudioTrackOpt.get()
                val forceTargetOpt = getConfiguration(workflowInstance, CONFIG_FORCE_TARGET)
                        .orElse(FORCE_TARGET_DEFAULT)

                val forceTargetTrackOpt = findTrackByFlavorType(augmentedTracks, forceTargetOpt)

                if (!forceTargetTrackOpt.isPresent) {
                    throw IllegalStateException(
                            String.format("\"%s\" set to \"%s\", but target flavor \"%s\" not found!",
                                    CONFIG_AUDIO_MUXING,
                                    AudioMuxing.FORCE, forceTargetOpt))
                }

                val forceTargetTrack = forceTargetTrackOpt.get()

                if (singleAudioTrack.track !== forceTargetTrack.track) {
                    // Copy it over...
                    val muxResult = mux(forceTargetTrack.track, singleAudioTrack.track, mediaPackage)
                    result.add(muxResult)

                    // ...and remove the original
                    val hideAudioResult = hideAudio(singleAudioTrack.track, mediaPackage)
                    result.add(hideAudioResult)
                } else {
                    result.add(copyTrack(singleAudioTrack.track))
                }

                // Just copy the rest of the tracks and remove audio where necessary
                for (augmentedTrack in augmentedTracks) {
                    if (augmentedTrack.track !== singleAudioTrack.track && augmentedTrack.track !== forceTargetTrack.track) {
                        if (augmentedTrack.hasAudio() && augmentedTrack.hide(SubTrack.AUDIO)) {
                            val hideAudioResult = hideAudio(augmentedTrack.track, mediaPackage)
                            result.add(hideAudioResult)
                        } else {
                            result.add(copyTrack(augmentedTrack.track))
                        }
                    }
                }
            } else {
                /* Case 1.3: We have one or more tracks and all tracks have non-hidden video streams. Audio muxing is either
           set to NONE or we don't have a single audio streams as required by DUPLICATE and FORCE.
           In this case, simply remove audio streams where requested or copy the track otherwise */
                val muxResult = muxMultipleVideoTracks(mediaPackage, augmentedTracks)
                result.add(muxResult)
            }
        } else if (allHidden(augmentedTracks, SubTrack.VIDEO)) {
            /* Case 2: No tracks have non-hidden video streams. In this case, simply remove video streams where
          requested or copy the track otherwise */
            for (t in augmentedTracks) {
                if (t.hasAudio()) {
                    if (t.hide(SubTrack.VIDEO)) {
                        val hideVideoResult = hideVideo(t.track, mediaPackage)
                        result.add(hideVideoResult)
                    } else {
                        result.add(copyTrack(t.track))
                    }
                }
            }
        } else {
            /* Case 3: We have one or more tracks where exactly one track has a non-hidden video stream (implied as this
         logic assumes at most two input tracks).
         Considering the audio stream, the track with the non-hidden video stream might also contain an audio stream
         or we have to mux the audio stream from another track into that track */
            val muxResult = muxSingleVideoTrack(mediaPackage, augmentedTracks)
            result.add(muxResult)
        }

        // Update Flavor and add to media package
        result.forEachTrack({ t ->
            t.flavor = MediaPackageElementFlavor(t.flavor.type!!, targetTrackFlavor.subtype!!)
            mediaPackage.add(t)
        })

        // Update Tags here
        getConfiguration(workflowInstance, "target-tags").ifPresent { tags ->
            val tagDiff = WorkflowOperationTagUtil.createTagDiff(tags)
            result.forEachTrack({ t -> WorkflowOperationTagUtil.applyTagDiff(tagDiff, t) })
        }

        return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE, result.queueTime)
    }

    private fun findTrackByFlavorType(augmentedTracks: Collection<AugmentedTrack>,
                                      flavorType: String): Optional<AugmentedTrack> {
        return augmentedTracks.stream().filter { augmentedTrack -> augmentedTrack.flavorType == flavorType }
                .findAny()
    }

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun muxSingleVideoTrack(mediaPackage: MediaPackage, augmentedTracks: Collection<AugmentedTrack>): MuxResult {
        var queueTime = 0L

        val resultingTracks = ArrayList<Track>(0)

        // This method expects exactly one track with a non-hidden video stream. Let's find it.
        val nonHiddenVideo = findNonHidden(augmentedTracks, SubTrack.VIDEO)
                .orElseThrow { IllegalStateException("couldn't find a stream with non-hidden video") }
        // Implicit here is the assumption that there's just _one_ other audio stream. It's written so that
        // we can loosen this assumption later on.
        val nonHiddenAudio = findNonHidden(augmentedTracks, SubTrack.AUDIO)

        // If there's just one non-hidden video stream, and that one has hidden audio, we have to cut that away, too.
        if (nonHiddenVideo.hasAudio() && nonHiddenVideo.hideAudio && (!nonHiddenAudio.isPresent || nonHiddenAudio.get() == nonHiddenVideo)) {
            val jobResult = hideAudio(nonHiddenVideo.track, mediaPackage)
            resultingTracks.add(jobResult.track)
            queueTime += jobResult.waitTime
        } else if (!nonHiddenAudio.isPresent || nonHiddenAudio.get() == nonHiddenVideo) {
            // It could be the case that the non-hidden video stream is also the non-hidden audio stream. In that
            // case, we don't have to mux. But have to clone it.
            val clonedTrack = nonHiddenVideo.track.clone() as Track
            clonedTrack.identifier = null
            resultingTracks.add(clonedTrack)
        } else {
            // Otherwise, we mux!
            val jobResult = mux(nonHiddenVideo.track, nonHiddenAudio.get().track, mediaPackage)
            resultingTracks.add(jobResult.track)
            queueTime += jobResult.waitTime
        }
        return MuxResult(queueTime, resultingTracks)
    }

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun muxMultipleVideoTracks(mediaPackage: MediaPackage, augmentedTracks: Iterable<AugmentedTrack>): MuxResult {
        var queueTime = 0L
        val resultingTracks = ArrayList<Track>(0)
        for (t in augmentedTracks) {
            if (t.hasAudio() && t.hideAudio) {
                // The flavor gets "nulled" in the process. Reverse that so we can treat all tracks equally.
                val previousFlavor = t.track.flavor
                val trackJobResult = hideAudio(t.track, mediaPackage)
                trackJobResult.track.flavor = previousFlavor
                resultingTracks.add(trackJobResult.track)
                queueTime += trackJobResult.waitTime
            } else {
                // Even if we don't modify the track, we clone and re-add it to the MP (since it will be a new track with a
                // different flavor)
                logger.debug("Add clone of track {} to mediapackage {}", t.track.identifier,
                        mediaPackage.identifier)
                val clonedTrack = t.track.clone() as Track
                clonedTrack.identifier = null
                resultingTracks.add(clonedTrack)
            }
        }
        return MuxResult(queueTime, resultingTracks)
    }

    /**
     * Returns the single track that has audio, or an empty `Optional` if either more than one audio track exists, or none exists.
     * @param augmentedTracks List of tracks
     * @return See above.
     */
    private fun findSingleAudioTrack(augmentedTracks: Iterable<AugmentedTrack>): Optional<AugmentedTrack> {
        var result: AugmentedTrack? = null
        for (augmentedTrack in augmentedTracks) {
            if (augmentedTrack.hasAudio() && !augmentedTrack.hideAudio) {
                // Already got an audio track? Aw, then there's more than one! :(
                if (result != null) {
                    return Optional.empty()
                }
                result = augmentedTrack
            }
        }
        return Optional.ofNullable(result)
    }

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun mux(videoTrack: Track, audioTrack: Track, mediaPackage: MediaPackage): TrackJobResult {
        logger.info("Mux video track {} and audio track {}", videoTrack, audioTrack)
        // Find the encoding profile
        val profile = getProfile(MUX_AV_PROFILE)

        val job = composerService!!.mux(videoTrack, audioTrack, profile.identifier)
        if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException(
                    String.format("Muxing video track %s and audio track %s failed", videoTrack, audioTrack))
        }
        val previousFlavor = videoTrack.flavor
        val trackJobResult = processJob(videoTrack, mediaPackage, job)
        trackJobResult.track.flavor = previousFlavor
        return trackJobResult
    }

    private class TrackJobResult private constructor(private val track: Track, private val waitTime: Long)

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun hideVideo(track: Track, mediaPackage: MediaPackage): TrackJobResult {
        logger.info("Remove video streams from track {}", track.identifier)
        return hide(PREPARE_AUDIO_ONLY_PROFILE, track, mediaPackage)
    }

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun hideAudio(track: Track, mediaPackage: MediaPackage): TrackJobResult {
        logger.info("Remove audio streams from track {}", track.identifier)
        return hide(PREPARE_VIDEO_ONLY_PROFILE, track, mediaPackage)
    }

    @Throws(MediaPackageException::class, EncoderException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun hide(encodingProfile: String, track: Track, mediaPackage: MediaPackage): TrackJobResult {
        // Find the encoding profile
        val profile = getProfile(encodingProfile)
        val job = composerService!!.encode(track, profile.identifier)
        if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException(String.format("Rewriting container for video track %s failed", track))
        }
        val previousFlavor = track.flavor
        val trackJobResult = processJob(track, mediaPackage, job)
        trackJobResult.track.flavor = previousFlavor
        return trackJobResult
    }

    @Throws(MediaPackageException::class, NotFoundException::class, IOException::class)
    private fun processJob(track: Track, mediaPackage: MediaPackage, job: Job): TrackJobResult {
        val composedTrack = MediaPackageElementParser.getFromXml(job.payload) as Track
        val fileName = getFileNameFromElements(track, composedTrack)

        // Note that the composed track must have an ID before being moved to the mediapackage in the working file
        // repository. This ID is generated when the track is added to the mediapackage. So the track must be added
        // to the mediapackage before attempting to move the file.
        composedTrack.setURI(workspace!!
                .moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(), composedTrack.identifier,
                        fileName))
        return TrackJobResult(composedTrack, job.queueTime!!)
    }

    private fun findNonHidden(augmentedTracks: Collection<AugmentedTrack>, st: SubTrack): Optional<AugmentedTrack> {
        return augmentedTracks.stream().filter { t -> t.has(st) && !t.hide(st) }.findAny()
    }

    private fun allNonHidden(augmentedTracks: Collection<AugmentedTrack>,
                             st: SubTrack): Boolean {
        return augmentedTracks.stream().noneMatch { t -> !t.has(st) || t.hide(st) }
    }

    private fun allHidden(augmentedTracks: Collection<AugmentedTrack>,
                          st: SubTrack): Boolean {
        return augmentedTracks.stream().noneMatch { t -> t.has(st) && !t.hide(st) }
    }

    private fun trackHidden(instance: WorkflowInstance, subtype: String?, st: SubTrack): Boolean {
        val hideProperty = instance.getConfiguration(constructHideProperty(subtype, st))
        return java.lang.Boolean.parseBoolean(hideProperty)
    }

    private fun createAugmentedTracks(tracks: Array<Track>, instance: WorkflowInstance): List<AugmentedTrack> {
        return Arrays.stream(tracks).map { t ->
            val hideAudio = trackHidden(instance, t.flavor.type, SubTrack.AUDIO)
            val hideVideo = trackHidden(instance, t.flavor.type, SubTrack.VIDEO)
            val result = AugmentedTrack(t, hideAudio, hideVideo)
            logger.debug("AugmentedTrack {}", result)
            result
        }.collect<List<AugmentedTrack>, Any>(Collectors.toList())
    }

    @Throws(WorkflowOperationException::class)
    private fun copyTrack(track: Track): TrackJobResult {
        logger.debug("Create copy of track {}", track)
        val copiedTrack = track.clone() as Track
        copiedTrack.identifier = UUID.randomUUID().toString()
        try {
            // Generate a new filename
            var targetFilename = copiedTrack.identifier
            val extension = FilenameUtils.getExtension(track.getURI().getPath())
            if (!extension.isEmpty()) {
                targetFilename += ".$extension"
            }

            // Copy the files on dis and put them into the working file repository
            val newUri = workspace!!.put(track.mediaPackage.identifier.toString(), copiedTrack.identifier,
                    targetFilename, workspace!!.read(track.getURI()))
            copiedTrack.setURI(newUri)
        } catch (e: IOException) {
            throw WorkflowOperationException(String.format("Error while copying track %s", track.identifier), e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException(String.format("Error while copying track %s", track.identifier), e)
        }

        return TrackJobResult(copiedTrack, 0)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SelectStreamsWorkflowOperationHandler::class.java)

        /** Name of the 'encode to video only work copy' encoding profile  */
        private val PREPARE_VIDEO_ONLY_PROFILE = "video-only.work"

        /** Name of the 'encode to video only work copy' encoding profile  */
        private val PREPARE_AUDIO_ONLY_PROFILE = "audio-only.work"

        /** Name of the muxing encoding profile  */
        private val MUX_AV_PROFILE = "mux-av.work"

        private val CONFIG_AUDIO_MUXING = "audio-muxing"

        private val CONFIG_FORCE_TARGET = "force-target"

        private val FORCE_TARGET_DEFAULT = "presenter"

        private fun getConfiguration(instance: WorkflowInstance, key: String): Optional<String> {
            return Optional.ofNullable(instance.currentOperation.getConfiguration(key)).map(Function<String, String> { StringUtils.trimToNull(it) })
        }

        private fun constructHideProperty(s: String?, st: SubTrack): String {
            return "hide_" + s + "_" + st.toString().toLowerCase()
        }
    }
}
