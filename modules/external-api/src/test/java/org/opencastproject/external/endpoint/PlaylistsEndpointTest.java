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
package org.opencastproject.external.endpoint;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.test.rest.RestServiceTestEnv;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

/** Unit tests for {@link PlaylistsEndpoint} */
public class PlaylistsEndpointTest {

  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(TestPlaylistsEndpoint.class);

  private static final JSONParser parser = new JSONParser();

  private static final String PLAYLIST_ID = "28";
  private static final String MISSING_PLAYLIST_ID = "4444";

  private static final String UNAUTHORIZED_PLAYLIST_ID = "1";

  private static final String INVALID_PLAYLIST_JSON = "{{ \"title\": \"bad request\" }";

  @BeforeClass
  public static void oneTimeSetUp() {
    env.setUpServer();
  }

  @AfterClass
  public static void oneTimeTearDown() {
    env.tearDownServer();
  }

  private void assertPlaylist(JSONObject json) {
    assertEquals("title", json.get("title"));
    assertEquals("description", json.get("description"));
    assertEquals("creator", json.get("creator"));
    assertEquals("2023-11-30T16:16:47Z", json.get("updated"));

    final JSONArray entries = (JSONArray) json.get("entries");
    assertEquals(2, entries.size());

    final JSONObject entry = (JSONObject) entries.get(0);
    assertEquals("1234", entry.get("contentId"));
    assertEquals("EVENT", entry.get("type"));

    final JSONObject entryTwo = (JSONObject) entries.get(1);
    assertEquals("abcd", entryTwo.get("contentId"));
    assertEquals("EVENT", entryTwo.get("type"));

    final JSONArray accessControlEntries = (JSONArray) json.get("accessControlEntries");
    assertEquals(1, accessControlEntries.size());

    final JSONObject accessControlEntry = (JSONObject) accessControlEntries.get(0);
    assertEquals(true, accessControlEntry.get("allow"));
    assertEquals("ROLE_USER_BOB", accessControlEntry.get("role"));
    assertEquals("read", accessControlEntry.get("action"));
  }

  @Test
  public void testGetPlaylist() throws Exception {
    String response = given()
        .pathParam("id", PLAYLIST_ID)
        .expect()
        .statusCode(SC_OK).when()
        .get(env.host("/{id}"))
        .asString();

    assertPlaylist((JSONObject) parser.parse(response));
  }

  @Test
  public void testGetPlaylistThatDoesNotExist() throws Exception {
    given()
        .pathParam("id", MISSING_PLAYLIST_ID)
        .expect()
        .statusCode(SC_NOT_FOUND).when()
        .get(env.host("/{id}"));
  }

  @Test
  public void testGetPlaylistUnauthorized() throws Exception {
    given()
        .pathParam("id", UNAUTHORIZED_PLAYLIST_ID)
        .expect()
        .statusCode(SC_FORBIDDEN).when()
        .get(env.host("/{id}"));
  }

  @Test
  public void testGetPlaylists() throws Exception {
    String response = given()
        .queryParam("limit", 100)
        .queryParam("offset", 0)
        .queryParam("sortByUpdated", false)
        .queryParam("sortByUpdatedAscending", false)
        .expect()
        .statusCode(SC_OK).when()
        .get(env.host("/"))
        .asString();

    JSONArray playlists = (JSONArray) parser.parse(response);
    assertPlaylist((JSONObject) playlists.get(0));
  }

  @Test
  public void testUpdatePlaylist() throws Exception {
    String response = given()
        .formParam("playlist", new Playlist())
        .expect()
        .statusCode(SC_OK).when()
        .post(env.host("/"))
        .asString();

    assertPlaylist((JSONObject) parser.parse(response));
  }

  @Test
  public void testUpdatePlaylistInvalid() throws Exception {
    given()
        .formParam("playlist", INVALID_PLAYLIST_JSON)
        .expect()
        .statusCode(SC_BAD_REQUEST).when()
        .post(env.host("/"));
  }

  @Test
  public void testRemovePlaylist() throws Exception {
    given()
        .pathParam("id", PLAYLIST_ID)
        .expect()
        .statusCode(SC_OK).when()
        .delete(env.host("/{id}"));
  }

  @Test
  public void testRemoveMissingPlaylist() throws Exception {
    given()
        .pathParam("id", MISSING_PLAYLIST_ID)
        .expect()
        .statusCode(SC_NOT_FOUND).when()
        .delete(env.host("/{id}"));
  }

  @Test
  public void testRemovePlaylistUnauthorized() throws Exception {
    given()
        .pathParam("id", UNAUTHORIZED_PLAYLIST_ID)
        .expect()
        .statusCode(SC_FORBIDDEN).when()
        .delete(env.host("/{id}"));
  }

  @Test
  public void testUpdateEntriesPlaylist() throws Exception {
    String response = given()
        .pathParam("id", PLAYLIST_ID)
        .formParam("playlistEntries", new ArrayList<PlaylistEntry>())
        .expect()
        .statusCode(SC_OK).when()
        .post(env.host("/{id}/entries"))
        .asString();
  }
}
