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
import org.opencastproject.workflow.api.RetryStrategy
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationAbortedException
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Workflow operation handler for choosing the retry strategy after a failing operation
 */
class ErrorResolutionWorkflowOperationHandler : ResumableWorkflowOperationHandlerBase() {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.handler.workflow.ResumableWorkflowOperationHandlerBase.activate
     */
    override fun activate(componentContext: ComponentContext) {
        super.activate(componentContext)
        setHoldActionTitle("Select retry strategy")
        registerHoldStateUserInterface(HOLD_UI_PATH)
        logger.info("Registering retry strategy failover hold state ui from classpath {}", HOLD_UI_PATH)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler.resume
     */
    @Throws(WorkflowOperationException::class)
    override fun resume(workflowInstance: WorkflowInstance, context: JobContext,
                        properties: Map<String, String>): WorkflowOperationResult {

        val strategyValue = properties[OPT_STRATEGY]
        if (StringUtils.isBlank(strategyValue)) {
            logger.warn("No retry strategy submitted for workflow '{}', holding again", workflowInstance)
            return createResult(null, properties, Action.PAUSE, 0)
        }

        try {
            val s = RetryStrategy.valueOf(strategyValue)
            when (s) {
                RetryStrategy.NONE -> {
                    logger.info("Error resolution 'fail' was triggered for workflow '{}'", workflowInstance)
                    throw WorkflowOperationAbortedException("Workflow $workflowInstance was failed by user")
                }
                RetryStrategy.RETRY -> {
                    logger.info("Error resolution 'retry' was triggered for workflow '{}'", workflowInstance)
                    return createResult(null, properties, Action.CONTINUE, 0)
                }
                else -> {
                    logger.warn("Unknown retry strategy '{}' submitted for workflow '{}'", strategyValue, workflowInstance)
                    return createResult(null, properties, Action.PAUSE, 0)
                }
            }
        } catch (e: IllegalArgumentException) {
            logger.warn("Unknown retry strategy '{}' submitted for workflow '{}'", strategyValue, workflowInstance)
            return createResult(null, properties, Action.PAUSE, 0)
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ErrorResolutionWorkflowOperationHandler::class.java)

        /** Path to the caption upload ui resources  */
        private val HOLD_UI_PATH = "/ui/operation/retry-strategy/index.html"

        /** Parameter name  */
        private val OPT_STRATEGY = "retryStrategy"
    }

}
