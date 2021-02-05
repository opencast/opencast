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

package org.opencastproject.tobira.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Tobira API Endpoint
 */
@Component(
    property = {
        "service.description=Tobira Api Endpoint",
        "opencast.service.type=org.opencastproject.tobira",
        "opencast.service.path=/tobira",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = TobiraApi.class
)
@Path("")
@RestService(name = "TobiraApiEndpoint",
    title = "Tobira Api Endpoint",
    abstractText = "Opencast Tobira Api endpoint.",
    notes = { "This provides API endpoint used by Tobira to harvest media metadata"})
public class TobiraApi {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TobiraApi.class);

  /** The service */
  private ServiceRegistry serviceRegistry;
  private OrganizationDirectoryService organizationDirectoryService;

  @Activate
  public void activate(BundleContext bundleContext) {
    logger.info("Activated Tobira API");
  }

  @GET
  @Path("/")
  @Produces(APPLICATION_JSON)
  @RestQuery(name = "metrics",
      description = "Metrics about Opencast",
      responses = {@RestResponse(description = "Metrics", responseCode = HttpServletResponse.SC_OK)},
      returnDescription = "OpenMetrics about Opencast.")
  public Response metrics() throws Exception {
    try (InputStream in = TobiraApi.class.getResourceAsStream("result.json")) {
      final String json = IOUtils.toString(in, StandardCharsets.UTF_8);
      return Response.ok().entity(json).build();
    } catch (Exception e) {
      return  Response.serverError().build();
    }
  }

}
