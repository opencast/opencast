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

import org.opencastproject.inspection.api.MediaInspectionOptions.OPTION_ACCURATE_FRAME_COUNT
import org.opencastproject.util.data.Collections.map

import org.opencastproject.inspection.api.MediaInspectionException
import org.opencastproject.inspection.ffmpeg.api.AudioStreamMetadata
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzer
import org.opencastproject.inspection.ffmpeg.api.MediaAnalyzerException
import org.opencastproject.inspection.ffmpeg.api.MediaContainerMetadata
import org.opencastproject.inspection.ffmpeg.api.VideoStreamMetadata
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Stream
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.UnsupportedElementException
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.util.Checksum
import org.opencastproject.util.ChecksumType
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.data.Tuple
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.BooleanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Dictionary
import java.util.Hashtable
import kotlin.collections.Map.Entry

/**
 * Contains the business logic for media inspection. Its primary purpose is to decouple the inspection logic from all
 * OSGi/MH job management boilerplate.
 */
class MediaInspector(private val workspace: Workspace, private val ffprobePath: String) {

    /**
     * Inspects the element that is passed in as uri.
     *
     * @param trackURI
     * the element uri
     * @return the inspected track
     * @throws org.opencastproject.inspection.api.MediaInspectionException
     * if inspection fails
     */
    @Throws(MediaInspectionException::class)
    fun inspectTrack(trackURI: URI, options: Map<String, String>): Track {
        logger.debug("inspect($trackURI) called, using workspace $workspace")
        throwExceptionIfInvalid(options)

        try {
            // Get the file from the URL (runtime exception if invalid)
            var file: File? = null
            try {
                file = workspace.get(trackURI)
            } catch (notFound: NotFoundException) {
                throw MediaInspectionException("Unable to find resource $trackURI", notFound)
            } catch (ioe: IOException) {
                throw MediaInspectionException("Error reading $trackURI from workspace", ioe)
            }

            // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
            // TODO: Try to guess the extension from the container's metadata
            if ("" == FilenameUtils.getExtension(file!!.name)) {
                throw MediaInspectionException("Can not inspect files without a filename extension")
            }

            val metadata = getFileMetadata(file, getAccurateFrameCount(options))
            if (metadata == null) {
                throw MediaInspectionException("Media analyzer returned no metadata from $file")
            } else {
                val elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                val track: TrackImpl
                val element: MediaPackageElement
                try {
                    element = elementBuilder.elementFromURI(trackURI, MediaPackageElement.Type.Track, null!!)
                } catch (e: UnsupportedElementException) {
                    throw MediaInspectionException("Unable to create track element from $file", e)
                }

                track = element as TrackImpl

                // Duration
                if (metadata.duration != null && metadata.duration > 0)
                    track.duration = metadata.duration

                // Checksum
                try {
                    track.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, file)
                } catch (e: IOException) {
                    throw MediaInspectionException("Unable to read $file", e)
                }

                // Mimetype
                var mimeType = MimeTypes.fromString(file.path)

                // The mimetype library doesn't know about audio/video metadata, so the type might be wrong.
                if ("audio" == mimeType.type && metadata.hasVideoStreamMetadata()) {
                    mimeType = MimeTypes.parseMimeType("video/" + mimeType.subtype)
                } else if ("video" == mimeType.type && !metadata.hasVideoStreamMetadata()) {
                    mimeType = MimeTypes.parseMimeType("audio/" + mimeType.subtype)
                }
                track.mimeType = mimeType

                // Audio metadata
                try {
                    addAudioStreamMetadata(track, metadata)
                } catch (e: Exception) {
                    throw MediaInspectionException("Unable to extract audio metadata from $file", e)
                }

                // Videometadata
                try {
                    addVideoStreamMetadata(track, metadata)
                } catch (e: Exception) {
                    throw MediaInspectionException("Unable to extract video metadata from $file", e)
                }

                return track
            }
        } catch (e: Exception) {
            logger.warn("Error inspecting $trackURI", e)
            if (e is MediaInspectionException) {
                throw e
            } else {
                throw MediaInspectionException(e)
            }
        }

    }

    /**
     * Enriches the given element's mediapackage.
     *
     * @param element
     * the element to enrich
     * @param override
     * `true` to override existing metadata
     * @return the enriched element
     * @throws MediaInspectionException
     * if enriching fails
     */
    @Throws(MediaInspectionException::class)
    fun enrich(element: MediaPackageElement, override: Boolean, options: Map<String, String>): MediaPackageElement {
        throwExceptionIfInvalid(options)
        return (element as? Track)?.let { enrichTrack(it, override, options) }
                ?: enrichElement(element, override, options)
    }

    /**
     * Enriches the track's metadata and can be executed in an asynchronous way.
     *
     * @param originalTrack
     * the original track
     * @param override
     * `true` to override existing metadata
     * @return the media package element
     * @throws MediaInspectionException
     */
    @Throws(MediaInspectionException::class)
    private fun enrichTrack(originalTrack: Track, override: Boolean, options: Map<String, String>): MediaPackageElement {
        try {
            val originalTrackUrl = originalTrack.getURI()
            val flavor = originalTrack.flavor
            logger.debug("enrich($originalTrackUrl) called")

            // Get the file from the URL
            var file: File? = null
            try {
                file = workspace.get(originalTrackUrl)
            } catch (e: NotFoundException) {
                throw MediaInspectionException("File " + originalTrackUrl + " was not found and can therefore not be "
                        + "inspected", e)
            } catch (e: IOException) {
                throw MediaInspectionException("Error accessing $originalTrackUrl", e)
            }

            // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
            // TODO: Try to guess the extension from the container's metadata
            if ("" == FilenameUtils.getExtension(file!!.name)) {
                throw MediaInspectionException("Can not inspect files without a filename extension")
            }

            val metadata = getFileMetadata(file, getAccurateFrameCount(options))
            if (metadata == null) {
                throw MediaInspectionException("Unable to acquire media metadata for $originalTrackUrl")
            } else {
                var track: TrackImpl? = null
                try {
                    track = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                            .elementFromURI(originalTrackUrl, MediaPackageElement.Type.Track, flavor) as TrackImpl
                } catch (e: UnsupportedElementException) {
                    throw MediaInspectionException("Unable to create track element from $file", e)
                }

                // init the new track with old
                track.checksum = originalTrack.checksum
                track.duration = originalTrack.duration
                track.elementDescription = originalTrack.elementDescription
                track.flavor = flavor
                track.identifier = originalTrack.identifier
                track.mimeType = originalTrack.mimeType
                track.reference = originalTrack.reference
                track.setSize(file.length())
                track.setURI(originalTrackUrl)
                for (tag in originalTrack.tags) {
                    track.addTag(tag)
                }

                // enrich the new track with basic info
                if (track.duration == null || override)
                    track.duration = metadata.duration
                if (track.checksum == null || override) {
                    try {
                        track.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, file)
                    } catch (e: IOException) {
                        throw MediaInspectionException("Unable to read $file", e)
                    }

                }

                // Add the mime type if it's not already present
                if (track.mimeType == null || override) {
                    try {
                        var mimeType = MimeTypes.fromURI(track.getURI())

                        // The mimetype library doesn't know about audio/video metadata, so the type might be wrong.
                        if ("audio" == mimeType.type && metadata.hasVideoStreamMetadata()) {
                            mimeType = MimeTypes.parseMimeType("video/" + mimeType.subtype)
                        } else if ("video" == mimeType.type && !metadata.hasVideoStreamMetadata()) {
                            mimeType = MimeTypes.parseMimeType("audio/" + mimeType.subtype)
                        }
                        track.mimeType = mimeType
                    } catch (e: UnknownFileTypeException) {
                        logger.info("Unable to detect the mimetype for track {} at {}", track.identifier, track.getURI())
                    }

                }

                // find all streams
                val streamsId2Stream = Hashtable<String, Stream>()
                for (stream in originalTrack.streams) {
                    streamsId2Stream[stream.identifier] = stream
                }

                // audio list
                try {
                    addAudioStreamMetadata(track, metadata)
                } catch (e: Exception) {
                    throw MediaInspectionException("Unable to extract audio metadata from $file", e)
                }

                // video list
                try {
                    addVideoStreamMetadata(track, metadata)
                } catch (e: Exception) {
                    throw MediaInspectionException("Unable to extract video metadata from $file", e)
                }

                logger.info("Successfully inspected track {}", track)
                return track
            }
        } catch (e: Exception) {
            logger.warn("Error enriching track $originalTrack", e)
            if (e is MediaInspectionException) {
                throw e
            } else {
                throw MediaInspectionException(e)
            }
        }

    }

    /**
     * Enriches the media package element metadata such as the mime type, the file size etc. The method mutates the
     * argument element.
     *
     * @param element
     * the media package element
     * @param override
     * `true` to overwrite existing metadata
     * @return the enriched element
     * @throws MediaInspectionException
     * if enriching fails
     */
    @Throws(MediaInspectionException::class)
    private fun enrichElement(element: MediaPackageElement, override: Boolean,
                              options: Map<String, String>): MediaPackageElement {
        try {
            val file: File
            try {
                file = workspace.get(element.getURI())
            } catch (e: NotFoundException) {
                throw MediaInspectionException("Unable to find " + element.getURI() + " in the workspace", e)
            } catch (e: IOException) {
                throw MediaInspectionException("Error accessing " + element.getURI() + " in the workspace", e)
            }

            // Checksum
            if (element.checksum == null || override) {
                try {
                    element.checksum = Checksum.create(ChecksumType.DEFAULT_TYPE, file)
                } catch (e: IOException) {
                    throw MediaInspectionException("Error generating checksum for " + element.getURI(), e)
                }

            }

            // Mimetype
            if (element.mimeType == null || override) {
                try {
                    element.mimeType = MimeTypes.fromString(file.path)
                } catch (e: UnknownFileTypeException) {
                    logger.info("unable to determine the mime type for {}", file.name)
                }

            }

            logger.info("Successfully inspected element {}", element)

            return element
        } catch (e: Exception) {
            logger.warn("Error enriching element $element", e)
            if (e is MediaInspectionException) {
                throw e
            } else {
                throw MediaInspectionException(e)
            }
        }

    }

    /**
     * Asks the media analyzer to extract the file's metadata.
     *
     * @param file
     * the file
     * @return the file container metadata
     * @throws MediaInspectionException
     * if metadata extraction fails
     */
    @Throws(MediaInspectionException::class)
    private fun getFileMetadata(file: File?, accurateFrameCount: Boolean): MediaContainerMetadata? {
        if (file == null)
            throw IllegalArgumentException("file to analyze cannot be null")
        try {
            val analyzer = FFmpegAnalyzer(accurateFrameCount)
            analyzer.setConfig(map(Tuple.tuple<String, Any>(FFmpegAnalyzer.FFPROBE_BINARY_CONFIG, ffprobePath)))
            return analyzer.analyze(file)
        } catch (e: MediaAnalyzerException) {
            throw MediaInspectionException(e)
        }

    }

    /**
     * Adds the video related metadata to the track.
     *
     * @param track
     * the track
     * @param metadata
     * the container metadata
     * @throws Exception
     * Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
     * media's metadata
     */
    @Throws(Exception::class)
    private fun addVideoStreamMetadata(track: TrackImpl, metadata: MediaContainerMetadata): Track {
        val videoList = metadata.videoStreamMetadata
        if (videoList != null && !videoList.isEmpty()) {
            for (i in videoList.indices) {
                val video = VideoStreamImpl("video-" + (i + 1))
                val v = videoList[i]
                video.bitRate = v.bitRate
                video.format = v.format
                video.formatVersion = v.formatVersion
                video.frameCount = v.frames
                video.setFrameHeight(v.frameHeight)
                video.frameRate = v.frameRate
                video.setFrameWidth(v.frameWidth)
                video.scanOrder = v.scanOrder
                video.setScanType(v.scanType)
                // TODO: retain the original video metadata
                track.addStream(video)
            }
        }
        return track
    }

    /**
     * Adds the audio related metadata to the track.
     *
     * @param track
     * the track
     * @param metadata
     * the container metadata
     * @throws Exception
     * Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
     * media's metadata
     */
    @Throws(Exception::class)
    private fun addAudioStreamMetadata(track: TrackImpl, metadata: MediaContainerMetadata): Track {
        val audioList = metadata.audioStreamMetadata
        if (audioList != null && !audioList.isEmpty()) {
            for (i in audioList.indices) {
                val audio = AudioStreamImpl("audio-" + (i + 1))
                val a = audioList[i]
                audio.bitRate = a.bitRate
                audio.channels = a.channels
                audio.format = a.format
                audio.formatVersion = a.formatVersion
                audio.frameCount = a.frames
                audio.bitDepth = a.resolution
                audio.samplingRate = a.samplingRate
                // TODO: retain the original audio metadata
                track.addStream(audio)
            }
        }
        return track
    }

    /* Return true if OPTION_ACCURATE_FRAME_COUNT is set to true, false otherwise */
    private fun getAccurateFrameCount(options: Map<String, String>): Boolean {
        return BooleanUtils.toBoolean(options[OPTION_ACCURATE_FRAME_COUNT])
    }

    /* Throws an exception if an unsupported option is set */
    @Throws(MediaInspectionException::class)
    private fun throwExceptionIfInvalid(options: Map<String, String>?) {
        if (options != null) {
            for ((key) in options) {
                if (key == OPTION_ACCURATE_FRAME_COUNT) {
                    // This option is supported
                } else {
                    throw MediaInspectionException("Unsupported option $key")
                }
            }
        } else {
            throw MediaInspectionException("Options must not be null")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MediaInspector::class.java)
    }
}
