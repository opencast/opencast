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

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID

/**
 * Workflow operation handler for cloning tracks from a flavor
 */
class CloneWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The workspace reference  */
    protected var workspace: Workspace? = null

    /**
     * Callback for the OSGi environment to set the workspace reference.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * {@inheritDoc}
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running clone workflow operation on workflow {}", workflowInstance.id)

        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation

        // Check which tags have been configured
        val sourceTagsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_TAGS))
        var sourceFlavorOption: String? = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_FLAVOR))
        val targetFlavorOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_TARGET_FLAVOR))

        val elementSelector = SimpleElementSelector()

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorOption)) {
            logger.info("No source tags or flavors have been specified, not matching anything. Operation will be skipped.")
            return createResult(mediaPackage, Action.SKIP)
        }

        // if no source-favor is specified, all flavors will be checked for given tags
        if (sourceFlavorOption == null) {
            sourceFlavorOption = "*/*"
        }

        val sb = StringBuilder()
        sb.append("Parameters passed to clone workflow operation:")
        sb.append("\n source-tags: ").append(sourceTagsOption)
        sb.append("\n source-flavor: ").append(sourceFlavorOption)
        sb.append("\n target-flavor: ").append(targetFlavorOption)
        logger.debug(sb.toString())

        // Select the source flavors
        val sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorOption)
        elementSelector.addFlavor(sourceFlavor)

        // Select the source tags
        for (tag in asList(sourceTagsOption)) {
            elementSelector.addTag(tag)
        }

        // Look for elements matching the tags and the flavor
        val elements = elementSelector.select(mediaPackage, true)

        // Check the the number of element returned
        if (elements.size == 0) {
            // If no one found, we skip the operation
            logger.debug("No matching elements found, skipping operation.")
            return createResult(workflowInstance.mediaPackage, Action.SKIP)
        } else {
            logger.debug("Copy " + elements.size + " elements to new flavor: {}", targetFlavorOption)

            val targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorOption)

            for (element in elements) {
                // apply the target flavor to the element
                val flavor = targetFlavor.applyTo(element.flavor)

                // Copy element and set new flavor
                val newElement = copyElement(element)
                newElement.flavor = flavor
                mediaPackage.add(newElement)
            }
        }

        return createResult(workflowInstance.mediaPackage, Action.CONTINUE)
    }

    @Throws(WorkflowOperationException::class)
    private fun copyElement(element: MediaPackageElement): MediaPackageElement {
        val elementId = UUID.randomUUID().toString()
        val newElement = element.clone() as MediaPackageElement
        newElement.identifier = elementId

        var sourceFile: File? = null
        var toFileName: String? = null
        try {
            val sourceURI = element.getURI()
            sourceFile = workspace!!.get(sourceURI)

            toFileName = elementId
            val extension = FilenameUtils.getExtension(sourceFile!!.name)
            if ("" != extension)
                toFileName += ".$extension"

            logger.debug("Start copying element {} to target {}.", sourceFile.path, toFileName)

            val newUri = workspace!!.put(element.mediaPackage.identifier.toString(), newElement.identifier,
                    toFileName, workspace!!.read(sourceURI))
            newElement.setURI(newUri)
            newElement.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, workspace!!.get(newUri))

            logger.debug("Element {} copied to target {}.", sourceFile.path, toFileName)
        } catch (e: IOException) {
            throw WorkflowOperationException("Unable to copy " + sourceFile!!.path + " to " + toFileName + ".", e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException("Unable to find " + element.getURI() + " in the workspace", e)
        }

        return newElement
    }

    companion object {

        /** Configuration key for the \"source-flavor\" of the track to use as a source input  */
        val OPT_SOURCE_FLAVOR = "source-flavor"

        /** Configuration key for the \"source-tag\" of the track to use as a source input  */
        val OPT_SOURCE_TAGS = "source-tags"

        /** Configuration key for the target-flavor  */
        val OPT_TARGET_FLAVOR = "target-flavor"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CloneWorkflowOperationHandler::class.java)
    }

}
