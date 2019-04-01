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

import com.entwinemedia.fn.Prelude.chuck
import com.entwinemedia.fn.Stream.`$`
import java.lang.String.format
import org.opencastproject.util.JobUtil.getPayload

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageSupport.Filters
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.smil.api.util.SmilUtil
import org.opencastproject.util.JobUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.data.VCell
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.smil.SMILDocument
import org.w3c.dom.smil.SMILElement
import org.w3c.dom.smil.SMILMediaElement
import org.w3c.dom.smil.SMILParElement
import org.xml.sax.SAXException

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.UUID

/**
 * The workflow definition for handling partial import operations
 */
open class PartialImportWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
        logger.debug("Running partial import workflow operation on workflow {}", workflowInstance.id)

        val elementsToClean = ArrayList<MediaPackageElement>()

        try {
            return concat(workflowInstance.mediaPackage, workflowInstance.currentOperation, elementsToClean)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        } finally {
            for (elem in elementsToClean) {
                try {
                    workspace!!.delete(elem.getURI())
                } catch (e: Exception) {
                    logger.warn("Unable to delete element {}: {}", elem, e)
                }

            }
        }
    }

    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class, ServiceRegistryException::class)
    private fun concat(src: MediaPackage, operation: WorkflowOperationInstance,
                       elementsToClean: MutableList<MediaPackageElement>): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage
        val operationId = operation.id
        //
        // read config options
        val presenterFlavor = getOptConfig(operation, SOURCE_PRESENTER_FLAVOR)
        val presentationFlavor = getOptConfig(operation, SOURCE_PRESENTATION_FLAVOR)
        val smilFlavor = MediaPackageElementFlavor.parseFlavor(getConfig(operation, SOURCE_SMIL_FLAVOR))
        val concatEncodingProfile = getConfig(operation, CONCAT_ENCODING_PROFILE)
        val concatOutputFramerate = getOptConfig(operation, CONCAT_OUTPUT_FRAMERATE)
        val trimEncodingProfile = getConfig(operation, TRIM_ENCODING_PROFILE)
        val targetPresenterFlavor = parseTargetFlavor(
                getConfig(operation, TARGET_PRESENTER_FLAVOR), "presenter")
        val targetPresentationFlavor = parseTargetFlavor(
                getConfig(operation, TARGET_PRESENTATION_FLAVOR), "presentation")
        val forceProfile = getForceEncodingProfile(operation)
        val forceEncoding = BooleanUtils.toBoolean(getOptConfig(operation, FORCE_ENCODING).getOr("false"))
        val forceDivisible = BooleanUtils.toBoolean(getOptConfig(operation, ENFORCE_DIVISIBLE_BY_TWO).getOr("false"))
        val requiredExtensions = getRequiredExtensions(operation)

        //
        // further checks on config options
        // Skip the worklow if no presenter and presentation flavor has been configured
        if (presenterFlavor.isNone && presentationFlavor.isNone) {
            logger.warn("No presenter and presentation flavor has been set.")
            return createResult(mediaPackage, Action.SKIP)
        }
        val concatProfile = composerService!!.getProfile(concatEncodingProfile)
                ?: throw WorkflowOperationException("Concat encoding profile '$concatEncodingProfile' was not found")

        var outputFramerate = -1.0f
        if (concatOutputFramerate.isSome) {
            if (NumberUtils.isNumber(concatOutputFramerate.get())) {
                logger.info("Using concat output framerate")
                outputFramerate = NumberUtils.toFloat(concatOutputFramerate.get())
            } else {
                throw WorkflowOperationException("Unable to parse concat output frame rate!")
            }
        }

        val trimProfile = composerService!!.getProfile(trimEncodingProfile)
                ?: throw WorkflowOperationException("Trim encoding profile '$trimEncodingProfile' was not found")

        //
        // get tracks
        val presenterTrackSelector = mkTrackSelector(presenterFlavor)
        val presentationTrackSelector = mkTrackSelector(presentationFlavor)
        val originalTracks = ArrayList<Track>()
        val presenterTracks = ArrayList<Track>()
        val presentationTracks = ArrayList<Track>()
        // Collecting presenter tracks
        for (t in presenterTrackSelector.select(mediaPackage, false)) {
            logger.info("Found partial presenter track {}", t)
            originalTracks.add(t)
            presenterTracks.add(t)
        }
        // Collecting presentation tracks
        for (t in presentationTrackSelector.select(mediaPackage, false)) {
            logger.info("Found partial presentation track {}", t)
            originalTracks.add(t)
            presentationTracks.add(t)
        }

        // flavor_type -> job
        val jobs = HashMap<String, Job>()
        // get SMIL catalog
        val smilDocument: SMILDocument
        try {
            smilDocument = SmilUtil.getSmilDocumentFromMediaPackage(mediaPackage, smilFlavor, workspace)
        } catch (e: SAXException) {
            throw WorkflowOperationException(e)
        }

        val parallel = smilDocument.body.childNodes.item(0) as SMILParElement
        val sequences = parallel.timeChildren
        val trackDurationInSeconds = parallel.dur
        val trackDurationInMs = Math.round(trackDurationInSeconds * 1000f).toLong()
        for (i in 0 until sequences.length) {
            val item = sequences.item(i) as SMILElement

            for (mediaType in arrayOf(NODE_TYPE_AUDIO, NODE_TYPE_VIDEO)) {
                val tracks = ArrayList<Track>()
                val sourceType = VCell.cell(EMPTY_VALUE)

                val position = processChildren(0, tracks, item.childNodes, originalTracks, sourceType, mediaType,
                        elementsToClean, operationId)

                if (tracks.isEmpty()) {
                    logger.debug("The tracks list was empty.")
                    continue
                }
                val lastTrack = tracks[tracks.size - 1]

                if (position < trackDurationInMs) {
                    val extendingTime = (trackDurationInMs - position) / 1000.0
                    if (extendingTime > 0) {
                        if (!lastTrack.hasVideo()) {
                            logger.info("Extending {} audio track end by {} seconds with silent audio", sourceType.get(),
                                    extendingTime)
                            tracks.add(getSilentAudio(extendingTime, elementsToClean, operationId))
                        } else {
                            logger.info("Extending {} track end with last image frame by {} seconds", sourceType.get(), extendingTime)
                            val tempLastImageFrame = extractLastImageFrame(lastTrack, elementsToClean)
                            tracks.add(createVideoFromImage(tempLastImageFrame, extendingTime, elementsToClean))
                        }
                    }
                }

                if (tracks.size < 2) {
                    logger.debug("There were less than 2 tracks, copying track...")
                    if (sourceType.get()!!.startsWith(PRESENTER_KEY)) {
                        createCopyOfTrack(mediaPackage, tracks[0], targetPresenterFlavor)
                    } else if (sourceType.get()!!.startsWith(PRESENTATION_KEY)) {
                        createCopyOfTrack(mediaPackage, tracks[0], targetPresentationFlavor)
                    } else {
                        logger.warn("Can't handle unkown source type '{}' for unprocessed track", sourceType.get())
                    }
                    continue
                }

                for (t in tracks) {
                    if (!t.hasVideo() && !t.hasAudio()) {
                        logger.error("No audio or video stream available in the track with flavor {}! {}", t.flavor, t)
                        throw WorkflowOperationException("No audio or video stream available in the track $t")
                    }
                }

                if (sourceType.get()!!.startsWith(PRESENTER_KEY)) {
                    logger.info("Concatenating {} track", PRESENTER_KEY)
                    jobs[sourceType.get()] = startConcatJob(concatProfile, tracks, outputFramerate, forceDivisible)
                } else if (sourceType.get()!!.startsWith(PRESENTATION_KEY)) {
                    logger.info("Concatenating {} track", PRESENTATION_KEY)
                    jobs[sourceType.get()] = startConcatJob(concatProfile, tracks, outputFramerate, forceDivisible)
                } else {
                    logger.warn("Can't handle unknown source type '{}'!", sourceType.get())
                }
            }
        }

        // Wait for the jobs to return
        if (jobs.size > 0) {
            if (!JobUtil.waitForJobs(serviceRegistry, jobs.values)!!.isSuccess) {
                throw WorkflowOperationException("One of the concat jobs did not complete successfully")
            }
        } else {
            logger.info("No concatenating needed for presenter and presentation tracks, took partial source elements")
        }

        // All the jobs have passed, let's update the media package
        var queueTime = 0L
        var adjustedTargetPresenterFlavor = targetPresenterFlavor
        var adjustedTargetPresentationFlavor = targetPresentationFlavor
        for ((key, value) in jobs) {
            val concatJob = JobUtil.update(serviceRegistry, value)
            if (concatJob.isSome) {
                val concatPayload = concatJob.get().payload
                if (concatPayload != null) {
                    val concatTrack: Track
                    try {
                        concatTrack = MediaPackageElementParser.getFromXml(concatPayload) as Track
                    } catch (e: MediaPackageException) {
                        throw WorkflowOperationException(e)
                    }

                    val fileName: String

                    // Adjust the target flavor.
                    if (key.startsWith(PRESENTER_KEY)) {
                        if (!concatTrack.hasVideo()) {
                            fileName = PRESENTER_KEY + FLAVOR_AUDIO_SUFFIX
                            adjustedTargetPresenterFlavor = deriveAudioFlavor(targetPresenterFlavor)
                        } else {
                            fileName = PRESENTER_KEY
                            adjustedTargetPresenterFlavor = targetPresenterFlavor
                        }
                        concatTrack.flavor = adjustedTargetPresenterFlavor
                    } else if (key.startsWith(PRESENTATION_KEY)) {
                        if (!concatTrack.hasVideo()) {
                            fileName = PRESENTATION_KEY + FLAVOR_AUDIO_SUFFIX
                            adjustedTargetPresentationFlavor = deriveAudioFlavor(targetPresentationFlavor)
                        } else {
                            fileName = PRESENTATION_KEY
                            adjustedTargetPresentationFlavor = targetPresentationFlavor
                        }
                        concatTrack.flavor = adjustedTargetPresentationFlavor
                    } else {
                        fileName = UNKNOWN_KEY
                    }

                    concatTrack.setURI(workspace!!.moveTo(concatTrack.getURI(), mediaPackage.identifier.toString(),
                            concatTrack.identifier,
                            fileName + "." + FilenameUtils.getExtension(concatTrack.getURI().toString())))

                    logger.info("Concatenated track {} got flavor '{}'", concatTrack, concatTrack.flavor)

                    mediaPackage.add(concatTrack)
                    queueTime += concatJob.get().queueTime!!
                } else {
                    // If there is no payload, then the item has not been distributed.
                    logger.warn("Concat job {} does not contain a payload", concatJob)
                }
            } else {
                logger.warn("Concat job {} could not be updated since it cannot be found", value)
            }
        }

        // Trim presenter and presentation source track if longer than the duration from the SMIL catalog
        queueTime += checkForTrimming(mediaPackage, trimProfile, targetPresentationFlavor, trackDurationInSeconds,
                elementsToClean)
        queueTime += checkForTrimming(mediaPackage, trimProfile, deriveAudioFlavor(targetPresentationFlavor),
                trackDurationInSeconds, elementsToClean)
        queueTime += checkForTrimming(mediaPackage, trimProfile, targetPresenterFlavor, trackDurationInSeconds,
                elementsToClean)
        queueTime += checkForTrimming(mediaPackage, trimProfile, deriveAudioFlavor(targetPresenterFlavor),
                trackDurationInSeconds, elementsToClean)

        adjustAudioTrackTargetFlavor(mediaPackage, targetPresenterFlavor)
        adjustAudioTrackTargetFlavor(mediaPackage, targetPresentationFlavor)

        queueTime += checkForMuxing(mediaPackage, targetPresenterFlavor, targetPresentationFlavor, false, elementsToClean)

        queueTime += checkForEncodeToStandard(mediaPackage, forceEncoding, forceProfile, requiredExtensions,
                targetPresenterFlavor, targetPresentationFlavor, elementsToClean)

        val result = createResult(mediaPackage, Action.CONTINUE, queueTime)
        logger.debug("Partial import operation completed")
        return result
    }

    @Throws(EncoderException::class, IOException::class, MediaPackageException::class, NotFoundException::class, ServiceRegistryException::class, WorkflowOperationException::class)
    protected fun checkForEncodeToStandard(mediaPackage: MediaPackage, forceEncoding: Boolean,
                                           forceProfile: Opt<EncodingProfile>, requiredExtensions: List<String>,
                                           targetPresenterFlavor: MediaPackageElementFlavor, targetPresentationFlavor: MediaPackageElementFlavor,
                                           elementsToClean: MutableList<MediaPackageElement>): Long {
        var queueTime: Long = 0
        if (forceProfile.isSome) {
            val targetPresenterTracks = mediaPackage.getTracks(targetPresenterFlavor)
            for (track in targetPresenterTracks) {
                if (forceEncoding || trackNeedsTobeEncodedToStandard(track, requiredExtensions)) {
                    logger.debug("Encoding '{}' flavored track '{}' with standard encoding profile {}",
                            targetPresenterFlavor, track.getURI(), forceProfile.get())
                    queueTime += encodeToStandard(mediaPackage, forceProfile.get(), targetPresenterFlavor, track)
                    elementsToClean.add(track)
                    mediaPackage.remove(track)
                }
            }
            // Skip presentation target if it is the same as the presenter one.
            if (!targetPresenterFlavor.toString().equals(targetPresentationFlavor.toString(), ignoreCase = true)) {
                val targetPresentationTracks = mediaPackage.getTracks(targetPresentationFlavor)
                for (track in targetPresentationTracks) {
                    if (forceEncoding || trackNeedsTobeEncodedToStandard(track, requiredExtensions)) {
                        logger.debug("Encoding '{}' flavored track '{}' with standard encoding profile {}",
                                targetPresentationFlavor, track.getURI(), forceProfile.get())
                        queueTime += encodeToStandard(mediaPackage, forceProfile.get(), targetPresentationFlavor, track)
                        elementsToClean.add(track)
                        mediaPackage.remove(track)
                    }
                }
            }
        }
        return queueTime
    }

    /**
     * This function creates a copy of a given track in the media package
     *
     * @param mediaPackage
     * The media package being processed.
     * @param track
     * The track we want to create a copy from.
     * @param targetFlavor
     * The target flavor for the copy of the track.
     */
    @Throws(IllegalArgumentException::class, NotFoundException::class, IOException::class)
    private fun createCopyOfTrack(mediaPackage: MediaPackage, track: Track, targetFlavor: MediaPackageElementFlavor) {

        var targetCopyFlavor: MediaPackageElementFlavor? = null
        if (track.hasVideo()) {
            targetCopyFlavor = targetFlavor
        } else {
            targetCopyFlavor = deriveAudioFlavor(targetFlavor)
        }
        logger.debug("Copying track {} with flavor {} using target flavor {}", track.getURI(), track.flavor, targetCopyFlavor)
        copyPartialToSource(mediaPackage, targetCopyFlavor, track)
    }

    /**
     * This functions adjusts the target flavor for audio tracks.
     * While processing audio tracks, an audio suffix is appended to the type of the audio tracks target flavor.
     * This functions essentially removes that suffix again and therefore ensures that the target flavor of
     * audio tracks is set correctly.
     *
     * @param mediaPackage
     * The media package to look for audio tracks.
     * @param targetFlavor
     * The target flavor for the audio tracks.
     */
    @Throws(IllegalArgumentException::class, NotFoundException::class, IOException::class)
    private fun adjustAudioTrackTargetFlavor(mediaPackage: MediaPackage, targetFlavor: MediaPackageElementFlavor) {

        val targetAudioTracks = mediaPackage.getTracks(deriveAudioFlavor(targetFlavor))
        for (track in targetAudioTracks) {
            logger.debug("Adding {} to finished audio tracks.", track.getURI())
            mediaPackage.remove(track)
            track.flavor = targetFlavor
            mediaPackage.add(track)
        }
    }

    @Throws(WorkflowOperationException::class)
    private fun mkTrackSelector(flavor: Opt<String>): TrackSelector {
        val s = TrackSelector()
        for (fs in flavor) {
            try {
                val f = MediaPackageElementFlavor.parseFlavor(fs)
                s.addFlavor(f)
                s.addFlavor(deriveAudioFlavor(f))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Flavor '$fs' is malformed")
            }

        }
        return s
    }

    /**
     * Start job to concatenate a list of tracks.
     *
     * @param profile
     * the encoding profile to use
     * @param tracks
     * non empty track list
     * @param forceDivisible
     * Whether to enforce the track's dimension to be divisible by two
     */
    @Throws(MediaPackageException::class, EncoderException::class)
    fun startConcatJob(profile: EncodingProfile?, tracks: List<Track>, outputFramerate: Float, forceDivisible: Boolean): Job {
        val dim = determineDimension(tracks, forceDivisible)
        return if (outputFramerate > 0.0) {
            composerService!!.concat(profile!!.identifier, dim, outputFramerate, false, Collections.toArray(Track::class.java, tracks))
        } else {
            composerService!!.concat(profile!!.identifier, dim, false, Collections.toArray(Track::class.java, tracks))
        }
    }

    /**
     * Get the extensions from configuration that don't need to be re-encoded.
     *
     * @param operation
     * The WorkflowOperationInstance to get the configuration from
     * @return The list of extensions
     */
    fun getRequiredExtensions(operation: WorkflowOperationInstance): List<String> {
        val requiredExtensions = ArrayList<String>()
        var configExtensions: String? = null
        try {
            configExtensions = StringUtils.trimToNull(getConfig(operation, REQUIRED_EXTENSIONS))
        } catch (e: WorkflowOperationException) {
            logger.info(
                    "Required extensions configuration key not specified so will be using default '{}'. Any input file not matching this extension will be re-encoded.",
                    DEFAULT_REQUIRED_EXTENSION)
        }

        if (configExtensions != null) {
            val extensions = configExtensions.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (extension in extensions) {
                requiredExtensions.add(extension)
            }
        }
        if (requiredExtensions.size == 0) {
            requiredExtensions.add(DEFAULT_REQUIRED_EXTENSION)
        }
        return requiredExtensions
    }

    /**
     * Get the force encoding profile from the operations config options.
     *
     * @return the encoding profile if option "force-encoding" is true, none otherwise
     * @throws WorkflowOperationException
     * if there is no such encoding profile or if no encoding profile is configured but force-encoding is true
     */
    @Throws(WorkflowOperationException::class)
    protected fun getForceEncodingProfile(woi: WorkflowOperationInstance): Opt<EncodingProfile> {
        return getOptConfig(woi, FORCE_ENCODING_PROFILE).map(object : Fn<String, EncodingProfile>() {
            override fun apply(profileName: String): EncodingProfile {
                for (profile in Opt.nul(composerService!!.getProfile(profileName))) {
                    return profile
                }
                return chuck(WorkflowOperationException("Force encoding profile '$profileName' was not found"))
            }
        }).orError(WorkflowOperationException("Force encoding profile must be set!"))
    }

    /**
     * @param flavorType
     * either "presenter" or "presentation", just for error messages
     */
    @Throws(WorkflowOperationException::class)
    private fun parseTargetFlavor(flavor: String, flavorType: String): MediaPackageElementFlavor {
        val targetFlavor: MediaPackageElementFlavor
        try {
            targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor)
            if ("*" == targetFlavor.type || "*" == targetFlavor.subtype) {
                throw WorkflowOperationException(format(
                        "Target %s flavor must have a type and a subtype, '*' are not allowed!", flavorType))
            }
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException(format("Target %s flavor '%s' is malformed", flavorType, flavor))
        }

        return targetFlavor
    }

    /** Create a derived audio flavor by appending [.FLAVOR_AUDIO_SUFFIX] to the flavor type.  */
    private fun deriveAudioFlavor(flavor: MediaPackageElementFlavor): MediaPackageElementFlavor {
        return MediaPackageElementFlavor.flavor(flavor.type + FLAVOR_AUDIO_SUFFIX, flavor.subtype)
    }

    /**
     * Determine the largest dimension of the given list of tracks
     *
     * @param tracks
     * the list of tracks
     * @param forceDivisible
     * Whether to enforce the track's dimension to be divisible by two
     * @return the largest dimension from the list of track
     */
    private fun determineDimension(tracks: List<Track>, forceDivisible: Boolean): Dimension? {
        val trackDimension = getLargestTrack(tracks) ?: return null

        if (forceDivisible && (trackDimension.b.height % 2 != 0 || trackDimension.b.width % 2 != 0)) {
            val scaledDimension = Dimension.dimension(trackDimension.b.width / 2 * 2, trackDimension
                    .b.height / 2 * 2)
            logger.info("Determined output dimension {} scaled down from {} for track {}", scaledDimension,
                    trackDimension.b, trackDimension.a)
            return scaledDimension
        } else {
            logger.info("Determined output dimension {} for track {}", trackDimension.b, trackDimension.a)
            return trackDimension.b
        }
    }

    /**
     * Returns the track with the largest resolution from the list of tracks
     *
     * @param tracks
     * the list of tracks
     * @return a [Tuple] with the largest track and it's dimension
     */
    private fun getLargestTrack(tracks: List<Track>): Tuple<Track, Dimension>? {
        var track: Track? = null
        var dimension: Dimension? = null
        for (t in tracks) {
            if (!t.hasVideo())
                continue

            val videoStreams = TrackSupport.byType(t.streams, VideoStream::class.java)
            val frameWidth = videoStreams[0].frameWidth!!
            val frameHeight = videoStreams[0].frameHeight!!
            if (dimension == null || frameWidth!! * frameHeight!! > dimension.width * dimension.height) {
                dimension = Dimension.dimension(frameWidth!!, frameHeight!!)
                track = t
            }
        }
        return if (track == null || dimension == null) null else Tuple.tuple(track, dimension)

    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
    private fun checkForTrimming(mediaPackage: MediaPackage, trimProfile: EncodingProfile,
                                 targetFlavor: MediaPackageElementFlavor, videoDuration: Float?, elementsToClean: MutableList<MediaPackageElement>): Long {
        val elements = mediaPackage.getElementsByFlavor(targetFlavor)
        if (elements.size == 0)
            return 0

        val trackToTrim = elements[0] as Track
        if (elements.size == 1 && trackToTrim.duration!! / 1000 > videoDuration) {
            val trimSeconds = (trackToTrim.duration!! / 1000 - videoDuration!!).toLong()
            logger.info("Shorten track {} to target duration {} by {} seconds",
                    trackToTrim.toString(), videoDuration.toString(), trimSeconds.toString())
            return trimEnd(mediaPackage, trimProfile, trackToTrim, videoDuration.toDouble(), elementsToClean)
        } else if (elements.size > 1) {
            logger.warn("Multiple tracks with flavor {} found! Trimming not possible!", targetFlavor)
        }
        return 0
    }

    private fun getPureVideoTracks(mediaPackage: MediaPackage, videoFlavor: MediaPackageElementFlavor): List<Track> {
        return `$`(*mediaPackage.tracks).filter(Filters.matchesFlavor(videoFlavor).toFn())
                .filter(Filters.hasVideo.toFn()).filter(Filters.hasNoAudio.toFn()).toList()
    }

    private fun getPureAudioTracks(mediaPackage: MediaPackage, audioFlavor: MediaPackageElementFlavor): List<Track> {
        return `$`(*mediaPackage.tracks).filter(Filters.matchesFlavor(audioFlavor).toFn())
                .filter(Filters.hasAudio.toFn()).filter(Filters.hasNoVideo.toFn()).toList()
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
    fun checkForMuxing(mediaPackage: MediaPackage, targetPresentationFlavor: MediaPackageElementFlavor,
                       targetPresenterFlavor: MediaPackageElementFlavor, useSuffix: Boolean, elementsToClean: MutableList<MediaPackageElement>): Long {

        var queueTime = 0L

        var videoElements = getPureVideoTracks(mediaPackage, targetPresentationFlavor)
        var audioElements: List<Track>
        if (useSuffix) {
            audioElements = getPureAudioTracks(mediaPackage, deriveAudioFlavor(targetPresentationFlavor))
        } else {
            audioElements = getPureAudioTracks(mediaPackage, targetPresentationFlavor)
        }

        var videoTrack: Track? = null
        var audioTrack: Track? = null

        if (videoElements.size == 1 && audioElements.size == 0) {
            videoTrack = videoElements[0]
        } else if (videoElements.size == 0 && audioElements.size == 1) {
            audioTrack = audioElements[0]
        }

        videoElements = getPureVideoTracks(mediaPackage, targetPresenterFlavor)
        if (useSuffix) {
            audioElements = getPureAudioTracks(mediaPackage, deriveAudioFlavor(targetPresenterFlavor))
        } else {
            audioElements = getPureAudioTracks(mediaPackage, targetPresenterFlavor)
        }

        if (videoElements.size == 1 && audioElements.size == 0) {
            videoTrack = videoElements[0]
        } else if (videoElements.size == 0 && audioElements.size == 1) {
            audioTrack = audioElements[0]
        }

        logger.debug("Check for mux between '{}' and '{}' flavors and found video track '{}' and audio track '{}'",
                targetPresentationFlavor, targetPresenterFlavor, videoTrack, audioTrack)
        if (videoTrack != null && audioTrack != null) {
            queueTime += mux(mediaPackage, videoTrack, audioTrack, elementsToClean)
            return queueTime
        } else {
            return queueTime
        }
    }

    /**
     * Mux a video and an audio track. Add the result to media package `mediaPackage` with the same flavor as
     * the `video`.
     *
     * @return the mux job's queue time
     */
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
    protected open fun mux(mediaPackage: MediaPackage, video: Track, audio: Track, elementsToClean: MutableList<MediaPackageElement>): Long {
        logger.debug("Muxing video {} and audio {}", video.getURI(), audio.getURI())
        var muxJob = composerService!!.mux(video, audio, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE)
        if (!waitForStatus(muxJob).isSuccess) {
            throw WorkflowOperationException("Muxing of audio $audio and video $video failed")
        }
        muxJob = serviceRegistry.getJob(muxJob.id)

        val muxed = MediaPackageElementParser.getFromXml(muxJob.payload) as Track
                ?: throw WorkflowOperationException("Muxed job $muxJob returned no payload!")
        muxed.flavor = video.flavor
        muxed.setURI(workspace!!.moveTo(muxed.getURI(), mediaPackage.identifier.toString(), muxed.identifier,
                FilenameUtils.getName(video.getURI().toString())))
        elementsToClean.add(audio)
        mediaPackage.remove(audio)
        elementsToClean.add(video)
        mediaPackage.remove(video)
        mediaPackage.add(muxed)
        return muxJob.queueTime!!
    }

    @Throws(NotFoundException::class, IOException::class)
    private fun copyPartialToSource(mediaPackage: MediaPackage, targetFlavor: MediaPackageElementFlavor?, track: Track) {
        var `in`: FileInputStream? = null
        try {
            val copyTrack = track.clone() as Track
            val originalFile = workspace!!.get(copyTrack.getURI())
            `in` = FileInputStream(originalFile)

            val elementID = UUID.randomUUID().toString()
            copyTrack.setURI(workspace!!.put(mediaPackage.identifier.toString(), elementID,
                    FilenameUtils.getName(copyTrack.getURI().toString()), `in`))
            copyTrack.flavor = targetFlavor
            copyTrack.identifier = elementID
            copyTrack.referTo(track)
            mediaPackage.add(copyTrack)
            logger.info("Copied partial source element {} to {} with target flavor {}", track.toString(),
                    copyTrack.toString(), targetFlavor!!.toString())
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    /**
     * Encode `track` using encoding profile `profile` and add the result to media package
     * `mp` under the given `targetFlavor`.
     *
     * @return the encoder job's queue time
     */
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
    private fun encodeToStandard(mp: MediaPackage, profile: EncodingProfile, targetFlavor: MediaPackageElementFlavor,
                                 track: Track): Long {
        var encodeJob = composerService!!.encode(track, profile.identifier)
        if (!waitForStatus(encodeJob).isSuccess) {
            throw WorkflowOperationException("Encoding of track $track failed")
        }
        encodeJob = serviceRegistry.getJob(encodeJob.id)
        val encodedTrack = MediaPackageElementParser.getFromXml(encodeJob.payload) as Track
                ?: throw WorkflowOperationException("Encoded track $track failed to produce a track")
        val uri: URI
        if (FilenameUtils.getExtension(encodedTrack.getURI().toString()).equals(
                        FilenameUtils.getExtension(track.getURI().toString()), ignoreCase = true)) {
            uri = workspace!!.moveTo(encodedTrack.getURI(), mp.identifier.compact(), encodedTrack.identifier,
                    FilenameUtils.getName(track.getURI().toString()))
        } else {
            // The new encoded file has a different extension.
            uri = workspace!!.moveTo(
                    encodedTrack.getURI(),
                    mp.identifier.compact(),
                    encodedTrack.identifier,
                    FilenameUtils.getBaseName(track.getURI().toString()) + "."
                            + FilenameUtils.getExtension(encodedTrack.getURI().toString()))
        }
        encodedTrack.setURI(uri)
        encodedTrack.flavor = targetFlavor
        mp.add(encodedTrack)
        return encodeJob.queueTime!!
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
    private fun trimEnd(mediaPackage: MediaPackage, trimProfile: EncodingProfile, track: Track, duration: Double,
                        elementsToClean: MutableList<MediaPackageElement>): Long {
        var trimJob = composerService!!.trim(track, trimProfile.identifier, 0, (duration * 1000).toLong())
        if (!waitForStatus(trimJob).isSuccess)
            throw WorkflowOperationException("Trimming of track $track failed")

        trimJob = serviceRegistry.getJob(trimJob.id)

        val trimmedTrack = MediaPackageElementParser.getFromXml(trimJob.payload) as Track
                ?: throw WorkflowOperationException("Trimming track $track failed to produce a track")

        val uri = workspace!!.moveTo(trimmedTrack.getURI(), mediaPackage.identifier.compact(),
                trimmedTrack.identifier, FilenameUtils.getName(track.getURI().toString()))
        trimmedTrack.setURI(uri)
        trimmedTrack.flavor = track.flavor

        elementsToClean.add(track)
        mediaPackage.remove(track)
        mediaPackage.add(trimmedTrack)

        return trimJob.queueTime!!
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun processChildren(position: Long, tracks: MutableList<Track>, children: NodeList, originalTracks: MutableList<Track>,
                                type: VCell<String>, mediaType: String, elementsToClean: MutableList<MediaPackageElement>, operationId: Long?): Long {
        var position = position
        for (j in 0 until children.length) {
            val item = children.item(j)
            if (item.hasChildNodes()) {
                position = processChildren(position, tracks, item.childNodes, originalTracks, type, mediaType,
                        elementsToClean, operationId)
            } else {
                val e = item as SMILMediaElement
                if (mediaType == e.nodeName) {
                    val track = getFromOriginal(e.id, originalTracks, type)
                    val beginInSeconds = e.begin.item(0).resolvedOffset
                    val beginInMs = Math.round(beginInSeconds * 1000.0)
                    // Fill out gaps with first or last frame from video
                    if (beginInMs > position) {
                        val positionInSeconds = position / 1000.0
                        if (position == 0L) {
                            if (NODE_TYPE_AUDIO == e.nodeName) {
                                logger.info("Extending {} audio track start by {} seconds silent audio", type.get(), beginInSeconds)
                                tracks.add(getSilentAudio(beginInSeconds, elementsToClean, operationId))
                            } else {
                                logger.info("Extending {} track start image frame by {} seconds", type.get(), beginInSeconds)
                                val tempFirstImageFrame = extractImage(track, 0.0, elementsToClean)
                                tracks.add(createVideoFromImage(tempFirstImageFrame, beginInSeconds, elementsToClean))
                            }
                            position += beginInMs
                        } else {
                            val fillTime = (beginInMs - position) / 1000.0
                            if (NODE_TYPE_AUDIO == e.nodeName) {
                                logger.info("Fill {} audio track gap from {} to {} with silent audio", type.get(),
                                        java.lang.Double.toString(positionInSeconds), java.lang.Double.toString(beginInSeconds))
                                tracks.add(getSilentAudio(fillTime, elementsToClean, operationId))
                            } else {
                                logger.info("Fill {} track gap from {} to {} with image frame",
                                        type.get(), java.lang.Double.toString(positionInSeconds), java.lang.Double.toString(beginInSeconds))
                                val previousTrack = tracks[tracks.size - 1]
                                val tempLastImageFrame = extractLastImageFrame(previousTrack, elementsToClean)
                                tracks.add(createVideoFromImage(tempLastImageFrame, fillTime, elementsToClean))
                            }
                            position = beginInMs
                        }
                    }
                    tracks.add(track)
                    position += Math.round(e.dur * 1000f).toLong()
                }
            }
        }
        return position
    }

    private fun getFromOriginal(trackId: String, originalTracks: MutableList<Track>, type: VCell<String>): Track {
        for (t in originalTracks) {
            if (t.identifier.contains(trackId)) {
                logger.debug("Track-Id from smil found in Mediapackage ID: " + t.identifier)
                if (EMPTY_VALUE == type.get()) {
                    val suffix = if (t.hasAudio() && !t.hasVideo()) FLAVOR_AUDIO_SUFFIX else ""
                    type.set(t.flavor.type!! + suffix)
                }
                originalTracks.remove(t)
                return t
            }
        }
        throw IllegalStateException("No track matching smil Track-id: $trackId")
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, IOException::class)
    private fun getSilentAudio(time: Double, elementsToClean: MutableList<MediaPackageElement>,
                               operationId: Long?): Track {
        val uri = workspace!!.putInCollection(COLLECTION_ID, operationId!!.toString() + "-silent", ByteArrayInputStream(
                EMPTY_VALUE.toByteArray()))
        val emptyAttachment = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(uri, Type.Attachment, MediaPackageElementFlavor.parseFlavor("audio/silent")) as Attachment
        elementsToClean.add(emptyAttachment)

        val silentAudioJob = composerService!!.imageToVideo(emptyAttachment, SILENT_AUDIO_PROFILE, time)
        if (!waitForStatus(silentAudioJob).isSuccess)
            throw WorkflowOperationException("Silent audio job did not complete successfully")

        // Get the latest copy
        try {
            for (payload in getPayload(serviceRegistry, silentAudioJob)) {
                val silentAudio = MediaPackageElementParser.getFromXml(payload) as Track
                elementsToClean.add(silentAudio)
                return silentAudio
            }
            // none
            throw WorkflowOperationException(format("Job %s has no payload or cannot be updated", silentAudioJob))
        } catch (ex: ServiceRegistryException) {
            throw WorkflowOperationException(ex)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class)
    private fun createVideoFromImage(image: Attachment, time: Double, elementsToClean: MutableList<MediaPackageElement>): Track {
        var imageToVideoJob = composerService!!.imageToVideo(image, IMAGE_MOVIE_PROFILE, time)
        if (!waitForStatus(imageToVideoJob).isSuccess)
            throw WorkflowOperationException("Image to video job did not complete successfully")

        // Get the latest copy
        try {
            imageToVideoJob = serviceRegistry.getJob(imageToVideoJob.id)
        } catch (e: ServiceRegistryException) {
            throw WorkflowOperationException(e)
        }

        val imageVideo = MediaPackageElementParser.getFromXml(imageToVideoJob.payload) as Track
        elementsToClean.add(imageVideo)
        return imageVideo
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class)
    private fun extractImage(presentationTrack: Track, time: Double, elementsToClean: MutableList<MediaPackageElement>): Attachment {
        var extractImageJob = composerService!!.image(presentationTrack, PREVIEW_PROFILE, time)
        if (!waitForStatus(extractImageJob).isSuccess)
            throw WorkflowOperationException("Extract image frame video job did not complete successfully")

        // Get the latest copy
        try {
            extractImageJob = serviceRegistry.getJob(extractImageJob.id)
        } catch (e: ServiceRegistryException) {
            throw WorkflowOperationException(e)
        }

        val composedImages = MediaPackageElementParser.getArrayFromXml(extractImageJob.payload)[0] as Attachment
        elementsToClean.add(composedImages)
        return composedImages
    }

    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class)
    private fun extractLastImageFrame(presentationTrack: Track, elementsToClean: MutableList<MediaPackageElement>): Attachment {
        val videoStreams = TrackSupport.byType(presentationTrack.streams, VideoStream::class.java)
        val properties = HashMap<String, String>()
        properties["frame"] = java.lang.Long.toString(videoStreams[0].frameCount!! - 1)

        var extractImageJob = composerService!!.image(presentationTrack, IMAGE_FRAME_PROFILE, properties)
        if (!waitForStatus(extractImageJob).isSuccess)
            throw WorkflowOperationException("Extract image frame video job did not complete successfully")

        // Get the latest copy
        try {
            extractImageJob = serviceRegistry.getJob(extractImageJob.id)
        } catch (e: ServiceRegistryException) {
            throw WorkflowOperationException(e)
        }

        val composedImages = MediaPackageElementParser.getArrayFromXml(extractImageJob.payload)[0] as Attachment
        elementsToClean.add(composedImages)
        return composedImages
    }

    companion object {

        /** Workflow configuration keys  */
        private val SOURCE_PRESENTER_FLAVOR = "source-presenter-flavor"
        private val SOURCE_PRESENTATION_FLAVOR = "source-presentation-flavor"
        private val SOURCE_SMIL_FLAVOR = "source-smil-flavor"

        private val TARGET_PRESENTER_FLAVOR = "target-presenter-flavor"
        private val TARGET_PRESENTATION_FLAVOR = "target-presentation-flavor"

        private val CONCAT_ENCODING_PROFILE = "concat-encoding-profile"
        private val CONCAT_OUTPUT_FRAMERATE = "concat-output-framerate"
        private val TRIM_ENCODING_PROFILE = "trim-encoding-profile"
        private val FORCE_ENCODING_PROFILE = "force-encoding-profile"

        private val FORCE_ENCODING = "force-encoding"
        private val REQUIRED_EXTENSIONS = "required-extensions"
        private val ENFORCE_DIVISIBLE_BY_TWO = "enforce-divisible-by-two"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(PartialImportWorkflowOperationHandler::class.java)

        /** Other constants  */
        private val EMPTY_VALUE = ""
        private val NODE_TYPE_AUDIO = "audio"
        private val NODE_TYPE_VIDEO = "video"
        private val FLAVOR_AUDIO_SUFFIX = "-audio"
        private val COLLECTION_ID = "composer"
        private val UNKNOWN_KEY = "unknown"
        private val PRESENTER_KEY = "presenter"
        private val PRESENTATION_KEY = "presentation"
        private val DEFAULT_REQUIRED_EXTENSION = "mp4"

        /** Needed encoding profiles  */
        private val PREVIEW_PROFILE = "import.preview"
        private val IMAGE_FRAME_PROFILE = "import.image-frame"
        private val SILENT_AUDIO_PROFILE = "import.silent"
        private val IMAGE_MOVIE_PROFILE = "image-movie.work"

        /**
         * Determines if the extension of a track is non-standard and therefore should be re-encoded.
         *
         * @param track
         * The track to check the extension on.
         */
        fun trackNeedsTobeEncodedToStandard(track: Track, requiredExtensions: List<String>): Boolean {
            val extension = FilenameUtils.getExtension(track.getURI().toString())
            for (requiredExtension in requiredExtensions) {
                if (requiredExtension.equals(extension, ignoreCase = true)) {
                    return false
                }
            }
            return true
        }
    }
}
