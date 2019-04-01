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

package org.opencastproject.textanalyzer.impl

import org.opencastproject.dictionary.api.DictionaryService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.metadata.mpeg7.MediaTime
import org.opencastproject.metadata.mpeg7.MediaTimeImpl
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition
import org.opencastproject.metadata.mpeg7.TemporalDecomposition
import org.opencastproject.metadata.mpeg7.Textual
import org.opencastproject.metadata.mpeg7.Video
import org.opencastproject.metadata.mpeg7.VideoSegment
import org.opencastproject.metadata.mpeg7.VideoText
import org.opencastproject.metadata.mpeg7.VideoTextImpl
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.textanalyzer.api.TextAnalyzerException
import org.opencastproject.textanalyzer.api.TextAnalyzerService
import org.opencastproject.textextractor.api.TextExtractor
import org.opencastproject.textextractor.api.TextExtractorException
import org.opencastproject.textextractor.api.TextFrame
import org.opencastproject.textextractor.api.TextLine
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
/**
 * Creates a new instance of the text analyzer service.
 */
class TextAnalyzerServiceImpl : AbstractJobProducer(JOB_TYPE), TextAnalyzerService, ManagedService {

    /** The approximate load placed on the system by creating a text analysis job  */
    private var analysisJobLoad = DEFAULT_ANALYSIS_JOB_LOAD

    /** The text extraction implemenetation  */
    private var textExtractor: TextExtractor? = null

    /** Reference to the receipt service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Sets the receipt service
     *
     * @param serviceRegistry
     * the service registry
     */
    protected override var serviceRegistry: ServiceRegistry? = null

    /** The workspace to ue when retrieving remote media files  */
    private var workspace: Workspace? = null

    /** The mpeg-7 service  */
    protected var mpeg7CatalogService: Mpeg7CatalogService

    /** The dictionary service  */
    protected var dictionaryService: DictionaryService

    /** The security service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /**
     * Callback for setting the security service.
     *
     * @param securityService
     * the securityService to set
     */
    override var securityService: SecurityService? = null
        set

    /** The user directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /**
     * Callback for setting the user directory service.
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** The organization directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /**
     * Sets a reference to the organization directory service.
     *
     * @param organizationDirectory
     * the organization directory
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** List of available operations on jobs  */
    private enum class Operation {
        Extract
    }

    /**
     * OSGi callback on component activation.
     *
     * @param cc
     * the component context
     */
    override fun activate(cc: ComponentContext) {
        logger.info("Activating Text analyser service")
        super.activate(cc)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.textanalyzer.api.TextAnalyzerService.extract
     */
    @Throws(TextAnalyzerException::class, MediaPackageException::class)
    override fun extract(image: Attachment): Job {
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Extract.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(image)), analysisJobLoad)
        } catch (e: ServiceRegistryException) {
            throw TextAnalyzerException("Unable to create job", e)
        }

    }

    /**
     * Starts text extraction on the image and returns a receipt containing the final result in the form of an
     * Mpeg7Catalog.
     *
     * @param image
     * the element to analyze
     * @param block
     * `true` to make this operation synchronous
     * @return a receipt containing the resulting mpeg-7 catalog
     * @throws TextAnalyzerException
     */
    @Throws(TextAnalyzerException::class, MediaPackageException::class)
    private fun extract(job: Job, image: Attachment): Catalog {

        val imageUrl = image.getURI()

        var imageFile: File? = null
        try {
            val mpeg7 = Mpeg7CatalogImpl.newInstance()

            logger.info("Starting text extraction from {}", imageUrl)
            try {
                imageFile = workspace!!.get(imageUrl)
            } catch (e: NotFoundException) {
                throw TextAnalyzerException("Image $imageUrl not found in workspace", e)
            } catch (e: IOException) {
                throw TextAnalyzerException("Unable to access $imageUrl in workspace", e)
            }

            val videoTexts = analyze(imageFile, image.identifier)

            // Create a temporal decomposition
            val mediaTime = MediaTimeImpl(0, 0)
            val avContent = mpeg7.addVideoContent(image.identifier, mediaTime, null)
            val temporalDecomposition = avContent
                    .temporalDecomposition as TemporalDecomposition<VideoSegment>

            // Add a segment
            val videoSegment = temporalDecomposition.createSegment("segment-0")
            videoSegment.mediaTime = mediaTime

            // Add the video text to the spacio temporal decomposition of the segment
            val spatioTemporalDecomposition = videoSegment.createSpatioTemporalDecomposition(true,
                    false)
            for (videoText in videoTexts) {
                spatioTemporalDecomposition.addVideoText(videoText)
            }

            logger.info("Text extraction of {} finished, {} lines found", image.getURI(), videoTexts.size)

            val uri: URI
            val `in`: InputStream
            try {
                `in` = mpeg7CatalogService.serialize(mpeg7)
            } catch (e: IOException) {
                throw TextAnalyzerException("Error serializing mpeg7", e)
            }

            try {
                uri = workspace!!.putInCollection(COLLECTION_ID, job.id.toString() + ".xml", `in`)
            } catch (e: IOException) {
                throw TextAnalyzerException("Unable to put mpeg7 into the workspace", e)
            }

            val catalog = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                    .newElement(Catalog.TYPE, MediaPackageElements.TEXTS) as Catalog
            catalog.setURI(uri)

            logger.debug("Created MPEG7 catalog for {}", imageUrl)

            return catalog
        } catch (e: Exception) {
            logger.warn("Error extracting text from $imageUrl", e)
            if (e is TextAnalyzerException) {
                throw e
            } else {
                throw TextAnalyzerException(e)
            }
        } finally {
            try {
                workspace!!.delete(imageUrl)
            } catch (e: Exception) {
                logger.warn("Unable to delete temporary text analysis image {}: {}", imageUrl, e)
            }

        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            when (op) {
                TextAnalyzerServiceImpl.Operation.Extract -> {
                    val element = MediaPackageElementParser.getFromXml(arguments[0]) as Attachment
                    val catalog = extract(job, element)
                    return MediaPackageElementParser.getAsXml(catalog)
                }
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * Returns the video text element for the given image.
     *
     * @param imageFile
     * the image
     * @param id
     * the video text id
     * @return the video text found on the image
     * @throws TextAnalyzerException
     * if accessing the image fails
     */
    @Throws(TextAnalyzerException::class)
    protected fun analyze(imageFile: File?, id: String): Array<VideoText> {

        /* Call the text extractor implementation to extract the text from the
     * provided image file */
        val videoTexts = ArrayList<VideoText>()
        var textFrame: TextFrame? = null
        try {
            textFrame = textExtractor!!.extract(imageFile!!)
        } catch (e: IOException) {
            logger.warn("Error reading image file {}: {}", imageFile, e.message)
            throw TextAnalyzerException(e)
        } catch (e: TextExtractorException) {
            logger.warn("Error extracting text from {}: {}", imageFile, e.message)
            throw TextAnalyzerException(e)
        }

        /* Get detected text as raw string */
        var i = 1
        for (line in textFrame.lines) {
            if (line.text != null) {
                val videoText = VideoTextImpl(id + "-" + i++)
                videoText.boundary = line.boundaries
                val text = dictionaryService.cleanUpText(line.text)
                if (text != null) {
                    videoText.text = text
                    videoTexts.add(videoText)
                }
            }
        }


        return videoTexts.toTypedArray()
    }

    /**
     * Sets the text extractor.
     *
     * @param textExtractor
     * a text extractor implementation
     */
    protected fun setTextExtractor(textExtractor: TextExtractor) {
        this.textExtractor = textExtractor
    }

    /**
     * Sets the workspace
     *
     * @param workspace
     * an instance of the workspace
     */
    protected fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Sets the mpeg7CatalogService
     *
     * @param mpeg7CatalogService
     * an instance of the mpeg7 catalog service
     */
    protected fun setMpeg7CatalogService(mpeg7CatalogService: Mpeg7CatalogService) {
        this.mpeg7CatalogService = mpeg7CatalogService
    }

    /**
     * Sets the dictionary service
     *
     * @param dictionaryService
     * an instance of the dicitonary service
     */
    protected fun setDictionaryService(dictionaryService: DictionaryService) {
        this.dictionaryService = dictionaryService
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        analysisJobLoad = LoadUtil.getConfiguredLoadValue(properties, ANALYSIS_JOB_LOAD_KEY, DEFAULT_ANALYSIS_JOB_LOAD, serviceRegistry!!)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(TextAnalyzerServiceImpl::class.java)

        /** Resulting collection in the working file repository  */
        val COLLECTION_ID = "ocrtext"

        /** The approximate load placed on the system by creating a text analysis job  */
        val DEFAULT_ANALYSIS_JOB_LOAD = 0.2f

        /** The key to look for in the service configuration file to override the [DEFAULT_ANALYSIS_JOB_LOAD]  */
        val ANALYSIS_JOB_LOAD_KEY = "job.load.analysis"
    }
}
