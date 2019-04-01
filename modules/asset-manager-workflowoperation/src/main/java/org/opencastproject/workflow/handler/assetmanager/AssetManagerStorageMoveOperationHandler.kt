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

import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.impl.TieredStorageAssetManagerJobProducer
import org.opencastproject.assetmanager.impl.VersionImpl
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AssetManagerStorageMoveOperationHandler : AbstractWorkflowOperationHandler() {

    /** The asset manager.  */
    private var tsamjp: TieredStorageAssetManagerJobProducer? = null

    /** OSGi DI  */
    fun setJobProducer(tsamjp: TieredStorageAssetManagerJobProducer) {
        this.tsamjp = tsamjp
    }


    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        val mp = workflowInstance.mediaPackage
        val operation = workflowInstance.currentOperation

        logger.debug("Working on mediapackage {}", mp.identifier.toString())

        val targetStorage = StringUtils.trimToNull(operation.getConfiguration("target-storage"))
        if (!tsamjp!!.datastoreExists(targetStorage)) {
            throw WorkflowOperationException("Target storage type $targetStorage is not available!")
        }
        logger.debug("Target storage set to {}", targetStorage)

        //A missing version is ok, that just means to select all of them (which is represented as null)
        val targetVersion = StringUtils.trimToNull(operation.getConfiguration("target-version"))
        var version: Version? = null
        if (null != targetVersion) {
            try {
                version = VersionImpl.mk(java.lang.Long.parseLong(targetVersion))
            } catch (e: NumberFormatException) {
                throw WorkflowOperationException("Invalid version number", e)
            }

        }

        logger.debug("Target version set to {}", version)

        logger.debug("Beginning moving process")
        //Note that a null version implies *all* versions
        val job = tsamjp!!.moveByIdAndVersion(version, mp.identifier.compact(), targetStorage)
        return if (waitForStatus(job).isSuccess) {
            createResult(WorkflowOperationResult.Action.CONTINUE)
        } else {
            throw WorkflowOperationException("Archive operation did not complete successfully!")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetManagerStorageMoveOperationHandler::class.java)
    }
}
