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
package org.opencastproject.scheduler.api

import java.util.Arrays

/**
 * A representation of a recording's current state (MH-1475).
 */
interface RecordingState {
    companion object {

        /** Constant `UNKNOWN="unknown"`  */
        val UNKNOWN = "unknown"

        /** Constant `CAPTURING="capturing"`  */
        val CAPTURING = "capturing"

        /** Constant `CAPTURE_FINISHED="capture_finished"`  */
        val CAPTURE_FINISHED = "capture_finished"

        /** Constant `CAPTURE_ERROR="capture_error"`  */
        val CAPTURE_ERROR = "capture_error"

        /** Constant `MANIFEST="manifest"`  */
        val MANIFEST = "manifest"

        /** Constant `MANIFEST_ERROR="manifest_error"`  */
        val MANIFEST_ERROR = "manifest_error"

        /** Constant `MANIFEST_FINISHED="manifest_finished"`  */
        val MANIFEST_FINISHED = "manifest_finished"

        /** Constant `COMPRESSING="compressing"`  */
        val COMPRESSING = "compressing"

        /** Constant `COMPRESSING_ERROR="compressing_error"`  */
        val COMPRESSING_ERROR = "compressing_error"

        /** Constant `UPLOADING="uploading"`  */
        val UPLOADING = "uploading"

        /** Constant `UPLOAD_FINISHED="upload_finished"`  */
        val UPLOAD_FINISHED = "upload_finished"

        /** Constant `UPLOAD_ERROR="upload_error"`  */
        val UPLOAD_ERROR = "upload_error"

        /** The collection of all known states. TODO: Remove this when the states are replaced with enums  */
        val KNOWN_STATES = Arrays.asList(*arrayOf(UNKNOWN, CAPTURING, CAPTURE_FINISHED, CAPTURE_ERROR, MANIFEST, MANIFEST_ERROR, MANIFEST_FINISHED, COMPRESSING, COMPRESSING_ERROR, UPLOADING, UPLOAD_FINISHED, UPLOAD_ERROR))

        /** Some of the known states should not be delivered to the workflow service  */
        val WORKFLOW_IGNORE_STATES = Arrays.asList(*arrayOf(UPLOADING, UPLOAD_FINISHED, UPLOAD_ERROR))
    }

}
