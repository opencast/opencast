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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.playlists.persistence.PlaylistDatabaseServiceImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlaylistServiceTest {

  private PlaylistService service;
  private SecurityService securityService = null;
  private DefaultOrganization organization = null;
  private Playlist playlist;
  private AccessControlList acl = new AccessControlList();

  @Before
  public void setUp() throws Exception {
    // Playlist
    PlaylistEntry playlistEntry = new PlaylistEntry("1234", PlaylistEntryType.EVENT);
    PlaylistEntry playlistEntry2 = new PlaylistEntry("abcd", PlaylistEntryType.EVENT);
    List<PlaylistAccessControlEntry> playlistAccessControlEntries = new ArrayList<>();
    playlistAccessControlEntries.add(new PlaylistAccessControlEntry(true, "ROLE_USER_BOB", "read"));

    playlist = new Playlist();
    playlist.setOrganization("mh_default_org");
    playlist.addEntry(playlistEntry);
    playlist.addEntry(playlistEntry2);
    playlist.setTitle("title");
    playlist.setDescription("description");
    playlist.setCreator("creator");
    playlist.setUpdated(new Date(1701361007521L));
    playlist.setAccessControlEntries(playlistAccessControlEntries);

    // Playlist Service
    service = new PlaylistService();

    organization = new DefaultOrganization();
    securityService = createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
        SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    expect(securityService.getUser()).andReturn(user).anyTimes();
    replay(securityService);
    service.setSecurityService(securityService);

    AuthorizationService authorizationService = createNiceMock(AuthorizationService.class);
    expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
        .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    replay(authorizationService);
    service.setAuthorizationService(authorizationService);

    PlaylistDatabaseServiceImpl playlistDb = new PlaylistDatabaseServiceImpl();
    playlistDb.setEntityManagerFactory(newEntityManagerFactory(PlaylistDatabaseServiceImpl.PERSISTENCE_UNIT));
    playlistDb.setDBSessionFactory(getDbSessionFactory());
    playlistDb.setSecurityService(securityService);
    playlistDb.activate(null);
    service.setPersistence(playlistDb);

    service.activate(null);
  }

  @Test
  public void testPlaylistManagement() throws Exception {
    playlist = service.update(playlist);
    Playlist playlistFromDb = service.getPlaylistById(playlist.getId());
    Assert.assertNotNull(playlistFromDb);
    Assert.assertEquals(playlist.getTitle(), playlistFromDb.getTitle());

    playlist.setTitle("different title");
    playlist = service.update(playlist);
    playlistFromDb = service.getPlaylistById(playlist.getId());
    Assert.assertEquals(playlist.getTitle(), playlistFromDb.getTitle());

    service.remove(playlist.getId());
    try {
      service.getPlaylistById(playlist.getId());
      Assert.fail("Playlist should not be available after removal.");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void addEntryShouldAddNewEntryWhenNotAlreadyPresent() throws Exception {
    Playlist playlist = new Playlist();
    PlaylistEntry playlistEntry = new PlaylistEntry(1, "1234", PlaylistEntryType.EVENT);

    boolean result = playlist.addEntry(playlistEntry);

    Assert.assertTrue(result);
    Assert.assertEquals(1, playlist.getEntries().size());
    Assert.assertEquals(playlistEntry, playlist.getEntries().get(0));
  }

  @Test
  public void addEntryShouldReplaceExistingEntry() throws Exception {
    Playlist playlist = new Playlist();
    PlaylistEntry playlistEntry = new PlaylistEntry(1, "1234", PlaylistEntryType.EVENT);
    playlist.addEntry(playlistEntry);

    PlaylistEntry newPlaylistEntry = new PlaylistEntry(1, "5678", PlaylistEntryType.EVENT);
    boolean result = playlist.addEntry(newPlaylistEntry);

    Assert.assertTrue(result);
    Assert.assertEquals(1, playlist.getEntries().size());
    Assert.assertEquals(newPlaylistEntry, playlist.getEntries().get(0));
  }

  @Test
  public void addEntryWithoutIdNotEqual() throws Exception {
    Playlist playlist = new Playlist();
    PlaylistEntry playlistEntry = new PlaylistEntry(0, "1234", PlaylistEntryType.EVENT);
    playlist.addEntry(playlistEntry);

    PlaylistEntry newPlaylistEntry = new PlaylistEntry(0, "5678", PlaylistEntryType.EVENT);
    boolean result = playlist.addEntry(newPlaylistEntry);

    Assert.assertTrue(result);
    Assert.assertEquals(2, playlist.getEntries().size());
  }

  @Test
  public void removeExistingEntry() throws Exception {
    Playlist playlist = new Playlist();
    PlaylistEntry playlistEntry = new PlaylistEntry(1, "1234", PlaylistEntryType.EVENT);
    playlist.addEntry(playlistEntry);

    PlaylistEntry removePlaylistEntry = new PlaylistEntry(1, "1234", PlaylistEntryType.EVENT);
    boolean result = playlist.removeEntry(removePlaylistEntry);

    Assert.assertTrue(result);
    Assert.assertEquals(0, playlist.getEntries().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void addEntryShouldThrowExceptionWhenEntryIsNull() throws Exception {
    Playlist playlist = new Playlist();
    playlist.addEntry(null);
  }
}
