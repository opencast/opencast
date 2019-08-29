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
package org.opencastproject.distribution.streaming.wowza;

import static java.lang.String.format;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;

import java.net.URI;
import java.net.URISyntaxException;

public class StreamingDistributionServiceTest {

  // List of URLs for testing
  private static final String[] inputStreamingUrls = new String[] {
          "noschema.myserver.com/my/path/to/server",
          "http://withhttp.example.com/path",
          "https://withhttps.testing.com/this/is/a/path",
          "rtmp://withrtmp.test.ext/another/path",
          "rtmps://withrtmps.anothertest.test/path/to/server",
          "other://withotherschema.test/mypath"
  };

  @Test
  public void testInitializationException() {

    // Set up mocks for testing
    BundleContext bundleContext = createNiceMock(BundleContext.class);

    final String[] inputStreamingUrls = new String[] {
            "incorrect url :"
    };

    final String[] inputAdaptiveStreamingUrls = new String[] {
            "another incorrect url :"
    };

    // Try all combinations
    for (String streamingUrl : inputStreamingUrls) {
      for (String adaptiveStreamingUrl : inputAdaptiveStreamingUrls) {
        expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(streamingUrl);
        expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(adaptiveStreamingUrl);
      }
    }

    replay(bundleContext);

    // Test service instance
    for (String inputStreamingUrl : inputStreamingUrls) {
      for (String inputAdaptiveStreamingUrl : inputAdaptiveStreamingUrls) {
        WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
        try {
          sds.activate(bundleContext);
          fail(format("Error. The Streaming Distribution Service should not initialize when the streaming URL is "
                  + "\"%s\" and the adaptive streaming URL is \"%s\"", inputStreamingUrl, inputAdaptiveStreamingUrl));

        } catch (ComponentException e) {
          // OK!
        }
      }
    }

  }

  @Test
  public void testStreamingUrlSetUp() throws URISyntaxException {

    // List of expected streaming URLs
    final String[] outputStreamingUrls = new String[] {
            "rtmp://noschema.myserver.com/my/path/to/server",
            null,
            null,
            "rtmp://withrtmp.test.ext/another/path",
            "rtmps://withrtmps.anothertest.test/path/to/server",
            null
    };

    // Set up mocks for testing
    BundleContext bundleContext = createNiceMock(BundleContext.class);

    for (String url : inputStreamingUrls)
      expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(url);
    expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes();
    expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_DIRECTORY_KEY)).andReturn("/").anyTimes();

    replay(bundleContext);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {
      WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
      if (outputStreamingUrls[i] == null) {
        try {
          sds.activate(bundleContext);
          fail("Should fail on invalid streaming URL");
        } catch (ComponentException e) {
          // this is expected
        }
      } else {
        sds.activate(bundleContext);
        assertEquals(new URI(outputStreamingUrls[i]), sds.streamingUri);
      }
    }
  }

  @Test
  public void testAdaptiveStreamingUrlSetUp() throws URISyntaxException {

    // List of expected adaptive streaming URLs
    final String[] outputAdaptiveStreamingUrls = new String[] {
            "http://noschema.myserver.com/my/path/to/server",
            "http://withhttp.example.com/path",
            "https://withhttps.testing.com/this/is/a/path",
            null,
            null,
            null
    };

    // Set up mocks for testing
    BundleContext bundleContext = createNiceMock(BundleContext.class);

    expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes();
    for (String url : inputStreamingUrls)
      expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(url);
    expect(bundleContext.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_DIRECTORY_KEY)).andReturn("/").anyTimes();

    replay(bundleContext);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {
      WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
      if (outputAdaptiveStreamingUrls[i] == null) {
        try {
          sds.activate(bundleContext);
          fail("Should fail on invalid streaming URL");
        } catch (ComponentException e) {
          // expected
        }
        assertNull(sds.adaptiveStreamingUri);
      } else {
        sds.activate(bundleContext);
        assertEquals(new URI(outputAdaptiveStreamingUrls[i]), sds.adaptiveStreamingUri);
      }
    }
  }
}
