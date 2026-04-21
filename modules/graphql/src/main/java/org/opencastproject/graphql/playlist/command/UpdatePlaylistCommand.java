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

package org.opencastproject.graphql.playlist.command;

import org.opencastproject.graphql.command.AbstractCommand;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.playlist.type.input.PlaylistEntryInput;
import org.opencastproject.graphql.playlist.type.input.PlaylistMetadataInput;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylist;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to update a playlist from GraphQL inputs (metadata, entries, acl).
 */
public class UpdatePlaylistCommand extends AbstractCommand<GqlPlaylist> {

  private final String playlistId;
  private final PlaylistMetadataInput metadataInput;
  private final List<PlaylistEntryInput> entriesInput;
  private final AccessControlListInput aclInput;

  public UpdatePlaylistCommand(final Builder builder) {
    super(builder);
    this.playlistId = builder.playlistId;
    this.metadataInput = builder.metadataInput;
    this.entriesInput = builder.entriesInput;
    this.aclInput = builder.aclInput;
  }

  @Override
  public GqlPlaylist execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final PlaylistService playlistService = context.getService(PlaylistService.class);
    try {
      Playlist playlist = playlistService.getPlaylistById(playlistId);

      if (metadataInput != null) {
        if (metadataInput.getTitle() != null) {
          playlist.setTitle(metadataInput.getTitle());
        }
        if (metadataInput.getDescription() != null) {
          playlist.setDescription(metadataInput.getDescription());
        }
      }

      if (entriesInput != null) {
        List<PlaylistEntry> playlistEntries = new ArrayList<>();
        for (var entryInput : entriesInput) {
          playlistEntries.add(new PlaylistEntry(entryInput.getContentId(), entryInput.getType().getType()));
        }
        playlist.setEntries(playlistEntries);
      }

      if (aclInput != null) {
        playlist.setAccessControlEntries(PlaylistCommandHelper.toPlaylistAce(aclInput, context));
      }

      playlist = playlistService.update(playlist);

      return new GqlPlaylist(playlist);
    } catch (UnauthorizedException e) {
      throw new GraphQLUnauthorizedException(e.getMessage());
    } catch (RuntimeException e) {
      throw new GraphQLRuntimeException(e);
    } catch (NotFoundException e) {
      throw new GraphQLNotFoundException(e.getMessage());
    }
  }

  public static Builder create(
      String playlistId,
      PlaylistMetadataInput metadataInput,
      List<PlaylistEntryInput> entriesInput,
      AccessControlListInput accessControlListInput) {
    return new Builder(playlistId, metadataInput, entriesInput, accessControlListInput);
  }

  public static class Builder extends AbstractCommand.Builder<GqlPlaylist> {

    private final String playlistId;
    private final PlaylistMetadataInput metadataInput;
    private final List<PlaylistEntryInput> entriesInput;
    private final AccessControlListInput aclInput;

    public Builder(String playlistId, PlaylistMetadataInput metadataInput, List<PlaylistEntryInput> entriesInput,
                   AccessControlListInput aclInput) {
      this.playlistId = playlistId;
      this.metadataInput = metadataInput;
      this.entriesInput = entriesInput;
      this.aclInput = aclInput;
    }

    @Override
    public void validate() {
      super.validate();
      if (playlistId == null || playlistId.isEmpty()) {
        throw new IllegalStateException("Playlist ID cannot be null or empty");
      }
    }

    @Override
    public UpdatePlaylistCommand build() {
      validate();
      return new UpdatePlaylistCommand(this);
    }
  }

}
