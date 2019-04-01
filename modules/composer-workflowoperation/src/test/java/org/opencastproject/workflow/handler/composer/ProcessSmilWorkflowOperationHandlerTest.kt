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
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
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
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.easymock.IAnswer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.HashSet

import javax.xml.bind.JAXBException

class ProcessSmilWorkflowOperationHandlerTest {
    private var operationHandler: ProcessSmilWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null
    private var mp2: MediaPackage? = null
    private var job: Job? = null
    private var encodedTracks: Array<Track>? = null
    private var encodedTracks2: Array<Track>? = null
    private var profileList: Array<EncodingProfile>? = null
    // mock services and objects
    private var profile: EncodingProfile? = null
    private var profile2: EncodingProfile? = null
    private var profile3: EncodingProfile? = null
    private var composerService: ComposerService? = null
    private val smilService: SmilService? = null
    private var workspace: Workspace? = null
    private var workingDirectory: File? = null
    private var smilfile: File? = null

    // <operation
    // id="processsmil"
    // if="${trimHold}"
    // fail-on-error="true"
    // exception-handler-workflow="error"
    // description="takes a smil edit and transcode to all final formats concurrently">
    // <configurations>
    // <configuration key="source-flavors">presenter/*;presentation/*</configuration>
    // <configuration key="smil-flavor">smil/smil</configuration>
    // <configuration key="target-flavors">presenter/delivery;presentation/delivery</configuration>
    // <configuration key="target-tags">engage;engage</configuration>
    // <configuration key="encoding-profile">flash-vga.http;h264-low.http</configuration>
    // <configuration key="tag-with-profile">true</configuration>
    // </configurations>
    // </operation>
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

    @Throws(SmilException::class, MalformedURLException::class, JAXBException::class, SAXException::class)
    internal fun mockSmilService(): SmilService {
        val paramGroupId1 = "pg-e823179d-f606-4975-8972-6d06feb92f04"
        val paramGroupId2 = "pg-acc84414-82b8-427f-8453-8b655101df22"
        val video1 = File("fooVideo1.flv")
        val video2 = File("fooVideo2.flv")
        smilfile = File("src/test/resources/smil.smil")

        val params1 = ArrayList<SmilMediaParam>()
        params1.add(mockSmilMediaParam("track-id", "b7c4f480-dd22-4f82-bb58-c4f218051059",
                "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params1.add(mockSmilMediaParam("track-src", "fooVideo1.flv", "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params1.add(mockSmilMediaParam("track-flavor", "presenter/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))
        val params2 = ArrayList<SmilMediaParam>()
        params2.add(mockSmilMediaParam("track-id", "144b489b-c498-4d11-9a63-1ab96a7ec0b1",
                "param-9b377a8f-ceec-412a-a5ea-7ff1d6bd07c9"))
        params2.add(mockSmilMediaParam("track-src", "fooVideo2.flv", "param-b0e82ab6-cec4-40cd-b1dd-8a76507ff318"))
        params2.add(mockSmilMediaParam("track-flavor", "presentation/work", "param-7f3cc9eb-cb2a-4611-8099-0ddd25b0d6b9"))

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params1).anyTimes()
        EasyMock.expect(group1.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group1)
        val group2 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group2.params).andReturn(params2).anyTimes()
        EasyMock.expect(group2.id).andReturn(paramGroupId1).anyTimes()
        EasyMock.replay(group2)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)
        paramGroups.add(group2)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val objects = ArrayList<SmilMediaObject>()
        objects.add(mockSmilMediaElement(video1.toURI(), 0L, 3000L, paramGroupId1))
        objects.add(mockSmilMediaElement(video2.toURI(), 0L, 3000L, paramGroupId2))
        objects.add(mockSmilMediaElement(video1.toURI(), 4000L, 7000L, paramGroupId1))
        objects.add(mockSmilMediaElement(video2.toURI(), 4000L, 7000L, paramGroupId2))

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
        EasyMock.expect(smil.toXML()).andReturn("").anyTimes()
        EasyMock.expect(smil.id).andReturn("s-38f4fa91-c381-4c0e-a51a-e15373428f2d").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as String)).andReturn(response).anyTimes()
        EasyMock.replay(smilService)
        return smilService
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        workingDirectory = FileSupport.getTempDirectory("processSmiltest")
        FileUtils.forceMkdir(workingDirectory!!)
        smilfile = File("src/test/resources/smil.smil")

        // set up mock smil service
        val smilService = mockSmilService()
        // smilService.fromXml(FileUtils.readFileToString(smilFile, "UTF-8"))

        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE_ID).anyTimes()
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Stream).anyTimes()
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.AudioVisual).anyTimes()
        EasyMock.expect(profile!!.suffix).andReturn("-v.flv").anyTimes()

        profile2 = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java) // Video Only
        EasyMock.expect(profile2!!.identifier).andReturn(PROFILE_ID2).anyTimes()
        EasyMock.expect(profile2!!.applicableMediaType).andReturn(MediaType.Visual).anyTimes()
        EasyMock.expect(profile2!!.outputType).andReturn(MediaType.Visual).anyTimes()
        EasyMock.expect(profile2!!.suffix).andReturn("-v.mp4").anyTimes()

        profile3 = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java) // different suffix
        EasyMock.expect(profile3!!.identifier).andReturn(PROFILE_ID3).anyTimes()
        EasyMock.expect(profile3!!.applicableMediaType).andReturn(MediaType.Audio).anyTimes()
        EasyMock.expect(profile3!!.outputType).andReturn(MediaType.Audio).anyTimes()
        EasyMock.expect(profile3!!.suffix).andReturn("-a.mp4").anyTimes()
        profileList = arrayOf<EncodingProfile>(profile, profile2, profile3)
        EasyMock.replay(profile, profile2, profile3)

        // AV both tracks
        val uriMP = InspectWorkflowOperationHandler::class.java.getResource("/process_smil_mediapackage.xml").toURI()
        // AV presenter, V only presentation
        val uriMP2 = InspectWorkflowOperationHandler::class.java.getResource("/process_smil_mediapackage2.xml").toURI()
        // AV single AV return
        val uriMPEncode = InspectWorkflowOperationHandler::class.java.getResource("/process_smil_result_mediapackage.xml")
                .toURI()
        // AV 2 AV tracks return
        val uriMPEncode2 = InspectWorkflowOperationHandler::class.java.getResource("/process_smil_result2_mediapackage.xml")
                .toURI()

        mp = builder.loadFromXml(uriMP.toURL().openStream())
        mp2 = builder.loadFromXml(uriMP2.toURL().openStream())
        val mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream())
        val mpEncode2 = builder.loadFromXml(uriMPEncode2.toURL().openStream())
        encodedTracks = mpEncode.tracks
        encodedTracks2 = mpEncode2.tracks

        job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job!!.payload).andReturn(MediaPackageElementParser.getArrayAsXml(Arrays.asList(*encodedTracks!!)))
                .anyTimes()
        EasyMock.expect<Status>(job!!.status).andReturn(Job.Status.FINISHED).anyTimes()
        EasyMock.expect(job!!.dateCreated).andReturn(Date()).anyTimes()
        EasyMock.expect(job!!.dateStarted).andReturn(Date()).anyTimes()
        EasyMock.expect<Long>(job!!.queueTime).andReturn(10).anyTimes()
        EasyMock.replay(job!!)
        val job2 = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job2.payload).andReturn(MediaPackageElementParser.getArrayAsXml(Arrays.asList(*encodedTracks2!!)))
                .anyTimes()
        EasyMock.expect<Status>(job2.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(job2.dateCreated).andReturn(Date())
        EasyMock.expect(job2.dateStarted).andReturn(Date())
        EasyMock.expect<Long>(job2.queueTime).andReturn(13)
        EasyMock.replay(job2)
        // set up mock service registry
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job2)
        EasyMock.replay(serviceRegistry)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID)).andReturn(profile).anyTimes()
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID2)).andReturn(profile2).anyTimes()
        EasyMock.expect(composerService!!.getProfile(PROFILE_ID3)).andReturn(profile3).anyTimes()
        EasyMock.expect(composerService!!.processSmil(EasyMock.anyObject<Any>() as Smil, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String, EasyMock.anyObject())).andReturn(job)
        EasyMock.expect(composerService!!.processSmil(EasyMock.anyObject<Any>() as Smil, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject(), EasyMock.anyObject())).andReturn(job2)
        EasyMock.replay(composerService!!)

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String)).andAnswer(IAnswer {
            val name: String
            try { // media file should be returned "as is"
                // URI uri = (URI) EasyMock.getCurrentArguments()[0];
                name = EasyMock.getCurrentArguments()[3] as String
                val ext = FilenameUtils.getExtension(name)
                if (ext.matches("[fm][pol][v43]".toRegex())) {
                    return@IAnswer URI(name)
                }
            } catch (e: Exception) {
            }

            uriMP // default
        }).anyTimes()

        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andAnswer(IAnswer<File> {
            var name: String
            try {
                val uri = EasyMock.getCurrentArguments()[0] as URI
                name = uri.path
                if (name.contains("smil.smil"))
                    return@IAnswer smilfile
            } catch (e: Exception) {
                name = uriMP.path
            }

            File(name) // default
        }).anyTimes()

        EasyMock.expect(workspace!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andAnswer {
            val f = File(workingDirectory, EasyMock.getCurrentArguments()[1] as String)
            val out = FileOutputStream(f)
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, out)
            f.toURI()
        }.anyTimes()
        EasyMock.replay(workspace!!)

        operationHandler = ProcessSmilWorkflowOperationHandler()
        operationHandler!!.setSmilService(smilService)
        operationHandler!!.setJobBarrierPollingInterval(0)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
        operationHandler!!.setComposerService(composerService)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.forceDelete(workingDirectory!!)
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilOneTrackOneSection() {
        // operation configuration
        val targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/*"
        configurations["smil-flavor"] = "smil/smil"
        configurations["target-tags"] = targetTags
        configurations["target-flavors"] = "presenter/livery"
        configurations["encoding-profiles"] = PROFILE_ID

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID)
        Assert.assertEquals("presenter/livery", trackEncoded.flavor.toString())
        val mytags = targetTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.sort(mytags)
        Assert.assertArrayEquals(mytags, trackEncoded.tags)
        Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.reference.identifier) // reference the
        // correct flavor
        Assert.assertEquals(10, result.timeInQueue) // Tracking queue time
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilOneTrackOneSectionNoTagsNoTargetFlavor() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/*"
        configurations["smil-flavor"] = "smil/smil"
        configurations["target-flavors"] = "presenter/deli"
        configurations["encoding-profiles"] = PROFILE_ID

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID)
        Assert.assertEquals("presenter/deli", trackEncoded.flavor.toString())
        Assert.assertEquals(0, trackEncoded.tags.size.toLong())
        Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.reference.identifier) // reference the
        // correct flavor
        Assert.assertEquals(10, result.timeInQueue) // Tracking queue time
        val trackPresentationEncoded = mpNew.getTrack(ENCODED_PRESENTATION_TRACK_ID)
        Assert.assertNull(trackPresentationEncoded) // not encoded
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilTwoSectionsNoTagsNoTargetFlavor() {
        // operation configuration
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/work;presentation/work"
        configurations["smil-flavor"] = "smil/smil"
        configurations["encoding-profiles"] = PROFILE_ID // 2 jobs for the 2 sources

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        val trackPresenterEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID)
        Assert.assertNull(trackPresenterEncoded.flavor)
        // Assert.assertEquals("presenter/delivery", trackEncoded.getFlavor().toString());
        Assert.assertEquals(0, trackPresenterEncoded.tags.size.toLong())

        val trackPresentationEncoded = mpNew.getTrack(ENCODED_PRESENTATION_TRACK_ID)
        Assert.assertEquals(0, trackPresentationEncoded.tags.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilTwoTrackAllSections() {
        // operation configuration
        val targetTags = "engage,$PROFILE_ID,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "*/work" // should do 2 flavors as 2 jobs
        configurations["smil-flavor"] = "smil/smil"
        configurations["target-tags"] = "engage;rss" // should collapse the tags
        configurations["target-flavors"] = "*/delivery"
        configurations["encoding-profiles"] = PROFILE_ID
        configurations["tag-with-profile"] = "true"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
        var mpelems = mpNew
                .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presentation/delivery"))
        logger.info("Encoded tracks are : " + Arrays.toString(mpelems))
        Assert.assertEquals(mpelems.size.toLong(), 2) // Hack because of the mock
        var trackEncoded = mpelems[0] as Track
        val mytags = trackEncoded.tags
        Arrays.sort(mytags)
        logger.info("Encoded tracks are tagged: {} should be {}", Arrays.toString(trackEncoded.tags), targetTags)
        Assert.assertArrayEquals(targetTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), mytags)

        Assert.assertEquals(SOURCE_PRESENTATION_TRACK_ID, trackEncoded.reference.identifier)

        mpelems = mpNew.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"))
        Assert.assertEquals(mpelems.size.toLong(), 1)
        trackEncoded = mpelems[0] as Track
        Assert.assertEquals("presenter/delivery", trackEncoded.flavor.toString())
        Assert.assertArrayEquals(targetTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), trackEncoded.tags)
        Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.reference.identifier)
        Assert.assertEquals(ENCODED_PRESENTER_TRACK_ID, trackEncoded.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilOneTrackVideoOnly() {
        // operation configuration
        var targetTags = "engage,rss"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/*"
        configurations["smil-flavor"] = "smil/smil"
        configurations["target-tags"] = targetTags
        configurations["target-flavors"] = "*/delivery"
        configurations["encoding-profiles"] = PROFILE_ID
        configurations["tag-with-profile"] = "true"

        // run the operation handler with video only mediapackage
        val result = getWorkflowOperationResult(mp2, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
        val mpelems = mpNew
                .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"))
        Assert.assertEquals(mpelems.size.toLong(), 1)
        val trackEncoded = mpelems[0] as Track
        targetTags += ",$PROFILE_ID"
        val mytags = targetTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.sort(mytags)
        Assert.assertArrayEquals(mytags, trackEncoded.tags)
        Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.reference.identifier)
    }

    @Test
    @Throws(Exception::class)
    fun testProcessSmilTwoTrackTwoSections() {
        // operation configuration
        var targetTags = "preview,rss"
        var targetTags2 = "archive,engage"
        val configurations = HashMap<String, String>()
        configurations["source-flavors"] = "presenter/*;presentation/*" // 2 sections
        configurations["smil-flavor"] = "smil/smil"
        configurations["target-tags"] = "$targetTags;$targetTags2" // different tags
        configurations["target-flavors"] = "*/delivery"
        configurations["encoding-profiles"] = "$PROFILE_ID;$PROFILE_ID2,$PROFILE_ID3" // different profiles
        configurations["tag-with-profile"] = "true"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        // check track metadata
        val mpNew = result.mediaPackage
        // Track trackEncoded = mpNew.getTrack(ENCODED_PRESENTER_TRACK_ID);
        var mpelems = mpNew
                .getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presentation/delivery"))
        Assert.assertEquals(mpelems.size.toLong(), 2) // Should have 2 tracks here
        logger.info("Encoded presentation tracks are : " + Arrays.toString(mpelems))
        var trackEncoded = mpelems[0] as Track // This is not ordered
        if (trackEncoded.getURI().toString().endsWith(profile3!!.suffix)) {
            targetTags2 += "," + profile3!!.identifier
        } else if (trackEncoded.getURI().toString().endsWith(profile2!!.suffix)) {
            targetTags2 += "," + profile2!!.identifier
        }
        val mytags = trackEncoded.tags
        Arrays.sort(mytags)
        logger.info("Encoded presentation tracks are tagged: " + Arrays.toString(mytags) + " == " + targetTags2)
        Assert.assertArrayEquals(targetTags2.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), mytags)
        Assert.assertEquals(SOURCE_PRESENTATION_TRACK_ID, trackEncoded.reference.identifier)
        Assert.assertEquals(ENCODED_PRESENTATION_TRACK_ID, trackEncoded.identifier)

        mpelems = mpNew.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/delivery"))
        Assert.assertEquals(mpelems.size.toLong(), 1)
        trackEncoded = mpelems[0] as Track
        logger.info("Encoded presenter tracks are : " + Arrays.toString(mpelems))
        Assert.assertEquals("presenter/delivery", trackEncoded.flavor.toString())
        targetTags += "," + profile!!.identifier
        logger.info(
                "Encoded presenter tracks are tagged: " + Arrays.toString(trackEncoded.tags) + " == " + targetTags)
        Assert.assertEquals(HashSet(Arrays.asList(*targetTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())),
                HashSet(Arrays.asList(*trackEncoded.tags)))
        // Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
        Assert.assertEquals(SOURCE_PRESENTER_TRACK_ID, trackEncoded.reference.identifier)
        Assert.assertEquals(ENCODED_PRESENTER_TRACK_ID, trackEncoded.identifier)
        Assert.assertEquals(23, result.timeInQueue) // Tracking queue time
    }

    @Test
    @Throws(Exception::class)
    fun testComposeMissingProfile() {
        // set up mock profile
        profile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(profile!!.identifier).andReturn(PROFILE_ID)
        EasyMock.expect(profile!!.applicableMediaType).andReturn(MediaType.Stream)
        EasyMock.expect(profile!!.outputType).andReturn(MediaType.Stream)
        profileList = arrayOf<EncodingProfile>(profile)
        EasyMock.replay(profile!!)

        // set up mock composer service
        composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composerService!!.listProfiles()).andReturn(profileList)
        EasyMock.expect(composerService!!.encode(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as String)).andReturn(job)
        EasyMock.expect(composerService!!.processSmil(EasyMock.anyObject<Any>() as Smil, EasyMock.anyObject<Any>() as String,
                EasyMock.anyString(), EasyMock.anyObject<Any>() as List<String>)).andReturn(job)
        EasyMock.replay(composerService!!)
        operationHandler!!.setComposerService(composerService)
        operationHandler!!.setSmilService(smilService)
        val configurations = HashMap<String, String>()
        try {
            // no encoding profile
            configurations["source-flavors"] = "presenter/work"
            configurations["smil-flavor"] = "smil/smil"
            configurations["target-flavors"] = "presenter/delivery"
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
        operation.template = "process-smil"
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
        private val logger = LoggerFactory.getLogger(ProcessSmilWorkflowOperationHandlerTest::class.java)

        // constant metadata values
        private val PROFILE_ID = "flash.http"
        private val PROFILE_ID2 = "x264.http"
        private val PROFILE_ID3 = "aac.http"
        private val SOURCE_PRESENTER_TRACK_ID = "compose-workflow-operation-test-source-presenter-track-id"
        private val ENCODED_PRESENTER_TRACK_ID = "compose-workflow-operation-test-trimmed-presenter-track-id"
        private val SOURCE_PRESENTATION_TRACK_ID = "compose-workflow-operation-test-source-presentation-track-id"
        private val ENCODED_PRESENTATION_TRACK_ID = "compose-workflow-operation-test-trimmed-presentation-track-id"
    }

}
