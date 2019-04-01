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

import java.util.Arrays
import java.util.HashSet

/**
 * This `MediaPackageElementSelector` selects tracks from a `MediaPackage` that contain audio
 * stream.
 */
class AudioElementSelector : AbstractMediaPackageElementSelector<Track> {

    /** Explicit audio flavor  */
    protected var audioFlavor: MediaPackageElementFlavor? = null

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
            return
        }
        setAudioFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
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
     * Returns the explicit audio flavor or `null` if none was specified.
     *
     * @return the audio flavor
     */
    fun getAudioFlavor(): MediaPackageElementFlavor? {
        return audioFlavor
    }

    /**
     * Returns a track or a number of tracks from the media package that together contain audio and video. If no such
     * combination can be found, e. g. there is no audio or video at all, an empty array is returned.
     *
     * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<Track> {
        // instead of relying on the broken superclass, we'll inspect every track
        // Collection<Track> candidates = super.select(mediaPackage);
        val candidates = Arrays.asList(*mediaPackage.tracks)
        val result = HashSet<Track>()

        var foundAudio = false

        // Look for a track containing audio
        for (t in candidates) {
            if (t.hasAudio() && !foundAudio && (audioFlavor == null || audioFlavor == t.flavor)) {
                result.add(t)
                foundAudio = true
            }
        }
        return result
    }

}
