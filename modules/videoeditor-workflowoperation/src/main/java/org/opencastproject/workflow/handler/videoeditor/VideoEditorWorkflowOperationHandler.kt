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

import java.lang.String.format

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement
import org.opencastproject.util.NotFoundException
import org.opencastproject.videoeditor.api.ProcessFailedException
import org.opencastproject.videoeditor.api.VideoEditorService
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.util.Collections

import javax.xml.bind.JAXBException

class VideoEditorWorkflowOperationHandler : ResumableWorkflowOperationHandlerBase() {

    /**
     * The SMIL service to modify SMIL files.
     */
    private var smilService: SmilService? = null
    /**
     * The VideoEditor service to edit files.
     */
    private var videoEditorService: VideoEditorService? = null
    /**
     * The workspace.
     */
    private var workspace: Workspace? = null

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        setHoldActionTitle("Review / VideoEdit")
        registerHoldStateUserInterface(HOLD_UI_PATH)
        logger.info("Registering videoEditor hold state ui from classpath {}", HOLD_UI_PATH)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mp = workflowInstance.mediaPackage
        logger.info("Start editor workflow for mediapackage {}", mp.identifier.compact())

        // Get configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        val smilFlavorsProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(SMIL_FLAVORS_PROPERTY))
                ?: throw WorkflowOperationException(format("Required configuration property %s not set", SMIL_FLAVORS_PROPERTY))
        val targetSmilFlavorProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY))
                ?: throw WorkflowOperationException(
                        format("Required configuration property %s not set", TARGET_SMIL_FLAVOR_PROPERTY))
        val previewTrackFlavorsProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(PREVIEW_FLAVORS_PROPERTY))
        if (previewTrackFlavorsProperty == null) {
            logger.info("Configuration property '{}' not set, use preview tracks from SMIL catalog",
                    PREVIEW_FLAVORS_PROPERTY)
        }

        /* false if it is missing */
        val skipProcessing = BooleanUtils
                .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY))
        /* skip smil processing (done in another operation) so target_flavors do not matter */
        if (!skipProcessing && StringUtils
                        .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY)) == null) {
            throw WorkflowOperationException(
                    String.format("Required configuration property %s not set", TARGET_FLAVOR_SUBTYPE_PROPERTY))
        }

        val interactive = BooleanUtils.toBoolean(worflowOperationInstance.getConfiguration(INTERACTIVE_PROPERTY))

        // Check at least one SMIL catalog exists
        val elementSelector = SimpleElementSelector()
        for (flavor in asList(smilFlavorsProperty)) {
            elementSelector.addFlavor(flavor)
        }
        val smilCatalogs = elementSelector.select(mp, false)
        val mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()

        if (smilCatalogs.isEmpty()) {

            // There is nothing to do, skip the operation
            // however, we still need the smil file to be produced for the entire video as one clip if
            // skipProcessing is TRUE
            if (!interactive && !skipProcessing) {
                logger.info("Skipping cutting operation since no edit decision list is available")
                return skip(workflowInstance, context)
            }

            // Without SMIL catalogs and without preview tracks, there is nothing we can do
            if (previewTrackFlavorsProperty == null) {
                throw WorkflowOperationException(
                        format("No SMIL catalogs with flavor %s nor preview files with flavor %s found in mediapackage %s",
                                smilFlavorsProperty, previewTrackFlavorsProperty, mp.identifier.compact()))
            }

            // Based on the preview tracks, create new and empty SMIL catalog
            val trackSelector = TrackSelector()
            for (flavor in asList(previewTrackFlavorsProperty)) {
                trackSelector.addFlavor(flavor)
            }
            val previewTracks = trackSelector.select(mp, false)
            if (previewTracks.isEmpty()) {
                throw WorkflowOperationException(format("No preview tracks found in mediapackage %s with flavor %s",
                        mp.identifier.compact(), previewTrackFlavorsProperty))
            }
            val previewTracksArr = previewTracks.toTypedArray()
            val smilFlavor = MediaPackageElementFlavor.parseFlavor(smilFlavorsProperty)

            for (previewTrack in previewTracks) {
                try {
                    var smilResponse = smilService!!.createNewSmil(mp)
                    smilResponse = smilService!!.addParallel(smilResponse.smil)
                    smilResponse = smilService!!.addClips(smilResponse.smil, smilResponse.entity.id,
                            previewTracksArr, 0L, previewTracksArr[0].duration!!)
                    val smil = smilResponse.smil

                    var `is`: InputStream? = null
                    try {
                        // Put new SMIL into workspace
                        `is` = IOUtils.toInputStream(smil.toXML(), "UTF-8")
                        val smilURI = workspace!!.put(mp.identifier.compact(), smil.id, SMIL_FILE_NAME, `is`)
                        var trackSmilFlavor = previewTrack.flavor
                        if ("*" != smilFlavor.type) {
                            trackSmilFlavor = MediaPackageElementFlavor(smilFlavor.type!!, trackSmilFlavor.subtype!!)
                        }
                        if ("*" != smilFlavor.subtype) {
                            trackSmilFlavor = MediaPackageElementFlavor(trackSmilFlavor.type!!, smilFlavor.subtype!!)
                        }
                        val catalog = mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
                                trackSmilFlavor) as Catalog
                        catalog.identifier = smil.id
                        mp.add(catalog)
                    } finally {
                        IOUtils.closeQuietly(`is`)
                    }
                } catch (ex: Exception) {
                    throw WorkflowOperationException(
                            format("Failed to create SMIL catalog for mediapackage %s", mp.identifier.compact()), ex)
                }

            }
        }

        // Check target SMIL catalog exists
        val targetSmilFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty)
        val targetSmilCatalogs = mp.getCatalogs(targetSmilFlavor)
        if (targetSmilCatalogs == null || targetSmilCatalogs.size == 0) {

            if (!interactive && !skipProcessing)
            // create a smil even if not interactive
                return skip(workflowInstance, context)

            // Create new empty SMIL to fill it from editor UI
            try {
                val smilResponse = smilService!!.createNewSmil(mp)
                val smil = smilResponse.smil

                var `is`: InputStream? = null
                try {
                    // Put new SMIL into workspace
                    `is` = IOUtils.toInputStream(smil.toXML(), "UTF-8")
                    val smilURI = workspace!!.put(mp.identifier.compact(), smil.id, SMIL_FILE_NAME, `is`)
                    val catalog = mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
                            targetSmilFlavor) as Catalog
                    catalog.identifier = smil.id
                    mp.add(catalog)
                } finally {
                    IOUtils.closeQuietly(`is`)
                }
            } catch (ex: Exception) {
                throw WorkflowOperationException(
                        format("Failed to create an initial empty SMIL catalog for mediapackage %s",
                                mp.identifier.compact()),
                        ex)
            }

            if (!interactive)
            // deferred skip, keep empty smil
                return skip(workflowInstance, context)
            logger.info("Holding for video edit...")
            return createResult(mp, Action.PAUSE)
        } else {
            logger.debug("Move on, SMIL catalog ({}) already exists for media package '{}'", targetSmilFlavor, mp)
            return resume(workflowInstance, context, emptyMap())
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.skip
     */
    @Throws(WorkflowOperationException::class)
    override fun skip(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        // If we do not hold for trim, we still need to put tracks in the mediapackage with the target flavor
        val mp = workflowInstance.mediaPackage
        logger.info("Skip video editor operation for mediapackage {}", mp.identifier.compact())

        // Get configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        var sourceTrackFlavorsProperty: String? = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(SKIPPED_FLAVORS_PROPERTY))
        if (sourceTrackFlavorsProperty == null || sourceTrackFlavorsProperty.isEmpty()) {
            logger.info("\"{}\" option not set, use value of \"{}\"", SKIPPED_FLAVORS_PROPERTY, SOURCE_FLAVORS_PROPERTY)
            sourceTrackFlavorsProperty = StringUtils
                    .trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY))
            if (sourceTrackFlavorsProperty == null) {
                throw WorkflowOperationException(
                        format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY))
            }
        }
        // processing will operate directly on source tracks as named in smil file
        val skipProcessing = BooleanUtils
                .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY))
        if (skipProcessing)
            return createResult(mp, Action.SKIP)
        // If not skipProcessing (set it up for process-smil), then clone and tag to target
        val targetFlavorSubTypeProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY))
                ?: throw WorkflowOperationException(
                        format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY))

        // Get source tracks
        val trackSelector = TrackSelector()
        for (flavor in asList(sourceTrackFlavorsProperty)) {
            trackSelector.addFlavor(flavor)
        }
        val sourceTracks = trackSelector.select(mp, false)

        for (sourceTrack in sourceTracks) {
            // Set target track flavor
            val clonedTrack = sourceTrack.clone() as Track
            clonedTrack.identifier = null
            // Use the same URI as the original
            clonedTrack.setURI(sourceTrack.getURI())
            clonedTrack
                    .flavor = MediaPackageElementFlavor(sourceTrack.flavor.type!!, targetFlavorSubTypeProperty)
            mp.addDerived(clonedTrack, sourceTrack)
        }

        return createResult(mp, Action.SKIP)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler.resume
     */
    @Throws(WorkflowOperationException::class)
    override fun resume(workflowInstance: WorkflowInstance, context: JobContext,
                        properties: Map<String, String>): WorkflowOperationResult {

        val mp = workflowInstance.mediaPackage
        logger.info("Resume video editor operation for mediapackage {}", mp.identifier.compact())

        // Get configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        val sourceTrackFlavorsProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(SOURCE_FLAVORS_PROPERTY))
                ?: throw WorkflowOperationException(
                        format("Required configuration property %s not set.", SOURCE_FLAVORS_PROPERTY))
        val targetSmilFlavorProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(TARGET_SMIL_FLAVOR_PROPERTY))
                ?: throw WorkflowOperationException(
                        format("Required configuration property %s not set.", TARGET_SMIL_FLAVOR_PROPERTY))
        val targetFlavorSybTypeProperty = StringUtils
                .trimToNull(worflowOperationInstance.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY))

        // if set, smil processing is done by another operation
        val skipProcessing = BooleanUtils
                .toBoolean(worflowOperationInstance.getConfiguration(SKIP_PROCESSING_PROPERTY))
        if (!skipProcessing) {
            if (targetFlavorSybTypeProperty == null) {
                throw WorkflowOperationException(
                        format("Required configuration property %s not set.", TARGET_FLAVOR_SUBTYPE_PROPERTY))
            }
        }

        val skipIfNoTrim = BooleanUtils.toBoolean(worflowOperationInstance.getConfiguration(SKIP_NOT_TRIMMED_PROPERTY))

        // Get source tracks
        val trackSelector = TrackSelector()
        for (flavor in asList(sourceTrackFlavorsProperty)) {
            trackSelector.addFlavor(flavor)
        }
        val sourceTracks = trackSelector.select(mp, false)
        if (sourceTracks.isEmpty()) {
            throw WorkflowOperationException(format("No source tracks found in mediapacksge %s with flavors %s.",
                    mp.identifier.compact(), sourceTrackFlavorsProperty))
        }

        // Get SMIL file
        val smilTargetFlavor = MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty)
        val smilCatalogs = mp.getCatalogs(smilTargetFlavor)
        if (smilCatalogs == null || smilCatalogs.size == 0) {
            throw WorkflowOperationException(format("No SMIL catalog found in mediapackage %s with flavor %s.",
                    mp.identifier.compact(), targetSmilFlavorProperty))
        }

        var smilFile: File? = null
        var smil: Smil? = null
        try {
            smilFile = workspace!!.get(smilCatalogs[0].getURI())
            smil = smilService!!.fromXml(smilFile!!).smil
            smil = replaceAllTracksWith(smil, sourceTracks.toTypedArray())

            var `is`: InputStream? = null
            try {
                `is` = IOUtils.toInputStream(smil.toXML(), "UTF-8")
                // Remove old SMIL
                workspace!!.delete(mp.identifier.compact(), smilCatalogs[0].identifier)
                mp.remove(smilCatalogs[0])
                // put modified SMIL into workspace
                val newSmilUri = workspace!!.put(mp.identifier.compact(), smil.id, SMIL_FILE_NAME, `is`)
                val catalog = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                        .elementFromURI(newSmilUri, MediaPackageElement.Type.Catalog, smilCatalogs[0].flavor) as Catalog
                catalog.identifier = smil.id
                mp.add(catalog)
            } catch (ex: Exception) {
                throw WorkflowOperationException(ex)
            } finally {
                IOUtils.closeQuietly(`is`)
            }

        } catch (ex: NotFoundException) {
            throw WorkflowOperationException(format("Failed to get SMIL catalog %s from mediapackage %s.",
                    smilCatalogs[0].identifier, mp.identifier.compact()), ex)
        } catch (ex: IOException) {
            throw WorkflowOperationException(format("Can't open SMIL catalog %s from mediapackage %s.",
                    smilCatalogs[0].identifier, mp.identifier.compact()), ex)
        } catch (ex: SmilException) {
            throw WorkflowOperationException(ex)
        }

        // If skipProcessing, The track is processed by a separate operation which takes the SMIL file and encode directly
        // to delivery format
        if (skipProcessing) {
            logger.info("VideoEdit workflow {} finished - smil file is {}", workflowInstance.id, smil.id)
            return createResult(mp, Action.CONTINUE)
        }
        // create video edit jobs and run them
        if (skipIfNoTrim) {
            // We need to check whether or not there are trimming points defined
            // TODO The SmilService implementation does not do any filtering or optimizations for us. We need to
            // process the SMIL file ourselves. The SmilService should be something more than a bunch of classes encapsulating
            // data types which provide no extra functionality (e.g. we shouldn't have to check the SMIL structure ourselves)

            // We should not modify the SMIL file as we traverse through its elements, so we make a copy and modify it instead
            try {
                var filteredSmil = smilService!!.fromXml(smil.toXML()).smil
                for (element in smil.body.mediaElements) {
                    // body should contain par elements
                    if (element.isContainer) {
                        val container = element as SmilMediaContainer
                        if (SmilMediaContainer.ContainerType.PAR === container.containerType) {
                            continue
                        }
                    }
                    filteredSmil = smilService!!.removeSmilElement(filteredSmil, element.id).smil
                }

                // Return an empty job list if not PAR components (i.e. trimming points) are defined, or if there is just
                // one that takes the whole video size
                when (filteredSmil.body.mediaElements.size) {
                    0 -> {
                        logger.info("Skipping SMIL job generation for mediapackage '{}', " + "because the SMIL does not define any trimming points", mp.identifier)
                        return skip(workflowInstance, context)
                    }

                    1 -> {
                        // If the whole duration was not defined in the mediapackage, we cannot tell whether or not this PAR
                        // component represents the whole duration or not, therefore we don't bother to try
                        if (mp.duration < 0)
                            break

                        val parElement = filteredSmil.body.mediaElements[0] as SmilMediaContainer
                        var skip = true
                        for (elementChild in parElement.elements) {
                            if (!elementChild.isContainer) {
                                val media = elementChild as SmilMediaElement
                                // Compare begin and endpoints
                                // If they don't represent the whole length, then we break --we have a trimming point
                                if (media.clipBeginMS != 0L || media.clipEndMS != mp.duration) {
                                    skip = false
                                    break
                                }
                            }
                        }

                        if (skip) {
                            logger.info("Skipping SMIL job generation for mediapackage '{}', "
                                    + "because the trimming points in the SMIL correspond "
                                    + "to the beginning and the end of the video", mp.identifier)
                            return skip(workflowInstance, context)
                        }
                    }

                    else -> {
                    }
                }
            } catch (e: MalformedURLException) {
                logger.warn("Error parsing input SMIL to determine if it has trimpoints. " + "We will assume it does and go on creating jobs.")
            } catch (e: SmilException) {
                logger.warn("Error parsing input SMIL to determine if it has trimpoints. " + "We will assume it does and go on creating jobs.")
            } catch (e: JAXBException) {
                logger.warn("Error parsing input SMIL to determine if it has trimpoints. " + "We will assume it does and go on creating jobs.")
            } catch (e: SAXException) {
                logger.warn("Error parsing input SMIL to determine if it has trimpoints. " + "We will assume it does and go on creating jobs.")
            }

        }

        // Create video edit jobs and run them
        var jobs: List<Job>? = null

        try {
            logger.info("Create processing jobs for SMIL file: {}", smilCatalogs[0].identifier)
            jobs = videoEditorService!!.processSmil(smil)
            if (!waitForStatus(*jobs.toTypedArray()).isSuccess) {
                throw WorkflowOperationException(
                        format("Processing SMIL file failed: %s", smilCatalogs[0].identifier))
            }
            logger.info("Finished processing of SMIL file: {}", smilCatalogs[0].identifier)
        } catch (ex: ProcessFailedException) {
            throw WorkflowOperationException(
                    format("Finished processing of SMIL file: %s", smilCatalogs[0].identifier), ex)
        }

        // Move edited tracks to work location and set target flavor
        var editedTrack: Track? = null
        var mpAdded = false
        for (job in jobs) {
            try {
                editedTrack = MediaPackageElementParser.getFromXml(job.payload) as Track
                val editedTrackFlavor = editedTrack.flavor
                editedTrack.flavor = MediaPackageElementFlavor(editedTrackFlavor.type!!, targetFlavorSybTypeProperty!!)
                val editedTrackNewUri = workspace!!.moveTo(editedTrack.getURI(), mp.identifier.compact(),
                        editedTrack.identifier, FilenameUtils.getName(editedTrack.getURI().toString()))
                editedTrack.setURI(editedTrackNewUri)
                for (track in sourceTracks) {
                    if (track.flavor.type == editedTrackFlavor.type) {
                        mp.addDerived(editedTrack, track)
                        mpAdded = true
                        break
                    }
                }

                if (!mpAdded) {
                    mp.add(editedTrack)
                }

            } catch (ex: MediaPackageException) {
                throw WorkflowOperationException("Failed to get information about the edited track(s)", ex)
            } catch (ex: NotFoundException) {
                throw WorkflowOperationException("Moving edited track to work location failed.", ex)
            } catch (ex: IOException) {
                throw WorkflowOperationException("Moving edited track to work location failed.", ex)
            } catch (ex: IllegalArgumentException) {
                throw WorkflowOperationException("Moving edited track to work location failed.", ex)
            } catch (ex: Exception) {
                throw WorkflowOperationException(ex)
            }

        }

        logger.info("VideoEdit workflow {} finished", workflowInstance.id)
        return createResult(mp, Action.CONTINUE)
    }

    @Throws(SmilException::class)
    protected fun replaceAllTracksWith(smil: Smil, otherTracks: Array<Track>): Smil {
        var smilResponse: SmilResponse
        try {
            // copy SMIL to work with
            smilResponse = smilService!!.fromXml(smil.toXML())
        } catch (ex: Exception) {
            throw SmilException("Can not parse SMIL files.")
        }

        var start: Long
        var end: Long
        var hasElements = false // Check for missing smil so the process will fail early if no tracks found
        // iterate over all elements inside SMIL body
        for (elem in smil.body.mediaElements) {
            start = -1L
            end = -1L
            // body should contain par elements (container)
            if (elem.isContainer) {
                // iterate over all elements in container
                for (child in (elem as SmilMediaContainer).elements) {
                    // second depth should contain media elements like audio or video
                    if (!child.isContainer && child is SmilMediaElement) {
                        start = child.clipBeginMS
                        end = child.clipEndMS
                        // remove it
                        smilResponse = smilService!!.removeSmilElement(smilResponse.smil, child.id)
                        hasElements = true
                    }
                }
                if (start != -1L && end != -1L) {
                    // add the new tracks inside
                    smilResponse = smilService!!.addClips(smilResponse.smil, elem.id, otherTracks, start, end - start)
                }
            } else if (elem is SmilMediaElement) {
                throw SmilException("Media elements inside SMIL body are not supported yet.")
            }
        }
        if (!hasElements) {
            throw SmilException("Smil does not define any elements")
        }
        return smilResponse.smil
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    fun setVideoEditorService(editor: VideoEditorService) {
        videoEditorService = editor
    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VideoEditorWorkflowOperationHandler::class.java)

        /** Path to the hold ui resources  */
        private val HOLD_UI_PATH = "/ui/operation/editor/index.html"

        /** Name of the configuration option that provides the source flavors we use for processing.  */
        private val SOURCE_FLAVORS_PROPERTY = "source-flavors"

        /** Name of the configuration option that provides the preview flavors we use as preview.  */
        private val PREVIEW_FLAVORS_PROPERTY = "preview-flavors"

        /** Bypasses Videoeditor's encoding operation but keep the raw smil for later processing  */
        private val SKIP_PROCESSING_PROPERTY = "skip-processing"

        /** Name of the configuration option that provides the source flavors on skipped videoeditor operation.  */
        private val SKIPPED_FLAVORS_PROPERTY = "skipped-flavors"

        /** Name of the configuration option that provides the SMIL flavor as input.  */
        private val SMIL_FLAVORS_PROPERTY = "smil-flavors"

        /** Name of the configuration option that provides the SMIL flavor as input.  */
        private val TARGET_SMIL_FLAVOR_PROPERTY = "target-smil-flavor"

        /** Name of the configuration that provides the target flavor subtype for encoded media tracks.  */
        private val TARGET_FLAVOR_SUBTYPE_PROPERTY = "target-flavor-subtype"

        /** Name of the configuration that provides the interactive flag  */
        private val INTERACTIVE_PROPERTY = "interactive"

        /** Name of the configuration that provides the SMIL file name  */
        private val SMIL_FILE_NAME = "smil.smil"

        /**
         * Name of the configuration that controls whether or not to process the input video(s) even when there are no
         * trimming points
         */
        private val SKIP_NOT_TRIMMED_PROPERTY = "skip-if-not-trimmed"
    }
}
