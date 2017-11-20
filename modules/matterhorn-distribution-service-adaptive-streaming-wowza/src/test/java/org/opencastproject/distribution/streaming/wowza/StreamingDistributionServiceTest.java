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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.net.URI;
import java.net.URISyntaxException;

public class StreamingDistributionServiceTest {

  // List of URLs for testing
  static final String[] inputStreamingUrls = new String[] {
          "noschema.myserver.com/my/path/to/server",
          "http://withhttp.example.com/path",
          "https://withhttps.testing.com/this/is/a/path",
          "rtmp://withrtmp.test.ext/another/path",
          "rtmps://withrtmps.anothertest.test/path/to/server",
          "other://withotherschema.test/mypath"
  };

  @Test
  public void testInitializationException() throws URISyntaxException {

    // Set up mocks for testing
    BundleContext bc = createNiceMock(BundleContext.class);
    ComponentContext cc = createNiceMock(ComponentContext.class);

    final String[] inputStreamingUrls = new String[] {
            "incorrect url :"
    };

    final String[] inputAdaptiveStreamingUrls = new String[] {
            "another incorrect url :"
    };

    // Try all combinations
    for (String streamingUrl : inputStreamingUrls) {
      for (String adaptiveStreamingUrl : inputAdaptiveStreamingUrls) {
        expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(streamingUrl);
        expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(adaptiveStreamingUrl);
      }
    }

    expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    replay(bc);
    replay(cc);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {
      for (int j = 0; j < inputAdaptiveStreamingUrls.length; j++) {
        WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
        try {
          sds.activate(cc);
          fail(format(
                  "Error. The Streaming Distribution Service should not initialize when the streaming URL is \"%s\" "
                          + "and the adaptive streaming URL is \"%s\"",
                  inputStreamingUrls[i], inputAdaptiveStreamingUrls[j]));

        } catch (IllegalArgumentException e) {
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
    BundleContext bc = createNiceMock(BundleContext.class);
    ComponentContext cc = createNiceMock(ComponentContext.class);

    for (String url : inputStreamingUrls)
      expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn(url);
    expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes();

    expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    replay(bc);
    replay(cc);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {
      WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
      sds.activate(cc);
      if (outputStreamingUrls[i] == null)
        assertEquals(null, sds.streamingUri);
      else
        assertEquals(new URI(outputStreamingUrls[i]), sds.streamingUri);
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
    BundleContext bc = createNiceMock(BundleContext.class);
    ComponentContext cc = createNiceMock(ComponentContext.class);

    expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.STREAMING_URL_KEY)).andReturn("a/valid/url").anyTimes();
    for (String url : inputStreamingUrls)
      expect(bc.getProperty(WowzaAdaptiveStreamingDistributionService.ADAPTIVE_STREAMING_URL_KEY)).andReturn(url);

    expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    replay(bc);
    replay(cc);

    // Test service instance
    for (int i = 0; i < inputStreamingUrls.length; i++) {
      WowzaAdaptiveStreamingDistributionService sds = new WowzaAdaptiveStreamingDistributionService();
      sds.activate(cc);
      if (outputAdaptiveStreamingUrls[i] == null)
        assertEquals(null, sds.adaptiveStreamingUri);
      else
        assertEquals(new URI(outputAdaptiveStreamingUrls[i]), sds.adaptiveStreamingUri);
    }
  }

  // Test port
}
