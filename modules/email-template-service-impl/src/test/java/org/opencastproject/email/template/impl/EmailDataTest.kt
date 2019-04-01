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
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.api.WorkflowDefinitionImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList

class EmailDataTest {

    private var workflowInstance: WorkflowInstance? = null
    private var mp: MediaPackage? = null
    private val catalogs = HashMap<String, HashMap<String, String>>()
    private var failedOperation: WorkflowOperationInstance? = null
    private var incident1: Incident? = null
    private var incident2: Incident? = null
    private var incidents: MutableList<Incident>? = null
    private var uriMP: URI? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        uriMP = EmailDataTest::class.java.getResource("/email_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP!!.toURL().openStream())

        val episodeURI = EmailDataTest::class.java.getResource("/episode_dublincore.xml").toURI()
        val seriesURI = EmailDataTest::class.java.getResource("/series_dublincore.xml").toURI()

        val episodeDc = DublinCores.read(episodeURI.toURL().openStream())
        catalogs["episode"] = buildCatalogHash(episodeDc)

        val seriesDc = DublinCores.read(seriesURI.toURL().openStream())
        catalogs["series"] = buildCatalogHash(seriesDc)

        val def = WorkflowDefinitionImpl()
        def.id = "wfdef"
        val props = HashMap<String, String>()
        props["emailAddress"] = "user@domain.com"

        // Create some incidents
        incident1 = EasyMock.createNiceMock<Incident>(Incident::class.java)
        var details: MutableList<Tuple<String, String>> = LinkedList()
        var detail = Tuple("detail-type", "error in operation1")
        details.add(detail)
        EasyMock.expect(incident1!!.details).andReturn(details)

        incident2 = EasyMock.createNiceMock<Incident>(Incident::class.java)
        details = LinkedList()
        detail = Tuple("detail-type", "error in operation2")
        details.add(detail)
        EasyMock.expect(incident2!!.details).andReturn(details)

        // Link the incident and the subtree
        incidents = LinkedList()
        incidents!!.add(incident1)
        incidents!!.add(incident2)

        workflowInstance = WorkflowInstanceImpl(def, null, null, null, null, props)
        workflowInstance!!.id = 1
        workflowInstance!!.state = WorkflowState.RUNNING
        workflowInstance!!.mediaPackage = mp

        failedOperation = WorkflowOperationInstanceImpl("operation1", OperationState.FAILED)

        val operation = WorkflowOperationInstanceImpl("email", OperationState.RUNNING)
        val operationList = ArrayList<WorkflowOperationInstance>()
        operationList.add(failedOperation)
        operationList.add(operation)
        workflowInstance!!.operations = operationList

        EasyMock.replay(incident1, incident2)
    }

    private fun buildCatalogHash(dc: DublinCoreCatalog): HashMap<String, String> {
        val catalogHash = HashMap<String, String>()
        for (ename in dc.properties) {
            val name = ename.localName
            catalogHash[name] = dc.getAsText(ename, DublinCore.LANGUAGE_ANY, EmailTemplateServiceImpl.DEFAULT_DELIMITER_FOR_MULTIPLE)
        }
        return catalogHash
    }

    @Test
    @Throws(Exception::class)
    fun testToMap() {
        val emailData = EmailData("data1", workflowInstance!!, catalogs, failedOperation, incidents)

        val map = emailData.toMap()

        val catalogs = map["catalogs"]
        Assert.assertNotNull(catalogs)
        Assert.assertTrue(catalogs is HashMap<*, *>)

        var catalogHash = (catalogs as HashMap<*, *>)["episode"]
        Assert.assertNotNull(catalogHash)
        Assert.assertTrue(catalogHash is HashMap<*, *>)
        Assert.assertEquals("Test Media Package", (catalogHash as HashMap<*, *>)["title"])

        catalogHash = catalogs["series"]
        Assert.assertNotNull(catalogHash)
        Assert.assertTrue(catalogHash is HashMap<*, *>)
        Assert.assertEquals("20140119997", (catalogHash as HashMap<*, *>)["identifier"])

        val mp = map["mediaPackage"]
        Assert.assertNotNull(mp)
        Assert.assertTrue(mp is MediaPackage)
        Assert.assertEquals("Test Media Package", (mp as MediaPackage).title)

        val wf = map["workflow"]
        Assert.assertNotNull(wf)
        Assert.assertTrue(wf is WorkflowInstance)
        Assert.assertEquals(1, (wf as WorkflowInstance).id)

        val wfConf = map["workflowConfig"]
        Assert.assertNotNull(wfConf)
        Assert.assertTrue(wfConf is Map<*, *>)
        Assert.assertEquals("user@domain.com", (wfConf as Map<*, *>)["emailAddress"])

        val op = map["failedOperation"]
        Assert.assertNotNull(op)
        Assert.assertTrue(op is WorkflowOperationInstance)
        Assert.assertEquals("operation1", (op as WorkflowOperationInstance).template)

        val inc = map["incident"]
        Assert.assertNotNull(inc)
        Assert.assertTrue(inc is List<*>)
        Assert.assertEquals(2, (inc as List<*>).size.toLong())
        Assert.assertTrue(inc.contains(incident1))
        Assert.assertTrue(inc.contains(incident2))
    }

}
