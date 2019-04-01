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

package org.opencastproject.workflow.handler.coverimage

import org.opencastproject.coverimage.CoverImageException
import org.opencastproject.coverimage.CoverImageService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.metadata.api.MetadataValue
import org.opencastproject.metadata.api.StaticMetadata
import org.opencastproject.metadata.api.StaticMetadataService
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

/**
 * Base implementation of the cover image workflow operation handler
 */
abstract class CoverImageWorkflowOperationHandlerBase : AbstractWorkflowOperationHandler() {

    /** Returns a cover image service  */
    protected abstract val coverImageService: CoverImageService

    /** Returns a workspace service  */
    protected abstract val workspace: Workspace

    /** Returns a static metadata service  */
    protected abstract val staticMetadataService: StaticMetadataService

    /** Returns a dublin core catalog service  */
    protected abstract val dublinCoreCatalogService: DublinCoreCatalogService

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mediaPackage = workflowInstance.mediaPackage
        val operation = workflowInstance.currentOperation

        logger.info("Cover Image Workflow started for media package '{}'", mediaPackage.identifier)

        // User XML metadata from operation configuration, fallback to default metadata
        var xml: String? = operation.getConfiguration(XML_METADATA)
        if (xml == null) {
            xml = getMetadataXml(mediaPackage)
            logger.debug("Metadata was not part of operation configuration, using Dublin Core as fallback")
        }
        logger.debug("Metadata set to: {}", xml)

        val xsl = loadXsl(operation)
        logger.debug("XSL for transforming metadata to SVG loaded: {}", xsl)

        // Read image dimensions
        val width = getIntConfiguration(operation, WIDTH)
        logger.debug("Image width set to {}px", width)
        val height = getIntConfiguration(operation, HEIGHT)
        logger.debug("Image height set to {}px", height)

        // Read optional poster image flavor
        var posterImgUri = getPosterImageFileUrl(operation.getConfiguration(POSTERIMAGE_URL))
        if (posterImgUri == null)
            posterImgUri = getPosterImageFileUrl(mediaPackage, operation.getConfiguration(POSTERIMAGE_FLAVOR))
        if (posterImgUri == null) {
            logger.debug("No optional poster image set")
        } else {
            logger.debug("Poster image found at '{}'", posterImgUri)
        }

        // Read target flavor
        val targetFlavor = operation.getConfiguration(TARGET_FLAVOR)
        if (StringUtils.isBlank(targetFlavor)) {
            logger.warn("Required configuration key '{}' is blank", TARGET_FLAVOR)
            throw WorkflowOperationException("Configuration key '$TARGET_FLAVOR' must be set")
        }
        try {
            MediaPackageElementFlavor.parseFlavor(targetFlavor)
        } catch (e: IllegalArgumentException) {
            logger.warn("Given target flavor '{}' is not a valid flavor", targetFlavor)
            throw WorkflowOperationException(e)
        }

        var generate: Job
        try {
            generate = coverImageService.generateCoverImage(xml, xsl, width.toString(), height.toString(),
                    posterImgUri!!, targetFlavor)
            logger.debug("Job for cover image generation created")

            if (!waitForStatus(generate).isSuccess) {
                throw WorkflowOperationException("'Cover image' job did not successfuly end")
            }

            generate = serviceRegistry.getJob(generate.id)
            val coverImage = MediaPackageElementParser.getFromXml(generate.payload) as Attachment

            val attachmentUri = workspace.moveTo(coverImage.getURI(), mediaPackage.identifier.compact(),
                    UUID.randomUUID().toString(), COVERIMAGE_FILENAME)
            coverImage.setURI(attachmentUri)

            coverImage.mimeType = MimeTypes.PNG

            // Add tags
            val targetTags = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS))
            if (targetTags != null) {
                for (tag in asList(targetTags)) {
                    logger.trace("Tagging image with '{}'", tag)
                    if (StringUtils.trimToNull(tag) != null)
                        coverImage.addTag(tag)
                }
            }

            mediaPackage.add(coverImage)
        } catch (e: MediaPackageException) {
            throw WorkflowOperationException(e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException(e)
        } catch (e: ServiceRegistryException) {
            throw WorkflowOperationException(e)
        } catch (e: CoverImageException) {
            throw WorkflowOperationException(e)
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException(e)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

        logger.info("Cover Image Workflow finished successfully for media package '{}' within {}ms",
                mediaPackage.identifier, generate.queueTime)
        return createResult(mediaPackage, Action.CONTINUE, generate.queueTime!!)
    }

    @Throws(WorkflowOperationException::class)
    protected fun getPosterImageFileUrl(mediaPackage: MediaPackage, posterimageFlavor: String?): String? {

        if (posterimageFlavor == null) {
            logger.debug("Optional configuration key '{}' not set", POSTERIMAGE_FLAVOR)
            return null
        }

        val flavor: MediaPackageElementFlavor
        try {
            flavor = MediaPackageElementFlavor.parseFlavor(posterimageFlavor)
        } catch (e: IllegalArgumentException) {
            logger.warn("'{}' is not a valid flavor", posterimageFlavor)
            throw WorkflowOperationException(e)
        }

        val atts = mediaPackage.getAttachments(flavor)
        if (atts.size > 1) {
            logger.warn("More than one attachment with the flavor '{}' found in media package '{}'", posterimageFlavor,
                    mediaPackage.identifier)
            throw WorkflowOperationException("More than one attachment with the flavor'$posterimageFlavor' found.")
        } else if (atts.size == 0) {
            logger.warn("No attachment with the flavor '{}' found in media package '{}'", posterimageFlavor,
                    mediaPackage.identifier)
            return null
        }
        try {
            return workspace.get(atts[0].getURI()).absolutePath
        } catch (e: NotFoundException) {
            throw WorkflowOperationException(e)
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

    }

    fun getPosterImageFileUrl(posterimageUrlOpt: String): String? {

        if (StringUtils.isBlank(posterimageUrlOpt))
            return null

        var url: URL? = null
        try {
            url = URL(posterimageUrlOpt)
        } catch (e: Exception) {
            logger.debug("Given poster image URI '{}' is not valid", posterimageUrlOpt)
        }

        if (url == null)
            return null

        if ("file" == url.protocol)
            return url.toExternalForm()

        try {
            val coverImageFile = workspace.get(url.toURI())
            return coverImageFile.path
        } catch (e: NotFoundException) {
            logger.warn("Poster image could not be found at '{}'", url)
            return null
        } catch (e: IOException) {
            logger.warn("Error getting poster image: {}", e.message)
            return null
        } catch (e: URISyntaxException) {
            logger.warn("Given URL '{}' is not a valid URI", url)
            return null
        }

    }

    @Throws(WorkflowOperationException::class)
    protected fun getIntConfiguration(operation: WorkflowOperationInstance, key: String): Int {
        val confString = operation.getConfiguration(key)
        var confValue = 0
        if (StringUtils.isBlank(confString))
            throw WorkflowOperationException("Configuration key '$key' must be set")
        try {
            confValue = Integer.parseInt(confString)
            if (confValue < 1)
                throw WorkflowOperationException("Configuration key '" + key
                        + "' must be set to a valid positive integer value")
        } catch (e: NumberFormatException) {
            throw WorkflowOperationException("Configuration key '" + key
                    + "' must be set to a valid positive integer value")
        }

        return confValue
    }

    @Throws(WorkflowOperationException::class)
    protected fun loadXsl(operation: WorkflowOperationInstance): String {
        val xslUriString = operation.getConfiguration(XSL_FILE_URL)
        if (StringUtils.isBlank(xslUriString))
            throw WorkflowOperationException("Configuration option '$XSL_FILE_URL' must not be empty")
        var reader: FileReader? = null
        try {
            val xslUri = URI(xslUriString)
            val xslFile = File(xslUri)
            reader = FileReader(xslFile)
            return IOUtils.toString(reader)
        } catch (e: FileNotFoundException) {
            logger.warn("There is no (xsl) file at the given uri '{}': {}", xslUriString, e.message)
            throw WorkflowOperationException("There is no (XSL) file at the given URI", e)
        } catch (e: URISyntaxException) {
            logger.warn("Given XSL file URI ({}) is not valid: {}", xslUriString, e.message)
            throw WorkflowOperationException("Given XSL file URI is not valid", e)
        } catch (e: IOException) {
            logger.warn("Error while reading XSL file ({}): {}", xslUriString, e.message)
            throw WorkflowOperationException("Error while reading XSL file", e)
        } finally {
            IOUtils.closeQuietly(reader)
        }
    }

    protected fun getMetadataXml(mp: MediaPackage): String {
        val metadata = staticMetadataService.getMetadata(mp)

        val xml = StringBuilder()
        xml.append("<metadata xmlns:dcterms=\"http://purl.org/dc/terms/\">")

        for (title in getFirstMetadataValue(metadata.titles))
            appendXml(xml, "title", title)
        for (description in getFirstMetadataValue(metadata.description))
            appendXml(xml, "description", description)
        for (language in metadata.language)
            appendXml(xml, "language", language)
        for (created in metadata.created)
        /* Method formatDate of org.apache.xalan.lib.ExsltDatetime requires the format CCYY-MM-DDThh:mm:ss */
            appendXml(xml, "date", SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss").format(created))
        for (period in metadata.temporalPeriod) {
            if (period[0] != null) {
                appendXml(xml, "start", SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss").format(period[0]))
            }
            if (period[1] != null) {
                appendXml(xml, "end", SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss").format(period[1]))
            }
        }
        for (instant in metadata.temporalInstant)
            appendXml(xml, "start", SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss").format(instant))
        for (duration in metadata.temporalDuration)
            appendXml(xml, "duration", SimpleDateFormat("HH:mm:ss").format(Date(duration!!)))
        for (license in getFirstMetadataValue(metadata.licenses))
            appendXml(xml, "license", license)
        for (isPartOf in metadata.isPartOf)
            appendXml(xml, "series", isPartOf)
        for (contributors in getFirstMetadataValue(metadata.contributors))
            appendXml(xml, "contributors", contributors)
        for (creators in getFirstMetadataValue(metadata.creators))
            appendXml(xml, "creators", creators)
        for (subjects in getFirstMetadataValue(metadata.subjects))
            appendXml(xml, "subjects", subjects)

        xml.append("</metadata>")

        return xml.toString()
    }

    protected fun getFirstMetadataValue(list: List<MetadataValue<String>>): Option<String> {
        for (data in list) {
            if (DublinCore.LANGUAGE_UNDEFINED == data.language)
                return Option.some(data.value)
        }
        return Option.none()
    }

    fun appendXml(xml: StringBuilder, name: String, body: String) {
        if (StringUtils.isBlank(body) || StringUtils.isBlank(name))
            return

        xml.append("<")
        xml.append(name)
        xml.append(">")

        xml.append(StringEscapeUtils.escapeXml(body))

        xml.append("</")
        xml.append(name)
        xml.append(">")
    }

    companion object {

        private val COVERIMAGE_FILENAME = "coverimage.png"
        private val XSL_FILE_URL = "stylesheet"
        private val XML_METADATA = "metadata"
        private val WIDTH = "width"
        private val HEIGHT = "height"
        private val POSTERIMAGE_FLAVOR = "posterimage-flavor"
        private val POSTERIMAGE_URL = "posterimage"
        private val TARGET_FLAVOR = "target-flavor"
        private val TARGET_TAGS = "target-tags"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CoverImageWorkflowOperationHandlerBase::class.java)
    }

}
