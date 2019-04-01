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

package org.opencastproject.inspection.ffmpeg

import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.opencastproject.util.MimeType.mimeType
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.inspection.api.util.Options
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.IoSupport
import org.opencastproject.util.MimeType
import org.opencastproject.util.StreamHelper
import org.opencastproject.util.data.Option
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

class MediaInspectionServiceImplTest {

    /** Setup test.  */
    @Ignore
    @Throws(Exception::class)
    private fun init(resource: URI): Option<MediaInspector> {
        for (binary in ffprobePath!!) {
            val f = File(resource)
            val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
            EasyMock.expect(workspace.get(resource)).andReturn(f)
            EasyMock.expect(workspace.get(resource)).andReturn(f)
            EasyMock.expect(workspace.get(resource)).andReturn(f)
            EasyMock.replay(workspace)
            return some(MediaInspector(workspace, binary))
        }
        return none()
    }

    private fun getResource(resource: String): URI {
        try {
            return MediaInspectionServiceImpl::class.java.getResource(resource).toURI()
        } catch (e: URISyntaxException) {
            return chuck(e)
        }

    }

    @Test
    @Throws(Exception::class)
    fun testInspection() {
        val trackUri = getResource("/test.mp4")
        for (mi in init(trackUri)) {
            val track = mi.inspectTrack(trackUri, Options.NO_OPTION)
            // test the returned values
            val cs = Checksum.create(ChecksumType.fromString("md5"), "cc72b7a4f1a68b84fba6f0fb895da395")
            assertEquals(cs, track.checksum)
            assertEquals("video", track.mimeType.type)
            assertEquals("mp4", track.mimeType.subtype)
            assertNotNull(track.duration)
            assertTrue(track.duration > 0)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInspectionEmptyContainer() {
        val trackUri = getResource("/nostreams.mp4")
        for (mi in init(trackUri)) {
            val track = mi.inspectTrack(trackUri, Options.NO_OPTION)
            assertEquals(0, track.streams.size.toLong())
            assertEquals("mp4", track.mimeType.subtype)
            assertEquals(null, track.duration)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEnrichment() {
        val trackUri = getResource("/test.mp4")
        for (mi in init(trackUri)) {
            val track = mi.inspectTrack(trackUri, Options.NO_OPTION)
            // make changes to metadata
            val cs = track.checksum
            track.checksum = null
            val mt = mimeType("video", "flash")
            track.mimeType = mt
            // test the enrich scenario
            var newTrack = mi.enrich(track, false, Options.NO_OPTION) as Track

            val videoStreams = TrackSupport.byType(newTrack.streams, VideoStream::class.java)
            assertTrue(videoStreams[0].frameCount > 0)
            val audioStreams = TrackSupport.byType(newTrack.streams, AudioStream::class.java)
            assertTrue(audioStreams[0].frameCount > 0)
            assertEquals(newTrack.checksum, cs)
            assertEquals(newTrack.mimeType, mt)
            assertNotNull(newTrack.duration)
            assertTrue(newTrack.duration > 0)
            // test the override scenario
            newTrack = mi.enrich(track, true, Options.NO_OPTION) as Track
            assertEquals(newTrack.checksum, cs)
            assertNotSame(newTrack.mimeType, mt)
            assertTrue(newTrack.duration > 0)
        }

        for (mi in init(trackUri)) {
            val track = mi.inspectTrack(trackUri, Options.NO_OPTION)
            // make changes to metadata
            val cs = track.checksum
            track.checksum = null
            val mt = mimeType("video", "flash")
            track.mimeType = mt
            // test the enrich scenario
            var newTrack = mi.enrich(track, false, Options.NO_OPTION) as Track

            val videoStreams = TrackSupport.byType(newTrack.streams, VideoStream::class.java)
            assertTrue(videoStreams[0].frameCount > 0)
            val audioStreams = TrackSupport.byType(newTrack.streams, AudioStream::class.java)
            assertTrue(audioStreams[0].frameCount > 0)
            assertEquals(newTrack.checksum, cs)
            assertEquals(newTrack.mimeType, mt)
            assertNotNull(newTrack.duration)
            assertTrue(newTrack.duration > 0)
            // test the override scenario
            newTrack = mi.enrich(track, true, Options.NO_OPTION) as Track
            assertEquals(newTrack.checksum, cs)
            assertNotSame(newTrack.mimeType, mt)
            assertTrue(newTrack.duration > 0)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEnrichmentEmptyContainer() {
        val trackUri = getResource("/nostreams.mp4")
        for (mi in init(trackUri)) {
            val track = mi.inspectTrack(trackUri, Options.NO_OPTION)
            // make changes to metadata
            val cs = track.checksum
            track.checksum = null
            val mt = mimeType("video", "flash")
            track.mimeType = mt
            // test the enrich scenario
            var newTrack = mi.enrich(track, false, Options.NO_OPTION) as Track
            assertEquals(newTrack.checksum, cs)
            assertEquals(newTrack.mimeType, mt)
            assertNull(newTrack.duration)
            // test the override scenario
            newTrack = mi.enrich(track, true, Options.NO_OPTION) as Track
            assertEquals(newTrack.checksum, cs)
            assertNotSame(newTrack.mimeType, mt)
            assertNull(newTrack.duration)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MediaInspectionServiceImplTest::class.java)

        /** True to run the tests  */
        private var ffprobePath: Option<String>? = null

        @BeforeClass
        fun setupClass() {
            var stdout: StreamHelper? = null
            var stderr: StreamHelper? = null
            var p: Process? = null
            try {
                // ffprobe requires a track in order to return a status code of 0, indicating that it is working as expected
                val uriTrack = MediaInspectionServiceImpl::class.java.getResource("/test.mp4").toURI()
                val f = File(uriTrack)
                p = ProcessBuilder(FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT, f.absolutePath).start()
                stdout = StreamHelper(p!!.inputStream)
                stderr = StreamHelper(p.errorStream)
                val exitCode = p.waitFor()
                stdout.stopReading()
                stderr.stopReading()
                if (exitCode != 0) {
                    throw IllegalStateException("process returned $exitCode")
                }
                ffprobePath = some(FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT)
            } catch (t: Throwable) {
                logger.warn("Skipping media inspection tests due to unsatisfied FFmpeg (ffprobe) installation: " + t.message)
                ffprobePath = none()
            } finally {
                IoSupport.closeQuietly(stdout)
                IoSupport.closeQuietly(stderr)
                IoSupport.closeQuietly(p)
            }
        }
    }
}
