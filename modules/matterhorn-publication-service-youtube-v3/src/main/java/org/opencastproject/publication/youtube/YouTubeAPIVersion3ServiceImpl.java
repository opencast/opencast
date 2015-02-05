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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.apache.commons.lang.ArrayUtils;
import org.opencastproject.publication.youtube.auth.ClientCredentials;
import org.opencastproject.publication.youtube.auth.OAuth2CredentialFactory;
import org.opencastproject.publication.youtube.auth.OAuth2CredentialFactoryImpl;
import org.opencastproject.util.data.Collections;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author John Crossman
 */
public class YouTubeAPIVersion3ServiceImpl implements YouTubeAPIVersion3Service {

  private YouTube youTube;

  @Override
  public void initialize(final ClientCredentials credentials) throws IOException {
    final OAuth2CredentialFactory factory = new OAuth2CredentialFactoryImpl();
    final GoogleCredential credential = factory.getGoogleCredential(credentials);
    youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
  }

  @Override
  public Video addVideoToMyChannel(final VideoUpload videoUpload) throws IOException {
    final Video videoObjectDefiningMetadata = new Video();
    final VideoStatus status = new VideoStatus();
    status.setPrivacyStatus(videoUpload.getPrivacyStatus());
    videoObjectDefiningMetadata.setStatus(status);
    // Metadata lives in VideoSnippet
    final VideoSnippet snippet = new VideoSnippet();
    snippet.setTitle(videoUpload.getTitle());
    snippet.setDescription(videoUpload.getDescription());
    final String[] tags = videoUpload.getTags();
    if (ArrayUtils.isNotEmpty(tags)) {
      snippet.setTags(Collections.list(tags));
    }
    // Attach metadata to video object.
    videoObjectDefiningMetadata.setSnippet(snippet);

    final File videoFile = videoUpload.getVideoFile();
    final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(videoFile));
    final InputStreamContent mediaContent = new InputStreamContent("video/*", inputStream);
    mediaContent.setLength(videoFile.length());

    final YouTube.Videos.Insert videoInsert = youTube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
    final MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

    uploader.setDirectUploadEnabled(false);
    uploader.setProgressListener(videoUpload.getProgressListener());
    return execute(videoInsert);
  }

  @Override
  public Playlist createPlaylist(final String title, final String description, final String... tags) throws IOException {
    final PlaylistSnippet playlistSnippet = new PlaylistSnippet();
    playlistSnippet.setTitle(title);
    playlistSnippet.setDescription(description);
    if (tags.length > 0) {
      playlistSnippet.setTags(Collections.list(tags));
    }
    // Playlists are always public. The videos therein might be private.
    final PlaylistStatus playlistStatus = new PlaylistStatus();
    playlistStatus.setPrivacyStatus("public");

    // Create playlist with metadata and status.
    final Playlist youTubePlaylist = new Playlist();
    youTubePlaylist.setSnippet(playlistSnippet);
    youTubePlaylist.setStatus(playlistStatus);

    // The first argument tells the API what to return when a successful insert has been executed.
    final YouTube.Playlists.Insert command = youTube.playlists().insert("snippet,status", youTubePlaylist);
    return execute(command);
  }

  @Override
  public PlaylistItem addPlaylistItem(final String playlistId, final String videoId) throws IOException {
    // Resource type (video,playlist,channel) needs to be set along with resource id.
    final ResourceId resourceId = new ResourceId();
    resourceId.setKind("youtube#video");
    resourceId.setVideoId(videoId);

    // Set the required snippet properties.
    final PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
    playlistItemSnippet.setTitle("First video in the test playlist");
    playlistItemSnippet.setPlaylistId(playlistId);
    playlistItemSnippet.setResourceId(resourceId);

    // Create the playlist item.
    final PlaylistItem playlistItem = new PlaylistItem();
    playlistItem.setSnippet(playlistItemSnippet);

    // The first argument tells the API what to return when a successful insert has been executed.
    final YouTube.PlaylistItems.Insert playlistItemsInsertCommand = youTube.playlistItems().insert("snippet,contentDetails", playlistItem);
    return execute(playlistItemsInsertCommand);
  }

  @Override
  public void removeVideoFromPlaylist(final String playlistId, final String videoId) throws IOException {
    final PlaylistItem playlistItem = findPlaylistItem(playlistId, videoId);
    final YouTube.PlaylistItems.Delete deleteRequest = youTube.playlistItems().delete(playlistItem.getId());
    execute(deleteRequest);
  }

  @Override
  public void removeMyVideo(final String videoId) throws Exception {
    final YouTube.Videos.Delete deleteRequest = youTube.videos().delete(videoId);
    execute(deleteRequest);
  }

  @Override
  public void removeMyPlaylist(final String playlistId) throws IOException {
    final YouTube.Playlists.Delete deleteRequest = youTube.playlists().delete(playlistId);
    execute(deleteRequest);
  }

  @Override
  public SearchListResponse searchMyVideos(final String queryTerm, final String pageToken, final long maxResults) throws IOException {
    final YouTube.Search.List search = youTube.search().list("id,snippet");
    if (pageToken != null) {
      search.set("pageToken", pageToken);
    }
    search.setQ(queryTerm);
    search.setType("video");
    search.setForMine(true);
    search.setMaxResults(maxResults);
    search.setFields("items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination");
    return execute(search);
  }

  @Override
  public Video getVideoById(final String videoId) throws IOException {
    final YouTube.Videos.List search = youTube.videos().list("id,snippet");
    search.setId(videoId);
    search.setFields("items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination");
    final VideoListResponse response = execute(search);
    return response.getItems().isEmpty() ? null : response.getItems().get(0);
  }

  @Override
  public Playlist getMyPlaylistByTitle(final String title) throws IOException {
    final String trimmedTitle = title.trim();
    boolean searchedAllPlaylist = false;
    Playlist playlist = null;
    String nextPageToken = null;
    while (!searchedAllPlaylist) {
      final PlaylistListResponse searchResult = getMyPlaylists(nextPageToken, 50);
      for (final Playlist p : searchResult.getItems()) {
        if (p.getSnippet().getTitle().trim().equals(trimmedTitle)) {
          playlist = p;
          break;
        }
      }
      nextPageToken = searchResult.getNextPageToken();
      searchedAllPlaylist = nextPageToken == null;
    }
    return playlist;
  }

  @Override
  public PlaylistListResponse getMyPlaylists(final String pageToken, final long maxResults) throws IOException {
    final YouTube.Playlists.List search = youTube.playlists().list("id,snippet");
    if (pageToken != null) {
      search.set("pageToken", pageToken);
    }
    search.setMaxResults(maxResults);
    search.setMine(true);
    search.setFields("items(id,snippet),kind,nextPageToken,pageInfo,prevPageToken,tokenPagination");
    return execute(search);
  }

  @Override
  public PlaylistItemListResponse getPlaylistItems(final String playlistId, final String pageToken, final long maxResults) throws IOException {
    final YouTube.PlaylistItems.List search = youTube.playlistItems().list("id,snippet");
    search.setPlaylistId(playlistId);
    search.setPageToken(pageToken);
    search.setMaxResults(maxResults);
    search.setFields("items(id,kind,snippet),nextPageToken,pageInfo,prevPageToken,tokenPagination");
    return execute(search);
  }

  /**
   * @param playlistId may not be {@code null}
   * @param videoId may not be {@code null}
   * @return null when not found.
   * @throws IOException when transaction fails.
   */
  private PlaylistItem findPlaylistItem(final String playlistId, final String videoId) throws IOException {
    final List<PlaylistItem> playlistItems = getAllPlaylistItems(playlistId);
    PlaylistItem playlistItem = null;
    for (final PlaylistItem next : playlistItems) {
      final String id = next.getSnippet().getResourceId().getVideoId();
      if (videoId.equals(id)) {
        playlistItem = next;
        break;
      }
    }
    return playlistItem;
  }

  /**
   *
   * @param playlistId may not be {@code null}
   * @return zero or more playlist items.
   * @throws IOException  when transaction fails.
   */
  private List<PlaylistItem> getAllPlaylistItems(final String playlistId) throws IOException {
    final List<PlaylistItem> playlistItems = new LinkedList<PlaylistItem>();
    boolean done = false;
    String nextPageToken = null;
    while (!done) {
      final PlaylistItemListResponse searchResult = getPlaylistItems(playlistId, nextPageToken, 50);
      playlistItems.addAll(searchResult.getItems());
      nextPageToken = searchResult.getNextPageToken();
      done = nextPageToken == null;
    }
    return playlistItems;
  }

  /**
   * @see com.google.api.services.youtube.YouTubeRequest#execute()
   * @param command may not be {@code null}
   * @param <T> type of request.
   * @return result; may be {@code null}
   * @throws IOException  when transaction fails.
   */
  private <T> T execute(final YouTubeRequest<T> command) throws IOException {
    return command.execute();
  }
}
