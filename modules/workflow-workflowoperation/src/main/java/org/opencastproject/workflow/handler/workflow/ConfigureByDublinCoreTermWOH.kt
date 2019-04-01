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

import org.opencastproject.metadata.dublincore.DublinCore.TERMS_NS_URI

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashMap

/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
class ConfigureByDublinCoreTermWOH : ResumableWorkflowOperationHandlerBase() {

    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * Callback for declarative services configuration that will introduce us to the local workspace service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param workspace
     * an instance of the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        val mediaPackage = workflowInstance.mediaPackage
        val currentOperation = workflowInstance.currentOperation

        val configuredCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCCATALOG_PROPERTY))
        val configuredDCTerm = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCTERM_PROPERTY))
        val configuredDefaultValue = StringUtils.trimToNull(currentOperation.getConfiguration(DEFAULT_VALUE_PROPERTY))
        val configuredMatchValue = StringUtils.trimToEmpty(currentOperation.getConfiguration(MATCH_VALUE_PROPERTY))

        // Find Catalog
        val catalogs = mediaPackage
                .getCatalogs(MediaPackageElementFlavor("dublincore", StringUtils.lowerCase(configuredCatalog)))

        if (catalogs != null && catalogs.size > 0) {
            var foundValue: Boolean? = false
            val dcterm = EName(TERMS_NS_URI, configuredDCTerm)

            // Find DCTerm
            for (catalog in catalogs) {
                val dc = DublinCoreUtil.loadDublinCore(workspace!!, catalog)
                // Match Value
                val values = dc[dcterm]
                if (values.isEmpty()) {
                    // Use default
                    if (configuredDefaultValue != null) {
                        foundValue = configuredDefaultValue == configuredMatchValue
                    }
                } else {
                    foundValue = values.contains(DublinCoreValue.mk(configuredMatchValue))
                }
            }

            if (foundValue!!) {
                val properties = HashMap<String, String>()

                for (key in currentOperation.configurationKeys) {
                    // Ignore this operations configuration
                    if (DCCATALOG_PROPERTY == key || DCTERM_PROPERTY == key || DEFAULT_VALUE_PROPERTY == key
                            || MATCH_VALUE_PROPERTY == key) {
                        continue
                    }

                    val value = currentOperation.getConfiguration(key)
                    properties[key] = value
                    logger.info("Configuration key '{}' of workflow {} is set to value '{}'", key, workflowInstance.id,
                            value)
                }

                return createResult(mediaPackage, properties, Action.CONTINUE, 0)

            } // if foundValue
        } // if catalogs

        return createResult(mediaPackage, Action.CONTINUE)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConfigureByDublinCoreTermWOH::class.java)

        /** Name of the configuration option that provides the catalog to examine  */
        val DCCATALOG_PROPERTY = "dccatalog"

        /** Name of the configuration option that provides Dublin Core term/element  */
        val DCTERM_PROPERTY = "dcterm"

        /** Name of the configuration option that provides term's default value if not present  */
        val DEFAULT_VALUE_PROPERTY = "default-value"

        /** Name of the configuration option that provides value to match  */
        val MATCH_VALUE_PROPERTY = "match-value"

        /** Name of the configuration option that provides the copy boolean we are looking for  */
        val COPY_PROPERTY = "copy"
    }
}
