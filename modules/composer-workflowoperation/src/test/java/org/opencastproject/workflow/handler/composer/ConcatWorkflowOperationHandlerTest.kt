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
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport
import org.opencastproject.mediapackage.VideoStream
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

import org.apache.commons.lang3.StringUtils
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.Date
import java.util.HashMap


class ConcatWorkflowOperationHandlerTest {
    private var operationHandler: ConcatWorkflowOperationHandler? = null

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
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/concat_mediapackage.xml").toURI()
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/concatenated_mediapackage.xml").toURI()
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
        operationHandler = ConcatWorkflowOperationHandler()
        operationHandler!!.setJobBarrierPollingInterval(0)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testConcat2EncodedTracksWithFlavor() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "presenter/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID)
        Assert.assertEquals("presenter/concat", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), trackEncoded.tags)
    }

    @Test
    @Throws(Exception::class)
    fun testResolutionByTrackMandatory() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "presenter/source"
        configurations["source-flavor-part-1-mandatory"] = "true"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "part-1"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID)
        Assert.assertEquals("presenter/concat", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), trackEncoded.tags)
    }

    @Test
    @Throws(Exception::class)
    fun testResolutionByTrackNotMandatory() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "presenter/source"
        configurations["source-flavor-part-1-mandatory"] = "false"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "part-1"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testFrameRateFixedValue() {
        createTestFrameRateWithValue("25", 25.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testFrameRateFixedDecimalValue() {
        createTestFrameRateWithValue("25.000", 25.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testFrameRatePartValue() {
        val part1 = mp!!.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/source"))[0] as Track
        val videoStreams = TrackSupport.byType(part1.streams, VideoStream::class.java)
        createTestFrameRateWithValue("part-1", videoStreams[0].frameRate!!)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testFrameRateInvalidDigitPartValue() {
        createTestFrameRateWithValue("part-10", java.lang.Float.MAX_VALUE)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testFrameRateInvalidPartValue() {
        createTestFrameRateWithValue("part-foo", java.lang.Float.MAX_VALUE)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testFrameRateInvalidValue() {
        createTestFrameRateWithValue("foo", java.lang.Float.MAX_VALUE)
    }

    @Throws(Exception::class)
    protected fun createTestFrameRateWithValue(frameRateValue: String, expectedFrameRateValue: Float) {
        setMockupsWithFrameRate(expectedFrameRateValue)

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "presenter/source"
        configurations["source-flavor-part-1-mandatory"] = "true"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "part-1"
        configurations["output-framerate"] = frameRateValue

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID)
        Assert.assertEquals("presenter/concat", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), trackEncoded.tags)
    }

    @Test
    @Throws(Exception::class)
    fun testConcat2EncodedTracksWithTags() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "presenter/source"
        configurations["source-tags-part-0"] = "part0,part0b"
        configurations["source-tags-part-1"] = "part1"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID)
        Assert.assertEquals("presenter/concat", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags.split("\\W".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), trackEncoded.tags)
    }

    @Test
    @Throws(Exception::class)
    fun testConcatMandatoryCheck() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "test/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testConcatOptionalCheck() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-part-1"] = "test/source"
        configurations["source-flavor-part-1-mandatory"] = "false"
        configurations["source-flavor-part-2"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.CONTINUE, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testConcatLessTracks() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["target-tags"] = targetTags
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)

        // check track metadata
        val mpNew = result.mediaPackage
        val tracks = mpNew.getTracks(MediaPackageElementFlavor.parseFlavor("presenter/concat"))
        Assert.assertEquals(1, tracks.size.toLong())
        Assert.assertArrayEquals(StringUtils.split(targetTags, ","), tracks[0].tags)
    }


    @Test
    @Throws(Exception::class)
    fun testConcatNumberedFiles() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-numbered-files"] = "*/source"
        configurations["target-flavor"] = "presenter/concat"
        configurations["target-tags"] = targetTags
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID)
        Assert.assertEquals("presenter/concat", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(StringUtils.split(targetTags, ","), trackEncoded.tags)
    }

    @Test
    @Throws(Exception::class)
    fun testConcatSingleNumberedFiles() {
        setMockups()

        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavor-numbered-files"] = "presenter/source"
        configurations["target-flavor"] = "presenter/concat"
        configurations["target-tags"] = targetTags
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val tracks = mpNew.getTracks(MediaPackageElementFlavor.parseFlavor("presenter/concat"))
        val trackEncoded = tracks[0] // mpNew.getTrack(ENCODED_TRACK_ID);
        Assert.assertArrayEquals(StringUtils.split(targetTags, ","), trackEncoded.tags)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testConcatNumberedPrefixedFiles() {
        setMockups()

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-flavor-part-0"] = "presentation/source"
        configurations["source-flavor-numbered-files"] = "*/source"
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testConcatNumberedTaggedFiles() {
        setMockups()

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-tags-part-1"] = "part1"
        configurations["source-flavor-numbered-files"] = "*/source"
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["output-resolution"] = "1900x1080"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testConcatPrefixSameCodecFiles() {
        setMockups()

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-tags-part-1"] = "part1"
        configurations["source-tags-part-2"] = "part2"
        configurations["target-flavor"] = "presenter/concat"
        configurations["encoding-profile"] = "concat"
        configurations["same-codec"] = "true"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun setMockups() {
        setMockupsWithFrameRate(-1.0f)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    private fun setMockupsWithFrameRate(expectedFramerate: Float) {
        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE_ID)
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Stream)
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.AudioVisual)
        EasyMock.replay(profile!!)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID)).andReturn(profile)
        if (expectedFramerate > 0) {
            EasyMock.expect(composerService!!.concat(
                    EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as Dimension,
                    EasyMock.eq(expectedFramerate),
                    EasyMock.anyBoolean(), EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as Track)).andReturn(job)
        } else {
            EasyMock.expect(composerService!!.concat(
                    EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as Dimension, EasyMock.anyBoolean(),
                    EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as Track)).andReturn(job)
        }
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
        operation.template = "concat"
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
        private val PROFILE_ID = "concat"
        private val ENCODED_TRACK_ID = "concatenated-workflow-operation-test-encode-track-id"
    }

}
