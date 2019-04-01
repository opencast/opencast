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
package org.opencastproject.workflow.handler.workflow

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.Fraction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.HashMap

/**
 * Workflow operation handler for analyzing tracks and set control variables.
 */
class AnalyzeTracksWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        logger.info("Running analyze-tracks workflow operation on workflow {}", workflowInstance.id)
        val mediaPackage = workflowInstance.mediaPackage
        val sourceFlavor = getConfig(workflowInstance, OPT_SOURCE_FLAVOR)
        val properties = HashMap<String, String>()

        val flavor = MediaPackageElementFlavor.parseFlavor(sourceFlavor)
        val tracks = mediaPackage.getTracks(flavor)
        if (tracks.size <= 0) {
            if (BooleanUtils.toBoolean(getConfig(workflowInstance, OPT_FAIL_NO_TRACK, "false"))) {
                throw WorkflowOperationException("No matching tracks for flavor $sourceFlavor")
            }
            logger.info("No tracks with specified flavors ({}) to analyse.", sourceFlavor)
            return createResult(mediaPackage, properties, Action.CONTINUE, 0)
        }

        val aspectRatios = getAspectRatio(getConfig(workflowInstance, OPT_VIDEO_ASPECT, ""))

        for (track in tracks) {
            val varName = toVariableName(track.flavor)
            properties[varName + "_media"] = "true"
            properties[varName + "_video"] = java.lang.Boolean.toString(track.hasVideo())
            properties[varName + "_audio"] = java.lang.Boolean.toString(track.hasAudio())

            // Check resolution
            if (track.hasVideo()) {
                for (video in (track as TrackImpl).getVideo()!!) {
                    // Set resolution variables
                    properties[varName + "_resolution_x"] = video.frameWidth!!.toString()
                    properties[varName + "_resolution_y"] = video.frameHeight!!.toString()
                    var trackAspect: Fraction? = Fraction.getReducedFraction(video.frameWidth!!, video.frameHeight!!)
                    properties[varName + "_aspect"] = trackAspect!!.toString()
                    properties[varName + "_framerate"] = video.frameRate!!.toString()

                    // Check if we should fall back to nearest defined aspect ratio
                    if (!aspectRatios.isEmpty()) {
                        trackAspect = getNearestAspectRatio(trackAspect, aspectRatios)
                        properties[varName + "_aspect_snap"] = trackAspect!!.toString()
                    }
                }
            }
        }
        logger.info("Finished analyze-tracks workflow operation adding the properties: {}", properties)
        return createResult(mediaPackage, properties, Action.CONTINUE, 0)
    }

    /**
     * Get nearest aspect ratio from list
     *
     * @param videoAspect
     * Aspect ratio of video to check
     * @param aspects
     * List of aspect ratios to snap to.
     * @return Nearest aspect ratio
     */
    internal fun getNearestAspectRatio(videoAspect: Fraction, aspects: List<Fraction>): Fraction? {
        var nearestAspect = aspects[0]
        for (aspect in aspects) {
            if (videoAspect.subtract(nearestAspect).abs().compareTo(videoAspect.subtract(aspect).abs()) > 0) {
                nearestAspect = aspect
            }
        }
        return nearestAspect
    }

    /**
     * Get aspect ratios to check from configuration string.
     *
     * @param aspectConfig
     * Configuration string
     * @return List of aspect rations to check
     */
    internal fun getAspectRatio(aspectConfig: String): List<Fraction> {
        val aspectRatios = ArrayList<Fraction>()
        for (aspect in aspectConfig.split(" *, *".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (StringUtils.isNotBlank(aspect)) {
                aspectRatios.add(Fraction.getFraction(aspect).reduce())
            }
        }
        return aspectRatios
    }

    companion object {

        /** Configuration key for the "flavor" of the tracks to use as a source input  */
        internal val OPT_SOURCE_FLAVOR = "source-flavor"

        /** Configuration key for video aspect ratio to check  */
        internal val OPT_VIDEO_ASPECT = "aspect-ratio"

        /** Configuration key to define behavior if no track matches  */
        internal val OPT_FAIL_NO_TRACK = "fail-no-track"

        /** The logging facility  */
        private val logger = LoggerFactory
                .getLogger(AnalyzeTracksWorkflowOperationHandler::class.java)

        /** Create a name for a workflow variable from a flavor  */
        private fun toVariableName(flavor: MediaPackageElementFlavor): String {
            return flavor.type + "_" + flavor.subtype
        }
    }
}
