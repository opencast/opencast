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

package org.opencastproject.workflow.handler.composer

import org.opencastproject.util.data.Collections.nil
import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageSupport.Filters
import org.opencastproject.mediapackage.Track
import org.opencastproject.util.JobUtil
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Function2
import org.opencastproject.util.data.Monadics
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.functions.Booleans
import org.opencastproject.util.data.functions.Misc
import org.opencastproject.util.data.functions.Strings
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The workflow definition creating a video from a still image.
 */
class ImageToVideoWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the local composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
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

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running image to video workflow operation on workflow {}", workflowInstance.id)
        try {
            return imageToVideo(workflowInstance.mediaPackage, workflowInstance)
        } catch (e: WorkflowOperationException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    @Throws(Exception::class)
    private fun imageToVideo(mp: MediaPackage, wi: WorkflowInstance): WorkflowOperationResult {
        // read cfg
        val sourceTags = getCfg(wi, OPT_SOURCE_TAGS).map(asList).getOrElse(nil(String::class.java))
        val sourceFlavor = getCfg(wi, OPT_SOURCE_FLAVOR).map<MediaPackageElementFlavor>(
                MediaPackageElementFlavor.parseFlavor)
        if (sourceFlavor.isNone && sourceTags.isEmpty()) {
            logger.warn("No source tags or flavor are given to determine the image to use")
            return createResult(mp, Action.SKIP)
        }
        val targetTags = getCfg(wi, OPT_TARGET_TAGS).map(asList).getOrElse(nil(String::class.java))
        val targetFlavor = getCfg(wi, OPT_TARGET_FLAVOR).map<MediaPackageElementFlavor>(
                MediaPackageElementFlavor.parseFlavor)
        val duration = getCfg(wi, OPT_DURATION).bind(Strings.toDouble).getOrElse(
                this.cfgKeyMissing(OPT_DURATION))
        val profile = getCfg(wi, OPT_PROFILE).getOrElse(this.cfgKeyMissing(OPT_PROFILE))
        // run image to video jobs
        val jobs = Monadics.mlist<MediaPackageElement>(*mp.attachments)
                .filter(sourceFlavor.map(Filters.matchesFlavor).getOrElse(Booleans.yes()))
                .filter(Filters.hasTagAny(sourceTags)).map(Misc.cast<MediaPackageElement, Attachment>())
                .map(imageToVideo(profile, duration)).value()
        if (JobUtil.waitForJobs(serviceRegistry, jobs)!!.isSuccess) {
            for (job in jobs) {
                if (job.payload.length > 0) {
                    val track = MediaPackageElementParser.getFromXml(job.payload) as Track
                    track.setURI(workspace!!.moveTo(track.getURI(), mp.identifier.toString(), track.identifier,
                            FilenameUtils.getName(track.getURI().toString())))
                    // Adjust the target tags
                    for (tag in targetTags) {
                        track.addTag(tag)
                    }
                    // Adjust the target flavor.
                    for (flavor in targetFlavor) {
                        track.flavor = flavor
                    }
                    // store new tracks to mediaPackage
                    mp.add(track)
                    logger.debug("Image to video operation completed")
                } else {
                    logger.info("Image to video operation unsuccessful, no payload returned: {}", job)
                    return createResult(mp, Action.SKIP)
                }
            }
            return createResult(mp, Action.CONTINUE, mlist(jobs).foldl(0L, object : Function2<Long, Job, Long>() {
                override fun apply(max: Long?, job: Job): Long? {
                    return Math.max(max!!, job.queueTime!!)
                }
            }))
        } else {
            throw WorkflowOperationException("The image to video encoding jobs did not return successfully")
        }
    }

    /** Returned function may throw exceptions.  */
    private fun imageToVideo(profile: String, duration: Double): Function<Attachment, Job> {
        return object : Function.X<Attachment, Job>() {
            @Throws(MediaPackageException::class, EncoderException::class)
            override fun xapply(attachment: Attachment): Job {
                logger.info("Converting image {} to a video of {} sec", attachment.getURI().toString(), duration)
                return composerService!!.imageToVideo(attachment, profile, duration)
            }
        }
    }

    companion object {
        private val OPT_SOURCE_TAGS = "source-tags"
        private val OPT_SOURCE_FLAVOR = "source-flavor"
        private val OPT_TARGET_TAGS = "target-tags"
        private val OPT_TARGET_FLAVOR = "target-flavor"
        private val OPT_DURATION = "duration"
        private val OPT_PROFILE = "profile"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ImageToVideoWorkflowOperationHandler::class.java)
    }
}
