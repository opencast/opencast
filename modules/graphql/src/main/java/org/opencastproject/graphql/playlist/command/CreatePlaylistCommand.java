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
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to create a playlist from GraphQL inputs (metadata, entries, acl).
 */
public class CreatePlaylistCommand extends AbstractCommand<GqlPlaylist> {

  private final PlaylistMetadataInput metadataInput;
  private final List<PlaylistEntryInput> entriesInput;
  private final AccessControlListInput aclInput;

  public CreatePlaylistCommand(final Builder builder) {
    super(builder);
    this.metadataInput = builder.metadataInput;
    this.entriesInput = builder.entriesInput;
    this.aclInput = builder.aclInput;
  }

  @Override
  public GqlPlaylist execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final PlaylistService playlistService = context.getService(PlaylistService.class);
    final SecurityService securityService = context.getService(SecurityService.class);
    try {
      Playlist playlist = new Playlist();
      if (metadataInput != null) {
        playlist.setTitle(metadataInput.getTitle());
        playlist.setDescription(metadataInput.getDescription());
        playlist.setCreator(securityService.getUser().getName());
      }

      List<PlaylistEntry> playlistEntries = new ArrayList<>();
      if (entriesInput != null) {
        for (var entryInput : entriesInput) {
          playlistEntries.add(new PlaylistEntry(entryInput.getContentId(), entryInput.getType().getType()));
        }
      }
      playlist.setEntries(playlistEntries);

      playlist.setAccessControlEntries(PlaylistCommandHelper.toPlaylistAce(aclInput, context));

      playlist = playlistService.update(playlist);

      return new GqlPlaylist(playlist);
    } catch (UnauthorizedException e) {
      throw new GraphQLUnauthorizedException(e.getMessage());
    } catch (RuntimeException e) {
      throw new GraphQLRuntimeException(e);
    }
  }

  public static Builder create(
      PlaylistMetadataInput metadataInput,
      List<PlaylistEntryInput> entriesInput,
      AccessControlListInput accessControlListInput) {
    return new Builder(metadataInput, entriesInput, accessControlListInput);
  }

  public static class Builder extends AbstractCommand.Builder<GqlPlaylist> {

    private final PlaylistMetadataInput metadataInput;
    private final List<PlaylistEntryInput> entriesInput;
    private final AccessControlListInput aclInput;

    public Builder(PlaylistMetadataInput metadataInput, List<PlaylistEntryInput> entriesInput,
                   AccessControlListInput aclInput) {
      this.metadataInput = metadataInput;
      this.entriesInput = entriesInput;
      this.aclInput = aclInput;
    }

    @Override
    public void validate() {
      super.validate();
      if (metadataInput == null) {
        throw new IllegalStateException("Playlist metadata cannot be null");
      }
      if (aclInput == null) {
        throw new IllegalStateException("Access control list cannot be null");
      }
    }

    @Override
    public CreatePlaylistCommand build() {
      validate();
      return new CreatePlaylistCommand(this);
    }
  }

}
