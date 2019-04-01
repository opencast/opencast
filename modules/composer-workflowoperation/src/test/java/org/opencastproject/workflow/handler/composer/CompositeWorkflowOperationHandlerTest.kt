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
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
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

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap

class CompositeWorkflowOperationHandlerTest {
    private var operationHandler: CompositeWorkflowOperationHandler? = null

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
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/composite_mediapackage.xml").toURI()
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/compound_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())
        mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream())
        encodedTracks = mpEncode!!.tracks

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(
                workspace!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String)).andReturn(uriMPEncode)
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(
                File(javaClass.getResource("/watermark.jpg").toURI()))
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
        operationHandler = CompositeWorkflowOperationHandler()
        operationHandler!!.setJobBarrierPollingInterval(0)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testAll() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["source-flavor-watermark"] = "watermark/source"
        configurations["source-url-watermark"] = javaClass.getResource("/watermark.jpg").toExternalForm()
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = "test"
        configurations["layout-test"] = TEST_LAYOUT
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        val trackEncoded = mpNew.getTrack(COMPOUND_TRACK_ID)
        Assert.assertEquals("composite/work", trackEncoded.flavor.toString())
        Assert.assertTrue(Arrays.asList(*targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).containsAll(Arrays.asList(*trackEncoded.tags)))
    }

    @Test
    @Throws(Exception::class)
    fun testDesignatedAudioSource() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-audio-name"] = ComposerService.UPPER
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["source-flavor-watermark"] = "watermark/source"
        configurations["source-url-watermark"] = javaClass.getResource("/watermark.jpg").toExternalForm()
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = "test"
        configurations["layout-test"] = TEST_LAYOUT
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        val trackEncoded = mpNew.getTrack(COMPOUND_TRACK_ID)
        Assert.assertEquals("composite/work", trackEncoded.flavor.toString())
        Assert.assertTrue(Arrays.asList(*targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).containsAll(Arrays.asList(*trackEncoded.tags)))
    }

    @Test
    @Throws(Exception::class)
    fun testBothAudioSourceNoWatermark() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-audio-name"] = ComposerService.BOTH // equivalent to null
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = "test"
        configurations["layout-test"] = TEST_LAYOUT
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        val trackEncoded = mpNew.getTrack(COMPOUND_TRACK_ID)
        Assert.assertEquals("composite/work", trackEncoded.flavor.toString())
        Assert.assertTrue(Arrays.asList(*targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).containsAll(Arrays.asList(*trackEncoded.tags)))
    }


    @Test
    @Throws(Exception::class)
    fun testWithoutWatermark() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = "test"
        configurations["layout-test"] = TEST_LAYOUT
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        val trackEncoded = mpNew.getTrack(COMPOUND_TRACK_ID)
        Assert.assertEquals("composite/work", trackEncoded.flavor.toString())
        Assert.assertTrue(Arrays.asList(*targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).containsAll(Arrays.asList(*trackEncoded.tags)))
    }

    @Test
    @Throws(Exception::class)
    fun testSingleLayout() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = TEST_LAYOUT
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        Assert.assertEquals(Action.CONTINUE, result.action)
        val trackEncoded = mpNew.getTrack(COMPOUND_TRACK_ID)
        Assert.assertEquals("composite/work", trackEncoded.flavor.toString())
        Assert.assertTrue(Arrays.asList(*targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).containsAll(Arrays.asList(*trackEncoded.tags)))
    }

    @Test
    @Throws(Exception::class)
    fun testMissingLayout() {
        setMockups()

        // operation configuration
        val targetTags = "engage,compound"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-upper"] = "presenter/source"
        configurations["source-flavor-lower"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "composite/work"
        configurations["encoding-profile"] = "composite"
        configurations["layout"] = "test"
        configurations["layout-single"] = TEST_SINGLE_LAYOUT
        configurations["output-resolution"] = "1900x1080"
        configurations["output-background"] = "black"

        // run the operation handler
        try {
            getWorkflowOperationResult(mp, configurations)
        } catch (e: WorkflowOperationException) {
            return
        }

        Assert.fail("No error occurred when using missing layout")
    }

    @Test
    @Throws(URISyntaxException::class, MalformedURLException::class, MediaPackageException::class, IOException::class, IllegalArgumentException::class, NotFoundException::class, ServiceRegistryException::class)
    fun testSingleVideoStream() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/composite_mediapackage.xml").toURI()
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/compound_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())
        mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream())
        encodedTracks = mpEncode!!.tracks

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(
                workspace!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String)).andReturn(uriMPEncode)
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(
                File(javaClass.getResource("/watermark.jpg").toURI()))
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
        operationHandler = CompositeWorkflowOperationHandler()
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun setMockups() {
        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE_ID)
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Stream)
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.AudioVisual)
        EasyMock.replay(profile!!)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID)).andReturn(profile)
        EasyMock.expect(
                composerService!!.composite(EasyMock.anyObject<Any>() as Dimension, Option.option(EasyMock.anyObject<Any>() as LaidOutElement<Track>),
                        EasyMock.anyObject<Any>() as LaidOutElement<Track>,
                        EasyMock.anyObject<Any>() as Option<LaidOutElement<Attachment>>, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String)).andReturn(job)
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
        operation.template = "composite"
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
        private val PROFILE_ID = "composite"
        private val COMPOUND_TRACK_ID = "compound-workflow-operation-test-work"

        private val TEST_LAYOUT = "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}};{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}};{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}"
        private val TEST_SINGLE_LAYOUT = "{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\": {\"left\":1.0,\"top\":1.0} ,\"offset\": {\"y\":-20,\"x\":-20} ,\"reference\": {\"left\":1.0,\"top\":1.0} }}; {\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\": {\"left\":1.0,\"top\":0.0} ,\"offset\": {\"y\":20,\"x\":20} ,\"reference\": {\"left\":1.0,\"top\":0.0} }}"
    }

}
