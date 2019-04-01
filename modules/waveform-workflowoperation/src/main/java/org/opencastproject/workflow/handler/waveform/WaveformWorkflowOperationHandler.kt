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
package org.opencastproject.workflow.handler.waveform

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.waveform.api.WaveformService
import org.opencastproject.waveform.api.WaveformServiceException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI
import java.util.ArrayList

/**
 * Workflow operation for the waveform service.
 */
open class WaveformWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The waveform service.  */
    private var waveformService: WaveformService? = null

    /** The workspace service.  */
    private var workspace: Workspace? = null

    public override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Registering waveform workflow operation handler")
    }

    /**
     * {@inheritDoc}
     *
     * @see  org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mediaPackage = workflowInstance.mediaPackage
        logger.info("Start waveform workflow operation for mediapackage {}", mediaPackage)

        val sourceFlavorProperty = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(SOURCE_FLAVOR_PROPERTY))
        val sourceTagsProperty = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY))
        if (StringUtils.isEmpty(sourceFlavorProperty) && StringUtils.isEmpty(sourceTagsProperty)) {
            throw WorkflowOperationException(
                    String.format("Required property %s or %s not set", SOURCE_FLAVOR_PROPERTY, SOURCE_TAGS_PROPERTY))
        }

        val targetFlavorProperty = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY))
                ?: throw WorkflowOperationException(String.format("Required property %s not set", TARGET_FLAVOR_PROPERTY))

        val targetTagsProperty = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(TARGET_TAGS_PROPERTY))

        val pixelsPerMinute = NumberUtils.toInt(
                StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(PIXELS_PER_MINUTE_PROPERTY)),
                DEFAULT_PIXELS_PER_MINUTE
        )

        val minWidth = NumberUtils.toInt(
                StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(MIN_WIDTH_PROPERTY)),
                DEFAULT_MIN_WIDTH
        )

        val maxWidth = NumberUtils.toInt(
                StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(MAX_WIDTH_PROPERTY)),
                DEFAULT_MAX_WIDTH
        )

        val height = NumberUtils.toInt(
                StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(HEIGHT_PROPERTY)),
                DEFAULT_HEIGHT
        )

        val color = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(COLOR_PROPERTY))

        try {
            val trackSelector = TrackSelector()
            for (flavor in asList(sourceFlavorProperty)) {
                trackSelector.addFlavor(flavor)
            }
            for (tag in asList(sourceTagsProperty)) {
                trackSelector.addTag(tag)
            }
            val sourceTracks = trackSelector.select(mediaPackage, false)
            if (sourceTracks.isEmpty()) {
                logger.info("No tracks found in mediapackage {} with specified {} = {}", mediaPackage, SOURCE_FLAVOR_PROPERTY,
                        sourceFlavorProperty)
                return createResult(mediaPackage, WorkflowOperationResult.Action.SKIP)
            }

            val waveformJobs = ArrayList<Job>(sourceTracks.size)
            for (sourceTrack in sourceTracks) {
                // Skip over track with no audio stream
                if (!sourceTrack.hasAudio()) {
                    logger.info("Skipping waveform extraction of track {} since it has no audio", sourceTrack.identifier)
                    continue
                }
                try {
                    // generate waveform
                    logger.info("Creating waveform extraction job for track '{}' in mediapackage '{}'", sourceTrack.identifier, mediaPackage)

                    val waveformJob = waveformService!!.createWaveformImage(sourceTrack, pixelsPerMinute, minWidth, maxWidth,
                            height, color)
                    waveformJobs.add(waveformJob)
                } catch (e: MediaPackageException) {
                    logger.error("Creating waveform extraction job for track '{}' in media package '{}' failed", sourceTrack.identifier, mediaPackage, e)
                } catch (e: WaveformServiceException) {
                    logger.error("Creating waveform extraction job for track '{}' in media package '{}' failed", sourceTrack.identifier, mediaPackage, e)
                }

            }

            logger.debug("Waiting for waveform jobs for media package {}", mediaPackage)
            if (!waitForStatus(*waveformJobs.toTypedArray()).isSuccess) {
                throw WorkflowOperationException(String.format("Waveform extraction jobs for media package '%s' have not completed successfully",
                        mediaPackage.identifier))
            }

            // copy waveform attachments into workspace and add them to the media package
            for (job in waveformJobs) {
                val jobPayload = job.payload
                if (StringUtils.isEmpty(jobPayload)) {
                    continue
                }
                var waveformMpe: MediaPackageElement? = null
                try {
                    waveformMpe = MediaPackageElementParser.getFromXml(jobPayload)
                    val newURI = workspace!!.moveTo(waveformMpe.getURI(), mediaPackage.identifier.toString(), waveformMpe.identifier,
                            "waveform.png")
                    waveformMpe.setURI(newURI)
                } catch (ex: MediaPackageException) {
                    // unexpected job payload
                    throw WorkflowOperationException("Can't parse waveform attachment from job " + job.id)
                } catch (ex: NotFoundException) {
                    throw WorkflowOperationException("Waveform image file '" + waveformMpe!!.getURI() + "' not found", ex)
                } catch (ex: IOException) {
                    throw WorkflowOperationException("Can't get workflow image file '" + waveformMpe!!.getURI() + "' from workspace")
                }

                // set the waveform attachment flavor and add it to the media package
                var targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty)
                if ("*" == targetFlavor.type) {
                    targetFlavor = MediaPackageElementFlavor(waveformMpe.flavor.type!!, targetFlavor.subtype!!)
                }
                if ("*" == targetFlavor.subtype) {
                    targetFlavor = MediaPackageElementFlavor(targetFlavor.type!!, waveformMpe.flavor.subtype!!)
                }
                waveformMpe.flavor = targetFlavor
                for (tag in asList(targetTagsProperty)) {
                    waveformMpe.addTag(tag)
                }
                mediaPackage.add(waveformMpe)
            }

            logger.info("Waveform workflow operation for mediapackage {} completed", mediaPackage)
            return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)

        } finally {
            try {
                workspace!!.cleanup(mediaPackage.identifier, true)
            } catch (e: IOException) {
                throw WorkflowOperationException(e)
            }

        }
    }

    fun setWaveformService(waveformService: WaveformService) {
        this.waveformService = waveformService
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WaveformWorkflowOperationHandler::class.java)

        /** Source flavor configuration property name.  */
        private val SOURCE_FLAVOR_PROPERTY = "source-flavor"

        /** Source tags configuration property name.  */
        private val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Target flavor configuration property name.  */
        private val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Target tags configuration property name.  */
        private val TARGET_TAGS_PROPERTY = "target-tags"

        /** Default value of pixel per minute configuration.  */
        private val DEFAULT_PIXELS_PER_MINUTE = 200

        /** Default value of minimum width configuration.  */
        private val DEFAULT_MIN_WIDTH = 5000

        /** Default value of maximum width configuration.  */
        private val DEFAULT_MAX_WIDTH = 20000

        /** Default value of height configuration.  */
        private val DEFAULT_HEIGHT = 500

        /** Pixel per minute of waveform image width configuration property name.  */
        private val PIXELS_PER_MINUTE_PROPERTY = "pixels-per-minute"

        /** Minimun width of waveform image configuration property name.  */
        private val MIN_WIDTH_PROPERTY = "min-width"

        /** Maximum width of waveform image configuration property name.  */
        private val MAX_WIDTH_PROPERTY = "max-width"

        /** Height of waveform image configuration property name.  */
        private val HEIGHT_PROPERTY = "height"

        /** Color of waveform image configuration property name.  */
        private val COLOR_PROPERTY = "color"
    }
}
