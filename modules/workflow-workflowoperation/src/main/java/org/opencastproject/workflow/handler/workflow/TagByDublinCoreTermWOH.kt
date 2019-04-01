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
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreUtil
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList

/**
 * Take look in specified catalog for specified term, if the value matches the specified value add the target-tags
 */
class TagByDublinCoreTermWOH : ResumableWorkflowOperationHandlerBase() {

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

        val configuredSourceFlavors = StringUtils
                .trimToEmpty(currentOperation.getConfiguration(SOURCE_FLAVORS_PROPERTY))
        val configuredSourceTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(SOURCE_TAGS_PROPERTY))
        val configuredCatalog = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCCATALOG_PROPERTY))
        val configuredDCTerm = StringUtils.trimToEmpty(currentOperation.getConfiguration(DCTERM_PROPERTY))
        val configuredDefaultValue = StringUtils.trimToNull(currentOperation.getConfiguration(DEFAULT_VALUE_PROPERTY))
        val configuredMatchValue = StringUtils.trimToEmpty(currentOperation.getConfiguration(MATCH_VALUE_PROPERTY))
        val configuredTargetFlavor = StringUtils.trimToNull(currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY))
        val configuredTargetTags = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY))
        val copy = BooleanUtils.toBoolean(currentOperation.getConfiguration(COPY_PROPERTY))

        val sourceTags = StringUtils.split(configuredSourceTags, ",")
        val targetTags = StringUtils.split(configuredTargetTags, ",")
        val sourceFlavors = StringUtils.split(configuredSourceFlavors, ",")

        val elementSelector = SimpleElementSelector()
        for (flavor in sourceFlavors) {
            elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
        for (tag in sourceTags) {
            elementSelector.addTag(tag)
        }

        val removeTags = ArrayList<String>()
        val addTags = ArrayList<String>()
        val overrideTags = ArrayList<String>()

        for (tag in targetTags) {
            if (tag.startsWith(MINUS)) {
                removeTags.add(tag)
            } else if (tag.startsWith(PLUS)) {
                addTags.add(tag)
            } else {
                overrideTags.add(tag)
            }
        }

        // Find Catalog
        val catalogs = mediaPackage.getCatalogs(MediaPackageElementFlavor("dublincore", StringUtils.lowerCase(configuredCatalog)))

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
                if (copy) {
                    logger.info("Retagging mediapackage elements as a copy")
                } else {
                    logger.info("Retagging mediapackage elements")
                }

                val elements = elementSelector.select(mediaPackage, false)
                for (e in elements) {
                    var element = e

                    if (copy) {
                        element = e.clone() as MediaPackageElement
                        element.identifier = null
                        element.setURI(e.getURI()) // use the same URI as the original
                    }
                    if (configuredTargetFlavor != null) {
                        element.flavor = MediaPackageElementFlavor.parseFlavor(configuredTargetFlavor)
                    }

                    if (overrideTags.size > 0) {
                        element.clearTags()
                        for (tag in overrideTags) {
                            element.addTag(tag)
                        }
                    } else {
                        for (tag in removeTags) {
                            element.removeTag(tag.substring(MINUS.length))
                        }
                        for (tag in addTags) {
                            element.addTag(tag.substring(PLUS.length))
                        }
                    }

                    if (copy) {
                        mediaPackage.addDerived(element, e)
                    }
                }
            } // if foundValue
        } // if catalogs

        return createResult(mediaPackage, Action.CONTINUE)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TagByDublinCoreTermWOH::class.java)
        private val PLUS = "+"
        private val MINUS = "-"

        /** Name of the configuration option that provides the source flavors we are looking for  */
        val SOURCE_FLAVORS_PROPERTY = "source-flavors"

        /** Name of the configuration option that provides the source tags we are looking for  */
        val SOURCE_TAGS_PROPERTY = "source-tags"

        /** Name of the configuration option that provides the target flavors we are looking for  */
        val TARGET_FLAVOR_PROPERTY = "target-flavor"

        /** Name of the configuration option that provides the target tags we are looking for  */
        val TARGET_TAGS_PROPERTY = "target-tags"

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
