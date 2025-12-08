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
package org.opencastproject.external.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.playlists.PlaylistRestService.SAMPLE_PLAYLIST_JSON;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponseBuilder;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistRestService;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.playlists.serialization.JaxbPlaylist;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/api/playlists")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
@RestService(
    name = "externalapiplaylists",
    title = "External API Playlists Service",
    notes = {},
    abstractText = "Provides access to playlist structures"
)
@Component(
    immediate = true,
    service = PlaylistsEndpoint.class,
    property = {
        "service.description=External API - Playlists Endpoint",
        "opencast.service.type=org.opencastproject.external.playlists",
        "opencast.service.path=/api/playlists"
    }
)
@JaxrsResource
public class PlaylistsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ListProviderEndpoint.class);

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /** The capture agent service */
  private PlaylistService service;

  /** OSGi DI */
  @Reference
  public void setPlaylistService(PlaylistService playlistService) {
    this.service = playlistService;
  }

  private PlaylistRestService restService;

  @Reference
  public void setPlaylistRestService(PlaylistRestService playlistRestService) {
    this.restService = playlistRestService;
  }

  /** OSGi activation method */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Playlists Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
        RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  @GET
  @Path("{id}")
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @RestQuery(
      name = "playlist",
      description = "Get a playlist.",
      returnDescription = "A playlist as JSON",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The playlist identifier", type = STRING),
      },
      responses = {
          @RestResponse(description = "Returns the playlist.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The specified playlist instance does not exist.",
              responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "The user doesn't have the rights to make this request.",
              responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response getPlaylistAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id) {
    try {
      Playlist playlist = service.getPlaylistById(id);

      return ApiResponseBuilder.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find playlist instance with id '%s'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IllegalStateException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("")
  @RestQuery(
      name = "playlists",
      description = "Get playlists. Playlists that you do not have read access to will not show up.",
      returnDescription = "A JSON object containing an array.",
      restParameters = {
          @RestParameter(name = "limit", isRequired = false, type = INTEGER,
              description = "The maximum number of results to return for a single request.", defaultValue = "100"),
          @RestParameter(name = "offset", isRequired = false, type = INTEGER,
              description = "The index of the first result to return."),
          @RestParameter(name = "sort", isRequired = false, type = STRING,
              description = "Sort the results based upon a sorting criteria. A criteria is specified as a pair such as:"
                  + "<Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or"
                  + "descending order and is mandatory. Sort Name is case sensitive. Supported Sort Names are 'updated'"
              , defaultValue = "updated:ASC"),
      },
      responses = {
          @RestResponse(description = "Returns the playlist.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response getPlaylistsAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("limit") int limit,
      @QueryParam("offset") int offset,
      @QueryParam("sort") String sort) {
    if (offset < 0) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (limit < 0) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    SortCriterion sortCriterion = new SortCriterion("", SortCriterion.Order.None);
    Optional<String> optSort = Optional.ofNullable(trimToNull(sort));
    if (optSort.isPresent()) {
      sortCriterion = SortCriterion.parse(optSort.get());

      switch (sortCriterion.getFieldName()) {
        case "updated":
          break;
        default:
          logger.info("Unknown sort criteria {}", sortCriterion.getFieldName());
          return Response.serverError().status(Response.Status.BAD_REQUEST).build();
      }
    }

    List<Playlist> playlists = service.getPlaylists(limit, offset, sortCriterion);

    JsonArray playlistsJson = new JsonArray();
    for (Playlist p : playlists) {
      playlistsJson.add(playlistToJson(p));
    }

    return Response.ok(playlistsJson.toString(), acceptHeader).build();
  }

  @POST
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("")
  @RestQuery(
      name = "create",
      description = "Creates a playlist.",
      returnDescription = "The created playlist.",
      restParameters = {
          @RestParameter(name = "playlist", isRequired = false, description = "Playlist in JSON format", type = TEXT,
              jaxbClass = JaxbPlaylist.class, defaultValue = SAMPLE_PLAYLIST_JSON)
      },
      responses = {
          @RestResponse(description = "Playlist created.", responseCode = HttpServletResponse.SC_CREATED),
          @RestResponse(description = "The user doesn't have the rights to make this request.",
              responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response createAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @FormParam("playlist") String playlistText) {
    try {
      // Map JSON to JPA
      Playlist playlist = restService.parseJsonToPlaylist(playlistText);

      // Persist
      playlist = service.update(playlist);
      return ApiResponseBuilder.Json.created(
          acceptHeader,
          URI.create(getPlaylistUrl(playlist.getId())),
          playlistToJson(playlist)
      );
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (ParseException | IOException | IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @PUT
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("{id}")
  @RestQuery(
      name = "update",
      description = "Updates a playlist.",
      returnDescription = "The updated playlist.",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING)
      },
      restParameters = {
          @RestParameter(name = "playlist", isRequired = false, description = "Playlist in JSON format", type = TEXT,
              jaxbClass = JaxbPlaylist.class, defaultValue = SAMPLE_PLAYLIST_JSON)
      },
      responses = {
          @RestResponse(description = "Playlist updated.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The user doesn't have the rights to make this request.",
              responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response updateAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id,
      @FormParam("playlist") String playlistText) {
    try {
      Playlist playlist = service.updateWithJson(id, playlistText);
      return ApiResponseBuilder.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IOException | IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @DELETE
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("{id}")
  @RestQuery(
      name = "remove",
      description = "Removes a playlist.",
      returnDescription = "The removed playlist.",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist removed."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public Response remove(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id) {
    try {
      Playlist playlist = service.remove(id);

      return ApiResponseBuilder.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find playlist instance with id '%s'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
  }

  private JsonObject playlistToJson(Playlist playlist) {
    JsonObject json = new JsonObject();

    json.addProperty("id", playlist.getId());
    JsonArray entriesArray = new JsonArray();
    for (PlaylistEntry entry : playlist.getEntries()) {
      entriesArray.add(playlistEntryToJson(entry));
    }
    json.add("entries", entriesArray);
    json.addProperty("title", safeString(playlist.getTitle()));
    json.addProperty("description", safeString(playlist.getDescription()));
    json.addProperty("creator", safeString(playlist.getCreator()));
    json.addProperty("updated", playlist.getUpdated() != null ? toUTC(playlist.getUpdated().getTime()) : "");
    JsonArray aceArray = new JsonArray();
    for (PlaylistAccessControlEntry ace : playlist.getAccessControlEntries()) {
      aceArray.add(playlistAccessControlEntryToJson(ace));
    }
    json.add("accessControlEntries", aceArray);

    return json;
  }

  private JsonObject playlistEntryToJson(PlaylistEntry playlistEntry) {
    JsonObject json = new JsonObject();

    json.addProperty("id", playlistEntry.getId());
    if (playlistEntry.getContentId() != null) {
      json.addProperty("contentId", playlistEntry.getContentId());
    } else {
      json.add("contentId", null);
    }

    json.add("type", enumToJSON(playlistEntry.getType()));

    return json;
  }

  private JsonObject playlistAccessControlEntryToJson(PlaylistAccessControlEntry playlistAccessControlEntry) {
    JsonObject json = new JsonObject();

    json.addProperty("id", playlistAccessControlEntry.getId());
    json.addProperty("allow", playlistAccessControlEntry.isAllow());
    json.addProperty("role", playlistAccessControlEntry.getRole());
    json.addProperty("action", playlistAccessControlEntry.getAction());

    return json;
  }

  private JsonElement enumToJSON(Enum<?> e) {
    return e == null ? null : new JsonPrimitive(e.toString());
  }

  private String getPlaylistUrl(String playlistId) {
    return UrlSupport.concat(endpointBaseUrl, playlistId);
  }
}
