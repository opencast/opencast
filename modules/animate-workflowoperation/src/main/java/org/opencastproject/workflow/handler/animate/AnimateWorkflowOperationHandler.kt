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
package org.opencastproject.workflow.handler.animate

import org.opencastproject.animate.api.AnimateService
import org.opencastproject.animate.api.AnimateServiceException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.UUID

/**
 * Workflow operation for the animate service.
 */
open class AnimateWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The animate service.  */
    private var animateService: AnimateService? = null

    /** The workspace service.  */
    private var workspace: Workspace? = null

    /** The inspection service  */
    private var mediaInspectionService: MediaInspectionService? = null

    public override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Registering animate workflow operation handler")
    }

    private fun addArgumentIfExists(operation: WorkflowOperationInstance, arguments: MutableList<String>,
                                    property: String, option: String) {
        val value = StringUtils.trimToNull(operation.getConfiguration(property))
        if (value != null) {
            arguments.add(option)
            arguments.add(value)
        }
    }

    private fun getMetadata(mediaPackage: MediaPackage): Map<String, String> {
        val metadata = HashMap<String, String>()
        // get episode metadata
        val flavors = arrayOf(MediaPackageElements.EPISODE, MediaPackageElements.SERIES)
        for (flavor in flavors) {

            // Get metadata catalogs
            for (catalog in mediaPackage.getCatalogs(flavor)) {
                val dc = DublinCoreUtil.loadDublinCore(workspace, catalog)
                for ((key1, value1) in dc.values) {
                    val key = String.format("%s.%s", flavor.subtype, key1.localName)
                    val value = value1[0].value
                    metadata[key] = value
                    logger.debug("metadata: {} -> {}", key, value)
                }
            }
        }
        return metadata
    }

    /**
     * {@inheritDoc}
     *
     * @see  org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage
        logger.info("Start animate workflow operation for media package {}", mediaPackage)

        val operation = workflowInstance.currentOperation
        val arguments: MutableList<String>

        // Check required options
        val animationFile = File(StringUtils.trimToEmpty(operation.getConfiguration(ANIMATION_FILE_PROPERTY)))
        if (!animationFile.isFile) {
            throw WorkflowOperationException(String.format("Animation file `%s` does not exist", animationFile))
        }
        val animation = animationFile.toURI()

        val targetFlavor: MediaPackageElementFlavor
        try {
            targetFlavor = MediaPackageElementFlavor.parseFlavor(StringUtils.trimToNull(
                    operation.getConfiguration(TARGET_FLAVOR_PROPERTY)))
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException("Invalid target flavor", e)
        }

        // Get optional options
        val targetTagsProperty = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS_PROPERTY))

        // Check if we have custom command line options
        val cmd = operation.getConfiguration(COMMANDLINE_ARGUMENTS_PROPERTY)
        if (StringUtils.isNotEmpty(cmd)) {
            arguments = Arrays.asList(*StringUtils.split(cmd))
        } else {
            // set default encoding
            arguments = ArrayList()
            arguments.add("-t")
            arguments.add("ffmpeg")
            arguments.add("--video-codec")
            arguments.add("libx264-lossless")
            arguments.add("--video-bitrate")
            arguments.add("10000")
            addArgumentIfExists(operation, arguments, WIDTH_PROPERTY, "-w")
            addArgumentIfExists(operation, arguments, HEIGHT_PROPERTY, "-h")
            addArgumentIfExists(operation, arguments, FPS_PROPERTY, "--fps")
        }

        val metadata = getMetadata(mediaPackage)

        val job: Job
        try {
            job = animateService!!.animate(animation, metadata, arguments)
        } catch (e: AnimateServiceException) {
            throw WorkflowOperationException(String.format("Rendering animation from '%s' in media package '%s' failed",
                    animation, mediaPackage), e)
        }

        if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException(String.format("Animate job for media package '%s' failed", mediaPackage))
        }

        // put animated clip into media package
        try {
            val output = URI(job.payload)
            val id = UUID.randomUUID().toString()
            val `in` = workspace!!.read(output)
            val uri = workspace!!.put(mediaPackage.identifier.toString(), id, FilenameUtils.getName(output.path), `in`)
            var track = TrackImpl()
            track.identifier = id
            track.flavor = targetFlavor
            track.setURI(uri)

            val inspection = mediaInspectionService!!.enrich(track, true)
            if (!waitForStatus(inspection).isSuccess) {
                throw AnimateServiceException(String.format("Animating %s failed", animation))
            }

            track = MediaPackageElementParser.getFromXml(inspection.payload) as TrackImpl

            // add track to media package
            for (tag in asList(targetTagsProperty)) {
                track.addTag(tag)
            }
            mediaPackage.add(track)
            workspace!!.delete(output)
        } catch (e: Exception) {
            throw WorkflowOperationException("Error handling animation service output", e)
        }

        try {
            workspace!!.cleanup(mediaPackage.identifier)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

        logger.info("Animate workflow operation for media package {} completed", mediaPackage)
        return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)
    }

    fun setAnimateService(animateService: AnimateService) {
        this.animateService = animateService
    }

    fun setMediaInspectionService(mediaInspectionService: MediaInspectionService) {
        this.mediaInspectionService = mediaInspectionService
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnimateWorkflowOperationHandler::class.java)

        /** Animation source file configuration property name.  */
        private val ANIMATION_FILE_PROPERTY = "animation-file"

        /** Command line arguments configuration property name.  */
        private val COMMANDLINE_ARGUMENTS_PROPERTY = "cmd-args"

        /** Animation width file configuration property name.  */
        private val WIDTH_PROPERTY = "width"

        /** Animation height configuration property name.  */
        private val HEIGHT_PROPERTY = "height"

        /** Animation fps configuration property name.  */
        private val FPS_PROPERTY = "fps"

        /** Target flavor configuration property name.  */
        private val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Target tags configuration property name.  */
        private val TARGET_TAGS_PROPERTY = "target-tags"
    }
}
