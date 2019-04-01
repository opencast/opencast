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
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.Map.Entry

/**
 * The workflow definition for handling "concat" operations
 */
class ConcatWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    internal enum class SourceType {
        None, PrefixedFile, NumberedFile
    }

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
        logger.debug("Running concat workflow operation on workflow {}", workflowInstance.id)

        try {
            return concat(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun concat(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage

        val trackSelectors = getTrackSelectors(operation)
        val outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION))
        val outputFrameRate = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_FRAMERATE))
        val encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE))
        val sameCodec = BooleanUtils.toBoolean(operation.getConfiguration(SAME_CODEC))

        // Skip the worklow if no source-flavors or tags has been configured
        if (trackSelectors.isEmpty()) {
            logger.warn("No source-tags or source-flavors has been set.")
            return createResult(mediaPackage, Action.SKIP)
        }

        val targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS))
        val targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR))

        // Target tags
        val targetTags = asList(targetTagsOption)

        // Target flavor
        if (targetFlavorOption == null)
            throw WorkflowOperationException("Target flavor must be set!")

        // Find the encoding profile
        if (encodingProfile == null)
            throw WorkflowOperationException("Encoding profile must be set!")

        val profile = composerService!!.getProfile(encodingProfile)
                ?: throw WorkflowOperationException("Encoding profile '$encodingProfile' was not found")

        // Output resolution - if not keeping dimensions the same, it must be set
        if (!sameCodec && outputResolution == null)
            throw WorkflowOperationException("Output resolution must be set!")

        var outputDimension: Dimension? = null
        if (!sameCodec) { // Ignore resolution if same Codec - no scaling
            if (outputResolution!!.startsWith(OUTPUT_PART_PREFIX)) {
                if (!trackSelectors.keys.contains(
                                Integer.parseInt(outputResolution.substring(OUTPUT_PART_PREFIX.length)))) {
                    throw WorkflowOperationException("Output resolution part not set!")
                }
            } else {
                try {
                    val outputResolutionArray = StringUtils.split(outputResolution, "x")
                    if (outputResolutionArray.size != 2) {
                        throw WorkflowOperationException("Invalid format of output resolution!")
                    }
                    outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
                            Integer.parseInt(outputResolutionArray[1]))
                } catch (e: WorkflowOperationException) {
                    throw e
                } catch (e: Exception) {
                    throw WorkflowOperationException("Unable to parse output resolution!", e)
                }

            }
        }

        var fps = -1.0f
        // Ignore fps if same Codec - no scaling
        if (!sameCodec && StringUtils.isNotEmpty(outputFrameRate)) {
            if (StringUtils.startsWith(outputFrameRate, OUTPUT_PART_PREFIX)) {
                if (!NumberUtils.isCreatable(outputFrameRate.substring(OUTPUT_PART_PREFIX.length)) || !trackSelectors.keys.contains(Integer.parseInt(
                                outputFrameRate.substring(OUTPUT_PART_PREFIX.length)))) {
                    throw WorkflowOperationException("Output frame rate part not set or invalid!")
                }
            } else if (NumberUtils.isCreatable(outputFrameRate)) {
                fps = NumberUtils.toFloat(outputFrameRate)
            } else {
                throw WorkflowOperationException("Unable to parse output frame rate!")
            }
        }

        var targetFlavor: MediaPackageElementFlavor? = null
        try {
            targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)
            if ("*" == targetFlavor!!.type || "*" == targetFlavor.subtype)
                throw WorkflowOperationException("Target flavor must have a type and a subtype, '*' are not allowed!")
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException("Target flavor '$targetFlavorOption' is malformed")
        }

        val tracks = ArrayList<Track>()
        for ((key, value) in trackSelectors) {
            var tracksForSelector = value.a.select(mediaPackage, false)
            val currentFlavor = StringUtils.join(*value.a.getFlavors())
            val currentTag = StringUtils.join(*value.a.getTags())

            // Cannot mix prefix-number tracks with numbered files
            // PREFIXED_FILES must have multiple files, but numbered file can skip the operation if there is only one
            if (trackSelectors.size == 1) {
                // NUMBERED FILES will have one trackSelector only and multiple sorted files in it
                val list = ArrayList(tracksForSelector)
                list.sort { left, right ->
                    val l = File(left.getURI().getPath()).getName() // Get and compare basename only, getPath() for mock
                    val r = File(right.getURI().getPath()).getName()
                    l.compareTo(r)
                }
                tracksForSelector = list
            } else if (tracksForSelector.size > 1) {
                logger.warn("More than one track has been found with flavor '{}' and/or tag '{}' for concat operation, " + "skipping concatenation!", currentFlavor, currentTag)
                return createResult(mediaPackage, Action.SKIP)
            } else if (tracksForSelector.size == 0 && value.b) {
                logger.warn("No track has been found with flavor '{}' and/or tag '{}' for concat operation, " + "skipping concatenation!", currentFlavor, currentTag)
                return createResult(mediaPackage, Action.SKIP)
            } else if (tracksForSelector.size == 0 && !value.b) {
                logger.info("No track has been found with flavor '{}' and/or tag '{}' for concat operation, skipping track!",
                        currentFlavor, currentTag)
                continue
            }

            for (t in tracksForSelector) {
                tracks.add(t)
                val videoStreams = TrackSupport.byType(t.streams, VideoStream::class.java)
                if (videoStreams.size == 0) {
                    logger.info("No video stream available in the track with flavor {}! {}", currentFlavor, t)
                    return createResult(mediaPackage, Action.SKIP)
                }
                if (StringUtils.startsWith(outputResolution, OUTPUT_PART_PREFIX)
                        && NumberUtils.isCreatable(outputResolution!!.substring(OUTPUT_PART_PREFIX.length))
                        && key == Integer.parseInt(outputResolution.substring(OUTPUT_PART_PREFIX.length))) {
                    outputDimension = Dimension(videoStreams[0].frameWidth!!, videoStreams[0].frameHeight!!)
                    if (!value.b) {
                        logger.warn("Output resolution track {} must be mandatory, skipping concatenation!", outputResolution)
                        return createResult(mediaPackage, Action.SKIP)
                    }
                }
                if (fps <= 0 && StringUtils.startsWith(outputFrameRate, OUTPUT_PART_PREFIX)
                        && NumberUtils.isCreatable(outputFrameRate.substring(OUTPUT_PART_PREFIX.length))
                        && key == Integer.parseInt(outputFrameRate.substring(OUTPUT_PART_PREFIX.length))) {
                    fps = videoStreams[0].frameRate!!
                }
            }
        }

        if (tracks.size == 0) {
            logger.warn("No tracks found for concating operation, skipping concatenation!")
            return createResult(mediaPackage, Action.SKIP)
        } else if (tracks.size == 1) {
            val track = tracks[0].clone() as Track
            track.identifier = null
            addNewTrack(mediaPackage, track, targetTags, targetFlavor)
            logger.info("At least two tracks are needed for the concating operation, skipping concatenation!")
            return createResult(mediaPackage, Action.SKIP)
        }

        val concatJob: Job
        if (fps > 0) {
            concatJob = composerService!!.concat(profile.identifier, outputDimension,
                    fps, sameCodec, *tracks.toTypedArray())
        } else {
            concatJob = composerService!!.concat(profile.identifier, outputDimension,
                    sameCodec, *tracks.toTypedArray())
        }

        // Wait for the jobs to return
        if (!waitForStatus(concatJob).isSuccess)
            throw WorkflowOperationException("The concat job did not complete successfully")

        if (concatJob.payload.length > 0) {

            val concatTrack = MediaPackageElementParser.getFromXml(concatJob.payload) as Track

            concatTrack.setURI(workspace!!.moveTo(concatTrack.getURI(), mediaPackage.identifier.toString(),
                    concatTrack.identifier, "concat." + FilenameUtils.getExtension(concatTrack.getURI().toString())))

            addNewTrack(mediaPackage, concatTrack, targetTags, targetFlavor)

            val result = createResult(mediaPackage, Action.CONTINUE, concatJob.queueTime!!)
            logger.debug("Concat operation completed")
            return result
        } else {
            logger.info("concat operation unsuccessful, no payload returned: {}", concatJob)
            return createResult(mediaPackage, Action.SKIP)
        }
    }

    private fun addNewTrack(mediaPackage: MediaPackage, track: Track, targetTags: List<String>,
                            targetFlavor: MediaPackageElementFlavor?) {
        // Adjust the target tags
        for (tag in targetTags) {
            logger.trace("Tagging compound track with '{}'", tag)
            track.addTag(tag)
        }

        // Adjust the target flavor.
        track.flavor = targetFlavor
        logger.debug("Compound track has flavor '{}'", track.flavor)

        mediaPackage.add(track)
    }

    @Throws(WorkflowOperationException::class)
    private fun getTrackSelectors(operation: WorkflowOperationInstance): Map<Int, Tuple<TrackSelector, Boolean>> {
        val trackSelectors = HashMap<Int, Tuple<TrackSelector, Boolean>>()
        var flavorType = SourceType.None
        var srcFlavor: String? = null

        // Search config for SOURCE_FLAVOR_NUMBERED_FILES and SOURCE_FLAVOR_PREFIX
        for (key in operation.configurationKeys) {
            if (key.startsWith(SOURCE_FLAVOR_PREFIX) || key.startsWith(SOURCE_TAGS_PREFIX)) {
                if (flavorType == SourceType.None) {
                    flavorType = SourceType.PrefixedFile
                } else if (flavorType != SourceType.PrefixedFile) {
                    throw WorkflowOperationException(
                            "Cannot mix source prefix flavor/tags with source numbered files - use one type of selector only")
                }
            }

            if (key == SOURCE_FLAVOR_NUMBERED_FILES) { // Search config for SOURCE_FLAVORS_NUMBERED_FILES
                srcFlavor = operation.getConfiguration(key)
                if (flavorType == SourceType.None) {
                    flavorType = SourceType.NumberedFile
                    srcFlavor = operation.getConfiguration(key)
                } else if (flavorType != SourceType.NumberedFile) {
                    throw WorkflowOperationException(
                            "Cannot mix source prefix flavor/tags with source numbered files - use one type of selector only")
                }
            }
        }

        // if is SOURCE_FLAVOR_NUMBERED_FILES, do not use prefixed (tags or flavor)
        if (srcFlavor != null) { // Numbered files has only one selector
            val number = 0
            var selectorTuple = trackSelectors[number]
            selectorTuple = Tuple.tuple(TrackSelector(), true)
            val trackSelector = selectorTuple.a
            trackSelector.addFlavor(srcFlavor)
            trackSelectors[number] = selectorTuple
            return trackSelectors
        }

        // Prefix only
        for (key in operation.configurationKeys) {
            var tags: String? = null
            var flavor: String? = null
            var mandatory: Boolean? = true
            var number = -1
            if (key.startsWith(SOURCE_TAGS_PREFIX) && !key.endsWith(MANDATORY_SUFFIX)) {
                number = NumberUtils.toInt(key.substring(SOURCE_TAGS_PREFIX.length), -1)
                tags = operation.getConfiguration(key)
                mandatory = BooleanUtils.toBooleanObject(operation.getConfiguration(SOURCE_TAGS_PREFIX + Integer.toString(number) + MANDATORY_SUFFIX))
            } else if (key.startsWith(SOURCE_FLAVOR_PREFIX) && !key.endsWith(MANDATORY_SUFFIX)) {
                number = NumberUtils.toInt(key.substring(SOURCE_FLAVOR_PREFIX.length), -1)
                flavor = operation.getConfiguration(key)
                mandatory = BooleanUtils.toBooleanObject(operation.getConfiguration(SOURCE_FLAVOR_PREFIX + Integer.toString(number) + MANDATORY_SUFFIX))
            }

            if (number < 0)
                continue

            var selectorTuple: Tuple<TrackSelector, Boolean>? = trackSelectors[number]
            if (selectorTuple == null) {
                selectorTuple = Tuple.tuple(TrackSelector(), BooleanUtils.toBooleanDefaultIfNull(mandatory, false))
            } else {
                selectorTuple = Tuple.tuple(selectorTuple.a,
                        selectorTuple.b || BooleanUtils.toBooleanDefaultIfNull(mandatory, false))
            }
            val trackSelector = selectorTuple.a
            if (StringUtils.isNotBlank(tags)) {
                for (tag in StringUtils.split(tags, ",")) {
                    trackSelector.addTag(tag)
                }
            }
            if (StringUtils.isNotBlank(flavor)) {
                try {
                    trackSelector.addFlavor(flavor)
                } catch (e: IllegalArgumentException) {
                    throw WorkflowOperationException("Source flavor '$flavor' is malformed")
                }

            }

            trackSelectors[number] = selectorTuple
        }
        return trackSelectors
    }

    companion object {

        private val SOURCE_TAGS_PREFIX = "source-tags-part-"
        private val SOURCE_FLAVOR_PREFIX = "source-flavor-part-"
        private val MANDATORY_SUFFIX = "-mandatory"

        private val TARGET_TAGS = "target-tags"
        private val TARGET_FLAVOR = "target-flavor"

        private val ENCODING_PROFILE = "encoding-profile"
        private val OUTPUT_RESOLUTION = "output-resolution"
        private val OUTPUT_FRAMERATE = "output-framerate"
        private val OUTPUT_PART_PREFIX = "part-"

        /** Concatenate flavored media by lexicographical order -eg v01.mp4, v02.mp4, etc  */
        private val SOURCE_FLAVOR_NUMBERED_FILES = "source-flavor-numbered-files"
        /**
         * If codec and dimension are the same in all the src files, do not scale and transcode, just put all the content into
         * the container
         */
        private val SAME_CODEC = "same-codec"


        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ConcatWorkflowOperationHandler::class.java)
    }
}
