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

import org.opencastproject.graphql.playlist.type.output.GqlEventPlaylistEntry;
import org.opencastproject.graphql.playlist.type.output.GqlInaccessiblePlaylistEntry;
import org.opencastproject.graphql.provider.GraphQLAdditionalTypeProvider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Set;

@Component
@ServiceDescription("Provides additional GraphQL types for Playlists")
public class PlaylistGraphQLProvider implements GraphQLAdditionalTypeProvider {

  @Override
  public Set<Class<?>> getAdditionalOutputTypes() {
    return Set.of(
        GqlEventPlaylistEntry.class,
        GqlInaccessiblePlaylistEntry.class
    );
  }

}
