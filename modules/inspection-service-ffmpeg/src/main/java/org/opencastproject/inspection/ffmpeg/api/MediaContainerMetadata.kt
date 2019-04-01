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


package org.opencastproject.inspection.ffmpeg.api


import java.util.ArrayList

/**
 * Encapsulates technical metadata of media containers, usually media files.
 *
 *
 * Each property may return null, which means that it could not be determined.
 */
class MediaContainerMetadata : TemporalMetadata() {

    /**
     * Returns metadata for all contained video streams.
     *
     * @return the metadata or an empty list
     */
    val videoStreamMetadata: List<VideoStreamMetadata> = ArrayList()
    /**
     * Returns metadata for all contained audio streams.
     *
     * @return the metadata or an empty list
     */
    val audioStreamMetadata: List<AudioStreamMetadata> = ArrayList()

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the file name, e.g. `metropolis.mov`
     */
    var fileName: String? = null
    /**
     * Returns the file extension, e.g. `mov`
     */
    var fileExtension: String? = null
    /**
     * Checks if contained audio and video streams are multiplexed.
     */
    var isInterleaved: Boolean? = null

    /**
     * Checks if any video metadata is present.
     */
    fun hasVideoStreamMetadata(): Boolean {
        return videoStreamMetadata.size > 0
    }

    /**
     * Checks if any audio metadata is present.
     */
    fun hasAudioStreamMetadata(): Boolean {
        return audioStreamMetadata.size > 0
    }

}
