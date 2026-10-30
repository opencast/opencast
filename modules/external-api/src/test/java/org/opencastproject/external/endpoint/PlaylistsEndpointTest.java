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
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.test.rest.RestServiceTestEnv.testEnvForClasses;

import org.opencastproject.playlists.Playlist;
import org.opencastproject.test.rest.RestServiceTestEnv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link PlaylistsEndpoint} */
public class PlaylistsEndpointTest {

  /** The REST test environment */
  private static final RestServiceTestEnv env = testEnvForClasses(TestPlaylistsEndpoint.class);


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

  private void assertPlaylist(JsonObject json) {
    assertEquals("title", json.get("title").getAsString());
    assertEquals("description", json.get("description").getAsString());
    assertEquals("creator", json.get("creator").getAsString());
    assertEquals("2023-11-30T16:16:47Z", json.get("updated").getAsString());

    final JsonArray entries = json.getAsJsonArray("entries");
    assertEquals(2, entries.size());

    final JsonObject entry = entries.get(0).getAsJsonObject();
    assertEquals("1234", entry.get("contentId").getAsString());
    assertEquals("EVENT", entry.get("type").getAsString());

    final JsonObject entryTwo = entries.get(1).getAsJsonObject();
    assertEquals("abcd", entryTwo.get("contentId").getAsString());
    assertEquals("EVENT", entryTwo.get("type").getAsString());

    final JsonArray accessControlEntries = json.getAsJsonArray("accessControlEntries");
    assertEquals(1, accessControlEntries.size());

    final JsonObject accessControlEntry = accessControlEntries.get(0).getAsJsonObject();
    assertEquals(true, accessControlEntry.get("allow").getAsBoolean());
    assertEquals("ROLE_USER_BOB", accessControlEntry.get("role").getAsString());
    assertEquals("read", accessControlEntry.get("action").getAsString());
  }

  @Test
  public void testGetPlaylist() throws Exception {
    String response = given()
        .pathParam("id", PLAYLIST_ID)
        .expect()
        .statusCode(SC_OK).when()
        .get(env.host("/{id}"))
        .asString();

    assertPlaylist(JsonParser.parseString(response).getAsJsonObject());
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

    JsonArray playlists = JsonParser.parseString(response).getAsJsonArray();
    assertPlaylist(playlists.get(0).getAsJsonObject());
  }

  @Test
  public void testCreatePlaylist() throws Exception {
    String response = given()
        .formParam("playlist", new Playlist())
        .expect()
        .statusCode(SC_CREATED).when()
        .post(env.host("/"))
        .asString();

    assertPlaylist(JsonParser.parseString(response).getAsJsonObject());
  }

  @Test
  public void testCreatePlaylistInvalid() throws Exception {
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
}
