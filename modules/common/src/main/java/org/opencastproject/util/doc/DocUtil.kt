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

package org.opencastproject.util.doc

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer

import freemarker.core.ParseException
import freemarker.template.Configuration
import freemarker.template.DefaultObjectWrapper
import freemarker.template.Template
import freemarker.template.TemplateException

/**
 * This provides methods for handling documentation generation The is mainly for generating REST documentation but it
 * could be used for other things as well
 *
 * @see DocData
 */
object DocUtil {

    private val logger = LoggerFactory.getLogger(DocUtil::class.java!!)

    private var freemarkerConfig: Configuration? = null // reusable template processor

    init {
        // initialize the freemarker template engine
        reset()
    }

    fun reset() {
        freemarkerConfig = null
        // static initializer
        freemarkerConfig = Configuration()
        freemarkerConfig!!.objectWrapper = DefaultObjectWrapper()
        freemarkerConfig!!.clearTemplateCache()
        logger.debug("Created new freemarker template processor for DocUtils")
    }

    /**
     * Handles the replacement of the variable strings within textual templates and also allows the setting of variables
     * for the control of logical branching within the text template as well<br></br>
     * Uses and expects freemarker (http://freemarker.org/) style templates (that is using ${name} as the marker for a
     * replacement)<br></br>
     * NOTE: These should be compatible with Velocity (http://velocity.apache.org/) templates if you use the formal
     * notation (formal: ${variable}, shorthand: $variable)
     *
     * @param templateName
     * this is the key to cache the template under
     * @param textTemplate
     * a freemarker/velocity style text template, cannot be null or empty string
     * @param data
     * a set of replacement values which are in the map like so:<br></br>
     * key =&gt; value (String =&gt; Object)<br></br>
     * "username" =&gt; "aaronz"<br></br>
     * @return the processed template
     */
    fun processTextTemplate(templateName: String, textTemplate: String, data: Map<String, Any>?): String {
        if (freemarkerConfig == null) {
            throw IllegalStateException("freemarkerConfig is not initialized")
        }
        if (StringUtils.isEmpty(templateName)) {
            throw IllegalArgumentException("The templateName cannot be null or empty string, " + "please specify a key name to use when processing this template (can be anything moderately unique)")
        }
        if (data == null || data.size == 0) {
            return textTemplate
        }
        if (StringUtils.isEmpty(textTemplate)) {
            throw IllegalArgumentException("The textTemplate cannot be null or empty string, " + "please pass in at least something in the template or do not call this method")
        }

        // get the template
        val template: Template
        try {
            template = Template(templateName, StringReader(textTemplate), freemarkerConfig)
        } catch (e: ParseException) {
            val msg = ("Failure while parsing the Doc template (" + templateName + "), template is invalid: " + e
                    + " :: template=" + textTemplate)
            logger.error(msg)
            throw RuntimeException(msg, e)
        } catch (e: IOException) {
            throw RuntimeException("Failure while creating freemarker template", e)
        }

        // process the template
        var result: String
        try {
            val output = StringWriter()
            template.process(data, output)
            result = output.toString()
            logger.debug("Generated complete document ({} chars) from template ({})", result.length, templateName)
        } catch (e: TemplateException) {
            logger.error("Failed while processing the Doc template ({}): {}", templateName, e)
            result = ("ERROR:: Failed while processing the template (" + templateName + "): " + e + "\n Template: "
                    + textTemplate + "\n Data: " + data)
        } catch (e: IOException) {
            throw RuntimeException("Failure while sending freemarker output to stream", e)
        }

        return result
    }

    /**
     * Use this method to generate the documentation using passed in document data
     *
     * @param data
     * any populated DocData object
     * @return the documentation (e.g. REST html) as a string
     * @throws IllegalArgumentException
     * if the input data is invalid in some way
     * @see DocData
     */
    fun generate(data: DocData): String {
        val template = loadTemplate(data.defaultTemplatePath)
        return generate(data, template)
    }

    /**
     * Use this method to generate the documentation using passed in document data, allows the user to specify the
     * template that is used
     *
     * @param data
     * any populated DocData object
     * @param template
     * any freemarker template which works with the DocData data structure
     * @return the documentation (e.g. REST html) as a string
     * @throws IllegalArgumentException
     * if the input data is invalid in some way
     * @see DocData
     */
    fun generate(data: DocData, template: String?): String {
        if (template == null) {
            throw IllegalArgumentException("template must be set")
        }
        return processTextTemplate(data.getMetaData("name"), template, data.toMap())
    }

    /**
     * Loads a template based on the given path
     *
     * @param path
     * the path to load the template from (uses the current classloader)
     * @return the template as a string
     */
    fun loadTemplate(path: String): String? {
        var textTemplate: String?
        var `in`: InputStream? = null
        try {
            `in` = DocUtil::class.java!!.getResourceAsStream(path)
            if (`in` == null) {
                throw NullPointerException("No template file could be found at: $path")
            }
            textTemplate = String(IOUtils.toByteArray(`in`!!))
        } catch (e: Exception) {
            logger.error("failed to load template file from path ($path): $e", e)
            textTemplate = null
        } finally {
            IOUtils.closeQuietly(`in`)
        }
        return textTemplate
    }

}
/** Disable construction of this utility class  */
