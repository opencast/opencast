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


package org.opencastproject.mediapackage

import org.opencastproject.mediapackage.track.ScanOrder
import org.opencastproject.mediapackage.track.ScanType
import org.opencastproject.mediapackage.track.VideoStreamImpl

import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * A stream containing video data.
 */
@XmlJavaTypeAdapter(VideoStream.Adapter::class)
interface VideoStream : Stream {

    val bitRate: Float?

    val frameRate: Float?

    val frameWidth: Int?

    val frameHeight: Int?

    val scanType: ScanType

    val scanOrder: ScanOrder

    val captureDevice: String

    val captureDeviceVersion: String

    val captureDeviceVendor: String

    val format: String

    val formatVersion: String

    val encoderLibraryVendor: String

    class Adapter : XmlAdapter<VideoStreamImpl, Stream>() {
        @Throws(Exception::class)
        override fun marshal(v: Stream): VideoStreamImpl {
            return v as VideoStreamImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(v: VideoStreamImpl): Stream {
            return v
        }
    }
}
