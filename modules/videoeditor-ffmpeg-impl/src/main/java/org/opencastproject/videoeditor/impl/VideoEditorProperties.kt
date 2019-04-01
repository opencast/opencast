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

package org.opencastproject.videoeditor.impl

/**
 * VideoEditorService properties that can be used to modify default processing values.
 */
interface VideoEditorProperties {
    companion object {

        /** audio encoder codec  */
        val AUDIO_CODEC = "audio.codec"

        /** video codec  */
        val VIDEO_CODEC = "video.codec"

        /** Custom output file extension  */
        val OUTPUT_FILE_EXTENSION = "outputfile.extension"
        val FFMPEG_PROPERTIES = "ffmpeg.properties"
        val FFMPEG_PRESET = "ffmpeg.preset"
        val FFMPEG_SCALE_FILTER = "ffmpeg.scalefilter"
        val AUDIO_FADE = "audio.fade"
        val VIDEO_FADE = "video.fade"
        val DEFAULT_EXTENSION = ".mp4"
    }

}
