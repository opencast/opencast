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

package org.opencastproject.timelinepreviews.ffmpeg

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.identifier.IdBuilderFactory
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.util.Arrays
import java.util.Dictionary
import java.util.UUID

/**
 * Media analysis plugin that takes a video stream and generates preview images that can be shown on the timeline.
 * This will be done using FFmpeg.
 */
class TimelinePreviewsServiceImpl : AbstractJobProducer(JOB_TYPE), TimelinePreviewsService, ManagedService {

    /** Path to the executable  */
    protected var binary = FFMPEG_BINARY_DEFAULT

    /** The load introduced on the system by creating a caption job  */
    private var timelinepreviewsJobLoad = DEFAULT_TIMELINEPREVIEWS_JOB_LOAD

    /** The horizontal resolution of a single preview image  */
    var resolutionX = DEFAULT_RESOLUTION_X

    /** The vertical resolution of a single preview image  */
    var resolutionY = DEFAULT_RESOLUTION_Y

    /** The file format of the generated preview images file  */
    var outputFormat = DEFAULT_OUTPUT_FORMAT

    /** The mimetype that will be set for the generated Attachment containing the timeline previews image  */
    var mimetype = DEFAULT_MIMETYPE


    /** Reference to the receipt service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Sets the receipt service
     *
     * @param serviceRegistry
     * the service registry
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The workspace to use when retrieving remote media files  */
    protected var workspace: Workspace? = null

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

    /** List of available operations on jobs  */
    protected enum class Operation {
        TimelinePreview
    }

    /**
     * Creates a new instance of the timeline previews service.
     */
    init {
        this.binary = FFMPEG_BINARY_DEFAULT
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Activate ffmpeg timeline previews service")
        val path = cc.bundleContext.getProperty(FFMPEG_BINARY_CONFIG)
        this.binary = path ?: FFMPEG_BINARY_DEFAULT
        logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, FFMPEG_BINARY_DEFAULT)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedService.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            return
        }
        logger.debug("Configuring the timeline previews service")

        // Horizontal resolution
        if (properties.get(OPT_RESOLUTION_X) != null) {
            val res = properties.get(OPT_RESOLUTION_X) as String
            try {
                resolutionX = Integer.parseInt(res)
                logger.info("Horizontal resolution set to {} pixels", resolutionX)
            } catch (e: Exception) {
                throw ConfigurationException(OPT_RESOLUTION_X, "Found illegal value '" + res
                        + "' for timeline previews horizontal resolution")
            }

        }
        // Vertical resolution
        if (properties.get(OPT_RESOLUTION_Y) != null) {
            val res = properties.get(OPT_RESOLUTION_Y) as String
            try {
                resolutionY = Integer.parseInt(res)
                logger.info("Vertical resolution set to {} pixels", resolutionY)
            } catch (e: Exception) {
                throw ConfigurationException(OPT_RESOLUTION_Y, "Found illegal value '" + res
                        + "' for timeline previews vertical resolution")
            }

        }
        // Output file format
        if (properties.get(OPT_OUTPUT_FORMAT) != null) {
            val format = properties.get(OPT_OUTPUT_FORMAT) as String
            try {
                outputFormat = format
                logger.info("Output file format set to \"{}\"", outputFormat)
            } catch (e: Exception) {
                throw ConfigurationException(OPT_OUTPUT_FORMAT, "Found illegal value '" + format
                        + "' for timeline previews output file format")
            }

        }
        // Output mimetype
        if (properties.get(OPT_MIMETYPE) != null) {
            val type = properties.get(OPT_MIMETYPE) as String
            try {
                mimetype = type
                logger.info("Mime type set to \"{}\"", mimetype)
            } catch (e: Exception) {
                throw ConfigurationException(OPT_MIMETYPE, "Found illegal value '" + type
                        + "' for timeline previews mimetype")
            }

        }

        timelinepreviewsJobLoad = LoadUtil.getConfiguredLoadValue(properties, TIMELINEPREVIEWS_JOB_LOAD_KEY,
                DEFAULT_TIMELINEPREVIEWS_JOB_LOAD, serviceRegistry!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.timelinepreviews.api.TimelinePreviewsService.createTimelinePreviewImages
     */
    @Throws(TimelinePreviewsException::class, MediaPackageException::class)
    override fun createTimelinePreviewImages(track: Track, imageCount: Int): Job {
        try {
            val parameters = Arrays.asList(MediaPackageElementParser.getAsXml(track), Integer.toString(imageCount))

            return serviceRegistry!!.createJob(JOB_TYPE,
                    Operation.TimelinePreview.toString(),
                    parameters,
                    timelinepreviewsJobLoad)
        } catch (e: ServiceRegistryException) {
            throw TimelinePreviewsException("Unable to create timelinepreviews job", e)
        }

    }

    /**
     * Starts generation of timeline preview images for the given video track
     * and returns an attachment containing one image that contains all the
     * timeline preview images.
     *
     * @param job
     * @param track the element to analyze
     * @param imageCount number of preview images that will be generated
     * @return an attachment containing the resulting timeline previews image
     * @throws TimelinePreviewsException
     * @throws org.opencastproject.mediapackage.MediaPackageException
     */
    @Throws(TimelinePreviewsException::class, MediaPackageException::class)
    protected fun generatePreviewImages(job: Job, track: Track, imageCount: Int): Attachment {

        // Make sure the element can be analyzed using this analysis implementation
        if (!track.hasVideo()) {
            logger.error("Element {} is not a video track", track.identifier)
            throw TimelinePreviewsException("Element is not a video track")
        }

        try {

            if (track.duration == null)
                throw MediaPackageException("Track $track does not have a duration")

            val duration = track.duration!! / 1000.0
            var seconds = duration / imageCount.toDouble()
            seconds = if (seconds <= 0.0) 1.0 else seconds

            // calculate number of tiles for row and column in tiled image
            val imageSize = Math.ceil(Math.sqrt(imageCount.toDouble())).toInt()

            val composedImage = createPreviewsFFmpeg(track, seconds, resolutionX, resolutionY, imageSize, imageSize,
                    duration) ?: throw IllegalStateException("Unable to compose image")


// Set the mimetype
            try {
                composedImage.mimeType = MimeTypes.parseMimeType(mimetype)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid mimetype provided for timeline previews image")
                try {
                    composedImage.mimeType = MimeTypes.fromURI(composedImage.getURI())
                } catch (ex: UnknownFileTypeException) {
                    logger.warn("No valid mimetype could be found for timeline previews image")
                }

            }

            composedImage.properties.put("imageCount", imageCount.toString())

            return composedImage

        } catch (e: Exception) {
            logger.warn("Error creating timeline preview images for $track", e)
            if (e is TimelinePreviewsException) {
                throw e
            } else {
                throw TimelinePreviewsException(e)
            }
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    public override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            when (op) {
                TimelinePreviewsServiceImpl.Operation.TimelinePreview -> {
                    val track = MediaPackageElementParser
                            .getFromXml(arguments[0]) as Track
                    val imageCount = Integer.parseInt(arguments[1])
                    val timelinePreviewsMpe = generatePreviewImages(job, track, imageCount)
                    return MediaPackageElementParser.getAsXml(timelinePreviewsMpe)
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * Executes the FFmpeg command to generate a timeline previews image
     *
     * @param track the track to generate the timeline previews image for
     * @param seconds the length of a segment that one preview image should represent
     * @param width the width of a single preview image
     * @param height the height of a single preview image
     * @param tileX the horizontal number of preview images that are stored in the timeline previews image
     * @param tileY the vertical number of preview images that are stored in the timeline previews image
     * @param duration the duration for which preview images should be generated
     * @return an attachment containing the timeline previews image
     * @throws TimelinePreviewsException
     */
    @Throws(TimelinePreviewsException::class)
    protected fun createPreviewsFFmpeg(track: Track, seconds: Double, width: Int, height: Int, tileX: Int, tileY: Int,
                                       duration: Double): Attachment {

        // copy source file into workspace
        val mediaFile: File
        try {
            mediaFile = workspace!!.get(track.getURI())
        } catch (e: NotFoundException) {
            throw TimelinePreviewsException(
                    "Error finding the media file in the workspace", e)
        } catch (e: IOException) {
            throw TimelinePreviewsException(
                    "Error reading the media file in the workspace", e)
        }

        val imageFilePath = (FilenameUtils.removeExtension(mediaFile.absolutePath) + '_'.toString() + UUID.randomUUID()
                + "_timelinepreviews" + outputFormat)
        var exitCode = 1
        val command = arrayOf(binary, "-loglevel", "error", "-t", (duration - seconds / 2.0).toString(), "-i", mediaFile.absolutePath, "-vf", "fps=1/" + seconds + ",scale=" + width + ":" + height + ",tile=" + tileX + "x" + tileY, imageFilePath)

        logger.debug("Start timeline previews ffmpeg process: {}", StringUtils.join(command, " "))
        logger.info("Create timeline preview images file for track '{}' at {}", track.identifier, imageFilePath)

        val pbuilder = ProcessBuilder(*command)

        pbuilder.redirectErrorStream(true)
        var ffmpegProcess: Process? = null
        exitCode = 1
        var errStream: BufferedReader? = null
        try {
            ffmpegProcess = pbuilder.start()

            errStream = BufferedReader(InputStreamReader(ffmpegProcess!!.inputStream))
            var line: String? = errStream.readLine()
            while (line != null) {
                logger.error("FFmpeg error: $line")
                line = errStream.readLine()
            }
            exitCode = ffmpegProcess.waitFor()
        } catch (ex: IOException) {
            throw TimelinePreviewsException("Starting ffmpeg process failed", ex)
        } catch (ex: InterruptedException) {
            throw TimelinePreviewsException("Timeline preview creation was unexpectedly interrupted", ex)
        } finally {
            IoSupport.closeQuietly(ffmpegProcess)
            IoSupport.closeQuietly(errStream)
            if (exitCode != 0) {
                try {
                    FileUtils.forceDelete(File(imageFilePath))
                } catch (e: IOException) {
                    // it is ok, no output file was generated by ffmpeg
                }

            }
        }

        if (exitCode != 0)
            throw TimelinePreviewsException("Generating timeline preview for track " + track.identifier
                    + " failed: ffmpeg process exited abnormally with exit code " + exitCode)

        // put timeline previews image into workspace
        var timelinepreviewsFileInputStream: FileInputStream? = null
        var previewsFileUri: URI? = null
        try {
            timelinepreviewsFileInputStream = FileInputStream(imageFilePath)
            previewsFileUri = workspace!!.putInCollection(COLLECTION_ID,
                    FilenameUtils.getName(imageFilePath), timelinepreviewsFileInputStream)
            logger.info("Copied the created timeline preview images file to the workspace {}", previewsFileUri!!.toString())
        } catch (ex: FileNotFoundException) {
            throw TimelinePreviewsException(
                    String.format("Timeline previews image file '%s' not found", imageFilePath), ex)
        } catch (ex: IOException) {
            throw TimelinePreviewsException(
                    String.format("Can't write timeline preview images file '%s' to workspace", imageFilePath), ex)
        } catch (ex: IllegalArgumentException) {
            throw TimelinePreviewsException(ex)
        } finally {
            IoSupport.closeQuietly(timelinepreviewsFileInputStream)
            logger.info("Deleted local timeline preview images file at {}", imageFilePath)
            FileUtils.deleteQuietly(File(imageFilePath))
        }

        // create media package element
        val mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        // it is up to the workflow operation handler to set the attachment flavor
        val timelinepreviewsMpe = mpElementBuilder.elementFromURI(
                previewsFileUri!!, MediaPackageElement.Type.Attachment, track.flavor) as Attachment

        // add reference to track
        timelinepreviewsMpe.referTo(track)

        // add additional properties to attachment
        timelinepreviewsMpe.properties.put("imageSizeX", tileX.toString())
        timelinepreviewsMpe.properties.put("imageSizeY", tileY.toString())
        timelinepreviewsMpe.properties.put("resolutionX", resolutionX.toString())
        timelinepreviewsMpe.properties.put("resolutionY", resolutionY.toString())

        // set the flavor and an ID
        timelinepreviewsMpe.flavor = track.flavor
        timelinepreviewsMpe.identifier = IdBuilderFactory.newInstance().newIdBuilder().createNew().compact()

        return timelinepreviewsMpe
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

    companion object {

        /** Resulting collection in the working file repository  */
        val COLLECTION_ID = "timelinepreviews"

        /** The key to look for in the service configuration file to override the DEFAULT_FFMPEG_BINARY  */
        val FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path"

        /** The default path to the FFmpeg binary  */
        val FFMPEG_BINARY_DEFAULT = "ffmpeg"

        /** Name of the constant used to retrieve the horizontal resolution  */
        val OPT_RESOLUTION_X = "resolutionX"

        /** Default value for the horizontal resolution  */
        val DEFAULT_RESOLUTION_X = 160

        /** Name of the constant used to retrieve the vertical resolution  */
        val OPT_RESOLUTION_Y = "resolutionY"

        /** Default value for the vertical resolution  */
        val DEFAULT_RESOLUTION_Y = -1

        /** Name of the constant used to retrieve the output file format  */
        val OPT_OUTPUT_FORMAT = "outputFormat"

        /** Default value for the format of the output image file  */
        val DEFAULT_OUTPUT_FORMAT = ".png"

        /** Name of the constant used to retrieve the mimetype  */
        val OPT_MIMETYPE = "mimetype"

        /** Default value for the mimetype of the generated image  */
        val DEFAULT_MIMETYPE = "image/png"


        /** The default job load of a timeline previews job  */
        val DEFAULT_TIMELINEPREVIEWS_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the DEFAULT_TIMELINEPREVIEWS_JOB_LOAD  */
        val TIMELINEPREVIEWS_JOB_LOAD_KEY = "job.load.timelinepreviews"

        /** The logging facility  */
        protected val logger = LoggerFactory
                .getLogger(TimelinePreviewsServiceImpl::class.java)
    }

}
