/**
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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.publication.youtube

import org.opencastproject.publication.youtube.auth.ClientCredentials

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.Video

import java.io.IOException

/**
 * Provides convenient access to [com.google.api.services.youtube.YouTube] service.
 */
interface YouTubeAPIVersion3Service {

    /**
     * Configure the underlying [com.google.api.services.youtube.YouTube] instance.
     * @param credentials may not be `null`
     * @throws IOException when configuration files not found.
     */
    @Throws(IOException::class)
    fun initialize(credentials: ClientCredentials)

    /**
     * Search for videos on predefined channel.
     * @param queryTerm may not be `null`
     * @param pageToken may not be `null`
     * @param maxResults may not be `null`
     * @return zero or more results. Will not be `null`.
     * @throws IOException when search fails.
     */
    @Throws(IOException::class)
    fun searchMyVideos(queryTerm: String, pageToken: String, maxResults: Long): SearchListResponse

    /**
     * Get video by id.
     * @param videoId may not be `null`
     * @return null when not found.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getVideoById(videoId: String): Video

    /**
     * Get playlist by title.
     * @param title may not be `null`
     * @return null when not found.
     * @throws IOException when lookup fails.
     */
    @Throws(IOException::class)
    fun getMyPlaylistByTitle(title: String): Playlist

    /**
     * Page through all YouTube playlists of predefined channel.
     * @param pageToken identifies a page in result-set.
     * @param maxResults limit on number of results.
     * @return zero or more [com.google.api.services.youtube.model.Playlist]
     * @throws IOException when lookup fails.
     */
    @Throws(IOException::class)
    fun getMyPlaylists(pageToken: String, maxResults: Long): PlaylistListResponse

    /**
     * Find YouTube playlist by id.
     * @param playlistId may not be `null`
     * @param pageToken may not be `null`
     * @param maxResults may not be `null`
     * @return will not be `null`
     * @throws IOException when lookup fails.
     */
    @Throws(IOException::class)
    fun getPlaylistItems(playlistId: String, pageToken: String, maxResults: Long): PlaylistItemListResponse

    /**
     * Upload a video to predefined YouTube channel.
     * @param videoUpload may not be `null`
     * @return YouTube object with non-null id.
     * @throws IOException when transaction fails.
     */
    @Throws(IOException::class)
    fun addVideoToMyChannel(videoUpload: VideoUpload): Video

    /**
     * Add a previously uploaded video to specified YouTube playlist.
     * @param playlistId may not be `null`
     * @param videoId may not be `null`
     * @return YouTube object which describes mapping, with non-null id.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun addPlaylistItem(playlistId: String, videoId: String): PlaylistItem

    /**
     * Creates YouTube Playlist and adds it to the authorized account.
     * @param title may not be `null`
     * @param description may not be `null`
     * @param tags zero or more tags to be applied to playlist on YouTube.
     */
    @Throws(IOException::class)
    fun createPlaylist(title: String, description: String, vararg tags: String): Playlist

    /**
     * Remove a previously uploaded video from YouTube.
     * @param videoId may not be `null`
     * @throws Exception when transaction fails.
     */
    @Throws(Exception::class)
    fun removeMyVideo(videoId: String)

    /**
     * Remove a previously uploaded video from specified YouTube playlist.
     * @param playlistId may not be `null`
     * @param videoId may not be `null`
     * @throws IOException when transaction fails.
     */
    @Throws(IOException::class)
    fun removeVideoFromPlaylist(playlistId: String, videoId: String)

    /**
     * Remove a previously created YouTube playlist.
     * @param playlistId may not be `null`
     * @throws IOException when transaction fails.
     */
    @Throws(IOException::class)
    fun removeMyPlaylist(playlistId: String)

}
