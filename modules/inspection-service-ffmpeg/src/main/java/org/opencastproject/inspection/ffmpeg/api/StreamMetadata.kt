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

/**
 * Common metadata for all kind of embedded media streams.
 */
open class StreamMetadata : TemporalMetadata() {

    var alignment: String

    var delay: Long? = null

    var frames: Long? = null

    var compressionRatio: Float? = null

    var captureDevice: String
    var captureDeviceVersion: String
    var captureDeviceVendor: String
    var captureDeviceURL: String
    var captureDeviceInfo: String

    // bits
    /** Returns the resolution in bits. Usually one of 8, 16, 20 or 24.  */
    /** Sets the resolution in bits. Usually one of 8, 16, 20 or 24.  */
    var resolution: Int? = null
}
