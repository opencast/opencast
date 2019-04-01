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

import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.silencedetection.api.MediaSegment
import org.opencastproject.silencedetection.api.MediaSegments
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.impl.SilenceDetectionProperties
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import com.google.common.io.LineReader

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.LinkedList
import java.util.Locale
import java.util.Properties
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Find silent sequences in audio stream using Gstreamer.
 */
class FFmpegSilenceDetector
/**
 * Create nonsilent sequences detection pipeline.
 * Parse audio stream and store all positions, where the volume level fall under the threshold.
 *
 * @param properties
 * @param track source track
 */
@Throws(SilenceDetectionFailedException::class, MediaPackageException::class, IOException::class)
constructor(properties: Properties?, track: Track, workspace: Workspace) {
    private var filePath: String? = null
    private val trackId: String

    private var segments: List<MediaSegment>? = null

    /**
     * Returns found media segments.
     * @return nonsilent media segments
     */
    val mediaSegments: MediaSegments?
        get() = if (segments == null) null else MediaSegments(trackId, filePath, segments)


    init {
        var properties = properties

        val minSilenceLength: Long
        val minVoiceLength: Long
        val preSilenceLength: Long
        val thresholdDB: String

        //Ensure properties is not null, avoids null checks later
        if (null == properties) {
            properties = Properties()
        }

        minSilenceLength = parseLong(properties, SilenceDetectionProperties.SILENCE_MIN_LENGTH, DEFAULT_SILENCE_MIN_LENGTH)!!
        minVoiceLength = parseLong(properties, SilenceDetectionProperties.VOICE_MIN_LENGTH, DEFAULT_VOICE_MIN_LENGTH)!!
        preSilenceLength = parseLong(properties, SilenceDetectionProperties.SILENCE_PRE_LENGTH, DEFAULT_SILENCE_PRE_LENGTH)!!
        thresholdDB = properties.getProperty(SilenceDetectionProperties.SILENCE_THRESHOLD_DB, DEFAULT_THRESHOLD_DB)

        trackId = track.identifier

        /* Make sure the element can be analyzed using this analysis implementation */
        if (!track.hasAudio()) {
            logger.warn("Track {} has no audio stream to run a silece detection on", trackId)
            throw SilenceDetectionFailedException("Element has no audio stream")
        }

        /* Make sure we are not allowed to move the beginning of a segment into the last segment */
        if (preSilenceLength > minSilenceLength) {
            logger.error("Pre silence length ({}) is configured to be greater than minimun silence length ({})",
                    preSilenceLength, minSilenceLength)
            throw SilenceDetectionFailedException("preSilenceLength > minSilenceLength")
        }

        try {
            val mediaFile = workspace.get(track.getURI())
            filePath = mediaFile.absolutePath
        } catch (e: NotFoundException) {
            throw SilenceDetectionFailedException("Error finding the media file in workspace", e)
        } catch (e: IOException) {
            throw SilenceDetectionFailedException("Error reading media file in workspace", e)
        }

        if (track.duration == null) {
            throw MediaPackageException("Track $trackId does not have a duration")
        }
        logger.info("Track {} loaded, duration is {} s", filePath, track.duration!! / 1000)

        logger.info("Starting silence detection of {}", filePath)
        val mediaPath = filePath!!.replace(" ".toRegex(), "\\ ")
        val decimalFmt = DecimalFormat("0.000", DecimalFormatSymbols(Locale.US))
        val minSilenceLengthInSeconds = decimalFmt.format(minSilenceLength.toDouble() / 1000.0)
        val filter = "silencedetect=noise=$thresholdDB:duration=$minSilenceLengthInSeconds"
        val command = arrayOf(binary, "-nostats", "-i", mediaPath, "-filter:a", filter, "-f", "null", "-")
        val commandline = StringUtils.join(command, " ")

        logger.info("Running {}", commandline)

        val pbuilder = ProcessBuilder(*command)
        val segmentsStrings = LinkedList<String>()
        val process = pbuilder.start()
        val reader = BufferedReader(InputStreamReader(process.errorStream))
        try {
            val lr = LineReader(reader)
            var line: String? = lr.readLine()
            while (null != line) {
                /* We want only lines from the silence detection filter */
                logger.debug("FFmpeg output: {}", line)
                if (line.startsWith("[silencedetect ")) {
                    segmentsStrings.add(line)
                }
                line = lr.readLine()
            }
        } catch (e: IOException) {
            logger.error("Error executing ffmpeg: {}", e.message)
        } finally {
            reader.close()
        }

        /**
         * Example output:
         * [silencedetect @ 0x2968e40] silence_start: 466.486
         * [silencedetect @ 0x2968e40] silence_end: 469.322 | silence_duration: 2.83592
         */

        val segmentsTmp = LinkedList<MediaSegment>()
        if (segmentsStrings.size == 0) {
            /* No silence found -> Add one segment for the whole track */
            logger.info("No silence found. Adding one large segment.")
            segmentsTmp.add(MediaSegment(0, track.duration!!))
        } else {
            var lastSilenceEnd: Long = 0
            var lastSilenceStart: Long = 0
            val patternStart = Pattern.compile("silence_start\\:\\ \\d+\\.\\d+")
            val patternEnd = Pattern.compile("silence_end\\:\\ \\d+\\.\\d+")
            for (seginfo in segmentsStrings) {
                /* Match silence ends */
                var matcher = patternEnd.matcher(seginfo)
                var time = ""
                while (matcher.find()) {
                    time = matcher.group().substring(13)
                }
                if ("" != time) {
                    val silenceEnd = (java.lang.Double.parseDouble(time) * 1000).toLong()
                    if (silenceEnd > lastSilenceEnd) {
                        logger.debug("Found silence end at {}", silenceEnd)
                        lastSilenceEnd = silenceEnd
                    }
                    continue
                }

                /* Match silence start -> End of segments */
                matcher = patternStart.matcher(seginfo)
                time = ""
                while (matcher.find()) {
                    time = matcher.group().substring(15)
                }
                if ("" != time) {
                    lastSilenceStart = (java.lang.Double.parseDouble(time) * 1000).toLong()
                    logger.debug("Found silence start at {}", lastSilenceStart)
                    if (lastSilenceStart - lastSilenceEnd > minVoiceLength) {
                        /* Found a valid segment */
                        val segmentStart = java.lang.Math.max(0, lastSilenceEnd - preSilenceLength)
                        logger.info("Adding segment from {} to {}", segmentStart, lastSilenceStart)
                        segmentsTmp.add(MediaSegment(segmentStart, lastSilenceStart))
                    }
                }
            }
            /* Add last segment if it is no silence and the segment is long enough */
            if (lastSilenceStart < lastSilenceEnd && track.duration!! - lastSilenceEnd > minVoiceLength) {
                val segmentStart = java.lang.Math.max(0, lastSilenceEnd - preSilenceLength)
                logger.info("Adding final segment from {} to {}", segmentStart, track.duration)
                segmentsTmp.add(MediaSegment(segmentStart, track.duration!!))
            }
        }

        logger.info("Segmentation of track {} yielded {} segments", trackId, segmentsTmp.size)
        segments = segmentsTmp

    }

    private fun parseLong(properties: Properties, key: String, defaultValue: Long): Long? {
        try {
            return java.lang.Long.parseLong(properties.getProperty(key, defaultValue.toString()))
        } catch (e: NumberFormatException) {
            logger.warn("Configuration value for {} is invalid, using default value of {} instead", key, defaultValue)
            return defaultValue
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger(FFmpegSilenceDetector::class.java)

        val FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path"
        val FFMPEG_BINARY_DEFAULT = "ffmpeg"

        private val DEFAULT_SILENCE_MIN_LENGTH = 5000L
        private val DEFAULT_SILENCE_PRE_LENGTH = 2000L
        private val DEFAULT_THRESHOLD_DB = "-40dB"
        private val DEFAULT_VOICE_MIN_LENGTH = 60000L

        private var binary = FFMPEG_BINARY_DEFAULT

        /**
         * Update FFMPEG binary path if set in configuration.
         *
         * @param bundleContext
         */
        fun init(bundleContext: BundleContext) {
            val binaryPath = bundleContext.getProperty(FFMPEG_BINARY_CONFIG)
            try {
                if (StringUtils.isNotBlank(binaryPath)) {
                    val binaryFile = File(StringUtils.trim(binaryPath))
                    if (binaryFile.exists()) {
                        binary = binaryFile.absolutePath
                    } else {
                        logger.warn("FFMPEG binary file {} does not exist", StringUtils.trim(binaryPath))
                    }
                }
            } catch (ex: Exception) {
                logger.error("Failed to set ffmpeg binary path", ex)
            }

        }
    }
}
