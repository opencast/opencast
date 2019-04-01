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
package org.opencastproject.waveform.ffmpeg

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement.Type
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
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.waveform.api.WaveformService
import org.opencastproject.waveform.api.WaveformServiceException
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
import java.util.concurrent.TimeUnit


/**
 * This service creates a waveform image from a media file with at least one audio channel.
 * This will be done using ffmpeg.
 */
class WaveformServiceImpl : AbstractJobProducer(JOB_TYPE), WaveformService, ManagedService {

    /** Path to the executable  */
    protected var binary = DEFAULT_FFMPEG_BINARY

    /** The waveform job load  */
    private var waveformJobLoad = DEFAULT_WAVEFORM_JOB_LOAD

    /** The waveform image scale algorithm  */
    private var waveformScale = DEFAULT_WAVEFORM_SCALE

    /** The value if the waveforms (per audio channel) should be rendered next to each other (if true)
     * or on top of each other (if false)  */
    private var waveformSplitChannels = DEFAULT_WAVEFORM_SPLIT_CHANNELS

    /** The waveform colors per audio channel  */
    private var waveformColor = DEFAULT_WAVEFORM_COLOR

    /** Filter to be prepended to the showwavespic filter  */
    private var waveformFilterPre = DEFAULT_WAVEFORM_FILTER_PRE

    /** Filter to be appended to the showwavespic filter  */
    private var waveformFilterPost = DEFAULT_WAVEFORM_FILTER_POST

    /** Reference to the service registry  */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The workspace to use when retrieving remote media files  */
    private var workspace: Workspace? = null

    /** The security service  */
    override var securityService: SecurityService? = null
        set

    /** The user directory service  */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** The organization directory service  */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** List of available operations on jobs  */
    internal enum class Operation {
        Waveform
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Activate ffmpeg waveform service")
        val path = cc.bundleContext.getProperty(FFMPEG_BINARY_CONFIG_KEY)
        binary = path ?: DEFAULT_FFMPEG_BINARY
        logger.debug("ffmpeg binary set to {}", binary)
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            return
        }
        logger.debug("Configuring the waveform service")
        waveformJobLoad = LoadUtil.getConfiguredLoadValue(properties,
                WAVEFORM_JOB_LOAD_CONFIG_KEY, DEFAULT_WAVEFORM_JOB_LOAD, serviceRegistry!!)

        var `val`: Any? = properties.get(WAVEFORM_SCALE_CONFIG_KEY)
        if (`val` != null) {
            if (StringUtils.isNotEmpty(`val` as String?)) {
                if ("lin" != `val` && "log" != `val`) {
                    logger.warn("Waveform scale configuration value '{}' is not in set of predefined values (lin, log). " + "The waveform image extraction job may fail.", `val`)
                }
                waveformScale = `val`
            }
        }

        `val` = properties.get(WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY)
        if (`val` != null) {
            waveformSplitChannels = java.lang.Boolean.parseBoolean(`val` as String?)
        }

        `val` = properties.get(WAVEFORM_COLOR_CONFIG_KEY)
        if (`val` != null && StringUtils.isNotEmpty(`val` as String?)) {
            val colorValue = `val` as String?
            if (StringUtils.isNotEmpty(colorValue) && StringUtils.isNotBlank(colorValue)) {
                waveformColor = StringUtils.split(colorValue, ", |:;")
            }
        }

        `val` = properties.get(WAVEFORM_FILTER_PRE_CONFIG_KEY)
        if (`val` != null) {
            waveformFilterPre = StringUtils.trimToNull(`val` as String?)
        } else {
            waveformFilterPre = null
        }

        `val` = properties.get(WAVEFORM_FILTER_POST_CONFIG_KEY)
        if (`val` != null) {
            waveformFilterPost = StringUtils.trimToNull(`val` as String?)
        } else {
            waveformFilterPost = null
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.waveform.api.WaveformService.createWaveformImage
     */
    @Throws(MediaPackageException::class, WaveformServiceException::class)
    override fun createWaveformImage(sourceTrack: Track, pixelsPerMinute: Int, minWidth: Int, maxWidth: Int, height: Int, color: String): Job {
        try {
            return serviceRegistry!!.createJob(jobType!!, Operation.Waveform.toString(),
                    Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), Integer.toString(pixelsPerMinute),
                            Integer.toString(minWidth), Integer.toString(maxWidth), Integer.toString(height), color), waveformJobLoad)
        } catch (ex: ServiceRegistryException) {
            throw WaveformServiceException("Unable to create waveform job", ex)
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
                WaveformServiceImpl.Operation.Waveform -> {
                    val track = MediaPackageElementParser.getFromXml(arguments[0]) as Track
                    val pixelsPerMinute = Integer.parseInt(arguments[1])
                    val minWidth = Integer.parseInt(arguments[2])
                    val maxWidth = Integer.parseInt(arguments[3])
                    val height = Integer.parseInt(arguments[4])
                    val color = arguments[5]
                    val waveformMpe = extractWaveform(track, pixelsPerMinute, minWidth, maxWidth, height, color)
                    return MediaPackageElementParser.getAsXml(waveformMpe)
                }
                else -> throw ServiceRegistryException("This service can't handle operations of type '$op'")
            }
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: MediaPackageException) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        } catch (e: WaveformServiceException) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * Create and run waveform extraction ffmpeg command.
     *
     * @param track source audio/video track with at least one audio channel
     * @param pixelsPerMinute width of waveform image in pixels per minute
     * @param minWidth minimum width of waveform image
     * @param maxWidth maximum width of waveform image
     * @param height height of waveform image
     * @param color color of waveform image
     * @return waveform image attachment
     * @throws WaveformServiceException if processing fails
     */
    @Throws(WaveformServiceException::class)
    private fun extractWaveform(track: Track, pixelsPerMinute: Int, minWidth: Int, maxWidth: Int, height: Int, color: String): Attachment {
        if (!track.hasAudio()) {
            throw WaveformServiceException("Track has no audio")
        }

        // copy source file into workspace
        val mediaFile: File
        try {
            mediaFile = workspace!!.get(track.getURI())
        } catch (e: NotFoundException) {
            throw WaveformServiceException(
                    "Error finding the media file in the workspace", e)
        } catch (e: IOException) {
            throw WaveformServiceException(
                    "Error reading the media file in the workspace", e)
        }

        val waveformFilePath = FilenameUtils.removeExtension(mediaFile.absolutePath) + ('-' + track.identifier) + "-waveform.png"

        val width = getWaveformImageWidth(track, pixelsPerMinute, minWidth, maxWidth)

        // create ffmpeg command
        val command = arrayOf(binary, "-nostats", "-nostdin", "-hide_banner", "-i", mediaFile.absolutePath, "-lavfi", createWaveformFilter(track, width, height, color), "-frames:v", "1", "-an", "-vn", "-sn", waveformFilePath)
        logger.debug("Start waveform ffmpeg process: {}", StringUtils.join(command, " "))
        logger.info("Create waveform image file for track '{}' at {}", track.identifier, waveformFilePath)

        // run ffmpeg
        val pb = ProcessBuilder(*command)
        pb.redirectErrorStream(true)
        var ffmpegProcess: Process? = null
        var exitCode = 1
        var errStream: BufferedReader? = null
        try {
            ffmpegProcess = pb.start()

            errStream = BufferedReader(InputStreamReader(ffmpegProcess!!.inputStream))
            var line: String? = errStream.readLine()
            while (line != null) {
                logger.debug(line)
                line = errStream.readLine()
            }

            exitCode = ffmpegProcess.waitFor()
        } catch (ex: IOException) {
            throw WaveformServiceException("Start ffmpeg process failed", ex)
        } catch (ex: InterruptedException) {
            throw WaveformServiceException("Waiting for encoder process exited was interrupted unexpectedly", ex)
        } finally {
            IoSupport.closeQuietly(ffmpegProcess)
            IoSupport.closeQuietly(errStream)
            if (exitCode != 0) {
                try {
                    FileUtils.forceDelete(File(waveformFilePath))
                } catch (e: IOException) {
                    // it is ok, no output file was generated by ffmpeg
                }

            }
        }

        if (exitCode != 0)
            throw WaveformServiceException(String.format("The encoder process exited abnormally with exit code %s " + "using command\n%s", exitCode, command.joinToString(" ")))

        // put waveform image into workspace
        var waveformFileInputStream: FileInputStream? = null
        val waveformFileUri: URI
        try {
            waveformFileInputStream = FileInputStream(waveformFilePath)
            waveformFileUri = workspace!!.putInCollection(COLLECTION_ID,
                    FilenameUtils.getName(waveformFilePath), waveformFileInputStream)
            logger.info("Copied the created waveform to the workspace {}", waveformFileUri)
        } catch (ex: FileNotFoundException) {
            throw WaveformServiceException(String.format("Waveform image file '%s' not found", waveformFilePath), ex)
        } catch (ex: IOException) {
            throw WaveformServiceException(String.format(
                    "Can't write waveform image file '%s' to workspace", waveformFilePath), ex)
        } catch (ex: IllegalArgumentException) {
            throw WaveformServiceException(ex)
        } finally {
            IoSupport.closeQuietly(waveformFileInputStream)
            logger.info("Deleted local waveform image file at {}", waveformFilePath)
            FileUtils.deleteQuietly(File(waveformFilePath))
        }

        // create media package element
        val mpElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        // it is up to the workflow operation handler to set the attachment flavor
        val waveformMpe = mpElementBuilder!!.elementFromURI(
                waveformFileUri, Type.Attachment, track.flavor) as Attachment
        waveformMpe.identifier = IdBuilderFactory.newInstance().newIdBuilder()!!.createNew().compact()
        return waveformMpe
    }

    /**
     * Create an ffmpeg waveform filter with parameters based on input track and service configuration.
     *
     * @param track source audio/video track with at least one audio channel
     * @param width width of waveform image
     * @param height height of waveform image
     * @param color color of waveform image
     * @return ffmpeg filter parameter
     */
    private fun createWaveformFilter(track: Track, width: Int, height: Int, color: String?): String {
        val filterBuilder = StringBuilder("")
        if (waveformFilterPre != null) {
            filterBuilder.append(waveformFilterPre)
            filterBuilder.append(",")
        }
        if (color != null) {
            waveformColor = StringUtils.split(color, "|")
        }
        filterBuilder.append("showwavespic=")
        filterBuilder.append("split_channels=")
        filterBuilder.append(if (waveformSplitChannels) 1 else 0)
        filterBuilder.append(":s=")
        filterBuilder.append(width)
        filterBuilder.append("x")
        filterBuilder.append(height)
        filterBuilder.append(":scale=")
        filterBuilder.append(waveformScale)
        filterBuilder.append(":colors=")
        filterBuilder.append(StringUtils.join(Arrays.asList(*waveformColor), "|"))
        if (waveformFilterPost != null) {
            filterBuilder.append(",")
            filterBuilder.append(waveformFilterPost)
        }
        return filterBuilder.toString()
    }

    /**
     * Return the waveform image width build from input track and service configuration.
     *
     * @param track source audio/video track with at least one audio channel
     * @param pixelsPerMinute width of waveform image in pixels per minute
     * @param minWidth minimum width of waveform image
     * @param maxWidth maximum width of waveform image
     * @return waveform image width
     */
    private fun getWaveformImageWidth(track: Track, pixelsPerMinute: Int, minWidth: Int, maxWidth: Int): Int {
        var imageWidth = minWidth
        if (track.duration > 0) {
            val trackDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(track.duration!!).toInt()
            if (pixelsPerMinute > 0 && trackDurationMinutes > 0) {
                imageWidth = Math.max(minWidth, trackDurationMinutes * pixelsPerMinute)
                imageWidth = Math.min(maxWidth, imageWidth)
            }
        }
        return imageWidth
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** The logging facility  */
        protected val logger = LoggerFactory.getLogger(WaveformServiceImpl::class.java)

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_JOB_LOAD  */
        val WAVEFORM_JOB_LOAD_CONFIG_KEY = "job.load.waveform"

        /** The default job load of a waveform job  */
        val DEFAULT_WAVEFORM_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the DEFAULT_FFMPEG_BINARY  */
        val FFMPEG_BINARY_CONFIG_KEY = "org.opencastproject.composer.ffmpeg.path"

        /** The default path to the ffmpeg binary  */
        val DEFAULT_FFMPEG_BINARY = "ffmpeg"

        /** The default waveform image scale algorithm  */
        val DEFAULT_WAVEFORM_SCALE = "lin"

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_SCALE  */
        val WAVEFORM_SCALE_CONFIG_KEY = "waveform.scale"

        /** The default value if the waveforms (per audio channel) should be renderen next to each other (if true)
         * or on top of each other (if false)  */
        val DEFAULT_WAVEFORM_SPLIT_CHANNELS = false

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_SPLIT_CHANNELS  */
        val WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY = "waveform.split.channels"

        /** The default waveform colors per audio channel  */
        val DEFAULT_WAVEFORM_COLOR = arrayOf("black")

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_COLOR  */
        val WAVEFORM_COLOR_CONFIG_KEY = "waveform.color"

        /** The default filter to be optionally prepended to the showwavespic filter  */
        val DEFAULT_WAVEFORM_FILTER_PRE: String? = null

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_FILTER_PRE  */
        val WAVEFORM_FILTER_PRE_CONFIG_KEY = "waveform.filter.pre"

        /** The default filter to be optionally appended to the showwavespic filter  */
        val DEFAULT_WAVEFORM_FILTER_POST: String? = null

        /** The key to look for in the service configuration file to override the DEFAULT_WAVEFORM_FILTER_POST  */
        val WAVEFORM_FILTER_POST_CONFIG_KEY = "waveform.filter.post"

        /** Resulting collection in the working file repository  */
        val COLLECTION_ID = "waveform"
    }
}
