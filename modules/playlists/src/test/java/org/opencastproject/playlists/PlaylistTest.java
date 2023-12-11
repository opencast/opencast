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
package org.opencastproject.playlists;

import org.opencastproject.playlists.serialization.JaxbPlaylist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlaylistTest {

  @Test
  public void testPlaylist() throws Exception {
    Playlist playlist = new Playlist();
    playlist.setTitle("title");
    Assert.assertEquals(playlist.getTitle(), "title");
  }

  @Test
  public void testPlaylistEntries() throws Exception {
    Playlist playlist = new Playlist();

    PlaylistEntry playlistEntry = new PlaylistEntry(1, "1234", PlaylistEntryType.EVENT);
    PlaylistEntry playlistEntry2 = new PlaylistEntry(2, "5687", PlaylistEntryType.EVENT);

    playlist.addEntry(playlistEntry);
    playlist.addEntry(playlistEntry2);
    Assert.assertEquals(2, playlist.getEntries().size());

    playlist.removeEntry(playlistEntry);
    Assert.assertEquals(1, playlist.getEntries().size());

    List<PlaylistEntry> playlistEntries = playlist.getEntries();
    Assert.assertEquals(playlistEntries.get(0).getEventId(), "5687");
  }

  @Test
  public void testPlaylistDeserialization() throws Exception {
//    PlaylistEntry playlistEntry = new PlaylistEntry("1234", "type");
//    List<PlaylistAccessControlEntry> playlistAccessControlEntries = new ArrayList<>();
//    playlistAccessControlEntries.add(new PlaylistAccessControlEntry(true, "ROLE_USER_BOB", "read"));

//    Playlist playlist = new Playlist();
//    playlist.setOrganization("mh_default_org");
//    playlist.addEntry(playlistEntry);
//    playlist.setTitle("title");
//    playlist.setDescription("description");
//    playlist.setCreator("creator");
//    playlist.setUpdated(new Date(1701361007521L));
//    playlist.setAccessControlEntries(playlistAccessControlEntries);
//
//    JaxbPlaylist jaxbPlaylistOriginal = new JaxbPlaylist(playlist);

    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);

    InputStream in = getClass().getResourceAsStream("/playlist.json");
    JaxbPlaylist jaxbPlaylistParsed = objectMapper.readValue(in, JaxbPlaylist.class);
    Playlist playlistParsed = jaxbPlaylistParsed.toPlaylist();

    Assert.assertEquals(playlistParsed.getTitle(), "title");
    Assert.assertEquals(playlistParsed.getEntries().get(1).getEventId(), "abcd");
  }

  @Test
  public void testPlaylistSerialization() throws Exception {
    PlaylistEntry playlistEntry = new PlaylistEntry("1234", PlaylistEntryType.EVENT);
    PlaylistEntry playlistEntry2 = new PlaylistEntry("abcd", PlaylistEntryType.EVENT);
    List<PlaylistAccessControlEntry> playlistAccessControlEntries = new ArrayList<>();
    playlistAccessControlEntries.add(new PlaylistAccessControlEntry(true, "ROLE_USER_BOB", "read"));

    Playlist playlist = new Playlist();
    playlist.setOrganization("mh_default_org");
    playlist.addEntry(playlistEntry);
    playlist.addEntry(playlistEntry2);
    playlist.setTitle("title");
    playlist.setDescription("description");
    playlist.setCreator("creator");
    playlist.setUpdated(new Date(1701361007521L));
    playlist.setAccessControlEntries(playlistAccessControlEntries);

    JaxbPlaylist jaxbPlaylist = new JaxbPlaylist(playlist);
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);

    String jsonSerialized = objectMapper.writeValueAsString(jaxbPlaylist);
    JaxbPlaylist jaxbPlaylistParsed = objectMapper.readValue(jsonSerialized, JaxbPlaylist.class);
    Playlist playlistParsed = jaxbPlaylistParsed.toPlaylist();

    Assert.assertEquals(playlist.getTitle(), playlistParsed.getTitle());
    Assert.assertEquals(playlist.getEntries().get(1).getEventId(), playlistParsed.getEntries().get(1).getEventId());
  }
}
