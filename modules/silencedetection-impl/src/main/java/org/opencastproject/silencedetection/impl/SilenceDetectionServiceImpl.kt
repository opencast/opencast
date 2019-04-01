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

package org.opencastproject.silencedetection.impl

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.silencedetection.api.MediaSegment
import org.opencastproject.silencedetection.api.MediaSegments
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.api.SilenceDetectionService
import org.opencastproject.silencedetection.ffmpeg.FFmpegSilenceDetector
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.LoadUtil
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.Dictionary
import java.util.Enumeration
import java.util.LinkedList
import java.util.Properties

/**
 * Implementation of SilenceDetectionService using FFmpeg.
 */
class SilenceDetectionServiceImpl : AbstractJobProducer(SilenceDetectionService.JOB_TYPE), SilenceDetectionService, ManagedService {

    private var jobload = DEFAULT_JOB_LOAD
    /**
     * Reference to the workspace service
     */
    private var workspace: Workspace? = null
    /**
     * Reference to the receipt service
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set
    /**
     * The organization directory service
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set
    /**
     * The security service
     */
    override var securityService: SecurityService? = null
        set
    /**
     * The user directory service
     */
    override var userDirectoryService: UserDirectoryService? = null
        set
    protected var smilService: SmilService? = null
    private var properties: Properties? = null

    private enum class Operation {
        SILENCE_DETECTION
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.silencedetection.api.SilenceDetectionService.detect
     */
    @Throws(SilenceDetectionFailedException::class)
    override fun detect(sourceTrack: Track): Job {
        return detect(sourceTrack, null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.silencedetection.api.SilenceDetectionService.detect
     */
    @Throws(SilenceDetectionFailedException::class)
    override fun detect(sourceTrack: Track?, referenceTracks: Array<Track>?): Job {
        try {
            if (sourceTrack == null) {
                throw SilenceDetectionFailedException("Source track is null!")
            }
            val arguments = LinkedList<String>()
            // put source track as job argument
            arguments.add(0, MediaPackageElementParser.getAsXml(sourceTrack))

            // put reference tracks as second argument
            if (referenceTracks != null) {
                arguments.add(1, MediaPackageElementParser.getArrayAsXml(Arrays.asList(*referenceTracks)))
            }

            return serviceRegistry!!.createJob(
                    jobType!!,
                    Operation.SILENCE_DETECTION.toString(),
                    arguments,
                    jobload)

        } catch (ex: ServiceRegistryException) {
            throw SilenceDetectionFailedException("Unable to create job! " + ex.message)
        } catch (ex: MediaPackageException) {
            throw SilenceDetectionFailedException("Unable to serialize track!")
        }

    }

    @Throws(SilenceDetectionFailedException::class, SmilException::class, MediaPackageException::class)
    override fun process(job: Job): String {
        if (Operation.SILENCE_DETECTION.toString() == job.operation) {
            // get source track
            val sourceTrackXml = StringUtils.trimToNull(job.arguments[0])
                    ?: throw SilenceDetectionFailedException("Track not set!")
            val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml) as Track

            // run detection on source track
            val segments = runDetection(sourceTrack)

            // get reference tracks if any
            var referenceTracks: List<Track>? = null
            if (job.arguments.size > 1) {
                val referenceTracksXml = StringUtils.trimToNull(job.arguments[1])
                if (referenceTracksXml != null) {
                    referenceTracks = MediaPackageElementParser.getArrayFromXml(referenceTracksXml) as List<Track>
                }
            }

            if (referenceTracks == null) {
                referenceTracks = Arrays.asList(sourceTrack)
            }

            // create smil XML as result
            try {
                return generateSmil(segments, referenceTracks).toXML()
            } catch (ex: Exception) {
                throw SmilException("Failed to create smil document.", ex)
            }

        }

        throw SilenceDetectionFailedException("Can't handle this operation: " + job.operation)
    }

    /**
     * Run silence detection on the source track and returns
     * [org.opencastproject.silencedetection.api.MediaSegments]
     * XML as string. Source track should have an audio stream. All detected
     * [org.opencastproject.silencedetection.api.MediaSegment]s
     * (one or more) are non silent sequences.
     *
     * @param track track where to run silence detection
     * @return [MediaSegments] Xml as String
     * @throws SilenceDetectionFailedException if an error occures
     */
    @Throws(SilenceDetectionFailedException::class)
    protected fun runDetection(track: Track): MediaSegments? {
        try {
            val silenceDetector = FFmpegSilenceDetector(properties, track, workspace)
            return silenceDetector.mediaSegments
        } catch (ex: Exception) {
            throw SilenceDetectionFailedException(ex.message)
        }

    }

    /**
     * Create a smil from given parameters.
     *
     * @param segments media segment list with timestamps
     * @param referenceTracks tracks to put as media segment source files
     * @return generated smil
     * @throws SmilException if smil creation failed
     */
    @Throws(SmilException::class)
    protected fun generateSmil(segments: MediaSegments?, referenceTracks: List<Track>): Smil {
        var smilResponse = smilService!!.createNewSmil()
        val referenceTracksArr = referenceTracks.toTypedArray()

        for (segment in segments!!.mediaSegments) {
            smilResponse = smilService!!.addParallel(smilResponse.smil)
            val parId = smilResponse.entity.id

            smilResponse = smilService!!.addClips(smilResponse.smil, parId, referenceTracksArr,
                    segment.segmentStart, segment.segmentStop - segment.segmentStart)
        }
        return smilResponse.smil
    }

    override fun activate(context: ComponentContext) {
        logger.debug("activating...")
        super.activate(context)
        FFmpegSilenceDetector.init(context.bundleContext)
    }

    protected fun deactivate(context: ComponentContext) {
        logger.debug("deactivating...")
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        this.properties = Properties()
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        val keys = properties.keys()
        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            this.properties!![key] = properties.get(key)
        }
        logger.debug("Properties updated!")

        jobload = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_KEY, DEFAULT_JOB_LOAD, serviceRegistry!!)
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    companion object {

        /**
         * The logging instance
         */
        private val logger = LoggerFactory.getLogger(SilenceDetectionServiceImpl::class.java)

        val JOB_LOAD_KEY = "job.load.videoeditor.silencedetection"

        private val DEFAULT_JOB_LOAD = 0.2f
    }
}
