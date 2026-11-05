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

package org.opencastproject.publication.youtube;

import org.opencastproject.publication.youtube.auth.ClientCredentials;

import com.google.api.services.youtube.model.Caption;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.Video;

import java.io.File;
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
   * Set a thumbnail for a video.
   * @param videoId the video id
   * @param thumbnail the thumbnail file
   * @param mimeType the thumbnail mime type
   * @throws IOException when transaction fails.
   */
  void setThumbnail(String videoId, File thumbnail, String mimeType) throws IOException;

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
   * @param privacyStatus may not be {@code null}
   * @param tags zero or more tags to be applied to playlist on YouTube.
   */
  Playlist createPlaylist(String title, String description, PrivacyStatus privacyStatus, String... tags)
          throws IOException;

  /**
   * Whether the underlying {@link com.google.api.services.youtube.YouTube} client has been configured with
   * credentials via {@link #initialize(ClientCredentials)}.
   * @return {@code true} once initialized.
   */
  boolean isInitialized();

  /**
   * Creates a YouTube playlist with an explicit privacy status and adds it to the authorized account.
   * @param title may not be {@code null}
   * @param description may not be {@code null}
   * @param privacyStatus may not be {@code null}; one of "public", "unlisted" or "private"
   * @return the newly created playlist, with non-null id.
   * @throws IOException when transaction fails.
   */
  Playlist createPlaylistWithPrivacyStatus(String title, String description, String privacyStatus)
          throws IOException;

  /**
   * Find a playlist by id.
   * @param playlistId may not be {@code null}
   * @return null when not found.
   * @throws IOException when lookup fails.
   */
  Playlist getPlaylistById(String playlistId) throws IOException;

  /**
   * Update the title and/or description of an existing playlist.
   * @param playlistId may not be {@code null}
   * @param title new title, or {@code null} to leave unchanged
   * @param description new description, or {@code null} to leave unchanged
   * @return the updated playlist, or {@code null} when the playlist does not exist.
   * @throws IOException when the update fails.
   */
  Playlist updatePlaylist(String playlistId, String title, String description) throws IOException;

  /**
   * Find the playlist item which links the given video to the given playlist.
   * @param playlistId may not be {@code null}
   * @param videoId may not be {@code null}
   * @return null when the video is not (yet) part of the playlist.
   * @throws IOException when lookup fails.
   */
  PlaylistItem findPlaylistItem(String playlistId, String videoId) throws IOException;

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

  /**
   * Add captions to a video.
   * @param videoId the video id
   * @param file the caption file
   * @param language the language of the captions
   * @param name the name of the captions
   * @return the created caption object
   * @throws IOException when transaction fails.
   */
  Caption addCaptions(String videoId, File file, String language, String name) throws IOException;

  /** Valid values for YouTube privacy statuses */
  enum PrivacyStatus {
    PUBLIC,
    PRIVATE,
    UNLISTED
  }
}
