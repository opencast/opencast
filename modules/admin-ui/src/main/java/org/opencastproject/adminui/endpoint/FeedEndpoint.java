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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@RestService(name = "FeedService", title = "Admin UI Feed Service",
  abstractText = "Provides Feed Information",
  notes = {"This service offers Feed information for the admin UI."})
public class FeedEndpoint extends RemoteBase {

  public FeedEndpoint() {
    super("org.opencastproject.feed.impl.FeedServiceImpl");
  }

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(FeedEndpoint.class);

  @GET
  @Path("/feeds")
  @Produces(MediaType.APPLICATION_JSON)
  public String listFeedServices() {

    HttpGet get = new HttpGet("/feeds");
    HttpResponse response = null;
    String result = null;

    try {
      response = getResponse(get);
      result = IOUtils.toString(response.getEntity().getContent(), "utf-8");
    } catch (Exception e) {
      logger.error("Could not get /feeds request in FeedEndpoint. " + e.toString());
    }

    return result;
  }

}
