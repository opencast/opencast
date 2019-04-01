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
import org.apache.commons.lang3.StringUtils.trimToEmpty

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowService

import com.entwinemedia.fn.data.Opt

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashMap

/**
 * This WOH starts a new workflow for given media package.
 */
class StartWorkflowWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    private var assetManager: AssetManager? = null

    private var workflowService: WorkflowService? = null

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param assetManager
     * the asset manager
     */
    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param workflowService
     * the workflow service
     */
    fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val operation = workflowInstance.currentOperation
        val configuredMediaPackageID = trimToEmpty(operation.getConfiguration(MEDIA_PACKAGE_ID))
        val configuredWorkflowDefinition = trimToEmpty(operation.getConfiguration(WORKFLOW_DEFINITION))

        // Get media package
        val mpOpt = assetManager!!.getMediaPackage(configuredMediaPackageID)
        if (mpOpt.isNone) {
            throw WorkflowOperationException(format("Media package %s not found", configuredMediaPackageID))
        }
        val mp = mpOpt.get()

        // Get workflow parameter
        val properties = HashMap<String, String>()
        for (key in operation.configurationKeys) {
            if (MEDIA_PACKAGE_ID == key || WORKFLOW_DEFINITION == key) {
                continue
            }
            properties[key] = operation.getConfiguration(key)
        }

        try {
            // Get workflow definition
            val workflowDefinition = workflowService!!.getWorkflowDefinitionById(
                    configuredWorkflowDefinition)

            // Start workflow
            logger.info("Starting '{}' workflow for media package '{}'", configuredWorkflowDefinition,
                    configuredMediaPackageID)
            workflowService!!.start(workflowDefinition, mp, properties)

        } catch (e: NotFoundException) {
            throw WorkflowOperationException(format("Workflow Definition '%s' not found", configuredWorkflowDefinition))
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

        return createResult(WorkflowOperationResult.Action.CONTINUE)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartWorkflowWorkflowOperationHandler::class.java)

        /** Name of the configuration option that provides the media package ID  */
        val MEDIA_PACKAGE_ID = "media-package"

        /** Name of the configuration option that provides the workflow definition ID  */
        val WORKFLOW_DEFINITION = "workflow-definition"
    }
}
