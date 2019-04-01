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
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.ArrayList

import junit.framework.Assert

/**
 * Test class for [ZipWorkflowOperationHandler]
 */
class ZipWorkflowOperationHandlerTest {

    private var operationHandler: ZipWorkflowOperationHandler? = null
    private var mp: MediaPackage? = null
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(this.javaClass.getResourceAsStream("/archive_mediapackage.xml"))

        // set up the handler
        operationHandler = ZipWorkflowOperationHandler()

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        val newURI = URI("http://www.url.org")
        EasyMock.expect(
                workspace!!.put(EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andReturn(newURI).anyTimes()
        EasyMock.expect(workspace!!.getURI(EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String)).andReturn(newURI).anyTimes()
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(
                File(javaClass.getResource("/dublincore.xml").toURI())).anyTimes()
        EasyMock.replay(workspace!!)
        operationHandler!!.setWorkspace(workspace)
    }

    /*
   * MH-9757
   */
    @Test
    @Throws(Exception::class)
    fun testInvalidWorkflow() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops

        operation.setConfiguration(ZipWorkflowOperationHandler.ZIP_COLLECTION_PROPERTY, "failed-zips")
        operation.setConfiguration(ZipWorkflowOperationHandler.INCLUDE_FLAVORS_PROPERTY, "*/source,dublincore/*")
        operation.setConfiguration(ZipWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "archive/zip")
        operation.setConfiguration(ZipWorkflowOperationHandler.COMPRESS_PROPERTY, "false")

        try {
            val result = operationHandler!!.start(null, null)
            Assert.fail("A null workflow is passed so an exception should be thrown")
        } catch (e: WorkflowOperationException) {
            // expecting exception
        }

    }

    /*
   * MH-9759
   */
    @Test
    @Throws(Exception::class)
    fun testInvalidMediaPackage() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = null

        operation.setConfiguration(ZipWorkflowOperationHandler.ZIP_COLLECTION_PROPERTY, "failed-zips")
        operation.setConfiguration(ZipWorkflowOperationHandler.INCLUDE_FLAVORS_PROPERTY, "*/source,dublincore/*")
        operation.setConfiguration(ZipWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "archive/zip")
        operation.setConfiguration(ZipWorkflowOperationHandler.COMPRESS_PROPERTY, "false")

        try {
            val result = operationHandler!!.start(instance, null)
            Assert.fail("A null mediapackage is passed so an exception should be thrown")
        } catch (e: WorkflowOperationException) {
            // expecting exception
        }

    }

    /*
   * MH-10043
   */
    @Test
    fun testConfigKeyTargetFlavorDefaultValue() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp

        operation.setConfiguration(ZipWorkflowOperationHandler.ZIP_COLLECTION_PROPERTY, "failed-zips")
        operation.setConfiguration(ZipWorkflowOperationHandler.INCLUDE_FLAVORS_PROPERTY, "*/source,dublincore/*")
        // targe-flavor is not mandatory
        // operation.setConfiguration(ZipWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "archive/zip");
        operation.setConfiguration(ZipWorkflowOperationHandler.COMPRESS_PROPERTY, "false")

        try {
            val result = operationHandler!!.start(instance, null)
            Assert.assertEquals("workflow result action not CONTINUE: " + result.action, WorkflowOperationResult.Action.CONTINUE, result.action)
        } catch (e: WorkflowOperationException) {
            Assert.fail("missing target-flavor and no default value kicked in: $e")
        }

    }
}
