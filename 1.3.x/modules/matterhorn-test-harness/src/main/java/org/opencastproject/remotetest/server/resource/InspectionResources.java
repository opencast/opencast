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
import org.apache.http.client.methods.HttpGet;

/**
 * Inspection REST resources
 * @author jamiehodge
 *
 */

public class InspectionResources {
  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/inspection/";
  }

  
  public static HttpResponse inspect(TrustedHttpClient client, String url) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "inspect?url=" + url));
  }
}
