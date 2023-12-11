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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistEntryType;
import org.opencastproject.playlists.PlaylistRestService;
import org.opencastproject.playlists.PlaylistService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;

import org.easymock.EasyMock;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Path;

@Path("")
@Ignore
public class TestPlaylistsEndpoint extends PlaylistsEndpoint {

  public TestPlaylistsEndpoint() throws Exception {

    long playlistId = 28L;
    long missingPlaylistId = 4444L;
    long unauthorizedPlaylistId = 1L;
    String invalidPlaylistJson = "{{ \"title\": \"bad request\" }";

    PlaylistEntry entry1 = createNiceMock(PlaylistEntry.class);
    expect(entry1.getId()).andReturn(0L);
    expect(entry1.getEventId()).andReturn("1234");
    expect(entry1.getType()).andReturn(PlaylistEntryType.EVENT);
    replay(entry1);

    PlaylistEntry entry2 = createNiceMock(PlaylistEntry.class);
    expect(entry2.getId()).andReturn(1L);
    expect(entry2.getEventId()).andReturn("abcd");
    expect(entry2.getType()).andReturn(PlaylistEntryType.EVENT);
    replay(entry2);

    PlaylistAccessControlEntry accessControlEntry1 = createNiceMock(PlaylistAccessControlEntry.class);
    expect(accessControlEntry1.getId()).andReturn(0L);
    expect(accessControlEntry1.isAllow()).andReturn(true);
    expect(accessControlEntry1.getRole()).andReturn("ROLE_USER_BOB");
    expect(accessControlEntry1.getAction()).andReturn("read");
    replay(accessControlEntry1);

    Playlist playlist = createNiceMock(Playlist.class);
    expect(playlist.getId()).andReturn(playlistId);
    expect(playlist.getTitle()).andReturn("title");
    expect(playlist.getDescription()).andReturn("description");
    expect(playlist.getCreator()).andReturn("creator");
    expect(playlist.getUpdated()).andReturn(new Date(1701361007521L)).anyTimes();
    expect(playlist.getEntries()).andReturn(Lists.newArrayList(entry1, entry2));
    expect(playlist.getAccessControlEntries()).andReturn(Lists.newArrayList(accessControlEntry1));
    replay(playlist);

    List<Playlist> playlists = new ArrayList<>();
    playlists.add(playlist);

    PlaylistService service = createNiceMock(PlaylistService.class);
    expect(service.getPlaylistById(playlistId)).andReturn(playlist);
    expect(service.getPlaylistById(0L)).andThrow(new IllegalStateException());
    expect(service.getPlaylists(100, 0, false, false)).andReturn(playlists);
    expect(service.update(anyObject(Playlist.class))).andReturn(playlist);
    expect(service.remove(playlistId)).andReturn(playlist);
    expect(service.updateEntries(anyLong(), EasyMock.<List<PlaylistEntry>> anyObject())).andReturn(playlist);

    expect(service.getPlaylistById(missingPlaylistId)).andThrow(new NotFoundException());
    expect(service.remove(missingPlaylistId)).andThrow(new NotFoundException());

    expect(service.getPlaylistById(unauthorizedPlaylistId)).andThrow(new UnauthorizedException(""));
    expect(service.remove(unauthorizedPlaylistId)).andThrow(new UnauthorizedException(""));

    replay(service);

    PlaylistRestService restService = createNiceMock(PlaylistRestService.class);
    expect(restService.parseJsonToPlaylist(invalidPlaylistJson)).andThrow(new ParseException(0));
    expect(restService.parseJsonToPlaylist(anyObject(String.class))).andReturn(playlist);
    expect(restService.parseJsonToPlaylistEntries(invalidPlaylistJson)).andThrow(new JsonProcessingException("") { });
    expect(restService.parseJsonToPlaylistEntries(anyObject(String.class))).andReturn(Lists.newArrayList(entry1, entry2));
    replay(restService);

    setPlaylistService(service);
    setPlaylistRestService(restService);
  }
}
