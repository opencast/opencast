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
 * This `MediaPackageElementSelector` selects tracks from a `MediaPackage` that contain video
 * streams.
 */
class VideoElementSelector : AbstractMediaPackageElementSelector<Track> {

    /** Explicit video flavor  */
    protected var videoFlavor: MediaPackageElementFlavor? = null

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
     * Specifies an explicit video flavor.
     *
     * @param flavor
     * the flavor
     */
    fun setVideoFlavor(flavor: String?) {
        if (flavor == null) {
            videoFlavor = null
            return
        }
        setVideoFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
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
     * Returns a track or a number of tracks from the media package that together contain video and video. If no such
     * combination can be found, e. g. there is no video or video at all, an empty array is returned.
     *
     * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<Track> {
        // instead of relying on the broken superclass, we'll inspect every track
        // Collection<Track> candidates = super.select(mediaPackage);
        val result = HashSet<Track>()

        var foundVideo = false

        // Look for a track containing video
        for (t in mediaPackage.tracks) {
            if (t.hasVideo() && !foundVideo && (videoFlavor == null || videoFlavor == t.flavor)) {
                result.add(t)
                foundVideo = true
            }
        }
        return result
    }

}
