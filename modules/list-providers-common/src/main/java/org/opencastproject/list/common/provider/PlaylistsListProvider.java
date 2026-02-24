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

package org.opencastproject.list.common.provider;

import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List provider for playlists.
 * Supplies distinct creator names for the creator filter dropdown on the playlists page.
 * Todo: Add more attributes.
 */
@Component(
    service = ResourceListProvider.class,
    property = {
        "service.description=Playlists list provider",
        "opencast.service.type=org.opencastproject.list.common.provider.PlaylistsListProvider"
    }
)
public class PlaylistsListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(PlaylistsListProvider.class);

  /** Provider name referenced by {@code PlaylistsListQuery.createCreatorFilter}. */
  public static final String PROVIDER_NAME = "PLAYLISTS.CREATORS";

  private static final String[] NAMES = { PROVIDER_NAME };

  private PlaylistService playlistService;

  @Reference
  public void setPlaylistService(PlaylistService playlistService) {
    this.playlistService = playlistService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> result = new LinkedHashMap<>();
    try {
      List<Playlist> playlists = playlistService.getPlaylists(Integer.MAX_VALUE, 0);
      playlists.stream()
          .map(Playlist::getCreator)
          .filter(c -> c != null && !c.isBlank())
          .distinct()
          .sorted(String.CASE_INSENSITIVE_ORDER)
          .forEach(c -> result.put(c, c));
    } catch (Exception e) {
      logger.warn("Could not retrieve playlist creators: {}", e.getMessage());
    }
    return result;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
