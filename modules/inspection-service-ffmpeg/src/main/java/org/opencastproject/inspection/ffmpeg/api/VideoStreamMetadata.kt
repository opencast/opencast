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

import org.opencastproject.mediapackage.track.Colorimetry
import org.opencastproject.mediapackage.track.FrameRateMode
import org.opencastproject.mediapackage.track.ScanOrder
import org.opencastproject.mediapackage.track.ScanType

/**
 * This class bundles technical information about a video stream.
 */
class VideoStreamMetadata : StreamMetadata() {

    var formatSettingsBVOP: String
    var formatSettingsCABAC: String
    var formatSettingsQPel: String
    var formatSettingsGMC: String
    var formatSettingsMatrix: String
    var formatSettingsRefFrames: String
    var formatSettingsPulldown: String

    /**
     * Returns the frame width in pixels.
     */
    /**
     * Sets the frame width in pixels.
     */
    var frameWidth: Int? = null
    /**
     * Returns the frame height in pixels.
     */
    /**
     * Sets the frame height in pixels.
     */
    var frameHeight: Int? = null
    /**
     * Gets the pixel aspect ratio.
     */
    var pixelAspectRatio: Float? = null
    var displayAspectRatio: Float? = null

    /**
     * Returns the frame rate in frames per second.
     */
    /**
     * Sets the frame rate in frames per second.
     */
    var frameRate: Float? = null
    var frameRateMinimum: Float? = null
    var frameRateMaximum: Float? = null
    var frameRateMode: FrameRateMode

    var frameCount: Long? = null

    // PAL, NTSC
    var videoStandard: String

    // bits / (pixel * frame)
    var qualityFactor: Float? = null

    var scanType: ScanType
    var scanOrder: ScanOrder

    var colorimetry: Colorimetry
}
