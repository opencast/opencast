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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.playlists.persistence.PlaylistDatabaseException;
import org.opencastproject.playlists.persistence.PlaylistDatabaseService;
import org.opencastproject.playlists.serialization.JaxbPlaylist;
import org.opencastproject.playlists.serialization.JaxbPlaylistEntry;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component(
    property = {
        "service.description=Playlist Service",
        "service.pid=org.opencastproject.playlists.PlaylistService"
    },
    immediate = true,
    service = { PlaylistService.class }
)
public class PlaylistService {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);

  /** Persistent storage */
  protected PlaylistDatabaseService persistence;

  /** The security service */
  protected SecurityService securityService;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  private ElasticsearchIndex elasticsearchIndex;

  /**
   * Callback to set the playlist database
   *
   * @param persistence
   *          the playlist database
   */
  @Reference(name = "playlist-persistence")
  public void setPersistence(PlaylistDatabaseService persistence) {
    this.persistence = persistence;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the authorization service.
   *
   * @param authorizationService
   *          the authorizationService to set
   */
  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Reference
  void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  @Activate
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Playlist Service");
  }

  /**
   * Returns a playlist from the database by its id
   * @param id playlist id
   * @return The {@link Playlist} belonging to the id
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have read access for the playlist
   */
  public Playlist getPlaylistById(String id) throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      Playlist playlist = persistence.getPlaylist(id);
      if (!checkPermission(playlist, Permissions.Action.READ)) {
        throw new UnauthorizedException("User does not have read permissions");
      }
      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not get playlist from database with id ");
    }
  }

  /**
   * Get multiple playlists from the database
   * @param limit The maximum amount of playlists to get with one request.
   * @param offset The index of the first result to return.
   * @return A list of {@link Playlist}s
   * @throws IllegalStateException If something went wrong in the database service
   */
  public List<Playlist> getPlaylists(int limit, int offset) throws IllegalStateException {
    return getPlaylists(limit, offset, new SortCriterion("", SortCriterion.Order.None));
  }

  public List<Playlist> getPlaylists(int limit, int offset, SortCriterion sortCriterion)
          throws IllegalStateException {
    try {
      List<Playlist> playlists = persistence.getPlaylists(limit, offset, sortCriterion);
      playlists.removeIf(playlist -> !checkPermission(playlist, Permissions.Action.READ));
      return playlists;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not get playlist from database with id ");
    }
  }

  public List<Playlist> getAllForAdministrativeRead(Date from, Date to, int limit)
          throws IllegalStateException, UnauthorizedException {
    final var user = securityService.getUser();
    final var orgAdminRole = securityService.getOrganization().getAdminRole();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(orgAdminRole)) {
      throw new UnauthorizedException("Only (org-)admins can call this method");
    }

    try {
      return persistence.getAllForAdministrativeRead(from, to, limit);
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not get playlist from database", e);
    }
  }

  /**
   * Persist a new playlist in the database or update an existing one
   * @param playlist The {@link Playlist} to create or update with
   * @return The updated {@link Playlist}
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for an existing playlist
   */
  public Playlist update(Playlist playlist)
          throws IllegalStateException, UnauthorizedException, IllegalArgumentException {
    try {
      Playlist existingPlaylist = persistence.getPlaylist(playlist.getId());
      if (!checkPermission(existingPlaylist, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }

      // Validate entry IDs
      for (PlaylistEntry entry : playlist.getEntries()) {
        if (existingPlaylist.getEntries().stream().noneMatch(e -> entry.getId() == e.getId())) {
          if (entry.getId() != 0) {
            throw new IllegalArgumentException("When updating a playlist, entries should either have the id of an "
                + "existing entry, or no id at all.");
          }
        }
      }
      for (PlaylistAccessControlEntry entry : playlist.getAccessControlEntries()) {
        if (existingPlaylist.getAccessControlEntries().stream().noneMatch(e -> entry.getId() == e.getId())) {
          if (entry.getId() != 0) {
            throw new IllegalArgumentException("When updating a playlist, ACL entries should either have the id of an "
                + "existing entry, or no id at all.");
          }
        }
      }
    } catch (NotFoundException e) {
      // This means we are creating a new playlist
      for (PlaylistEntry entry : playlist.getEntries()) {
        if (entry.getId() != 0) {
          throw new IllegalArgumentException("Entries for new playlists should not have identifiers set");
        }
      }
      for (PlaylistAccessControlEntry entry : playlist.getAccessControlEntries()) {
        if (entry.getId() != 0) {
          throw new IllegalArgumentException("ACL Entries for new playlists should not have identifiers set");
        }
      }
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not get playlist from database with id ");
    }

    if (playlist.getOrganization() == null) {
      playlist.setOrganization(securityService.getOrganization().getId());
    }
    for (PlaylistEntry entry : playlist.getEntries()) {
      entry.setPlaylist(playlist);
    }
    for (PlaylistAccessControlEntry entry : playlist.getAccessControlEntries()) {
      entry.setPlaylist(playlist);
    }

    try {
      playlist = persistence.updatePlaylist(playlist, securityService.getOrganization().getId());
      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not update playlist from database with id ");
    }
  }

  /**
   * Overwrite an existing playlist with a playlist in JSON format.
   * Only fields present in the JSON will be overwritten! Conversely, if a field is not present in the JSON,
   * the field in the existing playlist will not change.
   * @param id Identifier of the playlist to update.
   * @param json JSON containing data to update the playlist with.
   * @return The updated {@link Playlist}
   */
  public Playlist updateWithJson(String id, String json) throws JsonProcessingException, UnauthorizedException {
    try {
      Playlist existingPlaylist = persistence.getPlaylist(id);
      if (!checkPermission(existingPlaylist, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }

      JaxbAnnotationModule module = new JaxbAnnotationModule();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(module);
      objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

      ObjectReader updater = objectMapper.readerForUpdating(new JaxbPlaylist(existingPlaylist));
      JaxbPlaylist merged = updater.readValue(json);

      return update(merged.toPlaylist());
    } catch (NotFoundException | PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not get playlist from database with id " + id);
    }
  }

  /**
   * Deletes a playlist from the database
   * @param playlistId The playlist identifier
   * @return The removed {@link Playlist}
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  public Playlist remove(String playlistId)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    try {
      Playlist playlist = persistence.getPlaylist(playlistId);
      if (!checkPermission(playlist, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }
      playlist = persistence.deletePlaylist(playlist, securityService.getOrganization().getId());
      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not delete playlist from database with id " + playlistId);
    }
  }

  /**
   * Replaces the entries in the playlist with the given entries
   * @param playlistId identifier of the playlist to modify
   * @param playlistEntries the new playlist entries
   * @return {@link Playlist} with the new entries
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  public Playlist updateEntries(String playlistId, List<PlaylistEntry> playlistEntries)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    Playlist playlist;
    try {
      playlist = persistence.getPlaylist(playlistId);
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException(e);
    }
    if (!checkPermission(playlist, Permissions.Action.WRITE)) {
      throw new UnauthorizedException("User does not have write permissions");
    }
    playlist.setEntries(playlistEntries);

    try {
      playlist = persistence.updatePlaylist(playlist, securityService.getOrganization().getId());

      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not delete playlist from database with id " + playlistId);
    }
  }

  /**
   * Adds a new entry at the end of a playlist and persists it
   * @param playlistId The playlist identifier
   * @param contentId content (e.g. mediapacakge) identifier
   * @param type arbitrary string
   * @return {@link Playlist} with the new entry
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  public Playlist addEntry(String playlistId, String contentId, PlaylistEntryType type)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    Playlist playlist;
    try {
      playlist = persistence.getPlaylist(playlistId);
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException(e);
    }
    if (!checkPermission(playlist, Permissions.Action.WRITE)) {
      throw new UnauthorizedException("User does not have write permissions");
    }
    PlaylistEntry playlistEntry = new PlaylistEntry();
    playlistEntry.setContentId(contentId);
    playlistEntry.setType(type);
    playlist.addEntry(playlistEntry);

    try {
      playlist = persistence.updatePlaylist(playlist, securityService.getOrganization().getId());

      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not delete playlist from database with id " + playlistId);
    }
  }

  /**
   * Removes an entry with the given id from the playlist and persists it
   * @param playlistId The playlist identifier
   * @param entryId The entry identifier
   * @return {@link Playlist} without the entry
   * @throws NotFoundException If no playlist with the given id could be found
   * @throws IllegalStateException If something went wrong in the database service
   * @throws UnauthorizedException If the user does not have write access for the playlist
   */
  public Playlist removeEntry(String playlistId, long entryId)
          throws NotFoundException, IllegalStateException, UnauthorizedException {
    Playlist playlist;
    try {
      playlist = persistence.getPlaylist(playlistId);
      if (!checkPermission(playlist, Permissions.Action.WRITE)) {
        throw new UnauthorizedException("User does not have write permissions");
      }
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException(e);
    }

    playlist.removeEntry(
        playlist.getEntries()
            .stream()
            .filter(e -> e.getId() == entryId)
            .findFirst()
            .get()
    );

    try {
      playlist = persistence.updatePlaylist(playlist, securityService.getOrganization().getId());
      return playlist;
    } catch (PlaylistDatabaseException e) {
      throw new IllegalStateException("Could not delete playlist from database with id " + playlistId);
    }
  }

  /**
   * Enrich each entry of a playlist with information about the content. Intended to be used by endpoints when
   * returning information about a playlist. Currently only adds publication information for entries of type EVENT.
   * @param playlist The playlist to enrich
   * @return The serialization class of the playlist, since the added information does not belong to the playlist
   * itself.
   */
  public JaxbPlaylist enrich(Playlist playlist) {
    var jaxbPlaylist = new JaxbPlaylist(playlist);
    var org = securityService.getOrganization().getId();
    var user = securityService.getUser();

    // Add additional infos about events
    List<JaxbPlaylistEntry> jaxbPlaylistEntries = jaxbPlaylist.getEntries();
    for (JaxbPlaylistEntry entry : jaxbPlaylistEntries) {
      String contentId = entry.getContentId();

      if (contentId == null || contentId.isEmpty()) {
        entry.setType(PlaylistEntryType.INACCESSIBLE);
        logger.warn("Entry {} has no content, marking as inaccessible", entry.getId());
        continue;
      }

      try {
        if (entry.getType() == PlaylistEntryType.EVENT) {

          // We only get an event from the index if we have permission to do so (and if it exists ofc)
          SearchResult<Event> result = elasticsearchIndex.getByQuery(
              new EventSearchQuery(org, user).withIdentifier(contentId));
          if (result.getPageSize() != 0) {
            Event event = result.getItems()[0].getSource();
            entry.setPublications(event.getPublications());
          } else {
            entry.setType(PlaylistEntryType.INACCESSIBLE);
          }
        }
      } catch (SearchIndexException e) {
        throw new RuntimeException(e);
      }
    }
    jaxbPlaylist.setEntries(jaxbPlaylistEntries);

    return jaxbPlaylist;
  }

  /**
   * Runs a permission check on the given playlist for the given action
   * @param playlist {@link Playlist} to check permission for
   * @param action Action to check permission for
   * @return True if action is permitted on the {@link Playlist}, else false
   */
  private boolean checkPermission(Playlist playlist, Permissions.Action action) {
    User currentUser = securityService.getUser();
    Organization currentOrg = securityService.getOrganization();
    String currentOrgAdminRole = currentOrg.getAdminRole();
    String currentOrgId = currentOrg.getId();

    return currentUser.hasRole(GLOBAL_ADMIN_ROLE)
        || (currentUser.hasRole(currentOrgAdminRole) && currentOrgId.equals(playlist.getOrganization()))
        || authorizationService.hasPermission(getAccessControlList(playlist), action.toString());
  }

  /**
   * Parse the access information for a playlist from its database format into an {@link AccessControlList}
   * @param playlist The {@link Playlist} to get the {@link AccessControlList} for
   * @return The {@link AccessControlList} for the given {@link Playlist}
   */
  private AccessControlList getAccessControlList(Playlist playlist) {
    List<AccessControlEntry> accessControlEntries = new ArrayList<>();
    for (PlaylistAccessControlEntry entry : playlist.getAccessControlEntries()) {
      accessControlEntries.add(entry.toAccessControlEntry());
    }
    return new AccessControlList(accessControlEntries);
  }
}
