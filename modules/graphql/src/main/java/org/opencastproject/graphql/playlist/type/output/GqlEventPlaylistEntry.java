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

import org.opencastproject.graphql.datafetcher.event.EventDataFetcher;
import org.opencastproject.graphql.event.GqlEvent;
import org.opencastproject.playlists.PlaylistEntry;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.schema.DataFetchingEnvironment;

@GraphQLName(GqlEventPlaylistEntry.TYPE_NAME)
@GraphQLDescription("An entry in a playlist.")
public class GqlEventPlaylistEntry implements GqlPlaylistEntry {

  public static final String TYPE_NAME = "EventPlaylistEntry";

  private final PlaylistEntry entry;

  public GqlEventPlaylistEntry(PlaylistEntry entry) {
    this.entry = entry;
  }

  @Override
  public PlaylistEntry getEntry() {
    return entry;
  }

  @GraphQLField
  public GqlEvent event(final DataFetchingEnvironment environment) {
    var event = new EventDataFetcher(contentId()).get(environment);
    if (event == null) {
      return null;
    }
    return new GqlEvent(event.getEvent());
  }

}
