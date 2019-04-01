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
import org.opencastproject.publication.youtube.auth.OAuth2CredentialFactory
import org.opencastproject.publication.youtube.auth.OAuth2CredentialFactoryImpl
import org.opencastproject.util.data.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequest
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.PlaylistStatus
import com.google.api.services.youtube.model.ResourceId
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoListResponse
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus

import org.apache.commons.lang3.ArrayUtils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.LinkedList

class YouTubeAPIVersion3ServiceImpl : YouTubeAPIVersion3Service {

    private var youTube: YouTube? = null

    @Throws(IOException::class)
    override fun initialize(credentials: ClientCredentials) {
        val factory = OAuth2CredentialFactoryImpl()
        val credential = factory.getGoogleCredential(credentials)
        youTube = YouTube.Builder(NetHttpTransport(), JacksonFactory(), credential).build()
    }

    @Throws(IOException::class)
    override fun addVideoToMyChannel(videoUpload: VideoUpload): Video {
        val videoObjectDefiningMetadata = Video()
        val status = VideoStatus()
        status.privacyStatus = videoUpload.privacyStatus
        videoObjectDefiningMetadata.status = status
        // Metadata lives in VideoSnippet
        val snippet = VideoSnippet()
        snippet.title = videoUpload.title
        snippet.description = videoUpload.description
        val tags = videoUpload.tags
        if (ArrayUtils.isNotEmpty(tags)) {
            snippet.tags = Collections.list(*tags)
        }
        // Attach metadata to video object.
        videoObjectDefiningMetadata.snippet = snippet

        val videoFile = videoUpload.videoFile
        val inputStream = BufferedInputStream(FileInputStream(videoFile))
        val mediaContent = InputStreamContent("video/*", inputStream)
        mediaContent.length = videoFile.length()

        val videoInsert = youTube!!.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent)
        val uploader = videoInsert.mediaHttpUploader

        uploader.isDirectUploadEnabled = false
        uploader.progressListener = videoUpload.progressListener
        return execute(videoInsert)
    }

    @Throws(IOException::class)
    override fun createPlaylist(title: String, description: String, vararg tags: String): Playlist {
        val playlistSnippet = PlaylistSnippet()
        playlistSnippet.title = title
        playlistSnippet.description = description
        if (tags.size > 0) {
            playlistSnippet.tags = Collections.list(*tags)
        }
        // Playlists are always public. The videos therein might be private.
        val playlistStatus = PlaylistStatus()
        playlistStatus.privacyStatus = "public"

        // Create playlist with metadata and status.
        val youTubePlaylist = Playlist()
        youTubePlaylist.snippet = playlistSnippet
        youTubePlaylist.status = playlistStatus

        // The first argument tells the API what to return when a successful insert has been executed.
        val command = youTube!!.playlists().insert("snippet,status", youTubePlaylist)
        return execute(command)
    }

    @Throws(IOException::class)
    override fun addPlaylistItem(playlistId: String, videoId: String): PlaylistItem {
        // Resource type (video,playlist,channel) needs to be set along with resource id.
        val resourceId = ResourceId()
        resourceId.kind = "youtube#video"
        resourceId.videoId = videoId

        // Set the required snippet properties.
        val playlistItemSnippet = PlaylistItemSnippet()
        playlistItemSnippet.title = "First video in the test playlist"
        playlistItemSnippet.playlistId = playlistId
        playlistItemSnippet.resourceId = resourceId

        // Create the playlist item.
        val playlistItem = PlaylistItem()
        playlistItem.snippet = playlistItemSnippet

        // The first argument tells the API what to return when a successful insert has been executed.
        val playlistItemsInsertCommand = youTube!!.playlistItems().insert("snippet,contentDetails", playlistItem)
        return execute(playlistItemsInsertCommand)
    }

    @Throws(IOException::class)
    override fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        val playlistItem = findPlaylistItem(playlistId, videoId)
        val deleteRequest = youTube!!.playlistItems().delete(playlistItem!!.id)
        execute(deleteRequest)
    }

    @Throws(Exception::class)
    override fun removeMyVideo(videoId: String) {
        val deleteRequest = youTube!!.videos().delete(videoId)
        execute(deleteRequest)
    }

    @Throws(IOException::class)
    override fun removeMyPlaylist(playlistId: String) {
        val deleteRequest = youTube!!.playlists().delete(playlistId)
        execute(deleteRequest)
    }

    @Throws(IOException::class)
    override fun searchMyVideos(queryTerm: String, pageToken: String?, maxResults: Long): SearchListResponse {
        val search = youTube!!.search().list("id,snippet")
        if (pageToken != null) {
            search.set("pageToken", pageToken)
        }
        search.q = queryTerm
        search.type = "video"
        search.forMine = true
        search.maxResults = maxResults
        search.fields = "items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination"
        return execute(search)
    }

    @Throws(IOException::class)
    override fun getVideoById(videoId: String): Video? {
        val search = youTube!!.videos().list("id,snippet")
        search.id = videoId
        search.fields = "items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination"
        val response = execute(search)
        return if (response.items.isEmpty()) null else response.items[0]
    }

    @Throws(IOException::class)
    override fun getMyPlaylistByTitle(title: String): Playlist? {
        val trimmedTitle = title.trim { it <= ' ' }
        var searchedAllPlaylist = false
        var playlist: Playlist? = null
        var nextPageToken: String? = null
        while (!searchedAllPlaylist) {
            val searchResult = getMyPlaylists(nextPageToken, 50)
            for (p in searchResult.items) {
                if (p.snippet.title.trim { it <= ' ' } == trimmedTitle) {
                    playlist = p
                    break
                }
            }
            nextPageToken = searchResult.nextPageToken
            searchedAllPlaylist = nextPageToken == null
        }
        return playlist
    }

    @Throws(IOException::class)
    override fun getMyPlaylists(pageToken: String?, maxResults: Long): PlaylistListResponse {
        val search = youTube!!.playlists().list("id,snippet")
        if (pageToken != null) {
            search.set("pageToken", pageToken)
        }
        search.maxResults = maxResults
        search.mine = true
        search.fields = "items(id,snippet),kind,nextPageToken,pageInfo,prevPageToken,tokenPagination"
        return execute(search)
    }

    @Throws(IOException::class)
    override fun getPlaylistItems(playlistId: String, pageToken: String?, maxResults: Long): PlaylistItemListResponse {
        val search = youTube!!.playlistItems().list("id,snippet")
        search.playlistId = playlistId
        search.pageToken = pageToken
        search.maxResults = maxResults
        search.fields = "items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination"
        return execute(search)
    }

    /**
     * @param playlistId may not be `null`
     * @param videoId may not be `null`
     * @return null when not found.
     * @throws IOException when transaction fails.
     */
    @Throws(IOException::class)
    private fun findPlaylistItem(playlistId: String, videoId: String): PlaylistItem? {
        val playlistItems = getAllPlaylistItems(playlistId)
        var playlistItem: PlaylistItem? = null
        for (next in playlistItems) {
            val id = next.snippet.resourceId.videoId
            if (videoId == id) {
                playlistItem = next
                break
            }
        }
        return playlistItem
    }

    /**
     *
     * @param playlistId may not be `null`
     * @return zero or more playlist items.
     * @throws IOException  when transaction fails.
     */
    @Throws(IOException::class)
    private fun getAllPlaylistItems(playlistId: String): List<PlaylistItem> {
        val playlistItems = LinkedList<PlaylistItem>()
        var done = false
        var nextPageToken: String? = null
        while (!done) {
            val searchResult = getPlaylistItems(playlistId, nextPageToken, 50)
            playlistItems.addAll(searchResult.items)
            nextPageToken = searchResult.nextPageToken
            done = nextPageToken == null
        }
        return playlistItems
    }

    /**
     * @see com.google.api.services.youtube.YouTubeRequest.execute
     * @param command may not be `null`
     * @param <T> type of request.
     * @return result; may be `null`
     * @throws IOException  when transaction fails.
    </T> */
    @Throws(IOException::class)
    private fun <T> execute(command: YouTubeRequest<T>): T {
        return command.execute()
    }
}
