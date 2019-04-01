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

package org.opencastproject.sox.impl

import org.easymock.EasyMock.capture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.sox.api.SoxException
import org.opencastproject.util.IoSupport
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI
import java.util.ArrayList

/**
 * Tests the [SoxServiceImpl].
 */
class SoxServiceTest {

    /** The source file to test with  */
    private var source: File? = null

    /** The SoX service to test  */
    private var soxService: SoxServiceImpl? = null

    private var sourceTrack: TrackImpl? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        if (!soxInstalled)
            return

        // Copy an existing media file to a temp file
        val f = File(javaClass.getResource("/audio-test.wav").file)
        source = File.createTempFile(FilenameUtils.getBaseName(f.name), ".wav")
        FileUtils.copyFile(f, source!!)

        val org = DefaultOrganization()
        val user = JaxbUser("admin", "test", org, JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org))
        val orgDirectory = EasyMock.createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect<Organization>(orgDirectory.getOrganization(EasyMock.anyObject<Any>() as String)).andReturn(org).anyTimes()

        val userDirectory = EasyMock.createNiceMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes()

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<Organization>(securityService.organization).andReturn(org).anyTimes()
        EasyMock.expect(securityService.user).andReturn(user).anyTimes()

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject<Any>() as URI)).andReturn(source).anyTimes()

        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bc.getProperty(SoxServiceImpl.CONFIG_SOX_PATH)).andReturn(SOX_BINARY).anyTimes()

        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()

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
                    job.payload = soxService!!.process(job)
                    job
                }.anyTimes()

        // Finish setting up the mocks
        EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace, serviceRegistry)

        // Create and populate the composer service
        soxService = SoxServiceImpl()
        soxService!!.organizationDirectoryService = orgDirectory
        soxService!!.securityService = securityService
        soxService!!.serviceRegistry = serviceRegistry
        soxService!!.userDirectoryService = userDirectory
        soxService!!.setWorkspace(workspace)
        soxService!!.activate(cc)

        // Initialize track
        sourceTrack = TrackImpl()
        val audioStream = AudioStreamImpl()
        audioStream.bitDepth = 16
        audioStream.samplingRate = 8000
        audioStream.channels = 1
        audioStream.rmsLevDb = -20.409999f
        sourceTrack!!.addStream(audioStream)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(source)
    }

    @Test
    @Throws(Exception::class)
    fun testAnalyzeAudio() {
        if (!soxInstalled)
            return

        assertTrue(source!!.isFile)
        val job = soxService!!.analyze(sourceTrack!!)
        val track = MediaPackageElementParser.getFromXml(job.payload) as TrackImpl
        val audioStream = track.getAudio()!![0]
        assertEquals(-1.159999966621399, audioStream.pkLevDb!!.toDouble(), 0.0002)
        assertEquals(-20.40999984741211, audioStream.rmsLevDb!!.toDouble(), 0.0002)
        assertEquals(-13.779999732971191, audioStream.rmsPkDb!!.toDouble(), 0.0002)
    }

    @Test
    @Throws(Exception::class)
    fun testNormalizeIncreaseAudio() {
        if (!soxInstalled)
            return

        assertTrue(source!!.isFile)
        val job = soxService!!.normalize(sourceTrack!!, -25f)
        val track = MediaPackageElementParser.getFromXml(job.payload) as TrackImpl
        val audioStream = track.getAudio()!![0]
        assertEquals(-25.0, audioStream.rmsLevDb!!.toDouble(), 0.9)
    }

    @Test
    @Throws(Exception::class)
    fun testNormalizeDecreaseAudio() {
        if (!soxInstalled)
            return

        assertTrue(source!!.isFile)
        val job = soxService!!.normalize(sourceTrack!!, -30f)
        val track = MediaPackageElementParser.getFromXml(job.payload) as TrackImpl
        val audioStream = track.getAudio()!![0]
        assertEquals(-30.0, audioStream.rmsLevDb!!.toDouble(), 0.1)
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(SoxServiceTest::class.java)

        private val SOX_BINARY = "sox"

        /** True to run the tests  */
        private var soxInstalled = true

        @BeforeClass
        @Throws(SoxException::class)
        fun testForSox() {
            var p: Process? = null
            try {
                val command = ArrayList<String>()
                command.add(SOX_BINARY)
                command.add("--version")
                p = ProcessBuilder(command).start()
                if (p!!.waitFor() != 0) {
                    throw IllegalStateException()
                }
            } catch (t: Throwable) {
                logger.warn("Skipping sox audio processing service tests due to unsatisfied or erroneous sox installation")
                soxInstalled = false
            } finally {
                IoSupport.closeQuietly(p)
            }
        }
    }

}
