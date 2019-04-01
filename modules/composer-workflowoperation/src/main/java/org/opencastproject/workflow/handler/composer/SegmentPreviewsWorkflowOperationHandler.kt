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

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageReference
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.metadata.mpeg7.MediaTimePoint
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService
import org.opencastproject.metadata.mpeg7.Segment
import org.opencastproject.metadata.mpeg7.TemporalDecomposition
import org.opencastproject.metadata.mpeg7.Video
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.HashSet
import java.util.LinkedList
import java.util.concurrent.ExecutionException

/**
 * The workflow definition for creating segment preview images from an segment mpeg-7 catalog.
 */
class SegmentPreviewsWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The mpeg7 catalog service  */
    private var mpeg7CatalogService: Mpeg7CatalogService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the composer service
     */
    protected fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param catalogService
     * the catalog service
     */
    protected fun setMpeg7CatalogService(catalogService: Mpeg7CatalogService) {
        mpeg7CatalogService = catalogService
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
        logger.debug("Running segments preview workflow operation on {}", workflowInstance)

        // Check if there is an mpeg-7 catalog containing video segments
        val src = workflowInstance.mediaPackage.clone() as MediaPackage
        val segmentCatalogs = src.getCatalogs(MediaPackageElements.SEGMENTS)
        if (segmentCatalogs.size == 0) {
            logger.info("Media package {} does not contain segment information", src)
            return createResult(Action.CONTINUE)
        }

        // Create the images
        try {
            return createPreviews(src, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
     *
     * @param mediaPackage
     * @param properties
     * @return the operation result containing the updated mediapackage
     * @throws EncoderException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     * @throws NotFoundException
     * @throws WorkflowOperationException
     */
    @Throws(EncoderException::class, InterruptedException::class, ExecutionException::class, NotFoundException::class, MediaPackageException::class, IOException::class, WorkflowOperationException::class)
    private fun createPreviews(mediaPackage: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        var totalTimeInQueue: Long = 0

        // Read the configuration properties
        val sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"))
        val sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"))
        val targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"))
        val targetImageFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"))
        val encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"))
        val referenceFlavor = StringUtils.trimToNull(operation.getConfiguration("reference-flavor"))
        val referenceTags = StringUtils.trimToNull(operation.getConfiguration("reference-tags"))

        // Find the encoding profile
        val profile = composerService!!.getProfile(encodingProfileName)
                ?: throw IllegalStateException("Encoding profile '$encodingProfileName' was not found")

        val sourceTagSet = asList(sourceTags)

        // Select the tracks based on the tags and flavors
        val videoTrackSet = HashSet<Track>()
        for (track in mediaPackage.getTracksByTags(sourceTagSet)) {
            if (sourceVideoFlavor == null || track.flavor != null && sourceVideoFlavor == track.flavor.toString()) {
                if (!track.hasVideo())
                    continue
                videoTrackSet.add(track)
            }
        }

        if (videoTrackSet.size == 0) {
            logger.debug("Mediapackage {} has no suitable tracks to extract images based on tags {} and flavor {}",
                    mediaPackage, sourceTags, sourceVideoFlavor)
            return createResult(mediaPackage, Action.CONTINUE)
        } else {

            // Determine the tagset for the reference
            val referenceTagSet = asList(referenceTags)

            // Determine the reference master
            for (t in videoTrackSet) {

                // Try to load the segments catalog
                val trackReference = MediaPackageReferenceImpl(t)
                val segmentCatalogs = mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS, trackReference)
                var mpeg7: Mpeg7Catalog? = null
                if (segmentCatalogs.size > 0) {
                    mpeg7 = loadMpeg7Catalog(segmentCatalogs[0])
                    if (segmentCatalogs.size > 1)
                        logger.warn("More than one segments catalog found for track {}. Resuming with the first one ({})", t,
                                mpeg7)
                } else {
                    logger.debug("No segments catalog found for track {}", t)
                    continue
                }

                // Check the catalog's consistency
                if (mpeg7.videoContent() == null || mpeg7.videoContent().next() == null) {
                    logger.info("Segments catalog {} contains no video content", mpeg7)
                    continue
                }

                val videoContent = mpeg7.videoContent().next()
                val decomposition = videoContent.temporalDecomposition

                // Are there any segments?
                if (decomposition == null || !decomposition.hasSegments()) {
                    logger.info("Segments catalog {} contains no video content", mpeg7)
                    continue
                }

                // Is a derived track with the configured reference flavor available?
                val referenceMaster = getReferenceMaster(mediaPackage, t, referenceFlavor, referenceTagSet)

                // Create the preview images according to the mpeg7 segments
                if (t.hasVideo() && mpeg7 != null) {

                    val segmentIterator = decomposition.segments()

                    val timePointList = LinkedList<MediaTimePoint>()
                    while (segmentIterator.hasNext()) {
                        val segment = segmentIterator.next()
                        val tp = segment.mediaTime.mediaTimePoint
                        timePointList.add(tp)
                    }

                    // convert to time array
                    val timeArray = DoubleArray(timePointList.size)
                    for (i in timePointList.indices)
                        timeArray[i] = timePointList[i].timeInMilliseconds.toDouble() / 1000

                    var job = composerService!!.image(t, profile.identifier, *timeArray)
                    if (!waitForStatus(job).isSuccess) {
                        throw WorkflowOperationException("Extracting preview image from $t failed")
                    }

                    // Get the latest copy
                    try {
                        job = serviceRegistry.getJob(job.id)
                    } catch (e: ServiceRegistryException) {
                        throw WorkflowOperationException(e)
                    }

                    // add this receipt's queue time to the total
                    totalTimeInQueue += job.queueTime!!

                    val composedImages = MediaPackageElementParser
                            .getArrayFromXml(job.payload)
                    val it = timePointList.iterator()

                    for (element in composedImages) {
                        val composedImage = element as Attachment
                                ?: throw IllegalStateException("Unable to compose image")

                        // Add the flavor, either from the operation configuration or from the composer
                        if (targetImageFlavor != null) {
                            composedImage.flavor = MediaPackageElementFlavor.parseFlavor(targetImageFlavor)
                            logger.debug("Preview image has flavor '{}'", composedImage.flavor)
                        }

                        // Set the mimetype
                        try {
                            composedImage.mimeType = MimeTypes.fromURI(composedImage.getURI())
                        } catch (e: UnknownFileTypeException) {
                            logger.warn("Mime type unknown for file {}. Setting none.", composedImage.getURI(), e)
                        }

                        // Add tags
                        for (tag in asList(targetImageTags)) {
                            logger.trace("Tagging image with '{}'", tag)
                            composedImage.addTag(tag)
                        }

                        // Refer to the original track including a timestamp
                        val ref = MediaPackageReferenceImpl(referenceMaster)
                        ref.setProperty("time", it.next().toString())
                        composedImage.reference = ref

                        // store new image in the mediaPackage
                        mediaPackage.add(composedImage)
                        val fileName = getFileNameFromElements(t, composedImage)
                        composedImage.setURI(workspace!!.moveTo(composedImage.getURI(), mediaPackage.identifier.toString(),
                                composedImage.identifier, fileName))
                    }
                }
            }
        }

        return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
    }

    /**
     * Returns the track that is used as the reference for the segment previews. It is either identified by flavor and tag
     * set and being derived from `t` or `t` itself.
     *
     * @param mediaPackage
     * the media package
     * @param t
     * the source track for the images
     * @param referenceFlavor
     * the required flavor
     * @param referenceTagSet
     * the required tagset
     * @return the reference master
     */
    private fun getReferenceMaster(mediaPackage: MediaPackage, t: Track, referenceFlavor: String?,
                                   referenceTagSet: Collection<String>): MediaPackageElement {
        var referenceMaster: MediaPackageElement = t
        if (referenceFlavor != null) {
            val flavor = MediaPackageElementFlavor.parseFlavor(referenceFlavor)
            // Find a track with the given flavor that is (indirectly) derived from t?
            locateReferenceMaster@ for (e in mediaPackage.getTracks(flavor)) {
                var ref: MediaPackageReference? = e.reference
                while (ref != null) {
                    val tr = mediaPackage.getElementByReference(ref) ?: break@locateReferenceMaster
                    if (tr == t) {
                        var matches = true
                        for (tag in referenceTagSet) {
                            if (!e.containsTag(tag))
                                matches = false
                        }
                        if (matches) {
                            referenceMaster = e
                            break@locateReferenceMaster
                        }
                    }
                    ref = tr.reference
                }
            }
        }
        return referenceMaster
    }

    /**
     * Loads an mpeg7 catalog from a mediapackage's catalog reference
     *
     * @param catalog
     * the mediapackage's reference to this catalog
     * @return the mpeg7
     * @throws IOException
     * if there is a problem loading or parsing the mpeg7 object
     */
    @Throws(IOException::class)
    protected fun loadMpeg7Catalog(catalog: Catalog): Mpeg7Catalog {
        var `in`: InputStream? = null
        try {
            val f = workspace!!.get(catalog.getURI())
            `in` = FileInputStream(f)
            return mpeg7CatalogService!!.load(`in`)
        } catch (e: NotFoundException) {
            throw IOException("Unable to open catalog " + catalog + ": " + e.message)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(SegmentPreviewsWorkflowOperationHandler::class.java)
    }

}
