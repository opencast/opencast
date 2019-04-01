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

package org.opencastproject.videoeditor.ffmpeg

import org.opencastproject.util.IoSupport
import org.opencastproject.util.StreamHelper
import org.opencastproject.videoeditor.impl.VideoClip

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URISyntaxException
import java.util.ArrayList

import junit.framework.Assert

/**
 * Tests the ffmpeg concatenation service
 */
class FFmpegTest {

    protected var inputFilePath: String
    protected var outputFilePath: String

    init {
        try {
            testForFFmpeg()
            inputFilePath = File(javaClass.getResource("/testresources/testvideo_320x180.mp4").toURI()).absolutePath
            outputFilePath = File("target/testoutput/mux.mp4").absolutePath
        } catch (ex: URISyntaxException) {
            logger.error(ex.message)
        }

    }

    @Before
    fun setUp() {
        if (File(outputFilePath).exists()) {
            File(outputFilePath).delete()
        } else if (!File(outputFilePath).parentFile.exists()) {
            File(outputFilePath).parentFile.mkdir()
        }
    }


    /**
     * Test if ffmpeg can split and join 1 file
     * Skip if no ffmpeg
     */
    @Test
    @Throws(Exception::class)
    fun ffmpegEditTest() {

        if (!ffmpegInstalled) {
            return
        }
        val input = ArrayList<String>()
        val clips = ArrayList<VideoClip>()
        clips.add(VideoClip(0, 0.0, 10.0))
        clips.add(VideoClip(0, 25.0, 44.0))
        input.add(inputFilePath)
        val fmp = FFmpegEdit()
        fmp.processEdits(input, outputFilePath, null, clips)
        Assert.assertTrue("Edited File is nonzero", File(outputFilePath).length() > 0)
        logger.info("Ffmpeg concat 2 clips from 1 file OK!")

    }

    /**
     * Test if ffmpeg can split and join 2 files of the same size and frame rate
     * Different sizes will fail - need to add scale=WxH
     * Skip if no ffmpeg
     */
    @Test
    @Throws(Exception::class)
    fun ffmpegEditTest2Sources() {

        if (!ffmpegInstalled) {
            return
        }
        val input = ArrayList<String>()
        val clips = ArrayList<VideoClip>()
        clips.add(VideoClip(0, 0.0, 10.0))
        clips.add(VideoClip(1, 25.0, 44.0))
        input.add(inputFilePath)
        input.add(inputFilePath)
        val fmp = FFmpegEdit()
        fmp.processEdits(input, outputFilePath, "320x180", clips)    // Both clips are scaled to 320x180
        Assert.assertTrue("Edited File is nonzero", File(outputFilePath).length() > 0)
        logger.info("Ffmpeg concat 2 clips from 2 files OK!")
    }

    companion object {

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(FFmpegTest::class.java)
        private val FFMPEG_BINARY = "ffmpeg"
        protected var ffmpegInstalled = true

        @BeforeClass
        fun testForFFmpeg() {
            var stdout: StreamHelper? = null
            var stderr: StreamHelper? = null
            var p: Process? = null
            // Test that fmpeg exists
            try {
                p = ProcessBuilder(FFMPEG_BINARY, "-version").start()
                stdout = StreamHelper(p!!.inputStream)
                stderr = StreamHelper(p.errorStream)
                val status = p.waitFor()
                stdout.stopReading()
                stderr.stopReading()
                if (status != 0)
                    throw IllegalStateException()
            } catch (t: Throwable) {
                logger.warn("Skipping ffmpeg video editor service tests due to unsatisifed or erroneus ffmpeg installation")
                ffmpegInstalled = false
            } finally {
                IoSupport.closeQuietly(stdout)
                IoSupport.closeQuietly(stderr)
                IoSupport.closeQuietly(p)
            }
        }
    }
}
