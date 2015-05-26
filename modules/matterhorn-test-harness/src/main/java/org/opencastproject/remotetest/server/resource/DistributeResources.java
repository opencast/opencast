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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Distribute REST resources
 *
 * @author jamiehodge
 *
 */

public class DistributeResources {
  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/distribution/";
  }

  /**
   *
   * @param channel
   *          Distribution channel: local, youtube, itunesu
   *
   */
  public static HttpResponse distribute(TrustedHttpClient client, String channel, String mediapackage, String... elementId) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + channel.toLowerCase() + "/");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackage));
    for (String id : elementId) {
      params.add(new BasicNameValuePair("elementId", id));
    }
    post.setEntity(new UrlEncodedFormEntity(params));

    return client.execute(post);
  }
}
