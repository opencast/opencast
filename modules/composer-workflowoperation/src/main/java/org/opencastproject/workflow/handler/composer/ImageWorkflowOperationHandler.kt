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

import com.entwinemedia.fn.Equality.hash
import com.entwinemedia.fn.Prelude.chuck
import com.entwinemedia.fn.Prelude.unexhaustiveMatchError
import com.entwinemedia.fn.Stream.`$`
import com.entwinemedia.fn.parser.Parsers.character
import com.entwinemedia.fn.parser.Parsers.many
import com.entwinemedia.fn.parser.Parsers.opt
import com.entwinemedia.fn.parser.Parsers.space
import com.entwinemedia.fn.parser.Parsers.symbol
import com.entwinemedia.fn.parser.Parsers.token
import com.entwinemedia.fn.parser.Parsers.yield
import java.lang.String.format
import org.opencastproject.util.EqualsUtil.eq

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.MediaPackageSupport.Filters
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.JobUtil
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.PathSupport
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.data.Collections
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Fx
import com.entwinemedia.fn.P2
import com.entwinemedia.fn.Prelude
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.StreamFold
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings
import com.entwinemedia.fn.parser.Parser
import com.entwinemedia.fn.parser.Parsers
import com.entwinemedia.fn.parser.Result

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.IllegalFormatException
import java.util.UUID

/**
 * The workflow definition for handling "image" operations
 */
class ImageWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the composer service
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
    override fun start(wi: WorkflowInstance, ctx: JobContext): WorkflowOperationResult {
        logger.debug("Running image workflow operation on {}", wi)
        try {
            val e = Extractor(this, configure(wi.mediaPackage, wi.currentOperation))
            return e.main(MediaPackageSupport.copy(wi.mediaPackage))
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Computation within the context of a [Cfg].
     */
    internal class Extractor(private val handler: ImageWorkflowOperationHandler, private val cfg: Cfg) {

        /** Run the extraction.  */
        @Throws(WorkflowOperationException::class)
        fun main(mp: MediaPackage): WorkflowOperationResult {
            if (cfg.sourceTracks.size == 0) {
                logger.info("No source tracks found in media package {}, skipping operation", mp.identifier)
                return handler.createResult(mp, Action.SKIP)
            }
            // start image extraction jobs
            val extractions = `$`(cfg.sourceTracks).bind(object : Fn<Track, Stream<Extraction>>() {
                override fun apply(t: Track): Stream<Extraction> {
                    val p = limit(t, cfg.positions)
                    if (p.size != cfg.positions.size) {
                        logger.warn("Could not apply all configured positions to track $t")
                    } else {
                        logger.info(format("Extracting images from %s at position %s", t, `$`(p).mkString(", ")))
                    }
                    // create one extraction per encoding profile
                    return `$`(cfg.profiles).map(object : Fn<EncodingProfile, Extraction>() {
                        override fun apply(profile: EncodingProfile): Extraction {
                            return Extraction(extractImages(t, profile, p), t, profile, p)
                        }
                    })
                }
            }).toList()
            val extractionJobs = concatJobs(extractions)
            val extractionResult = JobUtil.waitForJobs(handler.serviceRegistry, extractionJobs)
            if (extractionResult!!.isSuccess) {
                // all extractions were successful; iterate them
                for (extraction in extractions) {
                    val images = getImages(extraction.job)
                    val expectedNrOfImages = extraction.positions.size
                    if (images.size == expectedNrOfImages) {
                        // post process images
                        for (image in `$`(images).zip(extraction.positions)) {
                            adjustMetadata(extraction, image.get1())
                            if (image.get1().identifier == null) image.get1().identifier = UUID.randomUUID().toString()
                            mp.addDerived(image.get1(), extraction.track)
                            val fileName = createFileName(
                                    extraction.profile.suffix, extraction.track.getURI(), image.get2())
                            moveToWorkspace(mp, image.get1(), fileName)
                        }
                    } else {
                        // less images than expected have been extracted
                        throw WorkflowOperationException(
                                format("Only %s of %s images have been extracted from track %s",
                                        images.size, expectedNrOfImages, extraction.track))
                    }
                }
                return handler.createResult(mp, Action.CONTINUE, JobUtil.sumQueueTime(extractionJobs))
            } else {
                throw WorkflowOperationException("Image extraction failed")
            }
        }

        /**
         * Adjust flavor, tags, mime type of `image` according to the
         * configuration and the extraction.
         */
        fun adjustMetadata(extraction: Extraction, image: Attachment) {
            // Adjust the target flavor. Make sure to account for partial updates
            for (flavor in cfg.targetImageFlavor) {
                val flavorType = if (eq("*", flavor.type))
                    extraction.track.flavor.type
                else
                    flavor.type
                val flavorSubtype = if (eq("*", flavor.subtype))
                    extraction.track.flavor.subtype
                else
                    flavor.subtype
                image.flavor = MediaPackageElementFlavor(flavorType!!, flavorSubtype!!)
                logger.debug("Resulting image has flavor '{}'", image.flavor)
            }
            // Set the mime type
            try {
                image.mimeType = MimeTypes.fromURI(image.getURI())
            } catch (e: UnknownFileTypeException) {
                logger.warn("Mime type unknown for file {}. Setting none.", image.getURI(), e)
            }

            // Add tags
            for (tag in cfg.targetImageTags) {
                logger.trace("Tagging image with '{}'", tag)
                image.addTag(tag)
            }
        }

        /** Create a file name for the extracted image.  */
        fun createFileName(suffix: String, trackUri: URI, pos: MediaPosition): String {
            val trackBaseName = FilenameUtils.getBaseName(trackUri.path)
            val format: String
            when (pos.type) {
                ImageWorkflowOperationHandler.PositionType.Seconds -> format = cfg.targetBaseNameFormatSecond.getOr(trackBaseName + "_%.3fs%s")
                ImageWorkflowOperationHandler.PositionType.Percentage -> format = cfg.targetBaseNameFormatPercent.getOr(trackBaseName + "_%.1fp%s")
                else -> throw unexhaustiveMatchError()
            }
            return formatFileName(format, pos.position, suffix)
        }

        /** Move the extracted `image` to its final location in the workspace and rename it to `fileName`.  */
        private fun moveToWorkspace(mp: MediaPackage, image: Attachment, fileName: String) {
            try {
                image.setURI(handler.workspace!!.moveTo(
                        image.getURI(),
                        mp.identifier.toString(),
                        image.identifier,
                        fileName))
            } catch (e: Exception) {
                chuck<Any>(WorkflowOperationException(e))
            }

        }

        /** Start a composer job to extract images from a track at the given positions.  */
        private fun extractImages(track: Track, profile: EncodingProfile, positions: List<MediaPosition>): Job {
            val p = `$`(positions).map(object : Fn<MediaPosition, Double>() {
                override fun apply(mediaPosition: MediaPosition): Double? {
                    return toSeconds(track, mediaPosition, cfg.endMargin.toDouble())
                }
            }).toList()
            try {
                return handler.composerService!!.image(track, profile.identifier, *Collections.toDoubleArray(p))
            } catch (e: Exception) {
                return chuck(WorkflowOperationException("Error starting image extraction job", e))
            }

        }
    }

    /**
     * Describes the extraction of a list of images from a track, extracted after a certain encoding profile.
     * Track -> (profile, positions)
     */
    internal class Extraction private constructor(
            /** The extraction job.  */
            private val job: Job,
            /** The track to extract from.  */
            private val track: Track,
            /** The encoding profile to use for extraction.  */
            private val profile: EncodingProfile,
            /** Media positions.  */
            private val positions: List<MediaPosition>)

    // ** ** **

    /**
     * The WOH's configuration options.
     */
    internal class Cfg(
            /** List of source tracks, with duration.  */
            private val sourceTracks: List<Track>,
            private val positions: List<MediaPosition>,
            private val profiles: List<EncodingProfile>,
            private val targetImageFlavor: Opt<MediaPackageElementFlavor>,
            private val targetImageTags: List<String>,
            private val targetBaseNameFormatSecond: Opt<String>,
            private val targetBaseNameFormatPercent: Opt<String>,
            private val endMargin: Long)

    /** Get and parse the configuration options.  */
    @Throws(WorkflowOperationException::class)
    private fun configure(mp: MediaPackage, woi: WorkflowOperationInstance): Cfg {
        val profiles = getOptConfig(woi, OPT_PROFILES).toStream().bind(asList.toFn())
                .map(fetchProfile(composerService)).toList()
        val targetImageTags = getOptConfig(woi, OPT_TARGET_TAGS).toStream().bind(asList.toFn()).toList()
        val targetImageFlavor = getOptConfig(woi, OPT_TARGET_FLAVOR).map(MediaPackageElementFlavor.parseFlavor.toFn())
        val sourceTracks: List<Track>
        run {
            // get the source flavors
            val sourceFlavors = getOptConfig(woi, OPT_SOURCE_FLAVORS).toStream()
                    .bind(Strings.splitCsv)
                    .append(getOptConfig(woi, OPT_SOURCE_FLAVOR))
                    .map(MediaPackageElementFlavor.parseFlavor.toFn())
            // get the source tags
            val sourceTags = getOptConfig(woi, OPT_SOURCE_TAGS).toStream().bind(Strings.splitCsv)
            // fold both into a selector
            val trackSelector = sourceTags.apply(tagFold<Track, TrackSelector>(sourceFlavors.apply(flavorFold(TrackSelector()))))
            // select the tracks based on source flavors and tags and skip those that don't have video
            sourceTracks = `$`(trackSelector.select(mp, true))
                    .filter(Filters.hasVideo.toFn())
                    .each(object : Fx<Track>() {
                        override fun apply(track: Track) {
                            if (track.duration == null) {
                                chuck<Any>(WorkflowOperationException(format("Track %s cannot tell its duration", track)))
                            }
                        }
                    }).toList()
        }
        val positions = parsePositions(getConfig(woi, OPT_POSITIONS))
        val endMargin = getOptConfig(woi, OPT_END_MARGIN).bind(Strings.toLong).getOr(END_MARGIN_DEFAULT)
        //
        return Cfg(sourceTracks,
                positions,
                profiles,
                targetImageFlavor,
                targetImageTags,
                getTargetBaseNameFormat(woi, OPT_TARGET_BASE_NAME_FORMAT_SECOND),
                getTargetBaseNameFormat(woi, OPT_TARGET_BASE_NAME_FORMAT_PERCENT),
                endMargin)
    }

    /** Validate a target base name format.  */
    private fun getTargetBaseNameFormat(woi: WorkflowOperationInstance, formatName: String): Opt<String> {
        return getOptConfig(woi, formatName).each(validateTargetBaseNameFormat(formatName))
    }

    // ** ** **

    /**
     * Parse media position parameter strings.
     */
    internal object MediaPositionParser {

        val number = token(Parsers.dbl)
        val seconds = number.bind(object : Fn<Double, Parser<MediaPosition>>() {
            override fun apply(p: Double?): Parser<MediaPosition> {
                return yield(MediaPosition(PositionType.Seconds, p!!))
            }
        })
        val percentage = number.bind(Parsers.ignore(symbol("%"))).bind(object : Fn<Double, Parser<MediaPosition>>() {
            override fun apply(p: Double?): Parser<MediaPosition> {
                return yield(MediaPosition(PositionType.Percentage, p!!))
            }
        })
        val comma = token(character(','))
        val ws = token(space)
        val position = percentage.or(seconds)

        /** Main parser.  */
        val positions = position.bind(object : Fn<MediaPosition, Parser<List<MediaPosition>>>() {
            // first position
            override fun apply(first: MediaPosition): Parser<List<MediaPosition>> {
                // following
                return many(opt(comma).bind(Parsers.ignorePrevious(position)))
                        .bind(object : Fn<List<MediaPosition>, Parser<List<MediaPosition>>>() {
                            override fun apply(rest: List<MediaPosition>): Parser<List<MediaPosition>> {
                                return yield(`$`(first).append(rest).toList())
                            }
                        })
            }
        })
    }

    @Throws(WorkflowOperationException::class)
    private fun parsePositions(time: String): List<MediaPosition> {
        val r = MediaPositionParser.positions.parse(time)
        return if (r.isDefined && r.rest.isEmpty()) {
            r.result
        } else {
            throw WorkflowOperationException(format("Cannot parse time string %s.", time))
        }
    }

    internal enum class PositionType {
        Percentage, Seconds
    }

    /**
     * A position in time in a media file.
     */
    internal class MediaPosition(private val type: PositionType, private val position: Double) {

        override fun hashCode(): Int {
            return hash(position, type)
        }

        override fun equals(that: Any?): Boolean {
            return this === that || that is MediaPosition && eqFields((that as MediaPosition?)!!)
        }

        private fun eqFields(that: MediaPosition): Boolean {
            return position == that.position && eq(type, that.type)
        }

        override fun toString(): String {
            return format("MediaPosition(%s, %s)", type, position)
        }
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ImageWorkflowOperationHandler::class.java)

        // legacy option
        val OPT_SOURCE_FLAVOR = "source-flavor"
        val OPT_SOURCE_FLAVORS = "source-flavors"
        val OPT_SOURCE_TAGS = "source-tags"
        val OPT_PROFILES = "encoding-profile"
        val OPT_POSITIONS = "time"
        val OPT_TARGET_FLAVOR = "target-flavor"
        val OPT_TARGET_TAGS = "target-tags"
        val OPT_TARGET_BASE_NAME_FORMAT_SECOND = "target-base-name-format-second"
        val OPT_TARGET_BASE_NAME_FORMAT_PERCENT = "target-base-name-format-percent"
        val OPT_END_MARGIN = "end-margin"

        private val END_MARGIN_DEFAULT: Long = 100

        // ** ** **

        /**
         * Format a filename and make it "safe".
         *
         * @see org.opencastproject.util.PathSupport.toSafeName
         */
        internal fun formatFileName(format: String, position: Double, suffix: String): String {
            // #toSafeName will be applied to the file name anyway when moving to the working file repository
            // but doing it here make the tests more readable and useful for documentation
            return PathSupport.toSafeName(format(format, position, suffix))
        }


        /** Concat the jobs of a list of extraction objects.  */
        private fun concatJobs(extractions: List<Extraction>): List<Job> {
            return `$`(extractions).map(object : Fn<Extraction, Job>() {
                override fun apply(extraction: Extraction): Job {
                    return extraction.job
                }
            }).toList()
        }

        /** Get the images (payload) from a job.  */
        private fun getImages(job: Job): List<Attachment> {
            val images: List<Attachment>
            try {
                images = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Attachment>
            } catch (e: MediaPackageException) {
                return chuck(e)
            }

            return if (!images.isEmpty()) {
                images
            } else {
                chuck(WorkflowOperationException("Job did not extract any images"))
            }
        }

        /** Limit the list of media positions to those that fit into the length of the track.  */
        internal fun limit(track: Track, positions: List<MediaPosition>): List<MediaPosition> {
            val duration = track.duration!!
            return `$`(positions).filter(object : Fn<MediaPosition, Boolean>() {
                override fun apply(p: MediaPosition): Boolean? {
                    return !(eq(p.type, PositionType.Seconds) && (p.position >= duration || p.position < 0.0) || eq(p.type, PositionType.Percentage) && (p.position < 0.0 || p.position > 100.0))
                }
            }).toList()
        }

        /**
         * Convert a `position` into seconds in relation to the given track.
         * *Attention:* The function does not check if the calculated absolute position is within
         * the bounds of the tracks length.
         */
        internal fun toSeconds(track: Track, position: MediaPosition, endMarginMs: Double): Double {
            val durationMs = track.duration!!
            val posMs: Double
            when (position.type) {
                ImageWorkflowOperationHandler.PositionType.Percentage -> posMs = durationMs * position.position / 100.0
                ImageWorkflowOperationHandler.PositionType.Seconds -> posMs = position.position * 1000.0
                else -> throw unexhaustiveMatchError()
            }
            // limit maximum position to Xms before the end of the video
            return if (Math.abs(durationMs - posMs) >= endMarginMs)
                posMs / 1000.0
            else
                Math.max(0.0, durationMs.toDouble() - endMarginMs) / 1000.0
        }

        // ** ** **

        /** Create a fold that folds flavors into a media package element selector.  */
        fun <E : MediaPackageElement, S : AbstractMediaPackageElementSelector<E>> flavorFold(selector: S): StreamFold<MediaPackageElementFlavor, S> {
            return StreamFold.foldl(selector, object : Fn2<S, MediaPackageElementFlavor, S>() {
                override fun apply(sum: S, flavor: MediaPackageElementFlavor): S {
                    sum.addFlavor(flavor)
                    return sum
                }
            })
        }

        /** Create a fold that folds tags into a media package element selector.  */
        fun <E : MediaPackageElement, S : AbstractMediaPackageElementSelector<E>> tagFold(selector: S): StreamFold<String, S> {
            return StreamFold.foldl(selector, object : Fn2<S, String, S>() {
                override fun apply(sum: S, tag: String): S {
                    sum.addTag(tag)
                    return sum
                }
            })
        }

        /**
         * Fetch a profile from the composer service. Throw a WorkflowOperationException in case the profile
         * does not exist.
         */
        fun fetchProfile(composerService: ComposerService?): Fn<String, EncodingProfile> {
            return object : Fn<String, EncodingProfile>() {
                override fun apply(profileName: String): EncodingProfile {
                    val profile = composerService!!.getProfile(profileName)
                    return profile
                            ?: Prelude.chuck(WorkflowOperationException("Encoding profile '$profileName' was not found"))
                }
            }
        }

        internal fun validateTargetBaseNameFormat(formatName: String): Fx<String> {
            return object : Fx<String>() {
                override fun apply(format: String) {
                    var valid: Boolean
                    try {
                        val name = formatFileName(format, 15.11, ".png")
                        valid = name.contains(".") && name.contains(".png")
                    } catch (e: IllegalFormatException) {
                        valid = false
                    }

                    if (!valid) {
                        chuck<Any>(WorkflowOperationException(format(
                                "%s is not a valid format string for config option %s",
                                format, formatName)))
                    }
                }
            }
        }
    }
}

