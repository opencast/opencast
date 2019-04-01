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

import org.opencastproject.mediapackage.MediaPackage

import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException

/**
 * Log progress of a YouTube video upload.
 */
class UploadProgressListener
/**
 * @param mediaPackage may not be `null`
 * @param file may not be `null`
 */
(private val mediaPackage: MediaPackage, private val file: File) : MediaHttpUploaderProgressListener {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    var isComplete: Boolean = false
        private set

    init {
        isComplete = false
    }

    @Throws(IOException::class)
    override fun progressChanged(uploader: MediaHttpUploader) {
        val uploadState = uploader.uploadState
        val describeProgress: String
        when (uploadState) {
            MediaHttpUploader.UploadState.INITIATION_STARTED -> describeProgress = "Initiating YouTube publish"
            MediaHttpUploader.UploadState.INITIATION_COMPLETE, MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                val percentComplete = "%" + uploader.progress * 100 + " complete"
                describeProgress = "Uploading " + file.absolutePath + " to YouTube (" + percentComplete + ")"
            }
            MediaHttpUploader.UploadState.NOT_STARTED -> describeProgress = "Waiting to start YouTube."
            MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                describeProgress = "YouTube publication is complete."
                isComplete = true
            }
            else -> describeProgress = "Warning: No formal description for upload state: $uploadState"
        }
        logger.info(describeProgress + "(MediaPackage Identifier: " + mediaPackage.identifier.toString() + ')'.toString())
    }

}
