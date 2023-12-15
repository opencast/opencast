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

import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.opencastproject.playlists.PlaylistRestService.SAMPLE_PLAYLIST_ENTRIES_JSON;
import static org.opencastproject.playlists.PlaylistRestService.SAMPLE_PLAYLIST_JSON;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistRestService;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.playlists.serialization.JaxbPlaylist;
import org.opencastproject.playlists.serialization.JaxbPlaylistEntry;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/")
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
public class PlaylistsEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ListProviderEndpoint.class);

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
  }

  @GET
  @Path("playlist.json")
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @RestQuery(
      name = "playlist",
      description = "Get a playlist.",
      returnDescription = "A playlist as JSON",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The playlist identifier", type = STRING),
      },
      responses = {
          @RestResponse(description = "Returns the playlist.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The specified playlist instance does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response getPlaylistAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("id") String id) {
    try {
      Playlist playlist = service.getPlaylistById(id);

      return ApiResponses.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find playlist instance with id '%s'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IllegalStateException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("playlists.json")
  @RestQuery(
      name = "playlists",
      description = "Get playlists. Playlists that you do not have read access to will not show up.",
      returnDescription = "A JSON object containing an array.",
      restParameters = {
          @RestParameter(name = "limit", isRequired = false, type = INTEGER,
              description = "The maximum number of results to return for a single request.", defaultValue = "100"),
          @RestParameter(name = "offset", isRequired = false, type = INTEGER,
              description = "The index of the first result to return."),
          @RestParameter(name = "sortByUpdated", isRequired = false, type = BOOLEAN,
              description = "Sort the results based on updated field.", defaultValue = "false"),
          @RestParameter(name = "sortByUpdatedAscending", isRequired = false, type = BOOLEAN,
              description = "If sorting by updated, should it be ascending?", defaultValue = "false")
      },
      responses = {
          @RestResponse(description = "Returns the playlist.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response getPlaylistsAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @QueryParam("limit") int limit,
      @QueryParam("offset") int offset,
      @QueryParam("sortByUpdated") boolean sortByUpdated,
      @QueryParam("sortByUpdatedAscending") boolean sortByUpdatedAscending) {
    List<Playlist> playlists = service.getPlaylists(limit, offset, sortByUpdatedAscending, sortByUpdatedAscending);

    List<JValue> playlistsJson = playlists.stream()
        .map(p -> playlistToJson(p))
        .collect(Collectors.toList());

    return ApiResponses.Json.ok(acceptHeader, arr(playlistsJson));
  }

  @POST
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("update.json")
  @RestQuery(
      name = "update",
      description = "Updates a playlist or creates a new one.",
      returnDescription = "The updated playlist.",
      restParameters = {
          @RestParameter(name = "playlist", isRequired = false, description = "Playlist in JSON format", type = TEXT,
              jaxbClass = JaxbPlaylist.class, defaultValue = SAMPLE_PLAYLIST_JSON)
      },
      responses = {
          @RestResponse(description = "Playlist updated.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response updateAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @FormParam("playlist") String playlistText) {
    try {
      // Map JSON to JPA
      Playlist playlist = restService.parseJsonToPlaylist(playlistText);

      // Persist
      playlist = service.update(playlist);
      return ApiResponses.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (ParseException | IOException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @DELETE
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("remove")
  @RestQuery(
      name = "remove",
      description = "Removes a playlist.",
      returnDescription = "No content.",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist removed."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public Response remove(
      @HeaderParam("Accept") String acceptHeader,
      @FormParam("id") String id) {
    try {
      Playlist playlist = service.remove(id);

      return ApiResponses.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find playlist instance with id '%s'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
  }

  @POST
  @Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_11_0 })
  @Path("updateEntries.json")
  @RestQuery(
      name = "updateEntries",
      description = "Updates the entries of a playlist",
      returnDescription = "The updated playlist.",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING),
          @RestParameter(name = "playlistEntries", isRequired = false, description = "Playlist entries in JSON format",
              type = TEXT, jaxbClass = JaxbPlaylistEntry[].class, defaultValue = SAMPLE_PLAYLIST_ENTRIES_JSON)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action"),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      })
  public Response updateEntriesAsJson(
      @HeaderParam("Accept") String acceptHeader,
      @FormParam("id") String playlistId,
      @FormParam("playlistEntries") String entriesText) {
    try {
      // Map JSON to JPA
      List<PlaylistEntry> playlistEntries = restService.parseJsonToPlaylistEntries(entriesText);

      // Persist
      Playlist playlist = service.updateEntries(playlistId, playlistEntries);
      return ApiResponses.Json.ok(acceptHeader, playlistToJson(playlist));
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (JsonProcessingException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find playlist instance with id '%s'.", playlistId);
    }
  }

  private JValue playlistToJson(Playlist playlist) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("id", v(playlist.getId())));
    fields.add(f("entries", arr(playlist.getEntries()
        .stream()
        .map(this::playlistEntryToJson)
        .collect(Collectors.toList()))));
    fields.add(f("title", v(playlist.getTitle(), BLANK)));
    fields.add(f("description", v(playlist.getDescription(), BLANK)));
    fields.add(f("creator", v(playlist.getCreator(), BLANK)));
    fields.add(f("updated", v(playlist.getUpdated() != null ? toUTC(playlist.getUpdated().getTime()) : null, BLANK)));
    fields.add(f("accessControlEntries", arr(playlist.getAccessControlEntries()
        .stream()
        .map(this::playlistAccessControlEntryToJson)
        .collect(Collectors.toList()))));

    return obj(fields);
  }

  private JValue playlistEntryToJson(PlaylistEntry playlistEntry) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("id", v(playlistEntry.getId())));
    fields.add(f("eventId", v(playlistEntry.getEventId())));
    fields.add(f("type", enumToJSON(playlistEntry.getType())));
    return obj(fields);
  }

  private JValue playlistAccessControlEntryToJson(PlaylistAccessControlEntry playlistAccessControlEntry) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("id", v(playlistAccessControlEntry.getId())));
    fields.add(f("allow", v(playlistAccessControlEntry.isAllow())));
    fields.add(f("role", v(playlistAccessControlEntry.getRole())));
    fields.add(f("action", v(playlistAccessControlEntry.getAction())));

    return obj(fields);
  }

  private JValue enumToJSON(Enum e) {
    return e == null ? null : v(e.toString());
  }
}
