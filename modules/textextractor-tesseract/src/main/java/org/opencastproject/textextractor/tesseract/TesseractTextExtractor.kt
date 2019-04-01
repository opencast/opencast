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


package org.opencastproject.textextractor.tesseract

import org.opencastproject.textextractor.api.TextExtractor
import org.opencastproject.textextractor.api.TextExtractorException
import org.opencastproject.textextractor.api.TextFrame
import org.opencastproject.util.ProcessRunner

import com.entwinemedia.fn.Pred

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Dictionary

/**
 * Commandline wrapper around tesseract' `tesseract` command.
 */
class TesseractTextExtractor
/**
 * Creates a new tesseract command wrapper that will be using the given binary.
 *
 * @param binary
 * the tesseract binary
 */
@JvmOverloads constructor(binary: String = TESSERACT_BINARY_DEFAULT) : TextExtractor, ManagedService {

    /** Binary of the tesseract command  */
    /**
     * Returns the path to the `tesseract` binary.
     *
     * @return path to the binary
     */
    /**
     * Sets the path to the `tesseract` binary.
     *
     * @param binary
     */
    var binary: String? = null

    /** Additional options for the tesseract command  */
    /**
     * Returns the additional options for tesseract..
     *
     * @return additional options
     */
    /**
     * Sets additional options for tesseract calls.
     *
     * @param addOptions
     */
    var additionalOptions = ""

    init {
        this.binary = binary
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.textextractor.api.TextExtractor.extract
     */
    @Throws(TextExtractorException::class)
    override fun extract(image: File): TextFrame {
        if (binary == null)
            throw IllegalStateException("Binary is not set")

        var `is`: InputStream? = null
        var outputFile: File? = null
        val outputFileBase = File(image.parentFile, FilenameUtils.getBaseName(image.name))
        // Run tesseract
        val opts = getAnalysisOptions(image, outputFileBase)
        logger.info("Running Tesseract: {} {}", binary, opts)
        try {
            val exitCode = ProcessRunner.run(ProcessRunner.mk(binary!!, opts), fnLogDebug, object : Pred<String>() {
                override fun apply(line: String): Boolean? {
                    if (!line.trim { it <= ' ' }.startsWith("Page") && !line.trim { it <= ' ' }.startsWith("Tesseract Open Source OCR Engine")) {
                        logger.warn(line)
                    }
                    return true
                }
            })
            if (exitCode != 0) {
                throw TextExtractorException("Text analyzer $binary exited with code $exitCode")
            }
            // Read the tesseract output file
            outputFile = File(outputFileBase.absolutePath + ".txt")
            `is` = FileInputStream(outputFile)
            val textFrame = TesseractTextFrame.parse(`is`)
            `is`.close()
            return textFrame
        } catch (e: IOException) {
            throw TextExtractorException("Error running text extractor " + binary!!, e)
        } finally {
            IOUtils.closeQuietly(`is`)
            FileUtils.deleteQuietly(outputFile)
        }
    }

    /**
     * The only parameter to `tesseract` is the filename, so this is what this method returns.
     *
     * @param image
     * the image file
     * @return the options to run analysis on the image
     */
    protected fun getAnalysisOptions(image: File, outputFile: File): String {
        val options = StringBuilder()
        options.append(image.absolutePath)
        options.append(" ")
        options.append(outputFile.absolutePath)
        options.append(" ")
        options.append(this.additionalOptions)
        return options.toString()
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        val path = properties.get(TESSERACT_BINARY_CONFIG_KEY) as String
        if (path != null) {
            logger.info("Setting Tesseract path to {}", path)
            this.binary = path
        }
        /* Set additional options for tesseract (i.e. language to use) */
        val addopts = properties.get(TESSERACT_OPTS_CONFIG_KEY) as String
        if (addopts != null) {
            logger.info("Setting additional options for Tesseract path to '{}'", addopts)
            this.additionalOptions = addopts
        }
    }

    fun activate(cc: ComponentContext) {
        // Configure ffmpeg
        val path = cc.bundleContext.getProperty(TESSERACT_BINARY_CONFIG_KEY) as String
        if (path == null) {
            logger.debug("DEFAULT $TESSERACT_BINARY_CONFIG_KEY: $TESSERACT_BINARY_DEFAULT")
        } else {
            binary = path
            logger.info("Setting Tesseract path to binary from config: {}", path)
        }
        /* Set additional options for tesseract (i.e. language to use) */
        val addopts = cc.bundleContext.getProperty(TESSERACT_OPTS_CONFIG_KEY) as String
        if (addopts != null) {
            logger.info("Setting additional options for Tesseract to '{}'", addopts)
            this.additionalOptions = addopts
        } else {
            logger.info("No additional options for Tesseract")
            this.additionalOptions = ""
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(TesseractTextExtractor::class.java)

        /** Default name of the tesseract binary  */
        val TESSERACT_BINARY_DEFAULT = "tesseract"

        /** Configuration property that defines the path to the tesseract binary  */
        val TESSERACT_BINARY_CONFIG_KEY = "org.opencastproject.textanalyzer.tesseract.path"

        /** Configuration property that defines additional tesseract options like the
         * language or the pagesegmode to use. This is just appended to the command
         * line when tesseract is called.  */
        val TESSERACT_OPTS_CONFIG_KEY = "org.opencastproject.textanalyzer.tesseract.options"

        private val fnLogDebug = object : Pred<String>() {
            override fun apply(s: String): Boolean {
                logger.debug(s)
                return true
            }
        }
    }
}
/**
 * Creates a new tesseract command wrapper that will be using the default binary.
 */
