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
import org.opencastproject.util.UrlSupport
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.HashMap

class CopyWorkflowOperationHandlerTest {

    private var operationHandler: CopyWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null
    private var videoFile: File? = null

    // mock services and objects
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = javaClass.getResource("/copy_mediapackage.xml").toURI()

        mp = builder.loadFromXml(uriMP.toURL().openStream())

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)

        // set up service
        operationHandler = CopyWorkflowOperationHandler()

        // Prepare file to returne from workflow
        videoFile = File(javaClass.getResource("/av.mov").toURI())
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(videoFile).anyTimes()
        EasyMock.replay(workspace!!)
        operationHandler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessfullCopy() {
        val copyTargetDirectory = videoFile!!.parent
        val copyFileName = "testCopy"
        val sb = StringBuilder().append(copyTargetDirectory).append("/").append(copyFileName).append(".mov")

        val copy = File(sb.toString())

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS] = "presentation/source"
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_TAGS] = "first"
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY] = copyTargetDirectory
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_FILENAME] = copyFileName

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertTrue(copy.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testOptionalFilename() {
        val copy = File(UrlSupport.concat(videoFile!!.parent, "copy", videoFile!!.name))
        val copyTargetDirectory = copy.parent
        FileUtils.forceMkdir(copy)

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS] = "presentation/source"
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_TAGS] = "first"
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY] = copyTargetDirectory

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertTrue(copy.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testCopyWithNoElementMatchingFlavorsTags() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS] = "no-video/source"
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_TAGS] = "engage,rss"
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY] = videoFile!!.parent
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_FILENAME] = "testCopy"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testCopyWithTwoElementsMatchingFlavorsTags() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[CopyWorkflowOperationHandler.OPT_SOURCE_FLAVORS] = "*/source"
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_DIRECTORY] = videoFile!!.parent
        configurations[CopyWorkflowOperationHandler.OPT_TARGET_FILENAME] = "testCopy"

        // run the operation handler
        try {
            val result = getWorkflowOperationResult(mp, configurations)
            Assert.assertEquals(Action.CONTINUE, result.action)
        } catch (e: WorkflowOperationException) {
            Assert.fail("The workflow should ")
        }

    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?, configurations: Map<String, String>): WorkflowOperationResult {
        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        operation.template = "copy"
        operation.state = OperationState.RUNNING
        for (key in configurations.keys) {
            operation.setConfiguration(key, configurations[key])
        }

        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        // Run the media package through the operation handler, ensuring that metadata gets added
        return operationHandler!!.start(workflowInstance, null)
    }

}
