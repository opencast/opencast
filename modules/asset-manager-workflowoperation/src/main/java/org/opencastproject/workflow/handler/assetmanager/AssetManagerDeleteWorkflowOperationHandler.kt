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
package org.opencastproject.workflow.handler.assetmanager

import java.lang.String.format
import org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.BooleanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Workflow operation for deleting an episode from the asset manager.
 *
 * @see AssetManager
 */
class AssetManagerDeleteWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The archive  */
    private var assetManager: AssetManager? = null

    /** OSGi DI  */
    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mediaPackage = workflowInstance.mediaPackage
        val mpId = mediaPackage.identifier.toString()

        val currentOperation = workflowInstance.currentOperation
        val keepLastSnapshot = BooleanUtils.toBoolean(currentOperation.getConfiguration(OPT_LAST_SNAPSHOT))

        try {
            val q = assetManager!!.createQuery()
            val deleted: Long

            if (keepLastSnapshot) {
                deleted = q.delete(DEFAULT_OWNER, q.snapshot())
                        .where(q.mediaPackageId(mpId).and(q.version().isLatest.not())).run()
                logger.info("Deleting all but latest Snapshot {}", mpId)
            } else {
                deleted = q.delete(DEFAULT_OWNER, q.snapshot())
                        .where(q.mediaPackageId(mpId)).run()
            }

            if (deleted == 0L) {
                logger.info(format("The asset manager does not contain episode {}", mpId))
            } else {
                logger.info(format("Successfully deleted {} version/s episode {} from the asset manager", deleted, mpId))
            }
        } catch (e: Exception) {
            logger.warn(format("Error deleting episode {} from the asset manager: {}", mpId, e))
            throw WorkflowOperationException("Unable to delete episode from the asset manager", e)
        }

        return createResult(mediaPackage, Action.CONTINUE)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetManagerDeleteWorkflowOperationHandler::class.java)

        /** Configuration if last snapshot should not be deleted  */
        private val OPT_LAST_SNAPSHOT = "keep-last-snapshot"
    }

}
