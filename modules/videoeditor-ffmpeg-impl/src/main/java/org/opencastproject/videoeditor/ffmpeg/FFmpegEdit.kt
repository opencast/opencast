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
import org.opencastproject.videoeditor.impl.VideoClip
import org.opencastproject.videoeditor.impl.VideoEditorProperties

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Arrays
import java.util.Locale
import java.util.Properties

/**
 * FFmpeg wrappers:
 * processEdits:    process SMIL definitions of segments into one consecutive video
 * There is a fade in and a fade out at the beginning and end of each clip
 *
 */
class FFmpegEdit {

    protected var vfade: Float = 0.toFloat()
    protected var afade: Float = 0.toFloat()
    protected var ffmpegProperties = DEFAULT_FFMPEG_PROPERTIES
    protected var ffmpegScaleFilter: String? = null
    protected var videoCodec: String? = null  // By default, use the same codec as source
    protected var audioCodec: String? = null

    constructor() {
        this.afade = java.lang.Float.parseFloat(DEFAULT_AUDIO_FADE)
        this.vfade = java.lang.Float.parseFloat(DEFAULT_VIDEO_FADE)
        this.ffmpegProperties = DEFAULT_FFMPEG_PROPERTIES
    }

    /*
   * Init with properties
   */
    constructor(properties: Properties) {
        var fade = properties.getProperty(VideoEditorProperties.AUDIO_FADE, DEFAULT_AUDIO_FADE)
        try {
            this.afade = java.lang.Float.parseFloat(fade)
        } catch (e: Exception) {
            logger.error("Unable to parse audio fade duration {}. Falling back to default value.", DEFAULT_AUDIO_FADE)
            this.afade = java.lang.Float.parseFloat(DEFAULT_AUDIO_FADE)
        }

        fade = properties.getProperty(VideoEditorProperties.VIDEO_FADE, DEFAULT_VIDEO_FADE)
        try {
            this.vfade = java.lang.Float.parseFloat(fade)
        } catch (e: Exception) {
            logger.error("Unable to parse video fade duration {}. Falling back to default value.", DEFAULT_VIDEO_FADE)
            this.vfade = java.lang.Float.parseFloat(DEFAULT_VIDEO_FADE)
        }

        this.ffmpegProperties = properties.getProperty(VideoEditorProperties.FFMPEG_PROPERTIES, DEFAULT_FFMPEG_PROPERTIES)
        this.ffmpegScaleFilter = properties.getProperty(VideoEditorProperties.FFMPEG_SCALE_FILTER, null)
        this.videoCodec = properties.getProperty(VideoEditorProperties.VIDEO_CODEC, null)
        this.audioCodec = properties.getProperty(VideoEditorProperties.AUDIO_CODEC, null)
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun processEdits(inputfiles: List<String>, dest: String, outputSize: String, cleanclips: List<VideoClip>,
                     hasAudio: Boolean = true, hasVideo: Boolean = true): String? {
        val cmd = makeEdits(inputfiles, dest, outputSize, cleanclips, hasAudio, hasVideo)
        return run(cmd)
    }

    /* Run the ffmpeg command with the params
   * Takes a list of words as params, the output is logged
   */
    private fun run(params: MutableList<String>): String? {
        var `in`: BufferedReader? = null
        var encoderProcess: Process? = null
        try {
            params.add(0, binary)
            logger.info("executing command: " + StringUtils.join(params, " "))
            val pbuilder = ProcessBuilder(params)
            pbuilder.redirectErrorStream(true)
            encoderProcess = pbuilder.start()
            `in` = BufferedReader(InputStreamReader(
                    encoderProcess!!.inputStream))
            var line: String
            var n = 5
            while ((line = `in`.readLine()) != null) {
                if (n-- > 0)
                    logger.info(line)
            }

            // wait until the task is finished
            encoderProcess.waitFor()
            val exitCode = encoderProcess.exitValue()
            if (exitCode != 0) {
                throw Exception("Ffmpeg exited abnormally with status $exitCode")
            }

        } catch (ex: Exception) {
            logger.error("VideoEditor ffmpeg failed", ex)
            return ex.toString()
        } finally {
            IoSupport.closeQuietly(`in`)
            IoSupport.closeQuietly(encoderProcess)
        }
        return null
    }

    /*
   * Construct the ffmpeg command from  src, in-out points and output resolution
   * Inputfile is an ordered list of video src
   * clips is a list of edit points indexing into the video src list
   * outputResolution when specified is the size to which all the clips will scale
   * hasAudio and hasVideo specify media type of the input files
   * NOTE: This command will fail if the sizes are mismatched or
   * if some of the clips aren't same as specified mediatype
   * (hasn't audio or video stream while hasAudio, hasVideo parameter set)
   */
    @Throws(Exception::class)
    fun makeEdits(inputfiles: List<String>, dest: String, outputResolution: String?,
                  clips: List<VideoClip>, hasAudio: Boolean, hasVideo: Boolean): MutableList<String> {

        if (!hasAudio && !hasVideo) {
            throw IllegalArgumentException("Inputfiles should have at least audio or video stream.")
        }

        val f = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        val n = clips.size
        var i: Int
        var outmap = ""
        var scale = ""
        val command = ArrayList<String>()
        val vpads = ArrayList<String>()
        val apads = ArrayList<String>()
        val clauses = ArrayList<String>() // The clauses are ordered

        if (n > 1) { // Create the input pads if we have multiple segments
            i = 0
            while (i < n) {
                if (hasVideo) {
                    vpads.add("[v$i]")  // post filter
                }
                if (hasAudio) {
                    apads.add("[a$i]")
                }
                i++
            }
        }
        if (hasVideo) {
            if (outputResolution != null && outputResolution.length > 3) { // format is "<width>x<height>"
                // scale each clip to the same size
                scale = ",scale=$outputResolution"
            } else if (ffmpegScaleFilter != null) {
                // Use scale filter if configured
                scale = ",scale=" + ffmpegScaleFilter!!
            }
        }

        i = 0
        while (i < n) { // Examine each clip
            // get clip and add fades to each clip
            val vclip = clips[i]
            val fileindx = vclip.src   // get source file by index
            val inpt = vclip.start     // get in points
            val duration = vclip.duration

            var clip = ""
            if (hasVideo) {
                var vfadeFilter = ""
                /* Only include fade into the filter graph if necessary */
                if (vfade > 0.00001) {
                    val vend = duration - vfade
                    vfadeFilter = ",fade=t=in:st=0:d=" + vfade + ",fade=t=out:st=" + f.format(vend) + ":d=" + vfade
                }
                /* Add filters for video */
                clip = ("[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                        + scale + ",setpts=PTS-STARTPTS" + vfadeFilter + "[v" + i + "]")

                clauses.add(clip)
            }

            if (hasAudio) {
                var afadeFilter = ""
                /* Only include fade into the filter graph if necessary */
                if (afade > 0.00001) {
                    val aend = duration - afade
                    afadeFilter = ",afade=t=in:st=0:d=" + afade + ",afade=t=out:st=" + f.format(aend) + ":d=" + afade
                }
                /* Add filters for audio */
                clip = ("[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                        + ",asetpts=PTS-STARTPTS" + afadeFilter + "[a"
                        + i + "]")
                clauses.add(clip)
            }
            i++
        }
        if (n > 1) { // concat the outpads when there are more then 1 per stream
            // use unsafe because different files may have different SAR/framerate
            if (hasVideo) {
                clauses.add(StringUtils.join(vpads, "") + "concat=n=" + n + ":unsafe=1[ov0]") // concat video clips
            }
            if (hasAudio) {
                clauses.add(StringUtils.join(apads, "") + "concat=n=" + n
                        + ":v=0:a=1[oa0]") // concat audio clips in stream 0, video in stream 1
            }
            outmap = "o"                 // if more than one clip
        }
        command.add("-y")      // overwrite old pathname
        for (o in inputfiles) {
            command.add("-i")   // Add inputfile in the order of entry
            command.add(o)
        }
        command.add("-filter_complex")
        command.add(StringUtils.join(clauses, ";"))
        val options = ffmpegProperties.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        command.addAll(Arrays.asList(*options))
        if (hasAudio) {
            command.add("-map")
            command.add("[" + outmap + "a0]")
        }
        if (hasVideo) {
            command.add("-map")
            command.add("[" + outmap + "v0]")
        }
        if (hasVideo && videoCodec != null) { // If using different codecs from source, add them here
            command.add("-c:v")
            command.add(videoCodec)
        }
        if (hasAudio && audioCodec != null) {
            command.add("-c:a")
            command.add(audioCodec)
        }
        command.add(dest)

        return command
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FFmpegEdit::class.java)
        private val FFMPEG_BINARY_DEFAULT = "ffmpeg"
        private val CONFIG_FFMPEG_PATH = "org.opencastproject.composer.ffmpeg.path"

        private val DEFAULT_FFMPEG_PROPERTIES = "-strict -2 -preset faster -crf 18"
        val DEFAULT_OUTPUT_FILE_EXTENSION = ".mp4"
        private val DEFAULT_AUDIO_FADE = "2.0"
        private val DEFAULT_VIDEO_FADE = "2.0"
        private var binary = FFMPEG_BINARY_DEFAULT

        fun init(bundleContext: BundleContext) {
            val path = bundleContext.getProperty(CONFIG_FFMPEG_PATH)

            if (StringUtils.isNotBlank(path)) {
                binary = path.trim { it <= ' ' }
            }
        }
    }
}
