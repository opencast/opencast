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

import org.opencastproject.job.api.Incident
import org.opencastproject.job.api.IncidentTree
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.api.WorkflowDefinitionImpl
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList

class EmailTemplateServiceImplTest {

    private var service: EmailTemplateServiceImpl? = null
    private var workflowInstance: WorkflowInstanceImpl? = null
    private var mp: MediaPackage? = null
    private var uriMP: URI? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        uriMP = EmailTemplateServiceImplTest::class.java.getResource("/email_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP!!.toURL().openStream())

        service = EmailTemplateServiceImpl()

        val episodeURI = EmailTemplateServiceImplTest::class.java.getResource("/episode_dublincore.xml").toURI()
        val seriesURI = EmailTemplateServiceImplTest::class.java.getResource("/series_dublincore.xml").toURI()
        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(URI("episode_dublincore.xml"))).andReturn(File(episodeURI))
        EasyMock.expect(workspace.get(URI("series_dublincore.xml"))).andReturn(File(seriesURI))
        EasyMock.replay(workspace)
        service!!.setWorkspace(workspace)

        val templateScanner = EasyMock.createMock<EmailTemplateScanner>(EmailTemplateScanner::class.java)
        EasyMock.expect(templateScanner.getTemplate("templateBasic")).andReturn(
                "Media package id: \${mediaPackage.identifier}, workflow id: \${workflow.id}, "
                        + "title: \${mediaPackage.title}, series title: \${mediaPackage.seriesTitle}, "
                        + "date: \${mediaPackage.date?datetime?iso_utc}")
        EasyMock.expect(templateScanner.getTemplate("templateCatalog"))
                .andReturn(
                        "EPISODE creator: \${catalogs[\"episode\"][\"creator\"]}, isPartOf: \${catalogs[\"episode\"][\"isPartOf\"]}, "
                                + "title: \${catalogs[\"episode\"][\"title\"]}, created: \${catalogs[\"episode\"][\"created\"]}, "
                                + "SERIES creator: \${catalogs[\"series\"][\"creator\"]}, description: \${catalogs[\"series\"][\"description\"]}, "
                                + "subject: \${catalogs[\"series\"][\"subject\"]}")
        EasyMock.expect(templateScanner.getTemplate("templateFailed")).andReturn(
                "<#if failedOperation?has_content>Workflow failed in operation: \${failedOperation.template}</#if>, " + "Workflow errors: <#list incident as inc><#list inc.details as de>\${de.b} </#list></#list>")
        EasyMock.expect(templateScanner.getTemplate("templateSyntaxError")).andReturn("\${mediaPackage")
        EasyMock.replay(templateScanner)
        service!!.setEmailTemplateScanner(templateScanner)

        val def = WorkflowDefinitionImpl()
        def.id = "wfdef"
        val props = HashMap<String, String>()
        props["emailAddress"] = "user@domain.com"

        val `is` = EasyMock.createMock<IncidentService>(IncidentService::class.java)

        /*
     * This is what we're building. EasyMock makes it ugly, of course /-------job1Tree / | / incident2 subtree |
     * incident1
     */

        // Create the tree and subtree
        val subtree = EasyMock.createNiceMock<IncidentTree>(IncidentTree::class.java)
        val job1Tree = EasyMock.createNiceMock<IncidentTree>(IncidentTree::class.java)

        // Create the incidents
        val incident1 = EasyMock.createNiceMock<Incident>(Incident::class.java)
        var details: MutableList<Tuple<String, String>> = LinkedList()
        var detail = Tuple("detail-type", "error in operation1")
        details.add(detail)
        EasyMock.expect(incident1.details).andReturn(details)

        val incident2 = EasyMock.createNiceMock<Incident>(Incident::class.java)
        details = LinkedList()
        detail = Tuple("detail-type", "error in operation2")
        details.add(detail)
        EasyMock.expect(incident2.details).andReturn(details)

        // Link the incident and the subtree
        var incidents = LinkedList<Incident>()
        incidents.add(incident1)
        EasyMock.expect(subtree.incidents).andReturn(incidents).anyTimes()
        // This is what Entwine wrote in the IncidentTree class, so rather than null we're using Immutables.nil()
        val subtreeDecendants = emptyList<IncidentTree>()
        EasyMock.expect(subtree.descendants).andReturn(subtreeDecendants).anyTimes()

        // Link the incident and the parent tree
        incidents = LinkedList()
        incidents.add(incident2)
        EasyMock.expect(job1Tree.incidents).andReturn(incidents).anyTimes()

        // Link the subtree and parent tree
        val subtreeList = LinkedList<IncidentTree>()
        subtreeList.add(subtree)
        EasyMock.expect(job1Tree.descendants).andReturn(subtreeList).anyTimes()

        service!!.setIncidentService(`is`)

        workflowInstance = WorkflowInstanceImpl(def, null, null, null, null, props)
        workflowInstance!!.id = 1
        workflowInstance!!.state = WorkflowState.RUNNING
        workflowInstance!!.mediaPackage = mp

        val failedOperation1 = WorkflowOperationInstanceImpl("operation1",
                OperationState.FAILED)
        failedOperation1.isFailWorkflowOnException = true
        failedOperation1.id = 1L
        EasyMock.expect(`is`.getIncidentsOfJob(1L, true)).andReturn(subtree).anyTimes()
        val failedOperation2 = WorkflowOperationInstanceImpl("operation2",
                OperationState.FAILED)
        failedOperation2.isFailWorkflowOnException = false
        failedOperation1.id = 2L
        EasyMock.expect(`is`.getIncidentsOfJob(2L, true)).andReturn(job1Tree).anyTimes()
        val operation = WorkflowOperationInstanceImpl("email", OperationState.RUNNING)
        val operationList = ArrayList<WorkflowOperationInstance>()
        operationList.add(failedOperation1)
        operationList.add(failedOperation2)
        operationList.add(operation)
        workflowInstance!!.operations = operationList

        EasyMock.replay(`is`, subtree, job1Tree, incident1, incident2)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateContent() {
        val templateName = "template"
        val templateContent = "This is the media package: \${mediaPackage.identifier}"

        val result = service!!.applyTemplate(templateName, templateContent, workflowInstance!!)
        Assert.assertEquals("This is the media package: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557", result)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateUsingBasicFields() {
        val templateName = "templateBasic"
        val templateContent: String? = null

        val result = service!!.applyTemplate(templateName, templateContent!!, workflowInstance!!)
        Assert.assertEquals("Media package id: 3e7bb56d-2fcc-4efe-9f0e-d6e56422f557, workflow id: 1, "
                + "title: Test Media Package, series title: Fall 2013 Test, " + "date: 2013-11-19T15:20:00Z", result)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateUsingCatalogFields() {
        val templateName = "templateCatalog"
        val templateContent: String? = null

        val result = service!!.applyTemplate(templateName, templateContent!!, workflowInstance!!)
        Assert.assertEquals("EPISODE creator: Rute Santos, isPartOf: 20140119997, "
                + "title: Test Media Package, created: 2013-11-19T15:20:00Z, "
                + "SERIES creator: Harvard Extension School, description: http://extension.harvard.edu, "
                + "subject: TEST E-19997", result)
    }

    @Test
    @Throws(Exception::class)
    fun testWorkflowConfiguration() {
        val templateName = "templateConfig"
        val templateContent = "This is an email address: \${workflowConfig['emailAddress']}"

        val result = service!!.applyTemplate(templateName, templateContent, workflowInstance!!)
        Assert.assertEquals("This is an email address: user@domain.com", result)
    }

    @Test
    @Throws(Exception::class)
    fun testFailedOperationAndErrors() {
        val templateName = "templateFailed"
        val templateContent: String? = null

        val result = service!!.applyTemplate(templateName, templateContent!!, workflowInstance!!)
        Assert.assertEquals("Workflow failed in operation: operation1, " + "Workflow errors: error in operation1 error in operation2 ", result)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun testErrorInTemplate() {
        val templateName = "templateSyntaxError"
        val templateContent: String? = null

        service!!.applyTemplate(templateName, templateContent!!, workflowInstance!!)
    }
}
