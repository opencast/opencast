/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.remotetest.server.resource;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler REST resources
 * @author jamiehodge
 *
 */

public class SchedulerResources {
  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/scheduler/";
  }

  public static HttpResponse addEvent(TrustedHttpClient client, String event) throws Exception {
    HttpPut put = new HttpPut(getServiceUrl() + "event");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("event", event));
    put.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(put);
  }

  public static HttpResponse updateEvent(TrustedHttpClient client, String event) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "updateEvent");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("event", event));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static HttpResponse getEvent(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getEvent/" + id));
  }

  public static HttpResponse getDublinCoreMetadata(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getDublinCoreMetadata/" + id));
  }

  public static HttpResponse findConflictingEvents(TrustedHttpClient client, String event) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "findConflictingEvents");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("event", event));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static HttpResponse removeEvent(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "removeEvent/" + id));
  }

  public static HttpResponse getEvents(TrustedHttpClient client) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getEvents"));
  }

  public static HttpResponse getUpcomingEvents(TrustedHttpClient client) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getUpcomingEvents"));
  }

  public static HttpResponse getCalendarForCaptureAgent(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getCalendarForCaptureAgent/" + id));
  }

  public static HttpResponse getCaptureAgentMetadata(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "getCaptureAgentMetadata/" + id));
  }
}
