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
package org.opencastproject.workflow.handler.distribution

import org.opencastproject.util.RequireUtil.notNull

import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * WOH that retracts elements from an internal distribution channel and removes the reflective publication elements from
 * the media package.
 */
class ConfigurableRetractWorkflowOperationHandler : ConfigurableWorkflowOperationHandlerBase() {
    // service references
    private var distributionService: DownloadDistributionService? = null

    /** OSGi DI  */
    internal fun setDownloadDistributionService(distributionService: DownloadDistributionService) {
        this.distributionService = distributionService
    }

    override fun getDistributionService(): DownloadDistributionService {
        assert(distributionService != null)
        return distributionService
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        notNull(workflowInstance, "workflowInstance")

        val mp = workflowInstance.mediaPackage
        val channelId = StringUtils.trimToEmpty(workflowInstance.currentOperation.getConfiguration(
                CHANNEL_ID_KEY))
        if (StringUtils.isBlank(channelId)) {
            throw WorkflowOperationException("Unable to publish this mediapackage as the configuration key "
                    + CHANNEL_ID_KEY + " is missing. Unable to determine where to publish these elements.")
        }

        retract(mp, channelId)

        return createResult(mp, Action.CONTINUE)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ConfigurableRetractWorkflowOperationHandler::class.java)
        private val CHANNEL_ID_KEY = "channel-id"
    }

}
