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
package org.opencastproject.distribution.streaming.wowza

import java.lang.String.format
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext

import java.net.URI
import java.net.URISyntaxException

class StreamingDistributionServiceTest {

    @Test
    @Throws(URISyntaxException::class)
    fun testInitializationException() {

        // Set up mocks for testing
        val bc = createNiceMock<BundleContext>(BundleContext::class.java)
        val cc = createNiceMock<ComponentContext>(ComponentContext::class.java)

        val inputStreamingUrls = arrayOf("incorrect url :")

        val inputAdaptiveStreamingUrls = arrayOf("another incorrect url :")

        // Try all combinations
        for (streamingUrl in inputStreamingUrls) {
            for (adaptiveStreamingUrl in inputAdaptiveStreamingUrls) {
                expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(streamingUrl)
                expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(adaptiveStreamingUrl)
            }
        }

        expect(cc.bundleContext).andReturn(bc).anyTimes()

        replay(bc)
        replay(cc)

        // Test service instance
        for (i in inputStreamingUrls.indices) {
            for (j in inputAdaptiveStreamingUrls.indices) {
                val sds = WowzaAdaptiveStreamingDistributionService()
                try {
                    sds.activate(cc)
                    fail(format(
                            "Error. The Streaming Distribution Service should not initialize when the streaming URL is \"%s\" " + "and the adaptive streaming URL is \"%s\"",
                            inputStreamingUrls[i], inputAdaptiveStreamingUrls[j]))

                } catch (e: IllegalArgumentException) {
                    // OK!
                }

            }
        }

    }

    @Test
    @Throws(URISyntaxException::class)
    fun testStreamingUrlSetUp() {

        // List of expected streaming URLs
        val outputStreamingUrls = arrayOf<String>("rtmp://noschema.myserver.com/my/path/to/server", null, null, "rtmp://withrtmp.test.ext/another/path", "rtmps://withrtmps.anothertest.test/path/to/server", null)

        // Set up mocks for testing
        val bc = createNiceMock<BundleContext>(BundleContext::class.java)
        val cc = createNiceMock<ComponentContext>(ComponentContext::class.java)

        for (url in inputStreamingUrls)
            expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(url)
        expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes()

        expect(cc.bundleContext).andReturn(bc).anyTimes()

        replay(bc)
        replay(cc)

        // Test service instance
        for (i in inputStreamingUrls.indices) {
            val sds = WowzaAdaptiveStreamingDistributionService()
            sds.activate(cc)
            if (outputStreamingUrls[i] == null)
                assertEquals(null, sds.streamingUri)
            else
                assertEquals(URI(outputStreamingUrls[i]), sds.streamingUri)
        }
    }

    @Test
    @Throws(URISyntaxException::class)
    fun testAdaptiveStreamingUrlSetUp() {

        // List of expected adaptive streaming URLs
        val outputAdaptiveStreamingUrls = arrayOf<String>("http://noschema.myserver.com/my/path/to/server", "http://withhttp.example.com/path", "https://withhttps.testing.com/this/is/a/path", null, null, null)

        // Set up mocks for testing
        val bc = createNiceMock<BundleContext>(BundleContext::class.java)
        val cc = createNiceMock<ComponentContext>(ComponentContext::class.java)

        expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes()
        for (url in inputStreamingUrls)
            expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(url)

        expect(cc.bundleContext).andReturn(bc).anyTimes()

        replay(bc)
        replay(cc)

        // Test service instance
        for (i in inputStreamingUrls.indices) {
            val sds = WowzaAdaptiveStreamingDistributionService()
            sds.activate(cc)
            if (outputAdaptiveStreamingUrls[i] == null)
                assertEquals(null, sds.adaptiveStreamingUri)
            else
                assertEquals(URI(outputAdaptiveStreamingUrls[i]), sds.adaptiveStreamingUri)
        }
    }

    companion object {

        // List of URLs for testing
        internal val inputStreamingUrls = arrayOf("noschema.myserver.com/my/path/to/server", "http://withhttp.example.com/path", "https://withhttps.testing.com/this/is/a/path", "rtmp://withrtmp.test.ext/another/path", "rtmps://withrtmps.anothertest.test/path/to/server", "other://withotherschema.test/mypath")
    }

    // Test port
}
