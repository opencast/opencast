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

import org.opencastproject.adopter.registration.AdopterRegistrationExtra;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.UserIdRoleProvider;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
@Designate(ocd = TobiraConfig.class)
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

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final Gson gson = new Gson();

  private SearchService searchService;
  private SeriesService seriesService;
  private AuthorizationService authorizationService;
  private SecurityService securityService;
  private PlaylistService playlistService;
  private Workspace workspace;
  private UserDirectoryService userDirectoryService;
  private UserIdRoleProvider userIdRoleProvider;

  private String callbackToken;
  private Predicate<String> allowedRolesPattern;
  private String headerName;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  private JsonObject cachedStats = new JsonObject();

  @Activate
  public void activate(TobiraConfig tobiraConfig, BundleContext bundleContext) {
    logger.info("Activated Tobira API");
    callbackToken = tobiraConfig.callbackToken();
    headerName = tobiraConfig.headerName();
    allowedRolesPattern = Pattern.compile(tobiraConfig.allowedRolesPattern()).asMatchPredicate();
    this.db = dbSessionFactory.createSession(emf);
  }

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.adopter)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
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

  @Reference
  public void setUserDirectoryService(UserDirectoryService service) {
    this.userDirectoryService = service;
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

  @GET
  @Path("/callback/{token}")
  @Produces(APPLICATION_JSON)
  @RestQuery(
      name = "callback",
      description = "Auth callback API to get user information. This is used by Tobira to get user information.",
      pathParameters = {
          @RestParameter(
              name = "token",
              isRequired = true,
              description = "The token to authorize the request.",
              type = Type.STRING
          )
      },
      responses = {
          @RestResponse(description = "User Outcome Data", responseCode = HttpServletResponse.SC_OK)
      },
      returnDescription = "Returns user information"
  )
  public Response callback(@PathParam("token") String token, @Context HttpHeaders headers) {

    if (callbackToken == null || !callbackToken.equals(token)) {
      return badRequest("Invalid token or callback disabled");
    }

    List<String> username = headers.getRequestHeader(headerName);

    if (username == null) {
      return badRequest("No username header provided");
    }

    User user = null;

    if (!username.isEmpty() && username.get(0) != null) {
      user = userDirectoryService.loadUser(username.get(0));
    }

    Jsons.Obj outcome;

    if (user == null) {
      outcome = Jsons.obj(
        Jsons.p("outcome", "no-user")
      );
    } else {
      outcome = Jsons.obj(
          Jsons.p("outcome", "user"),
          Jsons.p("username", user.getUsername()),
          Jsons.p("displayName", user.getName()),
          Jsons.p("email", user.getEmail()),
          Jsons.p("userRole",UserIdRoleProvider.getUserIdRole(user.getUsername())),
          Jsons.p("roles", Jsons.arr(
            user.getRoles().stream()
                .map(Role::getName)
                .filter(allowedRolesPattern)
                .map(Jsons::v)
                .collect(Collectors.toList())))
      );
    }

    return Response.ok(outcome.toJson()).build();
  }

  private static Response badRequest(String msg) {
    logger.warn("Bad request to tobira/harvest: {}", msg);
    return Response.status(BAD_REQUEST).entity(msg).build();
  }

  /* Since CXF doesn't seem to like accepting multiple types on a single endpoint, this is what Tobira currently sends:
    curl http://localhost/tobira/stats \
        -H "Authorization: Basic YWRtaW46b3BlbmNhc3Q=" \
        -H "Content-Type: application/json" \
        -d '{
        "num_realms": 3641,
        "num_blocks": 5544,
        "version": {
      "identifier": "v3.4",
          "build_time_utc": "Fri, 27 Jun 2025 09:13:53 +0000",
          "git_commit_hash": "d421b168cc6a004dd008f4b8bc0de4070cd99c2c",
          "git_was_dirty": true,
          "target": "aarch64-apple-darwin"
    },
        "config": {
      "download_button_shown": true,
          "auth_source": "tobira-session",
          "login_credentials_handler": "login-callback",
          "session_endpoint_handler": "none",
          "login_link_overridden": false,
          "logout_link_overridden": false,
          "uses_pre_auth": true
    }
  }' \
    -v
  */
  @POST
  @Path("/stats")
  @Consumes(APPLICATION_JSON)
  @RestQuery(
      name = "stats",
      description = "Accepts a json blob of statistical data about Tobira.  To test this properly see the code.",
      responses = {
          @RestResponse(description = "Stats parsed", responseCode = HttpServletResponse.SC_ACCEPTED)
      },
      returnDescription = "No data returned, just a 204 on success"
  )
  public Response acceptStats(@Context HttpServletRequest request) {
    try (InputStream is = request.getInputStream()) {
      String body = IOUtils.toString(is, request.getCharacterEncoding());
      cachedStats = gson.fromJson(body, JsonElement.class).getAsJsonObject();
    } catch (IOException e) {
      logger.error("Error reading request body:", e);
      return badRequest("Error reading response body");
    } catch (IllegalStateException e) {
      return Response.notAcceptable(null).build();
    }
    cachedStats.addProperty("updated", sdf.format(Calendar.getInstance().getTime()));

    db.execTx(em -> {
      AdopterRegistrationExtra existing = em.find(AdopterRegistrationExtra.class, "tobira");
      if (null != existing) {
        existing.setData(gson.toJson(cachedStats));
        em.merge(existing);
      } else {
        existing = new AdopterRegistrationExtra("tobira", gson.toJson(cachedStats));
        em.persist(existing);
      }
    });

    return Response.noContent().build();
  }

  @GET
  @Path("/stats")
  @Produces(APPLICATION_JSON)
  @RestQuery(name = "stats",
      description = "Returns the stats, if any, pushed from Tobira",
      returnDescription = "The stats, or an empty object",
      responses = {
          @RestResponse(description = "The stats, or an empty object", responseCode = HttpServletResponse.SC_OK)
      })
  public Response getCachedStats() {
    return Response.ok(gson.toJson(cachedStats)).build();
  }
}
