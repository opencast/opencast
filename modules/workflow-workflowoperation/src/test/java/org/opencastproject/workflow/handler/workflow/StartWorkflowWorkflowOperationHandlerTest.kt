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

import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.capture
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.eq
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.newCapture
import org.easymock.EasyMock.replay
import org.easymock.EasyMock.reset
import org.easymock.EasyMock.verify
import org.junit.Assert.assertEquals
import org.opencastproject.workflow.handler.workflow.StartWorkflowWorkflowOperationHandler.MEDIA_PACKAGE_ID
import org.opencastproject.workflow.handler.workflow.StartWorkflowWorkflowOperationHandler.WORKFLOW_DEFINITION

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowDefinitionImpl
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowService

import com.entwinemedia.fn.data.Opt
import com.google.common.collect.Lists

import org.easymock.Capture
import org.junit.Before
import org.junit.Test

class StartWorkflowWorkflowOperationHandlerTest {

    private var operationHandler: StartWorkflowWorkflowOperationHandler? = null
    private var assetManager: AssetManager? = null
    private var workflowService: WorkflowService? = null
    private var operation: WorkflowOperationInstanceImpl? = null
    private var workflowInstance: WorkflowInstanceImpl? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assetManager = createNiceMock<AssetManager>(AssetManager::class.java)
        expect(assetManager!!.getMediaPackage(anyString())).andReturn(Opt.none()).anyTimes()

        workflowService = createNiceMock<WorkflowService>(WorkflowService::class.java)

        replay(assetManager, workflowService)

        operationHandler = StartWorkflowWorkflowOperationHandler()
        operationHandler!!.setAssetManager(assetManager)
        operationHandler!!.setWorkflowService(workflowService)

        operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        operation!!.template = "start-workflow"
        operation!!.state = OperationState.RUNNING
        operation!!.setConfiguration(MEDIA_PACKAGE_ID, MP_ID)
        operation!!.setConfiguration(WORKFLOW_DEFINITION, WD_ID)
        operation!!.setConfiguration("workflowConfigurations", "true")
        operation!!.setConfiguration("key", "value")

        workflowInstance = WorkflowInstanceImpl()
        workflowInstance!!.mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        workflowInstance!!.state = WorkflowState.RUNNING
        workflowInstance!!.operations = Lists.newArrayList<WorkflowOperationInstance>(operation!!)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testNoMediaPackage() {
        operationHandler!!.start(workflowInstance, null)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testNoWorkflowDefinition() {
        reset(workflowService!!)
        expect(workflowService!!.getWorkflowDefinitionById(WD_ID)).andThrow(NotFoundException())
        replay(workflowService!!)

        operationHandler!!.start(workflowInstance, null)
    }

    @Test
    @Throws(Exception::class)
    fun testStartWorkflow() {
        // Media Package
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.identifier = IdImpl(MP_ID)

        // Asset Manager
        reset(assetManager!!)
        expect(assetManager!!.getMediaPackage(anyString())).andReturn(Opt.some(mp))

        // Workflow Service
        val wd = WorkflowDefinitionImpl()
        wd.id = WD_ID

        val wProperties = newCapture<Map<String, String>>()

        reset(workflowService!!)
        expect(workflowService!!.getWorkflowDefinitionById(WD_ID)).andReturn(wd)
        expect<WorkflowInstance>(workflowService!!.start(eq<WorkflowDefinition>(wd), eq(mp), capture(wProperties))).andReturn(null)

        replay(assetManager, workflowService)

        val result = operationHandler!!.start(workflowInstance, null)

        verify(assetManager, workflowService)

        assertEquals(WorkflowOperationResult.Action.CONTINUE, result.action)
        assertEquals(2, wProperties.value.size.toLong())
        assertEquals("true", wProperties.value["workflowConfigurations"])
        assertEquals("value", wProperties.value["key"])
    }

    companion object {
        private val MP_ID = "c3066908-39e3-44b1-842a-9ae93ef8d314"
        private val WD_ID = "test-workflow"
    }
}
