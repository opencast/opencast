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

package org.opencastproject.silencedetection.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail

import org.opencastproject.mediapackage.Track
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.impl.SilenceDetectionProperties
import org.opencastproject.util.IoSupport
import org.opencastproject.util.StreamHelper
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

class SilenceDetectorTest {


    /** Setup test.  */
    @Ignore
    @Throws(Exception::class)
    private fun init(resource: URI, hasAudio: Boolean?, props: Properties): FFmpegSilenceDetector {
        val f = File(resource)
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(resource)).andReturn(f)
        EasyMock.replay(workspace)
        val track = EasyMock.createNiceMock<Track>(Track::class.java)
        EasyMock.expect(track.getURI()).andReturn(resource)
        EasyMock.expect(track.identifier).andReturn("123")
        EasyMock.expect<Long>(track.duration).andStubReturn(60000L)
        EasyMock.expect(track.hasAudio()).andReturn(hasAudio)
        EasyMock.replay(track)
        return FFmpegSilenceDetector(props, track, workspace)
    }


    /** Setup test.  */
    @Ignore
    @Throws(Exception::class)
    private fun init(resource: URI, hasAudio: Boolean? = true): FFmpegSilenceDetector {
        val props = Properties()
        props.setProperty(SilenceDetectionProperties.VOICE_MIN_LENGTH, "4000")
        return init(resource, hasAudio, props)
    }


    @Throws(URISyntaxException::class)
    private fun getResource(resource: String): URI {
        return FFmpegSilenceDetector::class.java.getResource(resource).toURI()
    }


    @Test
    @Throws(Exception::class)
    fun testSilenceDetection() {
        if (this.skipTests) return
        val trackUri = getResource("/testspeech.mp4")
        val sd = init(trackUri)
        assertNotNull(sd.mediaSegments)
        assertEquals(2, sd.mediaSegments!!.mediaSegments.size.toLong())
    }


    @Test
    @Throws(Exception::class)
    fun testSilenceDetectionLongVoice() {
        if (this.skipTests) return
        val trackUri = getResource("/testspeech.mp4")
        val props = Properties()
        /* Set minumum voice length to something longer than the actual recording */
        props.setProperty(SilenceDetectionProperties.VOICE_MIN_LENGTH, "600000")
        val sd = init(trackUri, true, props)
        assertNotNull(sd.mediaSegments)
        assertEquals(0, sd.mediaSegments!!.mediaSegments.size.toLong())
    }


    @Test
    @Throws(Exception::class)
    fun testSilenceDetectionOnSilence() {
        if (this.skipTests) return
        val trackUri = getResource("/silent.mp4")
        val sd = init(trackUri)
        assertNotNull(sd.mediaSegments)
        assertEquals(0, sd.mediaSegments!!.mediaSegments.size.toLong())
    }


    @Test
    @Throws(Exception::class)
    fun testMisconfiguration() {
        if (this.skipTests) return
        val trackUri = getResource("/nostreams.mp4")
        val props = Properties()
        props.setProperty(SilenceDetectionProperties.SILENCE_PRE_LENGTH, "6000")
        props.setProperty(SilenceDetectionProperties.SILENCE_MIN_LENGTH, "4000")
        try {
            val sd = init(trackUri, true, props)
            fail("Silence detection of media without audio should fail")
        } catch (e: SilenceDetectionFailedException) {
        }

    }


    @Test
    @Throws(Exception::class)
    fun testNoAudio() {
        if (this.skipTests) return
        val trackUri = getResource("/nostreams.mp4")
        try {
            val sd = init(trackUri, false)
            fail("Silence detection of media without audio should fail")
        } catch (e: SilenceDetectionFailedException) {
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(SilenceDetectorTest::class.java)


        private var skipTests = false


        @BeforeClass
        fun setupClass() {
            var stdout: StreamHelper? = null
            var stderr: StreamHelper? = null
            var p: Process? = null
            try {
                p = ProcessBuilder(FFmpegSilenceDetector.FFMPEG_BINARY_DEFAULT, "-help").start()
                stdout = StreamHelper(p!!.inputStream)
                stderr = StreamHelper(p.errorStream)
                val exitCode = p.waitFor()
                stdout.stopReading()
                stderr.stopReading()
                if (exitCode != 0) {
                    throw IllegalStateException("process returned $exitCode")
                }
            } catch (t: Throwable) {
                logger.warn("Skipping silence detection tests due to unsatisfied FFmpeg installation: " + t.message)
                skipTests = true
            } finally {
                IoSupport.closeQuietly(stdout)
                IoSupport.closeQuietly(stderr)
                IoSupport.closeQuietly(p)
            }
        }
    }

}
/** Setup test.  */
