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

package org.opencastproject.execute.operation.handler

import org.junit.Assert.fail

import org.opencastproject.execute.api.ExecuteException
import org.opencastproject.execute.api.ExecuteService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobBarrier.Result
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.CatalogImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Stream

import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.HashMap

/**
 * Tests for ExecuteOnceWorkflowOperationHandler
 */
class ExecuteOnceWorkflowOperationHandlerTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Mocking just about everything, just testing the mediapackage parse
        val expectedTypeString = "catalog"
        val catalogId = "catalog-id"

        val catUri = URI("http://api.com/catalog")
        catalog = CatalogImpl.newInstance()
        catalog!!.addTag("engage-download")
        catalog!!.identifier = catalogId
        catalog!!.setURI(catUri)

        val operation = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)

        EasyMock.expect(operation.id).andReturn(123L).anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.EXEC_PROPERTY)).andReturn(null)
                .anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.PARAMS_PROPERTY)).andReturn(null)
                .anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.LOAD_PROPERTY)).andReturn("123")
                .anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY))
                .andReturn(null).anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.TARGET_TAGS_PROPERTY))
                .andReturn(null).anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.OUTPUT_FILENAME_PROPERTY))
                .andReturn(null).anyTimes()
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.SET_WF_PROPS_PROPERTY))
                .andReturn("false").anyTimes()
        // these two need to supply a real string
        EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.EXPECTED_TYPE_PROPERTY))
                .andReturn(expectedTypeString).anyTimes()
        EasyMock.replay(operation)

        val mpId = EasyMock.createMock<Id>(Id::class.java)

        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        mediaPackage.add(EasyMock.anyObject<Any>() as MediaPackageElement)
        EasyMock.expect(mediaPackage.identifier).andReturn(mpId).anyTimes()
        EasyMock.replay(mediaPackage)

        workspaceService = EasyMock.createMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspaceService!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String)).andReturn(catUri).anyTimes()
        EasyMock.replay(workspaceService!!)

        workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance!!.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflowInstance!!.currentOperation).andStubReturn(operation)
        EasyMock.replay(workflowInstance!!)

        // Override the waitForStatus method to not block the jobs
        execOnceWOH = object : ExecuteOnceWorkflowOperationHandler() {
            override fun waitForStatus(timeout: Long, vararg jobs: Job): Result {
                val map = Stream.mk(*jobs).foldl(HashMap(),
                        object : Fn2<HashMap<Job, Status>, Job, HashMap<Job, Status>>() {
                            override fun apply(a: HashMap<Job, Status>, b: Job): HashMap<Job, Status> {
                                a[b] = Status.FINISHED
                                return a
                            }
                        })
                return Result(map)
            }
        }
        execOnceWOH!!.setWorkspace(workspaceService)
    }

    @Throws(ExecuteException::class, MediaPackageException::class)
    private fun setEmptyPayload() {
        val catalogJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect<Long>(catalogJob.queueTime).andReturn(123L).anyTimes()
        EasyMock.expect(catalogJob.payload).andReturn("").anyTimes()
        EasyMock.replay(catalogJob)

        executeService = EasyMock.createMock<ExecuteService>(ExecuteService::class.java)
        EasyMock.expect(
                executeService!!.execute(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<Any>() as MediaPackage,
                        EasyMock.anyString(), EasyMock.anyObject<Any>() as MediaPackageElement.Type, EasyMock.anyLong().toFloat()))
                .andReturn(catalogJob).anyTimes()
        EasyMock.replay(executeService!!)

        execOnceWOH!!.setExecuteService(executeService)

    }

    @Throws(ExecuteException::class, MediaPackageException::class)
    private fun setSomePayload() {
        val catalogJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect<Long>(catalogJob.queueTime).andReturn(123L).anyTimes()
        EasyMock.expect(catalogJob.payload).andReturn(MediaPackageElementParser.getAsXml(catalog)).anyTimes()
        EasyMock.replay(catalogJob)

        executeService = EasyMock.createMock<ExecuteService>(ExecuteService::class.java)
        EasyMock.expect(
                executeService!!.execute(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<Any>() as MediaPackage,
                        EasyMock.anyString(), EasyMock.anyObject<Any>() as MediaPackageElement.Type, EasyMock.anyLong().toFloat()))
                .andReturn(catalogJob).anyTimes()
        EasyMock.replay(executeService!!)

        execOnceWOH!!.setExecuteService(executeService)

    }

    @Test
    @Throws(ExecuteException::class, MediaPackageException::class)
    fun startParseSomePayloadTest() {
        try {
            setSomePayload()
            execOnceWOH!!.start(workflowInstance, null)
        } catch (ex: WorkflowOperationException) {
            fail("Should not throw exception" + ex.message)
        }

    }

    @Test
    @Throws(ExecuteException::class, MediaPackageException::class)
    fun startParseEmptyPayloadTest() {
        try {
            setEmptyPayload()
            execOnceWOH!!.start(workflowInstance, null)
        } catch (ex: WorkflowOperationException) {
            fail("Should not throw exception" + ex.message)
        }

    }

    companion object {

        private var executeService: ExecuteService? = null
        private var workspaceService: Workspace? = null
        private var execOnceWOH: ExecuteOnceWorkflowOperationHandler? = null
        private var workflowInstance: WorkflowInstance? = null
        private var catalog: Catalog? = null
    }
}
