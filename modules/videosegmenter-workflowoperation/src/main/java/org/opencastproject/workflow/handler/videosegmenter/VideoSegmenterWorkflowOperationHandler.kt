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

package org.opencastproject.workflow.handler.videosegmenter

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.videosegmenter.api.VideoSegmenterService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays

/**
 * The workflow definition will run suitable recordings by the video segmentation.
 */
class VideoSegmenterWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var videosegmenter: VideoSegmenterService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running video segmentation on workflow {}", workflowInstance.id)

        val operation = workflowInstance.currentOperation
        val mediaPackage = workflowInstance.mediaPackage

        // Find movie track to analyze
        val trackFlavor = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_FLAVOR))
        val targetTags = asList(operation.getConfiguration(PROP_TARGET_TAGS))
        val candidates = ArrayList<Track>()
        if (trackFlavor != null)
            candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(trackFlavor))))
        else
            candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElements.PRESENTATION_SOURCE)))

        // Remove unsupported tracks (only those containing video can be segmented)
        val ti = candidates.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            if (!t.hasVideo())
                ti.remove()
        }

        // Found one?
        if (candidates.size == 0) {
            logger.info("No matching tracks available for video segmentation in workflow {}", workflowInstance)
            return createResult(Action.CONTINUE)
        }

        // More than one left? Let's be pragmatic...
        if (candidates.size > 1) {
            logger.info("Found more than one track to segment, choosing the first one ({})", candidates[0])
        }
        val track = candidates[0]

        // Segment the media package
        var mpeg7Catalog: Catalog? = null
        var job: Job? = null
        try {
            job = videosegmenter!!.segment(track)
            if (!waitForStatus(job).isSuccess) {
                throw WorkflowOperationException("Video segmentation of $track failed")
            }
            mpeg7Catalog = MediaPackageElementParser.getFromXml(job.payload) as Catalog
            mediaPackage.add(mpeg7Catalog)
            mpeg7Catalog.setURI(workspace!!.moveTo(mpeg7Catalog.getURI(), mediaPackage.identifier.toString(),
                    mpeg7Catalog.identifier, "segments.xml"))
            mpeg7Catalog.reference = MediaPackageReferenceImpl(track)
            // Add target tags
            for (tag in targetTags) {
                mpeg7Catalog.addTag(tag)
            }
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

        logger.debug("Video segmentation completed")
        return createResult(mediaPackage, Action.CONTINUE, job.queueTime!!)
    }

    /**
     * Callback for declarative services configuration that will introduce us to the videosegmenter service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param videosegmenter
     * the video segmenter
     */
    protected fun setVideoSegmenter(videosegmenter: VideoSegmenterService) {
        this.videosegmenter = videosegmenter
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

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(VideoSegmenterWorkflowOperationHandler::class.java)

        /** Name of the configuration key that specifies the flavor of the track to analyze  */
        private val PROP_ANALYSIS_TRACK_FLAVOR = "source-flavor"

        /** Name of the configuration key that specifies the flavor of the track to analyze  */
        private val PROP_TARGET_TAGS = "target-tags"
    }

}
