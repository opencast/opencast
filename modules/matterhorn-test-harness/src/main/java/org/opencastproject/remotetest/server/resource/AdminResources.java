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
package org.opencastproject.remotetest.server.resource;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

/**
 * Admin REST resources
 */

public class AdminResources {
  public static HttpResponse recordingsInactive(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/inactive"));
  }

  public static HttpResponse recordingsUpcoming(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/upcoming"));
  }

  public static HttpResponse recordingsCapturing(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/capturing"));
  }

  public static HttpResponse recordingsProcessing(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/capturing"));
  }

  public static HttpResponse recordingsFinished(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/finished"));
  }

  public static HttpResponse recordingsErrors(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "recordings/errors"));
  }

  public static HttpResponse countRecordings(TrustedHttpClient client) {
    return client.execute(new HttpGet(getServiceUrl() + "countRecordings"));
  }

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/admin/";
  }

}
