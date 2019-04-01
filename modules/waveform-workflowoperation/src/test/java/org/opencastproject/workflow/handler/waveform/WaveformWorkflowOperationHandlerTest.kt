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

package org.opencastproject.workflow.handler.waveform

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderImpl
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.waveform.api.WaveformService
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.Arrays
import java.util.LinkedList

class WaveformWorkflowOperationHandlerTest {

    private var track: TrackImpl? = null
    private var handler: WaveformWorkflowOperationHandler? = null
    private var workflow: WorkflowInstanceImpl? = null
    private var instance: WorkflowOperationInstance? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {

        handler = object : WaveformWorkflowOperationHandler() {
            @Throws(IllegalStateException::class, IllegalArgumentException::class)
            override fun waitForStatus(vararg jobs: Job): JobBarrier.Result {
                val result = EasyMock.createNiceMock<JobBarrier.Result>(JobBarrier.Result::class.java)
                EasyMock.expect(result.isSuccess).andReturn(true).anyTimes()
                EasyMock.replay(result)
                return result
            }
        }

        track = TrackImpl()
        track!!.flavor = MediaPackageElementFlavor.parseFlavor("xy/source")
        track!!.setAudio(Arrays.asList<AudioStream>(null, null))

        val builder = MediaPackageBuilderImpl()
        val mediaPackage = builder.createNew()
        mediaPackage.identifier = IdImpl("123-456")
        mediaPackage.add(track!!)

        instance = EasyMock.createNiceMock<WorkflowOperationInstance>(WorkflowOperationInstanceImpl::class.java)
        EasyMock.expect(instance!!.getConfiguration("target-flavor")).andReturn("*/*").anyTimes()
        EasyMock.expect(instance!!.getConfiguration("target-tags")).andReturn("a,b,c").anyTimes()

        workflow = EasyMock.createNiceMock<WorkflowInstanceImpl>(WorkflowInstanceImpl::class.java)
        EasyMock.expect(workflow!!.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflow!!.currentOperation).andReturn(instance).anyTimes()

        val payload = AttachmentImpl()
        payload.identifier = "x"
        payload.flavor = MediaPackageElementFlavor.parseFlavor("xy/source")
        val job = JobImpl(0)
        job.payload = MediaPackageElementParser.getAsXml(payload)

        val waveformService = EasyMock.createNiceMock<WaveformService>(WaveformService::class.java)
        EasyMock.expect(waveformService.createWaveformImage(EasyMock.anyObject<Track>(), EasyMock.anyInt(), EasyMock.anyInt(),
                EasyMock.anyInt(), EasyMock.anyInt(), EasyMock.anyString())).andReturn(job)

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)

        EasyMock.replay(waveformService, workspace, workflow)

        handler!!.setWaveformService(waveformService)
        handler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testStart() {
        EasyMock.expect(instance!!.getConfiguration("source-flavor")).andReturn("*/source").anyTimes()
        EasyMock.replay(instance!!)
        Assert.assertTrue(handler!!.start(workflow, null).allowsContinue())
    }

    @Test
    @Throws(Exception::class)
    fun testNoTracks() {
        EasyMock.expect(instance!!.getConfiguration("source-flavor")).andReturn("*/nothing").anyTimes()
        EasyMock.replay(instance!!)
        Assert.assertTrue(handler!!.start(workflow, null).allowsContinue())
    }

    @Test
    @Throws(Exception::class)
    fun testNoAudio() {
        track!!.setAudio(LinkedList<AudioStream>())
        EasyMock.expect(instance!!.getConfiguration("source-flavor")).andReturn("*/source").anyTimes()
        EasyMock.replay(instance!!)
        Assert.assertTrue(handler!!.start(workflow, null).allowsContinue())
    }

    @Test
    @Throws(Exception::class)
    fun testMissingSource() {
        EasyMock.replay(instance!!)
        try {
            handler!!.start(workflow, null)
            Assert.fail()
        } catch (e: WorkflowOperationException) {
            Assert.assertTrue(e.message.startsWith("Required property "))
        }

    }

}
