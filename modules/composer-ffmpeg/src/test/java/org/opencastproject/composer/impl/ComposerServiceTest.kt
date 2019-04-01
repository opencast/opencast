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

package org.opencastproject.composer.impl

import org.easymock.EasyMock.capture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.HorizontalCoverageLayoutSpec
import org.opencastproject.composer.layout.LayoutManager
import org.opencastproject.composer.layout.MultiShapeLayout
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.JsonObj
import org.opencastproject.util.MimeType
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.HashSet

/**
 * Tests the [ComposerServiceImpl].
 */
class ComposerServiceTest {
    /** The sources file to test with  */
    private var sourceVideoOnly: File? = null
    private val sourceVideosUnique = arrayOfNulls<File>(10)
    private var sourceAudioOnly: File? = null
    private var sourceImage: File? = null

    /** The composer service to test  */
    private var composerService: ComposerServiceImpl? = null
    private var sourceVideoTrack: Track? = null
    private var sourceAudioTrack: Track? = null
    private var inspectedTrack: Track? = null

    /** Encoding profile scanner  */
    private var profileScanner: EncodingProfileScanner? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Skip tests if FFmpeg is not installed
        Assume.assumeTrue(ffmpegInstalled)

        // Create video only file
        var f = getFile("/video.mp4")
        sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.name), ".mp4", testDir)
        FileUtils.copyFile(f, sourceVideoOnly!!)

        // Create another audio only file
        f = getFile("/audio.mp3")
        sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.name), ".mp3", testDir)
        FileUtils.copyFile(f, sourceAudioOnly!!)

        // Create an image file
        f = getFile("/image.jpg")
        sourceImage = File.createTempFile(FilenameUtils.getBaseName(f.name), ".jpg", testDir)
        FileUtils.copyFile(f, sourceImage!!)

        // create the needed mocks
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bc.getProperty(EasyMock.anyString())).andReturn(FFMPEG_BINARY)

        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()

        val org = DefaultOrganization()
        val roles = HashSet<JaxbRole>()
        roles.add(JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org, ""))
        val user = JaxbUser("admin", "test", org, roles)
        val orgDirectory = EasyMock.createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect<Organization>(orgDirectory.getOrganization(EasyMock.anyObject<Any>() as String)).andReturn(org).anyTimes()

        val userDirectory = EasyMock.createNiceMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes()

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<Organization>(securityService.organization).andReturn(org).anyTimes()
        EasyMock.expect(securityService.user).andReturn(user).anyTimes()

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceVideoOnly).anyTimes()
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.eq(false))).andReturn(sourceVideoOnly).anyTimes()

        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.eq(true))).andAnswer {
            val f1 = getFile("/video.mp4")
            val uniqueSourceVideo = File.createTempFile(FilenameUtils.getBaseName(f1.name), ".mp4", testDir)
            FileUtils.copyFile(f1, uniqueSourceVideo)
            uniqueSourceVideo
        }.anyTimes()

        profileScanner = EncodingProfileScanner()
        val encodingProfile = getFile("/encodingprofiles.properties")
        assertNotNull("Encoding profile must exist", encodingProfile)
        profileScanner!!.install(encodingProfile)

        // Finish setting up the mocks
        EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace)

        // Create an encoding engine factory

        inspectedTrack = MediaPackageElementParser.getFromXml(IOUtils.toString(
                ComposerServiceTest::class.java.getResourceAsStream("/composer_test_source_track_video.xml"), Charset.defaultCharset())) as Track
        sourceVideoTrack = MediaPackageElementParser.getFromXml(IOUtils.toString(
                ComposerServiceTest::class.java.getResourceAsStream("/composer_test_source_track_video.xml"), Charset.defaultCharset())) as Track
        sourceAudioTrack = MediaPackageElementParser.getFromXml(IOUtils.toString(
                ComposerServiceTest::class.java.getResourceAsStream("/composer_test_source_track_audio.xml"), Charset.defaultCharset())) as Track

        // Create and populate the composer service
        composerService = object : ComposerServiceImpl() {
            @Throws(EncoderException::class)
            override fun inspect(job: Job, workspaceURI: URI): Track? {
                return inspectedTrack
            }
        }


        val serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        val type = EasyMock.newCapture<String>()
        val operation = EasyMock.newCapture<String>()
        val args = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
                .andAnswer {
                    // you could do work here to return something different if you needed.
                    val job = JobImpl(0)
                    job.jobType = type.value
                    job.operation = operation.value
                    job.arguments = args.getValue()
                    job.payload = composerService!!.process(job)
                    job
                }.anyTimes()
        composerService!!.serviceRegistry = serviceRegistry
        composerService!!.setProfileScanner(profileScanner)
        composerService!!.setWorkspace(workspace)

        EasyMock.replay(serviceRegistry)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(sourceVideoOnly)
        FileUtils.deleteQuietly(sourceAudioOnly)
        FileUtils.deleteQuietly(sourceImage)
    }

    @Test
    @Throws(Exception::class)
    fun testConcurrentExecutionWithSameSource() {
        assertTrue(sourceVideoOnly!!.isFile)
        val jobs = ArrayList<Job>()
        for (i in 0..9) {
            jobs.add(composerService!!.image(sourceVideoTrack, "player-preview.http", 1.0))
        }
        for (j in jobs) {
            MediaPackageElementParser.getFromXml(j.payload)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEncode() {
        assertTrue(sourceVideoOnly!!.isFile)

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceVideoOnly!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        val job = composerService!!.encode(sourceVideoTrack, "av.work")
        MediaPackageElementParser.getFromXml(job.payload)
    }

    @Test
    @Throws(Exception::class)
    fun testParallelEncode() {
        assertTrue(sourceVideoOnly!!.isFile)

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceVideoOnly!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        // Prepare job
        val job = composerService!!.parallelEncode(sourceVideoTrack, "parallel.http")
        assertEquals(3, MediaPackageElementParser.getArrayFromXml(job.payload).size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testTrim() {
        assertTrue(sourceVideoOnly!!.isFile)

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceVideoOnly!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        val job = composerService!!.trim(sourceVideoTrack, "trim.work", 0, 100)
        MediaPackageElementParser.getFromXml(job.payload)
    }

    @Test
    @Throws(Exception::class)
    fun testMux() {
        assertTrue(sourceVideoOnly!!.isFile)
        assertTrue(sourceAudioOnly!!.isFile)

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceVideoOnly).once()
        EasyMock.expect(workspace.get(EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(sourceAudioOnly).once()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceVideoOnly!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        val job = composerService!!.mux(sourceVideoTrack, sourceAudioTrack, "mux-av.work")
        MediaPackageElementParser.getFromXml(job.payload)
    }

    @Test
    @Throws(Exception::class)
    fun testConvertImage() {
        assertTrue(sourceImage!!.isFile)

        val imageAttachment = MediaPackageElementParser.getFromXml(IOUtils.toString(
                ComposerServiceTest::class.java.getResourceAsStream("/composer_test_source_attachment_image.xml"),
                Charset.defaultCharset())) as Attachment

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceImage).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceImage!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        val job = composerService!!.convertImage(imageAttachment, "image-conversion.http")
        MediaPackageElementParser.getFromXml(job.payload)
    }

    /**
     * Test method for
     * [ComposerServiceImpl.composite]
     */
    @Test
    @Throws(Exception::class)
    fun testComposite() {
        if (!ffmpegInstalled)
            return

        val outputDimension = Dimension(500, 500)

        val layouts = ArrayList<HorizontalCoverageLayoutSpec>()
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}")))
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}")))
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}")))

        val shapes = ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>()
        shapes.add(0, Tuple.tuple(Dimension(300, 300), layouts[0]))
        shapes.add(1, Tuple.tuple(Dimension(200, 200), layouts[1]))

        val multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes)

        val watermarkOption = Option.none<LaidOutElement<Attachment>>()
        val lowerLaidOutElement = LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.shapes[0])
        val upperLaidOutElement = LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.shapes[1])

        val composite = composerService!!.composite(outputDimension, Option.option(lowerLaidOutElement), upperLaidOutElement,
                watermarkOption, "composite.work", "black", "both")
        //  null or "both" means that both tracks are checked for audio and both audio tracks
        // are mixed into the final composite if they exist

        val compositeTrack = MediaPackageElementParser.getFromXml(composite.payload) as Track
        Assert.assertNotNull(compositeTrack)
        inspectedTrack!!.identifier = compositeTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, compositeTrack)
    }

    @Test
    @Throws(Exception::class)
    fun testCompositeAudio() {
        if (!ffmpegInstalled)
            return

        val outputDimension = Dimension(500, 500)

        val layouts = ArrayList<HorizontalCoverageLayoutSpec>()
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}")))
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":0.2,\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":0.0,\"top\":0.0}}}")))
        layouts.add(Serializer.horizontalCoverageLayoutSpec(JsonObj
                .jsonObj("{\"horizontalCoverage\":1.0,\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":1.0,\"top\":0.0}}}")))

        val shapes = ArrayList<Tuple<Dimension, HorizontalCoverageLayoutSpec>>()
        shapes.add(0, Tuple.tuple(Dimension(300, 300), layouts[0]))
        shapes.add(1, Tuple.tuple(Dimension(200, 200), layouts[1]))

        val multiShapeLayout = LayoutManager.multiShapeLayout(outputDimension, shapes)

        val watermarkOption = Option.none<LaidOutElement<Attachment>>()
        val lowerLaidOutElement = LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.shapes[0])
        val upperLaidOutElement = LaidOutElement<Track>(sourceVideoTrack, multiShapeLayout.shapes[1])

        val composite = composerService!!.composite(outputDimension, Option.option(lowerLaidOutElement), upperLaidOutElement,
                watermarkOption, "composite.work", "black", "upper")
        val compositeTrack = MediaPackageElementParser.getFromXml(composite.payload) as Track
        Assert.assertNotNull(compositeTrack)
        inspectedTrack!!.identifier = compositeTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, compositeTrack)
    }

    /**
     * Test method for [ComposerServiceImpl.concat]
     */
    @Test
    @Throws(Exception::class)
    fun testConcat() {
        val outputDimension = Dimension(500, 500)
        val concat = composerService!!.concat("concat.work", outputDimension, false, sourceVideoTrack, sourceVideoTrack)
        val concatTrack = MediaPackageElementParser.getFromXml(concat.payload) as Track
        Assert.assertNotNull(concatTrack)
        inspectedTrack!!.identifier = concatTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, concatTrack)
    }

    /**
     * Test method for [ComposerServiceImpl.concat]
     */
    @Test
    @Throws(Exception::class)
    fun testConcatWithFrameRate() {
        val outputDimension = Dimension(500, 500)
        val concat = composerService!!.concat("concat.work", outputDimension, 20.0f, false, sourceVideoTrack, sourceVideoTrack)
        val concatTrack = MediaPackageElementParser.getFromXml(concat.payload) as Track
        Assert.assertNotNull(concatTrack)
        inspectedTrack!!.identifier = concatTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, concatTrack)
    }

    /**
     * Test method for [ComposerServiceImpl.concat]
     */
    @Test
    @Throws(Exception::class)
    fun testConcatWithSameCodec() {
        val concat = composerService!!.concat("concat.work", null, true, sourceVideoTrack, sourceVideoTrack)
        val concatTrack = MediaPackageElementParser.getFromXml(concat.payload) as Track
        Assert.assertNotNull(concatTrack)
        inspectedTrack!!.identifier = concatTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, concatTrack)
    }

    /**
     * Test method for
     * [org.opencastproject.composer.impl.ComposerServiceImpl.imageToVideo]
     */
    @Test
    @Throws(Exception::class)
    fun testImageToVideo() {
        if (!ffmpegInstalled)
            return

        assertTrue(sourceImage!!.isFile)

        // Need different media files
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject())).andReturn(sourceImage).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject<InputStream>()))
                .andReturn(sourceImage!!.toURI()).anyTimes()
        composerService!!.setWorkspace(workspace)
        EasyMock.replay(workspace)

        val imageToVideoProfile = profileScanner!!.getProfile("image-movie.work")

        val attachment = AttachmentImpl.fromURI(sourceImage!!.toURI())
        attachment.identifier = "test image"

        val imageToVideo = composerService!!.imageToVideo(attachment, imageToVideoProfile.identifier, 1.0)
        val imageToVideoTrack = MediaPackageElementParser.getFromXml(imageToVideo.payload) as Track
        Assert.assertNotNull(imageToVideoTrack)

        inspectedTrack!!.identifier = imageToVideoTrack.identifier
        inspectedTrack!!.mimeType = MimeType.mimeType("video", "mp4")
        Assert.assertEquals(inspectedTrack, imageToVideoTrack)
    }

    companion object {

        /** FFmpeg binary location  */
        private val FFMPEG_BINARY = "ffmpeg"

        /** File pointer to the testing dir to not pollute tmp  */
        private val testDir = File("target")

        /** True to run the tests  */
        private var ffmpegInstalled = true

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(ComposerServiceTest::class.java)

        @BeforeClass
        fun testForFFmpeg() {
            try {
                val p = ProcessBuilder(FFMPEG_BINARY, "-version").start()
                if (p.waitFor() != 0)
                    throw IllegalStateException()
            } catch (t: Throwable) {
                logger.warn("Skipping composer tests due to missing ffmpeg")
                ffmpegInstalled = false
            }

        }

        @Throws(Exception::class)
        private fun getFile(path: String): File {
            return File(ComposerServiceTest::class.java.getResource(path).toURI())
        }
    }
}
