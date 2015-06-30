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
import org.apache.http.client.methods.HttpGet;

/**
 * Capture Admin REST resources
 */

public class CaptureAdminResources {

  public static HttpResponse agents(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "agents"));
  }

  public static HttpResponse agent(TrustedHttpClient client, String id) {
    return client.execute(new HttpGet(getServiceUrl() + "agents/" + id));
  }

  public static HttpResponse recordings(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings"));
  }

  public static HttpResponse recording(TrustedHttpClient client, String id) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/" + id));
  }

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/capture-admin/";
  }
}
