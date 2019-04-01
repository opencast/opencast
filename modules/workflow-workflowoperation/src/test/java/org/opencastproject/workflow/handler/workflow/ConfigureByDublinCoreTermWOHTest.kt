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

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.ArrayList

/**
 * Test class for [TagWorkflowOperationHandler]
 */
class ConfigureByDublinCoreTermWOHTest {

    private var operationHandler: ConfigureByDublinCoreTermWOH? = null
    private var instance: WorkflowInstanceImpl? = null
    private var operation: WorkflowOperationInstanceImpl? = null
    private var mp: MediaPackage? = null
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(this.javaClass.getResourceAsStream("/archive_mediapackage.xml"))
        mp!!.getCatalog("catalog-1").setURI(this.javaClass.getResource("/dublincore.xml").toURI())

        // set up the handler
        operationHandler = ConfigureByDublinCoreTermWOH()

        // Initialize the workflow
        instance = WorkflowInstanceImpl()
        operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        val ops = ArrayList<WorkflowOperationInstance>()
        ops.add(operation)
        instance!!.operations = ops
        instance!!.setConfiguration("oldConfigProperty", "foo")
        instance!!.mediaPackage = mp

        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect<InputStream>(workspace!!.read(EasyMock.anyObject<URI>()))
                .andAnswer { javaClass.getResourceAsStream("/dublincore.xml") }
        EasyMock.replay(workspace!!)
        operationHandler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testMatchPresentDCTerm() {
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")
        operation!!.setConfiguration("newConfigProperty", "true")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties.containsKey("newConfigProperty"))
        Assert.assertEquals("true", properties["newConfigProperty"])
    }

    @Test
    @Throws(Exception::class)
    fun testMatchPresentDCTermOverwriteProperty() {
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")
        operation!!.setConfiguration("oldConfigProperty", "bar")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties.containsKey("oldConfigProperty"))
        Assert.assertEquals("bar", properties["oldConfigProperty"])
    }

    @Test
    @Throws(Exception::class)
    fun testMatchDefaultDCTerm() {
        // Match == Default Value
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")
        operation!!.setConfiguration("newConfigProperty", "true")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties.containsKey("newConfigProperty"))
        Assert.assertEquals("true", properties["newConfigProperty"])
    }

    @Test
    @Throws(Exception::class)
    fun testMisMatchDefaultDCTerm() {
        // Match != Default Value
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Cairo")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")
        operation!!.setConfiguration("newConfigProperty", "true")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties == null)
    }

    @Test
    @Throws(Exception::class)
    fun testMissingNoDefaultDCTerm() {
        // No Default Value
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")
        operation!!.setConfiguration("newConfigProperty", "true")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties == null)
    }

    @Test
    @Throws(Exception::class)
    fun testNoMatchConfiguredDCTerm() {
        // No Match or Default Value
        // without dcterm or default the match should always fail
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val properties = result.properties

        Assert.assertTrue(properties == null)
    }
}
