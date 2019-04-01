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
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfile.MediaType
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap

class MultiEncodeWorkflowOperationHandlerTest {
    private var operationHandler: MultiEncodeWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null
    private var mpEncode: MediaPackage? = null
    private var job: Job? = null
    private var job2: Job? = null
    private var sourceTracks: Array<Track>? = null
    private var encodedTracks: Array<Track>? = null
    private var encodedTracks2: Array<Track>? = null
    private var profileList: Array<EncodingProfile>? = null

    // mock services and objects
    private var profile: EncodingProfile? = null
    private var profile2: EncodingProfile? = null
    private var profile3: EncodingProfile? = null
    private var composerService: ComposerService? = null
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/multiencode_mediapackage.xml").toURI() // has 2
        // tracks
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/multiencode_results_mediapackage.xml")
                .toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())
        mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream())
        // String theString = IOUtils.toString(uriMPEncode.toURL().openStream());
        sourceTracks = mp!!.tracks
        encodedTracks = mpEncode!!.tracks
        encodedTracks2 = arrayOfNulls(encodedTracks!!.size)
        encodedTracks2[0] = encodedTracks!![0].clone() as Track
        encodedTracks2[1] = encodedTracks!![1].clone() as Track
        encodedTracks2!![0].setURI(URI("media$SUFFIX3"))
        encodedTracks2!![0].identifier = ENCODED_TRACK_ID3
        encodedTracks2!![1].identifier = ENCODED_TRACK_ID4
        val encodedXml = MediaPackageElementParser.getArrayAsXml(Arrays.asList(*encodedTracks!!))
        val encodedXml2 = MediaPackageElementParser.getArrayAsXml(Arrays.asList(*encodedTracks2!!))
        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String)).andReturn(uriMP)
        EasyMock.replay(workspace!!)

        // set up mock receipt
        job = EasyMock.createNiceMock<Job>(Job::class.java)
        job2 = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect<Status>(job!!.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job!!.dateCreated).andReturn(Date())
        EasyMock.expect(job!!.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job!!.queueTime).andReturn(0)
        EasyMock.expect(job!!.payload).andReturn(encodedXml).anyTimes()
        EasyMock.expect<Status>(job2!!.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job2!!.dateCreated).andReturn(Date())
        EasyMock.expect(job2!!.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job2!!.queueTime).andReturn(0)
        EasyMock.expect(job2!!.payload).andReturn(encodedXml2).anyTimes()
        EasyMock.replay(job, job2)

        // set up mock service registry
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job2)
        EasyMock.replay(serviceRegistry)

        // set up mock profiles
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andStubReturn(PROFILE1_ID)
        EasyMock.expect(profile!!.applicableMediaType).andStubReturn(MediaType.Stream)
        EasyMock.expect(profile!!.outputType).andStubReturn(MediaType.AudioVisual)
        EasyMock.expect(profile!!.suffix).andStubReturn(SUFFIX1)
        profile2 = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile2!!.identifier).andStubReturn(PROFILE2_ID)
        EasyMock.expect(profile2!!.applicableMediaType).andStubReturn(MediaType.Stream)
        EasyMock.expect(profile2!!.outputType).andStubReturn(MediaType.Visual)
        EasyMock.expect(profile2!!.suffix).andStubReturn(SUFFIX2)
        profile3 = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile3!!.identifier).andStubReturn(PROFILE3_ID)
        EasyMock.expect(profile3!!.applicableMediaType).andStubReturn(MediaType.Stream)
        EasyMock.expect(profile3!!.outputType).andStubReturn(MediaType.Visual)
        EasyMock.expect(profile3!!.suffix).andStubReturn(SUFFIX3)
        profileList = arrayOf<EncodingProfile>(profile, profile2, profile3)
        EasyMock.replay(profile, profile2, profile3)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.getProfile(PROFILE1_ID)).andStubReturn(profile)
        EasyMock.expect(composerService!!.getProfile(PROFILE2_ID)).andStubReturn(profile2)
        EasyMock.expect(composerService!!.getProfile(PROFILE3_ID)).andStubReturn(profile3)
        EasyMock.expect(composerService!!.multiEncode(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as List<String>))
                .andReturn(job)
        EasyMock.expect(composerService!!.multiEncode(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as List<String>))
                .andReturn(job2)
        EasyMock.replay(composerService!!)

        // set up services
        operationHandler = MultiEncodeWorkflowOperationHandler()
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
        operationHandler!!.setComposerService(composerService)
        operationHandler!!.setJobBarrierPollingInterval(0)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackTwoFlavorsNoTags() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/source;presentation/source"
        configurations["target-flavors"] = "presenter/work;presentation/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE3_ID;$PROFILE2_ID,$PROFILE3_ID"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track

        Assert.assertTrue(tracksEncoded.size == 6)
        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertEquals(0, trackEncoded.tags.size.toLong())
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        Assert.assertEquals(0, trackEncoded.tags.size.toLong())
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackTwoFlavorsOneProfileSet() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/source;presentation/source"
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE2_ID"
        configurations["target-tags"] = "1,2" // 2 tags, no profile tag

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track

        Assert.assertTrue(tracksEncoded.size == 6)
        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertEquals(2, trackEncoded.tags.size.toLong())
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        Assert.assertEquals(2, trackEncoded.tags.size.toLong())
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackTwoFlavors() {
        // operation configuration
        val targetTags = "archive,work"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/source;presentation/source"
        configurations["target-tags"] = targetTags // one
        configurations["target-flavors"] = "presenter/work;presentation/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE3_ID;$PROFILE2_ID,$PROFILE3_ID"
        configurations["tag-with-profile"] = "true"
        val targetTags1 = arrayOf("archive", PROFILE1_ID, "work")
        val targetTags2 = arrayOf("archive", PROFILE3_ID, "work")

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track

        Assert.assertTrue(tracksEncoded.size == 6)
        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags1, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags2, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackWildCardFlavors() {
        // operation configuration
        val targetTags = "archive,work"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "*/source"
        configurations["target-tags"] = targetTags // one
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE3_ID"
        configurations["tag-with-profile"] = "true"
        val targetTags1 = arrayOf("archive", PROFILE1_ID, "work")
        val targetTags2 = arrayOf("archive", PROFILE3_ID, "work")

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track

        Assert.assertTrue(tracksEncoded.size == 6)
        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags1, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags2, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackOneFlavor() {
        // operation configuration - single segment
        val targetTags = "archive,work"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presentation/source"
        configurations["target-tags"] = targetTags // one
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE2_ID"
        configurations["tag-with-profile"] = "true"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track

        Assert.assertTrue(tracksEncoded.size == 4)
        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        var tags = "$PROFILE1_ID,$targetTags".split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.sort(tags)
        Assert.assertArrayEquals(tags, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        tags = "$PROFILE2_ID,$targetTags".split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.sort(tags)
        Assert.assertArrayEquals(tags, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackTags() {
        // operation configuration
        val targetTags = "archive;work"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/source;presentation/source"
        configurations["target-tags"] = targetTags // one
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE3_ID"
        configurations["tag-with-profile"] = "true"
        val targetTags1 = arrayOf("archive", PROFILE1_ID)
        val targetTags2 = arrayOf(PROFILE3_ID, "work")

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track
        Assert.assertTrue(tracksEncoded.size == 6)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags1, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3)
        Assert.assertTrue("presentation/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags2, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID2 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackSourceTagAndFlavor() {
        // operation configuration
        val targetTags1 = arrayOf("archive", PROFILE1_ID)
        val targetTags2 = arrayOf("archive", PROFILE2_ID)
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/source;presentation/source"
        configurations["source-tags"] = "source" // one
        configurations["target-tags"] = "archive" // one
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE2_ID"
        configurations["tag-with-profile"] = "true"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track
        Assert.assertTrue(tracksEncoded.size == 4)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags1, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags2, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeEncodedTrackSourceTagsOnly() {
        // operation configuration
        val targetTags = "archive,work"
        val configurations = HashMap<String, String>()
        configurations["source-tags"] = "source,archive" // Should pick both tracks
        configurations["target-tags"] = targetTags
        configurations["target-flavors"] = "*/work"
        configurations["encoding-profiles"] = "$PROFILE1_ID,$PROFILE2_ID"
        configurations["tag-with-profile"] = "true"
        val targetTags1 = arrayOf("archive", PROFILE1_ID, "work")
        val targetTags2 = arrayOf("archive", PROFILE2_ID, "work")

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        // check track metadata
        val mpNew = result.mediaPackage
        val tracksEncoded = mpNew.tracks
        var trackEncoded: Track
        Assert.assertTrue(tracksEncoded.size == 6)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags1, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)

        trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2)
        Assert.assertTrue("presenter/work" == trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags2, trackEncoded.tags)
        Assert.assertTrue(SOURCE_TRACK_ID1 == trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testComposeMissingProfile() {
        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE1_ID)
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Stream)
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.Stream)
        profileList = arrayOf<EncodingProfile>(profile)
        EasyMock.replay(profile!!)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.listProfiles()).andReturn(profileList)
        EasyMock.expect(composerService!!.multiEncode(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as List<String>))
                .andReturn(job).anyTimes()
        EasyMock.replay(composerService!!)
        operationHandler!!.setComposerService(composerService)

        val configurations = HashMap<String, String>()
        try {
            // no encoding profile
            configurations["source-flavors"] = "multitrack/source"
            getWorkflowOperationResult(mp, configurations)
            Assert.fail("Since encoding profile is not specified exception should be thrown")
        } catch (e: WorkflowOperationException) {
            // expecting exception
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
        operation.template = "multiencode"
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
        private val PROFILE2_ID = "h264-low.http"
        private val PROFILE1_ID = "flv.rtmp"
        private val PROFILE3_ID = "webm.preview"
        private val SUFFIX2 = "-low.mp4"
        private val SUFFIX1 = ".http.flv"
        private val SUFFIX3 = ".webm"
        private val SOURCE_TRACK_ID1 = "multiencode-workflow-operation-test-source-track-id1"
        private val SOURCE_TRACK_ID2 = "multiencode-workflow-operation-test-source-track-id2"
        private val ENCODED_TRACK_ID1 = "multiencode-workflow-operation-test-encode-track-id1"
        private val ENCODED_TRACK_ID2 = "multiencode-workflow-operation-test-encode-track-id2"
        private val ENCODED_TRACK_ID3 = "multiencode-workflow-operation-test-encode-track-id3"
        private val ENCODED_TRACK_ID4 = "multiencode-workflow-operation-test-encode-track-id4"
    }

}
