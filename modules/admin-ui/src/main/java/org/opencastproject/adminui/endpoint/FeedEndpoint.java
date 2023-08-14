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

import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.http.client.methods.HttpGet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "FeedService", title = "Admin UI Feed Service",
  abstractText = "Provides Feed Information",
  notes = {"This service offers Feed information for the admin UI."})
@Component(
  immediate = true,
  service = FeedEndpoint.class,
  property = {
    "service.description=Admin UI - Feed Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.FeedEndpoint",
    "opencast.service.path=/admin-ng/feeds"
  }
)
public class FeedEndpoint extends RemoteBase {

  public FeedEndpoint() {
    super("org.opencastproject.feed.impl.FeedServiceImpl");
  }

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(FeedEndpoint.class);

  @GET
  @Path("/feeds")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "feeds",
      description = "List available series based feeds retrieved from the search service",
      returnDescription = "Return list of feeds",
      responses = {
          @RestResponse(
              responseCode = SC_OK,
              description = "List of available feeds returned.")
      })
  public Response listFeedServices() {

    HttpGet get = new HttpGet("/feeds");
    try {
      InputStream response = getResponse(get).getEntity().getContent();
      return Response.ok(response).build();
    } catch (Exception e) {
      logger.error("Error requesting data from feeds endpoint", e);
      return Response.serverError().build();
    }
  }

  @Reference
  public void setTrustedHttpClient(final TrustedHttpClient client) {
    this.client = client;
  }

  @Reference
  public void setRemoteServiceManager(final ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

}
