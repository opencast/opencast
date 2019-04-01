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

package org.opencastproject.workflow.handler.animate

import org.easymock.EasyMock.anyBoolean
import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.anyString

import org.opencastproject.animate.api.AnimateService
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderImpl
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

class AnimateWorkflowOperationHandlerTest {

    private var handler: AnimateWorkflowOperationHandler? = null
    private var workflow: WorkflowInstanceImpl? = null
    private var instance: WorkflowOperationInstance? = null
    private var file: File? = null

    @Rule
    var testFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        handler = object : AnimateWorkflowOperationHandler() {
            @Throws(IllegalStateException::class, IllegalArgumentException::class)
            override fun waitForStatus(vararg jobs: Job): JobBarrier.Result {
                val result = EasyMock.createNiceMock<JobBarrier.Result>(JobBarrier.Result::class.java)
                EasyMock.expect(result.isSuccess).andReturn(true).anyTimes()
                EasyMock.replay(result)
                return result
            }
        }

        file = File(javaClass.getResource("/dc-episode.xml").toURI())

        val mediaPackage = MediaPackageBuilderImpl().createNew()
        mediaPackage.identifier = IdImpl("123-456")

        val `in` = FileInputStream(file!!)
        val catalog = DublinCores.read(`in`)
        catalog.flavor = MediaPackageElements.EPISODE
        //catalog.setURI(getClass().getResource("/dc-episode.xml").toURI());
        mediaPackage.add(catalog)

        instance = EasyMock.createNiceMock<WorkflowOperationInstance>(WorkflowOperationInstanceImpl::class.java)
        EasyMock.expect(instance!!.getConfiguration("target-flavor")).andReturn("a/b").anyTimes()
        EasyMock.expect(instance!!.getConfiguration("target-tags")).andReturn("a,b,c").anyTimes()

        workflow = EasyMock.createMock<WorkflowInstanceImpl>(WorkflowInstanceImpl::class.java)
        EasyMock.expect(workflow!!.mediaPackage).andReturn(mediaPackage).anyTimes()
        EasyMock.expect(workflow!!.currentOperation).andReturn(instance).anyTimes()

        var job: Job = JobImpl(0)
        job.payload = file!!.absolutePath

        val animateService = EasyMock.createMock<AnimateService>(AnimateService::class.java)
        EasyMock.expect(animateService.animate(anyObject(), anyObject(), anyObject())).andReturn(job)

        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.put(anyString(), anyString(), anyString(), anyObject())).andReturn(file!!.toURI()).anyTimes()
        EasyMock.expect(workspace.read(anyObject()))
                .andAnswer { javaClass.getResourceAsStream("/dc-episode.xml") }.anyTimes()
        workspace.cleanup(anyObject(Id::class.java))
        EasyMock.expectLastCall<Any>()
        workspace.delete(anyObject(URI::class.java))
        EasyMock.expectLastCall<Any>()

        job = JobImpl(1)
        job.payload = MediaPackageElementParser.getAsXml(TrackImpl())
        val mediaInspectionService = EasyMock.createMock<MediaInspectionService>(MediaInspectionService::class.java)
        EasyMock.expect(mediaInspectionService.enrich(anyObject<MediaPackageElement>(), anyBoolean())).andReturn(job).once()

        EasyMock.replay(animateService, workspace, workflow, mediaInspectionService)

        handler!!.setAnimateService(animateService)
        handler!!.setMediaInspectionService(mediaInspectionService)
        handler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testStart() {
        EasyMock.expect(instance!!.getConfiguration("animation-file")).andReturn(file!!.absolutePath).anyTimes()
        EasyMock.expect(instance!!.getConfiguration("fps")).andReturn("24").anyTimes()
        EasyMock.replay(instance!!)
        Assert.assertTrue(handler!!.start(workflow, null).allowsContinue())
    }

    @Test
    fun testNoAnimation() {
        EasyMock.replay(instance!!)
        try {
            handler!!.start(workflow, null)
        } catch (e: WorkflowOperationException) {
            return
        }

        // We expect this to fail and the test should never reach this point
        Assert.fail()
    }

    @Test
    @Throws(Exception::class)
    fun testCustomCmdArgs() {
        EasyMock.expect(instance!!.getConfiguration("animation-file")).andReturn(file!!.absolutePath).anyTimes()
        EasyMock.expect(instance!!.getConfiguration("cmd-args")).andReturn("-t ffmpeg -w 160 -h 90").anyTimes()
        EasyMock.replay(instance!!)
        Assert.assertTrue(handler!!.start(workflow, null).allowsContinue())
    }

}
