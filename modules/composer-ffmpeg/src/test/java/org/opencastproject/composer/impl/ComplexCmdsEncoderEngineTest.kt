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

import org.easymock.EasyMock.anyBoolean
import org.easymock.EasyMock.capture
import org.junit.Assert.assertTrue

import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
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
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
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
import java.net.URL
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

/**
 * Tests the [ComposerServiceImpl].
 */

class ComplexCmdsEncoderEngineTest {
    /** The sources file to test with  */
    private var sourceVideoOnly: File? = null
    private var sourceAudioOnly: File? = null
    private var sourceAudioVideo: File? = null
    private val sourceMuxed: File? = null
    private var job: Job? = null
    private var workspace: Workspace? = null

    /** The composer service to test  */
    private var composerService: ComposerServiceImpl? = null
    private var engine: EncoderEngine? = null
    /** The service registry for job dispatching  */
    private val serviceRegistry: ServiceRegistry? = null
    private var inspectedTrack: Track? = null

    /** Encoding profile scanner  */
    private var profileScanner: EncodingProfileScanner? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Skip tests if FFmpeg is not installed
        Assume.assumeTrue(ffmpegInstalled)
        BasicConfigurator.configure()
        engine = EncoderEngine(FFMPEG_BINARY)

        var f = getFile("/video.mp4")
        sourceVideoOnly = File.createTempFile(FilenameUtils.getBaseName(f.name), ".mp4", workingDirectory)
        FileUtils.copyFile(f, sourceVideoOnly!!)

        // Create another audio only file
        f = getFile("/audio.mp3")
        sourceAudioOnly = File.createTempFile(FilenameUtils.getBaseName(f.name), ".mp3", workingDirectory)
        FileUtils.copyFile(f, sourceAudioOnly!!)

        f = getFile("/audiovideo.mov")
        sourceAudioVideo = File.createTempFile(FilenameUtils.getBaseName(f.name), ".mov", workingDirectory)
        FileUtils.copyFile(f, sourceAudioVideo!!)

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

        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andAnswer(org.easymock.IAnswer {
            val uri = EasyMock.getCurrentArguments()[0] as URI
            val name = uri.path
            logger.info("workspace Returns $name")
            if (name.contains("mux"))
                return@IAnswer sourceMuxed
            else if (name.contains("audiovideo"))
                return@IAnswer sourceAudioVideo
            else if (name.contains("audio"))
                return@IAnswer sourceAudioOnly
            else if (name.contains("video"))
                return@IAnswer sourceVideoOnly
            sourceAudioVideo // default
        }).anyTimes()

        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI, anyBoolean())).andAnswer(org.easymock.IAnswer {
            val uri = EasyMock.getCurrentArguments()[0] as URI
            val name = uri.path
            logger.info("workspace Returns $name")
            if (name.contains("mux"))
                return@IAnswer sourceMuxed
            else if (name.contains("audiovideo"))
                return@IAnswer sourceAudioVideo
            else if (name.contains("audio"))
                return@IAnswer sourceAudioOnly
            else if (name.contains("video"))
                return@IAnswer sourceVideoOnly
            sourceAudioVideo // default
        }).anyTimes()

        EasyMock.expect(
                workspace!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andAnswer(org.easymock.IAnswer {
            val f = File(workingDirectory, EasyMock.getCurrentArguments()[1] as String)
            val out = FileOutputStream(f)
            val `in` = EasyMock.getCurrentArguments()[2] as InputStream
            IOUtils.copy(`in`, out)
            f.toURI()
        }).anyTimes()

        profileScanner = EncodingProfileScanner()
        val encodingProfile = getFile("/encodingprofiles.properties")
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

        // Finish setting up the mocks
        EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace)

        val sourceTrackXml = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + " <track xmlns=\"http://mediapackage.opencastproject.org\" type='presentation/source'"
                + " id='f1fc0fc4-a926-4ba9-96d9-2fafbcc30d2a'>" + " <mimetype>video/avi</mimetype>"
                + " <url>muxed.avi</url>" + "       </track>")
        inspectedTrack = MediaPackageElementParser.getFromXml(sourceTrackXml) as Track

        val serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        val type = EasyMock.newCapture<String>()
        val operation = EasyMock.newCapture<String>()
        val args = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
                .andAnswer {
                    val job = JobImpl(0)
                    job.jobType = type.value
                    job.operation = operation.value
                    job.arguments = args.getValue()
                    job.payload = composerService!!.process(job)
                    job
                }.anyTimes()
        EasyMock.replay(serviceRegistry)

        // Create and populate the composer service
        composerService = object : ComposerServiceImpl() {
            @Throws(EncoderException::class)
            override fun inspect(job: Job, uris: List<URI>): List<Track> {
                val tracks = ArrayList<Track>(uris.size)
                uris.forEach { uri -> tracks.add(inspectedTrack) }
                return tracks
            }
        }

        val incidents = EasyMock.createNiceMock<IncidentService>(IncidentService::class.java)
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
        return
    }


    @Test
    @Throws(Exception::class)
    fun testConcatEdit() {
        val sourceUrl = javaClass.getResource("/audiovideo.mov")
        val sourceFile1 = File(workingDirectory, "audiovideo.mov")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        val eprofiles = arrayOf(profileScanner!!.getProfile("h264-low.http"), profileScanner!!.getProfile("h264-medium.http"), profileScanner!!.getProfile("h264-large.http"), profileScanner!!.getProfile("flash.rtmp"))

        val files = arrayOf(sourceFile1, sourceFile1)

        val params = HashMap<String, String>()
        val outDir = sourceFile1.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(sourceFile1.name)
        params["out.file.basename"] = outFileName
        params["out.dir"] = outDir
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val edits = ArrayList<Long>()
        edits.add(0.toLong())
        edits.add(1700.toLong())
        edits.add(9000.toLong())
        edits.add(1.toLong())
        edits.add(1500.toLong())
        edits.add(7500.toLong())
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), edits, Arrays.asList(*eprofiles), 2000) // Concat
        // 2
        // input files
        // into 2 output
        // formats
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices) {
            assertTrue(outputs[i].exists())
            assertTrue(outputs[i].length() > 0)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testConcatEdit2segments() {
        logger.info("testConcatEdit2segment")
        val sourceUrl = javaClass.getResource("/audiovideo.mov")
        val sourceFile1 = File(workingDirectory, "audiovideo.mov")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        val eprofiles = arrayOf(profileScanner!!.getProfile("h264-low.http"), profileScanner!!.getProfile("h264-medium.http"), profileScanner!!.getProfile("h264-large.http"))

        val files = arrayOf(sourceFile1, sourceFile1)

        val params = HashMap<String, String>()
        val outDir = sourceFile1.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(sourceFile1.name)
        params["out.file.basename"] = outFileName
        params["out.dir"] = outDir
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val edits = ArrayList<Long>()
        edits.add(0.toLong())
        edits.add(0.toLong())
        edits.add(2500.toLong()) // These 2 edits will be merged
        edits.add(0.toLong())
        edits.add(3000.toLong())
        edits.add(5500.toLong())
        edits.add(1.toLong())
        edits.add(8000.toLong())
        edits.add(10500.toLong())
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), edits, Arrays.asList(*eprofiles), 1000) // Concat
        // 2
        // input files
        // into 2 output
        // formats
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices)
            assertTrue(outputs[i].length() > 0)
    }

    // When edit points are out of order in the SMIL
    @Test
    @Throws(Exception::class)
    fun testConcatEditReorderSegments() {
        val sourceUrl = javaClass.getResource("/audiovideo.mov")
        val sourceFile1 = File(workingDirectory, "audiovideo.mov")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        val eprofiles = arrayOf(profileScanner!!.getProfile("h264-low.http"), profileScanner!!.getProfile("h264-medium.http"))
        val files = arrayOf(sourceFile1)

        val params = HashMap<String, String>()
        val outDir = sourceFile1.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(sourceFile1.name)
        params["out.file.basename"] = outFileName
        params["out.dir"] = outDir
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val edits = ArrayList<Long>()
        edits.add(0.toLong())
        edits.add(0.toLong())
        edits.add(2500.toLong())
        edits.add(0.toLong()) // This is out of order
        edits.add(8000.toLong())
        edits.add(10500.toLong())
        edits.add(0.toLong())
        edits.add(3000.toLong())
        edits.add(5500.toLong())
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), edits, Arrays.asList(*eprofiles), 0)
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices)
            assertTrue(outputs[i].length() > 0)
    }

    // Single input, Single output, Filter
    @Test
    @Throws(Exception::class)
    fun testConcatEditVideoNoSplitFilter() {
        val sourceUrl = javaClass.getResource("/video.mp4") // Video Only
        val sourceFile1 = File(workingDirectory, "video.mp4")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)

        val eprofiles = arrayOf(profileScanner!!.getProfile("h264-low.http")) // ,
        // profileScanner.getProfile("h264-medium.http")
        // };

        val files = arrayOf(sourceFile1)

        val params = HashMap<String, String>()
        val outDir = sourceFile1.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(sourceFile1.name)
        params["out.file.basename"] = outFileName
        params["out.dir"] = outDir
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val edits = ArrayList<Long>()
        edits.add(0.toLong())
        edits.add(0.toLong())
        edits.add(2500.toLong()) // These 2 edits will be merged
        edits.add(0.toLong())
        edits.add(3000.toLong())
        edits.add(5500.toLong())
        edits.add(0.toLong())
        edits.add(9000.toLong())
        edits.add(17500.toLong())
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), edits, Arrays.asList(*eprofiles), 1000, true,
                false) // Video
        // Only
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices)
            assertTrue(outputs[i].length() > 0)
    }

    // Single input, Single output, No Filter
    @Test
    @Throws(Exception::class)
    fun testConcatEditAudioNoSplitNoFilter() {
        val sourceUrl = javaClass.getResource("/audio.mp3") // Video Only
        val sourceFile1 = File(workingDirectory, "audio.mp3")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        val eprofiles = arrayOf(profileScanner!!.getProfile("mp3audio.http"))

        val files = arrayOf(sourceFile1)
        val params = HashMap<String, String>()
        val outDir = sourceFile1.absoluteFile.parent
        val outFileName = FilenameUtils.getBaseName(sourceFile1.name)
        params["out.file.basename"] = outFileName
        params["out.dir"] = outDir
        val edits = ArrayList<Long>()
        edits.add(0.toLong())
        edits.add(0.toLong())
        edits.add(5500.toLong()) // in ms
        edits.add(0.toLong())
        edits.add(9000.toLong())
        edits.add(17500.toLong())
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), edits, Arrays.asList(*eprofiles), 0, false, true) // Audio
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices)
            assertTrue(outputs[i].length() > 0)
    }

    // Single input, Two outputs, No Edit, No transition
    @Test
    @Throws(Exception::class)
    fun testmultiTrimConcatNoEdit() {
        val sourceUrl = javaClass.getResource("/audiovideo.mov") // Video Only
        val sourceFile1 = File(workingDirectory, "audiovideo.mov")
        FileUtils.copyURLToFile(sourceUrl, sourceFile1)
        val eprofiles = arrayOf(profileScanner!!.getProfile("h264-low.http"), profileScanner!!.getProfile("h264-medium.http"))
        val files = arrayOf(sourceFile1)
        val outputs = engine!!.multiTrimConcat(Arrays.asList(*files), null, Arrays.asList(*eprofiles), 0, true, true)
        assertTrue(outputs.size == eprofiles.size)
        for (i in eprofiles.indices)
            assertTrue(outputs[i].length() > 0)
    }

    @Test
    @Throws(EncoderException::class)
    fun testRawMultiEncode() {
        if (!ffmpegInstalled)
            return
        val profiles = ArrayList<EncodingProfile>()
        profiles.add(profileScanner!!.getProfile("h264-low.http"))
        profiles.add(profileScanner!!.getProfile("flash.rtmp"))
        profiles.add(profileScanner!!.getProfile("h264-medium.http"))
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val outputs = engine!!.multiTrimConcat(Arrays.asList(sourceAudioVideo!!), null, profiles, 0, true, true)
        assertTrue(outputs.size == profiles.size)
        for (i in profiles.indices) {
            assertTrue(outputs[i].exists())
            assertTrue(outputs[i].length() > 0)
        }
    }

    @Test
    @Throws(EncoderException::class)
    fun testRawMultiEncodeNoAudio() {
        if (!ffmpegInstalled)
            return
        // EncodingProfile profile = profileScanner.getProfile(multiProfile);
        val profiles = ArrayList<EncodingProfile>()
        profiles.add(profileScanner!!.getProfile("h264-low.http"))
        profiles.add(profileScanner!!.getProfile("flash.rtmp"))
        profiles.add(profileScanner!!.getProfile("h264-medium.http"))
        // create encoder process.
        // no special working dir is set which means the working dir of the
        // current java process is used
        val outputs = engine!!.multiTrimConcat(Arrays.asList(sourceAudioVideo!!), null, profiles, 0, true, false)
        assertTrue(outputs.size == profiles.size)
        for (i in profiles.indices) {
            assertTrue(outputs[i].exists())
            assertTrue(outputs[i].length() > 0)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMultiEncodeSingleProfile() {
        if (!ffmpegInstalled)
            return
        assertTrue(sourceAudioVideo!!.isFile)
        // Set up workspace
        val profiles = ArrayList<EncodingProfile>()
        profiles.add(profileScanner!!.getProfile("h264-low.http"))
        val outputs = engine!!.multiTrimConcat(Arrays.asList(sourceAudioVideo!!), null, profiles, 0, true, false)
        assertTrue(outputs.size == profiles.size)
        for (i in profiles.indices) {
            assertTrue(outputs[i].exists())
            assertTrue(outputs[i].length() > 0)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMultiEncodeJob() {
        if (!ffmpegInstalled)
            return
        val profiles = arrayOf("h264-low.http", "flash.rtmp", "h264-medium.http")
        val sourceTrack = MediaPackageElementParser.getFromXml(
                IOUtils.toString(ComposerServiceTest::class.java.getResourceAsStream("/composer_test_source_track_video.xml"),
                        Charset.defaultCharset())) as Track

        val multiencode = composerService!!.multiEncode(sourceTrack, Arrays.asList(*profiles))
        val processedTracks = MediaPackageElementParser.getArrayFromXml(multiencode.payload) as List<Track>
        Assert.assertNotNull(processedTracks)
        Assert.assertEquals(profiles.size.toLong(), processedTracks.size.toLong()) // Same number of outputs as profiles
        for (processedTrack in processedTracks) {
            Assert.assertNotNull(processedTrack.identifier)
        }
    }

    companion object {

        /** File pointer to the testing dir  */
        private val workingDirectory = File("target")

        /** FFmpeg binary location and engine  */
        private val FFMPEG_BINARY = "ffmpeg"

        /** True to run the tests  */
        private var ffmpegInstalled = true

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(ComplexCmdsEncoderEngineTest::class.java)

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
            return File(ComplexCmdsEncoderEngineTest::class.java.getResource(path).toURI())
        }
    }
}
