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

import org.opencastproject.util.data.Collections.list

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.layout.AbsolutePositionLayoutSpec
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.HorizontalCoverageLayoutSpec
import org.opencastproject.composer.layout.LayoutManager
import org.opencastproject.composer.layout.MultiShapeLayout
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.AttachmentSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.JsonObj
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.UUID
import java.util.regex.Pattern

import javax.imageio.ImageIO

/**
 * The workflow definition for handling "composite" operations
 */
class CompositeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
        logger.debug("Running composite workflow operation on workflow {}", workflowInstance.id)

        try {
            return composite(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun composite(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage
        val compositeSettings: CompositeSettings
        try {
            compositeSettings = CompositeSettings(mediaPackage, operation)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unable to parse composite settings because {}", ExceptionUtils.getStackTrace(e))
            return createResult(mediaPackage, Action.SKIP)
        }

        var watermarkAttachment = Option.none<Attachment>()
        val watermarkElements = compositeSettings.watermarkSelector.select(mediaPackage, false)
        if (watermarkElements.size > 1) {
            logger.warn("More than one watermark attachment has been found for compositing, skipping compositing!: {}",
                    watermarkElements)
            return createResult(mediaPackage, Action.SKIP)
        } else if (watermarkElements.size == 0 && compositeSettings.sourceUrlWatermark != null) {
            logger.info("No watermark found from flavor and tags, take watermark from URL {}",
                    compositeSettings.sourceUrlWatermark)
            val urlAttachment = AttachmentImpl()
            urlAttachment.identifier = compositeSettings.watermarkIdentifier

            if (compositeSettings.sourceUrlWatermark.startsWith("http")) {
                urlAttachment.setURI(UrlSupport.uri(compositeSettings.sourceUrlWatermark))
            } else {
                var `in`: InputStream? = null
                try {
                    `in` = UrlSupport.url(compositeSettings.sourceUrlWatermark).openStream()
                    val imageUrl = workspace!!.putInCollection(COLLECTION, compositeSettings.watermarkIdentifier + "."
                            + FilenameUtils.getExtension(compositeSettings.sourceUrlWatermark), `in`)
                    urlAttachment.setURI(imageUrl)
                } catch (e: Exception) {
                    logger.warn("Unable to read watermark source url {}: {}", compositeSettings.sourceUrlWatermark, e)
                    throw WorkflowOperationException("Unable to read watermark source url " + compositeSettings.sourceUrlWatermark, e)
                } finally {
                    IOUtils.closeQuietly(`in`)
                }
            }
            watermarkAttachment = Option.option(urlAttachment)
        } else if (watermarkElements.size == 0 && compositeSettings.sourceUrlWatermark == null) {
            logger.info("No watermark to composite")
        } else {
            for (a in watermarkElements)
                watermarkAttachment = Option.option(a)
        }

        val upperElements = compositeSettings.upperTrackSelector.select(mediaPackage, false)
        val lowerElements = compositeSettings.lowerTrackSelector.select(mediaPackage, false)

        // There is only a single track to work with.
        if (upperElements.size == 1 && lowerElements.size == 0 || upperElements.size == 0 && lowerElements.size == 1) {
            for (t in upperElements)
                compositeSettings.singleTrack = t
            for (t in lowerElements)
                compositeSettings.singleTrack = t
            return handleSingleTrack(mediaPackage, operation, compositeSettings, watermarkAttachment)
        } else {
            // Look for upper elements matching the tags and flavor
            if (upperElements.size > 1) {
                logger.warn("More than one upper track has been found for compositing, skipping compositing!: {}",
                        upperElements)
                return createResult(mediaPackage, Action.SKIP)
            } else if (upperElements.size == 0) {
                logger.warn("No upper track has been found for compositing, skipping compositing!")
                return createResult(mediaPackage, Action.SKIP)
            }

            for (t in upperElements) {
                compositeSettings.upperTrack = t
            }

            // Look for lower elements matching the tags and flavor
            if (lowerElements.size > 1) {
                logger.warn("More than one lower track has been found for compositing, skipping compositing!: {}",
                        lowerElements)
                return createResult(mediaPackage, Action.SKIP)
            } else if (lowerElements.size == 0) {
                logger.warn("No lower track has been found for compositing, skipping compositing!")
                return createResult(mediaPackage, Action.SKIP)
            }

            for (t in lowerElements) {
                compositeSettings.lowerTrack = t
            }

            return handleMultipleTracks(mediaPackage, operation, compositeSettings, watermarkAttachment)
        }
    }

    /**
     * This class collects and calculates all of the relevant data for doing a composite whether there is a single or two
     * video tracks.
     */
    private inner class CompositeSettings @Throws(WorkflowOperationException::class)
    internal constructor(mediaPackage: MediaPackage, operation: WorkflowOperationInstance) {

        val sourceAudioName: String?
        private val sourceTagsUpper: String?
        private val sourceFlavorUpper: String?
        private val sourceTagsLower: String?
        private val sourceFlavorLower: String?
        private val sourceTagsWatermark: String
        private val sourceFlavorWatermark: String?
        val sourceUrlWatermark: String?
        private val targetTagsOption: String
        private val targetFlavorOption: String?
        private val encodingProfile: String?
        private var layoutMultipleString: String? = null
        private val layoutSingleString: String?
        private val outputResolution: String?
        var outputBackground: String? = null
            private set

        val upperTrackSelector: AbstractMediaPackageElementSelector<Track> = TrackSelector()
        val lowerTrackSelector: AbstractMediaPackageElementSelector<Track> = TrackSelector()
        val watermarkSelector: AbstractMediaPackageElementSelector<Attachment> = AttachmentSelector()

        val watermarkIdentifier: String
        var watermarkLayout = Option.none()
            private set

        private val multiSourceLayouts = ArrayList<HorizontalCoverageLayoutSpec>()
        var singleSourceLayout: HorizontalCoverageLayoutSpec? = null
            private set

        var upperTrack: Track? = null
        var lowerTrack: Track? = null
        var singleTrack: Track? = null

        var outputResolutionSource: String? = null
            private set
        var outputDimension: Dimension? = null
            private set

        val profile: EncodingProfile?

        val targetTags: List<String>

        var targetFlavor: MediaPackageElementFlavor? = null
            private set

        init {
            // Check which tags have been configured
            sourceAudioName = StringUtils.trimToNull(operation.getConfiguration(SOURCE_AUDIO_NAME))
            sourceTagsUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_UPPER))
            sourceFlavorUpper = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_UPPER))
            sourceTagsLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_LOWER))
            sourceFlavorLower = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_LOWER))
            sourceTagsWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_WATERMARK))
            sourceFlavorWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_WATERMARK))
            sourceUrlWatermark = StringUtils.trimToNull(operation.getConfiguration(SOURCE_URL_WATERMARK))

            targetTagsOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS))
            targetFlavorOption = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR))
            encodingProfile = StringUtils.trimToNull(operation.getConfiguration(ENCODING_PROFILE))

            layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_MULTIPLE))
            if (layoutMultipleString == null) {
                layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT))
            }

            if (layoutMultipleString != null && !layoutMultipleString!!.contains(";")) {
                layoutMultipleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_PREFIX + layoutMultipleString!!))
            }

            layoutSingleString = StringUtils.trimToNull(operation.getConfiguration(LAYOUT_SINGLE))

            outputResolution = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_RESOLUTION))
            outputBackground = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_BACKGROUND))

            watermarkIdentifier = UUID.randomUUID().toString()

            if (outputBackground == null) {
                outputBackground = DEFAULT_BG_COLOR
            }

            if (layoutMultipleString != null) {
                val multipleLayouts = parseMultipleLayouts(layoutMultipleString)
                multiSourceLayouts.addAll(multipleLayouts.a)
                watermarkLayout = multipleLayouts.b
            }

            if (layoutSingleString != null) {
                val singleLayouts = parseSingleLayouts(layoutSingleString)
                singleSourceLayout = singleLayouts.a
                watermarkLayout = singleLayouts.b
            }

            // Check that source audio is upper, lower or use a combination of both
            if (sourceAudioName != null && !sourceAudioOption.matcher(sourceAudioName).matches())
                throw WorkflowOperationException("sourceAudioName if used, must be either upper, lower or both!")

            // Find the encoding profile
            if (encodingProfile == null)
                throw WorkflowOperationException("Encoding profile must be set!")

            profile = composerService!!.getProfile(encodingProfile)
            if (profile == null)
                throw WorkflowOperationException("Encoding profile '$encodingProfile' was not found")

            // Target tags
            targetTags = asList(targetTagsOption)

            // Target flavor
            if (targetFlavorOption == null)
                throw WorkflowOperationException("Target flavor must be set!")

            // Output resolution
            if (outputResolution == null)
                throw WorkflowOperationException("Output resolution must be set!")

            if (outputResolution == OUTPUT_RESOLUTION_LOWER || outputResolution == OUTPUT_RESOLUTION_UPPER) {
                outputResolutionSource = outputResolution
            } else {
                outputResolutionSource = OUTPUT_RESOLUTION_FIXED
                try {
                    val outputResolutionArray = StringUtils.split(outputResolution, "x")
                    if (outputResolutionArray.size != 2) {
                        throw WorkflowOperationException("Invalid format of output resolution!")
                    }
                    outputDimension = Dimension.dimension(Integer.parseInt(outputResolutionArray[0]),
                            Integer.parseInt(outputResolutionArray[1]))
                } catch (e: Exception) {
                    throw WorkflowOperationException("Unable to parse output resolution!", e)
                }

            }

            // Make sure either one of tags or flavor for the upper source are provided
            if (sourceTagsUpper == null && sourceFlavorUpper == null) {
                throw IllegalArgumentException(
                        "No source tags or flavor for the upper video have been specified, not matching anything")
            }

            // Make sure either one of tags or flavor for the lower source are provided
            if (sourceTagsLower == null && sourceFlavorLower == null) {
                throw IllegalArgumentException(
                        "No source tags or flavor for the lower video have been specified, not matching anything")
            }

            try {
                targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)
                if ("*" == targetFlavor!!.type || "*" == targetFlavor!!.subtype)
                    throw WorkflowOperationException("Target flavor must have a type and a subtype, '*' are not allowed!")
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Target flavor '$targetFlavorOption' is malformed")
            }

            // Support legacy "source-flavor-upper" option
            if (sourceFlavorUpper != null) {
                try {
                    upperTrackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorUpper))
                } catch (e: IllegalArgumentException) {
                    throw WorkflowOperationException("Source upper flavor '$sourceFlavorUpper' is malformed")
                }

            }

            // Support legacy "source-flavor-lower" option
            if (sourceFlavorLower != null) {
                try {
                    lowerTrackSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorLower))
                } catch (e: IllegalArgumentException) {
                    throw WorkflowOperationException("Source lower flavor '$sourceFlavorLower' is malformed")
                }

            }

            // Support legacy "source-flavor-watermark" option
            if (sourceFlavorWatermark != null) {
                try {
                    watermarkSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(sourceFlavorWatermark))
                } catch (e: IllegalArgumentException) {
                    throw WorkflowOperationException("Source watermark flavor '$sourceFlavorWatermark' is malformed")
                }

            }

            // Select the source tags upper
            for (tag in asList(sourceTagsUpper)) {
                upperTrackSelector.addTag(tag)
            }

            // Select the source tags lower
            for (tag in asList(sourceTagsLower)) {
                lowerTrackSelector.addTag(tag)
            }

            // Select the watermark source tags
            for (tag in asList(sourceTagsWatermark)) {
                watermarkSelector.addTag(tag)
            }
        }

        @Throws(WorkflowOperationException::class)
        private fun parseMultipleLayouts(
                layoutString: String): Tuple<List<HorizontalCoverageLayoutSpec>, Option<AbsolutePositionLayoutSpec>> {
            try {
                val layouts = StringUtils.split(layoutString, ";")
                if (layouts.size < 2)
                    throw WorkflowOperationException(
                            "Multiple layout doesn't contain the required layouts for (lower, upper, optional watermark)")

                val multipleLayouts = list(
                        Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[0])),
                        Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[1])))

                var watermarkLayout: AbsolutePositionLayoutSpec? = null
                if (layouts.size > 2)
                    watermarkLayout = Serializer.absolutePositionLayoutSpec(JsonObj.jsonObj(layouts[2]))

                return Tuple.tuple(multipleLayouts, Option.option(watermarkLayout))
            } catch (e: Exception) {
                throw WorkflowOperationException("Unable to parse layout!", e)
            }

        }

        @Throws(WorkflowOperationException::class)
        private fun parseSingleLayouts(
                layoutString: String): Tuple<HorizontalCoverageLayoutSpec, Option<AbsolutePositionLayoutSpec>> {
            try {
                val layouts = StringUtils.split(layoutString, ";")
                if (layouts.size < 1)
                    throw WorkflowOperationException(
                            "Single layout doesn't contain the required layouts for (video, optional watermark)")

                val singleLayout = Serializer
                        .horizontalCoverageLayoutSpec(JsonObj.jsonObj(layouts[0]))

                var watermarkLayout: AbsolutePositionLayoutSpec? = null
                if (layouts.size > 1)
                    watermarkLayout = Serializer.absolutePositionLayoutSpec(JsonObj.jsonObj(layouts[1]))

                return Tuple.tuple(singleLayout, Option.option(watermarkLayout))
            } catch (e: Exception) {
                throw WorkflowOperationException("Unable to parse layout!", e)
            }

        }

        fun getMultiSourceLayouts(): List<HorizontalCoverageLayoutSpec>? {
            return multiSourceLayouts
        }

        companion object {

            /** Use a fixed output resolution  */
            val OUTPUT_RESOLUTION_FIXED = "fixed"

            /** Use resolution of lower part as output resolution  */
            val OUTPUT_RESOLUTION_LOWER = "lower"

            /** Use resolution of upper part as output resolution  */
            val OUTPUT_RESOLUTION_UPPER = "upper"
        }
    }

    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun handleSingleTrack(mediaPackage: MediaPackage, operation: WorkflowOperationInstance,
                                  compositeSettings: CompositeSettings, watermarkAttachment: Option<Attachment>): WorkflowOperationResult {

        if (compositeSettings.singleSourceLayout == null) {
            throw WorkflowOperationException("Single video layout must be set! Please verify that you have a "
                    + LAYOUT_SINGLE + " property in your composite operation in your workflow definition.")
        }

        try {
            val videoStreams = TrackSupport.byType(compositeSettings.singleTrack!!.streams,
                    VideoStream::class.java)
            if (videoStreams.size == 0) {
                logger.warn("No video stream available to compose! {}", compositeSettings.singleTrack)
                return createResult(mediaPackage, Action.SKIP)
            }

            // Read the video dimensions from the mediapackage stream information
            val videoDimension = Dimension.dimension(videoStreams[0].frameWidth!!, videoStreams[0].frameHeight!!)

            // Create the video layout definitions
            val shapes = ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>()
            shapes.add(0, Tuple.tuple(videoDimension, compositeSettings.singleSourceLayout))

            // Determine dimension of output
            var outputDimension: Dimension? = null
            val outputResolutionSource = compositeSettings.outputResolutionSource
            if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_FIXED) {
                outputDimension = compositeSettings.outputDimension
            } else if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_LOWER) {
                outputDimension = videoDimension
            } else if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_UPPER) {
                outputDimension = videoDimension
            }

            // Calculate the single layout
            val multiShapeLayout = LayoutManager
                    .multiShapeLayout(outputDimension, shapes)

            // Create the laid out element for the videos
            val lowerLaidOutElement = LaidOutElement<Track>(compositeSettings.singleTrack,
                    multiShapeLayout.shapes[0])

            // Create the optionally laid out element for the watermark
            val watermarkOption = createWatermarkLaidOutElement(compositeSettings,
                    outputDimension, watermarkAttachment)

            val compositeJob = composerService!!.composite(outputDimension, Option
                    .none(), lowerLaidOutElement, watermarkOption, compositeSettings.profile!!
                    .identifier, compositeSettings.outputBackground, compositeSettings.sourceAudioName)

            // Wait for the jobs to return
            if (!waitForStatus(compositeJob).isSuccess)
                throw WorkflowOperationException("The composite job did not complete successfully")

            if (compositeJob.payload.length > 0) {

                val compoundTrack = MediaPackageElementParser.getFromXml(compositeJob.payload) as Track

                compoundTrack.setURI(workspace!!.moveTo(compoundTrack.getURI(), mediaPackage.identifier.toString(),
                        compoundTrack.identifier,
                        "composite." + FilenameUtils.getExtension(compoundTrack.getURI().toString())))

                // Adjust the target tags
                for (tag in compositeSettings.targetTags) {
                    logger.trace("Tagging compound track with '{}'", tag)
                    compoundTrack.addTag(tag)
                }

                // Adjust the target flavor.
                compoundTrack.flavor = compositeSettings.targetFlavor
                logger.debug("Compound track has flavor '{}'", compoundTrack.flavor)

                // store new tracks to mediaPackage
                mediaPackage.add(compoundTrack)
                val result = createResult(mediaPackage, Action.CONTINUE, compositeJob.queueTime!!)
                logger.debug("Composite operation completed")
                return result
            } else {
                logger.info("Composite operation unsuccessful, no payload returned: {}", compositeJob)
                return createResult(mediaPackage, Action.SKIP)
            }
        } finally {
            if (compositeSettings.sourceUrlWatermark != null)
                workspace!!.deleteFromCollection(
                        COLLECTION,
                        compositeSettings.watermarkIdentifier + "."
                                + FilenameUtils.getExtension(compositeSettings.sourceUrlWatermark))
        }
    }

    @Throws(WorkflowOperationException::class)
    private fun createWatermarkLaidOutElement(compositeSettings: CompositeSettings,
                                              outputDimension: Dimension?, watermarkAttachment: Option<Attachment>): Option<LaidOutElement<Attachment>> {
        var watermarkOption = Option.none<LaidOutElement<Attachment>>()
        if (watermarkAttachment.isSome && compositeSettings.watermarkLayout.isSome) {
            val image: BufferedImage
            try {
                val watermarkFile = workspace!!.get(watermarkAttachment.get().getURI())
                image = ImageIO.read(watermarkFile)
            } catch (e: Exception) {
                logger.warn("Unable to read the watermark image attachment {}: {}", watermarkAttachment.get().getURI(), e)
                throw WorkflowOperationException("Unable to read the watermark image attachment", e)
            }

            val imageDimension = Dimension.dimension(image.width, image.height)
            val watermarkShapes = ArrayList<Tuple<Dimension, AbsolutePositionLayoutSpec>>()
            watermarkShapes.add(0, Tuple.tuple(imageDimension, compositeSettings.watermarkLayout.get()))
            val watermarkLayout = LayoutManager.absoluteMultiShapeLayout(outputDimension,
                    watermarkShapes)
            watermarkOption = Option.some(LaidOutElement(watermarkAttachment.get(), watermarkLayout
                    .shapes[0]))
        }
        return watermarkOption
    }

    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun handleMultipleTracks(mediaPackage: MediaPackage, operation: WorkflowOperationInstance,
                                     compositeSettings: CompositeSettings, watermarkAttachment: Option<Attachment>): WorkflowOperationResult {
        if (compositeSettings.getMultiSourceLayouts() == null || compositeSettings.getMultiSourceLayouts()!!.size == 0) {
            throw WorkflowOperationException(
                    "Multi video layout must be set! Please verify that you have a "
                            + LAYOUT_MULTIPLE
                            + " or "
                            + LAYOUT
                            + " property in your composite operation in your workflow definition to be able to handle multiple videos")
        }

        try {
            val upperTrack = compositeSettings.upperTrack
            val lowerTrack = compositeSettings.lowerTrack
            val layouts = compositeSettings.getMultiSourceLayouts()

            val upperVideoStreams = TrackSupport.byType(upperTrack!!.streams, VideoStream::class.java)
            if (upperVideoStreams.size == 0) {
                logger.warn("No video stream available in the upper track! {}", upperTrack)
                return createResult(mediaPackage, Action.SKIP)
            }

            val lowerVideoStreams = TrackSupport.byType(lowerTrack!!.streams, VideoStream::class.java)
            if (lowerVideoStreams.size == 0) {
                logger.warn("No video stream available in the lower track! {}", lowerTrack)
                return createResult(mediaPackage, Action.SKIP)
            }

            // Read the video dimensions from the mediapackage stream information
            val upperDimensions = Dimension.dimension(upperVideoStreams[0].frameWidth!!,
                    upperVideoStreams[0].frameHeight!!)
            val lowerDimensions = Dimension.dimension(lowerVideoStreams[0].frameWidth!!,
                    lowerVideoStreams[0].frameHeight!!)

            // Determine dimension of output
            var outputDimension: Dimension? = null
            val outputResolutionSource = compositeSettings.outputResolutionSource
            if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_FIXED) {
                outputDimension = compositeSettings.outputDimension
            } else if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_LOWER) {
                outputDimension = lowerDimensions
            } else if (outputResolutionSource == CompositeSettings.OUTPUT_RESOLUTION_UPPER) {
                outputDimension = upperDimensions
            }

            // Create the video layout definitions
            val shapes = ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>()
            shapes.add(0, Tuple.tuple(lowerDimensions, layouts!![0]))
            shapes.add(1, Tuple.tuple(upperDimensions, layouts[1]))

            // Calculate the layout
            val multiShapeLayout = LayoutManager
                    .multiShapeLayout(outputDimension, shapes)

            // Create the laid out element for the videos
            val lowerLaidOutElement = LaidOutElement(lowerTrack, multiShapeLayout.shapes[0])
            val upperLaidOutElement = LaidOutElement(upperTrack, multiShapeLayout.shapes[1])

            // Create the optionally laid out element for the watermark
            val watermarkOption = createWatermarkLaidOutElement(compositeSettings,
                    outputDimension, watermarkAttachment)

            val compositeJob = composerService!!.composite(outputDimension, Option
                    .option(upperLaidOutElement), lowerLaidOutElement, watermarkOption, compositeSettings.profile!!
                    .identifier, compositeSettings.outputBackground, compositeSettings.sourceAudioName)

            // Wait for the jobs to return
            if (!waitForStatus(compositeJob).isSuccess)
                throw WorkflowOperationException("The composite job did not complete successfully")

            if (compositeJob.payload.length > 0) {

                val compoundTrack = MediaPackageElementParser.getFromXml(compositeJob.payload) as Track

                compoundTrack.setURI(workspace!!.moveTo(compoundTrack.getURI(), mediaPackage.identifier.toString(),
                        compoundTrack.identifier,
                        "composite." + FilenameUtils.getExtension(compoundTrack.getURI().toString())))

                // Adjust the target tags
                for (tag in compositeSettings.targetTags) {
                    logger.trace("Tagging compound track with '{}'", tag)
                    compoundTrack.addTag(tag)
                }

                // Adjust the target flavor.
                compoundTrack.flavor = compositeSettings.targetFlavor
                logger.debug("Compound track has flavor '{}'", compoundTrack.flavor)

                // store new tracks to mediaPackage
                mediaPackage.add(compoundTrack)
                val result = createResult(mediaPackage, Action.CONTINUE, compositeJob.queueTime!!)
                logger.debug("Composite operation completed")
                return result
            } else {
                logger.info("Composite operation unsuccessful, no payload returned: {}", compositeJob)
                return createResult(mediaPackage, Action.SKIP)
            }
        } finally {
            if (compositeSettings.sourceUrlWatermark != null)
                workspace!!.deleteFromCollection(
                        COLLECTION,
                        compositeSettings.watermarkIdentifier + "."
                                + FilenameUtils.getExtension(compositeSettings.sourceUrlWatermark))
        }
    }

    companion object {

        private val COLLECTION = "composite"

        private val SOURCE_AUDIO_NAME = "source-audio-name"
        private val SOURCE_TAGS_UPPER = "source-tags-upper"
        private val SOURCE_FLAVOR_UPPER = "source-flavor-upper"
        private val SOURCE_TAGS_LOWER = "source-tags-lower"
        private val SOURCE_FLAVOR_LOWER = "source-flavor-lower"
        private val SOURCE_TAGS_WATERMARK = "source-tags-watermark"
        private val SOURCE_FLAVOR_WATERMARK = "source-flavor-watermark"
        private val SOURCE_URL_WATERMARK = "source-url-watermark"

        private val TARGET_TAGS = "target-tags"
        private val TARGET_FLAVOR = "target-flavor"
        private val ENCODING_PROFILE = "encoding-profile"

        private val LAYOUT = "layout"
        private val LAYOUT_MULTIPLE = "layout-multiple"
        private val LAYOUT_SINGLE = "layout-single"
        private val LAYOUT_PREFIX = "layout-"

        private val OUTPUT_RESOLUTION = "output-resolution"
        private val OUTPUT_BACKGROUND = "output-background"
        private val DEFAULT_BG_COLOR = "black"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CompositeWorkflowOperationHandler::class.java)

        /** The legal options for SOURCE_AUDIO_NAME  */
        private val sourceAudioOption = Pattern.compile(
                ComposerService.LOWER + "|" + ComposerService.UPPER + "|" + ComposerService.BOTH, Pattern.CASE_INSENSITIVE)
    }
}
