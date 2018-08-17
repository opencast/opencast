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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.security.urlsigning;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class WowzaResourceStrategyImplTest {
  @Test
  public void testGetRTMPSource() throws URISyntaxException {
    /**
     * With the explicit Wowza delimiter.
     */
    // With explicit seperation between application and stream.
    URI noPathDelimiter = new URI("rtmp://hostname.com/matterhorn-engage/_definst_/mp4:myvideo.mov");
    String result = WowzaResourceStrategyImpl.getRTMPResource(noPathDelimiter);
    assertEquals("mp4:myvideo.mov", result);

    // With path, delimiter, no extension
    URI pathDelimiterNoExtension = new URI(
            "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo");
    result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterNoExtension);
    assertEquals("mp4:path/to/video/myvideo", result);

    // With path, delimiter, extension
    URI pathDelimiterExtension = new URI(
            "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo.mov");
    result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterExtension);
    assertEquals("mp4:path/to/video/myvideo.mov", result);

    // With path, delimiter, extension and query string
    URI pathDelimiterExtensionQueryString = new URI(
            "rtmp://hostname.com/matterhorn-engage/_definst_/mp4:path/to/video/myvideo.mov?param1=value1&param2=value2");
    result = WowzaResourceStrategyImpl.getRTMPResource(pathDelimiterExtensionQueryString);
    assertEquals("mp4:path/to/video/myvideo.mov?param1=value1&param2=value2", result);

    /**
     * With the format mp4: and no Wowza delimiter.
     */
    // No Path, extension and format.
    URI noPathNoDelimiterFormat = new URI("rtmp://hostname.com/matterhorn-engage/mp4:myvideo.mov");
    result = WowzaResourceStrategyImpl.getRTMPResource(noPathNoDelimiterFormat);
    assertEquals("mp4:myvideo.mov", result);

    // With path, no delimiter, no extension and format
    URI noPathNoDelimiterNoExtensionFormat = new URI(
            "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo");
    result = WowzaResourceStrategyImpl.getRTMPResource(noPathNoDelimiterNoExtensionFormat);
    assertEquals("mp4:path/to/video/myvideo", result);

    // With path, no delimiter, extension and format
    URI pathNoDelimiterExtensionFormat = new URI(
            "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo.mov");
    result = WowzaResourceStrategyImpl.getRTMPResource(pathNoDelimiterExtensionFormat);
    assertEquals("mp4:path/to/video/myvideo.mov", result);

    // With path, no delimiter, extension, format and query string
    URI pathNoDelimiterExtensionFormatQueryString = new URI(
            "rtmp://hostname.com/matterhorn-engage/mp4:path/to/video/myvideo.mov?param1=value1&param2=value2");
    result = WowzaResourceStrategyImpl.getRTMPResource(pathNoDelimiterExtensionFormatQueryString);
    assertEquals("mp4:path/to/video/myvideo.mov?param1=value1&param2=value2", result);

    // FLV with extension
    URI flvWithExtension = new URI(
            "rtmp://hostname.com/matterhorn-engage/flv:path/to/video/myvideo.flv?param1=value1&param2=value2");
    result = WowzaResourceStrategyImpl.getRTMPResource(flvWithExtension);
    assertEquals("flv:path/to/video/myvideo.flv?param1=value1&param2=value2", result);

    // FLV without extension
    URI flvWithoutExtension = new URI(
            "rtmp://hostname.com/matterhorn-engage/flv:path/to/video/myvideo?param1=value1&param2=value2");
    result = WowzaResourceStrategyImpl.getRTMPResource(flvWithoutExtension);
    assertEquals("flv:path/to/video/myvideo?param1=value1&param2=value2", result);

    // Without format
    URI withoutFormat = new URI(
            "rtmp://hostname.com/matterhorn-engage/path/to/video/myvideo.mp4?param1=value1&param2=value2");
    result = WowzaResourceStrategyImpl.getRTMPResource(withoutFormat);
    assertEquals("path/to/video/myvideo.mp4?param1=value1&param2=value2", result);
  }
}
