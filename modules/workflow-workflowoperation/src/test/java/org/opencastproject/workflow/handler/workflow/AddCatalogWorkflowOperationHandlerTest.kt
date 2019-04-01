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
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.InputStream
import java.util.ArrayList

class AddCatalogWorkflowOperationHandlerTest {

    private var operationHandler: AddCatalogWorkflowOperationHandler? = null
    private var instance: WorkflowInstanceImpl? = null
    private var operation: WorkflowOperationInstanceImpl? = null

    @Rule
    var temporaryFolder = TemporaryFolder()

    @Rule
    var expectedException = ExpectedException.none()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operationHandler = AddCatalogWorkflowOperationHandler()

        instance = WorkflowInstanceImpl()

        val ops = ArrayList<WorkflowOperationInstance>()
        operation = WorkflowOperationInstanceImpl("test", OperationState.RUNNING)
        ops.add(operation)
        instance!!.operations = ops
        val catalogName = "test-catalog"
        operation!!.setConfiguration("catalog-name", catalogName)
        operation!!.setConfiguration("catalog-path", javaClass.getResource("/dublincore.xml").path)

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        instance!!.mediaPackage = mp

        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        val inStream = EasyMock.newCapture<InputStream>()
        EasyMock.expect<URI>(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.eq(catalogName),
                EasyMock.capture(inStream))).andAnswer {
            val file = temporaryFolder.newFile()
            FileUtils.copyInputStreamToFile(inStream.value, file)
            file.toURI()
        }.anyTimes()
        EasyMock.replay(workspace)

        operationHandler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testBasic() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "flavor/test")
        operation!!.setConfiguration("catalog-type-collision-behavior", "keep")

        operation!!.setConfiguration("catalog-tags", "tag1,tag2")

        // execution
        val result = operationHandler!!.start(instance, null)

        // checks
        Assert.assertEquals(Action.CONTINUE, result.action)

        val mp = result.mediaPackage

        Assert.assertEquals(mp.catalogs.size.toLong(), 1)

        Assert.assertEquals(mp.catalogs[0].tags.size.toLong(), 2)
        Assert.assertEquals(mp.catalogs[0].tags[0], "tag1")
        Assert.assertEquals(mp.catalogs[0].tags[1], "tag2")

        Assert.assertEquals(mp.catalogs[0].flavor,
                MediaPackageElementFlavor.parseFlavor("flavor/test"))
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testNoTags() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "flavor/test")
        operation!!.setConfiguration("catalog-type-collision-behavior", "keep")

        // execution
        val result = operationHandler!!.start(instance, null)

        // checks
        Assert.assertEquals(Action.CONTINUE, result.action)

        val mp = result.mediaPackage

        Assert.assertEquals(mp.catalogs.size.toLong(), 1)

        Assert.assertEquals(mp.catalogs[0].tags.size.toLong(), 0)

        Assert.assertEquals(mp.catalogs[0].flavor,
                MediaPackageElementFlavor.parseFlavor("flavor/test"))
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testNoFlavorFail() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "")
        operation!!.setConfiguration("catalog-type-collision-behavior", "keep")

        // execution
        expectedException.expect(WorkflowOperationException::class.java)
        operationHandler!!.start(instance, null)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testKeep() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "flavor/test")
        operation!!.setConfiguration("catalog-type-collision-behavior", "keep")

        // execution
        operationHandler!!.start(instance, null)
        val result = operationHandler!!.start(instance, null)

        // checks
        Assert.assertEquals(Action.CONTINUE, result.action)

        val mp = result.mediaPackage

        Assert.assertEquals(mp.catalogs.size.toLong(), 2)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testSkip() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "flavor/test")
        operation!!.setConfiguration("catalog-type-collision-behavior", "skip")

        // execution
        operationHandler!!.start(instance, null)
        val result = operationHandler!!.start(instance, null)

        // checks
        Assert.assertEquals(Action.CONTINUE, result.action)

        val mp = result.mediaPackage

        Assert.assertEquals(mp.catalogs.size.toLong(), 1)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testFail() {
        // setup
        operation!!.setConfiguration("catalog-flavor", "flavor/test")
        operation!!.setConfiguration("catalog-type-collision-behavior", "fail")

        // execution
        operationHandler!!.start(instance, null)
        expectedException.expect(WorkflowOperationException::class.java)
        operationHandler!!.start(instance, null)
    }
}
