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

import org.opencastproject.graphql.playlist.type.GqlPlaylistEntryType;
import org.opencastproject.graphql.playlist.type.resolver.PlaylistEntryTypeResolver;
import org.opencastproject.playlists.PlaylistEntry;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeResolver;

@GraphQLName(GqlPlaylistEntry.TYPE_NAME)
@GraphQLDescription("An entry in a playlist.")
@GraphQLTypeResolver(PlaylistEntryTypeResolver.class)
public interface GqlPlaylistEntry {

  String TYPE_NAME = "PlaylistEntry";

  PlaylistEntry getEntry();

  @GraphQLField
  default Long id() {
    return getEntry().getId();
  }

  @GraphQLField
  default String contentId() {
    return getEntry().getContentId();
  }

  @GraphQLField
  default GqlPlaylistEntryType type() {
    return getEntry().getType() != null ? GqlPlaylistEntryType.valueOf(getEntry().getType().name()) : null;
  }

}
