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
import java.lang.String.format

import org.opencastproject.job.api.JobContext
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.api.WorkflowService

import com.entwinemedia.fn.Fn2

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashMap

/**
 * Workflow operation handler that will conditionally insert a complete workflow into the current one
 * at its own position.
 */
class IncludeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The workflow service instance  */
    private var workflowService: WorkflowService? = null

    /**
     * {@inheritDoc}
     *
     */
    public override fun activate(componentContext: ComponentContext) {
        super.activate(componentContext)
    }

    /**
     * {@inheritDoc}
     *
     */
    @Throws(WorkflowOperationException::class)
    override fun start(wi: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val workflowDefinitionId = getConfig(wi, WORKFLOW_CFG)
        insertWorkflow(wi, workflowDefinitionId)
        // Return all existing workflow parameters with the result object to
        // make the workflow service replace the variables again. This is
        // necessary to 'propagate' the parameters to the included workflow.
        val props = `$`(wi.configurationKeys).foldl(HashMap(), object : Fn2<HashMap<String, String>, String, HashMap<String, String>>() {
            override fun apply(sum: HashMap<String, String>, key: String): HashMap<String, String> {
                sum[key] = wi.getConfiguration(key)
                return sum
            }
        })
        return createResult(wi.mediaPackage, props, Action.CONTINUE, 0)
    }

    /**
     * Adds the operations found in the workflow defined by `workflowDefinitionId` to the workflow instance and
     * returns `true` if everything worked fine, `false` otherwise.
     *
     * @param wi
     * the instance to insert the workflow identified by `workflowDefinitionId` into
     * @param workflowDefinitionId
     * id of the workflow definition to insert
     * @throws WorkflowOperationException
     * in case of any error
     */
    @Throws(WorkflowOperationException::class)
    fun insertWorkflow(wi: WorkflowInstance, workflowDefinitionId: String) {
        try {
            val definition = workflowService!!.getWorkflowDefinitionById(workflowDefinitionId)
            if (definition != null) {
                logger.info(format("Insert workflow %s into the current workflow instance", workflowDefinitionId))
                wi.insert(definition, wi.currentOperation)
            } else {
                logger.warn(format("Workflow definition %s cannot be found", workflowDefinitionId))
            }
        } catch (e: Exception) {
            throw WorkflowOperationException("Error inserting workflow $workflowDefinitionId", e)
        }

    }

    /**
     * OSGi DI.
     */
    fun setWorkflowService(service: WorkflowService) {
        this.workflowService = service
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(IncludeWorkflowOperationHandler::class.java)

        /** Configuration value for the workflow operation definition  */
        val WORKFLOW_CFG = "workflow-id"
    }
}
