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

package org.opencastproject.workflow.handler.videoeditor

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.api.SilenceDetectionService
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.net.URI

/**
 * workflowoperationhandler for silencedetection executes the silencedetection and adds a SMIL document to the
 * mediapackage containing the cutting points
 */
class SilenceDetectionWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The silence detection service.  */
    private var detetionService: SilenceDetectionService? = null

    /** The smil service for smil parsing.  */
    private var smilService: SmilService? = null

    /** The workspace.  */
    private var workspace: Workspace? = null

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mp = workflowInstance.mediaPackage
        logger.debug("Start silence detection workflow operation for mediapackage {}", mp.identifier.compact())

        val sourceFlavors = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                SOURCE_FLAVORS_PROPERTY))
        val sourceFlavor = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                SOURCE_FLAVOR_PROPERTY))
        val smilFlavorSubType = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                SMIL_FLAVOR_SUBTYPE_PROPERTY))
        val smilTargetFlavorString = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                SMIL_TARGET_FLAVOR_PROPERTY))

        var smilTargetFlavor: MediaPackageElementFlavor? = null
        if (smilTargetFlavorString != null)
            smilTargetFlavor = MediaPackageElementFlavor.parseFlavor(smilTargetFlavorString)

        if (sourceFlavor == null && sourceFlavors == null) {
            throw WorkflowOperationException(String.format("No %s or %s have been specified", SOURCE_FLAVOR_PROPERTY,
                    SOURCE_FLAVORS_PROPERTY))
        }
        if (smilFlavorSubType == null && smilTargetFlavor == null) {
            throw WorkflowOperationException(String.format("No %s or %s have been specified",
                    SMIL_FLAVOR_SUBTYPE_PROPERTY, SMIL_TARGET_FLAVOR_PROPERTY))
        }
        if (sourceFlavors != null && smilTargetFlavor != null) {
            throw WorkflowOperationException(String.format("Can't use %s and %s together", SOURCE_FLAVORS_PROPERTY,
                    SMIL_TARGET_FLAVOR_PROPERTY))
        }

        val finalSourceFlavors: String?
        if (smilTargetFlavor != null) {
            finalSourceFlavors = sourceFlavor
        } else {
            finalSourceFlavors = sourceFlavors
        }

        var referenceTracksFlavor: String? = StringUtils.trimToNull(workflowInstance.currentOperation.getConfiguration(
                REFERENCE_TRACKS_FLAVOR_PROPERTY))
        if (referenceTracksFlavor == null)
            referenceTracksFlavor = finalSourceFlavors

        var trackSelector = TrackSelector()
        for (flavor in asList(finalSourceFlavors)) {
            trackSelector.addFlavor(flavor)
        }
        val sourceTracks = trackSelector.select(mp, false)
        if (sourceTracks.isEmpty()) {
            logger.info("No source tracks found, skip silence detection")
            return createResult(mp, Action.SKIP)
        }

        trackSelector = TrackSelector()
        for (flavor in asList(referenceTracksFlavor)) {
            trackSelector.addFlavor(flavor)
        }
        val referenceTracks = trackSelector.select(mp, false)
        if (referenceTracks.isEmpty()) {
            // REFERENCE_TRACKS_FLAVOR_PROPERTY was set to wrong value
            throw WorkflowOperationException(String.format("No tracks found filtered by flavor(s) '%s'",
                    referenceTracksFlavor))
        }
        val mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()

        for (sourceTrack in sourceTracks) {
            // Skip over track with no audio stream
            if (!sourceTrack.hasAudio()) {
                logger.info("Skipping silence detection of track {} since it has no audio", sourceTrack)
                continue
            }
            logger.info("Executing silence detection on track {}", sourceTrack.identifier)
            try {
                val detectionJob = detetionService!!.detect(sourceTrack,
                        referenceTracks.toTypedArray())
                if (!waitForStatus(detectionJob).isSuccess) {
                    throw WorkflowOperationException("Silence Detection failed")
                }
                val smil = smilService!!.fromXml(detectionJob.payload).smil
                var `is`: InputStream? = null
                try {
                    `is` = IOUtils.toInputStream(smil.toXML(), "UTF-8")
                    val smilURI = workspace!!.put(mp.identifier.compact(), smil.id, TARGET_FILE_NAME, `is`)
                    var smilFlavor = smilTargetFlavor
                    if (smilFlavor == null)
                        smilFlavor = MediaPackageElementFlavor(sourceTrack.flavor.type!!, smilFlavorSubType!!)
                    val catalog = mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog, smilFlavor) as Catalog
                    catalog.identifier = smil.id
                    mp.add(catalog)
                } catch (ex: Exception) {
                    throw WorkflowOperationException(String.format(
                            "Failed to put smil into workspace. Silence detection for track %s failed",
                            sourceTrack.identifier), ex)
                } finally {
                    IOUtils.closeQuietly(`is`)
                }

                logger.info("Finished silence detection on track {}", sourceTrack.identifier)
            } catch (ex: SilenceDetectionFailedException) {
                throw WorkflowOperationException(String.format("Failed to create silence detection job for track %s",
                        sourceTrack.identifier))
            } catch (ex: SmilException) {
                throw WorkflowOperationException(String.format(
                        "Failed to get smil from silence detection job for track %s", sourceTrack.identifier))
            }

        }
        logger.debug("Finished silence detection workflow operation for mediapackage {}", mp.identifier.compact())
        return createResult(mp, Action.CONTINUE)
    }

    public override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.info("Registering silence detection workflow operation handler")
    }

    fun setDetectionService(detectionService: SilenceDetectionService) {
        this.detetionService = detectionService
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** Logger  */
        private val logger = LoggerFactory.getLogger(SilenceDetectionWorkflowOperationHandler::class.java)

        /** Name of the configuration option that provides the source flavors we are looking for.  */
        private val SOURCE_FLAVORS_PROPERTY = "source-flavors"

        /** Name of the configuration option that provides the source flavor we are looking for.  */
        private val SOURCE_FLAVOR_PROPERTY = "source-flavor"

        /** Name of the configuration option that provides the smil flavor subtype we will produce.  */
        private val SMIL_FLAVOR_SUBTYPE_PROPERTY = "smil-flavor-subtype"

        /** Name of the configuration option that provides the smil target flavor we will produce.  */
        private val SMIL_TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Name of the configuration option for track flavors to reference in generated smil.  */
        private val REFERENCE_TRACKS_FLAVOR_PROPERTY = "reference-tracks-flavor"

        /** Name of the configuration option that provides the smil file name  */
        private val TARGET_FILE_NAME = "smil.smil"
    }
}
