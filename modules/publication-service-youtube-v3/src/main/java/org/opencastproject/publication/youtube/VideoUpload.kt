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

package org.opencastproject.publication.youtube

import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener

import java.io.File

/**
 * Represents a YouTube video.
 *
 * @see com.google.api.services.youtube.model.Video
 */
class VideoUpload
/**
 * @param title may not be `null`.
 * @param description may be `null`.
 * @param privacyStatus may not be `null`.
 * @param videoFile may not be `null`.
 * @param progressListener may be `null`.
 * @param tags may be `null`.
 */
(
        /**
         * The video's title.
         * The value will not be `null`.
         */
        val title: String,
        /**
         * The video's description.
         * The value may be `null`.
         */
        val description: String,
        /**
         * @see com.google.api.services.youtube.model.VideoStatus.setPrivacyStatus
         * @return will not be `null`
         */
        val privacyStatus: String,
        /**
         * @see com.google.api.services.youtube.model.Video
         *
         * @return will not be `null`
         */
        val videoFile: File,
        /**
         * Real-time updates of upload status.
         * @return may be `null`
         */
        val progressListener: MediaHttpUploaderProgressListener, vararg tags: String) {
    /**
     * @see com.google.api.services.youtube.model.VideoSnippet.getTags
     * @return may be `null`
     */
    val tags: Array<String>

    init {
        this.tags = tags
    }
}
