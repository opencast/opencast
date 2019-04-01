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

package org.opencastproject.videoeditor.impl

import org.easymock.EasyMock.capture
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.ffmpeg.FFmpegAnalyzer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.api.SmilBody
import org.opencastproject.smil.entity.api.SmilHead
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup
import org.opencastproject.util.MimeTypes
import org.opencastproject.videoeditor.api.ProcessFailedException
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.util.ArrayList


/**
 * Test class for video editor
 */
class VideoEditorTest {
    @Rule
    var folder = TemporaryFolder()

    /** The in-memory service registration  */
    protected var serviceRegistry: ServiceRegistry? = null

    /** The video editor  */
    protected var veditor: VideoEditorServiceImpl? = null

    /** Temp storage in workspace  */
    protected var tempFile1: File? = null

    /** output track  */
    protected var inspectedTrack: Track? = null

    /**
     * Setup for the video editor service, including creation of a mock workspace and all dependencies.
     *
     * @throws Exception
     * if setup fails
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {

        val tmpDir = folder.newFolder(javaClass.name)
        tempFile1 = File(tmpDir, "testoutput.mp4") // output file

        /* mock the workspace for the input/output file */
        // workspace.get(new URI(sourceTrackUri));
        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.rootDirectory()).andReturn(tmpDir.absolutePath)
        EasyMock.expect(workspace.get(track1!!.getURI())).andReturn(File(track1!!.getURI())).anyTimes()
        EasyMock.expect(workspace.get(track2!!.getURI())).andReturn(File(track2!!.getURI())).anyTimes()
        EasyMock.expect(workspace.putInCollection(EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.anyObject(InputStream::class.java))).andAnswer {
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, FileOutputStream(tempFile1!!))
            tempFile1!!.toURI()
        }

        /* mock the role/org/security dependencies */
        val anonymous = JaxbUser("anonymous", "test", DefaultOrganization(), JaxbRole(
                DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, DefaultOrganization()))
        val userDirectoryService = EasyMock.createMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject<Any>() as String)).andReturn(anonymous).anyTimes()

        val organization = DefaultOrganization()
        val organizationDirectoryService = EasyMock.createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect(organizationDirectoryService.getOrganization(EasyMock.anyObject<Any>() as String))
                .andReturn(organization).anyTimes()

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.user).andReturn(anonymous).anyTimes()
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()

        /* mock the osgi init for the video editor itself */
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        val storageDir = folder.newFolder()
        logger.info("storageDir: {}", storageDir)
        EasyMock.expect(bc.getProperty("org.opencastproject.storage.dir")).andReturn(storageDir.path).anyTimes()
        EasyMock.expect(bc.getProperty("org.opencastproject.composer.ffmpegpath")).andReturn(FFMPEG_BINARY).anyTimes()
        EasyMock.expect(bc.getProperty(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG)).andReturn("ffprobe").anyTimes()
        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()
        EasyMock.replay(bc, cc, workspace, userDirectoryService, organizationDirectoryService, securityService)

        /* mock inspector output so that the job will alway pass */
        val sourceTrackXml = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source' id='deadbeef-a926-4ba9-96d9-2fafbcc30d2a'>"
                + "<audio id='audio-1'><encoder type='MP3 (MPEG audio layer 3)'/><channels>2</channels>"
                + "<bitrate>96000.0</bitrate></audio><video id='video-1'><device/>"
                + "<encoder type='FLV / Sorenson Spark / Sorenson H.263 (Flash Video)'/>"
                + "<bitrate>512000.0</bitrate><framerate>15.0</framerate>"
                + "<resolution>854x480</resolution></video>"
                + "<mimetype>video/mpeg</mimetype><url>video.mp4</url></track>")

        inspectedTrack = MediaPackageElementParser.getFromXml(sourceTrackXml) as Track
        veditor = object : VideoEditorServiceImpl() {
            @Throws(MediaInspectionException::class, ProcessFailedException::class)
            override fun inspect(job: Job, workspaceURI: URI): Job {
                val inspectionJob = EasyMock.createNiceMock<Job>(Job::class.java)
                try {
                    EasyMock.expect(inspectionJob.payload).andReturn(MediaPackageElementParser.getAsXml(inspectedTrack))
                } catch (e: MediaPackageException) {
                    throw MediaInspectionException(e)
                }

                EasyMock.replay(inspectionJob)
                return inspectionJob
            }
        }

        /* set up video editor */
        veditor!!.activate(cc)
        veditor!!.setWorkspace(workspace)
        veditor!!.securityService = securityService
        veditor!!.userDirectoryService = userDirectoryService
        veditor!!.setSmilService(smilService)
        veditor!!.organizationDirectoryService = organizationDirectoryService

        serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        val type = EasyMock.newCapture<String>()
        val operation = EasyMock.newCapture<String>()
        val args = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry!!.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
                .andAnswer {
                    val job = JobImpl(0)
                    logger.error("type: {}", type.value)
                    job.jobType = type.value
                    job.operation = operation.value
                    job.arguments = args.getValue()
                    job.payload = veditor!!.process(job)
                    job
                }.anyTimes()
        EasyMock.replay(serviceRegistry!!)

        veditor!!.serviceRegistry = serviceRegistry

    }

    /**
     * @throws java.io.File.IOException
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(tempFile1)
    }

    /**
     * Run the smil file and test that file is created
     */
    @Test
    @Throws(Exception::class)
    fun testAnalyze() {
        logger.debug("SMIL is " + smil!!.toXML())
        for (receipt in veditor!!.processSmil(smil!!)) {
            assertNotNull("Audiovisual content expected", receipt.payload)
            assertTrue("Merged File exists", tempFile1!!.exists())
            assertTrue("Merged video is is not empty", tempFile1!!.length() > 0)
            logger.info("Resulting file size {} ", tempFile1!!.length())
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VideoEditorTest::class.java)
        /** FFmpeg binary location  */
        private val FFMPEG_BINARY = "ffmpeg"
        /** SMIL file to run the editing  */
        protected var smil: Smil? = null

        /** Videos file to test. 2 videos of different framerate, must have same resolution  */
        protected val mediaResource = "/testresources/testvideo_320x180.mp4"// 320x180, 30fps h264
        protected val smilResource = "/testresources/SmilObjectToXml.xml"

        /** Duration of first and second movie  */
        protected val movieDuration = 217650L //3:37.65 seconds

        /** The smil service  */
        protected var smilService: SmilService? = null

        /** The media url  */
        protected var track1: TrackImpl? = null
        protected var track2: TrackImpl? = null


        /**
         * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
         * bundle.
         *
         * @throws Exception
         * if setup fails
         */
        @BeforeClass
        @Throws(Exception::class)
        fun setUpClass() {
            /* Set up the 2 tracks for merging */
            track1 = TrackImpl.fromURI(VideoEditorTest::class.java.getResource(mediaResource).toURI())
            track1!!.identifier = "track-1"
            track1!!.flavor = MediaPackageElementFlavor("source", "presentater")
            track1!!.mimeType = MimeTypes.MJPEG
            track1!!.addStream(VideoStreamImpl())
            track1!!.duration = movieDuration

            track2 = TrackImpl.fromURI(VideoEditorTest::class.java.getResource(mediaResource).toURI())
            track2!!.identifier = "track-2"
            track2!!.flavor = MediaPackageElementFlavor("source", "presentater")
            track2!!.mimeType = MimeTypes.MJPEG
            track2!!.addStream(VideoStreamImpl())
            track2!!.duration = movieDuration

            /* Start of Smil mockups */

            val mediaUrl = VideoEditorTest::class.java.getResource(mediaResource)
            val smilUrl = VideoEditorTest::class.java.getResource(smilResource)
            val smilString = IOUtils.toString(smilUrl)

            val trackParamGroupId = "pg-a6d8e576-495f-44c7-8ed7-b5b47c807f0f"

            val param1 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
            EasyMock.expect(param1.name).andReturn("track-id").anyTimes()
            EasyMock.expect(param1.value).andReturn("track-1").anyTimes()
            EasyMock.expect(param1.id).andReturn("param-e2f41e7d-caba-401b-a03a-e524296cb235").anyTimes()
            val param2 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
            EasyMock.expect(param2.name).andReturn("track-src").anyTimes()
            EasyMock.expect(param2.value).andReturn("file:" + mediaUrl.path).anyTimes()
            EasyMock.expect(param2.id).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes()
            val param3 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
            EasyMock.expect(param3.name).andReturn("track-flavor").anyTimes()
            EasyMock.expect(param3.value).andReturn("source/presenter").anyTimes()
            EasyMock.expect(param3.id).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes()
            EasyMock.replay(param1, param2, param3)

            val params = ArrayList<SmilMediaParam>()
            params.add(param1)
            params.add(param2)
            params.add(param3)

            val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
            EasyMock.expect(group1.params).andReturn(params).anyTimes()
            EasyMock.expect(group1.id).andReturn(trackParamGroupId).anyTimes()
            EasyMock.replay(group1)

            val paramGroups = ArrayList<SmilMediaParamGroup>()
            paramGroups.add(group1)

            val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
            EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
            EasyMock.replay(head)

            val object1 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
            EasyMock.expect(object1.isContainer).andReturn(false).anyTimes()
            EasyMock.expect(object1.paramGroup).andReturn(trackParamGroupId).anyTimes()
            EasyMock.expect(object1.clipBeginMS).andReturn(1000L).anyTimes()
            EasyMock.expect(object1.clipEndMS).andReturn(12000L).anyTimes()
            EasyMock.expect(object1.src).andReturn(mediaUrl.toURI()).anyTimes()
            EasyMock.replay(object1)

            val object2 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
            EasyMock.expect(object2.isContainer).andReturn(false).anyTimes()
            EasyMock.expect(object2.paramGroup).andReturn(trackParamGroupId).anyTimes()
            EasyMock.expect(object2.clipBeginMS).andReturn(1000L).anyTimes()
            EasyMock.expect(object2.clipEndMS).andReturn(13000L).anyTimes()
            EasyMock.expect(object2.src).andReturn(mediaUrl.toURI()).anyTimes()
            EasyMock.replay(object2)

            val objects = ArrayList<SmilMediaObject>()
            objects.add(object1)
            objects.add(object2)

            val objectContainer = EasyMock.createNiceMock<SmilMediaContainer>(SmilMediaContainer::class.java)
            EasyMock.expect(objectContainer.isContainer).andReturn(true).anyTimes()
            EasyMock.expect<ContainerType>(objectContainer.containerType).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes()
            EasyMock.expect(objectContainer.elements).andReturn(objects).anyTimes()
            EasyMock.replay(objectContainer)

            val containerObjects = ArrayList<SmilMediaObject>()
            containerObjects.add(objectContainer)

            val body = EasyMock.createNiceMock<SmilBody>(SmilBody::class.java)
            EasyMock.expect(body.mediaElements).andReturn(containerObjects).anyTimes()
            EasyMock.replay(body)

            smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
            EasyMock.expect<SmilObject>(smil!![trackParamGroupId]).andReturn(group1).anyTimes()
            EasyMock.expect(smil!!.body).andReturn(body).anyTimes()
            EasyMock.expect(smil!!.head).andReturn(head).anyTimes()
            EasyMock.expect(smil!!.toXML()).andReturn(smilString).anyTimes()
            EasyMock.expect(smil!!.id).andReturn("s-ec404c2a-5092-4cd4-8717-7b7bbc244656").anyTimes()
            EasyMock.replay(smil!!)

            val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
            EasyMock.expect(response.smil).andReturn(smil).anyTimes()
            EasyMock.replay(response)

            smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
            EasyMock.expect(smilService!!.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
            EasyMock.replay(smilService!!)

            /* End of Smil mockups */

        }
    }
}
