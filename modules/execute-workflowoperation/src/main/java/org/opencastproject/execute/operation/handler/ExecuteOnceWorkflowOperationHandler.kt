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

package org.opencastproject.execute.operation.handler

import org.opencastproject.execute.api.ExecuteException
import org.opencastproject.execute.api.ExecuteService
import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.api.WorkflowOperationResultImpl
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.HashMap
import java.util.Properties

/**
 * Runs an operation once with using elements within a certain MediaPackage as parameters
 */
open class ExecuteOnceWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The text analyzer  */
    protected var executeService: ExecuteService

    /** Reference to the media inspection service  */
    private var inspectionService: MediaInspectionService? = null

    /** The workspace service  */
    protected var workspace: Workspace

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult? {

        val mediaPackage = workflowInstance.mediaPackage
        val operation = workflowInstance.currentOperation

        logger.debug("Running execute workflow operation with ID {}", operation.id)

        // Get operation parameters
        val exec = StringUtils.trimToNull(operation.getConfiguration(EXEC_PROPERTY))
        val params = StringUtils.trimToNull(operation.getConfiguration(PARAMS_PROPERTY))
        var load = 1.0f
        val loadPropertyStr = StringUtils.trimToEmpty(operation.getConfiguration(LOAD_PROPERTY))
        if (StringUtils.isNotBlank(loadPropertyStr)) {
            try {
                load = java.lang.Float.parseFloat(loadPropertyStr)
            } catch (e: NumberFormatException) {
                val description = StringUtils.trimToEmpty(operation.description)
                logger.warn("Ignoring invalid load value '{}' on execute operation with description '{}'", loadPropertyStr, description)
            }

        }
        val targetFlavorStr = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR_PROPERTY))
        val targetTags = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS_PROPERTY))
        val outputFilename = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_FILENAME_PROPERTY))
        val expectedTypeStr = StringUtils.trimToNull(operation.getConfiguration(EXPECTED_TYPE_PROPERTY))

        val setWfProps = java.lang.Boolean.valueOf(StringUtils.trimToNull(operation.getConfiguration(SET_WF_PROPS_PROPERTY)))

        // Unmarshall target flavor
        var targetFlavor: MediaPackageElementFlavor? = null
        if (targetFlavorStr != null)
            targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavorStr)

        // Unmarshall expected mediapackage element type
        var expectedType: MediaPackageElement.Type? = null
        if (expectedTypeStr != null) {
            for (type in MediaPackageElement.Type.values())
                if (type.toString().equals(expectedTypeStr, ignoreCase = true)) {
                    expectedType = type
                    break
                }

            if (expectedType == null)
                throw WorkflowOperationException("'$expectedTypeStr' is not a valid element type")
        }

        // Process the result element
        var resultElement: MediaPackageElement? = null

        try {
            val job = executeService.execute(exec, params, mediaPackage, outputFilename, expectedType!!, load)

            var result: WorkflowOperationResult? = null

            // Wait for all jobs to be finished
            if (!waitForStatus(job).isSuccess)
                throw WorkflowOperationException("Execute operation failed")

            if (StringUtils.isNotBlank(job.payload)) {

                if (setWfProps) {
                    // The job payload is a file with set of properties for the workflow
                    resultElement = MediaPackageElementParser.getFromXml(job.payload)

                    val properties = Properties()
                    val propertiesFile = workspace.get(resultElement.getURI())
                    FileInputStream(propertiesFile).use { `is` -> properties.load(`is`) }
                    logger.debug("Loaded {} properties from {}", properties.size, propertiesFile)
                    workspace.deleteFromCollection(ExecuteService.COLLECTION, propertiesFile.name)

                    val wfProps = HashMap<String, String>(properties as Map<*, *>)

                    result = createResult(mediaPackage, wfProps, Action.CONTINUE, job.queueTime!!)
                } else {
                    // The job payload is a new element for the MediaPackage
                    resultElement = MediaPackageElementParser.getFromXml(job.payload)

                    if (resultElement.elementType === MediaPackageElement.Type.Track) {
                        // Have the track inspected and return the result
                        var inspectionJob: Job? = null
                        inspectionJob = inspectionService!!.inspect(resultElement.getURI())
                        val barrier = JobBarrier(job, serviceRegistry, inspectionJob!!)
                        if (!barrier.waitForJobs()!!.isSuccess) {
                            throw ExecuteException("Media inspection of " + resultElement.getURI() + " failed")
                        }

                        resultElement = MediaPackageElementParser.getFromXml(inspectionJob.payload)
                    }

                    // Store new element to mediaPackage
                    mediaPackage.add(resultElement)
                    val uri = workspace.moveTo(resultElement.getURI(), mediaPackage.identifier.toString(),
                            resultElement.identifier, outputFilename)
                    resultElement.setURI(uri)

                    // Set new flavor
                    if (targetFlavor != null)
                        resultElement.flavor = targetFlavor

                    // Set new tags
                    if (targetTags != null) {
                        // Assume the tags starting with "-" means we want to eliminate such tags form the result element
                        for (tag in asList(targetTags)) {
                            if (tag.startsWith("-"))
                            // We remove the tag resulting from stripping all the '-' characters at the beginning of the tag
                                resultElement.removeTag(tag.replace("^-+".toRegex(), ""))
                            else
                                resultElement.addTag(tag)
                        }
                    }
                    result = createResult(mediaPackage, Action.CONTINUE, job.queueTime!!)
                }
            } else {
                // Payload is empty
                result = createResult(mediaPackage, Action.CONTINUE, job.queueTime!!)
            }

            logger.debug("Execute operation {} completed", operation.id)

            return result

        } catch (e: ExecuteException) {
            throw WorkflowOperationException(e)
        } catch (e: MediaPackageException) {
            throw WorkflowOperationException("Some result element couldn't be serialized", e)
        } catch (e: NotFoundException) {
            throw WorkflowOperationException("Could not find mediapackage", e)
        } catch (e: IOException) {
            throw WorkflowOperationException("Error unmarshalling a result mediapackage element", e)
        } catch (e: MediaInspectionException) {
            throw WorkflowOperationException("Media inspection of " + resultElement!!.getURI() + " failed", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.skip
     */
    @Throws(WorkflowOperationException::class)
    override fun skip(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        return WorkflowOperationResultImpl(workflowInstance.mediaPackage, null, Action.SKIP, 0)
    }

    override fun getId(): String {
        return "execute"
    }

    override fun getDescription(): String {
        return "Executes command line workflow operations in workers"
    }

    @Throws(WorkflowOperationException::class)
    override fun destroy(workflowInstance: WorkflowInstance, context: JobContext) {
        // Do nothing (nothing to clean up, the command line program should do this itself)
    }

    /**
     * Sets the service
     *
     * @param service
     */
    fun setExecuteService(service: ExecuteService) {
        this.executeService = service
    }

    /**
     * Sets a reference to the workspace service.
     *
     * @param workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Sets the media inspection service
     *
     * @param mediaInspectionService
     * an instance of the media inspection service
     */
    protected fun setMediaInspectionService(mediaInspectionService: MediaInspectionService) {
        this.inspectionService = mediaInspectionService
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ExecuteOnceWorkflowOperationHandler::class.java)

        /** Property containing the command to run  */
        val EXEC_PROPERTY = "exec"

        /** Property containing the list of command parameters  */
        val PARAMS_PROPERTY = "params"

        /** Property containingn an approximation of the load imposed by running this operation  */
        val LOAD_PROPERTY = "load"

        /** Property containing the "flavor" that a mediapackage elements must have in order to be used as input arguments  */
        val SOURCE_FLAVOR_PROPERTY = "source-flavor"

        /** Property containing the filename of the elements created by this operation  */
        val OUTPUT_FILENAME_PROPERTY = "output-filename"

        /** Property containing the expected type of the element generated by this operation  */
        val EXPECTED_TYPE_PROPERTY = "expected-type"

        /** Property containing the tags that must exist on a mediapackage element for the element to be used as an input arguments  */
        val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Property containing the flavor that the resulting mediapackage elements will be assigned  */
        val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Property containing the tags that the resulting mediapackage elements will be assigned  */
        val TARGET_TAGS_PROPERTY = "target-tags"

        /** Property to control whether command output will be used to set workflow properties  */
        val SET_WF_PROPS_PROPERTY = "set-workflow-properties"
    }
}
