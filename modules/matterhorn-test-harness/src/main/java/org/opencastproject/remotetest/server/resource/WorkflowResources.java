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

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow REST resources
 * @author jamiehodge
 *
 */

public class WorkflowResources {
  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/search/";
  }

  /**
   *
   * @param format Response format: xml or json
   *
   */
  public static HttpResponse definitions(TrustedHttpClient client, String format) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "definitions." + format.toLowerCase()));
  }

  /**
   *
   * @param format Response format: xml or json
   *
   */
  public static HttpResponse instances(TrustedHttpClient client, String format) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "instances." + format.toLowerCase()));
  }

  /**
   *
   * @param id Workflow instance ID
   * @param format Response format: xml or json
   *
   */
  public static HttpResponse instance(TrustedHttpClient client, String id, String format) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "instance/" + id + format.toLowerCase()));
  }

  public static HttpResponse start(TrustedHttpClient client, String mediapackage,
      String workflowDefinition, String properties) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "start");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackage));
    params.add(new BasicNameValuePair("definition", workflowDefinition));
    params.add(new BasicNameValuePair("properties", properties));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static HttpResponse suspend(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "suspend/" + id));
  }

  public static HttpResponse resume(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "resume/" + id));
  }

  public static HttpResponse stop(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "stop/" + id));
  }
}
