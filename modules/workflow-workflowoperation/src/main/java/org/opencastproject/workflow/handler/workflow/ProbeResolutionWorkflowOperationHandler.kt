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

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.Fraction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

/**
 * Workflow operation handler for setting variables based on video resolutions
 */

class ProbeResolutionWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        logger.info("Running probe-resolution workflow operation")
        val mediaPackage = workflowInstance.mediaPackage
        val sourceFlavorName = getConfig(workflowInstance, OPT_SOURCE_FLAVOR)
        val sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorName)

        // Ensure we have a matching track
        val tracks = mediaPackage.getTracks(sourceFlavor)
        if (tracks.size <= 0) {
            logger.info("No tracks with specified flavor ({}).", sourceFlavorName)
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Create mapping:  resolution -> [varNames]
        val resolutionMapping = HashMap<Fraction, Set<String>>()
        for (key in workflowInstance.currentOperation.configurationKeys) {
            if (key.startsWith(OPT_VAR_PREFIX)) {
                val varName = key.substring(OPT_VAR_PREFIX.length)
                for (resolution in getResolutions(getConfig(workflowInstance, key))) {
                    if (!resolutionMapping.containsKey(resolution)) {
                        resolutionMapping[resolution] = HashSet()
                    }
                    resolutionMapping[resolution].add(varName)
                }
            }
        }

        // Create mapping:  varName -> value
        val valueMapping = HashMap<String, String>()
        for (key in workflowInstance.currentOperation.configurationKeys) {
            if (key.startsWith(OPT_VAL_PREFIX)) {
                val varName = key.substring(OPT_VAL_PREFIX.length)
                valueMapping[varName] = getConfig(workflowInstance, key)
            }
        }

        val properties = HashMap<String, String>()
        for (track in tracks) {
            val flavor = toVariableName(track.flavor)

            // Check if resolution fits
            if (track.hasVideo()) {
                for (video in (track as TrackImpl).getVideo()!!) {
                    val resolution = Fraction.getFraction(video.frameWidth!!, video.frameHeight!!)
                    if (resolutionMapping.containsKey(resolution)) {
                        for (varName in resolutionMapping[resolution]) {
                            val value = if (valueMapping.containsKey(varName)) valueMapping[varName] else "true"
                            properties[flavor + varName] = value
                        }
                    }
                }
            }
        }
        logger.info("Finished workflow operation adding the properties: {}", properties)
        return createResult(mediaPackage, properties, Action.CONTINUE, 0)
    }

    /**
     * Get resolution to probe for from configuration string.
     *
     * @param resolutionsConfig
     * Configuration string
     * @return List of resolutions to check
     */
    internal fun getResolutions(resolutionsConfig: String): List<Fraction> {
        val resolutions = ArrayList<Fraction>()
        for (res in resolutionsConfig.split(" *, *".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (StringUtils.isNotBlank(res)) {
                resolutions.add(Fraction.getFraction(res.replace('x', '/')))
            }
        }
        return resolutions
    }

    companion object {

        /** Configuration key for the "flavor" of the tracks to use as a source input  */
        internal val OPT_SOURCE_FLAVOR = "source-flavor"

        /** Configuration key for video resolutions to check  */
        internal val OPT_VAR_PREFIX = "var:"

        /** Configuration key for value to set  */
        internal val OPT_VAL_PREFIX = "val:"

        /** The logging facility  */
        private val logger = LoggerFactory
                .getLogger(ProbeResolutionWorkflowOperationHandler::class.java)

        /** Create a name for a workflow variable from a flavor  */
        private fun toVariableName(flavor: MediaPackageElementFlavor): String {
            return flavor.type + "_" + flavor.subtype + "_"
        }
    }
}
