/*
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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.opencastproject.util.doc.rest.RestParameter.Type;

import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Tobira API Endpoint
 */
@Path("/tobira")
@RestService(
    name = "TobiraApiEndpoint",
    title = "Tobira API Endpoint",
    abstractText = "Opencast Tobira API endpoint.",
    notes = {
      "This provides API endpoint used by Tobira to harvest media metadata. "
              + "This API is specifically designed for Tobira and there are no "
              + "stability guarantees for this API beyond what Tobira needs. "
              + "Thus, you should not use this API for any other purposes!"
    }
)
@Component(
    property = {
        "service.description=Tobira-related APIs",
        "opencast.service.type=org.opencastproject.tobira",
        "opencast.service.path=/tobira",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = TobiraEndpoint.class
)
@JaxrsResource
public class TobiraEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(TobiraEndpoint.class);

  // Versioning the Tobira API:
  //
  // Since both Tobira and this API are changing over time, we need some mechanism for ensuring they
  // are compatible. We don't want to enforce a 1:1 thing, where a particular Tobira needs one
  // exact API as that makes the update process harder (especially once this module is included in
  // the community version). So instead we have some semver-like versioning here. Increase the
  // minor version for backwards-compatible changes and the major version for breaking changes.
  //
  // Note that we cannot use the Opencast version as some institutions might want to patch their
  // Opencast to include a newer Tobira module.
  //
  // So what's a breaking change and what not? For starters, the harvesting still needs to work with
  // all Tobira versions that it worked with previously. Since Tobira ignores unknown fields,
  // adding new JSON fields is a non-breaking change. You should also consider whether Tobira needs
  // to resynchronize, i.e. to get new data.
  private static final int VERSION_MAJOR = 1;
  private static final int VERSION_MINOR = 6;
  private static final String VERSION = VERSION_MAJOR + "." + VERSION_MINOR;

  private SearchService searchService;
  private SeriesService seriesService;
  private AuthorizationService authorizationService;
  private SecurityService securityService;
  private PlaylistService playlistService;
  private Workspace workspace;

  @Activate
  public void activate(BundleContext bundleContext) {
    logger.info("Activated Tobira API");
  }

  @Reference
  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  @Reference
  public void setSeriesService(SeriesService service) {
    this.seriesService = service;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService service) {
    this.authorizationService = service;
  }

  @Reference
  public void setSecurityService(SecurityService service) {
    this.securityService = service;
  }

  @Reference
  public void setPlaylistService(PlaylistService service) {
    this.playlistService = service;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @GET
  @Path("/version")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "version",
      description = "The Tobira Module API version",
      restParameters = {},
      responses = {
          @RestResponse(description = "Version information", responseCode = HttpServletResponse.SC_OK)
      },
      returnDescription = "JSON object with string field 'version'"
  )
  public Response version() {
    var body = Jsons.obj(Jsons.p("version", VERSION));
    return Response.ok(body.toJson()).build();
  }

  @GET
  @Path("/harvest")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "harvest",
      description = "Harvesting API to get incremental updates about series and events.",
      restParameters = {
          @RestParameter(
              name = "preferredAmount",
              isRequired = true,
              description = "A preferred number of items the request should return. This is "
                  + "merely a rough guideline and the API might return more or fewer items than "
                  + "this parameter. You cannot rely on an exact number of returned items! "
                  + "In practice this API usually returns between 0 and twice this parameter "
                  + "number of items.",
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
      @QueryParam("preferredAmount") Integer preferredAmount,
      @QueryParam("since") Long since
  ) {
    // Parameter error handling
    if (since == null) {
      return badRequest("Required parameter 'since' not specified");
    }
    if (preferredAmount == null) {
      return badRequest("Required parameter 'preferredAmount' not specified");
    }
    if (since < 0) {
      return badRequest("Parameter 'since' < 0, but it has to be positive or 0");
    }
    if (preferredAmount <= 0) {
      return badRequest("Parameter 'preferredAmount' <= 0, but it has to be positive");
    }

    logger.debug("Request to '/harvest' with preferredAmount={} since={}", preferredAmount, since);

    try {
      var json = Harvest.harvest(
          preferredAmount,
          new Date(since),
          searchService, seriesService, authorizationService, securityService, playlistService, workspace);

      // TODO: encoding
      return Response.ok(json.toJson()).build();
    } catch (Exception e) {
      logger.error("Unexpected exception in tobira/harvest", e);
      return Response.serverError().build();
    }
  }

  private static Response badRequest(String msg) {
    logger.warn("Bad request to tobira/harvest: {}", msg);
    return Response.status(BAD_REQUEST).entity(msg).build();
  }
}
