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

package org.opencastproject.caption.impl

import org.opencastproject.util.MimeType.mimeType

import org.opencastproject.caption.api.Caption
import org.opencastproject.caption.api.CaptionConverter
import org.opencastproject.caption.api.CaptionConverterException
import org.opencastproject.caption.api.CaptionService
import org.opencastproject.caption.api.UnsupportedCaptionFormatException
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.framework.InvalidSyntaxException
import org.osgi.framework.ServiceReference
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.Arrays
import java.util.Dictionary
import java.util.HashMap

import javax.activation.FileTypeMap

/**
 * Implementation of [CaptionService]. Uses [ComponentContext] to get all registered
 * [CaptionConverter]s. Converters are searched based on `caption.format` property. If there is no
 * match for specified input or output format [UnsupportedCaptionFormatException] is thrown.
 *
 */
/**
 * Creates a new caption service.
 */
class CaptionServiceImpl : AbstractJobProducer(JOB_TYPE), CaptionService, ManagedService {

    /** The load introduced on the system by creating a caption job  */
    private var captionJobLoad = DEFAULT_CAPTION_JOB_LOAD

    /** Reference to workspace  */
    protected var workspace: Workspace

    /** Reference to remote service manager  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Setter for remote service manager via declarative activation
     */
    protected override var serviceRegistry: ServiceRegistry

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

    /** Component context needed for retrieving Converter Engines  */
    protected var componentContext: ComponentContext? = null

    /**
     * Returns all registered CaptionFormats.
     */
    protected// should not happen since it is called with null argument
    val availableCaptionConverters: HashMap<String, CaptionConverter>
        get() {
            val captionConverters = HashMap<String, CaptionConverter>()
            var refs: Array<ServiceReference<*>>? = null
            try {
                refs = componentContext!!.bundleContext.getServiceReferences(CaptionConverter::class.java.name, null)
            } catch (e: InvalidSyntaxException) {
            }

            if (refs != null) {
                for (ref in refs) {
                    val converter = componentContext!!.bundleContext.getService<Any>(ref) as CaptionConverter
                    val format = ref.getProperty("caption.format") as String
                    if (captionConverters.containsKey(format)) {
                        logger.warn("Caption converter with format {} has already been registered. Ignoring second definition.",
                                format)
                    } else {
                        captionConverters[ref.getProperty("caption.format") as String] = converter
                    }
                }
            }

            return captionConverters
        }

    /** List of available operations on jobs  */
    private enum class Operation {
        Convert, ConvertWithLanguage
    }

    /**
     * Activate this service implementation via the OSGI service component runtime.
     *
     * @param componentContext
     * the component context
     */
    override fun activate(componentContext: ComponentContext) {
        super.activate(componentContext)
        this.componentContext = componentContext
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.CaptionService.convert
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class, MediaPackageException::class)
    override fun convert(input: MediaPackageElement, inputFormat: String, outputFormat: String): Job {

        if (input == null)
            throw IllegalArgumentException("Input catalog can't be null")
        if (StringUtils.isBlank(inputFormat))
            throw IllegalArgumentException("Input format is null")
        if (StringUtils.isBlank(outputFormat))
            throw IllegalArgumentException("Output format is null")

        try {
            return serviceRegistry.createJob(JOB_TYPE, Operation.Convert.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(input), inputFormat, outputFormat), captionJobLoad)
        } catch (e: ServiceRegistryException) {
            throw CaptionConverterException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.caption.api.CaptionService.convert
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class, MediaPackageException::class)
    override fun convert(input: MediaPackageElement, inputFormat: String, outputFormat: String, language: String): Job {

        if (input == null)
            throw IllegalArgumentException("Input catalog can't be null")
        if (StringUtils.isBlank(inputFormat))
            throw IllegalArgumentException("Input format is null")
        if (StringUtils.isBlank(outputFormat))
            throw IllegalArgumentException("Output format is null")
        if (StringUtils.isBlank(language))
            throw IllegalArgumentException("Language format is null")

        try {
            return serviceRegistry.createJob(JOB_TYPE, Operation.ConvertWithLanguage.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(input), inputFormat, outputFormat, language), captionJobLoad)
        } catch (e: ServiceRegistryException) {
            throw CaptionConverterException("Unable to create a job", e)
        }

    }

    /**
     * Converts the captions and returns them in a new catalog.
     *
     * @return the converted catalog
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class, MediaPackageException::class)
    protected fun convert(job: Job, input: MediaPackageElement?, inputFormat: String, outputFormat: String,
                          language: String?): MediaPackageElement {
        try {

            // check parameters
            if (input == null)
                throw IllegalArgumentException("Input element can't be null")
            if (StringUtils.isBlank(inputFormat))
                throw IllegalArgumentException("Input format is null")
            if (StringUtils.isBlank(outputFormat))
                throw IllegalArgumentException("Output format is null")

            // get input file
            val captionsFile: File
            try {
                captionsFile = workspace.get(input.getURI())
            } catch (e: NotFoundException) {
                throw CaptionConverterException("Requested media package element $input could not be found.")
            } catch (e: IOException) {
                throw CaptionConverterException("Requested media package element " + input + "could not be accessed.")
            }

            logger.debug("Atempting to convert from {} to {}...", inputFormat, outputFormat)

            var collection: List<Caption>? = null
            try {
                collection = importCaptions(captionsFile, inputFormat, language)
                logger.debug("Parsing to collection succeeded.")
            } catch (e: UnsupportedCaptionFormatException) {
                throw UnsupportedCaptionFormatException(inputFormat)
            } catch (e: CaptionConverterException) {
                throw e
            }

            val exported: URI
            try {
                exported = exportCaptions(collection,
                        job.id.toString() + "." + FilenameUtils.getExtension(captionsFile.absolutePath), outputFormat, language)
                logger.debug("Exporting captions succeeding.")
            } catch (e: UnsupportedCaptionFormatException) {
                throw UnsupportedCaptionFormatException(outputFormat)
            } catch (e: IOException) {
                throw CaptionConverterException("Could not export caption collection.", e)
            }

            // create catalog and set properties
            val converter = getCaptionConverter(outputFormat)
            val elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            val mpe = elementBuilder.elementFromURI(exported, converter!!.elementType,
                    MediaPackageElementFlavor(
                            "captions", outputFormat + if (language == null) "" else "+$language"))
            if (mpe.mimeType == null) {
                val mimetype = FileTypeMap.getDefaultFileTypeMap().getContentType(exported.path).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                mpe.mimeType = mimeType(mimetype[0], mimetype[1])
            }
            if (language != null)
                mpe.addTag("lang:$language")

            return mpe

        } catch (e: Exception) {
            logger.warn("Error converting captions in " + input!!, e)
            if (e is CaptionConverterException) {
                throw e
            } else if (e is UnsupportedCaptionFormatException) {
                throw e
            } else {
                throw CaptionConverterException(e)
            }
        }

    }

    /**
     *
     * {@inheritDoc}
     *
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class)
    override fun getLanguageList(input: MediaPackageElement, format: String): Array<String> {

        if (format == null) {
            throw UnsupportedCaptionFormatException("<null>")
        }
        val converter = getCaptionConverter(format) ?: throw UnsupportedCaptionFormatException(format)

        val captions: File
        try {
            captions = workspace.get(input.getURI())
        } catch (e: NotFoundException) {
            throw CaptionConverterException("Requested media package element $input could not be found.")
        } catch (e: IOException) {
            throw CaptionConverterException("Requested media package element " + input + "could not be accessed.")
        }

        var stream: FileInputStream? = null
        val languageList: Array<String>?
        try {
            stream = FileInputStream(captions)
            languageList = converter.getLanguageList(stream)
        } catch (e: FileNotFoundException) {
            throw CaptionConverterException("Requested file " + captions + "could not be found.")
        } finally {
            IoSupport.closeQuietly(stream)
        }

        return languageList ?: arrayOfNulls(0)
    }

    /**
     * Returns specific [CaptionConverter]. Registry is searched based on formatName, so in order for
     * [CaptionConverter] to be found, it has to have `caption.format` property set with
     * [CaptionConverter] format. If none is found, null is returned, if more than one is found then the first
     * reference is returned.
     *
     * @param formatName
     * name of the caption format
     * @return [CaptionConverter] or null if none is found
     */
    protected fun getCaptionConverter(formatName: String): CaptionConverter? {
        var ref: Array<ServiceReference<*>>? = null
        try {
            ref = componentContext!!.bundleContext.getServiceReferences(CaptionConverter::class.java.name,
                    "(caption.format=$formatName)")
        } catch (e: InvalidSyntaxException) {
            throw RuntimeException(e)
        }

        if (ref == null) {
            logger.warn("No caption format available for {}.", formatName)
            return null
        }
        if (ref.size > 1)
            logger.warn("Multiple references for caption format {}! Returning first service reference.", formatName)
        return componentContext!!.bundleContext.getService<Any>(ref[0]) as CaptionConverter
    }

    /**
     * Imports captions using registered converter engine and specified language.
     *
     * @param input
     * file containing captions
     * @param inputFormat
     * format of imported captions
     * @param language
     * (optional) captions' language
     * @return [List] of parsed captions
     * @throws UnsupportedCaptionFormatException
     * if there is no registered engine for given format
     * @throws IllegalCaptionFormatException
     * if parser encounters exception
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class)
    private fun importCaptions(input: File, inputFormat: String, language: String?): List<Caption> {
        // get input format
        val converter = getCaptionConverter(inputFormat)
        if (converter == null) {
            logger.error("No available caption format found for {}.", inputFormat)
            throw UnsupportedCaptionFormatException(inputFormat)
        }

        var fileStream: FileInputStream? = null
        try {
            fileStream = FileInputStream(input)
            return converter.importCaption(fileStream, language!!)
        } catch (e: FileNotFoundException) {
            throw CaptionConverterException("Could not locate file $input")
        } finally {
            IOUtils.closeQuietly(fileStream)
        }
    }

    /**
     * Exports captions [List] to specified format. Extension is added to exported file name. Throws
     * [UnsupportedCaptionFormatException] if format is not supported.
     *
     * @param captions
     * [{][outputName]
     */
    @Throws(UnsupportedCaptionFormatException::class, IOException::class)
    private fun exportCaptions(captions: List<Caption>?, outputName: String, outputFormat: String, language: String?): URI {
        val converter = getCaptionConverter(outputFormat)
        if (converter == null) {
            logger.error("No available caption format found for {}.", outputFormat)
            throw UnsupportedCaptionFormatException(outputFormat)
        }

        // TODO instead of first writing it all in memory, write it directly to disk
        val outputStream = ByteArrayOutputStream()
        try {
            converter.exportCaption(outputStream, captions!!, language!!)
        } catch (e: IOException) {
            // since we're writing to memory, this should not happen
        }

        val `in` = ByteArrayInputStream(outputStream.toByteArray())
        return workspace.putInCollection(COLLECTION, outputName + "." + converter.extension, `in`)
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

            val catalog = MediaPackageElementParser.getFromXml(arguments[0])
            val inputFormat = arguments[1]
            val outputFormat = arguments[2]

            var resultingCatalog: MediaPackageElement? = null

            when (op) {
                CaptionServiceImpl.Operation.Convert -> {
                    resultingCatalog = convert(job, catalog, inputFormat, outputFormat, null)
                    return MediaPackageElementParser.getAsXml(resultingCatalog)
                }
                CaptionServiceImpl.Operation.ConvertWithLanguage -> {
                    val language = arguments[3]
                    resultingCatalog = convert(job, catalog, inputFormat, outputFormat, language)
                    return MediaPackageElementParser.getAsXml(resultingCatalog)
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
     * Setter for workspace via declarative activation
     */
    protected fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        captionJobLoad = LoadUtil.getConfiguredLoadValue(properties, CAPTION_JOB_LOAD_KEY, DEFAULT_CAPTION_JOB_LOAD, serviceRegistry)
    }

    companion object {

        /** Logging utility  */
        private val logger = LoggerFactory.getLogger(CaptionServiceImpl::class.java)

        /** The collection name  */
        val COLLECTION = "captions"

        /** The load introduced on the system by creating a caption job  */
        val DEFAULT_CAPTION_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the [DEFAULT_CAPTION_JOB_LOAD]  */
        val CAPTION_JOB_LOAD_KEY = "job.load.caption"
    }

}
