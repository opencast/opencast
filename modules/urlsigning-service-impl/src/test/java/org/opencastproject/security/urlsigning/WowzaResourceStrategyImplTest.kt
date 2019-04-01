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
package org.opencastproject.security.urlsigning

import org.junit.Assert.assertEquals

import org.junit.Test

import java.net.URI
import java.net.URISyntaxException

class WowzaResourceStrategyImplTest {
    @Test
    @Throws(URISyntaxException::class)
    fun testGetRTMPSource() {
        /**
         * With the explicit Wowza delimiter.
         */
        // With explicit seperation between application and stream.
        val noPathDelimiter = URI("rtmp://hostname.com/matterhorn-engage/_definst_/mp4:myvideo.mov")
        var result = WowzaResourceStrategyImpl.getRTMPResource(noPathDelimiter)
        assertEquals("mp4:myvideo.mov", result)

        // With path, delimiter, no extension
        val pathDelimiterNoExtension = URI(
                "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo")
        result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterNoExtension)
        assertEquals("mp4:path/to/video/myvideo", result)

        // With path, delimiter, extension
        val pathDelimiterExtension = URI(
                "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo.mov")
        result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterExtension)
        assertEquals("mp4:path/to/video/myvideo.mov", result)

        // With path, delimiter, extension and query string
        val pathDelimiterExtensionQueryString = URI(
                "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo.mov?param1=value1&param2=value2")
        result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterExtensionQueryString)
        assertEquals("mp4:path/to/video/myvideo.mov?param1=value1&param2=value2", result)

        /**
         * With the format mp4: and no Wowza delimiter.
         */
        // No Path, extension and format.
        val noPathNoDelimiterFormat = URI("rtmp://hostname.com/matterhorn-engage/mp4:myvideo.mov")
        result = WowzaResourceStrategyImpl.getRTMPResource(noPathNoDelimiterFormat)
        assertEquals("mp4:myvideo.mov", result)

        // With path, no delimiter, no extension and format
        val noPathNoDelimiterNoExtensionFormat = URI(
                "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo")
        result = WowzaResourceStrategyImpl.getRTMPResource(noPathNoDelimiterNoExtensionFormat)
        assertEquals("mp4:path/to/video/myvideo", result)

        // With path, no delimiter, extension and format
        val pathNoDelimiterExtensionFormat = URI(
                "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo.mov")
        result = WowzaResourceStrategyImpl.getRTMPResource(pathNoDelimiterExtensionFormat)
        assertEquals("mp4:path/to/video/myvideo.mov", result)

        // With path, no delimiter, extension, format and query string
        val pathNoDelimiterExtensionFormatQueryString = URI(
                "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo.mov?param1=value1&param2=value2")
        result = WowzaResourceStrategyImpl.getRTMPResource(pathNoDelimiterExtensionFormatQueryString)
        assertEquals("mp4:path/to/video/myvideo.mov?param1=value1&param2=value2", result)

        // FLV with extension
        val flvWithExtension = URI(
                "rtmp://hostname.com/matterhorn-engage/flv:path/to/video/myvideo.flv?param1=value1&param2=value2")
        result = WowzaResourceStrategyImpl.getRTMPResource(flvWithExtension)
        assertEquals("flv:path/to/video/myvideo.flv?param1=value1&param2=value2", result)

        // FLV without extension
        val flvWithoutExtension = URI(
                "rtmp://hostname.com/matterhorn-engage/flv:path/to/video/myvideo?param1=value1&param2=value2")
        result = WowzaResourceStrategyImpl.getRTMPResource(flvWithoutExtension)
        assertEquals("flv:path/to/video/myvideo?param1=value1&param2=value2", result)

        // Without format
        val withoutFormat = URI(
                "rtmp://hostname.com/matterhorn-engage/path/to/video/myvideo.mp4?param1=value1&param2=value2")
        result = WowzaResourceStrategyImpl.getRTMPResource(withoutFormat)
        assertEquals("path/to/video/myvideo.mp4?param1=value1&param2=value2", result)
    }
}
