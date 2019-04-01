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

import java.lang.String.format

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.util.FileSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
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

/**
 * Workflow operation handler for copying video data through NFS
 */
class CopyWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
        logger.debug("Running copy workflow operation on workflow {}", workflowInstance.id)

        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation

        // Check which tags have been configured
        val sourceTagsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_TAGS))
        val sourceFlavorsOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_SOURCE_FLAVORS))
        val targetDirectoryOption = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_TARGET_DIRECTORY))
        val targetFilenameOption = Option.option(StringUtils.trimToNull(currentOperation
                .getConfiguration(OPT_TARGET_FILENAME)))

        val sb = StringBuilder()
        sb.append("Parameters passed to copy workflow operation:")
        sb.append("\n source-tags: ").append(sourceTagsOption)
        sb.append("\n source-flavors: ").append(sourceFlavorsOption)
        sb.append("\n target-directory: ").append(targetDirectoryOption)
        sb.append("\n target-filename: ").append(targetFilenameOption)
        logger.debug(sb.toString())

        val elementSelector = SimpleElementSelector()

        // Make sure either one of tags or flavors are provided
        if (StringUtils.isBlank(sourceTagsOption) && StringUtils.isBlank(sourceFlavorsOption)) {
            logger.info("No source tags or flavors have been specified, not matching anything")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Make the target filename and directory are provided
        if (StringUtils.isBlank(targetDirectoryOption))
            throw WorkflowOperationException("No target directory has been set for the copy operation!")

        // Select the source flavors
        for (flavor in asList(sourceFlavorsOption)) {
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Source flavor '$flavor' is malformed")
            }

        }

        // Select the source tags
        for (tag in asList(sourceTagsOption)) {
            elementSelector.addTag(tag)
        }

        // Look for elements matching the tag
        val elements = elementSelector.select(mediaPackage, true)

        // Check the the number of element returned
        if (elements.size == 0) {
            // If no one found, we skip the operation
            return createResult(workflowInstance.mediaPackage, Action.SKIP)
        } else if (elements.size == 1) {
            for (element in elements) {
                logger.debug("Copy single element to: {}", targetDirectoryOption)
                val fileName: String
                if (targetFilenameOption.isSome) {
                    fileName = targetFilenameOption.get()
                } else {
                    fileName = FilenameUtils.getBaseName(element.getURI().toString())
                }

                var ext = FilenameUtils.getExtension(element.getURI().toString())
                ext = if (ext.length > 0) ".$ext" else ""
                val targetFile = File(UrlSupport.concat(targetDirectoryOption, fileName + ext))

                copyElement(element, targetFile)
            }
        } else {
            logger.debug("Copy multiple elements to: {}", targetDirectoryOption)
            var i = 1
            for (element in elements) {
                val fileName: String
                if (targetFilenameOption.isSome) {
                    fileName = String.format(targetFilenameOption.get(), i)
                } else {
                    fileName = FilenameUtils.getBaseName(element.getURI().toString())
                }

                var ext = FilenameUtils.getExtension(element.getURI().toString())
                ext = if (ext.length > 0) ".$ext" else ""
                val targetFile = File(UrlSupport.concat(targetDirectoryOption, fileName + ext))

                copyElement(element, targetFile)
                i++
            }
        }

        return createResult(workflowInstance.mediaPackage, Action.CONTINUE)
    }

    @Throws(WorkflowOperationException::class)
    private fun copyElement(element: MediaPackageElement, targetFile: File) {
        val sourceFile: File
        try {
            sourceFile = workspace!!.get(element.getURI())
        } catch (e: NotFoundException) {
            throw WorkflowOperationException("Unable to find " + element.getURI() + " in the workspace", e)
        } catch (e: IOException) {
            throw WorkflowOperationException("Error loading " + element.getURI() + " from the workspace", e)
        }

        logger.debug("Start copying element {} to target {}.", sourceFile.path, targetFile.path)
        try {
            FileSupport.copy(sourceFile, targetFile)
        } catch (e: IOException) {
            throw WorkflowOperationException(format("Unable to copy %s to %s: %s", sourceFile.path,
                    targetFile.path, e.message))
        }

        logger.debug("Element {} copied to target {}.", sourceFile.path, targetFile.path)
    }

    companion object {

        /** Configuration key for the \"tag\" of the track to use as a source input  */
        val OPT_SOURCE_TAGS = "source-tags"

        /** Configuration key for the \"flavor\" of the track to use as a source input  */
        val OPT_SOURCE_FLAVORS = "source-flavors"

        /** Configuration key for the directory where the file must be delivered  */
        val OPT_TARGET_DIRECTORY = "target-directory"

        /** Configuration key for the name of the target file  */
        val OPT_TARGET_FILENAME = "target-filename"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CopyWorkflowOperationHandler::class.java)
    }

}
