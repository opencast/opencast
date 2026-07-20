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

package org.opencastproject.graphql.playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.opencastproject.graphql.playlist.type.output.GqlPlaylist;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistEntryType;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GqlPlaylistTest {

  @Test
  public void mapsAllFieldsCorrectly() {
    PlaylistEntry entry = new PlaylistEntry(1L, "mp-1", PlaylistEntryType.EVENT);
    List<PlaylistEntry> entries = Collections.singletonList(entry);
    PlaylistAccessControlEntry ace = new PlaylistAccessControlEntry(true, "ROLE_USER", "read");
    List<PlaylistAccessControlEntry> aces = Collections.singletonList(ace);
    Date now = new Date();

    Playlist playlist = new Playlist("id-1", "org-1", entries, "title", "desc", "creator", now, aces);
    GqlPlaylist gql = new GqlPlaylist(playlist);

    assertEquals("id-1", gql.id());

    assertNotNull(gql.entries());
    assertEquals(1, gql.entries().size());
    assertEquals(Long.valueOf(1L), gql.entries().getFirst().id());
    assertEquals("mp-1", gql.entries().getFirst().contentId());
    assertEquals("EVENT", gql.entries().getFirst().type().toString());

    assertEquals("title", gql.title());
    assertEquals("desc", gql.description());
    assertEquals("creator", gql.creator());
    assertNotNull(gql.updated());

    assertNotNull(gql.accessControlEntries());
    assertEquals(1, gql.accessControlEntries().size());
    assertEquals("ROLE_USER", gql.accessControlEntries().getFirst().role());
    assertEquals("read", gql.accessControlEntries().getFirst().action());
    assertEquals(Boolean.TRUE, gql.accessControlEntries().getFirst().allow());
  }

  @Test
  public void handlesNullEntriesAndAcls() {
    Date now = new Date();
    Playlist mock = EasyMock.createMock(Playlist.class);
    EasyMock.expect(mock.getId()).andReturn("id-2");
    EasyMock.expect(mock.getEntries()).andReturn(null);
    EasyMock.expect(mock.getTitle()).andReturn(null);
    EasyMock.expect(mock.getDescription()).andReturn(null);
    EasyMock.expect(mock.getCreator()).andReturn(null);
    EasyMock.expect(mock.getUpdated()).andReturn(now);
    EasyMock.expect(mock.getAccessControlEntries()).andReturn(null);
    EasyMock.replay(mock);

    GqlPlaylist gql = new GqlPlaylist(mock);

    assertEquals("id-2", gql.id());
    assertEquals(Collections.emptyList(), gql.entries());
    assertNull(gql.title());
    assertNull(gql.description());
    assertNull(gql.creator());
    assertNotNull(gql.updated());
    assertEquals(Collections.emptyList(), gql.accessControlEntries());

    EasyMock.verify(mock);
  }

}
