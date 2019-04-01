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

import org.opencastproject.urlsigning.common.ResourceStrategy

import org.apache.commons.lang3.StringUtils

import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A [ResourceStrategy] that transforms URLs for a Wowza streaming server. Based upon:
 * http://www.wowza.com/forums/content.php?55-How-to-format-Adobe-Flash-RTMP-URLs and http://docs.aws.amazon.com/
 * AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-custom-policy.html
 */
class WowzaResourceStrategyImpl : ResourceStrategy {

    override fun getResource(baseUri: String): String {
        try {
            val uri = URI(baseUri)
            val scheme = uri.scheme
            return if (RTMP_SCHEME == scheme) {
                getRTMPResource(uri)
            } else {
                throw IllegalArgumentException(WowzaResourceStrategyImpl::class.java.simpleName
                        + " is unable to sign urls with scheme " + scheme)
            }
        } catch (e: URISyntaxException) {
            throw IllegalStateException(e)
        }

    }

    companion object {
        /** Regex pattern that matches something like mp4:path/to/resource/video  */
        private val FIND_STREAM_FORMAT = ".{3}[:].*$"
        /** The URI scheme that an RTMP address uses.  */
        private val RTMP_SCHEME = "rtmp"
        /** The possible delimiter between the server & application and the stream path and file.  */
        private val WOWZA_STREAM_DELIMITER = "_definst_"

        /**
         * Transform a base URI into a proper stream location without the host and application name.
         *
         * @param baseUri
         * The full URI to the resource including the host and application.
         * @return A safe standard RTMP resource location.
         */
        fun getRTMPResource(baseUri: URI): String {
            var stream: String? = null
            if (baseUri.toString().contains(WOWZA_STREAM_DELIMITER)) {
                // There is the explicit delimiter so return the stream as the resource.
                stream = baseUri.toString().split(WOWZA_STREAM_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                return if (stream[0] == '/') {
                    stream.substring(1)
                } else stream
            } else if (baseUri.path.contains(":")) {
                // The path contains a ":" denoting the type e.g. mp4 which is always the start of the stream path.
                val pattern = Pattern.compile(FIND_STREAM_FORMAT)
                val matcher = pattern.matcher(baseUri.path)
                if (matcher.find()) {
                    return baseUri.path.substring(matcher.start()) + if (StringUtils.isNotBlank(baseUri.query)) "?" + baseUri.query else ""
                }
            }
            // There are no special delimiters so assume the first value between two forward slashes (/.../) is the application.
            return baseUri.path.substring(baseUri.path.indexOf("/", 1) + 1) + if (StringUtils.isNotBlank(baseUri.query)) "?" + baseUri.query else ""
        }
    }

}
