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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.opencastproject.util.doc.rest.RestParameter.Type;

import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Tobira API Endpoint
 */
@Component(
    property = {
        "service.description=Tobira API Endpoint",
        "opencast.service.type=org.opencastproject.tobira",
        "opencast.service.path=/tobira",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = TobiraApi.class
)
@Path("")
@RestService(name = "TobiraApiEndpoint",
    title = "Tobira API Endpoint",
    abstractText = "Opencast Tobira API endpoint.",
    notes = { "This provides API endpoint used by Tobira to harvest media metadata" })
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
  @Path("/harvest")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "harvest",
      description = "Harvesting API to get incremental updates about series and events.",
      restParameters = {
          @RestParameter(
              name = "limit",
              isRequired = true,
              description = "The maximum number of items to return. Has to be positive.",
              type = Type.INTEGER
          ),
          @RestParameter(
              name = "since",
              isRequired = true,
              description = "Only return items that changed after or at this timestamp. "
                  + "Specified in milliseconds since 1970-01-01T00:00:00Z.",
              type = Type.INTEGER
          ),
      },
      responses = {
          @RestResponse(description = "Event and Series Data", responseCode = HttpServletResponse.SC_OK)
      },
      returnDescription = "Event and Series Data changed after the given timestamp"
  )
  public Response harvest(
      @QueryParam("limit") Integer limit,
      @QueryParam("since") Long since
  ) {
    // Parameter error handling
    if (since == null) {
      return badRequest("Required parameter 'since' not specified");
    }
    if (limit == null) {
      return badRequest("Required parameter 'limit' not specified");
    }
    if (since < 0) {
      return badRequest("Parameter 'since' < 0, but it has to be positive or 0");
    }
    if (limit <= 0) {
      return badRequest("Parameter 'limit' <= 0, but it has to be positive");
    }

    try {
      final String json = "{ \"limit\": " + limit +  ", \"since\": " + since + " }";
      return Response.ok()
          .type(APPLICATION_JSON_TYPE)
          // TODO: encoding
          .entity(json)
          .build();
    } catch (Exception e) {
      logger.error("Exception handling tobira/harvest: {}", e);
      return Response.serverError().build();
    }
  }

  private static Response badRequest(String msg) {
    logger.warn("Bad request to tobira/harvest: {}", msg);
    return Response.status(BAD_REQUEST).entity(msg).build();
  }
}
