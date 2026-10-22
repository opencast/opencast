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

package org.opencastproject.graphql.playlist.datafetcher;

import org.opencastproject.graphql.datafetcher.OpenSearchDataFetcher;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylistList;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.playlists.serialization.JaxbPlaylist;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.User;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;

public class PlaylistOffsetDataFetcher extends OpenSearchDataFetcher<GqlPlaylistList> {

  private User user;
  private boolean writeOnly;

  public PlaylistOffsetDataFetcher withUser(User user) {
    this.user = user;
    return this;
  }

  public PlaylistOffsetDataFetcher writeOnly(boolean writeOnly) {
    this.writeOnly = writeOnly;
    return this;
  }

  @Override
  public GqlPlaylistList get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    PlaylistService playlistService = opencastContext.getService(PlaylistService.class);
    AuthorizationService authorizationService = opencastContext.getService(AuthorizationService.class);

    int limit = parseParam("limit", DEFAULT_PAGE_SIZE, dataFetchingEnvironment);
    int offset = parseParam("offset", 0, dataFetchingEnvironment);

    List<Playlist> playlists = playlistService.getPlaylists(limit, offset).stream()
        .filter(p -> !writeOnly || hasWritePermission(p, authorizationService))
        .map(playlistService::enrich)
        .map(JaxbPlaylist::toPlaylist)
        .toList();

    return new GqlPlaylistList(playlists, limit, offset);
  }

  private boolean hasWritePermission(Playlist playlist, AuthorizationService authorizationService) {
    AccessControlList acl = new AccessControlList(playlist.getAccessControlEntries().stream()
        .map(PlaylistAccessControlEntry::toAccessControlEntry)
        .toList());

    return user.getOrganization().getId().equals(playlist.getOrganization())
        && authorizationService.hasPermission(acl, Permissions.Action.WRITE.toString());
  }
}
