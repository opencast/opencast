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
import org.opencastproject.workflow.api.WorkflowOperationResult.Action.CONTINUE
import org.opencastproject.workflow.api.WorkflowOperationResult.Action.SKIP

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings

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
 * Workflow operation handler for importing workflow properties.
 */
class ImportWorkflowPropertiesWOH : AbstractWorkflowOperationHandler() {

    /* Service references */
    private var workspace: Workspace? = null

    /** OSGi DI  */
    internal fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(WorkflowOperationException::class)
    override fun start(wi: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.info("Start importing workflow properties for workflow {}", wi)
        val sourceFlavor = getConfig(wi, SOURCE_FLAVOR_PROPERTY)
        val propertiesElem = loadPropertiesElementFromMediaPackage(
                MediaPackageElementFlavor.parseFlavor(sourceFlavor), wi)
        if (propertiesElem.isSome) {
            val properties = loadPropertiesFromXml(workspace!!, propertiesElem.get().getURI())
            val keys = `$`(getOptConfig(wi, KEYS_PROPERTY)).bind(Strings.splitCsv).toSet()
            return createResult(wi.mediaPackage, convertToWorkflowProperties(properties, keys), CONTINUE, 0)
        } else {
            logger.info("No attachment with workflow properties found, skipping...")
            return createResult(wi.mediaPackage, SKIP)
        }
    }

    private fun convertToWorkflowProperties(properties: Properties, keys: Set<String>): Map<String, String> {
        val workflowProperties = HashMap<String, String>()
        for ((key, value) in properties) {
            if (keys.isEmpty() || keys.contains(key)) {
                workflowProperties[key as String] = value as String
            }
        }
        return workflowProperties
    }

    companion object {

        /* Configuration options */
        val SOURCE_FLAVOR_PROPERTY = "source-flavor"
        val KEYS_PROPERTY = "keys"

        private val logger = LoggerFactory.getLogger(ImportWorkflowPropertiesWOH::class.java)

        @Throws(WorkflowOperationException::class)
        internal fun loadPropertiesElementFromMediaPackage(sourceFlavor: MediaPackageElementFlavor,
                                                           wi: WorkflowInstance): Opt<Attachment> {
            val mp = wi.mediaPackage
            val elements = mp.getAttachments(sourceFlavor)

            if (elements.size < 1) {
                logger.info("Cannot import workflow properties - no element with flavor '{}' found in media package '{}'",
                        sourceFlavor, mp.identifier)
                return Opt.none()
            } else if (elements.size > 1) {
                throw WorkflowOperationException(format("Found more than one element with flavor '%s' in media package '%s'",
                        sourceFlavor, mp.identifier))
            }

            return Opt.some(elements[0])
        }

        @Throws(WorkflowOperationException::class)
        internal fun loadPropertiesFromXml(workspace: Workspace, uri: URI): Properties {
            val properties = Properties()
            try {
                val propertiesFile = workspace.get(uri)
                FileInputStream(propertiesFile).use { `is` ->
                    properties.loadFromXML(`is`)
                    logger.debug("Properties loaded from {}", propertiesFile)
                }
            } catch (e: NotFoundException) {
                throw WorkflowOperationException(e)
            } catch (e: IOException) {
                throw WorkflowOperationException(e)
            }

            return properties
        }
    }
}
