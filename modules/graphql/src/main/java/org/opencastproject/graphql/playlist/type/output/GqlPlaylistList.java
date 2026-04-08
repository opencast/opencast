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

package org.opencastproject.graphql.playlist.type.output;

import org.opencastproject.graphql.type.output.OffsetPageInfo;
import org.opencastproject.playlists.Playlist;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName(GqlPlaylistList.TYPE_NAME)
@GraphQLNonNull
@GraphQLDescription("A list of playlists")
public class GqlPlaylistList {

  public static final String TYPE_NAME = "PlaylistList";

  private final List<Playlist> playlists;
  private final long totalCount;
  private final long limit;
  private final long offset;

  public GqlPlaylistList(List<Playlist> playlists, int limit, int offset) {
    this.playlists = playlists;
    this.totalCount = playlists == null ? 0L : playlists.size();
    this.limit = limit;
    this.offset = offset;
  }

  @GraphQLField
  @GraphQLNonNull
  public Long totalCount() {
    return totalCount;
  }

  @GraphQLField
  @GraphQLNonNull
  public OffsetPageInfo pageInfo() {
    long pageCount = limit > 0 ? (totalCount + limit - 1) / limit : 0L;
    return new OffsetPageInfo(pageCount, limit, offset);
  }

  @GraphQLField
  @GraphQLNonNull
  public List<GqlPlaylist> nodes() {
    if (playlists == null) {
      return Collections.emptyList();
    }
    return playlists.stream().map(GqlPlaylist::new).collect(Collectors.toList());
  }

}
