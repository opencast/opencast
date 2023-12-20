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
package org.opencastproject.playlists;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.LONG;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.playlists.serialization.JaxbPlaylist;
import org.opencastproject.playlists.serialization.JaxbPlaylistEntry;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlSafeParser;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * A REST endpoint for the {@link PlaylistService}
 */
@Path("/")
@RestService(
    name = "playlistservice",
    title = "Playlist Service",
    abstractText = "This service lists available playlists and stuff",
    notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
@Component(
    immediate = true,
    service = PlaylistRestService.class,
    property = {
        "service.description=Playlist REST Endpoint",
        "opencast.service.type=org.opencastproject.playlists",
        "opencast.service.path=/playlists"
    }
)
public class PlaylistRestService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(PlaylistRestService.class);

  public static final String SAMPLE_PLAYLIST_JSON = "{\n"
      + "        \"title\": \"Opencast Playlist\",\n"
      + "        \"description\": \"This is a playlist about Opencast\",\n"
      + "        \"creator\": \"Opencast\",\n"
      + "        \"updated\": 1701361007521,\n"
      + "        \"entries\": [\n"
      + "            {\n"
      + "                \"contentId\": \"ID-about-opencast\",\n"
      + "                \"type\": \"EVENT\"\n"
      + "            },\n"
      + "            {\n"
      + "                \"contentId\": \"ID-3d-print\",\n"
      + "                \"type\": \"EVENT\"\n"
      + "            }\n"
      + "        ],\n"
      + "        \"accessControlEntries\": [\n"
      + "            {\n"
      + "                \"allow\": true,\n"
      + "                \"role\": \"ROLE_USER_BOB\",\n"
      + "                \"action\": \"read\"\n"
      + "            }\n"
      + "        ]\n"
      + "}";

  public static final String SAMPLE_PLAYLIST_ENTRIES_JSON = "[\n"
      + "            {\n"
      + "                \"contentId\": \"ID-about-opencast\",\n"
      + "                \"type\": \"EVENT\"\n"
      + "            },\n"
      + "            {\n"
      + "                \"contentId\": \"ID-3d-print\",\n"
      + "                \"type\": \"EVENT\"\n"
      + "            }\n"
      + "        ],";

  public static final String SAMPLE_PLAYLIST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><"
      + "ns3:playlist xmlns:ns2=\"http://mediapackage.opencastproject.org\" "
      + "xmlns:ns3=\"http://playlist.opencastproject.org\" id=\"1059\"><organization>mh_default_org</organization>"
      + "<entries id=\"1061\"><contentId>ID-av-portal</contentId><type>EVENT</type></entries><entries id=\"1062\">"
      + "<contentId>ID-av-print</contentId><type>EVENT</type></entries><title>Opencast Playlist</title>"
      + "<description>This is a playlist about Opencast</description><creator>Opencast</creator>"
      + "<updated>1701787700848</updated><accessControlEntries><allow>true</allow><role>ROLE_USER_BOB</role>"
      + "<action>read</action></accessControlEntries></ns3:playlist>";

  public static final String SAMPLE_PLAYLIST_ENTRIES_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
      + "<entries>\n"
      + "\t<entry id=\"1061\">\n" + "\t\t<contentId>ID-av-portal</contentId>\n" + "\t\t<type>EVENT</type>\n"
      + "\t</entry>\n"
      + "\t<entry id=\"1062\">\n" + "\t\t<contentId>ID-av-print</contentId>\n" + "\t\t<type>EVENT</type>\n"
      + "\t</entry>\n" + "</entries>";

  /** The playlist service instance */
  private PlaylistService service;

  /** The search service */
  protected SearchService searchService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /**
   * Sets the playlist service
   *
   * @param service
   *          the playlist service instance
   */
  @Reference
  public void setService(PlaylistService service) {
    this.service = service;
  }

  @Reference
  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("playlist.json")
  @RestQuery(
      name = "playlist",
      description = "Get a playlist.",
      returnDescription = "A playlist as JSON",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The playlist identifier", type = STRING),
          @RestParameter(name = "withPublications", isRequired = false, description = "If available publications for"
              + "the content should be returned. Only works for content of type EVENT.", type = BOOLEAN,
              defaultValue = "true")
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "A playlist as JSON."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist getPlaylistAsJson(
      @FormParam("id") String id,
      @FormParam("withPublications") boolean withPublications)
          throws NotFoundException, UnauthorizedException {
    Playlist playlist = service.getPlaylistById(id);

    JaxbPlaylist jaxbPlaylist;
    if (withPublications) {
      jaxbPlaylist = service.enrich(playlist);
    } else {
      jaxbPlaylist = new JaxbPlaylist(playlist);
    }

    return jaxbPlaylist;
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Path("playlist.xml")
  @RestQuery(
      name = "playlist",
      description = "Get a playlist.",
      returnDescription = "A playlist as XML",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The playlist identifier", type = STRING),
          @RestParameter(name = "withPublications", isRequired = false, description = "If available publications for"
              + "the content should be returned. Only works for content of type EVENT.", type = BOOLEAN,
              defaultValue = "true")
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "A playlist as XML."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist getPlaylistAsXml(
      @FormParam("id") String id,
      @FormParam("withPublications") boolean withPublications)
          throws NotFoundException, UnauthorizedException {
    return getPlaylistAsJson(id, withPublications);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
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
          @RestResponse(responseCode = SC_OK, description = "A playlist as JSON."),
      })
  public List<JaxbPlaylist> getPlaylistsAsJson(
      @FormParam("limit") int limit,
      @FormParam("offset") int offset,
      @FormParam("sortByUpdated") boolean sortByUpdated,
      @FormParam("sortByUpdatedAscending") boolean sortByUpdatedAscending)
          throws NotFoundException {
    List<JaxbPlaylist> jaxbPlaylists = new ArrayList<>();
    for (Playlist playlist : service.getPlaylists(limit, offset, sortByUpdatedAscending, sortByUpdatedAscending)) {
      jaxbPlaylists.add(new JaxbPlaylist(playlist));
    }
    return jaxbPlaylists;
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Path("playlists.xml")
  @RestQuery(
      name = "playlists",
      description = "Get playlists. Playlists that you do not have read access to will not show up.",
      returnDescription = "A XML object containing an array.",
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
          @RestResponse(responseCode = SC_OK, description = "A playlist as XML."),
      })
  public List<JaxbPlaylist> getPlaylistsAsXml(
      @FormParam("limit") int limit,
      @FormParam("offset") int offset,
      @FormParam("sortByUpdated") boolean sortByUpdated,
      @FormParam("sortByUpdatedAscending") boolean sortByUpdatedAscending)
          throws NotFoundException {
    return getPlaylistsAsJson(limit, offset, sortByUpdated, sortByUpdatedAscending);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
//  @Consumes(MediaType.APPLICATION_JSON)
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
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist updateAsJson(@FormParam("playlist") String playlistText)
          throws UnauthorizedException {
    try {
      // Map JSON to JPA
      Playlist playlist = parseJsonToPlaylist(playlistText);

      // Persist
      playlist = service.update(playlist);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  @Path("update.xml")
  @RestQuery(
      name = "update",
      description = "Updates a playlist or creates a new one.",
      returnDescription = "The updated playlist.",
      restParameters = {
          @RestParameter(name = "playlist", isRequired = false, description = "Playlist in XML format", type = TEXT,
              jaxbClass = JaxbPlaylist.class, defaultValue = SAMPLE_PLAYLIST_XML)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist updateAsXml(@FormParam("playlist") String playlistText)
          throws UnauthorizedException {
    try {
      // Map XML to JPA
      Playlist playlist = parseXmlToPlaylist(playlistText);

      // Persist
      playlist = service.update(playlist);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
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
  public JaxbPlaylist remove(@FormParam("id") String id) throws NotFoundException, UnauthorizedException {
    try {
      // Persist
      Playlist playlist = service.remove(id);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  //  @Consumes(MediaType.APPLICATION_JSON)
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
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist updateEntriesAsJson(
      @FormParam("id") String playlistId,
      @FormParam("playlistEntries") String entriesText)
          throws UnauthorizedException {
    try {
      // Map JSON to JPA
      List<PlaylistEntry> playlistEntries = parseJsonToPlaylistEntries(entriesText);

      // Persist
      Playlist playlist = service.updateEntries(playlistId, playlistEntries);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  //  @Consumes(MediaType.APPLICATION_JSON)
  @Path("updateEntries.xml")
  @RestQuery(
      name = "updateEntries",
      description = "Updates the entries of a playlist",
      returnDescription = "The updated playlist.",
      restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "Playlist identifier", type = STRING),
          @RestParameter(name = "playlistEntries", isRequired = false, description = "Playlist entries in XML format",
              type = TEXT, jaxbClass = JaxbPlaylistEntry[].class, defaultValue = SAMPLE_PLAYLIST_ENTRIES_XML)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist updateEntriesAsXml(
      @FormParam("id") String playlistId,
      @FormParam("playlistEntries") String entriesText)
          throws UnauthorizedException {
    try {
      // Map JSON to JPA
      List<PlaylistEntry> playlistEntries = parseXmlToPlaylistEntries(entriesText);

      // Persist
      Playlist playlist = service.updateEntries(playlistId, playlistEntries);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("addEntry")
  @RestQuery(
      name = "addEntry",
      description = "Add entry to playlist.",
      returnDescription = "No content.",
      restParameters = {
          @RestParameter(name = "playlistId", isRequired = true, description = "Identifier of the playlist to add to",
              type = STRING),
          @RestParameter(name = "contentId", isRequired = false, description = "Content identifier", type = STRING),
          @RestParameter(name = "type", isRequired = false, description = "Entry type. Enum. Valid values are EVENT,"
              + " INACCESSIBLE.", type = STRING),
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist addEntry(
      @FormParam("playlistId") String playlistId,
      @FormParam("contentId") String contentId,
      @FormParam("type") PlaylistEntryType type)
          throws NotFoundException, UnauthorizedException {
    try {
      Playlist playlist = service.addEntry(playlistId, contentId, type);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("removeEntry")
  @RestQuery(
      name = "removeEntry",
      description = "Remove entry from playlist.",
      returnDescription = "Playlist without the enrty.",
      restParameters = {
          @RestParameter(name = "playlistId", isRequired = true, type = STRING,
              description = "Identifier of the playlist to delete from"),
          @RestParameter(name = "entryId", isRequired = false, type = LONG,
              description = "Identifier of the enrty that should be deleted")
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "Playlist updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No playlist or entry with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "Not authorized to perform this action")
      })
  public JaxbPlaylist addEntry(
      @FormParam("playlistId") String playlistId,
      @FormParam("entryId") Long entryId)
          throws NotFoundException, UnauthorizedException {
    try {
      Playlist playlist = service.removeEntry(playlistId, entryId);
      return new JaxbPlaylist(playlist);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * While jackson takes care of automatically converting JAXB to JSON when returning from a request, getting it to
   * parse JSON to JAXB when accepting a request is not that automatic. This functions takes care of that.
   * @param json Valid JSON as a string
   * @return A Playlist containing the information from the JSON
   * @throws ParseException
   * @throws IOException
   */
  public Playlist parseJsonToPlaylist(String json) throws ParseException, IOException {
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);

    JaxbPlaylist jaxbPlaylist = objectMapper.readValue(json, JaxbPlaylist.class);
    return jaxbPlaylist.toPlaylist();
  }

  /**
   * More string to JAXB conversions, this time for a list of entries
   * @param json a JSON array of playlist entries
   * @return A list of playlist entries
   * @throws JsonProcessingException
   */
  public List<PlaylistEntry> parseJsonToPlaylistEntries(String json) throws JsonProcessingException {
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);

    JaxbPlaylistEntry[] jaxbPlaylistEntries = objectMapper.readValue(json, JaxbPlaylistEntry[].class);

    List<PlaylistEntry> playlistEntries = new ArrayList<>();
    for (JaxbPlaylistEntry entry : jaxbPlaylistEntries) {
      playlistEntries.add(entry.toPlaylistEntry());
    }
    return playlistEntries;
  }

  private Playlist parseXmlToPlaylist(String xml) throws JAXBException, IOException, SAXException {
    JAXBContext context = JAXBContext.newInstance(JaxbPlaylist.class);
    JaxbPlaylist jaxbPlaylist = context.createUnmarshaller()
        .unmarshal(XmlSafeParser.parse(IOUtils.toInputStream(xml, "UTF8")), JaxbPlaylist.class)
        .getValue();
    return jaxbPlaylist.toPlaylist();
  }

  private List<PlaylistEntry>  parseXmlToPlaylistEntries(String xml) throws JAXBException, IOException, SAXException {
    JAXBContext context = JAXBContext.newInstance(JaxbPlaylistEntry[].class);
    JaxbPlaylistEntry[] jaxbPlaylistEntries = context.createUnmarshaller()
        .unmarshal(XmlSafeParser.parse(IOUtils.toInputStream(xml, "UTF8")), JaxbPlaylistEntry[].class)
        .getValue();

    List<PlaylistEntry> playlistEntries = new ArrayList<>();
    for (JaxbPlaylistEntry entry : jaxbPlaylistEntries) {
      playlistEntries.add(entry.toPlaylistEntry());
    }
    return playlistEntries;
  }
}
