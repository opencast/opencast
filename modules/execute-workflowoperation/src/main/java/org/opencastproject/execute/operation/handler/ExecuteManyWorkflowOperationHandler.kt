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

import java.io.IOException
import java.net.URI
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.Map.Entry

/**
 * Runs an operation multiple times with each MediaPackageElement matching the characteristics
 */
class ExecuteManyWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

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
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

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
                logger.warn("Ignoring invalid load value '{}' on execute operation with description '{}'", loadPropertyStr,
                        description)
            }

        }
        val sourceFlavor = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR_PROPERTY))
        val sourceTags = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAGS_PROPERTY))
        val targetFlavorStr = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR_PROPERTY))
        val targetTags = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAGS_PROPERTY))
        val outputFilename = StringUtils.trimToNull(operation.getConfiguration(OUTPUT_FILENAME_PROPERTY))
        val expectedTypeStr = StringUtils.trimToNull(operation.getConfiguration(EXPECTED_TYPE_PROPERTY))

        var matchingFlavor: MediaPackageElementFlavor? = null
        if (sourceFlavor != null)
            matchingFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavor)

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

        val sourceTagList = asList(sourceTags)

        // Select the tracks based on source flavors and tags
        val inputSet = HashSet<MediaPackageElement>()
        for (element in mediaPackage.getElementsByTags(sourceTagList)) {
            val elementFlavor = element.flavor
            if (sourceFlavor == null || elementFlavor != null && elementFlavor.matches(matchingFlavor)) {
                inputSet.add(element)
            }
        }

        if (inputSet.size == 0) {
            logger.warn("Mediapackage {} has no suitable elements to execute the command {} based on tags {} and flavor {}",
                    mediaPackage, exec, sourceTags, sourceFlavor)
            return createResult(mediaPackage, Action.CONTINUE)
        }

        val inputElements = inputSet.toTypedArray()

        try {
            val jobs = arrayOfNulls<Job>(inputElements.size)
            val resultElements = arrayOfNulls<MediaPackageElement>(inputElements.size)
            var totalTimeInQueue: Long = 0

            for (i in inputElements.indices)
                jobs[i] = executeService.execute(exec, params, inputElements[i], outputFilename, expectedType!!, load)

            // Wait for all jobs to be finished
            if (!waitForStatus(*jobs).isSuccess)
                throw WorkflowOperationException("Execute operation failed")

            // Find which output elements are tracks and inspect them
            val jobMap = HashMap<Int, Job>()
            for (i in jobs.indices) {
                // Add this job's queue time to the total
                totalTimeInQueue += jobs[i].queueTime!!
                if (StringUtils.trimToNull(jobs[i].payload) != null) {
                    resultElements[i] = MediaPackageElementParser.getFromXml(jobs[i].payload)
                    if (resultElements[i].elementType === MediaPackageElement.Type.Track) {
                        jobMap[i] = inspectionService!!.inspect(resultElements[i].getURI())
                    }
                } else
                    resultElements[i] = inputElements[i]
            }

            if (jobMap.size > 0) {
                if (!waitForStatus(*jobMap.values.toTypedArray()).isSuccess)
                    throw WorkflowOperationException("Execute operation failed in track inspection")

                for ((key, value) in jobMap) {
                    // Add this job's queue time to the total
                    totalTimeInQueue += value.queueTime!!
                    resultElements[key] = MediaPackageElementParser.getFromXml(value.payload)
                }
            }

            for (i in resultElements.indices) {
                if (resultElements[i] !== inputElements[i]) {
                    // Store new element to mediaPackage
                    mediaPackage.addDerived(resultElements[i], inputElements[i])
                    // Store new element to mediaPackage
                    val uri = workspace.moveTo(resultElements[i].getURI(), mediaPackage.identifier.toString(),
                            resultElements[i].identifier, outputFilename)

                    resultElements[i].setURI(uri)

                    // Set new flavor
                    if (targetFlavor != null)
                        resultElements[i].flavor = targetFlavor
                }

                // Set new tags
                if (targetTags != null) {
                    // Assume the tags starting with "-" means we want to eliminate such tags form the result element
                    for (tag in asList(targetTags)) {
                        if (tag.startsWith("-"))
                        // We remove the tag resulting from stripping all the '-' characters at the beginning of the tag
                            resultElements[i].removeTag(tag.replace("^-+".toRegex(), ""))
                        else
                            resultElements[i].addTag(tag)
                    }
                }
            }

            val result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
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
            throw WorkflowOperationException("Error inspecting one of the created tracks", e)
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
        executeService = service
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
        inspectionService = mediaInspectionService
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ExecuteManyWorkflowOperationHandler::class.java)

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

        /**
         * Property containing the tags that must exist on a mediapackage element for the element to be used as an input
         * arguments
         */
        val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Property containing the flavor that the resulting mediapackage elements will be assigned  */
        val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Property containing the tags that the resulting mediapackage elements will be assigned  */
        val TARGET_TAGS_PROPERTY = "target-tags"
    }
}
