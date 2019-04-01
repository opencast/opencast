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

package org.opencastproject.composer.impl

import java.lang.String.format
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.opencastproject.serviceregistry.api.Incidents.Companion.NO_DETAILS
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.api.VideoClip
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.Layout
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.identifier.IdBuilder
import org.opencastproject.mediapackage.identifier.IdBuilderFactory
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup
import org.opencastproject.util.FileSupport
import org.opencastproject.util.JsonObj
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.workspace.api.Workspace

import com.google.gson.Gson

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.net.URI
import java.net.URISyntaxException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.Locale
import kotlin.collections.Map.Entry
import java.util.Properties

/** FFMPEG based implementation of the composer service api.  */
/** Creates a new composer service instance.  */
open class ComposerServiceImpl : AbstractJobProducer(ComposerService.JOB_TYPE), ComposerService, ManagedService {

    private var maxMultipleProfilesJobLoad = DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES
    private var processSmilJobLoadFactor = DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR
    private val multiEncodeJobLoadFactor = DEFAULT_MULTI_ENCODE_JOB_LOAD_FACTOR

    /** default transition  */
    private var transitionDuration = (DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION * 1000).toInt()

    /** tracked encoder engines  */
    private val activeEncoder = HashSet<EncoderEngine>()

    /** Encoding profile manager  */
    private var profileScanner: EncodingProfileScanner? = null

    /** Reference to the media inspection service  */
    private var inspectionService: MediaInspectionService? = null

    /** Reference to the workspace service  */
    private var workspace: Workspace? = null

    /** Reference to the receipt service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Sets the service registry
     *
     * @param serviceRegistry
     * the service registry
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The organization directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /**
     * Sets a reference to the organization directory service.
     *
     * @param organizationDirectory
     * the organization directory
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** Id builder used to create ids for encoded tracks  */
    private val idBuilder = IdBuilderFactory.newInstance().newIdBuilder()

    /** The security service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /**
     * Callback for setting the security service.
     *
     * @param securityService
     * the securityService to set
     */
    override var securityService: SecurityService? = null
        set

    /** SmilService for process SMIL  */
    private var smilService: SmilService? = null

    /** The user directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /**
     * Callback for setting the user directory service.
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** Path to the FFmpeg binary  */
    private var ffmpegBinary = FFMPEG_BINARY_DEFAULT

    private val encoderEngine: EncoderEngine
        get() {
            val engine = EncoderEngine(ffmpegBinary)
            activeEncoder.add(engine)
            return engine
        }

    /** List of available operations on jobs  */
    internal enum class Operation {
        Encode, Image, ImageConversion, Mux, Trim, Composite, Concat, ImageToVideo, ParallelEncode, Demux, ProcessSmil, MultiEncode
    }

    /**
     * OSGi callback on component activation.
     *
     * @param cc
     * the component context
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        ffmpegBinary = StringUtils.defaultString(cc.bundleContext.getProperty(CONFIG_FFMPEG_PATH),
                FFMPEG_BINARY_DEFAULT)
        logger.debug("ffmpeg binary: {}", ffmpegBinary)
        logger.info("Activating composer service")
    }

    /**
     * OSGi callback on component deactivation.
     */
    fun deactivate() {
        logger.info("Deactivating composer service")
        for (engine in activeEncoder) {
            engine.close()
        }
        logger.debug("Closed encoder engine factory")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.encode
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun encode(sourceTrack: Track, profileId: String): Job {
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Encode.toString(),
                    Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)), profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * Load track into workspace and return a file handler, filing an incident if something went wrong.
     *
     * @param job The job in which context this operation is executed
     * @param name Name of the track to load into the workspace
     * @param track Track to load into the workspace
     * @return File handler for track
     * @throws EncoderException Could not load file into workspace
     */
    @Throws(EncoderException::class)
    private fun loadTrackIntoWorkspace(job: Job, name: String, track: Track, unique: Boolean): File {
        try {
            return workspace!!.get(track.getURI(), unique)
        } catch (e: NotFoundException) {
            incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                    getWorkspaceMediapackageParams(name, track), NO_DETAILS)
            throw EncoderException(format("%s track %s not found", name, track))
        } catch (e: IOException) {
            incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                    getWorkspaceMediapackageParams(name, track), NO_DETAILS)
            throw EncoderException(format("Unable to access %s track %s", name, track))
        }

    }

    /**
     * Load URI into workspace by URI and return a file handler, filing an incident if something went wrong.
     *
     * @param job
     * The job in which context this operation is executed
     * @param name
     * Name of the track to load into the workspace
     * @param uri
     * URI of Track to load into the workspace
     * @return File handler for track
     * @throws EncoderException
     * Could not load file into workspace
     */
    @Throws(EncoderException::class)
    private fun loadURIIntoWorkspace(job: Job, name: String, uri: URI): File {
        try {
            return workspace!!.get(uri)
        } catch (e: NotFoundException) {
            incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e, getWorkspaceCollectionParams(name, name, uri),
                    NO_DETAILS)
            throw EncoderException(String.format("%s uri %s not found", name, uri))
        } catch (e: IOException) {
            incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e, getWorkspaceCollectionParams(name, name, uri),
                    NO_DETAILS)
            throw EncoderException(String.format("Unable to access %s uri %s", name, uri))
        }

    }

    /**
     * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
     * one movie container.
     *
     * @param tracks
     * tracks to use for processing
     * @param profileId
     * the encoding profile
     * @param properties
     * encoding properties
     * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
     * two pass encodings where the first pass only creates metadata for the second one
     * @throws EncoderException
     * if encoding fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    private fun encode(job: Job, tracks: Map<String, Track>, profileId: String): Option<Track> {

        val targetTrackId = idBuilder.createNew().toString()

        val files = HashMap<String, File>()
        // Get the tracks and make sure they exist
        for ((key, value) in tracks) {
            files[key] = loadTrackIntoWorkspace(job, key, value, false)
        }

        // Get the encoding profile
        val profile = getProfile(job, profileId)

        val trackMsg = LinkedList<String>()
        for ((key, value) in tracks) {
            trackMsg.add(format("%s: %s", key, value.identifier))
        }
        logger.info("Encoding {} into {} using profile {}", StringUtils.join(trackMsg, ", "), targetTrackId, profileId)

        // Do the work
        val encoder = encoderEngine
        val output: List<File>
        try {
            output = encoder.process(files, profile, null)
        } catch (e: EncoderException) {
            val params = HashMap<String, String>()
            for ((key, value) in tracks) {
                params[key] = value.identifier
            }
            params["profile"] = profile.identifier
            params["properties"] = "EMPTY"
            incident().recordFailure(job, ENCODING_FAILED, e, params, detailsFor(e, encoder))
            throw e
        } finally {
            activeEncoder.remove(encoder)
        }

        // We expect zero or one file as output
        if (output.size == 0) {
            return none()
        } else if (output.size != 1) {
            // Ensure we do not leave behind old files in the workspace
            for (file in output) {
                FileUtils.deleteQuietly(file)
            }
            throw EncoderException("Composite does not support multiple files as output")
        }

        // Put the file in the workspace
        val workspaceURI = putToCollection(job, output[0], "encoded file")

        // Have the encoded track inspected and return the result
        val inspectedTrack = inspect(job, workspaceURI)
        inspectedTrack.identifier = targetTrackId

        return some(inspectedTrack)
    }

    /**
     * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
     * one movie container.
     *
     * @param job
     * Job in which context the encoding is done
     * @param mediaTrack
     * Source track
     * @param profileId
     * the encoding profile
     * @return the encoded track or none if the operation does not return a track. This may happen for example when doing
     * two pass encodings where the first pass only creates metadata for the second one
     * @throws EncoderException
     * if encoding fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    private fun parallelEncode(job: Job?, mediaTrack: Track, profileId: String): List<Track> {
        if (job == null) {
            throw EncoderException("The Job parameter must not be null")
        }
        // Get the tracks and make sure they exist
        val mediaFile = loadTrackIntoWorkspace(job, "source", mediaTrack, false)

        // Create the engine
        val profile = getProfile(profileId)
        val encoderEngine = encoderEngine

        // List of encoded tracks
        val encodedTracks = LinkedList<Track>()
        // Do the work
        var i = 0
        val source = HashMap<String, File>()
        source["video"] = mediaFile
        val outputFiles = encoderEngine.process(source, profile, null)
        activeEncoder.remove(encoderEngine)
        for (encodingOutput in outputFiles) {
            // Put the file in the workspace
            var returnURL: URI
            val targetTrackId = idBuilder.createNew().toString()

            try {
                FileInputStream(encodingOutput).use { `in` ->
                    returnURL = workspace!!.putInCollection(COLLECTION,
                            job.id.toString() + "-" + i + "." + FilenameUtils.getExtension(encodingOutput.absolutePath), `in`)
                    logger.info("Copied the encoded file to the workspace at {}", returnURL)
                    if (encodingOutput.delete()) {
                        logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.absolutePath)
                    } else {
                        logger.warn("Unable to delete the encoding output at {}", encodingOutput)
                    }
                }
            } catch (e: Exception) {
                throw EncoderException("Unable to put the encoded file into the workspace", e)
            }

            // Have the encoded track inspected and return the result
            val inspectedTrack = inspect(job, returnURL)
            inspectedTrack.identifier = targetTrackId

            val tags = profile.tags
            for (tag in tags) {
                if (encodingOutput.name.endsWith(profile.getSuffix(tag)))
                    inspectedTrack.addTag(tag)
            }

            encodedTracks.add(inspectedTrack)
            i++
        }

        return encodedTracks
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.encode
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun parallelEncode(sourceTrack: Track, profileId: String): Job {
        try {
            val profile = profileScanner!!.getProfile(profileId)
            logger.info("Starting parallel encode with profile {} with job load {}", profileId, df.format(profile.jobLoad.toDouble()))
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.ParallelEncode.toString(),
                    Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)), profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.trim
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun trim(sourceTrack: Track, profileId: String, start: Long, duration: Long): Job {
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Trim.toString(),
                    Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack), java.lang.Long.toString(start),
                            java.lang.Long.toString(duration)), profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * Trims the given track using the encoding profile `profileId` and the given starting point and duration
     * in miliseconds.
     *
     * @param job
     * the associated job
     * @param sourceTrack
     * the source track
     * @param profileId
     * the encoding profile identifier
     * @param start
     * the trimming in-point in millis
     * @param duration
     * the trimming duration in millis
     * @return the trimmed track or none if the operation does not return a track. This may happen for example when doing
     * two pass encodings where the first pass only creates metadata for the second one
     * @throws EncoderException
     * if trimming fails
     */
    @Throws(EncoderException::class)
    private fun trim(job: Job, sourceTrack: Track, profileId: String, start: Long, duration: Long): Option<Track> {
        val targetTrackId = idBuilder.createNew().toString()

        // Get the track and make sure it exists
        val trackFile = loadTrackIntoWorkspace(job, "source", sourceTrack, false)

        // Get the encoding profile
        val profile = getProfile(job, profileId)

        // Create the engine
        val encoderEngine = encoderEngine

        val output: File
        try {
            output = encoderEngine.trim(trackFile, profile, start, duration, null)
        } catch (e: EncoderException) {
            val params = HashMap<String, String>()
            params["track"] = sourceTrack.getURI().toString()
            params["profile"] = profile.identifier
            params["start"] = java.lang.Long.toString(start)
            params["duration"] = java.lang.Long.toString(duration)
            incident().recordFailure(job, TRIMMING_FAILED, e, params, detailsFor(e, encoderEngine))
            throw e
        } finally {
            activeEncoder.remove(encoderEngine)
        }

        // trim did not return a file
        if (!output.exists() || output.length() == 0L)
            return none()

        // Put the file in the workspace
        val workspaceURI = putToCollection(job, output, "trimmed file")

        // Have the encoded track inspected and return the result
        val inspectedTrack = inspect(job, workspaceURI)
        inspectedTrack.identifier = targetTrackId
        return some(inspectedTrack)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.mux
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun mux(videoTrack: Track, audioTrack: Track, profileId: String): Job {
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Mux.toString(),
                    Arrays.asList(profileId, MediaPackageElementParser.getAsXml(videoTrack),
                            MediaPackageElementParser.getAsXml(audioTrack)), profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * Muxes the audio and video track into one movie container.
     *
     * @param job
     * the associated job
     * @param videoTrack
     * the video track
     * @param audioTrack
     * the audio track
     * @param profileId
     * the profile identifier
     * @return the muxed track
     * @throws EncoderException
     * if encoding fails
     * @throws MediaPackageException
     * if serializing the mediapackage elements fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    private fun mux(job: Job, videoTrack: Track, audioTrack: Track, profileId: String): Option<Track> {
        return encode(job, Collections.map(tuple("audio", audioTrack), tuple("video", videoTrack)), profileId)
    }

    /**
     * {@inheritDoc}
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun composite(compositeTrackSize: Dimension, upperTrack: Option<LaidOutElement<Track>>,
                           lowerTrack: LaidOutElement<Track>, watermark: Option<LaidOutElement<Attachment>>, profileId: String,
                           background: String, sourceAudioName: String): Job {
        val arguments = ArrayList<String>(10)
        arguments.add(PROFILE_ID_INDEX, profileId)
        arguments.add(LOWER_TRACK_INDEX, MediaPackageElementParser.getAsXml(lowerTrack.element))
        arguments.add(LOWER_TRACK_LAYOUT_INDEX, Serializer.json(lowerTrack.layout).toJson())
        if (upperTrack.isNone) {
            arguments.add(UPPER_TRACK_INDEX, NOT_AVAILABLE)
            arguments.add(UPPER_TRACK_LAYOUT_INDEX, NOT_AVAILABLE)
        } else {
            arguments.add(UPPER_TRACK_INDEX, MediaPackageElementParser.getAsXml(upperTrack.get().element))
            arguments.add(UPPER_TRACK_LAYOUT_INDEX, Serializer.json(upperTrack.get().layout).toJson())
        }
        arguments.add(COMPOSITE_TRACK_SIZE_INDEX, Serializer.json(compositeTrackSize).toJson())
        arguments.add(BACKGROUND_COLOR_INDEX, background)
        if (watermark.isSome) {
            val watermarkLaidOutElement = watermark.get()
            arguments.add(WATERMARK_INDEX, MediaPackageElementParser.getAsXml(watermarkLaidOutElement.element))
            arguments.add(WATERMARK_LAYOUT_INDEX, Serializer.json(watermarkLaidOutElement.layout).toJson())
        }
        arguments.add(AUDIO_SOURCE_INDEX, sourceAudioName)
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Composite.toString(), arguments, profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create composite job", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun composite(job: Job, compositeTrackSize: Dimension, lowerLaidOutElement: LaidOutElement<Track>,
                          upperLaidOutElement: Option<LaidOutElement<Track>>, watermarkOption: Option<LaidOutElement<Attachment>>,
                          profileId: String, backgroundColor: String, audioSourceName: String): Option<Track> {

        // Get the encoding profile
        val profile = getProfile(job, profileId)

        // Create the engine
        val encoderEngine = encoderEngine

        val targetTrackId = idBuilder.createNew().toString()
        var upperVideoFile = Option.none()
        try {
            // Get the tracks and make sure they exist
            val lowerVideoFile = loadTrackIntoWorkspace(job, "lower video", lowerLaidOutElement.element, false)

            if (upperLaidOutElement.isSome) {
                upperVideoFile = Option.option(
                        loadTrackIntoWorkspace(job, "upper video", upperLaidOutElement.get().element, false))
            }
            var watermarkFile: File? = null
            if (watermarkOption.isSome) {
                try {
                    watermarkFile = workspace!!.get(watermarkOption.get().element.getURI())
                } catch (e: NotFoundException) {
                    incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                            getWorkspaceMediapackageParams("watermark image", watermarkOption.get().element),
                            NO_DETAILS)
                    throw EncoderException("Requested watermark image " + watermarkOption.get().element
                            + " is not found")
                } catch (e: IOException) {
                    incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                            getWorkspaceMediapackageParams("watermark image", watermarkOption.get().element),
                            NO_DETAILS)
                    throw EncoderException("Unable to access right watermark image " + watermarkOption.get().element)
                }

                if (upperLaidOutElement.isSome) {
                    logger.info("Composing lower video track {} {} and upper video track {} {} including watermark {} {} into {}",
                            lowerLaidOutElement.element.identifier, lowerLaidOutElement.element.getURI(),
                            upperLaidOutElement.get().element.identifier, upperLaidOutElement.get().element.getURI(),
                            watermarkOption.get().element.identifier, watermarkOption.get().element.getURI(),
                            targetTrackId)
                } else {
                    logger.info("Composing video track {} {} including watermark {} {} into {}",
                            lowerLaidOutElement.element.identifier, lowerLaidOutElement.element.getURI(),
                            watermarkOption.get().element.identifier, watermarkOption.get().element.getURI(),
                            targetTrackId)
                }
            } else {
                if (upperLaidOutElement.isSome) {
                    logger.info("Composing lower video track {} {} and upper video track {} {} into {}",
                            lowerLaidOutElement.element.identifier, lowerLaidOutElement.element.getURI(),
                            upperLaidOutElement.get().element.identifier, upperLaidOutElement.get().element.getURI(),
                            targetTrackId)
                } else {
                    logger.info("Composing video track {} {} into {}", lowerLaidOutElement.element.identifier,
                            lowerLaidOutElement.element.getURI(), targetTrackId)
                }
            }

            // Creating video filter command
            val compositeCommand = buildCompositeCommand(compositeTrackSize, lowerLaidOutElement,
                    upperLaidOutElement, upperVideoFile, watermarkOption, watermarkFile, backgroundColor, audioSourceName)

            val properties = HashMap<String, String>()
            properties[EncoderEngine.CMD_SUFFIX + ".compositeCommand"] = compositeCommand
            val output: List<File>
            try {
                val source = HashMap<String, File>()
                if (upperVideoFile.isSome) {
                    source["audio"] = upperVideoFile.get()
                }
                source["video"] = lowerVideoFile
                output = encoderEngine.process(source, profile, properties)
            } catch (e: EncoderException) {
                val params = HashMap<String, String>()
                if (upperLaidOutElement.isSome) {
                    params["upper"] = upperLaidOutElement.get().element.getURI().toString()
                }
                params["lower"] = lowerLaidOutElement.element.getURI().toString()
                if (watermarkFile != null)
                    params["watermark"] = watermarkOption.get().element.getURI().toString()
                params["profile"] = profile.identifier
                params["properties"] = properties.toString()
                incident().recordFailure(job, COMPOSITE_FAILED, e, params, detailsFor(e, encoderEngine))
                throw e
            } finally {
                activeEncoder.remove(encoderEngine)
            }

            // We expect one file as output
            if (output.size != 1) {
                // Ensure we do not leave behind old files in the workspace
                for (file in output) {
                    FileUtils.deleteQuietly(file)
                }
                throw EncoderException("Composite does not support multiple files as output")
            }


            // Put the file in the workspace
            val workspaceURI = putToCollection(job, output[0], "compound file")

            // Have the compound track inspected and return the result

            val inspectedTrack = inspect(job, workspaceURI)
            inspectedTrack.identifier = targetTrackId

            return some(inspectedTrack)
        } catch (e: Exception) {
            if (upperLaidOutElement.isSome) {
                logger.warn("Error composing {}  and {}: {}", lowerLaidOutElement.element, upperLaidOutElement.get().element,
                        getStackTrace(e))
            } else {
                logger.warn("Error composing {}: {}", lowerLaidOutElement.element, getStackTrace(e))
            }
            if (e is EncoderException) {
                throw e
            } else {
                throw EncoderException(e)
            }
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun concat(profileId: String, outputDimension: Dimension, sameCodec: Boolean, vararg tracks: Track): Job {
        return concat(profileId, outputDimension, -1.0f, sameCodec, *tracks)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun concat(profileId: String, outputDimension: Dimension?, outputFrameRate: Float, sameCodec: Boolean, vararg tracks: Track): Job {
        val arguments = ArrayList<String>()
        arguments.add(0, profileId)
        if (outputDimension != null) {
            arguments.add(1, Serializer.json(outputDimension).toJson())
        } else {
            arguments.add(1, "")
        }
        arguments.add(2, String.format(Locale.US, "%f", outputFrameRate))
        arguments.add(3, java.lang.Boolean.toString(sameCodec))
        for (i in tracks.indices) {
            arguments.add(i + 4, MediaPackageElementParser.getAsXml(tracks[i]))
        }
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Concat.toString(), arguments, profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create concat job", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun concat(job: Job, tracks: List<Track>, profileId: String, outputDimension: Dimension?,
                       outputFrameRate: Float, sameCodec: Boolean): Option<Track> {

        if (tracks.size < 2) {
            val params = HashMap<String, String>()
            params["tracks-size"] = Integer.toString(tracks.size)
            params["tracks"] = StringUtils.join(tracks, ",")
            incident().recordFailure(job, CONCAT_LESS_TRACKS, params)
            throw EncoderException("The track parameter must at least have two tracks present")
        }

        var onlyAudio = true
        for (t in tracks) {
            if (t.hasVideo()) {
                onlyAudio = false
                break
            }
        }

        if (!sameCodec && !onlyAudio && outputDimension == null) {
            val params = HashMap<String, String>()
            params["tracks"] = StringUtils.join(tracks, ",")
            incident().recordFailure(job, CONCAT_NO_DIMENSION, params)
            throw EncoderException("The output dimension id parameter must not be null when concatenating video")
        }

        val targetTrackId = idBuilder.createNew().toString()
        // Get the tracks and make sure they exist
        val trackFiles = ArrayList<File>()
        var i = 0
        for (track in tracks) {
            if (!track.hasAudio() && !track.hasVideo()) {
                val params = HashMap<String, String>()
                params["track-id"] = track.identifier
                params["track-url"] = track.getURI().toString()
                incident().recordFailure(job, NO_STREAMS, params)
                throw EncoderException("Track has no audio or video stream available: $track")
            }
            trackFiles.add(i++, loadTrackIntoWorkspace(job, "concat", track, false))
        }

        // Create the engine
        val encoderEngine = encoderEngine

        if (onlyAudio) {
            logger.info("Concatenating audio tracks {} into {}", trackFiles, targetTrackId)
        } else {
            logger.info("Concatenating video tracks {} into {}", trackFiles, targetTrackId)
        }

        // Get the encoding profile
        val profile = getProfile(job, profileId)
        val concatCommand: String
        var fileList: File? = null
        // Creating video filter command for concat
        if (sameCodec) {
            // create file list to use Concat demuxer - lossless - pack contents into a single container
            fileList = File(workspace!!.rootDirectory(), "concat_tracklist_" + job.id + ".txt")
            fileList.deleteOnExit()
            try {
                PrintWriter(FileWriter(fileList, true)).use { printer ->
                    for (track in tracks) {
                        printer.append("file '").append(workspace!!.get(track.getURI()).absolutePath).append("'\n")
                    }
                }
            } catch (e: IOException) {
                throw EncoderException("Cannot create file list for concat", e)
            } catch (e: NotFoundException) {
                throw EncoderException("Cannot find track filename in workspace for concat", e)
            }

            concatCommand = "-f concat -safe 0 -i " + fileList.absolutePath
        } else {
            concatCommand = buildConcatCommand(onlyAudio, outputDimension, outputFrameRate, trackFiles, tracks)
        }

        val properties = HashMap<String, String>()
        properties[EncoderEngine.CMD_SUFFIX + ".concatCommand"] = concatCommand

        val output: File
        try {
            output = encoderEngine.encode(trackFiles[0], profile, properties)
        } catch (e: EncoderException) {
            val params = HashMap<String, String>()
            val trackList = ArrayList<String>()
            for (t in tracks) {
                trackList.add(t.getURI().toString())
            }
            params["tracks"] = StringUtils.join(trackList, ",")
            params["profile"] = profile.identifier
            params["properties"] = properties.toString()
            incident().recordFailure(job, CONCAT_FAILED, e, params, detailsFor(e, encoderEngine))
            throw e
        } finally {
            activeEncoder.remove(encoderEngine)
            if (fileList != null) {
                FileSupport.deleteQuietly(fileList)
            }
        }

        // concat did not return a file
        if (!output.exists() || output.length() == 0L)
            return none()

        // Put the file in the workspace
        val workspaceURI = putToCollection(job, output, "concatenated file")

        // Have the concat track inspected and return the result
        val inspectedTrack = inspect(job, workspaceURI)
        inspectedTrack.identifier = targetTrackId

        return some(inspectedTrack)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun imageToVideo(sourceImageAttachment: Attachment, profileId: String, time: Double): Job {
        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.ImageToVideo.toString(), Arrays.asList(
                    profileId, MediaPackageElementParser.getAsXml(sourceImageAttachment), java.lang.Double.toString(time)),
                    profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create image to video job", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun imageToVideo(job: Job, sourceImage: Attachment, profileId: String, time: Double?): Option<Track> {
        var time = time

        // Get the encoding profile
        val profile = getProfile(job, profileId)

        val targetTrackId = idBuilder.createNew().toString()
        // Get the attachment and make sure it exist
        val imageFile: File
        try {
            imageFile = workspace!!.get(sourceImage.getURI())
        } catch (e: NotFoundException) {
            incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                    getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS)
            throw EncoderException("Requested source image $sourceImage is not found")
        } catch (e: IOException) {
            incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                    getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS)
            throw EncoderException("Unable to access source image $sourceImage")
        }

        // Create the engine
        val encoderEngine = encoderEngine

        logger.info("Converting image attachment {} into video {}", sourceImage.identifier, targetTrackId)

        val properties = HashMap<String, String>()
        if (time == -1)
            time = 0.0

        val ffmpegFormat = DecimalFormatSymbols()
        ffmpegFormat.decimalSeparator = '.'
        val df = DecimalFormat("0.000", ffmpegFormat)
        properties["time"] = df.format(time)

        val output: File
        try {
            output = encoderEngine.encode(imageFile, profile, properties)
        } catch (e: EncoderException) {
            val params = HashMap<String, String>()
            params["image"] = sourceImage.getURI().toString()
            params["profile"] = profile.identifier
            params["properties"] = properties.toString()
            incident().recordFailure(job, IMAGE_TO_VIDEO_FAILED, e, params, detailsFor(e, encoderEngine))
            throw e
        } finally {
            activeEncoder.remove(encoderEngine)
        }

        // encoding did not return a file
        if (!output.exists() || output.length() == 0L)
            return none()

        // Put the file in the workspace
        val workspaceURI = putToCollection(job, output, "converted image file")

        // Have the compound track inspected and return the result
        val inspectedTrack = inspect(job, workspaceURI)
        inspectedTrack.identifier = targetTrackId

        return some(inspectedTrack)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.image
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun image(sourceTrack: Track?, profileId: String, vararg times: Double): Job {
        if (sourceTrack == null)
            throw IllegalArgumentException("SourceTrack cannot be null")

        if (times.size == 0)
            throw IllegalArgumentException("At least one time argument has to be specified")

        val parameters = ArrayList<String>()
        parameters.add(profileId)
        parameters.add(MediaPackageElementParser.getAsXml(sourceTrack))
        parameters.add(java.lang.Boolean.TRUE.toString())
        for (time in times) {
            parameters.add(java.lang.Double.toString(time))
        }

        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Image.toString(), parameters, profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun imageSync(sourceTrack: Track, profileId: String, vararg time: Double): List<Attachment> {
        var job: Job? = null
        try {
            val profile = profileScanner!!.getProfile(profileId)
            job = serviceRegistry!!
                    .createJob(
                            ComposerService.JOB_TYPE, Operation.Image.toString(), null!!, null!!, false, profile.jobLoad)
            job.status = Job.Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val images = image(job, sourceTrack, profileId, *time)
            job.status = Job.Status.FINISHED
            return images
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        } catch (e: NotFoundException) {
            throw EncoderException("Unable to create a job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun image(sourceTrack: Track?, profileId: String, properties: Map<String, String>): Job {
        if (sourceTrack == null)
            throw IllegalArgumentException("SourceTrack cannot be null")

        val arguments = ArrayList<String>()
        arguments.add(profileId)
        arguments.add(MediaPackageElementParser.getAsXml(sourceTrack))
        arguments.add(java.lang.Boolean.FALSE.toString())
        arguments.add(getPropertiesAsString(properties))

        try {
            val profile = profileScanner!!.getProfile(profileId)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Image.toString(), arguments, profile.jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * Extracts an image from `sourceTrack` at the given point in time.
     *
     * @param job
     * the associated job
     * @param sourceTrack
     * the source track
     * @param profileId
     * the identifier of the encoding profile to use
     * @param times
     * (one or more) times in seconds
     * @return the images as an attachment element list
     * @throws EncoderException
     * if extracting the image fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    protected fun image(job: Job, sourceTrack: Track?, profileId: String, vararg times: Double): List<Attachment> {
        if (sourceTrack == null)
            throw EncoderException("SourceTrack cannot be null")

        validateVideoStream(job, sourceTrack)

        // The time should not be outside of the track's duration
        for (time in times) {
            if (sourceTrack.duration == null) {
                val params = HashMap<String, String>()
                params["track-id"] = sourceTrack.identifier
                params["track-url"] = sourceTrack.getURI().toString()
                incident().recordFailure(job, IMAGE_EXTRACTION_UNKNOWN_DURATION, params)
                throw EncoderException("Unable to extract an image from a track with unknown duration")
            }
            if (time < 0 || time * 1000 > sourceTrack.duration) {
                val duration = Duration.ofMillis(sourceTrack.duration!!)
                val durationSeconds = duration.seconds
                val durationMillis = duration.minusSeconds(durationSeconds).toMillis()
                val formattedDuration = (durationSeconds.toString() + '.'.toString()
                        + StringUtils.leftPad(durationMillis.toString(), 3, '0'))

                val params = HashMap<String, String>()
                params["track-id"] = sourceTrack.identifier
                params["track-url"] = sourceTrack.getURI().toString()
                params["track-duration"] = formattedDuration
                params["time"] = java.lang.Double.toString(time)
                incident().recordFailure(job, IMAGE_EXTRACTION_TIME_OUTSIDE_DURATION, params)

                throw EncoderException("An image could not be extracted from the track " + sourceTrack.getURI()
                        + " with id " + sourceTrack.identifier + " because the extraction time (" + time + " second(s)) is "
                        + "outside of the track's duration (" + formattedDuration + " second(s))")
            }
        }

        return extractImages(job, sourceTrack, profileId, null, *times)
    }

    /**
     * Extracts an image from `sourceTrack` by the given properties and the corresponding encoding profile.
     *
     * @param job
     * the associated job
     * @param sourceTrack
     * the source track
     * @param profileId
     * the identifier of the encoding profile to use
     * @param properties
     * the properties applied to the encoding profile
     * @return the images as an attachment element list
     * @throws EncoderException
     * if extracting the image fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    protected fun image(job: Job, sourceTrack: Track?, profileId: String, properties: Map<String, String>): List<Attachment> {
        if (sourceTrack == null)
            throw EncoderException("SourceTrack cannot be null")

        validateVideoStream(job, sourceTrack)

        return extractImages(job, sourceTrack, profileId, properties)
    }

    @Throws(EncoderException::class)
    private fun extractImages(job: Job, sourceTrack: Track, profileId: String, properties: Map<String, String>?,
                              vararg times: Double): List<Attachment> {
        logger.info("creating an image using video track {}", sourceTrack.identifier)

        // Get the encoding profile
        val profile = getProfile(job, profileId)

        // Create the encoding engine
        val encoderEngine = encoderEngine

        // Finally get the file that needs to be encoded
        val videoFile = loadTrackIntoWorkspace(job, "video", sourceTrack, true)

        // Do the work
        val encodingOutput: List<File>?
        try {
            encodingOutput = encoderEngine.extract(videoFile, profile, properties, *times)
            // check for validity of output
            if (encodingOutput == null || encodingOutput.isEmpty()) {
                logger.error("Image extraction from video {} with profile {} failed: no images were produced",
                        sourceTrack.getURI(), profile.identifier)
                throw EncoderException("Image extraction failed: no images were produced")
            }
        } catch (e: EncoderException) {
            val params = HashMap<String, String>()
            params["video"] = sourceTrack.getURI().toString()
            params["profile"] = profile.identifier
            params["positions"] = Arrays.toString(times)
            incident().recordFailure(job, IMAGE_EXTRACTION_FAILED, e, params, detailsFor(e, encoderEngine))
            throw e
        } finally {
            activeEncoder.remove(encoderEngine)
        }

        var i = 0
        val workspaceURIs = LinkedList<URI>()
        for (output in encodingOutput!!) {

            if (!output.exists() || output.length() == 0L) {
                logger.warn("Extracted image {} is empty!", output)
                throw EncoderException("Extracted image $output is empty!")
            }

            // Put the file in the workspace

            try {
                FileInputStream(output).use { `in` ->
                    val returnURL = workspace!!.putInCollection(COLLECTION,
                            job.id.toString() + "_" + i++ + "." + FilenameUtils.getExtension(output.absolutePath), `in`)
                    logger.debug("Copied image file to the workspace at {}", returnURL)
                    workspaceURIs.add(returnURL)
                }
            } catch (e: Exception) {
                cleanup(*encodingOutput.toTypedArray())
                cleanupWorkspace(*workspaceURIs.toTypedArray())
                incident().recordFailure(job, WORKSPACE_PUT_COLLECTION_IO_EXCEPTION, e,
                        getWorkspaceCollectionParams("extracted image file", COLLECTION, output.toURI()), NO_DETAILS)
                throw EncoderException("Unable to put image file into the workspace", e)
            }

        }

        // cleanup
        cleanup(*encodingOutput.toTypedArray())
        cleanup(videoFile)

        val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        val imageAttachments = LinkedList<Attachment>()
        for (url in workspaceURIs) {
            val attachment = builder.elementFromURI(url, Attachment.TYPE, null!!) as Attachment
            imageAttachments.add(attachment)
        }

        return imageAttachments
    }

    @Throws(EncoderException::class)
    private fun validateVideoStream(job: Job, sourceTrack: Track?) {
        // make sure there is a video stream in the track
        if (sourceTrack != null && !sourceTrack.hasVideo()) {
            val params = HashMap<String, String>()
            params["track-id"] = sourceTrack.identifier
            params["track-url"] = sourceTrack.getURI().toString()
            incident().recordFailure(job, IMAGE_EXTRACTION_NO_VIDEO, params)
            throw EncoderException("Cannot extract an image without a video stream")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.convertImage
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun convertImage(image: Attachment?, vararg profileIds: String): Job {
        if (image == null)
            throw IllegalArgumentException("Source image cannot be null")

        if (profileIds == null)
            throw IllegalArgumentException("At least one encoding profile must be set")

        val gson = Gson()
        val params = Arrays.asList(gson.toJson(profileIds), MediaPackageElementParser.getAsXml(image))
        val jobLoad = Arrays.stream(profileIds)
                .map { p -> profileScanner!!.getProfile(p).jobLoad }
                .max { f1, f2 -> java.lang.Float.compare(f1, f2) }
                .orElse(0f)
        try {
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.ImageConversion.toString(), params, jobLoad)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.convertImageSync
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun convertImageSync(image: Attachment, vararg profileIds: String): List<Attachment> {
        var job: Job? = null
        try {
            val jobLoad = Arrays.stream(profileIds)
                    .map { p -> profileScanner!!.getProfile(p) }
                    .mapToDouble(ToDoubleFunction<EncodingProfile> { it.getJobLoad() })
                    .max()
                    .orElse(0.0).toFloat()
            job = serviceRegistry!!
                    .createJob(
                            ComposerService.JOB_TYPE, Operation.Image.toString(), null!!, null!!, false, jobLoad)
            job.status = Job.Status.RUNNING
            job = serviceRegistry!!.updateJob(job)
            val results = convertImage(job, image, *profileIds)
            job.status = Job.Status.FINISHED
            if (results.isEmpty()) {
                throw EncoderException(format(
                        "Unable to convert image %s with encoding profiles %s. The result set is empty.",
                        image.getURI().toString(), profileIds))
            }
            return results
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        } catch (e: NotFoundException) {
            throw EncoderException("Unable to create a job", e)
        } finally {
            finallyUpdateJob(job)
        }
    }

    /**
     * Converts an image from `sourceImage` to a new format.
     *
     * @param job
     * the associated job
     * @param sourceImage
     * the source image
     * @param profileIds
     * the identifier of the encoding profiles to use
     * @return the list of converted images as an attachment.
     * @throws EncoderException
     * if converting the image fails
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    private fun convertImage(job: Job, sourceImage: Attachment, vararg profileIds: String): List<Attachment> {
        val convertedImages = ArrayList<Attachment>()
        val encoderEngine = encoderEngine
        try {
            for (profileId in profileIds) {
                logger.info("Converting {} using encoding profile {}", sourceImage, profileId)

                // Get the encoding profile
                val profile = getProfile(job, profileId)

                // Finally get the file that needs to be encoded
                val imageFile: File
                try {
                    imageFile = workspace!!.get(sourceImage.getURI())
                } catch (e: NotFoundException) {
                    incident().recordFailure(job, WORKSPACE_GET_NOT_FOUND, e,
                            getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS)
                    throw EncoderException("Requested attachment $sourceImage was not found", e)
                } catch (e: IOException) {
                    incident().recordFailure(job, WORKSPACE_GET_IO_EXCEPTION, e,
                            getWorkspaceMediapackageParams("source image", sourceImage), NO_DETAILS)
                    throw EncoderException("Error accessing attachment $sourceImage", e)
                }

                // Do the work
                val output: File
                try {
                    output = encoderEngine.encode(imageFile, profile, null)
                } catch (e: EncoderException) {
                    val params = HashMap<String, String>()
                    params["image"] = sourceImage.getURI().toString()
                    params["profile"] = profile.identifier
                    incident().recordFailure(job, CONVERT_IMAGE_FAILED, e, params, detailsFor(e, encoderEngine))
                    throw e
                }

                // encoding did not return a file
                if (!output.exists() || output.length() == 0L)
                    throw EncoderException(format(
                            "Image conversion job %d didn't created an output file for the source image %s with encoding profile %s",
                            job.id, sourceImage.getURI().toString(), profileId))

                // Put the file in the workspace
                val workspaceURI = putToCollection(job, output, "converted image file")

                val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                val convertedImage = builder.elementFromURI(workspaceURI, Attachment.TYPE, null!!) as Attachment
                convertedImage.identifier = idBuilder.createNew().toString()
                try {
                    convertedImage.mimeType = MimeTypes.fromURI(convertedImage.getURI())
                } catch (e: UnknownFileTypeException) {
                    logger.warn("Mime type unknown for file {}. Setting none.", convertedImage.getURI(), e)
                }

                convertedImages.add(convertedImage)
            }
        } catch (t: Throwable) {
            for (convertedImage in convertedImages) {
                try {
                    workspace!!.delete(convertedImage.getURI())
                } catch (ex: NotFoundException) {
                    // do nothing here
                } catch (ex: IOException) {
                    logger.warn("Unable to delete converted image {} from workspace", convertedImage.getURI(), ex)
                }

            }
            throw t
        } finally {
            activeEncoder.remove(encoderEngine)
        }
        return convertedImages
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(ServiceRegistryException::class)
    public override fun process(job: Job): String {
        val operation = job.operation
        val arguments = job.arguments
        try {
            val op = Operation.valueOf(operation)
            val firstTrack: Track
            val secondTrack: Track
            val encodingProfile = arguments[0]
            val serialized: String

            when (op) {
                ComposerServiceImpl.Operation.Encode -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    serialized = encode(job, Collections.map(tuple("video", firstTrack)), encodingProfile).map(
                            MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.ParallelEncode -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    serialized = MediaPackageElementParser.getArrayAsXml(parallelEncode(job, firstTrack, encodingProfile))
                }
                ComposerServiceImpl.Operation.Image -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    val resultingElements: List<Attachment>
                    if (java.lang.Boolean.parseBoolean(arguments[2])) {
                        val times = DoubleArray(arguments.size - 3)
                        for (i in 3 until arguments.size) {
                            times[i - 3] = java.lang.Double.parseDouble(arguments[i])
                        }
                        resultingElements = image(job, firstTrack, encodingProfile, *times)
                    } else {
                        val properties = parseProperties(arguments[3])
                        resultingElements = image(job, firstTrack, encodingProfile, properties)
                    }
                    serialized = MediaPackageElementParser.getArrayAsXml(resultingElements)
                }
                ComposerServiceImpl.Operation.ImageConversion -> {
                    val gson = Gson()
                    val encodingProfilesArr = gson.fromJson(arguments[0], Array<String>::class.java)
                    val sourceImage = MediaPackageElementParser.getFromXml(arguments[1]) as Attachment
                    val convertedImages = convertImage(job, sourceImage, *encodingProfilesArr)
                    serialized = MediaPackageElementParser.getArrayAsXml(convertedImages)
                }
                ComposerServiceImpl.Operation.Mux -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    secondTrack = MediaPackageElementParser.getFromXml(arguments[2]) as Track
                    serialized = mux(job, firstTrack, secondTrack, encodingProfile).map(
                            MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.Trim -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    val start = java.lang.Long.parseLong(arguments[2])
                    val duration = java.lang.Long.parseLong(arguments[3])
                    serialized = trim(job, firstTrack, encodingProfile, start, duration).map(
                            MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.Composite -> {
                    val watermarkAttachment: Attachment
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[LOWER_TRACK_INDEX]) as Track
                    val lowerLayout = Serializer.layout(JsonObj.jsonObj(arguments[LOWER_TRACK_LAYOUT_INDEX]))
                    val lowerLaidOutElement = LaidOutElement(firstTrack, lowerLayout)
                    var upperLaidOutElement = Option.none()
                    if (NOT_AVAILABLE == arguments[UPPER_TRACK_INDEX] && NOT_AVAILABLE == arguments[UPPER_TRACK_LAYOUT_INDEX]) {
                        logger.trace("This composite action does not use a second track.")
                    } else {
                        secondTrack = MediaPackageElementParser.getFromXml(arguments[UPPER_TRACK_INDEX]) as Track
                        val upperLayout = Serializer.layout(JsonObj.jsonObj(arguments[UPPER_TRACK_LAYOUT_INDEX]))
                        upperLaidOutElement = Option.option(LaidOutElement(secondTrack, upperLayout))
                    }
                    val compositeTrackSize = Serializer
                            .dimension(JsonObj.jsonObj(arguments[COMPOSITE_TRACK_SIZE_INDEX]))
                    val backgroundColor = arguments[BACKGROUND_COLOR_INDEX]
                    val audioSourceName = arguments[AUDIO_SOURCE_INDEX]

                    var watermarkOption = Option.none()
                    if (arguments.size == 9) {
                        watermarkAttachment = MediaPackageElementParser.getFromXml(arguments[WATERMARK_INDEX]) as Attachment
                        val watermarkLayout = Serializer.layout(JsonObj.jsonObj(arguments[WATERMARK_LAYOUT_INDEX]))
                        watermarkOption = Option.some(LaidOutElement<A>(watermarkAttachment, watermarkLayout))
                    }
                    serialized = composite(job, compositeTrackSize, lowerLaidOutElement, upperLaidOutElement, watermarkOption,
                            encodingProfile, backgroundColor, audioSourceName).map(MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.Concat -> {
                    val dimensionString = arguments[1]
                    val frameRateString = arguments[2]
                    var outputDimension: Dimension? = null
                    if (StringUtils.isNotBlank(dimensionString))
                        outputDimension = Serializer.dimension(JsonObj.jsonObj(dimensionString))
                    val outputFrameRate = NumberUtils.toFloat(frameRateString, -1.0f)
                    val sameCodec = java.lang.Boolean.parseBoolean(arguments[3])
                    val tracks = ArrayList<Track>()
                    for (i in 4 until arguments.size) {
                        tracks.add(i - 4, MediaPackageElementParser.getFromXml(arguments[i]) as Track)
                    }
                    serialized = concat(job, tracks, encodingProfile, outputDimension, outputFrameRate, sameCodec).map(
                            MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.ImageToVideo -> {
                    val image = MediaPackageElementParser.getFromXml(arguments[1]) as Attachment
                    val time = java.lang.Double.parseDouble(arguments[2])
                    serialized = imageToVideo(job, image, encodingProfile, time)
                            .map(MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                ComposerServiceImpl.Operation.Demux -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[1]) as Track
                    var outTracks = demux(job, firstTrack, encodingProfile)
                    serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks))
                }
                ComposerServiceImpl.Operation.ProcessSmil -> {
                    val smil = this.smilService!!.fromXml(arguments[0]).smil // Pass the entire smil
                    val trackParamGroupId = arguments[1] // Only process this track
                    val mediaType = arguments[2] // v=video,a=audio,otherwise both
                    val encodingProfiles = arguments.subList(3, arguments.size)
                    outTracks = processSmil(job, smil, trackParamGroupId, mediaType, encodingProfiles)
                    serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks))
                }
                ComposerServiceImpl.Operation.MultiEncode -> {
                    firstTrack = MediaPackageElementParser.getFromXml(arguments[0]) as Track
                    val encodingProfiles2 = arguments.subList(1, arguments.size)
                    outTracks = multiEncode(job, firstTrack, encodingProfiles2)
                    serialized = StringUtils.trimToEmpty(MediaPackageElementParser.getArrayAsXml(outTracks))
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }

            return serialized
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException(format("Cannot handle operations of type '%s'", operation), e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException(format("Invalid arguments for operation '%s'", operation), e)
        } catch (e: Exception) {
            throw ServiceRegistryException(format("Error handling operation '%s'", operation), e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.listProfiles
     */
    override fun listProfiles(): Array<EncodingProfile> {
        val profiles = profileScanner!!.profiles.values
        return profiles.toTypedArray()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.getProfile
     */
    override fun getProfile(profileId: String): EncodingProfile {
        return profileScanner!!.profiles[profileId]
    }

    @Throws(EncoderException::class)
    protected open fun inspect(job: Job, uris: List<URI>): List<Track> {
        // Start inspection jobs
        val inspectionJobs = arrayOfNulls<Job>(uris.size)
        for (i in uris.indices) {
            try {
                inspectionJobs[i] = inspectionService!!.inspect(uris[i])
            } catch (e: MediaInspectionException) {
                incident().recordJobCreationIncident(job, e)
                throw EncoderException(String.format("Media inspection of %s failed", uris[i]), e)
            }

        }

        // Wait for inspection jobs
        val barrier = JobBarrier(job, serviceRegistry!!, *inspectionJobs)
        if (!barrier.waitForJobs()!!.isSuccess) {
            for ((key, value) in barrier.status!!.status) {
                if (value !== Job.Status.FINISHED) {
                    logger.error("Media inspection failed in job {}: {}", key, value)
                }
            }
            throw EncoderException("Inspection of encoded file failed")
        }

        // De-serialize tracks
        val results = ArrayList<Track>(uris.size)
        for (inspectionJob in inspectionJobs) {
            try {
                results.add(MediaPackageElementParser.getFromXml(inspectionJob.payload) as Track)
            } catch (e: MediaPackageException) {
                throw EncoderException(e)
            }

        }
        return results
    }

    @Throws(EncoderException::class)
    protected open fun inspect(job: Job, workspaceURI: URI): Track {
        return inspect(job, listOf(workspaceURI))[0]
    }

    /**
     * Deletes any valid file in the list.
     *
     * @param encodingOutput
     * list of files to be deleted
     */
    private fun cleanup(vararg encodingOutput: File) {
        for (file in encodingOutput) {
            if (file != null && file.isFile) {
                val path = file.absolutePath
                if (file.delete()) {
                    logger.info("Deleted local copy of encoding file at {}", path)
                } else {
                    logger.warn("Could not delete local copy of encoding file at {}", path)
                }
            }
        }
    }

    private fun cleanupWorkspace(vararg workspaceURIs: URI) {
        for (url in workspaceURIs) {
            try {
                workspace!!.delete(url)
            } catch (e: Exception) {
                logger.warn("Could not delete {} from workspace: {}", url, e.message)
            }

        }
    }

    @Throws(EncoderException::class)
    private fun getProfile(job: Job, profileId: String): EncodingProfile {
        val profile = profileScanner!!.getProfile(profileId)
        if (profile == null) {
            val msg = format("Profile %s is unknown", profileId)
            logger.error(msg)
            incident().recordFailure(job, PROFILE_NOT_FOUND, Collections.map(tuple("profile", profileId)))
            throw EncoderException(msg)
        }
        return profile
    }

    private fun getWorkspaceMediapackageParams(description: String, element: MediaPackageElement): Map<String, String> {
        return Collections.map(tuple("description", description),
                tuple("type", element.elementType.toString()),
                tuple("url", element.getURI().toString()))
    }

    private fun getWorkspaceCollectionParams(description: String, collectionId: String, url: URI): Map<String, String> {
        val params = HashMap<String, String>()
        params["description"] = description
        params["collection"] = collectionId
        params["url"] = url.toString()
        return params
    }

    private fun buildConcatCommand(onlyAudio: Boolean, dimension: Dimension, outputFrameRate: Float, files: List<File>, tracks: List<Track>): String {
        val sb = StringBuilder()

        // Add input file paths
        for (f in files) {
            sb.append("-i ").append(f.absolutePath).append(" ")
        }
        sb.append("-filter_complex ")

        var hasAudio = false
        if (!onlyAudio) {
            // fps video filter if outputFrameRate is valid
            var fpsFilter = StringUtils.EMPTY
            if (outputFrameRate > 0) {
                fpsFilter = format(Locale.US, "fps=fps=%f,", outputFrameRate)
            }
            // Add video scaling and check for audio
            var characterCount = 0
            for (i in files.indices) {
                if (i % 25 == 0)
                    characterCount++
                sb.append("[").append(i).append(":v]").append(fpsFilter)
                        .append("scale=iw*min(").append(dimension.width).append("/iw\\,").append(dimension.height)
                        .append("/ih):ih*min(").append(dimension.width).append("/iw\\,").append(dimension.height)
                        .append("/ih),pad=").append(dimension.width).append(":").append(dimension.height)
                        .append(":(ow-iw)/2:(oh-ih)/2").append(",setdar=")
                        .append(dimension.width.toFloat() / dimension.height.toFloat()).append("[")
                val character = 'a'.toInt() + i + 1 - (characterCount - 1) * 25
                for (y in 0 until characterCount) {
                    sb.append(character.toChar())
                }
                sb.append("];")
                if (tracks[i].hasAudio())
                    hasAudio = true
            }

            // Add silent audio streams if at least one audio stream is available
            if (hasAudio) {
                for (i in files.indices) {
                    if (!tracks[i].hasAudio())
                        sb.append("aevalsrc=0:d=1[silent").append(i + 1).append("];")
                }
            }
        }

        // Add concat segments
        var characterCount = 0
        for (i in files.indices) {
            if (i % 25 == 0)
                characterCount++

            val character = 'a'.toInt() + i + 1 - (characterCount - 1) * 25
            if (!onlyAudio) {
                sb.append("[")
                for (y in 0 until characterCount) {
                    sb.append(character.toChar())
                }
                sb.append("]")
            }

            if (tracks[i].hasAudio()) {
                sb.append("[").append(i).append(":a]")
            } else if (hasAudio) {
                sb.append("[silent").append(i + 1).append("]")
            }
        }

        // Add concat command and output mapping
        sb.append("concat=n=").append(files.size).append(":v=")
        if (onlyAudio) {
            sb.append("0")
        } else {
            sb.append("1")
        }
        sb.append(":a=")

        if (!onlyAudio) {
            if (hasAudio) {
                sb.append("1[v][a] -map [v] -map [a] ")
            } else {
                sb.append("0[v] -map [v] ")
            }
        } else {
            sb.append("1[a] -map [a]")
        }
        return sb.toString()
    }

    @Throws(EncoderException::class)
    private fun putToCollection(job: Job, files: List<File>, description: String): List<URI> {
        val returnURLs = ArrayList<URI>(files.size)
        for (file in files) {
            try {
                FileInputStream(file).use { `in` ->
                    val newFileName = format("%s.%s", job.id, FilenameUtils.getName(file.absolutePath))
                    val newFileURI = workspace!!.putInCollection(COLLECTION, newFileName, `in`)
                    logger.info("Copied the {} to the workspace at {}", description, newFileURI)
                    returnURLs.add(newFileURI)
                }
            } catch (e: Exception) {
                incident().recordFailure(job, WORKSPACE_PUT_COLLECTION_IO_EXCEPTION, e,
                        getWorkspaceCollectionParams(description, COLLECTION, file.toURI()), NO_DETAILS)
                returnURLs.forEach(Consumer<URI> { this.cleanupWorkspace(it) })
                throw EncoderException("Unable to put the $description into the workspace", e)
            } finally {
                cleanup(file)
            }
        }
        return returnURLs
    }

    @Throws(EncoderException::class)
    private fun putToCollection(job: Job, output: File, description: String): URI {
        return putToCollection(job, listOf(output), description)[0]
    }

    @Throws(IOException::class)
    private fun parseProperties(serializedProperties: String): Map<String, String> {
        val properties = Properties()
        IOUtils.toInputStream(serializedProperties, "UTF-8").use { `in` ->
            properties.load(`in`)
            val map = HashMap<String, String>()
            for ((key, value) in properties) {
                map[key as String] = value as String
            }
            return map
        }
    }

    private fun getPropertiesAsString(props: Map<String, String>): String {
        val sb = StringBuilder()
        for ((key, value) in props) {
            sb.append(key)
            sb.append("=")
            sb.append(value)
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Sets the media inspection service
     *
     * @param mediaInspectionService
     * an instance of the media inspection service
     */
    fun setMediaInspectionService(mediaInspectionService: MediaInspectionService) {
        this.inspectionService = mediaInspectionService
    }

    /**
     * Sets the workspace
     *
     * @param workspace
     * an instance of the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Sets the profile scanner.
     *
     * @param scanner
     * the profile scanner
     */
    fun setProfileScanner(scanner: EncodingProfileScanner) {
        this.profileScanner = scanner
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun demux(sourceTrack: Track, profileId: String): Job {
        try {
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.Demux.toString(),
                    Arrays.asList(profileId, MediaPackageElementParser.getAsXml(sourceTrack)))
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    @Throws(EncoderException::class)
    private fun demux(job: Job?, videoTrack: Track?, encodingProfile: String): List<Track>? {
        if (job == null)
            throw IllegalArgumentException("The Job parameter must not be null")

        try {
            // Get the track and make sure it exists
            val videoFile = if (videoTrack != null) loadTrackIntoWorkspace(job, "source", videoTrack, false) else null

            // Get the encoding profile
            val profile = getProfile(job, encodingProfile)
            // Create the engine/get
            logger.info(format("Encoding video track %s using profile '%s'", videoTrack!!.identifier, profile))
            val encoderEngine = encoderEngine

            // Do the work
            val outputs: List<File>
            try {
                val source = HashMap<String, File>()
                source["video"] = videoFile
                outputs = encoderEngine.process(source, profile, null)
            } catch (e: EncoderException) {
                val params = HashMap<String, String>()
                params["video"] = if (videoFile != null) videoTrack.getURI().toString() else "EMPTY"
                params["profile"] = profile.identifier
                params["properties"] = "EMPTY"
                incident().recordFailure(job, ENCODING_FAILED, e, params, detailsFor(e, encoderEngine))
                throw e
            } finally {
                activeEncoder.remove(encoderEngine)
            }

            // demux did not return a file
            if (outputs.isEmpty() || !outputs[0].exists() || outputs[0].length() == 0L)
                return null

            val workspaceURIs = putToCollection(job, outputs, "demuxed file")
            val tracks = inspect(job, workspaceURIs)
            tracks.forEach { track -> track.identifier = idBuilder.createNew().toString() }
            return tracks
        } catch (e: Exception) {
            logger.warn("Demux/MultiOutputEncode operation failed to encode " + videoTrack!!, e)
            if (e is EncoderException) {
                throw e
            } else {
                throw EncoderException(e)
            }
        }

    }

    /**
     * OSGI callback when the configuration is updated. This method is only here to prevent the
     * configuration admin service from calling the service deactivate and activate methods
     * for a config update. It does not have to do anything as the updates are handled by updated().
     */
    @Throws(ConfigurationException::class)
    fun modified(config: Map<String, Any>) {
        logger.debug("Modified")
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        maxMultipleProfilesJobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_MAX_MULTIPLE_PROFILES,
                DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES, serviceRegistry!!)
        processSmilJobLoadFactor = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_FACTOR_PROCESS_SMIL,
                DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR, serviceRegistry!!)
        if (processSmilJobLoadFactor == 0f) {
            processSmilJobLoadFactor = DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR
        }
        transitionDuration = 1000 * LoadUtil.getConfiguredLoadValue(properties, PROCESS_SMIL_CLIP_TRANSITION_DURATION,
                DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION, serviceRegistry!!).toInt()
    }

    /**
     * ProcessSmil processes editing of one source group (which may contain multiple source tracks) to one set of outputs
     * (to one or more encoding profiles). Note that the source tracks are expected to have the same dimensions.
     *
     * @param smil
     * - smil containing with video names and clip sections from them
     * @param trackparamId
     * - group id
     * @param mediaType
     * - VIDEO_ONLY, AUDIO_ONLY, or "" if neither is true
     * @param profileIds
     * - list of encoding profile Ids
     * @return Compose Job
     * @throws EncoderException
     * - if encoding fails
     * @throws MediaPackageException
     * - if missing files or bad mp
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun processSmil(smil: Smil, trackparamId: String?, mediaType: String, profileIds: List<String>): Job {
        try {
            val al = ArrayList<String>()
            al.add(smil.toXML())
            al.add(trackparamId) // place holder for param ID
            al.add(mediaType) // audio, video or av
            for (i in profileIds) {
                al.add(i)
            }
            val load = calculateJobLoadForMultipleProfiles(profileIds, processSmilJobLoadFactor)
            try {
                for (paramGroup in smil.head.paramGroups) {
                    for (param in paramGroup.params) {
                        if (SmilMediaParam.PARAM_NAME_TRACK_ID == param.name) {
                            if (trackparamId == null || trackparamId == paramGroup.id) { // any track or specific groupid
                                al[1] = paramGroup.id
                                return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.ProcessSmil.toString(), al, load)
                            }
                        }
                    }
                }
            } catch (e: ServiceRegistryException) {
                throw EncoderException("Unable to create a job", e)
            } catch (e: Exception) {
                throw EncoderException("Unable to create a job - Exception in Parsing Smil", e)
            }

        } catch (e: Exception) {
            throw EncoderException("Unable to create a job - Exception processing XML in ProcessSmil", e)
        }

        throw EncoderException("Unable to create a job - Cannot find paramGroup")
    }

    private fun findSuitableProfiles(encodingProfiles: List<String>, mediaType: String): List<EncodingProfile> {
        val profiles = ArrayList<EncodingProfile>()
        for (profileId1 in encodingProfiles) { // Check for mismatched profiles/media types
            val profile = profileScanner!!.getProfile(profileId1)
            // warn about bad encoding profiles, but encode anyway, the profile type is not enforced
            if (ComposerService.VIDEO_ONLY == mediaType && profile.applicableMediaType == EncodingProfile.MediaType.Audio) {
                logger.warn("Profile '{}' supports {} but media is Video Only", profileId1, profile.applicableMediaType)
            } else if (ComposerService.AUDIO_ONLY == mediaType && profile.applicableMediaType == EncodingProfile.MediaType.Visual) {
                logger.warn("Profile '{}' supports {} but media is Audio Only", profileId1, profile.applicableMediaType)
            }
            profiles.add(profile)
        }
        return profiles
    }

    /**
     * Fetch specified or first SmilMediaParamGroup from smil
     *
     * @param smil
     * - smil object
     * @param trackParamGroupId
     * - id for a particular param group or null
     * @return a named track group by id, if id is not specified, get first param group
     * @throws EncoderException
     */
    @Throws(EncoderException::class)
    private fun getSmilMediaParamGroup(smil: Smil, trackParamGroupId: String?): SmilMediaParamGroup {
        var trackParamGroupId = trackParamGroupId
        try { // Find a track group id if not specified, get first param group
            if (trackParamGroupId == null)
                for (paramGroup in smil.head.paramGroups) {
                    for (param in paramGroup.params) {
                        if (SmilMediaParam.PARAM_NAME_TRACK_ID == param.name) {
                            trackParamGroupId = paramGroup.id
                            break
                        }
                    }
                }
            return smil.get(trackParamGroupId) as SmilMediaParamGroup // If we want to concat multiple files
        } catch (ex: SmilException) {
            throw EncoderException("Smil does not contain a paramGroup element with Id " + trackParamGroupId!!, ex)
        }

    }

    /**
     * Splice segments given by smil document for the given track to the new one. This function reads the smil file and
     * reduce them to arguments to send to the encoder
     *
     * @param job
     * processing job
     * @param smil
     * smil document with media segments description
     * @param trackParamGroupId
     * source track group
     * @param mediaType
     * VIDEO_ONLY or AUDIO_ONLY or "" if it has both
     * @param encodingProfiles
     * - profiles
     * @return serialized array of processed tracks
     * @throws EncoderException
     * if an error occurred
     * @throws MediaPackageException
     * - bad Mediapackage
     * @throws URISyntaxException
     */
    @Throws(EncoderException::class, MediaPackageException::class, URISyntaxException::class)
    protected fun processSmil(job: Job, smil: Smil, trackParamGroupId: String, mediaType: String,
                              encodingProfiles: List<String>): List<Track> {

        val profiles = findSuitableProfiles(encodingProfiles, mediaType)
        // If there are no usable encoding profiles, throw exception
        if (profiles.size == 0)
            throw EncoderException(
                    "ProcessSmil - Media is not supported by the assigned encoding Profiles '$encodingProfiles'")

        val trackParamGroup: SmilMediaParamGroup
        val inputfile = ArrayList<String>()
        val props = HashMap<String, String>()

        val videoclips = ArrayList<VideoClip>()
        trackParamGroup = getSmilMediaParamGroup(smil, trackParamGroupId)

        var sourceTrackId: String? = null
        var sourceTrackFlavor: MediaPackageElementFlavor? = null
        var sourceTrackUri: String? = null
        var sourceFile: File? = null

        // get any source track from track group to get metadata
        for (param in trackParamGroup.params) {
            if (SmilMediaParam.PARAM_NAME_TRACK_ID == param.name) {
                sourceTrackId = param.value
            } else if (SmilMediaParam.PARAM_NAME_TRACK_SRC == param.name) {
                sourceTrackUri = param.value
            } else if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR == param.name) {
                sourceTrackFlavor = MediaPackageElementFlavor.parseFlavor(param.value)
            }
        }

        logger.info("ProcessSmil: Start processing track {}", sourceTrackUri)
        sourceFile = loadURIIntoWorkspace(job, "source", URI(sourceTrackUri!!))
        inputfile.add(sourceFile.absolutePath) // default source - add to source table as 0
        props["in.video.path"] = sourceFile.absolutePath
        val srcIndex = inputfile.indexOf(sourceFile.absolutePath) // index = 0
        try {
            val outputs: List<File>
            // parse body elements
            for (element in smil.body.mediaElements) {
                // body should contain par elements
                if (element.isContainer) {
                    val container = element as SmilMediaContainer
                    if (SmilMediaContainer.ContainerType.PAR == container.containerType) {
                        // par element should contain media elements
                        for (elementChild in container.elements) {
                            if (!elementChild.isContainer) {
                                val media = elementChild as SmilMediaElement
                                if (trackParamGroupId == media.paramGroup) {
                                    val begin = media.clipBeginMS
                                    val end = media.clipEndMS
                                    val clipTrackURI = media.src
                                    var clipSourceFile: File? = null
                                    if (clipTrackURI != null) {
                                        clipSourceFile = loadURIIntoWorkspace(job, "Source", clipTrackURI)
                                    }
                                    if (sourceFile == null) {
                                        sourceFile = clipSourceFile // need one source file
                                    }
                                    var index = -1

                                    if (clipSourceFile != null) { // clip has different source
                                        index = inputfile.indexOf(clipSourceFile.absolutePath) // Look for known tracks
                                        if (index < 0) { // if new unknown track
                                            inputfile.add(clipSourceFile.absolutePath) // add track
                                            props["in.video.path$index"] = sourceFile!!.absolutePath
                                            index = inputfile.indexOf(clipSourceFile.absolutePath)
                                        }
                                    } else {
                                        index = srcIndex // default source track
                                    }
                                    logger.debug("Adding edit clip index " + index + " begin " + begin + " end " + end + " to "
                                            + sourceTrackId)
                                    videoclips.add(VideoClip(index, begin, end))
                                }
                            } else {
                                throw EncoderException("Smil container '"
                                        + (elementChild as SmilMediaContainer).containerType.toString() + "'is not supported yet")
                            }
                        }
                    } else {
                        throw EncoderException(
                                "Smil container '" + container.containerType.toString() + "'is not supported yet")
                    }
                }
            }
            val edits = ArrayList<Long>() // collect edit points
            for (clip in videoclips) {
                edits.add(clip.src.toLong())
                edits.add(clip.startMS)
                edits.add(clip.endMS)
            }
            val inputs = ArrayList<File>() // collect input source tracks
            for (f in inputfile) {
                inputs.add(File(f))
            }
            val encoderEngine = encoderEngine
            try {
                outputs = encoderEngine.multiTrimConcat(inputs, edits, profiles, transitionDuration,
                        ComposerService.AUDIO_ONLY != mediaType, ComposerService.VIDEO_ONLY != mediaType)
            } catch (e: EncoderException) {
                val params = HashMap<String, String>()
                val profileList = ArrayList<String>()
                for (p in profiles) {
                    profileList.add(p.identifier.toString())
                }
                params["videos"] = StringUtils.join(inputs, ",")
                params["profiles"] = StringUtils.join(profileList, ",")
                incident().recordFailure(job, PROCESS_SMIL_FAILED, e, params, detailsFor(e, encoderEngine))
                throw e
            } finally {
                activeEncoder.remove(encoderEngine)
            }
            logger.info("ProcessSmil/MultiTrimConcat returns {} media files {}", outputs.size, outputs)
            val workspaceURIs = putToCollection(job, outputs, "processSmil files")
            val tracks = inspect(job, workspaceURIs)
            tracks.forEach { track -> track.identifier = idBuilder.createNew().toString() }
            return tracks
        } catch (e: Exception) { // clean up all the stored files
            throw EncoderException("ProcessSmil operation failed to run ", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun multiEncode(sourceTrack: Track, profileIds: List<String>): Job {
        try {
            // Job Load is based on number of encoding profiles
            val load = calculateJobLoadForMultipleProfiles(profileIds, multiEncodeJobLoadFactor)
            val args = ArrayList<String>()
            args.add(MediaPackageElementParser.getAsXml(sourceTrack))
            args.addAll(profileIds)
            return serviceRegistry!!.createJob(ComposerService.JOB_TYPE, Operation.MultiEncode.toString(), args, load)
        } catch (e: ServiceRegistryException) {
            throw EncoderException("Unable to create a job", e)
        }

    }

    /**
     * A single encoding process that produces multiple outputs from a single track(s) using a list of encoding profiles.
     * Each output can be tagged by the profile name.
     *
     * @param job
     * - encoding job
     * @param track
     * - source track
     * @param profileIds
     * - list of encoding profile Ids
     * @return encoded files
     * @throws EncoderException
     * - if can't encode
     * @throws IllegalArgumentException
     * - if missing arguments
     */
    @Throws(EncoderException::class, IllegalArgumentException::class)
    protected fun multiEncode(job: Job?, track: Track?, profileIds: List<String>?): List<Track> {
        if (job == null)
            throw IllegalArgumentException("The Job parameter must not be null")
        if (track == null)
            throw IllegalArgumentException("Source track cannot be null")
        if (profileIds == null || profileIds.isEmpty())
            throw IllegalArgumentException("Cannot encode without encoding profiles")
        var outputs: List<File>? = null
        try {
            val videoFile = loadTrackIntoWorkspace(job, "source", track, false)
            // Get the encoding profiles
            val profiles = ArrayList<EncodingProfile>()
            for (profileId in profileIds) {
                val profile = getProfile(job, profileId)
                profiles.add(profile)
            }
            logger.info("Encoding source track {} using profiles '{}'", track.identifier, profileIds)
            // Do the work
            val encoderEngine = encoderEngine
            try {
                outputs = encoderEngine.multiTrimConcat(Arrays.asList(videoFile), null, profiles, 0, track.hasVideo(),
                        track.hasAudio())
            } catch (e: EncoderException) {
                val params = HashMap<String, String>()
                params["videos"] = videoFile.name
                params["profiles"] = StringUtils.join(profileIds, ",")
                incident().recordFailure(job, MULTI_ENCODE_FAILED, e, params, detailsFor(e, encoderEngine))
                throw e
            } finally {
                activeEncoder.remove(encoderEngine)
            }
            logger.info("MultiEncode returns {} media files {} ", outputs!!.size, outputs)
            val workspaceURIs = putToCollection(job, outputs, "multiencode files")
            val tracks = inspect(job, workspaceURIs)
            tracks.forEach { eachtrack -> eachtrack.identifier = idBuilder.createNew().toString() }
            return tracks
        } catch (e: Exception) {
            throw EncoderException("MultiEncode operation failed to run ", e)
        }

    }

    @Throws(EncoderException::class)
    private fun calculateJobLoadForMultipleProfiles(profileIds: List<String>, adjustmentFactor: Float): Float {
        // Job load is calculated based on the encoding profiles. They are summed up and multiplied by a factor.
        // The factor represents the adjustment that should be made assuming each profile job load was specified
        // based on it running with 1 input -> 1 output so normally will be a number 0 < n < 1.
        var load = 0.0f
        for (profileId in profileIds) {
            val profile = profileScanner!!.getProfile(profileId)
                    ?: throw EncoderException("Encoding profile not found: $profileId")
            load += profile.jobLoad
        }
        load *= adjustmentFactor
        if (load > maxMultipleProfilesJobLoad) {
            load = maxMultipleProfilesJobLoad
        }
        return load
    }

    companion object {
        /**
         * The indexes the composite job uses to create a Job
         */
        private val BACKGROUND_COLOR_INDEX = 6
        private val COMPOSITE_TRACK_SIZE_INDEX = 5
        private val LOWER_TRACK_INDEX = 1
        private val LOWER_TRACK_LAYOUT_INDEX = 2
        private val PROFILE_ID_INDEX = 0
        private val UPPER_TRACK_INDEX = 3
        private val UPPER_TRACK_LAYOUT_INDEX = 4
        private val WATERMARK_INDEX = 8
        private val WATERMARK_LAYOUT_INDEX = 9
        private val AUDIO_SOURCE_INDEX = 7
        /**
         * Error codes
         */
        private val WORKSPACE_GET_IO_EXCEPTION = 1
        private val WORKSPACE_GET_NOT_FOUND = 2
        private val WORKSPACE_PUT_COLLECTION_IO_EXCEPTION = 3
        private val PROFILE_NOT_FOUND = 4
        private val ENCODING_FAILED = 7
        private val TRIMMING_FAILED = 8
        private val COMPOSITE_FAILED = 9
        private val CONCAT_FAILED = 10
        private val CONCAT_LESS_TRACKS = 11
        private val CONCAT_NO_DIMENSION = 12
        private val IMAGE_TO_VIDEO_FAILED = 13
        private val CONVERT_IMAGE_FAILED = 14
        private val IMAGE_EXTRACTION_FAILED = 15
        private val IMAGE_EXTRACTION_UNKNOWN_DURATION = 16
        private val IMAGE_EXTRACTION_TIME_OUTSIDE_DURATION = 17
        private val IMAGE_EXTRACTION_NO_VIDEO = 18
        private val PROCESS_SMIL_FAILED = 19
        private val MULTI_ENCODE_FAILED = 20
        private val NO_STREAMS = 23

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(ComposerServiceImpl::class.java)

        /** Default location of the ffmepg binary (resembling the installer)  */
        private val FFMPEG_BINARY_DEFAULT = "ffmpeg"

        /** Configuration for the FFmpeg binary  */
        private val CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path"

        /** The collection name  */
        private val COLLECTION = "composer"

        /** Used to mark a track unavailable to composite.  */
        private val NOT_AVAILABLE = "n/a"

        /** The formatter for load values  */
        private val df = DecimalFormat("#.#")

        /** Configuration for process-smil transition duration  */
        val PROCESS_SMIL_CLIP_TRANSITION_DURATION = "org.composer.process_smil.edit.transition.duration"

        /** default transition duration for process_smil in seconds  */
        val DEFAULT_PROCESS_SMIL_CLIP_TRANSITION_DURATION = 2.0f

        /** The maximum job load allowed for operations that use multiple profile (ProcessSmil, MultiEncode)  */
        val DEFAULT_JOB_LOAD_MAX_MULTIPLE_PROFILES = 0.8f
        /** The default factor used to multiply the sum of encoding profiles load job for ProcessSmil  */
        val DEFAULT_PROCESS_SMIL_JOB_LOAD_FACTOR = 0.5f
        val DEFAULT_MULTI_ENCODE_JOB_LOAD_FACTOR = 0.5f

        val JOB_LOAD_MAX_MULTIPLE_PROFILES = "job.load.max.multiple.profiles"
        val JOB_LOAD_FACTOR_PROCESS_SMIL = "job.load.factor.process.smil"

        /**
         * Example composite command below. Use with `-filter_complex` option of ffmpeg if upper video is available otherwise
         * use -filver:v option for a single video.
         *
         * Dual video sample: The ffmpeg command needs two source files set with the `-i` option. The first media file is the
         * `lower`, the second the `upper` one. Example filter: -filter_complex
         * [0:v]scale=909:682,pad=1280:720:367:4:0x444345FF[lower];[1:v]scale=358:151[upper];[lower][upper]overlay=4:4[out]
         *
         * Single video sample: The ffmpeg command needs one source files set with the `-i` option. Example filter: filter:v
         * [in]scale=909:682,pad=1280:720:367:4:0x444345FF[out]
         *
         * @return commandline part with -filter_complex and -map options
         */
        private fun buildCompositeCommand(compositeTrackSize: Dimension, lowerLaidOutElement: LaidOutElement<Track>,
                                          upperLaidOutElement: Option<LaidOutElement<Track>>, upperFile: Option<File>,
                                          watermarkOption: Option<LaidOutElement<Attachment>>, watermarkFile: File?, backgroundColor: String, audioSourceName: String?): String {
            val cmd = StringBuilder()
            val videoId = if (watermarkOption.isNone) "[out]" else "[video]"
            if (upperLaidOutElement.isNone) {
                // There is only one video track and possibly one watermark.
                val videoLayout = lowerLaidOutElement.layout
                val videoPosition = videoLayout.offset.x.toString() + ":" + videoLayout.offset.y
                val scaleVideo = videoLayout.dimension.width.toString() + ":" + videoLayout.dimension.height
                val padLower = (compositeTrackSize.width.toString() + ":" + compositeTrackSize.height + ":"
                        + videoPosition + ":" + backgroundColor)
                cmd.append("-filter:v [in]scale=").append(scaleVideo).append(",pad=").append(padLower).append(videoId)
            } else if (upperFile.isSome && upperLaidOutElement.isSome) {
                // There are two video tracks to handle.
                val lowerLayout = lowerLaidOutElement.layout
                val upperLayout = upperLaidOutElement.get().layout

                val upperPosition = upperLayout.offset.x.toString() + ":" + upperLayout.offset.y
                val lowerPosition = lowerLayout.offset.x.toString() + ":" + lowerLayout.offset.y

                val scaleUpper = upperLayout.dimension.width.toString() + ":" + upperLayout.dimension.height
                val scaleLower = lowerLayout.dimension.width.toString() + ":" + lowerLayout.dimension.height

                val padLower = (compositeTrackSize.width.toString() + ":" + compositeTrackSize.height + ":"
                        + lowerPosition + ":" + backgroundColor)

                // Add input file for the upper track
                cmd.append("-i ").append(upperFile.get().absolutePath).append(" ")
                // Add filter complex mode
                cmd.append("-filter_complex").append(" [0:v]scale=")// lower video
                        .append(scaleLower).append(",pad=").append(padLower).append("[lower]")
                        // upper video
                        .append(";[1:v]scale=").append(scaleUpper).append("[upper]")
                        // mix
                        .append(";[lower][upper]overlay=").append(upperPosition).append(videoId)
            }

            for (watermarkLayout in watermarkOption) {
                val watermarkPosition = (watermarkLayout.layout.offset.x.toString() + ":"
                        + watermarkLayout.layout.offset.y)
                cmd.append(";").append("movie=").append(watermarkFile!!.absoluteFile).append("[watermark];").append(videoId)
                        .append("[watermark]overlay=").append(watermarkPosition).append("[out]")
            }

            if (upperLaidOutElement.isSome) {
                // handle audio
                var lowerAudio = lowerLaidOutElement.element.hasAudio()
                var upperAudio = upperLaidOutElement.get().element.hasAudio()
                // if not specfied or "both", use both videos
                if (audioSourceName != null && !ComposerService.BOTH.equals(audioSourceName, ignoreCase = true)) {
                    lowerAudio = lowerAudio and ComposerService.LOWER.equals(audioSourceName, ignoreCase = true)
                    upperAudio = upperAudio and ComposerService.UPPER.equals(audioSourceName, ignoreCase = true)
                }
                if (lowerAudio && upperAudio) {
                    cmd.append(";[0:a][1:a]amix=inputs=2[aout] -map [out] -map [aout]")
                } else if (lowerAudio) {
                    cmd.append(" -map [out] -map 0:a")
                } else if (upperAudio) {
                    cmd.append(" -map [out] -map 1:a")
                } else {
                    cmd.append(" -map [out]")
                }
            }

            return cmd.toString()
        }

        private fun detailsFor(ex: EncoderException, engine: EncoderEngine): List<Tuple<String, String>> {
            val d = ArrayList<Tuple<String, String>>()
            d.add(tuple("encoder-engine-class", engine.javaClass.name))
            if (ex is CmdlineEncoderException) {
                d.add(tuple("encoder-commandline", ex.commandLine))
            }
            return d
        }
    }
}
