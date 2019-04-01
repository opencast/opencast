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
package org.opencastproject.workflow.handler.timelinepreviews

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService
import org.opencastproject.util.IoSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.ArrayList

/**
 * Workflow operation for the timeline previews service.
 */
class TimelinePreviewsWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The timeline previews service.  */
    private var timelinePreviewsService: TimelinePreviewsService? = null

    /** The workspace service.  */
    private var workspace: Workspace? = null

    public override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Registering timeline previews workflow operation handler")
    }

    /**
     * {@inheritDoc}
     *
     * @see  org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage
        logger.info("Start timeline previews workflow operation for mediapackage {}", mediaPackage.identifier.compact())

        val sourceFlavorProperty = StringUtils.trimToNull(
                workflowInstance.currentOperation.getConfiguration(SOURCE_FLAVOR_PROPERTY))
        val sourceTagsProperty = StringUtils.trimToNull(
                workflowInstance.currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY))
        if (StringUtils.isEmpty(sourceFlavorProperty) && StringUtils.isEmpty(sourceTagsProperty)) {
            throw WorkflowOperationException(String.format("Required property %s or %s not set",
                    SOURCE_FLAVOR_PROPERTY, SOURCE_TAGS_PROPERTY))
        }

        val targetFlavorProperty = StringUtils.trimToNull(
                workflowInstance.currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY))
                ?: throw WorkflowOperationException(String.format("Required property %s not set", TARGET_FLAVOR_PROPERTY))

        val targetTagsProperty = StringUtils.trimToNull(
                workflowInstance.currentOperation.getConfiguration(TARGET_TAGS_PROPERTY))

        val imageSizeArg = StringUtils.trimToNull(
                workflowInstance.currentOperation.getConfiguration(IMAGE_SIZE_PROPERTY))
        var imageSize: Int
        if (imageSizeArg != null) {
            try {
                imageSize = Integer.parseInt(imageSizeArg)
            } catch (e: NumberFormatException) {
                imageSize = DEFAULT_IMAGE_SIZE
                logger.info("No valid integer given for property {}, using default value: {}",
                        IMAGE_SIZE_PROPERTY, DEFAULT_IMAGE_SIZE)
            }

        } else {
            imageSize = DEFAULT_IMAGE_SIZE
            logger.info("Property {} not set, using default value: {}", IMAGE_SIZE_PROPERTY, DEFAULT_IMAGE_SIZE)
        }

        val trackSelector = TrackSelector()
        for (flavor in asList(sourceFlavorProperty)) {
            trackSelector.addFlavor(flavor)
        }
        for (tag in asList(sourceTagsProperty)) {
            trackSelector.addTag(tag)
        }
        val sourceTracks = trackSelector.select(mediaPackage, false)
        if (sourceTracks.isEmpty()) {
            logger.info("No tracks found in mediapackage {} with specified {} {}", mediaPackage.identifier.compact(),
                    SOURCE_FLAVOR_PROPERTY,
                    sourceFlavorProperty)
            createResult(mediaPackage, WorkflowOperationResult.Action.SKIP)
        }

        val timelinepreviewsJobs = ArrayList<Job>(sourceTracks.size)
        for (sourceTrack in sourceTracks) {
            try {
                // generate timeline preview images
                logger.info("Create timeline previews job for track '{}' in mediapackage '{}'",
                        sourceTrack.identifier, mediaPackage.identifier.compact())

                val timelinepreviewsJob = timelinePreviewsService!!.createTimelinePreviewImages(sourceTrack, imageSize)
                timelinepreviewsJobs.add(timelinepreviewsJob)
            } catch (ex: MediaPackageException) {
                logger.error("Creating timeline previews job for track '{}' in media package '{}' failed with error {}",
                        sourceTrack.identifier, mediaPackage.identifier.compact(), ex.message)
            } catch (ex: TimelinePreviewsException) {
                logger.error("Creating timeline previews job for track '{}' in media package '{}' failed with error {}", sourceTrack.identifier, mediaPackage.identifier.compact(), ex.message)
            }

        }

        logger.info("Wait for timeline previews jobs for media package {}", mediaPackage.identifier.compact())
        if (!waitForStatus(*timelinepreviewsJobs.toTypedArray()).isSuccess) {
            cleanupWorkspace(timelinepreviewsJobs)
            throw WorkflowOperationException(
                    String.format("Timeline previews jobs for media package '%s' have not completed successfully",
                            mediaPackage.identifier.compact()))
        }


        try {
            // copy timeline previews attachments into workspace and add them to the media package
            for (job in timelinepreviewsJobs) {
                val jobPayload = job.payload
                if (StringUtils.isNotEmpty(jobPayload)) {
                    var timelinePreviewsMpe: MediaPackageElement? = null
                    var timelinePreviewsFile: File? = null
                    try {
                        timelinePreviewsMpe = MediaPackageElementParser.getFromXml(jobPayload)
                        timelinePreviewsFile = workspace!!.get(timelinePreviewsMpe.getURI())
                    } catch (ex: MediaPackageException) {
                        // unexpected job payload
                        throw WorkflowOperationException("Can't parse timeline previews attachment from job " + job.id)
                    } catch (ex: NotFoundException) {
                        throw WorkflowOperationException("Timeline preview images file '" + timelinePreviewsMpe!!.getURI()
                                + "' not found", ex)
                    } catch (ex: IOException) {
                        throw WorkflowOperationException("Can't get workflow image file '" + timelinePreviewsMpe!!.getURI()
                                + "' from workspace")
                    }

                    var timelinePreviewsInputStream: FileInputStream? = null
                    logger.info("Put timeline preview images file {} from media package {} to the media package work space",
                            timelinePreviewsMpe.getURI(), mediaPackage.identifier.compact())

                    try {
                        timelinePreviewsInputStream = FileInputStream(timelinePreviewsFile!!)
                        val fileName = FilenameUtils.getName(timelinePreviewsMpe.getURI().getPath())
                        val timelinePreviewsWfrUri = workspace!!.put(mediaPackage.identifier.compact(),
                                timelinePreviewsMpe.identifier, fileName, timelinePreviewsInputStream)
                        timelinePreviewsMpe.setURI(timelinePreviewsWfrUri)
                    } catch (ex: FileNotFoundException) {
                        throw WorkflowOperationException("Timeline preview images file " + timelinePreviewsFile!!.path
                                + " not found", ex)
                    } catch (ex: IOException) {
                        throw WorkflowOperationException("Can't read just created timeline preview images file " + timelinePreviewsFile!!.path, ex)
                    } catch (ex: IllegalArgumentException) {
                        throw WorkflowOperationException(ex)
                    } finally {
                        IoSupport.closeQuietly(timelinePreviewsInputStream)
                    }

                    // set the timeline previews attachment flavor and add it to the mediapackage
                    var targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorProperty)
                    if ("*" == targetFlavor.type) {
                        targetFlavor = MediaPackageElementFlavor(timelinePreviewsMpe.flavor.type!!, targetFlavor.subtype!!)
                    }
                    if ("*" == targetFlavor.subtype) {
                        targetFlavor = MediaPackageElementFlavor(targetFlavor.type!!, timelinePreviewsMpe.flavor.subtype!!)
                    }
                    timelinePreviewsMpe.flavor = targetFlavor
                    if (!StringUtils.isEmpty(targetTagsProperty)) {
                        for (tag in asList(targetTagsProperty)) {
                            timelinePreviewsMpe.addTag(tag)
                        }
                    }

                    mediaPackage.add(timelinePreviewsMpe)
                }
            }
        } finally {
            cleanupWorkspace(timelinepreviewsJobs)
        }


        logger.info("Timeline previews workflow operation for mediapackage {} completed", mediaPackage.identifier.compact())
        return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE)
    }

    /**
     * Remove all files created by the given jobs
     * @param jobs
     */
    private fun cleanupWorkspace(jobs: List<Job>) {
        for (job in jobs) {
            val jobPayload = job.payload
            if (StringUtils.isNotEmpty(jobPayload)) {
                try {
                    val timelinepreviewsMpe = MediaPackageElementParser.getFromXml(jobPayload)
                    val timelinepreviewsUri = timelinepreviewsMpe.getURI()
                    workspace!!.delete(timelinepreviewsUri)
                } catch (ex: MediaPackageException) {
                    // unexpected job payload
                    logger.error("Can't parse timeline previews attachment from job {}", job.id)
                } catch (ex: NotFoundException) {
                    // this is ok, because we want delete the file
                } catch (ex: IOException) {
                    logger.warn("Deleting timeline previews image file from workspace failed: {}", ex.message)
                    // this is ok, because workspace cleaner will remove old files if they exist
                }

            }
        }
    }

    fun setTimelinePreviewsService(timelinePreviewsService: TimelinePreviewsService) {
        this.timelinePreviewsService = timelinePreviewsService
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(TimelinePreviewsWorkflowOperationHandler::class.java)

        /** Source flavor configuration property name.  */
        private val SOURCE_FLAVOR_PROPERTY = "source-flavor"

        /** Source tags configuration property name.  */
        private val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Target flavor configuration property name.  */
        private val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Target tags configuration property name.  */
        private val TARGET_TAGS_PROPERTY = "target-tags"

        /** Image size configuration property name.  */
        private val IMAGE_SIZE_PROPERTY = "image-count"

        /** Default value for image size.  */
        private val DEFAULT_IMAGE_SIZE = 10
    }
}
