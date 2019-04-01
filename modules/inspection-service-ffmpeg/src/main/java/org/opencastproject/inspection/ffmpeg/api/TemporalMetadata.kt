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

import org.opencastproject.mediapackage.track.BitRateMode

/**
 * Common metadata for all kind of temporal media.
 */
abstract class TemporalMetadata : CommonMetadata() {

    // ms
    var duration: Long? = null

    var bitRateMode: BitRateMode
    // b/s
    /** Returns the bit rate in bits per second.  */
    var bitRate: Float? = null
    /** Returns the maximum bit rate in bits per second.  */
    var bitRateMinimum: Float? = null
    var bitRateMaximum: Float? = null
    /**
     * Returns the nominal bit rate in bits per second.
     */
    var bitRateNominal: Float? = null
}
