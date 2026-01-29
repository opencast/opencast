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

import org.opencastproject.graphql.playlist.datafetcher.PlaylistDataFetcher;
import org.opencastproject.graphql.playlist.datafetcher.PlaylistOffsetDataFetcher;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylist;
import org.opencastproject.graphql.playlist.type.output.GqlPlaylistList;
import org.opencastproject.graphql.type.input.PlaylistOrderByInput;
import org.opencastproject.graphql.type.output.Query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(Query.class)
public final class PlaylistQueryExtension {

  private PlaylistQueryExtension() {
  }

  @SuppressWarnings("unused")
  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Returns playlist list")
  public static GqlPlaylistList allPlaylists(
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("query") String query,
      @GraphQLName("orderBy") PlaylistOrderByInput orderBy,
      final DataFetchingEnvironment environment) {
    return new PlaylistOffsetDataFetcher().get(environment);
  }

  @SuppressWarnings("unused")
  @GraphQLField
  @GraphQLDescription("Returns a playlist by id")
  public static GqlPlaylist playlistById(
      @GraphQLName("id") @GraphQLNonNull String id,
      final DataFetchingEnvironment environment) {
    return new PlaylistDataFetcher(id).get(environment);
  }

}
