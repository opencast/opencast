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

package org.opencastproject.workflow.handler.logging

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.api.WorkflowParser
import org.opencastproject.workflow.api.WorkflowParsingException

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/**
 * The `LoggingWorkflowOperationHandlerTest` will log the current state of a workflow instance and its media
 * package at a given point of a workflow.
 */
class LoggingWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val operation = workflowInstance.currentOperation

        val directoryPath = StringUtils.trimToNull(operation.getConfiguration("directory"))
        var directory: File? = null
        if (directoryPath != null) {
            directory = File(directoryPath)
            try {
                FileUtils.forceMkdir(directory)
            } catch (e: IOException) {
                throw WorkflowOperationException(String.format("Failed to create output directory '%s'", directoryPath), e)
            }

        }


        var logMediaPackageJSON = BooleanUtils.toBoolean(operation.getConfiguration("mediapackage-json"))
        val logMediaPackageXML = BooleanUtils.toBoolean(operation.getConfiguration("mediapackage-xml"))
        val logWorkflowInstance = BooleanUtils.toBoolean(operation.getConfiguration("workflowinstance-xml"))

        if (!(logMediaPackageJSON || logMediaPackageXML || logWorkflowInstance)) {
            logMediaPackageJSON = true
        }

        val mediaPackage = workflowInstance.mediaPackage

        // Log media package as JSON
        if (logMediaPackageJSON) {
            val filename = String.format("workflow-%d-%d-mediapackage-%s.json", workflowInstance.id,
                    operation.id, mediaPackage.identifier)
            saveOrLog(MediaPackageParser.getAsJSON(mediaPackage), directory, filename)
        }

        // Log media package as XML
        if (logMediaPackageXML) {
            val filename = String.format("workflow-%d-%d-mediapackage-%s.xml", workflowInstance.id,
                    operation.id, mediaPackage.identifier)
            saveOrLog(MediaPackageParser.getAsXml(mediaPackage), directory, filename)
        }

        // Log workflow instance as XML
        if (logWorkflowInstance) {
            val filename = String.format("workflow-%d-%d.xml", workflowInstance.id, operation.id)
            try {
                saveOrLog(WorkflowParser.toXml(workflowInstance), directory, filename)
            } catch (e: WorkflowParsingException) {
                throw WorkflowOperationException(e)
            }

        }

        // Continue with unmodified media package
        return createResult(mediaPackage, Action.CONTINUE)
    }

    /**
     * Log data to directory if set, otherwise use the system logger.
     *
     * @param data
     * Data to log
     * @param directory
     * Directory to log to. If set to null, the default logger is used.
     * @param filename
     * Filename to write the data to. Used by internal logger as well.
     * @throws WorkflowOperationException
     * In case the data could not be written
     */
    @Throws(WorkflowOperationException::class)
    private fun saveOrLog(data: String, directory: File?, filename: String) {
        // Write to directory if set
        if (directory != null) {
            val file = File(directory, filename)
            try {
                logger.info("Logging current workflow state to to {}", file.absolutePath)
                FileUtils.writeStringToFile(file, data, Charset.defaultCharset())
            } catch (e: IOException) {
                throw WorkflowOperationException(e)
            }

        } else {
            // …otherwise, just use the logger
            logger.info("({}):\n{}", filename, data)
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(LoggingWorkflowOperationHandler::class.java)
    }


}
