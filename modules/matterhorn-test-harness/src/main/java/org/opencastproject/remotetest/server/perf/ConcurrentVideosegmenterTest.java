/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.remotetest.server.perf;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs many concurrent workflows
 */
public class ConcurrentVideosegmenterTest {
  @Rule
  public ContiPerfRule i = new ContiPerfRule();

  TrustedHttpClient httpClient;
  String trackXml = "<track id=\"track-1\"><mimetype>video/quicktime</mimetype><tags/><url>https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-analysis-videosegmenter/src/test/resources/scene-change.mov</url><checksum type=\"md5\">89b99cf1efe6614e35b1a765b519f56d</checksum><duration>20000</duration><video id=\"video-1\"><device/><encoder type=\"M-JPEG\"/><bitrate>187396.0</bitrate><framerate>0.95</framerate><resolution>320x240</resolution></video></track>";

  @Before
  public void setUp() throws Exception {
    httpClient = Main.getClient();

    // We run this once in the startup so we don't try to download the same file 10 times at once
    httpClient.execute(getPost());


  }

  protected HttpPost getPost() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/vsegmenter/analyze");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("track", trackXml));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    return postEncode;
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(httpClient);
  }

  @Test
  @PerfTest(invocations=10, threads=10)
  public void testConcurrentVideoSegmentation() throws Exception {
    // Grab the receipt from the response
    HttpResponse postResponse = httpClient.execute(getPost());
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    Assert.assertTrue(postResponseXml.contains("receipt"));
  }
}
