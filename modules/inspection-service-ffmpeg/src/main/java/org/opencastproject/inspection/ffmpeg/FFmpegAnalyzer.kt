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

import org.opencastproject.inspection.ffmpeg.api.AudioStreamMetadata
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzerException
import org.opencastproject.inspection.ffmpeg.api.MediaContainerMetadata
import org.opencastproject.inspection.ffmpeg.api.VideoStreamMetadata
import org.opencastproject.util.ProcessRunner
import org.opencastproject.util.ProcessRunner.ProcessInfo

import com.entwinemedia.fn.Pred

import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.util.ArrayList

/**
 * This MediaAnalyzer implementation uses the ffprobe binary of FFmpeg for media analysis. Also this implementation does
 * not keep control-, text- or other non-audio or video streams and purposefully ignores them during the
 * `postProcess()` step.
 */
class FFmpegAnalyzer(
        /** Whether the calculation of the frames is accurate or not  */
        private val accurateFrameCount: Boolean) : MediaAnalyzer {

    /** Path to the executable  */
    /**
     * Returns the binary used to provide media inspection functionality.
     *
     * @return the binary
     */
    protected var binary: String? = null
        set

    init {
        // instantiated using MediaAnalyzerFactory via newInstance()
        this.binary = FFPROBE_BINARY_DEFAULT
    }

    @Throws(MediaAnalyzerException::class)
    override fun analyze(media: File): MediaContainerMetadata {
        if (binary == null)
            throw IllegalStateException("Binary is not set")

        val command = ArrayList<String>()
        command.add("-show_format")
        command.add("-show_streams")
        if (accurateFrameCount)
            command.add("-count_frames")
        command.add("-of")
        command.add("json")
        command.add(media.absolutePath.replace(" ".toRegex(), "\\ "))

        val commandline = StringUtils.join(command, " ")

        /* Execute ffprobe and obtain the result */
        logger.debug("Running {} {}", binary, commandline)

        val metadata = MediaContainerMetadata()

        val sb = StringBuilder()
        try {
            val info = ProcessRunner.mk(binary!!, command.toTypedArray())
            val exitCode = ProcessRunner.run(info, object : Pred<String>() {
                override fun apply(s: String): Boolean? {
                    logger.debug(s)
                    sb.append(s)
                    sb.append(System.getProperty("line.separator"))
                    return true
                }
            }, fnLogError)
            // Windows binary will return -1 when queried for options
            if (exitCode != -1 && exitCode != 0 && exitCode != 255)
                throw MediaAnalyzerException("Frame analyzer $binary exited with code $exitCode")
        } catch (e: IOException) {
            logger.error("Error executing ffprobe", e)
            throw MediaAnalyzerException("Error while running ffprobe " + binary!!, e)
        }

        val parser = JSONParser()

        try {
            val jsonObject = parser.parse(sb.toString()) as JSONObject
            var obj: Any?
            var duration: Double?

            /* Get format specific stuff */
            val jsonFormat = jsonObject["format"] as JSONObject

            /* File Name */
            obj = jsonFormat["filename"]
            if (obj != null) {
                metadata.fileName = obj as String?
            }

            /* Format */
            obj = jsonFormat["format_long_name"]
            if (obj != null) {
                metadata.format = obj as String?
            }

            /*
       * Mediainfo does not return a duration if there is no stream but FFprobe will return 0. For compatibility
       * reasons, check if there are any streams before reading the duration:
       */
            obj = jsonFormat["nb_streams"]
            if (obj != null && obj as Long? > 0) {
                obj = jsonFormat["duration"]
                if (obj != null) {
                    duration = Double((obj as String?)!!) * 1000
                    metadata.duration = duration.toLong()
                }
            }

            /* File Size */
            obj = jsonFormat["size"]
            if (obj != null) {
                metadata.size = Long((obj as String?)!!)
            }

            /* Bitrate */
            obj = jsonFormat["bit_rate"]
            if (obj != null) {
                metadata.bitRate = Float((obj as String?)!!)
            }

            /* Loop through streams */
            /*
       * FFprobe will return an empty stream array if there are no streams. Thus we do not need to check.
       */
            val streams = jsonObject["streams"] as JSONArray
            val iterator = streams.iterator()
            while (iterator.hasNext()) {
                val stream = iterator.next()
                /* Check type of string */
                val codecType = stream.get("codec_type") as String

                /* Handle audio streams ----------------------------- */

                if ("audio" == codecType) {
                    /* Extract audio stream metadata */
                    val aMetadata = AudioStreamMetadata()

                    /* Codec */
                    obj = stream.get("codec_long_name")
                    if (obj != null) {
                        aMetadata.format = obj as String?
                    }

                    /* Duration */
                    obj = stream.get("duration")
                    if (obj != null) {
                        duration = Double((obj as String?)!!) * 1000
                        aMetadata.duration = duration.toLong()
                    } else {
                        /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
                        aMetadata.duration = metadata.duration
                    }

                    /* Bitrate */
                    obj = stream.get("bit_rate")
                    if (obj != null) {
                        aMetadata.bitRate = Float((obj as String?)!!)
                    }

                    /* Channels */
                    obj = stream.get("channels")
                    if (obj != null) {
                        aMetadata.channels = (obj as Long).toInt()
                    }

                    /* Sample Rate */
                    obj = stream.get("sample_rate")
                    if (obj != null) {
                        aMetadata.samplingRate = Integer.parseInt((obj as String?)!!)
                    }

                    /* Frame Count */
                    obj = stream.get("nb_read_frames")
                    if (obj != null) {
                        aMetadata.frames = java.lang.Long.parseLong((obj as String?)!!)
                    } else {

                        /* alternate JSON element if accurate frame count is not requested from ffmpeg */
                        obj = stream.get("nb_frames")
                        if (obj != null) {
                            aMetadata.frames = java.lang.Long.parseLong((obj as String?)!!)
                        }
                    }

                    /* Add video stream metadata to overall metadata */
                    metadata.audioStreamMetadata.add(aMetadata)

                    /* Handle video streams ----------------------------- */

                } else if ("video" == codecType) {
                    /* Extract video stream metadata */
                    val vMetadata = VideoStreamMetadata()

                    /* Codec */
                    obj = stream.get("codec_long_name")
                    if (obj != null) {
                        vMetadata.format = obj as String?
                    }

                    /* Duration */
                    obj = stream.get("duration")
                    if (obj != null) {
                        duration = Double((obj as String?)!!) * 1000
                        vMetadata.duration = duration.toLong()
                    } else {
                        /*
             * If no duration for this stream is specified assume the duration of the file for this as well.
             */
                        vMetadata.duration = metadata.duration
                    }

                    /* Bitrate */
                    obj = stream.get("bit_rate")
                    if (obj != null) {
                        vMetadata.bitRate = Float((obj as String?)!!)
                    }

                    /* Width */
                    obj = stream.get("width")
                    if (obj != null) {
                        vMetadata.frameWidth = (obj as Long).toInt()
                    }

                    /* Height */
                    obj = stream.get("height")
                    if (obj != null) {
                        vMetadata.frameHeight = (obj as Long).toInt()
                    }

                    /* Profile */
                    obj = stream.get("profile")
                    if (obj != null) {
                        vMetadata.formatProfile = obj as String?
                    }

                    /* Aspect Ratio */
                    obj = stream.get("sample_aspect_ratio")
                    if (obj != null) {
                        vMetadata.pixelAspectRatio = parseFloat((obj as String?)!!)
                    }

                    /* Frame Rate */
                    obj = stream.get("avg_frame_rate")
                    if (obj != null) {
                        vMetadata.frameRate = parseFloat((obj as String?)!!)
                    }

                    /* Frame Count */
                    obj = stream.get("nb_read_frames")
                    if (obj != null) {
                        vMetadata.frames = java.lang.Long.parseLong((obj as String?)!!)
                    } else {

                        /* alternate JSON element if accurate frame count is not requested from ffmpeg */
                        obj = stream.get("nb_frames")
                        if (obj != null) {
                            vMetadata.frames = java.lang.Long.parseLong((obj as String?)!!)
                        }
                    }

                    /* Add video stream metadata to overall metadata */
                    metadata.videoStreamMetadata.add(vMetadata)
                }
            }

        } catch (e: ParseException) {
            logger.error("Error parsing ffprobe output: {}", e.message)
        }

        return metadata
    }

    /**
     * Allows configuration {@inheritDoc}
     *
     * @see org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer.setConfig
     */
    override fun setConfig(config: Map<String, Any>?) {
        if (config != null) {
            if (config.containsKey(FFPROBE_BINARY_CONFIG)) {
                var binary = config[FFPROBE_BINARY_CONFIG] as String
                binary = binary
                logger.debug("FFmpegAnalyzer config binary: $binary")
            }
        }
    }

    private fun parseFloat(`val`: String): Float {
        if (`val`.contains("/")) {
            val v = `val`.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return java.lang.Float.parseFloat(v[0]) / java.lang.Float.parseFloat(v[1])
        } else if (`val`.contains(":")) {
            val v = `val`.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return java.lang.Float.parseFloat(v[0]) / java.lang.Float.parseFloat(v[1])
        } else {
            return java.lang.Float.parseFloat(`val`)
        }
    }

    companion object {

        val FFPROBE_BINARY_CONFIG = "org.opencastproject.inspection.ffprobe.path"
        val FFPROBE_BINARY_DEFAULT = "ffprobe"

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(FFmpegAnalyzer::class.java)

        private val fnLogError = object : Pred<String>() {
            override fun apply(s: String): Boolean {
                logger.debug(s)
                return true
            }
        }
    }

}
