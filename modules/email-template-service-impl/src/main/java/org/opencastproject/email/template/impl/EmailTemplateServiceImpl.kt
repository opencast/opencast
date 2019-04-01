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
package org.opencastproject.email.template.impl

import org.opencastproject.email.template.api.EmailTemplateService
import org.opencastproject.job.api.Incident
import org.opencastproject.job.api.IncidentTree
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.util.doc.DocUtil
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicReference

class EmailTemplateServiceImpl : EmailTemplateService {

    /** The workspace (needed to read the catalogs when processing templates)  */
    private var workspace: Workspace? = null

    /** Email template scanner is optional and has dynamic policy  */
    private val templateScannerRef = AtomicReference<EmailTemplateScanner>()

    /** The incident service (to list errors in email)  */
    private var incidentService: IncidentService? = null

    protected fun activate(context: ComponentContext) {
        logger.info("EmailTemplateServiceImpl activated")
    }

    /**
     * Apply the template to the workflow instance.
     *
     * @param templateName
     * template name
     * @param templateContent
     * template content
     * @param workflowInstance
     * workflow
     * @return text with applied template
     */
    override fun applyTemplate(templateName: String, templateContent: String, workflowInstance: WorkflowInstance): String {
        var templateContent = templateContent
        if (templateContent == null && templateScannerRef.get() != null) {
            templateContent = templateScannerRef.get().getTemplate(templateName)
        }

        if (templateContent == null) {
            logger.warn("E-mail template not found: {}", templateName)
            return "TEMPLATE NOT FOUND: $templateName" // it's probably missing
        }

        // Build email data structure and apply the template
        val catalogs = initCatalogs(workflowInstance.mediaPackage)

        val failed = findFailedOperation(workflowInstance)
        var incidentList: List<Incident>? = null
        if (failed != null) {
            try {
                val incidents = incidentService!!.getIncidentsOfJob(failed.id!!, true)
                incidentList = generateIncidentList(incidents)
            } catch (e: Exception) {
                logger.error("Error when populating template with incidents", e)
                // Incidents in email will be empty
            }

        }

        return DocUtil.generate(EmailData(templateName, workflowInstance, catalogs, failed, incidentList),
                templateContent)
    }

    /**
     * Initializes the map with all fields from the dublin core catalogs.
     */
    private fun initCatalogs(mediaPackage: MediaPackage): HashMap<String, HashMap<String, String>> {
        val catalogs = HashMap<String, HashMap<String, String>>()
        val dcs = mediaPackage.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE)

        var i = 0
        while (dcs != null && i < dcs!!.size) {
            var dc: DublinCoreCatalog? = null
            var `in`: InputStream? = null
            try {
                val f = workspace!!.get(dcs!![i].getURI())
                `in` = FileInputStream(f)
                dc = DublinCores.read(`in`)
            } catch (e: Exception) {
                logger.warn("Error when populating catalog data", e)
                // Don't include the info
                i++
                continue
            } finally {
                IOUtils.closeQuietly(`in`)
            }

            if (dc != null) {
                val catalogFlavor = dcs!![i].flavor.subtype
                val catalogHash = HashMap<String, String>()
                for (ename in dc.properties) {
                    val name = ename.localName
                    catalogHash[name] = dc.getAsText(ename, DublinCore.LANGUAGE_ANY, DEFAULT_DELIMITER_FOR_MULTIPLE)
                }
                catalogs[catalogFlavor] = catalogHash
            }
            i++
        }

        return catalogs
    }

    /**
     * Traverses the workflow until it finds a failed operation that has failOnError=true
     *
     * @param workflow
     * @return the workflow operation that failed
     */
    private fun findFailedOperation(workflow: WorkflowInstance): WorkflowOperationInstance? {
        val operations = ArrayList(workflow.operations)
        // Current operation is the email operation
        val emailOp = workflow.currentOperation
        // Look for the last operation that is in failed state and has failOnError true
        var i = operations.indexOf(emailOp) - 1
        var op: WorkflowOperationInstance? = null
        while (i >= 0) {
            op = operations[i]
            if (OperationState.FAILED == op!!.state && op.isFailWorkflowOnException) {
                return op
            }
            i--
        }
        return null
    }

    /**
     * Generates list of all incidents in the tree
     *
     * @param tree
     * the incident tree
     * @return a flat list of incidents
     */
    private fun generateIncidentList(tree: IncidentTree?): List<Incident> {
        val list = LinkedList<Incident>()
        if (tree != null && tree.descendants != null && tree.descendants.size > 0) {
            for (subtree in tree.descendants) {
                list.addAll(generateIncidentList(subtree))
            }
        }
        list.addAll(tree!!.incidents)
        return list
    }

    /**
     * Callback for OSGi to set the [Workspace].
     *
     * @param ws
     * the workspace
     */
    internal fun setWorkspace(ws: Workspace) {
        this.workspace = ws
    }

    /**
     * Callback for OSGi to set the [EmailTemplateScanner].
     *
     * @param templateScanner
     * the template scanner service
     */
    internal fun setEmailTemplateScanner(templateScanner: EmailTemplateScanner) {
        this.templateScannerRef.compareAndSet(null, templateScanner)
    }

    /**
     * Callback for OSGi to unset the [EmailTemplateScanner].
     *
     * @param templateScanner
     * the template scanner service
     */
    internal fun unsetEmailTemplateScanner(templateScanner: EmailTemplateScanner) {
        this.templateScannerRef.compareAndSet(templateScanner, null)
    }

    /**
     * Callback for OSGi to unset the [IncidentService].
     *
     * @param incidentService
     * the incident service
     */
    fun setIncidentService(incidentService: IncidentService) {
        this.incidentService = incidentService
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmailTemplateServiceImpl::class.java)

        val DEFAULT_DELIMITER_FOR_MULTIPLE = ","
    }

}
