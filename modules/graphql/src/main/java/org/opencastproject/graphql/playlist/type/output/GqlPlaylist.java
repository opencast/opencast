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

import org.opencastproject.graphql.type.DateTimeFunction;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLType;

@GraphQLName(GqlPlaylist.TYPE_NAME)
@GraphQLDescription("A playlist of events.")
public class GqlPlaylist {

  public static final String TYPE_NAME = "Playlist";

  private final Playlist playlist;

  public GqlPlaylist(Playlist playlist) {
    this.playlist = playlist;
  }

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  public String id() {
    return playlist.getId();
  }

  @GraphQLField
  public LinkedList<GqlPlaylistEntry> entries() {
    List<PlaylistEntry> entries = playlist.getEntries();
    if (entries == null) {
      return new LinkedList<>();
    }
    return entries.stream().map(e -> switch (e.getType()) {
      case EVENT -> new GqlEventPlaylistEntry(e);
      case INACCESSIBLE -> new GqlInaccessiblePlaylistEntry(e);
    }).collect(Collectors.toCollection(LinkedList::new));
  }

  @GraphQLField
  public String title() {
    return playlist.getTitle();
  }

  @GraphQLField
  public String description() {
    return playlist.getDescription();
  }

  @GraphQLField
  public String creator() {
    return playlist.getCreator();
  }

  @GraphQLField
  @GraphQLType(DateTimeFunction.class)
  public OffsetDateTime updated() {
    return playlist.getUpdated().toInstant().atOffset(ZoneOffset.UTC);
  }

  @GraphQLField
  public LinkedList<GqlPlaylistAccessControlEntry> accessControlEntries() {
    List<PlaylistAccessControlEntry> aces = playlist.getAccessControlEntries();
    if (aces == null) {
      return new LinkedList<>();
    }
    return aces.stream().map(GqlPlaylistAccessControlEntry::new)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public Playlist getPlaylist() {
    return playlist;
  }

}
