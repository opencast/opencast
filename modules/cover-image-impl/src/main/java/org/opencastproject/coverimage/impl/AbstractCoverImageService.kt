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

package org.opencastproject.coverimage.impl

import org.apache.commons.lang3.StringUtils.isNotBlank

import org.opencastproject.coverimage.CoverImageException
import org.opencastproject.coverimage.CoverImageService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.workspace.api.Workspace

import org.apache.batik.apps.rasterizer.DestinationType
import org.apache.batik.apps.rasterizer.SVGConverter
import org.apache.batik.apps.rasterizer.SVGConverterException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.util.Arrays

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Service for creating cover images
 */
/** Creates a new composer service instance.  */
abstract class AbstractCoverImageService : AbstractJobProducer(JOB_TYPE), CoverImageService {

    /** The workspace service  */
    protected var workspace: Workspace? = null

    /** The service registry service  */
    protected var serviceRegistry: ServiceRegistry? = null

    /** The security service  */
    protected var securityService: SecurityService? = null

    /** The user directory service  */
    protected var userDirectoryService: UserDirectoryService? = null

    /** The organization directory service  */
    protected var organizationDirectoryService: OrganizationDirectoryService? = null

    /** List of available operations on jobs  */
    protected enum class Operation {
        Generate
    }

    @Throws(Exception::class)
    override fun process(job: Job): String {

        val arguments = job.arguments
        val xml = arguments[0]
        val xsl = arguments[1]
        val width = Integer.valueOf(arguments[2])
        val height = Integer.valueOf(arguments[3])
        val posterImage = arguments[4]
        val targetFlavor = arguments[5]

        var op: Operation? = null
        op = Operation.valueOf(job.operation)
        when (op) {
            AbstractCoverImageService.Operation.Generate -> {
                val result = generateCoverImageInternal(job, xml, xsl, width, height, posterImage, targetFlavor)
                return MediaPackageElementParser.getAsXml(result)
            }
            else -> throw IllegalStateException("Don't know how to handle operation '" + job.operation + "'")
        }
    }

    @Throws(CoverImageException::class)
    override fun generateCoverImage(xml: String, xsl: String, width: String, height: String, posterImageUri: String,
                                    targetFlavor: String): Job {
        var posterImageUri = posterImageUri

        // Null values are not passed to the arguments list
        if (posterImageUri == null)
            posterImageUri = ""

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Generate.toString(),
                    Arrays.asList<T>(xml, xsl, width, height, posterImageUri, targetFlavor))
        } catch (e: ServiceRegistryException) {
            throw CoverImageException("Unable to create a job", e)
        }

    }

    @Throws(CoverImageException::class)
    protected fun generateCoverImageInternal(job: Job, xml: String, xsl: String, width: Int, height: Int,
                                             posterImage: String, targetFlavor: String): Attachment {

        val result: URI
        var tempSvg: File? = null
        var tempPng: File? = null
        var xmlReader: StringReader? = null

        try {
            val xslDoc = parseXsl(xsl)

            // Create temp SVG file for transformation result
            tempSvg = createTempFile(job, ".svg")
            val svg = StreamResult(tempSvg)

            // Load Metadata (from resources)
            xmlReader = StringReader(xml)
            val xmlSource = StreamSource(xmlReader)

            // Transform XML metadata with stylesheet to SVG
            transformSvg(svg, xmlSource, xslDoc, width, height, posterImage)

            // Rasterize SVG to PNG
            tempPng = createTempFile(job, ".png")
            rasterizeSvg(tempSvg, tempPng)

            var `in`: FileInputStream? = null
            try {
                `in` = FileInputStream(tempPng)
                result = workspace!!.putInCollection(COVERIMAGE_WORKSPACE_COLLECTION, job.id.toString() + "_coverimage.png", `in`)
                log.debug("Put the cover image into the workspace ({})", result)
            } catch (e: FileNotFoundException) {
                // should never happen...
                throw CoverImageException(e)
            } catch (e: IOException) {
                log.warn("Error while putting resulting image into workspace collection '{}': {}",
                        COVERIMAGE_WORKSPACE_COLLECTION, e)
                throw CoverImageException("Error while putting resulting image into workspace collection", e)
            } finally {
                IOUtils.closeQuietly(`in`)
            }
        } finally {
            FileUtils.deleteQuietly(tempSvg)
            FileUtils.deleteQuietly(tempPng)
            log.debug("Removed temporary files")

            IOUtils.closeQuietly(xmlReader)
        }

        return MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(result, Type.Attachment, MediaPackageElementFlavor.parseFlavor(targetFlavor)) as Attachment
    }

    @Throws(CoverImageException::class)
    protected fun createTempFile(job: Job, suffix: String): File {
        val tempFile: File
        try {
            tempFile = File.createTempFile(COVERIMAGE_WORKSPACE_COLLECTION, java.lang.Long.toString(job.id) + "_" + suffix)
            log.debug("Created temporary file {}", tempFile)
        } catch (e: IOException) {
            log.warn("Error creating temporary file:", e)
            throw CoverImageException("Error creating temporary file", e)
        }

        return tempFile
    }

    companion object {

        protected val COVERIMAGE_WORKSPACE_COLLECTION = "coverimage"

        /** The logging facility  */
        private val log = LoggerFactory.getLogger(AbstractCoverImageService::class.java)

        @Throws(CoverImageException::class)
        fun parseXsl(xsl: String): Document {
            if (StringUtils.isBlank(xsl))
                throw IllegalArgumentException("XSL string must not be empty")

            val dbFactory = DocumentBuilderFactory.newInstance()
            dbFactory.isNamespaceAware = true
            val xslDoc: Document
            try {
                log.debug("Parse given XSL to a org.w3c.dom.Document object")
                val dBuilder = dbFactory.newDocumentBuilder()
                xslDoc = dBuilder.parse(InputSource(ByteArrayInputStream(xsl.toByteArray(charset("utf-8")))))
            } catch (e: ParserConfigurationException) {
                // this should never happen...
                throw CoverImageException("The XSLT parser has serious configuration errors", e)
            } catch (e: SAXException) {
                log.warn("Error while parsing the XSLT stylesheet: {}", e.message)
                throw CoverImageException("Error while parsing the XSLT stylesheet", e)
            } catch (e: IOException) {
                log.warn("Error while reading the XSLT stylesheet: {}", e.message)
                throw CoverImageException("Error while reading the XSLT stylesheet", e)
            }

            return xslDoc
        }

        @Throws(TransformerFactoryConfigurationError::class, CoverImageException::class)
        fun transformSvg(svg: Result?, xmlSource: Source?, xslDoc: Document?, width: Int, height: Int,
                         posterImage: String) {
            if (svg == null || xmlSource == null || xslDoc == null)
                throw IllegalArgumentException("Neither svg nor xmlSource nor xslDoc must be null")

            val factory = TransformerFactory.newInstance()
            val transformer: Transformer
            try {
                transformer = factory.newTransformer(DOMSource(xslDoc))
            } catch (e: TransformerConfigurationException) {
                // this should never happen...
                throw CoverImageException("The XSL transformer factory has serious configuration errors", e)
            }

            transformer.setParameter("width", width)
            transformer.setParameter("height", height)
            if (isNotBlank(posterImage))
                transformer.setParameter("posterimage", posterImage)

            val thread = Thread.currentThread()
            val loader = thread.contextClassLoader
            thread.contextClassLoader = AbstractCoverImageService::class.java.classLoader
            try {
                log.debug("Transform XML source to SVG")
                transformer.transform(xmlSource, svg)
            } catch (e: TransformerException) {
                log.warn("Error while transforming SVG to image: {}", e.message)
                throw CoverImageException("Error while transforming SVG to image", e)
            } finally {
                thread.contextClassLoader = loader
            }
        }

        @Throws(CoverImageException::class)
        protected fun rasterizeSvg(svgSource: File, pngResult: File?) {
            val converter = SVGConverter()
            converter.destinationType = DestinationType.PNG
            converter.dst = pngResult
            converter.setSources(arrayOf(svgSource.absolutePath))
            try {
                log.debug("Start converting SVG to PNG")
                converter.execute()
            } catch (e: SVGConverterException) {
                log.warn("Error while converting the SVG to a PNG: {}", e.message)
                throw CoverImageException("Error while converting the SVG to a PNG", e)
            }

        }
    }
}
