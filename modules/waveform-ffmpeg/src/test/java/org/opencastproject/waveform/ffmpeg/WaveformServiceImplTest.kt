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
package org.opencastproject.waveform.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.IoSupport
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.exception.ExceptionUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary
import java.util.Hashtable

/**
 * Test class for WaveformServiceImpl.
 */
class WaveformServiceImplTest {

    /**
     * Test of updated method of class WaveformServiceImpl.
     */
    @Test
    @Throws(Exception::class)
    fun testUpdated() {
        val properties = Hashtable<String, String>()
        properties[WaveformServiceImpl.WAVEFORM_COLOR_CONFIG_KEY] = "blue green 0x2A2A2A 323232CC"
        properties[WaveformServiceImpl.WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY] = "false"
        properties[WaveformServiceImpl.WAVEFORM_SCALE_CONFIG_KEY] = "lin"

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect<List<HostRegistration>>(serviceRegistry.hostRegistrations).andReturn(ArrayList())
        EasyMock.replay(serviceRegistry)

        val instance = WaveformServiceImpl()
        instance.serviceRegistry = serviceRegistry
        try {
            instance.updated(properties)
            // we can not check private fields but it should not throw any exception
        } catch (e: Exception) {
            fail("updated method should not throw any exceptions but has thrown: " + ExceptionUtils.getStackTrace(e))
        }

    }

    /**
     * Test of createWaveformImage method of class WaveformServiceImpl.
     */
    @Test
    @Throws(Exception::class)
    fun testGenerateWaveformImage() {
        val expectedJob = JobImpl(1)
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.createJob(
                EasyMock.eq<String>(WaveformServiceImpl.JOB_TYPE),
                EasyMock.eq(WaveformServiceImpl.Operation.Waveform.toString()),
                EasyMock.anyObject<Any>() as List<String>, EasyMock.anyFloat()))
                .andReturn(expectedJob)
        EasyMock.replay(serviceRegistry)

        val instance = WaveformServiceImpl()
        instance.serviceRegistry = serviceRegistry
        val job = instance.createWaveformImage(dummyTrack!!, 200, 5000, 20000, 500, "black")
        assertEquals(expectedJob, job)
    }

    /**
     * Test of process method of class WaveformServiceImpl.
     */
    @Test
    @Throws(Exception::class)
    fun testProcess() {
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject()))
                .andReturn(File(audioTrack!!.getURI()))
        val filenameCapture = Capture.newInstance<String>()
        EasyMock.expect(workspace.putInCollection(
                EasyMock.anyString(), EasyMock.capture(filenameCapture), EasyMock.anyObject<InputStream>()))
                .andReturn(URI("waveform.png"))
        EasyMock.replay(workspace)

        val instance = WaveformServiceImpl()
        instance.setWorkspace(workspace)

        val audioTrackXml = MediaPackageElementParser.getAsXml(audioTrack)
        val job = JobImpl(1)
        job.jobType = WaveformServiceImpl.JOB_TYPE
        job.operation = WaveformServiceImpl.Operation.Waveform.toString()
        job.arguments = Arrays.asList(audioTrackXml, "200", "5000", "20000", "500", "black")
        val result = instance.process(job)
        assertNotNull(result)

        val waveformAttachment = MediaPackageElementParser.getFromXml(result)
        assertEquals(URI("waveform.png"), waveformAttachment.getURI())
        assertTrue(filenameCapture.hasCaptured())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WaveformServiceImplTest::class.java)

        private var audioTrack: Track? = null
        private var dummyTrack: Track? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUpClass() {
            audioTrack = readTrackFromResource("/audio-track.xml")
            audioTrack!!.setURI(WaveformServiceImplTest::class.java.getResource("/test.mp3").toURI())
            dummyTrack = readTrackFromResource("/dummy-track.xml")
        }

        @Throws(IOException::class, MediaPackageException::class)
        private fun readTrackFromResource(resourceName: String): Track {
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(InputStreamReader(
                        WaveformServiceImplTest::class.java.getResourceAsStream(resourceName)))
                var line: String? = reader.readLine()
                val trackBuilder = StringBuilder()
                while (line != null) {
                    trackBuilder.append(line)
                    line = reader.readLine()
                }

                return MediaPackageElementParser.getFromXml(trackBuilder.toString()) as Track

            } finally {
                IoSupport.closeQuietly(reader)
            }
        }
    }
}
