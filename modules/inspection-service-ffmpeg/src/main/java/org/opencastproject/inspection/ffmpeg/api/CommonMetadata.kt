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

import java.net.URL
import java.util.Date
import java.util.Locale

/**
 * Common metadata for all kinds of media objects.
 */
open class CommonMetadata {

    var format: String
    var formatInfo: String
    var formatURL: URL
    var formatVersion: String
    var formatProfile: String
    var formatSettingsSummary: String

    var encoderApplication: String
    var encoderApplicationURL: URL
    var encoderApplicationVendor: URL
    var encoderLibrary: String
    var encoderLibraryURL: URL
    var encoderLibraryInfo: String
    var encoderLibraryVersion: String
    var encoderLibraryVendor: String
    var encoderLibraryReleaseDate: String
    var encoderLibrarySettings: String

    var isEncrypted: Boolean? = null

    var encodedDate: Date
    var taggedDate: Date

    /**
     * Returns the title of the media object.
     */
    var title: String

    /**
     * Returns the locale of the media object, usually only the language.
     */
    var language: Locale

    // bytes
    var size: Long? = null

}
