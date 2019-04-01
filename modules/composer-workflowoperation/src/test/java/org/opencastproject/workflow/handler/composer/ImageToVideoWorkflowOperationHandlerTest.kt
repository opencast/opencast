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

package org.opencastproject.workflow.handler.composer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfile.MediaType
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

class ImageToVideoWorkflowOperationHandlerTest {
    private var operationHandler: ImageToVideoWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null
    private var mpEncode: MediaPackage? = null
    private var job: Job? = null
    private var encodedTracks: Array<Track>? = null

    // mock services and objects
    private var profile: EncodingProfile? = null
    private var composerService: ComposerService? = null
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/imagetovideo_mediapackage.xml").toURI()
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/imagetovideo_converted_mediapackage.xml")
                .toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())
        mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream())
        encodedTracks = mpEncode!!.tracks

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(
                workspace!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String)).andReturn(uriMP)
        EasyMock.replay(workspace!!)

        // set up mock receipt
        job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job!!.payload).andReturn(MediaPackageElementParser.getAsXml(encodedTracks!![0])).anyTimes()
        EasyMock.expect<Status>(job!!.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job!!.dateCreated).andReturn(Date())
        EasyMock.expect(job!!.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job!!.queueTime).andReturn(0)
        EasyMock.replay(job!!)

        // set up mock service registry
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)
        EasyMock.replay(serviceRegistry)

        // set up service
        operationHandler = ImageToVideoWorkflowOperationHandler()
        operationHandler!!.setJobBarrierPollingInterval(0)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testOneImageToVideo() {
        setMockups()
        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor"] = "image/intro"
        configurations["source-tag"] = "intro"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "video/intro"
        configurations["profile"] = "image-movie"
        configurations["duration"] = "5"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertNotNull(mpNew.getTrack(CONVERTED_TRACK_ID))
    }

    @Test
    @Throws(Exception::class)
    fun testOneImageToVideoWithMissingParams() {
        setMockups()
        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor"] = "image/intro"
        configurations["source-tag"] = "intro"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "video/intro"
        configurations["profile"] = "image-movie"

        try {
            // run the operation handler
            val result = getWorkflowOperationResult(mp, configurations)
            Assert.fail()
        } catch (e: WorkflowOperationException) {
            Assert.assertNotNull("Duration is required!", e)
        }

    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun setMockups() {
        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE_ID)
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Image)
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.AudioVisual)
        EasyMock.replay(profile!!)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID)).andReturn(profile)
        EasyMock.expect(
                composerService!!.imageToVideo(EasyMock.anyObject<Any>() as Attachment, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyLong().toDouble())).andReturn(job)
        EasyMock.replay(composerService!!)
        operationHandler!!.setComposerService(composerService)
    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?, configurations: Map<String, String>): WorkflowOperationResult {
        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        operation.template = PROFILE_ID
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

    companion object {

        // constant metadata values
        private val PROFILE_ID = "image-movie"
        private val CONVERTED_TRACK_ID = "converted-image"
    }

}
