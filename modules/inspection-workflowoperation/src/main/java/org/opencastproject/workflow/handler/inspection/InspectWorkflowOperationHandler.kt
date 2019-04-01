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

package org.opencastproject.workflow.handler.inspection

import java.lang.String.format

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionOptions
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.UnsupportedElementException
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils
import org.opencastproject.metadata.dublincore.Precision
import org.opencastproject.util.MimeType
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.HashMap

/**
 * Workflow operation used to inspect all tracks of a media package.
 */
class InspectWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The inspection service  */
    private var inspectionService: MediaInspectionService? = null

    /** The dublin core catalog service  */
    private var dcService: DublinCoreCatalogService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    fun setDublincoreService(dcService: DublinCoreCatalogService) {
        this.dcService = dcService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param inspectionService
     * the inspection service
     */
    fun setInspectionService(inspectionService: MediaInspectionService) {
        this.inspectionService = inspectionService
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
        val mediaPackage = workflowInstance.mediaPackage.clone() as MediaPackage
        // Inspect the tracks
        var totalTimeInQueue: Long = 0

        val operation = workflowInstance.currentOperation
        val rewrite = "true".equals(operation.getConfiguration(OPT_OVERWRITE), ignoreCase = true)
        val acceptNoMedia = "true".equals(operation.getConfiguration(OPT_ACCEPT_NO_MEDIA), ignoreCase = true)

        val options = HashMap<String, String>()
        if ("true".equals(operation.getConfiguration(OPT_ACCURATE_FRAME_COUNT), ignoreCase = true)) {
            logger.info("Using accurate frame count for inspection media package {}", mediaPackage)
            options[MediaInspectionOptions.OPTION_ACCURATE_FRAME_COUNT] = java.lang.Boolean.TRUE.toString()
        }

        // Test if there are tracks in the mediapackage
        if (mediaPackage.tracks.size == 0) {
            logger.warn("Recording {} contains no media", mediaPackage)
            if (!acceptNoMedia)
                throw WorkflowOperationException("Mediapackage $mediaPackage contains no media")
        }

        for (track in mediaPackage.tracks) {

            logger.info("Inspecting track '{}' of {}", track.identifier, mediaPackage)

            var inspectJob: Job? = null
            val inspectedTrack: Track?
            if (track != null && track.getURI() != null && (track.getURI().toString().endsWith(".vtt") || track.getURI().toString().endsWith(".srt"))) {
                inspectedTrack = track.clone() as Track
                inspectedTrack.mimeType = MimeType.mimeType("text", "vtt")
                logger.info("Track '{}' of {} contains captions", track.identifier, mediaPackage)
            } else {
                try {
                    inspectJob = inspectionService!!.enrich(track, rewrite, options)
                    if (!waitForStatus(inspectJob).isSuccess) {
                        throw WorkflowOperationException("Track $track could not be inspected")
                    }
                } catch (e: MediaInspectionException) {
                    throw WorkflowOperationException("Error inspecting media package", e)
                } catch (e: MediaPackageException) {
                    throw WorkflowOperationException("Error parsing media package", e)
                }

                // add this receipt's queue and execution times to the total
                val timeInQueue = if (inspectJob.queueTime == null) 0 else inspectJob.queueTime
                totalTimeInQueue += timeInQueue


                try {
                    inspectedTrack = MediaPackageElementParser.getFromXml(inspectJob.payload) as Track
                } catch (e: MediaPackageException) {
                    throw WorkflowOperationException("Unable to parse track from job " + inspectJob.id, e)
                }

                if (inspectedTrack == null)
                    throw WorkflowOperationException("Track $track could not be inspected")

                if (inspectedTrack.streams.size == 0)
                    throw WorkflowOperationException(format("Track %s does not contain any streams", track))
            }
            // Replace the original track with the inspected one
            try {
                mediaPackage.remove(track)
                mediaPackage.add(inspectedTrack)
            } catch (e: UnsupportedElementException) {
                logger.error("Error adding {} to media package", inspectedTrack, e)
            }

        }

        // Update dublin core with metadata
        try {
            updateDublinCore(mediaPackage)
        } catch (e: Exception) {
            logger.warn("Unable to update dublin core data: {}", e.message, e)
            throw WorkflowOperationException(e.message)
        }

        return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
    }

    /**
     * Updates those dublin core fields that can be gathered from the technical metadata.
     *
     * @param mediaPackage
     * the media package
     */
    @Throws(Exception::class)
    protected fun updateDublinCore(mediaPackage: MediaPackage) {
        // Complete episode dublin core catalog (if available)
        val dcCatalogs = mediaPackage.getCatalogs(MediaPackageElements.EPISODE,
                MediaPackageReferenceImpl.ANY_MEDIAPACKAGE)
        if (dcCatalogs.size > 0) {
            val dublinCore = loadDublinCoreCatalog(dcCatalogs[0])

            // Extent
            if (mediaPackage.duration != null && !dublinCore.hasValue(DublinCore.PROPERTY_EXTENT)) {
                val extent = EncodingSchemeUtils.encodeDuration(mediaPackage.duration!!)
                dublinCore[DublinCore.PROPERTY_EXTENT] = extent
                logger.debug("Setting dc:extent to '{}'", extent.value)
            }

            // Date created
            if (mediaPackage.date != null && !dublinCore.hasValue(DublinCore.PROPERTY_CREATED)) {
                val date = EncodingSchemeUtils.encodeDate(mediaPackage.date, Precision.Minute)
                dublinCore[DublinCore.PROPERTY_CREATED] = date
                logger.debug("Setting dc:date to '{}'", date.value)
            }

            // Serialize changed dublin core
            val `in` = dcService!!.serialize(dublinCore)
            val mpId = mediaPackage.identifier.toString()
            val elementId = dcCatalogs[0].identifier
            workspace!!.put(mpId, elementId, FilenameUtils.getName(dcCatalogs[0].getURI().getPath()), `in`)
            dcCatalogs[0].setURI(workspace!!.getURI(mpId, elementId))
        }
    }

    /**
     * Loads a dublin core catalog from a mediapackage's catalog reference
     *
     * @param catalog
     * the mediapackage's reference to this catalog
     * @return the dublin core
     * @throws IOException
     * if there is a problem loading or parsing the dublin core object
     */
    @Throws(IOException::class)
    protected fun loadDublinCoreCatalog(catalog: Catalog): DublinCoreCatalog {
        var `in`: InputStream? = null
        try {
            val f = workspace!!.get(catalog.getURI())
            `in` = FileInputStream(f)
            return dcService!!.load(`in`)
        } catch (e: NotFoundException) {
            throw IOException("Unable to open catalog $catalog", e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(InspectWorkflowOperationHandler::class.java)

        /** Option for rewriting existing metadata  */
        private val OPT_OVERWRITE = "overwrite"

        /** Option to adjust whether mediapackages without media should be accepted  */
        private val OPT_ACCEPT_NO_MEDIA = "accept-no-media"

        /** Option to adjust whether the exact frame count should be determined
         * Note that this is an expensive operation. Its use should be avoided if not depending on the exact framecount
         * Default: false  */
        private val OPT_ACCURATE_FRAME_COUNT = "accurate-frame-count"
    }

}
