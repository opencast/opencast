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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfileImpl
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Collections
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList

/**
 * Test class for [PartialImportWorkflowOperationHandler]
 */
class PartialImportWorkflowOperationHandlerTest {

    @Test
    @Throws(URISyntaxException::class)
    fun trackNeedsTobeEncodedToStandardInputMp4ReturnsFalse() {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI())
                .andReturn(
                        URI(
                                "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mp4"))
                .anyTimes()
        EasyMock.replay(track)
        val result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions)
        assertFalse(result)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun trackNeedsTobeEncodedToStandardInputNoExtensionReturnsTrue() {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI())
                .andReturn(
                        URI(
                                "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio"))
                .anyTimes()
        EasyMock.replay(track)
        val result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions)
        assertTrue(result)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun trackNeedsTobeEncodedToStandardInputOnlyPeriodExtensionReturnsTrue() {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI())
                .andReturn(
                        URI(
                                "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio."))
                .anyTimes()
        EasyMock.replay(track)
        val result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions)
        assertTrue(result)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun trackNeedsTobeEncodedToStandardInputMovExtensionOnlyMp4AllowedReturnsTrue() {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI())
                .andReturn(
                        URI(
                                "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mov"))
                .anyTimes()
        EasyMock.replay(track)
        val result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions)
        assertTrue(result)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun trackNeedsTobeEncodedToStandardInputMovMp4AndMovAllowedReturnsFalse() {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI())
                .andReturn(
                        URI(
                                "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mov"))
                .anyTimes()
        EasyMock.replay(track)
        val result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, moreExtensions)
        assertFalse(result)
    }

    @Throws(URISyntaxException::class)
    private fun createTrack(flavor: MediaPackageElementFlavor, filename: String, video: Boolean, audio: Boolean): Track {
        val track = EasyMock.createMock<Track>(Track::class.java)
        EasyMock.expect(track.flavor).andReturn(flavor).anyTimes()
        EasyMock.expect(track.hasAudio()).andReturn(audio).anyTimes()
        EasyMock.expect(track.hasVideo()).andReturn(video).anyTimes()
        EasyMock.expect(track.getURI()).andReturn(URI(filename)).anyTimes()
        EasyMock.replay(track)
        return track
    }

    @Test
    fun getRequiredExtensionsInput3ExtensionsExpect3InList() {
        val operation = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        EasyMock.expect(operation.getConfiguration("required-extensions")).andReturn("mp4,mov,m4a")
        EasyMock.replay(operation)
        val handler = PartialImportWorkflowOperationHandler()
        val result = handler.getRequiredExtensions(operation)
        assertEquals("There should be 3 required extensions", 3, result.size.toLong())
    }

    @Test
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class, URISyntaxException::class)
    fun checkForMuxingInputPresenterVideoPresenterAudioNoAudioSuffixExpectsNoMux() {
        // Setup tracks
        val audioTrack = createTrack(PRESENTER_TARGET_FLAVOR, "audio.mp4", false, true)
        val videoTrack = createTrack(PRESENTER_TARGET_FLAVOR, "video.mp4", true, false)
        val tracks = arrayOf(audioTrack, videoTrack)
        // Setup media package
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediaPackage.tracks).andReturn(tracks).anyTimes()

        val composerService = EasyMock.createMock<ComposerService>(ComposerService::class.java)
        // Replay all mocks
        EasyMock.replay(composerService, mediaPackage)
        // Make sure that the composer service was not called.
        EasyMock.verify(composerService)

        val handler = PartialImportWorkflowOperationHandler()
        handler.setComposerService(composerService)
        handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
                ArrayList())
    }

    @Test
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class, URISyntaxException::class)
    fun checkForMuxingInputPresentationVideoPresentationAudioExpectsNoMux() {
        // Setup tracks
        val audioTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "audio.mp4", false, true)
        val videoTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "video.mp4", true, false)
        val tracks = arrayOf(audioTrack, videoTrack)
        // Setup media package
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediaPackage.tracks).andReturn(tracks).anyTimes()

        val composerService = EasyMock.createMock<ComposerService>(ComposerService::class.java)
        // Replay all mocks
        EasyMock.replay(composerService, mediaPackage)
        // Make sure that the composer service was not called.
        EasyMock.verify(composerService)

        val handler = PartialImportWorkflowOperationHandler()
        handler.setComposerService(composerService)
        handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
                ArrayList())
    }

    @Test
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class, URISyntaxException::class)
    fun checkForMuxingInputPresenterVideoPresentationAudioExpectsMux() {
        // Setup tracks
        val audioTrack = createTrack(PRESENTER_TARGET_FLAVOR, "audio.mp4", false, true)
        val videoTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "video.mp4", true, false)
        val tracks = arrayOf(audioTrack, videoTrack)
        // Setup media package
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediaPackage.tracks).andReturn(tracks).anyTimes()
        // Create a Job for the mux Job to return.
        val muxJob = EasyMock.createMock<Job>(Job::class.java)
        EasyMock.expect(muxJob.id).andReturn(1L)
        // Create the composer service to track muxing of tracks.
        val composerService = EasyMock.createMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService.mux(videoTrack, audioTrack, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE))
                .andReturn(muxJob)
        // Service Registry
        val serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(muxJob)
        // Replay all mocks
        EasyMock.replay(composerService, mediaPackage, serviceRegistry)

        val handler = TestPartialImportWorkflowOperationHandler(videoTrack,
                audioTrack)
        handler.setComposerService(composerService)
        handler.setServiceRegistry(serviceRegistry)
        handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
                ArrayList())

    }

    @Test
    @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class, URISyntaxException::class)
    fun checkForMuxingInputPresentationVideoPresenterAudioExpectsMux() {
        // Setup tracks
        val audioTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "audio.mp4", false, true)
        val videoTrack = createTrack(PRESENTER_TARGET_FLAVOR, "video.mp4", true, false)
        val tracks = arrayOf(audioTrack, videoTrack)
        // Setup media package
        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediaPackage.tracks).andReturn(tracks).anyTimes()
        // Create a Job for the mux Job to return.
        val muxJob = EasyMock.createMock<Job>(Job::class.java)
        EasyMock.expect(muxJob.id).andReturn(1L)
        // Create the composer service to track muxing of tracks.
        val composerService = EasyMock.createMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService.mux(videoTrack, audioTrack, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE))
                .andReturn(muxJob)
        // Service Registry
        val serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(muxJob)
        // Replay all mocks
        EasyMock.replay(composerService, mediaPackage, serviceRegistry)

        val handler = TestPartialImportWorkflowOperationHandler(videoTrack,
                audioTrack)
        handler.setComposerService(composerService)
        handler.setServiceRegistry(serviceRegistry)
        handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
                ArrayList())
    }

    @Test
    @Throws(Exception::class)
    fun testDetermineDimension() {
        // Setup tracks
        val videoStream = VideoStreamImpl("test1")
        videoStream.setFrameWidth(80)
        videoStream.setFrameHeight(30)
        val videoStream2 = VideoStreamImpl("test2")
        videoStream2.setFrameWidth(101)
        videoStream2.setFrameHeight(50)

        val videoTrack = TrackImpl()
        videoTrack.setURI(URI.create("/test"))
        videoTrack.setVideo(Collections.list(videoStream as VideoStream))

        val videoTrack2 = TrackImpl()
        videoTrack2.setURI(URI.create("/test"))
        videoTrack2.setVideo(Collections.list(videoStream2 as VideoStream))

        val tracks = Collections.list(videoTrack as Track, videoTrack2 as Track)

        val encodingProfile = EncodingProfileImpl()
        encodingProfile.identifier = "test"

        val composerService = EasyMock.createMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(
                composerService.concat(encodingProfile.identifier, Dimension.dimension(101, 50), false,
                        *tracks.toTypedArray())).andReturn(null).once()
        EasyMock.expect(
                composerService.concat(encodingProfile.identifier, Dimension.dimension(100, 50), false,
                        *tracks.toTypedArray())).andReturn(null).once()
        EasyMock.replay(composerService)

        val handler = PartialImportWorkflowOperationHandler()
        handler.setComposerService(composerService)
        handler.startConcatJob(encodingProfile, tracks, -1.0f, false)
        handler.startConcatJob(encodingProfile, tracks, -1.0f, true)
    }

    /**
     * Test class to verify that muxing is done as expected without circumventing the service registry.
     */
    private inner class TestPartialImportWorkflowOperationHandler : PartialImportWorkflowOperationHandler {
        private val expectedVideo: Track? = null
        private val expectedAudio: Track? = null

        internal constructor() : super() {}

        internal constructor(expectedVideo: Track, expectedAudio: Track) {
            this.expectedVideo = expectedVideo
            this.expectedAudio = expectedAudio
        }

        @Throws(EncoderException::class, MediaPackageException::class, WorkflowOperationException::class, NotFoundException::class, ServiceRegistryException::class, IOException::class)
        override fun mux(mediaPackage: MediaPackage, video: Track, audio: Track, elementsToClean: MutableList<MediaPackageElement>): Long {
            if (expectedVideo == null || expectedAudio == null) {
                Assert.fail("This test was not expected to mux a video and audio track together.")
            } else if (expectedVideo !== video || expectedAudio !== audio) {
                Assert.fail("The expected tracks are not being muxed together.")
            }
            return 100L
        }

        @Throws(IllegalStateException::class, IllegalArgumentException::class)
        override fun waitForStatus(vararg jobs: Job): JobBarrier.Result {
            val result = EasyMock.createMock<JobBarrier.Result>(JobBarrier.Result::class.java)
            EasyMock.expect(result.isSuccess).andReturn(true)
            EasyMock.replay(result)
            return result
        }
    }

    companion object {

        // Target flavors
        private val PRESENTER_TARGET_FLAVOR_STRING = "presenter/target"
        private val PRESENTER_TARGET_FLAVOR = MediaPackageElementFlavor
                .parseFlavor(PRESENTER_TARGET_FLAVOR_STRING)
        private val PRESENTATION_TARGET_FLAVOR_STRING = "presentation/target"
        private val PRESENTATION_TARGET_FLAVOR = MediaPackageElementFlavor
                .parseFlavor(PRESENTATION_TARGET_FLAVOR_STRING)
        private val defaultExtensions = ArrayList<String>()
        private val moreExtensions = ArrayList<String>()

        @BeforeClass
        fun setUpClass() {
            defaultExtensions.add("mp4")
            moreExtensions.add("mp4")
            moreExtensions.add("mov")
        }
    }
}
