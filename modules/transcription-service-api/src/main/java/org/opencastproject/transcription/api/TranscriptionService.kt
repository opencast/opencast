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
package org.opencastproject.transcription.api

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.Track

/*
 * API for asynchronous external automated transcription service.
 * Supports starting a transcription job and getting a callback when transcription is done.
 */
interface TranscriptionService {

    /*
   * Returns a string representing the language supported.
   */
    val language: String

    /*
   * Start transcription job on external service
   */
    @Throws(TranscriptionServiceException::class)
    fun startTranscription(mpId: String, track: Track): Job

    /*
   * Get element containing transcription generated by external service
   */
    @Throws(TranscriptionServiceException::class)
    fun getGeneratedTranscription(mpId: String, jobId: String): MediaPackageElement

    /*
   * Called when external service finished transcription.
   */
    @Throws(TranscriptionServiceException::class)
    fun transcriptionDone(mpId: String, results: Any)

    /*
   * Called when external service reported an error in transcription.
   */
    @Throws(TranscriptionServiceException::class)
    fun transcriptionError(mpId: String, results: Any)
}
