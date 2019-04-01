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

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.Incidents
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.smil.api.SmilException
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
import org.opencastproject.util.FileSupport
import org.opencastproject.util.MimeType
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.log4j.BasicConfigurator
import org.easymock.Capture
import org.easymock.EasyMock
import org.easymock.IAnswer
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
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

/**
 * Tests the [ComposerServiceImpl].
 */

class ProcessSmilTest {
    /** The sources file to test with  */
    private var videoOnly: File? = null
    private var audioOnly: File? = null
    private var sourceAudioVideo1: File? = null
    private var sourceAudioVideo2: File? = null
    private var job: Job? = null
    private var workingDirectory: File? = null

    /** The composer service to test  */
    private var composerService: ComposerServiceImpl? = null

    /** The service registry for job dispatching  */
    private var serviceRegistry: ServiceRegistry? = null
    private var inspectedTrack: Track? = null

    /** Encoding profile scanner  */
    private var profileScanner: EncodingProfileScanner? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Skip tests if FFmpeg is not installed

        Assume.assumeTrue(ffmpegInstalled)
        BasicConfigurator.configure()
        workingDirectory = FileSupport.getTempDirectory("processSmiltest")
        FileUtils.forceMkdir(workingDirectory!!)

        // Copy an existing media file to a temp file
        var f: File? = File("src/test/resources/av1.mp4")
        sourceAudioVideo1 = File(workingDirectory, "av1.mp4")
        FileUtils.copyFile(f!!, sourceAudioVideo1!!)
        f = null

        f = File("src/test/resources/audiovideo.mov")
        sourceAudioVideo2 = File(workingDirectory, "av2.mov")
        FileUtils.copyFile(f, sourceAudioVideo2!!)
        f = null

        f = File("src/test/resources/video.mp4")
        videoOnly = File(workingDirectory, "video.mp4")
        FileUtils.copyFile(f, videoOnly!!)
        f = null

        f = File("src/test/resources/audio.mp3")
        audioOnly = File(workingDirectory, "audio.mp3")
        FileUtils.copyFile(f, audioOnly!!)
        f = null

        // create the needed mocks
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bc.getProperty(EasyMock.anyObject<Any>() as String)).andReturn(FFMPEG_BINARY)

        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()

        job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job!!.id).andReturn(123456789.toLong()).anyTimes()
        EasyMock.replay(job!!)

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
        EasyMock.expect(workspace.get(EasyMock.anyObject<Any>() as URI)).andAnswer(org.easymock.IAnswer {
            val uri = EasyMock.getCurrentArguments()[0] as URI
            val name = uri.path
            logger.info("workspace Returns $name")
            if (name.contains("av2"))
                return@IAnswer sourceAudioVideo2
            if (name.contains("audio"))
                return@IAnswer audioOnly
            else if (name.contains("video"))
                return@IAnswer videoOnly
            sourceAudioVideo1 // default
        }).anyTimes()

        EasyMock.expect(
                workspace.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andAnswer(org.easymock.IAnswer {
            val f = File(workingDirectory, EasyMock.getCurrentArguments()[1] as String)
            val out = FileOutputStream(f)
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, out)
            f.toURI()
        }).anyTimes()

        profileScanner = EncodingProfileScanner()
        val encodingProfile = File("src/test/resources/encodingprofiles.properties")
        Assert.assertNotNull("Encoding profile must exist", encodingProfile)
        profileScanner!!.install(encodingProfile)

        val inspectionService = object : MediaInspectionService {
            @Throws(MediaInspectionException::class)
            override fun inspect(workspaceURI: URI): Job {
                val inspectionJob = EasyMock.createNiceMock<Job>(Job::class.java)
                EasyMock.expect(inspectionJob.status).andReturn(Status.FINISHED).anyTimes()
                try {
                    EasyMock.expect(inspectionJob.payload).andReturn(MediaPackageElementParser.getAsXml(inspectedTrack))
                } catch (e: MediaPackageException) {
                    throw RuntimeException(e)
                }

                EasyMock.replay(inspectionJob)
                return inspectionJob
            }

            @Throws(MediaInspectionException::class, MediaPackageException::class)
            override fun enrich(original: MediaPackageElement, override: Boolean): Job? {
                return null
            }

            @Throws(MediaInspectionException::class)
            override fun inspect(uri: URI, options: Map<String, String>): Job? {
                return null
            }


            @Throws(MediaInspectionException::class, MediaPackageException::class)
            override fun enrich(original: MediaPackageElement, override: Boolean, options: Map<String, String>): Job? {
                return null
            }
        }


        val sourceTrackXml = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
                + "       <mimetype>video/mpeg</mimetype>" + "       <url>video.mp4</url>" + "       </track>")
        inspectedTrack = MediaPackageElementParser.getFromXml(sourceTrackXml) as Track

        // Create and populate the composer service
        composerService = object : ComposerServiceImpl() {
            @Throws(EncoderException::class)
            override fun inspect(job: Job, uris: List<URI>): List<Track> {
                val results = ArrayList<Track>(uris.size)
                uris.forEach { uri -> results.add(inspectedTrack) }
                return results
            }
        }

        val incidentService = EasyMock.createNiceMock<IncidentService>(IncidentService::class.java)

        serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)     // To quiet the warnings
        val type = EasyMock.newCapture<String>()
        val operation = EasyMock.newCapture<String>()
        val args = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry!!.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
                .andAnswer {
                    val job = JobImpl(0)
                    job.jobType = type.value
                    job.operation = operation.value
                    job.arguments = args.getValue()
                    job.payload = composerService!!.process(job)
                    job
                }.anyTimes()
        EasyMock.expect(serviceRegistry!!.incident()).andAnswer {
            val incidents = Incidents(serviceRegistry!!, incidentService)
            incidents
        }.anyTimes()
        // Finish setting up the mocks
        EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace, incidentService, serviceRegistry)
        composerService!!.serviceRegistry = serviceRegistry
        composerService!!.organizationDirectoryService = orgDirectory
        composerService!!.securityService = securityService
        composerService!!.serviceRegistry = serviceRegistry
        composerService!!.userDirectoryService = userDirectory
        composerService!!.setProfileScanner(profileScanner)
        composerService!!.setWorkspace(workspace)
        composerService!!.setMediaInspectionService(inspectionService)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(sourceAudioVideo1)
        FileUtils.deleteQuietly(sourceAudioVideo2)
        FileUtils.deleteQuietly(audioOnly)
        FileUtils.deleteQuietly(videoOnly)
        FileUtils.forceDelete(workingDirectory!!)
    }

    // Convenience function to mock a param
    internal fun mockSmilMediaParam(name: String, value: String, id: String): SmilMediaParam {
        val param = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
        EasyMock.expect(param.name).andReturn(name).anyTimes()
        EasyMock.expect(param.value).andReturn(value).anyTimes()
        EasyMock.expect(param.id).andReturn(id).anyTimes()
        EasyMock.replay(param)
        return param
    }

    @Throws(SmilException::class)
    internal fun mockSmilMediaElement(src: URI, clipBeginMS: Long?, clipEndMS: Long?, paramGroupId: String): SmilMediaElement {
        val elem = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
        EasyMock.expect(elem.paramGroup).andReturn(paramGroupId).anyTimes()
        EasyMock.expect(elem.src).andReturn(src).anyTimes()
        EasyMock.expect(elem.isContainer).andReturn(false).anyTimes()
        EasyMock.expect(elem.clipBeginMS).andReturn(clipBeginMS).anyTimes()
        EasyMock.expect(elem.clipEndMS).andReturn(clipEndMS).anyTimes()
        EasyMock.replay(elem)
        return elem
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilOneSegment() {
        logger.info("testProcessSmilOneSegment")
        assertTrue(sourceAudioVideo1!!.isFile)
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='video.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='video.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>")
        // build a single media package to test with
        try {
            val paramGroupId = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"

            val params1 = ArrayList<SmilMediaParam>()
            params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
            params1.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                    "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
            params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))

            val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
            EasyMock.expect(group1.params).andReturn(params1).anyTimes()
            EasyMock.expect(group1.id).andReturn(paramGroupId).anyTimes()
            EasyMock.replay(group1)

            val paramGroups = ArrayList<SmilMediaParamGroup>()
            paramGroups.add(group1)

            val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
            EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
            EasyMock.replay(head)

            val sme1 = mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 1000L, 5000L, paramGroupId)
            val sme2 = mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 1000L, 5000L,
                    "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a")

            val objects = ArrayList<SmilMediaObject>()
            objects.add(sme1)
            objects.add(sme2)

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

            val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
            EasyMock.expect<SmilObject>(smil.get(paramGroupId)).andReturn(group1).anyTimes()
            EasyMock.expect(smil.body).andReturn(body).anyTimes()
            EasyMock.expect(smil.head).andReturn(head).anyTimes()
            EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
            EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
            EasyMock.replay(smil)

            val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
            EasyMock.expect(response.smil).andReturn(smil).anyTimes()
            EasyMock.replay(response)

            val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
            EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
            EasyMock.replay(smilService)
            composerService!!.setSmilService(smilService)

            val encodingProfiles = Arrays.asList("h264-low.http")
            val job = composerService!!.processSmil(smil, paramGroupId, "0", encodingProfiles)
            val outputs = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
            assertNotNull(outputs)
            for (track in outputs) {
                logger.info("testProcessOneTrack got file:", track.description)
            }
            assertTrue(outputs.size == 1) // One for each profile
        } catch (e: EncoderException) {
            // assertTrue("test complete".equals(e.getMessage()));
        }

    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilVideoOnly() {
        assertTrue(videoOnly!!.isFile)
        logger.info("testProcessSmilVideoOnly")
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='videoonly.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='audio.mp3' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='videoonly.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='audio.mp3' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>")
        // build a single media package to test with
        val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"
        val paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a"
        val params = ArrayList<SmilMediaParam>()
        params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params.add(mockSmilMediaParam("track-src", "file:" + videoOnly!!.path,
                "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val sme1 = mockSmilMediaElement(videoOnly!!.toURI(), 1000L, 5000L, paramGroupId1) // Only doing group1
        val sme2 = mockSmilMediaElement(audioOnly!!.toURI(), 1000L, 5000L, paramGroupId2)

        val objects = ArrayList<SmilMediaObject>()
        objects.add(sme1)
        objects.add(sme2)

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)
        val encodingProfiles = Arrays.asList("h264-low.http")
        val job = composerService!!.processSmil(smil, paramGroupId1, ComposerService.VIDEO_ONLY, encodingProfiles)
        val outputs = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
        // Video Only - video.mp4 has no audio track
        assertNotNull(outputs)
        logger.info("testProcessSmilOneTrack got {} files", outputs)
        for (track in outputs) {
            logger.info("testProcessOneTrack got file:", track.description)
        }
        assertTrue(outputs.size == 1) // One for each profile
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilAudioOnly() {
        assertTrue(audioOnly!!.isFile)
        logger.info("testProcessSmilAudioOnly")
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='videoonly.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='audio.mp3' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='videoonly.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='audio.mp3' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>")
        // build a single media package to test with
        try {
            val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814" // Pick the presenter flavor
            val paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a"
            val params1 = ArrayList<SmilMediaParam>()
            val params2 = ArrayList<SmilMediaParam>()
            params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
            params1.add(mockSmilMediaParam("track-src", "file:" + audioOnly!!.path,
                    "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
            params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))
            params2.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"))
            params2.add(mockSmilMediaParam("track-src", "file:" + audioOnly!!.path,
                    "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"))
            params2.add(
                    mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"))


            val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
            EasyMock.expect(group1.params).andReturn(params1).anyTimes()
            EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
            EasyMock.replay(group1)

            val paramGroups = ArrayList<SmilMediaParamGroup>()
            paramGroups.add(group1)

            val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
            EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
            EasyMock.replay(head)

            val sme1 = mockSmilMediaElement(audioOnly!!.toURI(), 1000L, 5000L, paramGroupId1)
            val sme2 = mockSmilMediaElement(audioOnly!!.toURI(), 1000L, 5000L, paramGroupId2)

            val objects = ArrayList<SmilMediaObject>()
            objects.add(sme1)
            objects.add(sme2)

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

            val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
            EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
            EasyMock.expect(smil.body).andReturn(body).anyTimes()
            EasyMock.expect(smil.head).andReturn(head).anyTimes()
            EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
            EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
            EasyMock.replay(smil)

            val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
            EasyMock.expect(response.smil).andReturn(smil).anyTimes()
            EasyMock.replay(response)

            val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
            EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
            EasyMock.replay(smilService)
            composerService!!.setSmilService(smilService)
            val encodingProfiles = Arrays.asList("mp3audio.http")
            // Let processSmil know that there is no video
            val job = composerService!!.processSmil(smil, paramGroupId1, ComposerService.AUDIO_ONLY, encodingProfiles)
            val outputs = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
            // Audio Only - video.mp4 has no audio track
            assertNotNull(outputs)
            logger.info("testProcessSmilOneTrack got {} files", outputs)
            for (track in outputs) {
                logger.info("testProcessOneTrack got file:", track.description)
            }
            assertTrue(outputs.size == 1) // One for each profile
        } catch (e: EncoderException) {
            // assertTrue("test complete".equals(e.getMessage()));
        }

    }

    @Test(expected = EncoderException::class)
    @Throws(Exception::class)
    fun testProcessSmilBadProfile() {
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='audiovideo.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='video.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='video.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='video.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='5000ms' clipBegin='1000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/></par></body></smil>")

        val encodingProfiles = Arrays.asList("player-preview.http", "av.work") // Should throw exception
        // Encoding profile must support visual or audiovisual
        val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814"
        val paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a"
        val params = ArrayList<SmilMediaParam>()
        params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        objects.add(mockSmilMediaElement(videoOnly!!.toURI(), 1000L, 5000L, paramGroupId1))
        objects.add(mockSmilMediaElement(videoOnly!!.toURI(), 1000L, 5000L, paramGroupId2))

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)
        composerService!!.processSmil(smil, paramGroupId1, "", encodingProfiles)
    }


    @Test
    @Throws(Exception::class)
    fun testProcessSmilMultiSegment() {
        assertTrue(sourceAudioVideo1!!.isFile)
        assertTrue(sourceAudioVideo2!!.isFile)
        logger.info("testProcessSmilMultiSegment")
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "</paramGroup><paramGroup xml:id='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a'>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='1000ms' clipBegin='0ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='100"
                + "0ms' clipBegin='0ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>"
                + "</par>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e77'>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='3500ms' clipBegin='2000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='350"
                + "0ms' clipBegin='2000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>"
                + "</par>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e79'>"
                + "<video src='audiovideo2.mov' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a1'/>"
                + "<video src='audiovideo2.mov' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d42'/>"
                + "</par></body></smil>")
        val encodingProfiles = Arrays.asList("mp3audio.http", "h264-low.http", "h264-medium.http")
        // Encoding profile must support visual or audiovisual
        val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814" // Pick the presenter flavor
        val paramGroupId2 = "pg-54d11c80-f8d1-4911-8e91-fffeb02e727a"

        val params1 = ArrayList<SmilMediaParam>()
        val params2 = ArrayList<SmilMediaParam>()
        params1.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params1.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))
        params2.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"))
        params2.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo2!!.path,
                "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"))
        params2.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params1).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)
        val group2 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group2.params).andReturn(params2).anyTimes()
        EasyMock.expect(group2.id).andReturn(paramGroupId2).anyTimes()
        EasyMock.replay(group2)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)
        paramGroups.add(group2)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        // Second track is listed in different paramGroup
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 0L, 1000L, paramGroupId1))
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 2000L, 3500L, paramGroupId1))
        objects.add(mockSmilMediaElement(sourceAudioVideo2!!.toURI(), 1000L, 3500L, paramGroupId1))

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
        EasyMock.expect<SmilObject>(smil.get(paramGroupId2)).andReturn(group2).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)

        val job = composerService!!.processSmil(smil, paramGroupId1, null, encodingProfiles)
        Assert.assertNotNull(job.payload)
        assertEquals(3, MediaPackageElementParser.getArrayFromXml(job.payload).size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilMultiTrack() {
        assertTrue(sourceAudioVideo1!!.isFile)
        assertTrue(sourceAudioVideo2!!.isFile)
        val sourceTrack1Xml = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presenter/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
                + "<duration>7000</duration>" + "<mimetype>video/mpeg</mimetype>"
                + "       <url>audiovideo1.mp4</url>"
                + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
                + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
                + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>")
        val track1 = MediaPackageElementParser.getFromXml(sourceTrack1Xml) as Track
        val sourceTrack2Xml = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<track xmlns=\"http://mediapackage.opencastproject.org\" type=\"presenter/source\" id=\"f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a\">\n"
                + "<duration>7000</duration>" + "  <mimetype>video/mpeg</mimetype>" + "<url>audiovideo2.mov</url>"
                + "<video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
                + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution>"
                + "<scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track>")
        val track2 = MediaPackageElementParser.getFromXml(sourceTrack2Xml) as Track
        val smil1 = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<smil baseProfile='Language' version='3.0' xml:id='s-38f4fa91-c381-4c0e-a51a-e15373428f2d' xmlns='http://www.w3.org/ns/SMIL'>"
                + "<head xml:id='h-1c58939f-b323-4a1b-af48-1f1c0703f4b0'><meta name='track-duration' content='60000ms' xml:id='meta-4e8ba66c-8c90-47d0-bc8a-174eba1fc91f'/>"
                + "<paramGroup xml:id='pg-54da9288-36c0-4e9c-87a1-adb30562b814'>"
                + "<param value='7a9aed12-74a6-4bdb-8fba-f3d8bf6fad22' name='track-id' valuetype='data' xml:id='param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9'/>"
                + "<param value='audiovideo1.mp4' name='track-src' valuetype='data' xml:id='param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318'/>"
                + "<param value='presenter/work' name='track-flavor' valuetype='data' xml:id='param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9'/>"
                + "<param value='5354d0e0-87c0-4f5a-a9d9-e9dbb86a9dc6' name='track-id' valuetype='data' xml:id='param-1097ff2d-431f-497c-b186-5d8f2ca6c88f'/>"
                + "<param value='audiovideo2.mp4' name='track-src' valuetype='data' xml:id='param-c57e9beb-a67a-4a96-96a6-9ede29e653ec'/>"
                + "<param value='presentation/work' name='track-flavor' valuetype='data' xml:id='param-476ade36-9193-40bb-aae3-0c1028471797'/>"
                + "</paramGroup></head><body xml:id='b-994529e5-e981-40ed-83cb-b70b7f94ae5f'>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e78'>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='3000ms' clipBegin='0ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a0'/>"
                + "<video src='audiovideo1.mp4' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='300"
                + "0ms' clipBegin='0ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d44'/>" + "</par>"
                + "<par xml:id='par-3a0e134e-eabe-46d5-8c8d-066127585e79'>"
                + "<video src='audiovideo2.mov' paramGroup='pg-54da9288-36c0-4e9c-87a1-adb30562b814' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-beb9f77e-1ee2-4ad4-a2b2-acd6ab9550a1'/>"
                + "<video src='audiovideo2.mov' paramGroup='pg-54d11c80-f8d1-4911-8e91-fffeb02e727a' clipEnd='7500ms' clipBegin='5000ms' xml:id='v-aa936d8a-11fe-4f70-85eb-e02e95d63d42'/>"
                + "</par></body></smil>")
        val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814" // Pick the presenter flavor
        // 2 tracks in the same group
        val params = ArrayList<SmilMediaParam>()
        params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))
        params.add(mockSmilMediaParam("track-id", "track-2", "param-1097ff2d-431f-497c-b186-5d8f2ca6c88f"))
        params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo2!!.path,
                "param-c57e9beb-a67a-4a96-96a6-9ede29e653ec"))
        params.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-476ade36-9193-40bb-aae3-0c1028471797"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        // Second track is listed in same paramGroup
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 0L, 3000L, paramGroupId1))
        objects.add(mockSmilMediaElement(sourceAudioVideo2!!.toURI(), 5000L, 7500L, paramGroupId1))

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(smil1).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)
        // Code equivalence to the mock
        // SmilServiceImpl smilService = new SmilServiceImpl();
        // SmilResponse smilResponse = smilService.createNewSmil();
        // smilResponse = smilService.addParallel(smilResponse.getSmil());
        // SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
        // smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track1, 0L, 3000L);
        // List<SmilMediaParamGroup> groups = smilResponse.getSmil().getHead().getParamGroups();
        // String paramGroupId = groups.get(0).getId();
        // smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), track2, 6000L, 8500L, paramGroupId);
        // Smil smil = smilResponse.getSmil();
        val encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http", "h264-large.http")

        val job = composerService!!.processSmil(smil, paramGroupId1, "", encodingProfiles)
        val outputs = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
        assertNotNull(outputs)
        logger.info("testProcessSmilDirect got {} files", outputs)
        assertTrue(outputs.size == 3) // One for each profile
    }

    @Test
    @Throws(Exception::class)
    fun testProcessProdSysTracks() {
        assertTrue(sourceAudioVideo1!!.isFile)
        assertTrue(sourceAudioVideo2!!.isFile)
        val prodsmil = ("<?xml version='1.0' encoding='UTF-8'?>"
                + "<smil xmlns='http://www.w3.org/ns/SMIL' baseProfile='Language' version='3.0' xml:id='s-aedc52a7-3207-49cf-8a9b-221ee8baba66'>"
                + "<head xml:id='h-d9ba75ce-2b50-458d-8919-7a92933a75a0'>"
                + "<meta content='17d14143-5bbf-4c60-b082-fbc401350691' name='media-package-id' xml:id='meta-a9d5fb98-af82-4b9e-abc2-abe5a9dd843c'/>"
                + "<meta content='300000ms' name='track-duration' xml:id='meta-1950b9b6-cd8f-43ac-9744-f6d8173ca171'/>"
                + "<paramGroup xml:id='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1'>"
                + "<param name='track-id' value='220f90fb-c764-40ac-b308-cd6731d22d2e' valuetype='data' xml:id='param-31a5a322-18ae-4659-b696-b3772d99651d'/>"
                + "<param name='track-src' value='audiovideo1.mp4' valuetype='data' xml:id='param-aed3f3db-7d68-4a76-9229-557940fb44be'/>"
                + "<param name='track-flavor' value='presenter/source' valuetype='data' xml:id='param-1c069d4a-23cd-45e3-b951-53de908b2b69'/>"
                + "</paramGroup></head>"
                + "<body xml:id='b-de826a33-7858-4172-a4a2-9cf1e9a53183'>"
                + "<par xml:id='par-c82881e2-f372-4a98-8cd4-97bc145cbfbe'>"
                + "<video paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo1.mp4' clipBegin='0ms' clipEnd='10000ms' xml:id='v-2fc1b286-d87b-4393-a922-161af39a9f93'/>"
                + "</par>"
                + "<par xml:id='par-eb80f89d-0fce-466d-b0c5-48a30006b691'>"
                + "<video clipBegin='18000ms' clipEnd='30000ms' paramGroup='pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1' src='sourceAudioVideo2.mp4' xml:id='v-d4d19997-f06b-4bdd-8e8c-d23907a971a8'/>"
                + "</par></body>" + "</smil>")
        // SmilResponse smilResponse = smilService.fromXml(prodsmil);
        val paramGroupId = "pg-7b7d7eb9-8006-41a4-82b3-3fbc31b08ff1" // Pick the presenter flavor

        val params = ArrayList<SmilMediaParam>()
        params.add(mockSmilMediaParam("track-id", "track-1", "param-31a5a322-18ae-4659-b696-b3772d99651d"))
        params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                "param-aed3f3db-7d68-4a76-9229-557940fb44be"))
        params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-1c069d4a-23cd-45e3-b951-53de908b2b69"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        // Second track is not listed in paramGroup
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 0L, 3000L, paramGroupId))
        objects.add(mockSmilMediaElement(sourceAudioVideo2!!.toURI(), 5000L, 7500L, paramGroupId))

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId)).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(prodsmil).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)
        val encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http", "h264-large.http")
        val job = composerService!!.processSmil(smil, paramGroupId, "x", encodingProfiles)
        val outputs = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
        assertNotNull(outputs)
        logger.info("testProcessSmilDirect got {} files", outputs)
        assertTrue(outputs.size == 3) // One for each profile
    }

    /**
     * Test method for
     * [org.opencastproject.composer.impl.ComposerServiceImpl.processSmil]
     */
    @Test
    @Throws(Exception::class)
    fun testProcessSmilJob() {
        val paramGroupId1 = "pg-54da9288-36c0-4e9c-87a1-adb30562b814" // Pick the presenter flavor
        val params = ArrayList<SmilMediaParam>()
        params.add(mockSmilMediaParam("track-id", "track-1", "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params.add(mockSmilMediaParam("track-src", "file:" + sourceAudioVideo1!!.path,
                "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        // Second track is listed in same paramGroup
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 0L, 3000L, paramGroupId1))
        objects.add(mockSmilMediaElement(sourceAudioVideo1!!.toURI(), 5000L, 8300L, paramGroupId1))

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

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil.get(paramGroupId1)).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn("").anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        composerService!!.setSmilService(smilService)

        // Encoding profile must support stream, visual or audiovisual - also does not like yadif for some edits
        val encodingProfiles = Arrays.asList("h264-low.http", "h264-medium.http")

        val job = composerService!!.processSmil(smil, paramGroupId1, "x", encodingProfiles)
        val processedTracks = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
        logger.debug("testProcessSmil got " + processedTracks.size + " tracks")
        Assert.assertNotNull(processedTracks)
        Assert.assertEquals(2, processedTracks.size.toLong())
        for (processedTrack in processedTracks) {
            Assert.assertNotNull(processedTrack.identifier)
            Assert.assertEquals(processedTrack.mimeType, MimeType.mimeType("video", "mpeg"))
        }

    }

    companion object {

        /** FFmpeg binary location  */
        private val FFMPEG_BINARY = "ffmpeg"

        /** True to run the tests  */
        private var ffmpegInstalled = true

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(ProcessSmilTest::class.java)

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
    }
}
