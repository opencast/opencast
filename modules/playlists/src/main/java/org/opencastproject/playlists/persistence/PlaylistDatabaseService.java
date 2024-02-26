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
package org.opencastproject.playlists.persistence;

import org.opencastproject.playlists.Playlist;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import java.util.List;

/**
 * API that defines persistent storage of playlists
 */
public interface PlaylistDatabaseService {

  /**
   * Gets a single playlist in the current organization context by its identifier.
   * @param playlistId the playlist identifier
   * @return the {@link Playlist} with the given identifier
   * @throws NotFoundException if there is no playlist  with this identifier
   * @throws PlaylistDatabaseException if there is a problem communicating with the underlying data store
   */
  Playlist getPlaylist(String playlistId) throws NotFoundException, PlaylistDatabaseException;

  /**
   * Gets a single playlist by its identifier.
   * @param playlistId the playlist identifier
   * @param orgId the organisation identifier
   * @return the {@link Playlist} with the given identifier
   * @throws NotFoundException if there is no playlist  with this identifier
   * @throws PlaylistDatabaseException if there is a problem communicating with the underlying data store
   */
  Playlist getPlaylist(String playlistId, String orgId) throws NotFoundException, PlaylistDatabaseException;

  /**
   * Get several playlists based on their order in the database
   * @param limit Maximum amount of playlists to return
   * @param offset The index of the first result to return
   * @return a list of {@link Playlist}s
   * @throws PlaylistDatabaseException if there is a problem communicating with the underlying data store
   */
  List<Playlist> getPlaylists(int limit, int offset, SortCriterion sortCriterion)
          throws PlaylistDatabaseException;

  /**
   * Creates or updates a single playlist.
   * @param playlist The {@link Playlist}
   * @return The updated {@link Playlist}
   * @throws PlaylistDatabaseException if there is a problem communicating with the underlying data store
   */
  Playlist updatePlaylist(Playlist playlist, String orgId) throws PlaylistDatabaseException;

  /**
   * Removes a single playlist.
   * @param playlist The {@link Playlist}
   * @return The deleted {@link Playlist}
   * @throws PlaylistDatabaseException if there is a problem communicating with the underlying data store
   */
  Playlist deletePlaylist(Playlist playlist, String orgId) throws PlaylistDatabaseException;
}
