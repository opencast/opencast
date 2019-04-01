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

package org.opencastproject.workflow.handler.textanalyzer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageReference
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.metadata.mpeg7.MediaDuration
import org.opencastproject.metadata.mpeg7.MediaRelTimePointImpl
import org.opencastproject.metadata.mpeg7.MediaTime
import org.opencastproject.metadata.mpeg7.MediaTimeImpl
import org.opencastproject.metadata.mpeg7.MediaTimePoint
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService
import org.opencastproject.metadata.mpeg7.Segment
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocator
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocatorImpl
import org.opencastproject.metadata.mpeg7.TemporalDecomposition
import org.opencastproject.metadata.mpeg7.Video
import org.opencastproject.metadata.mpeg7.VideoSegment
import org.opencastproject.metadata.mpeg7.VideoText
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.textanalyzer.api.TextAnalyzerException
import org.opencastproject.textanalyzer.api.TextAnalyzerService
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Dictionary
import java.util.HashMap
import java.util.LinkedList
import kotlin.collections.Map.Entry
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ExecutionException

/**
 * The `TextAnalysisOperationHandler` will take an `MPEG-7` catalog, look for video segments and
 * run a text analysis on the associated still images. The resulting `VideoText` elements will then be added
 * to the segments.
 */
class TextAnalysisWorkflowOperationHandler : AbstractWorkflowOperationHandler(), ManagedService {

    /** The stability threshold  */
    private var stabilityThreshold = DEFAULT_STABILITY_THRESHOLD

    /** The local workspace  */
    private var workspace: Workspace? = null

    /** The mpeg7 catalog service  */
    private var mpeg7CatalogService: Mpeg7CatalogService? = null

    /** The text analysis service  */
    private var analysisService: TextAnalyzerService? = null

    /** The composer service  */
    protected var composer: ComposerService? = null

    /**
     * Callback for the OSGi declarative services configuration that will set the text analysis service.
     *
     * @param analysisService
     * the text analysis service
     */
    protected fun setTextAnalyzer(analysisService: TextAnalyzerService) {
        this.analysisService = analysisService
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
     * Callback for the OSGi declarative services configuration.
     *
     * @param catalogService
     * the catalog service
     */
    protected fun setMpeg7CatalogService(catalogService: Mpeg7CatalogService) {
        this.mpeg7CatalogService = catalogService
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

        try {
            return extractVideoText(src, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    /**
     * Runs the text analysis service on each of the video segments found.
     *
     * @param mediaPackage
     * the original mediapackage
     * @param operation
     * the workflow operation
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NotFoundException
     * @throws WorkflowOperationException
     */
    @Throws(EncoderException::class, InterruptedException::class, ExecutionException::class, IOException::class, NotFoundException::class, MediaPackageException::class, TextAnalyzerException::class, WorkflowOperationException::class, ServiceRegistryException::class)
    protected fun extractVideoText(mediaPackage: MediaPackage,
                                   operation: WorkflowOperationInstance): WorkflowOperationResult {
        var totalTimeInQueue: Long = 0

        val sourceTagSet = asList(operation.getConfiguration("source-tags"))
        val targetTagSet = asList(operation.getConfiguration("target-tags"))

        // Select the catalogs according to the tags
        val catalogs = loadSegmentCatalogs(mediaPackage, operation)

        // Was there at least one matching catalog
        if (catalogs.size == 0) {
            logger.debug("Mediapackage {} has no suitable mpeg-7 catalogs based on tags {} to to run text analysis",
                    mediaPackage, sourceTagSet)
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Loop over all existing segment catalogs
        for ((segmentCatalog, value) in catalogs) {
            val jobs = HashMap<VideoSegment, Job>()
            val images = LinkedList<Attachment>()
            try {
                val catalogRef = segmentCatalog.reference

                // Make sure we can figure out the source track
                if (catalogRef == null) {
                    logger.info("Skipping catalog {} since we can't determine the source track", segmentCatalog)
                } else if (mediaPackage.getElementByReference(catalogRef) == null) {
                    logger.info("Skipping catalog {} since we can't determine the source track", segmentCatalog)
                } else if (mediaPackage.getElementByReference(catalogRef) !is Track) {
                    logger.info("Skipping catalog {} since it's source was not a track", segmentCatalog)
                }

                logger.info("Analyzing mpeg-7 segments catalog {} for text", segmentCatalog)

                // Create a copy that will contain the segments enriched with the video text elements
                val textCatalog = value.clone()
                val sourceTrack = mediaPackage.getTrack(catalogRef.identifier)

                // Load the temporal decomposition (segments)
                val videoContent = textCatalog.videoContent().next()
                val decomposition = videoContent.temporalDecomposition
                val segmentIterator = decomposition.segments()

                // For every segment, try to find the still image and run text analysis on it
                val videoSegments = LinkedList<VideoSegment>()
                while (segmentIterator.hasNext()) {
                    val segment = segmentIterator.next()
                    if (segment is VideoSegment)
                        videoSegments.add(segment)
                }

                // argument array for image extraction
                val times = LongArray(videoSegments.size)

                for (i in videoSegments.indices) {
                    val videoSegment = videoSegments[i]
                    val segmentTimePoint = videoSegment.mediaTime.mediaTimePoint
                    val segmentDuration = videoSegment.mediaTime.mediaDuration

                    // Choose a time
                    var reference: MediaPackageReference? = null
                    if (catalogRef == null)
                        reference = MediaPackageReferenceImpl()
                    else
                        reference = MediaPackageReferenceImpl(catalogRef.type, catalogRef.identifier)
                    reference.setProperty("time", segmentTimePoint.toString())

                    // Have the time for ocr image created. To circumvent problems with slowly building slides, we take the image
                    // that is
                    // almost at the end of the segment, it should contain the most content and is stable as well.
                    val startTimeSeconds = segmentTimePoint.timeInMilliseconds / 1000
                    val durationSeconds = segmentDuration.durationInMilliseconds / 1000
                    times[i] = Math.max(startTimeSeconds + durationSeconds - stabilityThreshold + 1, 0)
                }

                // Have the ocr image(s) created.

                // TODO: Note that the way of having one image extracted after the other is suited for
                // the ffmpeg-based encoder. When switching to other encoding engines such as gstreamer, it might be preferable
                // to pass in all timepoints to the image extraction method at once.
                val extractImageJobs = TreeMap<Long, Job>()

                try {
                    for (time in times) {
                        extractImageJobs[time] = composer!!.image(sourceTrack, IMAGE_EXTRACTION_PROFILE, time)
                    }
                    if (!waitForStatus(*extractImageJobs.values.toTypedArray()).isSuccess)
                        throw WorkflowOperationException("Extracting scene image from $sourceTrack failed")
                    for ((_, value1) in extractImageJobs) {
                        val job = serviceRegistry.getJob(value1.id)
                        val image = MediaPackageElementParser.getFromXml(job.payload) as Attachment
                        images.add(image)
                        totalTimeInQueue += job.queueTime!!
                    }
                } catch (e: EncoderException) {
                    logger.error("Error creating still image(s) from {}", sourceTrack)
                    throw e
                }

                // Run text extraction on each of the images
                val it = videoSegments.iterator()
                for (element in images) {
                    val videoSegment = it.next()
                    jobs[videoSegment] = analysisService!!.extract(element)
                }

                // Wait for all jobs to be finished
                if (!waitForStatus(*jobs.values.toTypedArray()).isSuccess) {
                    throw WorkflowOperationException("Text extraction failed on images from $sourceTrack")
                }

                // Process the text extraction results
                for ((videoSegment, value1) in jobs) {
                    val job = serviceRegistry.getJob(value1.id)
                    totalTimeInQueue += job.queueTime!!

                    val segmentDuration = videoSegment.mediaTime.mediaDuration
                    val catalog = MediaPackageElementParser.getFromXml(job.payload) as Catalog
                    if (catalog == null) {
                        logger.warn("Text analysis did not return a valid mpeg7 for segment {}", videoSegment)
                        continue
                    }
                    val videoTextCatalog = loadMpeg7Catalog(catalog)
                            ?: throw IllegalStateException("Text analysis service did not return a valid mpeg7")

                    // Add the spatiotemporal decompositions from the new catalog to the existing video segments
                    val videoTextContents = videoTextCatalog.videoContent()
                    if (videoTextContents == null || !videoTextContents.hasNext()) {
                        logger.debug("Text analysis was not able to extract any text from {}", job.arguments[0])
                        break
                    }

                    try {
                        val textVideoContent = videoTextContents.next()
                        val textVideoSegment = textVideoContent.temporalDecomposition.segments()
                                .next() as VideoSegment
                        val videoTexts = textVideoSegment.spatioTemporalDecomposition.videoText
                        val std = videoSegment.createSpatioTemporalDecomposition(true, false)
                        for (videoText in videoTexts) {
                            val mediaTime = MediaTimeImpl(MediaRelTimePointImpl(0), segmentDuration)
                            val locator = SpatioTemporalLocatorImpl(mediaTime)
                            videoText.spatioTemporalLocator = locator
                            std.addVideoText(videoText)
                        }
                    } catch (e: Exception) {
                        logger.warn("The mpeg-7 structure returned by the text analyzer is not what is expected", e)
                        continue
                    }

                }

                // Put the catalog into the workspace and add it to the media package
                val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                val catalog = builder.newElement(MediaPackageElement.Type.Catalog, MediaPackageElements.TEXTS) as Catalog
                catalog.identifier = null
                catalog.reference = segmentCatalog.reference
                mediaPackage.add(catalog) // the catalog now has an ID, so we can store the file properly
                val `in` = mpeg7CatalogService!!.serialize(textCatalog)
                val filename = "slidetext.xml"
                val workspaceURI = workspace!!
                        .put(mediaPackage.identifier.toString(), catalog.identifier, filename, `in`)
                catalog.setURI(workspaceURI)

                // Since we've enriched and stored the mpeg7 catalog, remove the original
                try {
                    mediaPackage.remove(segmentCatalog)
                    workspace!!.delete(segmentCatalog.getURI())
                } catch (e: Exception) {
                    logger.warn("Unable to delete segment catalog {}: {}", segmentCatalog.getURI(), e)
                }

                // Add flavor and target tags
                catalog.flavor = MediaPackageElements.TEXTS
                for (tag in targetTagSet) {
                    catalog.addTag(tag)
                }
            } finally {
                // Remove images that were created for text extraction
                logger.debug("Removing temporary images")
                for (image in images) {
                    try {
                        workspace!!.delete(image.getURI())
                    } catch (e: Exception) {
                        logger.warn("Unable to delete temporary image {}: {}", image.getURI(), e)
                    }

                }
                // Remove the temporary text
                for (j in jobs.values) {
                    var catalog: Catalog? = null
                    try {
                        val job = serviceRegistry.getJob(j.id)
                        if (Job.Status.FINISHED != job.status)
                            continue
                        catalog = MediaPackageElementParser.getFromXml(job.payload) as Catalog
                        if (catalog != null)
                            workspace!!.delete(catalog.getURI())
                    } catch (e: Exception) {
                        if (catalog != null) {
                            logger.warn("Unable to delete temporary text file {}: {}", catalog.getURI(), e)
                        } else {
                            logger.warn("Unable to parse textextraction payload of job {}", j.id)
                        }
                    }

                }
            }
        }

        logger.debug("Text analysis completed")
        return createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
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

    /**
     * Extracts the catalogs from the media package that match the requirements of flavor and tags specified in the
     * operation handler.
     *
     * @param mediaPackage
     * the media package
     * @param operation
     * the workflow operation
     * @return a map of catalog elements and their mpeg-7 representations
     * @throws IOException
     * if there is a problem reading the mpeg7
     */
    @Throws(IOException::class)
    protected fun loadSegmentCatalogs(mediaPackage: MediaPackage,
                                      operation: WorkflowOperationInstance): Map<Catalog, Mpeg7Catalog> {
        val catalogs = HashMap<Catalog, Mpeg7Catalog>()

        val sourceFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"))
        val sourceTagSet = asList(operation.getConfiguration("source-tags"))

        val catalogsWithTags = mediaPackage.getCatalogsByTags(sourceTagSet)

        for (mediaPackageCatalog in catalogsWithTags) {
            if (!MediaPackageElements.SEGMENTS.equals(mediaPackageCatalog.flavor)) {
                continue
            }
            if (sourceFlavor != null) {
                if (mediaPackageCatalog.reference == null)
                    continue
                val t = mediaPackage.getTrack(mediaPackageCatalog.reference.identifier)
                if (t == null || !t.flavor.matches(MediaPackageElementFlavor.parseFlavor(sourceFlavor)))
                    continue
            }

            // Make sure the catalog features at least one of the required tags
            if (!mediaPackageCatalog.containsTag(sourceTagSet))
                continue

            val mpeg7 = loadMpeg7Catalog(mediaPackageCatalog)

            // Make sure there is video content
            if (mpeg7.videoContent() == null || !mpeg7.videoContent().hasNext()) {
                logger.debug("Mpeg-7 segments catalog {} does not contain any video content", mpeg7)
                continue
            }

            // Make sure there is a temporal decomposition
            val videoContent = mpeg7.videoContent().next()
            val decomposition = videoContent.temporalDecomposition
            if (decomposition == null || !decomposition.hasSegments()) {
                logger.debug("Mpeg-7 catalog {} does not contain a temporal decomposition", mpeg7)
                continue
            }
            catalogs[mediaPackageCatalog] = mpeg7
        }

        return catalogs
    }

    /**
     * @see org.osgi.service.cm.ManagedService.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        if (properties != null && properties.get(OPT_STABILITY_THRESHOLD) != null) {
            val threshold = StringUtils.trimToNull(properties.get(OPT_STABILITY_THRESHOLD) as String)
            try {
                stabilityThreshold = Integer.parseInt(threshold)
                logger.info("The videosegmenter's stability threshold has been set to {} frames", stabilityThreshold)
            } catch (e: Exception) {
                stabilityThreshold = DEFAULT_STABILITY_THRESHOLD
                logger.warn("Found illegal value '{}' for the videosegmenter stability threshold. Falling back to default value of {} frames", threshold, DEFAULT_STABILITY_THRESHOLD)
            }

        } else {
            stabilityThreshold = DEFAULT_STABILITY_THRESHOLD
            logger.info("Using the default value of {} frames for the videosegmenter stability threshold", DEFAULT_STABILITY_THRESHOLD)
        }
    }

    /**
     * Sets the composer service.
     *
     * @param composerService
     */
    internal fun setComposerService(composerService: ComposerService) {
        this.composer = composerService
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(TextAnalysisWorkflowOperationHandler::class.java)

        /** Name of the encoding profile that extracts a still image from a movie  */
        val IMAGE_EXTRACTION_PROFILE = "text-analysis.http"

        /** The threshold for scene stability, in seconds  */
        private val DEFAULT_STABILITY_THRESHOLD = 5

        /** Name of the constant used to retreive the stability threshold  */
        val OPT_STABILITY_THRESHOLD = "stabilitythreshold"
    }

}
