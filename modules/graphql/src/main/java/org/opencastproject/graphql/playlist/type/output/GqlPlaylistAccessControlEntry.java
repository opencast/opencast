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

import org.opencastproject.playlists.PlaylistAccessControlEntry;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("PlaylistAccessControlEntry")
@GraphQLDescription("An access control entry for a playlist.")
public class GqlPlaylistAccessControlEntry {

  private final PlaylistAccessControlEntry ace;

  public GqlPlaylistAccessControlEntry(PlaylistAccessControlEntry ace) {
    this.ace = ace;
  }

  @GraphQLField
  public String role() {
    return ace.getRole();
  }

  @GraphQLField
  public String action() {
    return ace.getAction();
  }

  @GraphQLField
  public Boolean allow() {
    return ace.isAllow();
  }

}
