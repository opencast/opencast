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
package org.opencastproject.remotetest.capture;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Test the functionality of the capture endpoints. Does not assume capture
 * devices are connected, so only mock captures are tested
 */
@Ignore
public class CaptureRestEndpointTest {

  private TrustedHttpClient httpClient;
  private ArrayList<NameValuePair> startParams = new ArrayList<NameValuePair>();
  private ArrayList<NameValuePair> stopParams = new ArrayList<NameValuePair>();

  @Before
  public void setUp() throws Exception {
    httpClient = Main.getClient();
    String time = String.valueOf(System.currentTimeMillis());

    // Test Properties from resources
    Properties props = new Properties();
    props.put("capture.recording.id", time);

    StringWriter writer = new StringWriter();
    props.store(writer, null);
    startParams.add(new BasicNameValuePair("config", writer.toString()));

    stopParams = new ArrayList<NameValuePair>();
    stopParams.add(new BasicNameValuePair("recordingID", time));

  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(httpClient);
  }

  @Test
  public void testUnscheduledCapture() throws Exception {
    //Test using only unscheduled calls
    sendGet(BASE_URL + "/capture/startCapture", HttpStatus.SC_OK);
    Thread.sleep(1000);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_OK);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testUnscheduledMix() throws Exception {
    //Test using both scheduled and unscheduled calls
    sendGet(BASE_URL + "/capture/startCapture", HttpStatus.SC_OK);
    Thread.sleep(1000);
    sendPost(BASE_URL + "/capture/stopCapture", stopParams, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_OK);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testCapturePost() throws Exception {
    //Test using only scheduled calls
    sendPost(BASE_URL + "/capture/startCapture", startParams, HttpStatus.SC_OK);
    Thread.sleep(1000);
    sendPost(BASE_URL + "/capture/stopCapture", stopParams, HttpStatus.SC_OK);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testCaptureMix() throws Exception {
    //Test using both scheduled and unscheduled calls
    sendPost(BASE_URL + "/capture/startCapture", startParams, HttpStatus.SC_OK);
    Thread.sleep(1000);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_OK);
    sendPost(BASE_URL + "/capture/stopCapture", stopParams, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    sendGet(BASE_URL + "/capture/stopCapture", HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  private void sendGet(String URL, int returnCode) throws Exception {
    HttpGet get = new HttpGet(URL);
    int getResponse = httpClient.execute(get).getStatusLine().getStatusCode();
    Assert.assertTrue(getResponse == returnCode);
    get.abort();
  }

  private void sendPost(String URL, List<NameValuePair> params, int returnCode) throws Exception {
    HttpPost post = new HttpPost(URL);
    post.setEntity(new UrlEncodedFormEntity(params));
    int postResponse = httpClient.execute(post).getStatusLine().getStatusCode();
    Assert.assertTrue(postResponse == returnCode);
    post.abort();
  }

  @Test
  public void printTestingInfo() {
    System.out.println("\n\nInstructions to test capture agent\n==================================");
    System.out.println("The capture agents can be tested by setting the appropriate\n" +
        "properties for the JVM. They properties right now are: testHauppauge, " +
        "testBt878, testAlsa.\n" +
        "Each property should be assigned to its location instead of setting a boolean.\n" +
        "An example test could be: mvn test -DargLine=\"-DtestHauppauge=/dev/video0 -DtestAlsa=hw:0\"\n\n");
  }


}
