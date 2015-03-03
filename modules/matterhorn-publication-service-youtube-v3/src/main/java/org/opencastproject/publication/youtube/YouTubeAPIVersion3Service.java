/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.publication.youtube;

import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import org.opencastproject.publication.youtube.auth.ClientCredentials;

import java.io.IOException;

/**
 * Provides convenient access to {@link com.google.api.services.youtube.YouTube} service.
 */
public interface YouTubeAPIVersion3Service {

  /**
   * Configure the underlying {@link com.google.api.services.youtube.YouTube} instance.
   * @param credentials may not be {@code null}
   * @throws IOException when configuration files not found.
   */
  void initialize(ClientCredentials credentials) throws IOException;

  /**
   * Search for videos on predefined channel.
   * @param queryTerm may not be {@code null}
   * @param pageToken may not be {@code null}
   * @param maxResults may not be {@code null}
   * @return zero or more results. Will not be {@code null}.
   * @throws IOException when search fails.
   */
  SearchListResponse searchMyVideos(String queryTerm, String pageToken, long maxResults) throws IOException;

  /**
   * Get video by id.
   * @param videoId may not be {@code null}
   * @return null when not found.
   * @throws IOException
   */
  Video getVideoById(String videoId) throws IOException;

  /**
   * Get playlist by title.
   * @param title may not be {@code null}
   * @return null when not found.
   * @throws IOException when lookup fails.
   */
  Playlist getMyPlaylistByTitle(String title) throws IOException;

  /**
   * Page through all YouTube playlists of predefined channel.
   * @param pageToken identifies a page in result-set.
   * @param maxResults limit on number of results.
   * @return zero or more {@link com.google.api.services.youtube.model.Playlist}
   * @throws IOException when lookup fails.
   */
  PlaylistListResponse getMyPlaylists(String pageToken, long maxResults) throws IOException;

  /**
   * Find YouTube playlist by id.
   * @param playlistId may not be {@code null}
   * @param pageToken may not be {@code null}
   * @param maxResults may not be {@code null}
   * @return will not be {@code null}
   * @throws IOException when lookup fails.
   */
  PlaylistItemListResponse getPlaylistItems(String playlistId, String pageToken, long maxResults) throws IOException;

  /**
   * Upload a video to predefined YouTube channel.
   * @param videoUpload may not be {@code null}
   * @return YouTube object with non-null id.
   * @throws IOException when transaction fails.
   */
  Video addVideoToMyChannel(VideoUpload videoUpload) throws IOException;

  /**
   * Add a previously uploaded video to specified YouTube playlist.
   * @param playlistId may not be {@code null}
   * @param videoId may not be {@code null}
   * @return YouTube object which describes mapping, with non-null id.
   * @throws IOException
   */
  PlaylistItem addPlaylistItem(String playlistId, String videoId) throws IOException;

  /**
   * Creates YouTube Playlist and adds it to the authorized account.
   * @param title may not be {@code null}
   * @param description may not be {@code null}
   * @param tags zero or more tags to be applied to playlist on YouTube.
   */
  Playlist createPlaylist(String title, String description, String... tags) throws IOException;

  /**
   * Remove a previously uploaded video from YouTube.
   * @param videoId may not be {@code null}
   * @throws Exception when transaction fails.
   */
  void removeMyVideo(String videoId) throws Exception;

  /**
   * Remove a previously uploaded video from specified YouTube playlist.
   * @param playlistId may not be {@code null}
   * @param videoId may not be {@code null}
   * @throws IOException when transaction fails.
   */
  void removeVideoFromPlaylist(String playlistId, String videoId) throws IOException;

  /**
   * Remove a previously created YouTube playlist.
   * @param playlistId may not be {@code null}
   * @throws IOException when transaction fails.
   */
  void removeMyPlaylist(String playlistId) throws IOException;

}
