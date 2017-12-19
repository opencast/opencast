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

import org.opencastproject.urlsigning.common.ResourceStrategy;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link ResourceStrategy} that transforms URLs for a Wowza streaming server. Based upon:
 * http://www.wowza.com/forums/content.php?55-How-to-format-Adobe-Flash-RTMP-URLs and http://docs.aws.amazon.com/
 * AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html
 */
public class WowzaResourceStrategyImpl implements ResourceStrategy {
  /** Regex pattern that matches something like mp4:path/to/resource/video */
  private static final String FIND_STREAM_FORMAT = ".{3}[:].*$";
  /** The URI scheme that an RTMP address uses. */
  private static final String RTMP_SCHEME = "rtmp";
  /** The possible delimiter between the server & application and the stream path and file. */
  private static final String WOWZA_STREAM_DELIMITER = "_definst_";

  @Override
  public String getResource(String baseUri) {
    try {
      URI uri = new URI(baseUri);
      String scheme = uri.getScheme();
      if (RTMP_SCHEME.equals(scheme)) {
        return getRTMPResource(uri);
      } else {
        throw new IllegalArgumentException(WowzaResourceStrategyImpl.class.getSimpleName()
                + " is unable to sign urls with scheme " + scheme);
      }
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Transform a base URI into a proper stream location without the host and application name.
   *
   * @param baseUri
   *          The full URI to the resource including the host and application.
   * @return A safe standard RTMP resource location.
   */
  protected static String getRTMPResource(URI baseUri) {
    String stream = null;
    if (baseUri.toString().contains(WOWZA_STREAM_DELIMITER)) {
      // There is the explicit delimiter so return the stream as the resource.
      stream = baseUri.toString().split(WOWZA_STREAM_DELIMITER)[1];
      if (stream.charAt(0) == '/') {
        return stream.substring(1);
      }
      return stream;
    } else if (baseUri.getPath().contains(":")) {
      // The path contains a ":" denoting the type e.g. mp4 which is always the start of the stream path.
      Pattern pattern = Pattern.compile(FIND_STREAM_FORMAT);
      Matcher matcher = pattern.matcher(baseUri.getPath());
      if (matcher.find()) {
        return baseUri.getPath().substring(matcher.start())
                + (StringUtils.isNotBlank(baseUri.getQuery()) ? "?" + baseUri.getQuery() : "");
      }
    }
    // There are no special delimiters so assume the first value between two forward slashes (/.../) is the application.
    return baseUri.getPath().substring(baseUri.getPath().indexOf("/", 1) + 1)
            + (StringUtils.isNotBlank(baseUri.getQuery()) ? "?" + baseUri.getQuery() : "");
  }

}
