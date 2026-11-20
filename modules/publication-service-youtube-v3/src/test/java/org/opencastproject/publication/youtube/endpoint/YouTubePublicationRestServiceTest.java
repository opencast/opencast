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

package org.opencastproject.publication.youtube.endpoint;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.publication.youtube.YouTubeAPIVersion3Service;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import javax.ws.rs.core.Response;

/**
 * Tests for the synchronous YouTube playlist endpoints. The underlying {@link YouTubeAPIVersion3Service} is mocked,
 * so these tests exercise the validation, idempotency, error-mapping and retry logic of the REST layer only.
 */
public class YouTubePublicationRestServiceTest {

  private YouTubePublicationRestService restService;
  private YouTubeAPIVersion3Service youTubeApiService;

  @Before
  public void before() {
    youTubeApiService = createMock(YouTubeAPIVersion3Service.class);
    restService = new YouTubePublicationRestService();
    restService.setYouTubeApiService(youTubeApiService);
  }

  @Test
  public void testCreatePlaylistFailsWhenServiceNotInitialized() {
    expect(youTubeApiService.isInitialized()).andReturn(false);
    replay(youTubeApiService);

    final Response response = restService.createPlaylist("title", null, null);

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreatePlaylistRejectsMissingTitle() {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    replay(youTubeApiService);

    final Response response = restService.createPlaylist(" ", null, null);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreatePlaylistRejectsInvalidPrivacyStatus() {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    replay(youTubeApiService);

    final Response response = restService.createPlaylist("title", null, "bogus");

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreatePlaylistDefaultsPrivacyStatusToUnlisted() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.createPlaylistWithPrivacyStatus("title", "", "unlisted"))
        .andReturn(new Playlist().setId("PL123"));
    replay(youTubeApiService);

    final Response response = restService.createPlaylist("title", null, null);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("PL123", jsonEntity(response).get("playlistId"));
  }

  @Test
  public void testCreatePlaylistMapsApiFailureToServerError() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.createPlaylistWithPrivacyStatus("title", "", "public"))
        .andThrow(new IOException("boom"));
    replay(youTubeApiService);

    final Response response = restService.createPlaylist("title", null, "public");

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("boom", jsonEntity(response).get("error"));
  }

  @Test
  public void testAddPlaylistItemRejectsMissingVideoId() {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", " ");

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddPlaylistItemIsIdempotentWhenAlreadyPresent() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V1")).andReturn(new PlaylistItem().setId("PI1"));
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", "V1");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    final JSONObject json = jsonEntity(response);
    assertTrue((Boolean) json.get("alreadyPresent"));
    assertEquals("PI1", json.get("playlistItemId"));
  }

  @Test
  public void testAddPlaylistItemInsertsWhenNotPresent() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V2")).andReturn(null);
    expect(youTubeApiService.addPlaylistItem("PL1", "V2")).andReturn(new PlaylistItem().setId("PI2"));
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", "V2");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    final JSONObject json = jsonEntity(response);
    assertFalse((Boolean) json.get("alreadyPresent"));
    assertEquals("PI2", json.get("playlistItemId"));
  }

  @Test
  public void testAddPlaylistItemMapsNotFoundToNotFound() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V3")).andReturn(null);
    expect(youTubeApiService.addPlaylistItem("PL1", "V3")).andThrow(googleJsonError(404, null));
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", "V3");

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAddPlaylistItemRetriesTransientErrorThenSucceeds() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V4")).andReturn(null);
    expect(youTubeApiService.addPlaylistItem("PL1", "V4")).andThrow(googleJsonError(409, null));
    expect(youTubeApiService.addPlaylistItem("PL1", "V4")).andReturn(new PlaylistItem().setId("PI4"));
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", "V4");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals("PI4", jsonEntity(response).get("playlistItemId"));
  }

  @Test
  public void testAddPlaylistItemGivesUpAfterMaxTransientRetries() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V5")).andReturn(null);
    expect(youTubeApiService.addPlaylistItem("PL1", "V5"))
        .andThrow(googleJsonError(503, "serviceUnavailable")).times(3);
    replay(youTubeApiService);

    final Response response = restService.addPlaylistItem("PL1", "V5");

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdatePlaylistReturnsNotFoundWhenMissing() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.updatePlaylist("PL1", "new title", null)).andReturn(null);
    replay(youTubeApiService);

    final Response response = restService.updatePlaylist("PL1", "new title", null);

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdatePlaylistSucceeds() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.updatePlaylist("PL1", "new title", "new description"))
        .andReturn(new Playlist().setId("PL1"));
    replay(youTubeApiService);

    final Response response = restService.updatePlaylist("PL1", "new title", "new description");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue((Boolean) jsonEntity(response).get("ok"));
  }

  @Test
  public void testRemovePlaylistItemReportsNotRemovedWhenAbsent() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V6")).andReturn(null);
    replay(youTubeApiService);

    final Response response = restService.removePlaylistItem("PL1", "V6");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertFalse((Boolean) jsonEntity(response).get("removed"));
  }

  @Test
  public void testRemovePlaylistItemRemovesWhenPresent() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.findPlaylistItem("PL1", "V7")).andReturn(new PlaylistItem().setId("PI7"));
    youTubeApiService.removeVideoFromPlaylist("PL1", "V7");
    expectLastCall();
    replay(youTubeApiService);

    final Response response = restService.removePlaylistItem("PL1", "V7");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue((Boolean) jsonEntity(response).get("removed"));
  }

  @Test
  public void testDeletePlaylistReturnsNotFoundWhenMissing() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.getPlaylistById("PL1")).andReturn(null);
    replay(youTubeApiService);

    final Response response = restService.deletePlaylist("PL1");

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeletePlaylistSucceeds() throws Exception {
    expect(youTubeApiService.isInitialized()).andReturn(true);
    expect(youTubeApiService.getPlaylistById("PL1")).andReturn(new Playlist().setId("PL1"));
    youTubeApiService.removeMyPlaylist("PL1");
    expectLastCall();
    replay(youTubeApiService);

    final Response response = restService.deletePlaylist("PL1");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue((Boolean) jsonEntity(response).get("ok"));
  }

  private static GoogleJsonResponseException googleJsonError(final int statusCode, final String reason) {
    final HttpResponseException.Builder builder =
        new HttpResponseException.Builder(statusCode, "error", new HttpHeaders());
    final GoogleJsonError details = new GoogleJsonError();
    details.setCode(statusCode);
    if (reason != null) {
      final GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
      errorInfo.setReason(reason);
      details.setErrors(Collections.singletonList(errorInfo));
    }
    return new GoogleJsonResponseException(builder, details);
  }

  private static JSONObject jsonEntity(final Response response) throws Exception {
    return (JSONObject) new JSONParser().parse((String) response.getEntity());
  }

}
