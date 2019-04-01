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

import org.opencastproject.caption.api.CaptionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
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
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.net.URI
import java.util.ArrayList
import java.util.Date

class AttachTranscriptionOperationHandlerTest {

    /** The operation handler to test  */
    private var operationHandler: AttachTranscriptionOperationHandler? = null

    /** The transcription service  */
    private var service: TranscriptionService? = null

    /** The operation instance  */
    private var operation: WorkflowOperationInstance? = null

    private var mediaPackage: MediaPackage? = null
    private var workflowInstance: WorkflowInstance? = null
    private var job1: Job? = null
    private var job2: Job? = null
    private var captionService: CaptionService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // Media package set up
        val mediaPackageURI = StartTranscriptionOperationHandlerTest::class.java.getResource("/mp.xml").toURI()
        mediaPackage = builder.loadFromXml(mediaPackageURI.toURL().openStream())
        val dfxpURI = StartTranscriptionOperationHandlerTest::class.java.getResource("/attachment_dfxp.xml").toURI()
        val dfxpXml = FileUtils.readFileToString(File(dfxpURI))
        val captionDfxp = MediaPackageElementParser.getFromXml(dfxpXml) as Attachment
        val vttURI = StartTranscriptionOperationHandlerTest::class.java.getResource("/attachment_vtt.xml").toURI()
        val vttXml = FileUtils.readFileToString(File(vttURI))
        val captionVtt = MediaPackageElementParser.getFromXml(vttXml) as Attachment

        // Service registry set up
        job1 = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job1!!.id).andReturn(1L)
        EasyMock.expect(job1!!.payload).andReturn(dfxpXml).anyTimes()
        EasyMock.expect<Status>(job1!!.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job1!!.dateCreated).andReturn(Date())
        EasyMock.expect(job1!!.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job1!!.queueTime).andReturn(0)
        EasyMock.replay(job1!!)

        job2 = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job2!!.id).andReturn(2L)
        EasyMock.expect(job2!!.payload).andReturn(vttXml).anyTimes()
        EasyMock.expect<Status>(job2!!.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job2!!.dateCreated).andReturn(Date())
        EasyMock.expect(job2!!.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job2!!.queueTime).andReturn(0)
        EasyMock.replay(job2!!)

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1)
        EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job2)
        EasyMock.replay(serviceRegistry)

        // Transcription service set up
        service = EasyMock.createStrictMock<TranscriptionService>(TranscriptionService::class.java)

        EasyMock.expect<MediaPackageElement>(service!!.getGeneratedTranscription("mpId1", "transcriptionJob")).andReturn(captionDfxp)
        EasyMock.expect(service!!.language).andReturn("en").once()
        EasyMock.expect<MediaPackageElement>(service!!.getGeneratedTranscription("mpId2", "transcriptionJob")).andReturn(captionVtt)
        EasyMock.expect(service!!.language).andReturn("en").once()
        EasyMock.replay(service!!)

        // Caption service set up
        captionService = EasyMock.createNiceMock<CaptionService>(CaptionService::class.java)

        // Workspace set up
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.moveTo(EasyMock.anyObject(URI::class.java), EasyMock.anyObject(String::class.java),
                EasyMock.anyObject(String::class.java), EasyMock.anyObject(String::class.java)))
                .andReturn(URI("http://opencast.server.com/captions.xml")) // just something valid
        EasyMock.replay(workspace)

        // Workflow set up
        val def = WorkflowDefinitionImpl()
        def.id = "DCE-start-transcription"
        def.isPublished = true
        workflowInstance = WorkflowInstanceImpl(def, mediaPackage, null, null, null, null)
        workflowInstance!!.id = 1
        operation = WorkflowOperationInstanceImpl("attach-transcript", OperationState.RUNNING)
        val operationList = ArrayList<WorkflowOperationInstance>()
        operationList.add(operation)
        workflowInstance!!.operations = operationList

        // Operation handler set up
        operationHandler = AttachTranscriptionOperationHandler()
        operationHandler!!.setTranscriptionService(service)
        operationHandler!!.setServiceRegistry(serviceRegistry)
        operationHandler!!.setCaptionService(captionService)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setJobBarrierPollingInterval(1L)
    }

    @Test
    @Throws(Exception::class)
    fun testStartDfxp() {
        EasyMock.expect(captionService!!.convert(EasyMock.anyObject(Attachment::class.java), EasyMock.anyObject(String::class.java),
                EasyMock.anyObject(String::class.java), EasyMock.anyObject(String::class.java))).andReturn(job1)
        EasyMock.replay(captionService!!)

        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID, "transcriptionJob")
        // operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_FLAVOR, "captions/timedtext");
        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TARGET_TAG, "tag1,tag2")
        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TARGET_CAPTION_FORMAT, "dfxp")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        val updatedMp = result.mediaPackage
        val attachments = updatedMp.getAttachments(MediaPackageElementFlavor.parseFlavor("captions/dfxp+en"))

        Assert.assertNotNull(attachments)
        Assert.assertEquals(1, attachments.size.toLong())
        Assert.assertNotNull(attachments[0].tags)
        Assert.assertEquals(3, attachments[0].tags.size.toLong())
        Assert.assertEquals("lang:en", attachments[0].tags[0])
        Assert.assertEquals("tag1", attachments[0].tags[1])
        Assert.assertEquals("tag2", attachments[0].tags[2])
    }

    @Test
    @Throws(Exception::class)
    fun testStartWebVtt() {
        EasyMock.expect(captionService!!.convert(EasyMock.anyObject(Attachment::class.java), EasyMock.anyObject(String::class.java),
                EasyMock.anyObject(String::class.java), EasyMock.anyObject(String::class.java))).andReturn(job2)
        EasyMock.replay(captionService!!)

        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID, "transcriptionJob")
        // operation.setConfiguration(AttachTranscriptionOperationHandler.TARGET_FLAVOR, "captions/timedtext");
        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TARGET_TAG, "tag1,tag2")
        operation!!.setConfiguration(AttachTranscriptionOperationHandler.TARGET_CAPTION_FORMAT, "vtt")

        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        val updatedMp = result.mediaPackage
        val attachments = updatedMp.getAttachments(MediaPackageElementFlavor.parseFlavor("captions/vtt+en"))

        Assert.assertNotNull(attachments)
        Assert.assertEquals(1, attachments.size.toLong())
        Assert.assertNotNull(attachments[0].tags)
        Assert.assertEquals(3, attachments[0].tags.size.toLong())
        Assert.assertEquals("lang:en", attachments[0].tags[0])
        Assert.assertEquals("tag1", attachments[0].tags[1])
        Assert.assertEquals("tag2", attachments[0].tags[2])
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testStartMissingTargetFlavor() {
        operationHandler!!.start(workflowInstance, null)
    }
}
