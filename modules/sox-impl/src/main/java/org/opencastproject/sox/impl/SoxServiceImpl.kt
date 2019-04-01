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

package org.opencastproject.sox.impl

import org.opencastproject.util.data.Option.some

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.identifier.IdBuilder
import org.opencastproject.mediapackage.identifier.IdBuilderFactory
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.sox.api.SoxException
import org.opencastproject.sox.api.SoxService
import org.opencastproject.util.FileSupport
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.UUID

/** Creates a new composer service instance.  */
class SoxServiceImpl : AbstractJobProducer(JOB_TYPE), SoxService, ManagedService {

    /** The load introduced on the system by creating a analyze job  */
    private var analyzeJobLoad = DEFAULT_ANALYZE_JOB_LOAD

    /** The load introduced on the system by creating a normalize job  */
    private var normalizeJobLoad = DEFAULT_NORMALIZE_JOB_LOAD

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

    private var binary = SOX_BINARY_DEFAULT

    /** List of available operations on jobs  */
    private enum class Operation {
        Analyze, Normalize
    }

    /**
     * OSGi callback on component activation.
     *
     * @param cc
     * the component context
     */
    override fun activate(cc: ComponentContext) {
        logger.info("Activating sox service")
        super.activate(cc)
        // Configure sox
        val path = cc.bundleContext.getProperty(CONFIG_SOX_PATH) as String
        if (path == null) {
            logger.debug("DEFAULT $CONFIG_SOX_PATH: $SOX_BINARY_DEFAULT")
        } else {
            binary = path
            logger.debug("SoX config binary: {}", path)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.sox.api.SoxService.analyze
     */
    @Throws(MediaPackageException::class, SoxException::class)
    override fun analyze(sourceAudioTrack: Track): Job {
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Analyze.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(sourceAudioTrack)), analyzeJobLoad)
        } catch (e: ServiceRegistryException) {
            throw SoxException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.sox.api.SoxService.normalize
     */
    @Throws(MediaPackageException::class, SoxException::class)
    override fun normalize(sourceAudioTrack: Track, targetRmsLevDb: Float?): Job {
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Normalize.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(sourceAudioTrack), targetRmsLevDb!!.toString()),
                    normalizeJobLoad)
        } catch (e: ServiceRegistryException) {
            throw SoxException("Unable to create a job", e)
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
            var audioTrack: TrackImpl? = null

            val serialized: String
            when (op) {
                SoxServiceImpl.Operation.Analyze -> {
                    audioTrack = MediaPackageElementParser.getFromXml(arguments[0]) as TrackImpl
                    serialized = analyze(job, audioTrack).map(MediaPackageElementParser.getAsXml()).getOrElse("")
                }
                SoxServiceImpl.Operation.Normalize -> {
                    audioTrack = MediaPackageElementParser.getFromXml(arguments[0]) as TrackImpl
                    val targetRmsLevDb = Float(arguments[1])
                    serialized = normalize(job, audioTrack, targetRmsLevDb).map(MediaPackageElementParser.getAsXml())
                            .getOrElse("")
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }

            return serialized
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    @Throws(SoxException::class)
    protected fun analyze(job: Job, audioTrack: Track): Option<Track> {
        if (!audioTrack.hasAudio())
            throw SoxException("No audio stream available")
        if (audioTrack.hasVideo())
            throw SoxException("It must not have a video stream")

        try {
            // Get the tracks and make sure they exist
            val audioFile: File
            try {
                audioFile = workspace!!.get(audioTrack.getURI())
            } catch (e: NotFoundException) {
                throw SoxException("Requested audio track $audioTrack is not found")
            } catch (e: IOException) {
                throw SoxException("Unable to access audio track $audioTrack")
            }

            logger.info("Analyzing audio track {}", audioTrack.identifier)

            // Do the work
            val command = ArrayList<String>()
            command.add(binary)
            command.add(audioFile.absolutePath)
            command.add("-n")
            command.add("remix")
            command.add("-")
            command.add("stats")
            val analyzeResult = launchSoxProcess(command)

            // Add audio metadata and return audio track
            return some(addAudioMetadata(audioTrack, analyzeResult))
        } catch (e: Exception) {
            logger.warn("Error analyzing {}: {}", audioTrack, e.message)
            if (e is SoxException) {
                throw e
            } else {
                throw SoxException(e)
            }
        }

    }

    private fun addAudioMetadata(audioTrack: Track, metadata: List<String>): Track {
        val track = audioTrack as TrackImpl
        val audio = track.getAudio()

        if (audio!!.size == 0) {
            audio.add(AudioStreamImpl())
            logger.info("No audio streams found created new audio stream")
        }

        val audioStream = audio[0] as AudioStreamImpl
        if (audio.size > 1)
            logger.info("Multiple audio streams found, take first audio stream {}", audioStream)

        for (value in metadata) {
            if (value.startsWith("Pk lev dB")) {
                val pkLevDb = Float(StringUtils.substringAfter(value, "Pk lev dB").trim { it <= ' ' })
                audioStream.pkLevDb = pkLevDb
            } else if (value.startsWith("RMS lev dB")) {
                val rmsLevDb = Float(StringUtils.substringAfter(value, "RMS lev dB").trim { it <= ' ' })
                audioStream.rmsLevDb = rmsLevDb
            } else if (value.startsWith("RMS Pk dB")) {
                val rmsPkDb = Float(StringUtils.substringAfter(value, "RMS Pk dB").trim { it <= ' ' })
                audioStream.rmsPkDb = rmsPkDb
            }
        }
        return track
    }

    @Throws(SoxException::class)
    private fun launchSoxProcess(command: List<String>): List<String> {
        var process: Process? = null
        var `in`: BufferedReader? = null
        try {
            logger.info("Start sox process {}", command)
            val pb = ProcessBuilder(command)
            pb.redirectErrorStream(true) // Unfortunately merges but necessary for deadlock prevention
            process = pb.start()
            `in` = BufferedReader(InputStreamReader(process!!.inputStream))
            process.waitFor()
            var line: String? = null
            val stats = ArrayList<String>()
            while ((line = `in`.readLine()) != null) {
                logger.info(line)
                stats.add(line)
            }
            if (process.exitValue() != 0)
                throw SoxException("Sox process failed with error code: " + process.exitValue())
            logger.info("Sox process finished")
            return stats
        } catch (e: IOException) {
            throw SoxException("Could not start sox process: " + command + "\n" + e.message)
        } catch (e: InterruptedException) {
            throw SoxException("Could not start sox process: " + command + "\n" + e.message)
        } finally {
            IoSupport.closeQuietly(`in`)
        }
    }

    @Throws(SoxException::class)
    private fun normalize(job: Job, audioTrack: TrackImpl, targetRmsLevDb: Float?): Option<Track> {
        if (!audioTrack.hasAudio())
            throw SoxException("No audio stream available")
        if (audioTrack.hasVideo())
            throw SoxException("It must not have a video stream")
        if (audioTrack.getAudio()!!.size < 1)
            throw SoxException("No audio stream metadata available")
        if (audioTrack.getAudio()!![0].rmsLevDb == null)
            throw SoxException("No RMS Lev dB metadata available")

        val targetTrackId = idBuilder.createNew().toString()

        val rmsLevDb = audioTrack.getAudio()!![0].rmsLevDb

        // Get the tracks and make sure they exist
        val audioFile: File
        try {
            audioFile = workspace!!.get(audioTrack.getURI())
        } catch (e: NotFoundException) {
            throw SoxException("Requested audio track $audioTrack is not found")
        } catch (e: IOException) {
            throw SoxException("Unable to access audio track $audioTrack")
        }

        val outDir = audioFile.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(audioFile.name) + "_" + UUID.randomUUID().toString()
        val suffix = "-norm." + FilenameUtils.getExtension(audioFile.name)

        val normalizedFile = File(outDir, outFileName + suffix)

        logger.info("Normalizing audio track {} to {}", audioTrack.identifier, targetTrackId)

        // Do the work
        val command = ArrayList<String>()
        command.add(binary)
        command.add(audioFile.absolutePath)
        command.add(normalizedFile.absolutePath)
        command.add("remix")
        command.add("-")
        command.add("gain")
        if (targetRmsLevDb > rmsLevDb)
            command.add("-l")
        command.add((targetRmsLevDb!! - rmsLevDb!!).toString())
        command.add("stats")

        val normalizeResult = launchSoxProcess(command)

        if (normalizedFile.length() == 0L)
            throw SoxException("Normalization failed: Output file is empty!")

        // Put the file in the workspace
        var returnURL: URI? = null
        var `in`: InputStream? = null
        try {
            `in` = FileInputStream(normalizedFile)
            returnURL = workspace!!.putInCollection(COLLECTION,
                    job.id.toString() + "." + FilenameUtils.getExtension(normalizedFile.absolutePath), `in`)
            logger.info("Copied the normalized file to the workspace at {}", returnURL)
            if (normalizedFile.delete()) {
                logger.info("Deleted the local copy of the normalized file at {}", normalizedFile.absolutePath)
            } else {
                logger.warn("Unable to delete the normalized output at {}", normalizedFile)
            }
        } catch (e: Exception) {
            throw SoxException("Unable to put the normalized file into the workspace", e)
        } finally {
            IOUtils.closeQuietly(`in`)
            FileSupport.deleteQuietly(normalizedFile)
        }

        var normalizedTrack = audioTrack.clone() as Track
        normalizedTrack.setURI(returnURL)
        normalizedTrack.identifier = targetTrackId
        // Add audio metadata and return audio track
        normalizedTrack = addAudioMetadata(normalizedTrack, normalizeResult)

        return some(normalizedTrack)
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

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        analyzeJobLoad = LoadUtil.getConfiguredLoadValue(properties, ANALYZE_JOB_LOAD_KEY, DEFAULT_ANALYZE_JOB_LOAD,
                serviceRegistry!!)
        normalizeJobLoad = LoadUtil.getConfiguredLoadValue(properties, NORMALIZE_JOB_LOAD_KEY, DEFAULT_NORMALIZE_JOB_LOAD,
                serviceRegistry!!)
    }

    companion object {

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(SoxServiceImpl::class.java)

        /** Default location of the SoX binary (resembling the installer)  */
        val SOX_BINARY_DEFAULT = "sox"

        val CONFIG_SOX_PATH = "org.opencastproject.sox.path"

        /** The load introduced on the system by creating a analyze job  */
        val DEFAULT_ANALYZE_JOB_LOAD = 0.2f

        /** The key to look for in the service configuration file to override the [DEFAULT_ANALYZE_JOB_LOAD]  */
        val ANALYZE_JOB_LOAD_KEY = "job.load.analyze"

        /** The load introduced on the system by creating a normalize job  */
        val DEFAULT_NORMALIZE_JOB_LOAD = 0.2f

        /** The key to look for in the service configuration file to override the [DEFAULT_NORMALIZE_JOB_LOAD]  */
        val NORMALIZE_JOB_LOAD_KEY = "job.load.normalize"

        /** The collection name  */
        val COLLECTION = "sox"
    }

}
