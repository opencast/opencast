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

import org.opencastproject.graphql.datafetcher.ContextDataFetcher;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.OpencastErrorType;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylist;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

public class PlaylistDataFetcher implements ContextDataFetcher<GqlPlaylist> {

  private final String playlistId;

  public PlaylistDataFetcher(String playlistId) {
    Objects.requireNonNull(playlistId, "Playlist identifier must not be null.");
    this.playlistId = playlistId;
  }

  @Override
  public GqlPlaylist get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    PlaylistService playlistService = opencastContext.getService(PlaylistService.class);

    try {
      Playlist playlist = playlistService.getPlaylistById(playlistId);
      if (playlist == null) {
        return null;
      }
      try {
        //playlist = playlistService.enrich(playlist).toPlaylist();
      } catch (Throwable ignored) {
      }
      return new GqlPlaylist(playlist);
    } catch (NotFoundException e) {
      return null;
    } catch (UnauthorizedException e) {
      throw new GraphQLRuntimeException("Unauthorized", OpencastErrorType.Unauthorized, e);
    } catch (Exception e) {
      throw new GraphQLRuntimeException("Error fetching playlist", OpencastErrorType.InternalError, e);
    }
  }
}

