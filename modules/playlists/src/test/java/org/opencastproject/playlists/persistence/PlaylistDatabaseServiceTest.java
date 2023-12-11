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
package org.opencastproject.playlists.persistence;

import static org.junit.Assert.assertTrue;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.playlists.Playlist;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistEntryType;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlaylistDatabaseServiceTest {

  private PlaylistDatabaseServiceImpl playlistDatabaseService;
  private SecurityService securityService;

  private Playlist testPlaylist;

  @Before
  public void setUp() throws Exception {
    // Init default playlist
    PlaylistEntry playlistEntry = new PlaylistEntry("1234", PlaylistEntryType.EVENT);
    PlaylistEntry playlistEntry2 = new PlaylistEntry("abcd", PlaylistEntryType.EVENT);
    List<PlaylistAccessControlEntry> playlistAccessControlEntries = new ArrayList<>();
    playlistAccessControlEntries.add(new PlaylistAccessControlEntry(true, "ROLE_USER_BOB", "read"));

    testPlaylist = new Playlist();
    testPlaylist.setId(42L);
    testPlaylist.setOrganization("mh_default_org");
    testPlaylist.addEntry(playlistEntry);
    testPlaylist.addEntry(playlistEntry2);
    testPlaylist.setTitle("title");
    testPlaylist.setDescription("description");
    testPlaylist.setCreator("creator");
    testPlaylist.setUpdated(new Date(1701361007521L));
    testPlaylist.setAccessControlEntries(playlistAccessControlEntries);

    // Mock security service
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
        SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    // Init database
    playlistDatabaseService = new PlaylistDatabaseServiceImpl();
    playlistDatabaseService.setEntityManagerFactory(newEntityManagerFactory(
        PlaylistDatabaseServiceImpl.PERSISTENCE_UNIT));
    playlistDatabaseService.setDBSessionFactory(getDbSessionFactory());
    playlistDatabaseService.setSecurityService(securityService);
    playlistDatabaseService.activate(null);
  }

  @Test
  public void testAdding() throws Exception {
    playlistDatabaseService.updatePlaylist(testPlaylist, securityService.getOrganization().getId());
  }

  @Test
  public void testMerging() throws Exception {
    playlistDatabaseService.updatePlaylist(testPlaylist, securityService.getOrganization().getId());
    playlistDatabaseService.updatePlaylist(testPlaylist, securityService.getOrganization().getId());
  }

  @Test
  public void testDeleting() throws Exception {
    playlistDatabaseService.updatePlaylist(testPlaylist, securityService.getOrganization().getId());
    playlistDatabaseService.deletePlaylist(testPlaylist, securityService.getOrganization().getId());
  }

  @Test
  public void testRetrieving() throws Exception {
    playlistDatabaseService.updatePlaylist(testPlaylist, securityService.getOrganization().getId());

    List playlists = playlistDatabaseService.getPlaylists(100, 0, false, false);
    assertTrue("Exactly one playlists should be returned", playlists.size() == 1);
    playlistDatabaseService.deletePlaylist(testPlaylist, securityService.getOrganization().getId());
    playlists = playlistDatabaseService.getPlaylists(100, 0, false, false);
    assertTrue("Exactly zero playlists should be returned", playlists.isEmpty());
  }
}
