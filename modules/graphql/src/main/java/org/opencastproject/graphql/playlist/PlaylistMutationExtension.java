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

package org.opencastproject.graphql.playlist;

import org.opencastproject.graphql.command.DeletePlaylistCommand;
import org.opencastproject.graphql.directive.RolesAllowed;
import org.opencastproject.graphql.playlist.command.CreatePlaylistCommand;
import org.opencastproject.graphql.playlist.command.UpdatePlaylistCommand;
import org.opencastproject.graphql.playlist.type.input.PlaylistEntryInput;
import org.opencastproject.graphql.playlist.type.input.PlaylistMetadataInput;
import org.opencastproject.graphql.playlist.type.output.GqlDeletePlaylistPayload;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylist;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.graphql.type.input.Mutation;

import java.util.List;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(Mutation.class)
public final class PlaylistMutationExtension {

  private PlaylistMutationExtension() {
  }

  @GraphQLField
  @GraphQLDescription("Create playlist from metadata, entries and acl")
  @RolesAllowed("ROLE_USER")
  public static GqlPlaylist createPlaylist(
      @GraphQLName("metadata") @GraphQLNonNull PlaylistMetadataInput metadata,
      @GraphQLName("entries") List<PlaylistEntryInput> entries,
      @GraphQLName("acl") @GraphQLNonNull AccessControlListInput acl,
      final DataFetchingEnvironment environment) {
    return CreatePlaylistCommand.create(metadata, entries, acl)
        .environment(environment)
        .build()
        .execute();
  }

  @GraphQLField
  @GraphQLDescription("Update playlist with metadata, entries and acl")
  @RolesAllowed("ROLE_USER")
  public static GqlPlaylist updatePlaylist(
      @GraphQLName("id") @GraphQLNonNull String id,
      @GraphQLName("metadata") PlaylistMetadataInput metadata,
      @GraphQLName("entries") List<PlaylistEntryInput> entries,
      @GraphQLName("acl") AccessControlListInput acl,
      final DataFetchingEnvironment environment) {
    return UpdatePlaylistCommand.create(id, metadata, entries, acl)
        .environment(environment)
        .build()
        .execute();
  }

  @GraphQLField
  @GraphQLDescription("Delete playlist")
  @RolesAllowed("ROLE_USER")
  public static GqlDeletePlaylistPayload deletePlaylist(
      @GraphQLName("id") @GraphQLNonNull String id,
      final DataFetchingEnvironment environment) {
    return DeletePlaylistCommand.create(id)
        .environment(environment)
        .build()
        .execute();
  }

}
