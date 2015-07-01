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

package org.opencastproject.remotetest.server.resource;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capture REST resources
 */

public class CaptureResources {
  public static String AGENT = "demo_capture_agent";

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/capture/";
  }

  public static HttpResponse startCaptureGet(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "startCapture"));
  }

  public static HttpResponse stopCapture(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "stopCapture"));
  }

  public static HttpResponse startCapturePost(TrustedHttpClient client) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "startCapture");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("config", captureProperties()));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static HttpResponse stopCapturePost(TrustedHttpClient client, String id) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "stopCapture");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("recordingID", id));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static String captureProperties() throws Exception {
    return IOUtils.toString(CaptureResources.class.getClassLoader().getResourceAsStream("capture.properties"), "UTF-8");
  }

  public static String captureId(HttpResponse response) throws Exception {
    String pattern = "Unscheduled-\\d+";
    Matcher matcher = Pattern.compile(pattern).matcher(EntityUtils.toString(response.getEntity(), "UTF-8"));
    matcher.find();
    return matcher.group();
  }
}
