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

package org.opencastproject.videoeditor.impl

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
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
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.videoeditor.api.ProcessFailedException
import org.opencastproject.videoeditor.api.VideoEditorService
import org.opencastproject.videoeditor.ffmpeg.FFmpegEdit
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.Enumeration
import java.util.LinkedList
import java.util.Properties

import javax.xml.bind.JAXBException

/**
 * Implementation of VideoeditorService using FFMPEG
 */
open class VideoEditorServiceImpl : AbstractJobProducer(JOB_TYPE), VideoEditorService, ManagedService {

    private var jobload = DEFAULT_JOB_LOAD

    /**
     * Reference to the media inspection service
     */
    private var inspectionService: MediaInspectionService? = null
    /**
     * Reference to the workspace service
     */
    private var workspace: Workspace? = null
    /**
     * Id builder used to create ids for encoded tracks
     */
    private val idBuilder = IdBuilderFactory.newInstance().newIdBuilder()
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
    /**
     * The smil service.
     */
    protected var smilService: SmilService? = null
    /**
     * Bundle properties
     */
    private var properties = Properties()

    private enum class Operation {
        PROCESS_SMIL
    }

    /**
     * Splice segments given by smil document for the given track to the new one.
     *
     * @param job
     * processing job
     * @param smil
     * smil document with media segments description
     * @param trackParamGroupId
     * @return processed track
     * @throws ProcessFailedException
     * if an error occured
     */
    @Throws(ProcessFailedException::class)
    protected fun processSmil(job: Job, smil: Smil, trackParamGroupId: String): Track {

        val trackParamGroup: SmilMediaParamGroup
        val inputfile = ArrayList<String>()
        val videoclips = ArrayList<VideoClip>()
        try {
            trackParamGroup = smil[trackParamGroupId] as SmilMediaParamGroup
        } catch (ex: SmilException) {
            // can't be thrown, because we found the Id in processSmil(Smil)
            throw ProcessFailedException("Smil does not contain a paramGroup element with Id $trackParamGroupId")
        }

        var sourceTrackFlavor: MediaPackageElementFlavor? = null
        var sourceTrackUri: String? = null
        // get source track metadata
        for (param in trackParamGroup.params) {
            if (SmilMediaParam.PARAM_NAME_TRACK_SRC.equals(param.name)) {
                sourceTrackUri = param.value
            } else if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR.equals(param.name)) {
                sourceTrackFlavor = MediaPackageElementFlavor.parseFlavor(param.value)
            }
        }
        val sourceFile: File
        try {
            sourceFile = workspace!!.get(URI(sourceTrackUri!!))
        } catch (ex: IOException) {
            throw ProcessFailedException("Can't read " + sourceTrackUri!!)
        } catch (ex: NotFoundException) {
            throw ProcessFailedException("Workspace does not contain a track " + sourceTrackUri!!)
        } catch (ex: URISyntaxException) {
            throw ProcessFailedException("Source URI $sourceTrackUri is not valid.")
        }

        // inspect input file to retrieve media information
        var inspectionJob: Job
        val sourceTrack: Track
        try {
            inspectionJob = inspect(job, URI(sourceTrackUri))
            sourceTrack = MediaPackageElementParser.getFromXml(inspectionJob.payload) as Track
        } catch (e: URISyntaxException) {
            throw ProcessFailedException("Source URI $sourceTrackUri is not valid.")
        } catch (e: MediaInspectionException) {
            throw ProcessFailedException("Media inspection of $sourceTrackUri failed", e)
        } catch (e: MediaPackageException) {
            throw ProcessFailedException("Deserialization of source track $sourceTrackUri failed", e)
        }

        // get output file extension
        var outputFileExtension = properties.getProperty(VideoEditorProperties.DEFAULT_EXTENSION, ".mp4")
        outputFileExtension = properties.getProperty(VideoEditorProperties.OUTPUT_FILE_EXTENSION, outputFileExtension)

        if (!outputFileExtension.startsWith(".")) {
            outputFileExtension = ".$outputFileExtension"
        }

        // create working directory
        var tempDirectory = File(File(workspace!!.rootDirectory()), "editor")
        tempDirectory = File(tempDirectory, java.lang.Long.toString(job.id))
        val filename = String.format("%s-%s%s", sourceTrackFlavor, sourceFile.name, outputFileExtension)
        val outputPath = File(tempDirectory, filename)

        if (!outputPath.parentFile.exists()) {
            outputPath.parentFile.mkdirs()
        }
        val newTrackURI: URI
        inputfile.add(sourceFile.absolutePath) // default source - add to source table as 0
        val srcIndex = inputfile.indexOf(sourceFile.absolutePath) // index = 0
        logger.info("Start processing srcfile {}", sourceFile.absolutePath)
        try {
            // parse body elements
            for (element in smil.body.mediaElements) {
                // body should contain par elements
                if (element.isContainer) {
                    val container = element as SmilMediaContainer
                    if (SmilMediaContainer.ContainerType.PAR === container.containerType) {
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
                                        try {
                                            clipSourceFile = workspace!!.get(clipTrackURI)
                                        } catch (ex: IOException) {
                                            throw ProcessFailedException("Can't read $clipTrackURI")
                                        } catch (ex: NotFoundException) {
                                            throw ProcessFailedException("Workspace does not contain a track $clipTrackURI")
                                        }

                                    }
                                    var index: Int

                                    if (clipSourceFile != null) {      // clip has different source
                                        index = inputfile.indexOf(clipSourceFile.absolutePath) // Look for known tracks
                                        if (index == -1) {
                                            inputfile.add(clipSourceFile.absolutePath) // add new track
                                            //TODO: inspect each new video file, bad input will throw exc
                                        }
                                        index = inputfile.indexOf(clipSourceFile.absolutePath)
                                    } else {
                                        index = srcIndex // default src
                                    }

                                    videoclips.add(VideoClip(index, begin / 1000.0, end / 1000.0))
                                }
                            } else {
                                throw ProcessFailedException("Smil container '"
                                        + (elementChild as SmilMediaContainer).containerType.toString()
                                        + "'is not supportet yet")
                            }
                        }
                    } else {
                        throw ProcessFailedException("Smil container '"
                                + container.containerType.toString() + "'is not supportet yet")
                    }
                }
            }
            val cleanclips = sortSegments(videoclips)    // remove very short cuts that will look bad
            var error: String? = null
            val outputResolution = ""    //TODO: fetch the largest output resolution from SMIL.head.layout.root-layout
            // When outputResolution is set to WxH, all clips are scaled to that size in the output video.
            // TODO: Each clips could have a region id, relative to the root-layout
            // Then each clip is zoomed/panned/padded to WxH befor concatenation
            val ffmpeg = FFmpegEdit(properties)
            error = ffmpeg.processEdits(inputfile, outputPath.absolutePath, outputResolution, cleanclips,
                    sourceTrack.hasAudio(), sourceTrack.hasVideo())

            if (error != null) {
                FileUtils.deleteQuietly(tempDirectory)
                throw ProcessFailedException("Editing pipeline exited abnormaly! Error: $error")
            }

            // create Track for edited file
            val newTrackId = idBuilder.createNew().toString()
            val `in` = FileInputStream(outputPath)
            try {
                newTrackURI = workspace!!.putInCollection(COLLECTION_ID,
                        String.format("%s-%s%s", sourceTrackFlavor!!.type, newTrackId, outputFileExtension), `in`)
            } catch (ex: IllegalArgumentException) {
                throw ProcessFailedException("Copy track into workspace failed! " + ex.message)
            } finally {
                IOUtils.closeQuietly(`in`)
                FileUtils.deleteQuietly(tempDirectory)
            }

            // inspect new Track
            try {
                inspectionJob = inspect(job, newTrackURI)
            } catch (e: MediaInspectionException) {
                throw ProcessFailedException("Media inspection of $newTrackURI failed", e)
            }

            val editedTrack = MediaPackageElementParser.getFromXml(inspectionJob.payload) as Track
            logger.info("Finished editing track {}", editedTrack)
            editedTrack.identifier = newTrackId
            editedTrack.flavor = MediaPackageElementFlavor(sourceTrackFlavor!!.type!!, SINK_FLAVOR_SUBTYPE)

            return editedTrack

        } catch (ex: MediaInspectionException) {
            throw ProcessFailedException("Inspecting encoded Track failed with: " + ex.message)
        } catch (ex: MediaPackageException) {
            throw ProcessFailedException("Unable to serialize edited Track! " + ex.message)
        } catch (ex: Exception) {
            throw ProcessFailedException("Unable to process SMIL: " + ex.message, ex)
        } finally {
            FileUtils.deleteQuietly(tempDirectory)
        }
    }

    /*
   * Inspect the output file
   */
    @Throws(MediaInspectionException::class, ProcessFailedException::class)
    protected open fun inspect(job: Job, workspaceURI: URI): Job {
        val inspectionJob: Job
        try {
            inspectionJob = inspectionService!!.inspect(workspaceURI)
        } catch (e: MediaInspectionException) {
            incident().recordJobCreationIncident(job, e)
            throw MediaInspectionException("Media inspection of $workspaceURI failed", e)
        }

        val barrier = JobBarrier(job, serviceRegistry!!, inspectionJob)
        if (!barrier.waitForJobs()!!.isSuccess) {
            throw ProcessFailedException("Media inspection of $workspaceURI failed")
        }
        return inspectionJob
    }

    /**
     * {@inheritDoc}
     *
     * @see  org.opencastproject.videoeditor.api.VideoEditorService.processSmil
     */
    @Throws(ProcessFailedException::class)
    override fun processSmil(smil: Smil): List<Job> {
        if (smil == null) {
            throw ProcessFailedException("Smil document is null!")
        }

        val jobs = LinkedList<Job>()
        try {
            for (paramGroup in smil.head.paramGroups) {
                for (param in paramGroup.params) {
                    if (SmilMediaParam.PARAM_NAME_TRACK_ID.equals(param.name)) {
                        jobs.add(serviceRegistry!!.createJob(jobType!!, Operation.PROCESS_SMIL.toString(),
                                Arrays.asList(smil.toXML(), paramGroup.id), jobload))
                    }
                }
            }
            return jobs
        } catch (ex: JAXBException) {
            throw ProcessFailedException("Failed to serialize smil " + smil.id)
        } catch (ex: ServiceRegistryException) {
            throw ProcessFailedException("Failed to create job: " + ex.message)
        } catch (ex: Exception) {
            throw ProcessFailedException(ex.message)
        }

    }

    @Throws(Exception::class)
    public override fun process(job: Job): String {
        if (Operation.PROCESS_SMIL.toString() == job.operation) {
            val smil = smilService!!.fromXml(job.arguments[0]).smil
                    ?: throw ProcessFailedException("Smil document is null!")

            val editedTrack = processSmil(job, smil, job.arguments[1])
            return MediaPackageElementParser.getAsXml(editedTrack)
        }

        throw ProcessFailedException("Can't handle this operation: " + job.operation)
    }

    override fun activate(context: ComponentContext) {
        logger.debug("activating...")
        super.activate(context)
        FFmpegEdit.init(context.bundleContext)
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
            this.properties[key] = properties.get(key)
        }
        logger.debug("Properties updated!")

        jobload = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_KEY, DEFAULT_JOB_LOAD, serviceRegistry!!)
    }

    fun setMediaInspectionService(inspectionService: MediaInspectionService) {
        this.inspectionService = inspectionService
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    companion object {

        val JOB_LOAD_KEY = "job.load.videoeditor"

        private val DEFAULT_JOB_LOAD = 0.8f

        /**
         * The logging instance
         */
        private val logger = LoggerFactory.getLogger(VideoEditorServiceImpl::class.java)
        private val JOB_TYPE = "org.opencastproject.videoeditor"
        private val COLLECTION_ID = "videoeditor"
        private val SINK_FLAVOR_SUBTYPE = "trimmed"

        /* Clean up the edit points, make sure they are at least 2 seconds apart (default fade duration)
   * Otherwise it can be very slow to run and output will be ugly because of the cross fades
   */
        private fun sortSegments(edits: List<VideoClip>): List<VideoClip> {
            val ll = LinkedList<VideoClip>()
            val clips = ArrayList<VideoClip>()
            val it = edits.iterator()
            var clip: VideoClip
            var nextclip: VideoClip
            while (it.hasNext()) {     // Check for legal durations
                clip = it.next()
                if (clip.duration > 2) { // Keep segments at least 2 seconds long
                    ll.add(clip)
                }
            }
            clip = ll.pop()        // initialize
            while (!ll.isEmpty()) { // Check that 2 consecutive segments from same src are at least 2 secs apart
                if (ll.peek() != null) {
                    nextclip = ll.pop()  // check next consecutive segment
                    if (nextclip.src == clip.src && nextclip.start - clip.end < 2) { // collapse two segments into one
                        clip.end = nextclip.end                             // by using inpt of seg 1 and outpoint of seg 2
                    } else {
                        clips.add(clip)   // keep last segment
                        clip = nextclip   // check next segment
                    }
                }
            }
            clips.add(clip) // add last segment
            return clips
        }
    }
}
