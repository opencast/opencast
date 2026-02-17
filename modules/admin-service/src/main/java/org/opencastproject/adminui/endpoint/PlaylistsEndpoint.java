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

package org.opencastproject.adminui.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.playlists.PlaylistRestService.SAMPLE_PLAYLIST_JSON;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistRestService;
import org.opencastproject.playlists.PlaylistService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin-ng/playlists")
@Produces(MediaType.APPLICATION_JSON)
@RestService(
    name = "playlistsproxyservice",
    title = "Admin UI - Playlists",
    abstractText = "This service provides the playlists data for the admin UI.",
    notes = {
        "This service offers playlist CRUD operations for the admin UI.",
        "<strong>Important:</strong> "
            + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
            + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
            + "DO NOT use this for integration of third-party applications.</em>"
    }
)
@Component(
    immediate = true,
    service = PlaylistsEndpoint.class,
    property = {
        "service.description=Admin UI - Playlists Endpoint",
        "opencast.service.type=org.opencastproject.adminui.PlaylistsEndpoint",
        "opencast.service.path=/admin-ng/playlists"
    }
)
@JaxrsResource
public class PlaylistsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PlaylistsEndpoint.class);

  private static final int DEFAULT_LIMIT = 100;

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /** The playlists service */
  private PlaylistService service;

  /** The playlists REST service for parsing utilities */
  private PlaylistRestService restService;

  /** OSGi DI */
  @Reference
  public void setPlaylistService(PlaylistService playlistService) {
    this.service = playlistService;
  }

  @Reference
  public void setPlaylistRestService(PlaylistRestService playlistRestService) {
    this.restService = playlistRestService;
  }

  /** OSGi activation method */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating Admin UI - Playlists Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.SERVER_URL_PROPERTY,
        RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  @GET
  @Path("{id}")
  @RestQuery(
      name = "getPlaylist",
      description = "Get a playlist by ID.",
      returnDescription = "A playlist as JSON",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The playlist identifier", type = STRING)
      },
      responses = {
          @RestResponse(description = "Returns the playlist.", responseCode = SC_OK),
          @RestResponse(description = "The specified playlist does not exist.",
              responseCode = SC_NOT_FOUND),
          @RestResponse(description = "The user does not have permission to access this playlist.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = SC_BAD_REQUEST)
      })
  public Response getPlaylist(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id) {
    try {
      Playlist playlist = service.getPlaylistById(id);
      return Response.ok(playlistToJson(playlist).toString(), MediaType.APPLICATION_JSON_TYPE).build();
    } catch (NotFoundException e) {
      logger.info("Playlist '{}' not found.", id);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.warn("User doesn't have permission to access playlist '{}'.", id);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IllegalStateException e) {
      logger.warn("Invalid state when accessing playlist '{}': {}", id, e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Path("")
  @RestQuery(
      name = "getPlaylists",
      description = "Get playlists. Playlists that you do not have read access to will not show up.",
      returnDescription = "A JSON object containing an array of playlists.",
      restParameters = {
          @RestParameter(name = "limit", isRequired = false, type = INTEGER,
              description = "The maximum number of results to return for a single request.", defaultValue = "100"),
          @RestParameter(name = "offset", isRequired = false, type = INTEGER,
              description = "The index of the first result to return.", defaultValue = "0"),
          @RestParameter(name = "sort", isRequired = false, type = STRING,
              description = "Sort the results based upon a sorting criteria. Must be in the form '<Sort "
                  + "Name>:ASC' or '<Sort Name>:DESC'. Sort Name is case sensitive."
                  + " Supported Sort Names are 'updated', 'title', and 'creator'.",
              defaultValue = "updated:ASC"),
          @RestParameter(name = "filter", isRequired = false, type = STRING,
              description = "The filters used for the query. Format: 'key:value' separated by commas. "
                  + "Supported keys: 'textFilter' (searches title, description, creator), 'title', "
                  + "'creator', 'description'. Example: 'textFilter:opencast' or "
                  + "'title:meeting,creator:john'.")
      },
      responses = {
          @RestResponse(description = "Returns the playlists.", responseCode = SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = SC_BAD_REQUEST)
      })
  public Response getPlaylists(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("limit") Integer limit,
      @QueryParam("offset") Integer offset,
      @QueryParam("sort") String sort,
      @QueryParam("filter") String filter) {

    Optional<Integer> optLimit = Optional.ofNullable(limit);
    Optional<Integer> optOffset = Optional.ofNullable(offset);
    Optional<String> optSort = Optional.ofNullable(trimToNull(sort));

    // If the limit is set to 0, this is not taken into account.
    if (optLimit.isPresent() && limit == 0) {
      optLimit = Optional.empty();
    }

    Map<String, String> filters = RestUtils.parseFilter(filter);
    List<Predicate<Playlist>> filterPredicates = new ArrayList<>();
    // Add basic filters.
    for (String name : filters.keySet()) {
      if ("textFilter".equals(name)) {
        String value = filters.get(name).toLowerCase();
        filterPredicates.add(p ->
            (p.getTitle() != null
                && p.getTitle().toLowerCase().contains(value))
            || (p.getDescription() != null
                && p.getDescription().toLowerCase().contains(value))
            || (p.getCreator() != null
                && p.getCreator().toLowerCase().contains(value)));
      }
      if ("title".equals(name)) {
        String value = filters.get(name).toLowerCase();
        filterPredicates.add(p -> p.getTitle() != null
            && p.getTitle().toLowerCase().contains(value));
      }
      if ("creator".equals(name)) {
        String value = filters.get(name).toLowerCase();
        filterPredicates.add(p -> p.getCreator() != null
            && p.getCreator().toLowerCase().contains(value));
      }
      if ("description".equals(name)) {
        String value = filters.get(name).toLowerCase();
        filterPredicates.add(p -> p.getDescription() != null
            && p.getDescription().toLowerCase().contains(value));
      }
    }

    SortCriterion sortCriterion
        = new SortCriterion("updated", SortCriterion.Order.Ascending);
    if (optSort.isPresent()) {
      ArrayList<SortCriterion> sortCriteria
          = RestUtils.parseSortQueryParameter(optSort.get());
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case "updated":
          case "creator":
          case "title":
            sortCriterion = criterion;
            break;
          default:
            logger.warn("Unknown sort criteria: {}",
                criterion.getFieldName());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
      }
    }

    // Fetch all playlists (filtering is done in memory since
    // playlists are not in the search index). Todo: Add to index?
    List<Playlist> allPlaylists
        = service.getPlaylists(Integer.MAX_VALUE, 0, sortCriterion);

    // Apply filters
    List<Playlist> filteredPlaylists = allPlaylists;
    for (Predicate<Playlist> predicate : filterPredicates) {
      filteredPlaylists = filteredPlaylists.stream()
          .filter(predicate).toList();
    }

    // Apply pagination
    int effectiveOffset = optOffset.orElse(0);
    int effectiveLimit = optLimit.orElse(DEFAULT_LIMIT);
    int actualOffset = Math.min(effectiveOffset, filteredPlaylists.size());
    int endIndex = Math.min(
        actualOffset + effectiveLimit, filteredPlaylists.size());
    List<Playlist> paginatedPlaylists
        = filteredPlaylists.subList(actualOffset, endIndex);

    List<JsonObject> playlistsList = new ArrayList<>();
    for (Playlist p : paginatedPlaylists) {
      playlistsList.add(playlistToJson(p));
    }

    return okJsonList(playlistsList, effectiveOffset, effectiveLimit,
        filteredPlaylists.size());
  }

  @POST
  @Path("")
  @RestQuery(
      name = "createPlaylist",
      description = "Creates a new playlist.",
      returnDescription = "The created playlist.",
      restParameters = {
          @RestParameter(name = "playlist", isRequired = true, description = "Playlist in JSON format", type = TEXT,
              defaultValue = SAMPLE_PLAYLIST_JSON)
      },
      responses = {
          @RestResponse(description = "Playlist created.", responseCode = SC_CREATED),
          @RestResponse(description = "The user does not have permission to create a playlist.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = SC_BAD_REQUEST)
      })
  public Response createPlaylist(
      @HeaderParam("Accept") String acceptHeader,
      @FormParam("playlist") String playlistText) {
    try {
      // Parse JSON to playlist
      Playlist playlist = restService.parseJsonToPlaylist(playlistText);

      // Persist
      playlist = service.update(playlist);

      return Response.created(URI.create(getPlaylistUrl(playlist.getId())))
          .entity(playlistToJson(playlist).toString())
          .type(MediaType.APPLICATION_JSON_TYPE)
          .build();
    } catch (UnauthorizedException e) {
      logger.warn("User doesn't have permission to create playlist.");
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (ParseException | IOException | IllegalArgumentException e) {
      logger.warn("Invalid playlist JSON: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @PUT
  @Path("{id}")
  @RestQuery(
      name = "updatePlaylist",
      description = "Updates an existing playlist.",
      returnDescription = "The updated playlist.",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING)
      },
      restParameters = {
          @RestParameter(name = "playlist", isRequired = true, description = "Playlist in JSON format", type = TEXT,
              defaultValue = SAMPLE_PLAYLIST_JSON)
      },
      responses = {
          @RestResponse(description = "Playlist updated.", responseCode = SC_OK),
          @RestResponse(description = "The user does not have permission to update this playlist.",
              responseCode = SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.",
              responseCode = SC_BAD_REQUEST)
      })
  public Response updatePlaylist(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id,
      @FormParam("playlist") String playlistText) {
    try {
      Playlist playlist = service.updateWithJson(id, playlistText);
      return Response.ok(playlistToJson(playlist).toString(), MediaType.APPLICATION_JSON_TYPE).build();
    } catch (UnauthorizedException e) {
      logger.warn("User doesn't have permission to update playlist '{}'.", id);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IOException | IllegalArgumentException e) {
      logger.warn("Invalid playlist JSON: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @DELETE
  @Path("{id}")
  @RestQuery(
      name = "removePlaylist",
      description = "Removes a playlist.",
      returnDescription = "The removed playlist.",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING)
      },
      responses = {
          @RestResponse(description = "Playlist removed.", responseCode = SC_OK),
          @RestResponse(description = "No playlist with that identifier exists.",
              responseCode = SC_NOT_FOUND),
          @RestResponse(description = "The user does not have permission to delete this playlist.",
              responseCode = SC_FORBIDDEN)
      })
  public Response removePlaylist(
      @HeaderParam("Accept") String acceptHeader,
      @PathParam("id") String id) {
    try {
      Playlist playlist = service.remove(id);
      return Response.ok(playlistToJson(playlist).toString(), MediaType.APPLICATION_JSON_TYPE).build();
    } catch (NotFoundException e) {
      logger.info("Playlist '{}' not found.", id);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      logger.warn("User doesn't have permission to delete playlist '{}'.", id);
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

  private JsonObject playlistAccessControlEntryToJson(PlaylistAccessControlEntry ace) {
    JsonObject json = new JsonObject();

    json.addProperty("id", ace.getId());
    json.addProperty("allow", ace.isAllow());
    json.addProperty("role", ace.getRole());
    json.addProperty("action", ace.getAction());

    return json;
  }

  private JsonElement enumToJSON(Enum<?> e) {
    return e == null ? null : new JsonPrimitive(e.toString());
  }

  private String getPlaylistUrl(String playlistId) {
    return UrlSupport.concat(endpointBaseUrl, playlistId);
  }
}
