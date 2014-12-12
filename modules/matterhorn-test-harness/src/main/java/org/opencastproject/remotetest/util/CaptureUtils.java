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
package org.opencastproject.remotetest.util;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;

/**
 * Utility class that deals with capture-related data.
 */
public final class CaptureUtils {

  /**
   * This utility class is not meant to be instantiated.
   */
  private CaptureUtils() {
    // Nothing to do here
  }

  /**
   * Returns <code>true</code> if the capture agent with id <code>{@link #CAPTURE_AGENT_ID}</code> is online.
   *
   * @param agent
   *          the agent identifier
   * @return <code>true</code> if the agent is online
   */
  public static boolean isOnline(String agent) {
    HttpGet request = new HttpGet(BASE_URL + "/capture-admin/agents/" + agent);
    return HttpStatus.SC_OK == Main.getClient().execute(request).getStatusLine().getStatusCode();
  }

  /**
   * Returns <code>true</code> if the capture agent with id <code>captureAgentId</code> is currently capturing. If the
   * agent is not online, an {@link IllegalStateException} is thrown.
   *
   * @param captureAgentId
   *          the capture agent
   * @return <code>true</code> if the agent is capturing
   * @throws IllegalStateException
   *           if the agent is not online
   * @throws Exception
   *           if the response can't be parsed
   */
  public static boolean isCapturing(String captureAgentId) throws IllegalStateException, Exception {
    HttpGet request = new HttpGet(BASE_URL + "/capture-admin/agents/" + captureAgentId);
    HttpResponse response = Main.getClient().execute(request);
    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode())
      throw new IllegalStateException("Capture agent '" + captureAgentId + "' is unexpectedly offline");
    String responseBody = EntityUtils.toString(response.getEntity());
    return "capturing".equalsIgnoreCase((String) Utils.xpath(responseBody, "/*[local-name() = 'state']",
            XPathConstants.STRING));
  }

  public static boolean recordingExists(String recordingId) throws Exception {
    HttpGet request = new HttpGet(BASE_URL + "/capture-admin/recordings/" + recordingId);
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(request);
    return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
  }

  public static boolean isInState(String recordingId, String state) throws Exception {
    HttpGet request = new HttpGet(BASE_URL + "/capture-admin/recordings/" + recordingId);
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(request);
    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode())
      throw new IllegalStateException("Recording '" + recordingId + "' not found");
    String responseBody = EntityUtils.toString(response.getEntity());
    Main.returnClient(client);
    return state.equalsIgnoreCase((String) Utils.xpath(responseBody, "//*[local-name() = 'state']",
            XPathConstants.STRING));
  }

  public static void setState(String recordingId, String state) throws Exception {
    HttpPost request = new HttpPost(BASE_URL + "/capture-admin/recordings/" + recordingId);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("state", state));
    request.setEntity(new UrlEncodedFormEntity(params));
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(request);
    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode())
      throw new IllegalStateException("Recording '" + recordingId + "' not found");
    Main.returnClient(client);
  }

}
