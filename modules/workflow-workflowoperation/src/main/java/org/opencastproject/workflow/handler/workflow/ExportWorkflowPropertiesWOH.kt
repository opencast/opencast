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
package org.opencastproject.workflow.handler.workflow

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesElementFromMediaPackage
import org.opencastproject.workflow.handler.workflow.ImportWorkflowPropertiesWOH.loadPropertiesFromXml

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.util.MimeTypes
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings

import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.util.Properties
import java.util.UUID

/**
 * Workflow operation handler for exporting workflow properties.
 */
class ExportWorkflowPropertiesWOH : AbstractWorkflowOperationHandler() {

    /** The workspace  */
    private var workspace: Workspace? = null

    /** OSGi DI  */
    internal fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.info("Start exporting workflow properties for workflow {}", workflowInstance)
        val mediaPackage = workflowInstance.mediaPackage
        val keys = `$`(getOptConfig(workflowInstance, KEYS_PROPERTY)).bind(Strings.splitCsv).toSet()
        val targetFlavorString = getOptConfig(workflowInstance, TARGET_FLAVOR_PROPERTY).getOr(DEFAULT_TARGET_FLAVOR)
        val targetTags = `$`(getOptConfig(workflowInstance, TARGET_TAGS_PROPERTY)).bind(Strings.splitCsv)
        val targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorString)

        // Read optional existing workflow properties from mediapackage
        var workflowProps = Properties()
        val existingPropsElem = loadPropertiesElementFromMediaPackage(targetFlavor, workflowInstance)
        if (existingPropsElem.isSome) {
            workflowProps = loadPropertiesFromXml(workspace!!, existingPropsElem.get().getURI())

            // Remove specified keys
            for (key in keys)
                workflowProps.remove(key)
        }

        // Extend with specified properties
        for (key in workflowInstance.configurationKeys) {
            if (keys.isEmpty() || keys.contains(key))
                workflowProps[key] = workflowInstance.getConfiguration(key)
        }

        // Store properties as an attachment
        var attachment: Attachment
        try {
            ByteArrayOutputStream().use { out ->
                workflowProps.storeToXML(out, null, "UTF-8")
                val elementId = UUID.randomUUID().toString()
                val uri = workspace!!.put(mediaPackage.identifier.compact(), elementId, EXPORTED_PROPERTIES_FILENAME,
                        ByteArrayInputStream(out.toByteArray()))
                val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                attachment = builder.elementFromURI(uri, Attachment.TYPE, targetFlavor) as Attachment
                attachment.mimeType = MimeTypes.XML
            }
        } catch (e: IOException) {
            logger.error("Unable to store workflow properties as Attachment with flavor '{}': {}", targetFlavorString,
                    ExceptionUtils.getStackTrace(e))
            throw WorkflowOperationException("Unable to store workflow properties as Attachment", e)
        }

        // Add the target tags
        for (tag in targetTags) {
            logger.trace("Tagging with '{}'", tag)
            attachment.addTag(tag)
        }

        // Update attachment
        if (existingPropsElem.isSome)
            mediaPackage.remove(existingPropsElem.get())
        mediaPackage.add(attachment)

        logger.info("Added properties from {} as Attachment with flavor {}", workflowInstance, targetFlavorString)

        logger.debug("Workflow properties: {}", propertiesAsString(workflowProps))

        return createResult(mediaPackage, null, Action.CONTINUE, 0)
    }

    /** Serialize the properties into a string.  */
    private fun propertiesAsString(properties: Properties): String {
        val writer = StringWriter()
        properties.list(PrintWriter(writer))
        return writer.buffer.toString()
    }

    companion object {

        /** Configuration options  */
        val KEYS_PROPERTY = "keys"
        val TARGET_FLAVOR_PROPERTY = "target-flavor"
        val TARGET_TAGS_PROPERTY = "target-tags"

        val DEFAULT_TARGET_FLAVOR = MediaPackageElements.PROCESSING_PROPERTIES.toString()
        val EXPORTED_PROPERTIES_FILENAME = "processing-properties.xml"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ExportWorkflowPropertiesWOH::class.java)
    }

}
