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
package org.opencastproject.transcription.workflowoperation

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.transcription.api.TranscriptionService
import org.opencastproject.workflow.api.WorkflowDefinitionImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.Date

class StartTranscriptionOperationHandlerTest {

    /** The operation handler to test  */
    private var operationHandler: StartTranscriptionOperationHandler? = null

    /** The transcription service  */
    private var service: TranscriptionService? = null

    /** The operation instance  */
    private var operation: WorkflowOperationInstance? = null

    private var mediaPackage: MediaPackage? = null
    private var workflowInstance: WorkflowInstance? = null
    private var capturedTrack: Capture<Track>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // Media package set up
        val mediaPackageURI = StartTranscriptionOperationHandlerTest::class.java.getResource("/mp.xml").toURI()
        mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream())

        // Service registry set up
        val job1 = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job1.id).andReturn(1L)
        EasyMock.expect(job1.payload).andReturn(null).anyTimes()
        EasyMock.expect<Status>(job1.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job1.dateCreated).andReturn(Date())
        EasyMock.expect(job1.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job1.queueTime).andReturn(0)
        EasyMock.replay(job1)

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1)
        EasyMock.replay(serviceRegistry)

        // Transcription service set up
        service = EasyMock.createStrictMock<TranscriptionService>(TranscriptionService::class.java)
        capturedTrack = Capture.newInstance()
        EasyMock.expect(service!!.startTranscription(EasyMock.anyObject(String::class.java), EasyMock.capture(capturedTrack)))
                .andReturn(null)
        EasyMock.replay(service!!)

        // Workflow set up
        val def = WorkflowDefinitionImpl()
        def.id = "DCE-start-transcription"
        def.isPublished = true
        workflowInstance = WorkflowInstanceImpl(def, mediaPackage, null, null, null, null)
        workflowInstance!!.id = 1
        operation = WorkflowOperationInstanceImpl("start-transcript", OperationState.RUNNING)
        val operationList = ArrayList<WorkflowOperationInstance>()
        operationList.add(operation)
        workflowInstance!!.operations = operationList

        // Operation handler set up
        operationHandler = StartTranscriptionOperationHandler()
        operationHandler!!.setTranscriptionService(service)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testStartSelectByFlavor() {
        operation!!.setConfiguration(StartTranscriptionOperationHandler.SOURCE_FLAVOR, "audio/ogg")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("audioTrack1", capturedTrack!!.value.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testStartSelectByTag() {
        operation!!.setConfiguration(StartTranscriptionOperationHandler.SOURCE_TAG, "transcript")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("audioTrack1", capturedTrack!!.value.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testStartSkipFlavor() {
        // Make sure operation will be skipped if media package already contains the flavor passed
        operation!!.setConfiguration(StartTranscriptionOperationHandler.SKIP_IF_FLAVOR_EXISTS, "audio/ogg")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testStartDontSkipFlavor() {
        operation!!.setConfiguration(StartTranscriptionOperationHandler.SOURCE_TAG, "transcript")
        // Make sure operation will NOT be skipped if media package does NOT contain the flavor passed
        operation!!.setConfiguration(StartTranscriptionOperationHandler.SKIP_IF_FLAVOR_EXISTS, "captions/timedtext")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("audioTrack1", capturedTrack!!.value.identifier)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testStartMissingConfiguration() {
        operationHandler!!.start(workflowInstance, null)
    }
}
