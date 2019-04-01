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

package org.opencastproject.mediapackage.selector

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import java.util.HashSet

/**
 * This `MediaPackageElementSelector` selects a combination of tracks from a `MediaPackage` that
 * contain audio and video stream.
 */
class AudioVisualElementSelector : AbstractMediaPackageElementSelector<Track> {

    /** Explicit video flavor  */
    protected var videoFlavor: MediaPackageElementFlavor? = null

    /** Explicit audio flavor  */
    protected var audioFlavor: MediaPackageElementFlavor? = null

    /** The resulting audio track  */
    /**
     * Returns the audio track that has been selected by a call to [.select], which might be
     * `null` if no audio is required or available.
     *
     * @return the audio track
     */
    var audioTrack: Track? = null
        protected set

    /** The resulting video track  */
    /**
     * Returns the video track that has been selected by a call to [.select], which might be
     * `null` if no video is required or available.
     *
     * @return the video track
     */
    var videoTrack: Track? = null
        protected set

    /** Flag to indicate whether an audio track is required  */
    protected var requireAudio = false

    /** Flag to indicate whether a video track is required  */
    protected var requireVideo = false

    /**
     * Creates a new selector.
     */
    constructor() {}

    /**
     * Creates a new selector that will restrict the result of `select()` to the given flavor.
     *
     * @param flavor
     * the flavor
     */
    constructor(flavor: String) : this(MediaPackageElementFlavor.parseFlavor(flavor)) {}

    /**
     * Creates a new selector that will restrict the result of `select()` to the given flavor.
     *
     * @param flavor
     * the flavor
     */
    constructor(flavor: MediaPackageElementFlavor) {
        addFlavor(flavor)
    }

    /**
     * Specifies an explicit audio flavor.
     *
     * @param flavor
     * the flavor
     */
    fun setAudioFlavor(flavor: String?) {
        if (flavor == null) {
            audioFlavor = null
        } else {
            setAudioFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
    }

    /**
     * Specifies an explicit audio flavor.
     *
     * @param flavor
     * the flavor
     */
    fun setAudioFlavor(flavor: MediaPackageElementFlavor?) {
        if (flavor != null)
            addFlavor(flavor)
        audioFlavor = flavor
    }

    /**
     * If set to `true`, this selector requires an audio track to be part of the result set, or a video track
     * containing at least one audio stream.
     *
     * @param require
     * `true` to require an audio track
     */
    fun setRequireAudioTrack(require: Boolean) {
        requireAudio = require
    }

    /**
     * If set to `true`, this selector requires a video track to be part of the result set.
     *
     * @param require
     * `true` to require a video track
     */
    fun setRequireVideoTrack(require: Boolean) {
        requireVideo = require
    }

    /**
     * Returns the explicit audio flavor or `null` if none was specified.
     *
     * @return the audio flavor
     */
    fun getAudioFlavor(): MediaPackageElementFlavor? {
        return audioFlavor
    }

    /**
     * Specifies an explicit video flavor.
     *
     * @param flavor
     * the flavor
     */
    fun setVideoFlavor(flavor: String?) {
        if (flavor == null) {
            videoFlavor = null
        } else {
            setVideoFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }
    }

    /**
     * Specifies an explicit video flavor.
     *
     * @param flavor
     * the flavor
     */
    fun setVideoFlavor(flavor: MediaPackageElementFlavor?) {
        if (flavor != null)
            addFlavor(flavor)
        videoFlavor = flavor
    }

    /**
     * Returns the explicit video flavor or `null` if none was specified.
     *
     * @return the video flavor
     */
    fun getVideoFlavor(): MediaPackageElementFlavor? {
        return videoFlavor
    }

    /**
     * Returns a track or a number of tracks from the media package that together contain audio and video. If no such
     * combination can be found, e. g. there is no audio or video at all, an empty array is returned.
     *
     * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<Track> {
        val candidates = super.select(mediaPackage, withTagsAndFlavors)
        val result = HashSet<Track>()

        var foundAudio = false
        var foundVideo = false

        // Try to look for the perfect match: a track containing audio and video
        for (t in candidates) {
            if (audioFlavor == null && videoFlavor == null) {
                foundAudio = foundAudio or t.hasAudio()
                foundVideo = foundVideo or t.hasVideo()
                result.add(t)
            } else {
                if (audioFlavor != null && t.hasAudio() && audioFlavor!!.matches(t.flavor)
                        && !(videoFlavor == null && t.hasVideo())) {
                    foundAudio = true
                    audioTrack = t
                    result.add(t)
                }
                if (videoFlavor != null && t.hasVideo() && videoFlavor!!.matches(t.flavor)
                        && !(audioFlavor == null && t.hasAudio())) {
                    foundVideo = true
                    videoTrack = t
                    result.add(t)
                }
            }
            if ((foundAudio || audioFlavor == null) && (foundVideo || videoFlavor == null))
                break
        }

        if (!foundAudio && requireAudio || !foundVideo && requireVideo)
            result.clear()

        // We were lucky, a combination was found!
        return result
    }

}
