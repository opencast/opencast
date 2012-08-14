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
package org.opencastproject.episode.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.episode.api.Version.version;
import static org.opencastproject.episode.impl.EpisodeServiceImpl.rewriteForArchival;

import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.persistence.AbstractEpisodeServiceDatabase;
import org.opencastproject.episode.impl.persistence.EpisodeDto;
import org.opencastproject.episode.impl.persistence.EpisodeServiceDatabase;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple3;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class EpisodeServicePersistenceTest {

  private EpisodeServiceDatabase episodeDatabase;
  private PersistenceEnv penv;
  private String storage;

  private MediaPackage mediaPackage;
  private AccessControlList accessControlList;
  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new User("admin", SecurityConstants.DEFAULT_ORGANIZATION_ID,
            new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE });
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    penv = PersistenceUtil.newTestPersistenceEnv("org.opencastproject.episode.impl.persistence");
    episodeDatabase = new AbstractEpisodeServiceDatabase() {
      @Override protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override protected SecurityService getSecurityService() {
        return securityService;
      }
    };

    mediaPackage = MediaPackageSupport.loadMediaPackageFromClassPath("/manifest-simple.xml");

    accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "write", true));
  }

  @Test
  public void testAdding() throws Exception {
    Date modifictaionDate = new Date();
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, modifictaionDate, version(1L));

    Iterator<Tuple3<MediaPackage, Version, String>> allEpisodes = episodeDatabase.getAllEpisodes();
    while (allEpisodes.hasNext()) {
      Tuple3<MediaPackage, Version, String> episode = allEpisodes.next();

      String mediaPackageId = episode.getA().getIdentifier().toString();
      Version version = episode.getB();

      AccessControlList acl = episodeDatabase.getAccessControlList(mediaPackageId, version);
      assertEquals(accessControlList.getEntries().size(), acl.getEntries().size());
      assertEquals(accessControlList.getEntries().get(0), acl.getEntries().get(0));
      assertNull(episodeDatabase.getDeletionDate(mediaPackageId));
      assertTrue(episodeDatabase.getLockState(mediaPackageId));
      assertTrue(episodeDatabase.isLatestVersion(mediaPackageId, version));
      assertEquals(modifictaionDate, episodeDatabase.getModificationDate(mediaPackageId, version));
      assertEquals(episode.getA(), MediaPackageParser.getFromXml(episodeDatabase.getEpisode(mediaPackageId, version).get().getMediaPackageXML()));
      assertEquals(securityService.getOrganization().getId(), episode.getC());
      assertEquals(securityService.getOrganization().getId(),
                   episodeDatabase.getOrganizationId(mediaPackageId, version));
    }
  }

  @Test
  public void testVersionAdding() throws Exception {
    episodeDatabase.storeEpisode(rewriteForArchival(version(1L)).apply(mediaPackage), accessControlList, new Date(), version(1L));
    assertTrue(episodeDatabase.isLatestVersion(mediaPackage.getIdentifier().toString(), version(1L)));
    episodeDatabase.storeEpisode(rewriteForArchival(version(2L)).apply(mediaPackage), accessControlList, new Date(), version(2L));
    assertFalse(episodeDatabase.isLatestVersion(mediaPackage.getIdentifier().toString(), version(1L)));
  }

  @Test
  public void testLocking() throws Exception {
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(1L));
    assertTrue(episodeDatabase.getLockState(mediaPackage.getIdentifier().toString()));
    episodeDatabase.lockEpisode(mediaPackage.getIdentifier().toString(), false);
    assertFalse(episodeDatabase.getLockState(mediaPackage.getIdentifier().toString()));
  }

  @Test
  public void testDeleting() throws Exception {
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(1L));
    Date deletionDate = new Date();
    episodeDatabase.deleteEpisode(mediaPackage.getIdentifier().toString(), deletionDate);
    assertEquals(deletionDate, episodeDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()));
  }

  @Test
  public void testRetrieving() throws Exception {
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(1L));

    boolean exception = false;
    Option<EpisodeDto> episodeOpt = episodeDatabase.getEpisode(mediaPackage.getIdentifier().toString(), version(0L));
    assertTrue(episodeOpt.isNone());

    EpisodeDto episode = episodeDatabase.getEpisode(mediaPackage.getIdentifier().toString(), version(1L)).get();
    assertNotNull(episode);

    Date deletionDate = new Date();
    episodeDatabase.deleteEpisode(mediaPackage.getIdentifier().toString(), deletionDate);
    episode = episodeDatabase.getEpisode(mediaPackage.getIdentifier().toString(), version(1L)).get();
    assertEquals(deletionDate, episodeDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()));

    Iterator<Tuple3<MediaPackage, Version, String>> allEpisodes = episodeDatabase.getAllEpisodes();
    int i = 0;
    while (allEpisodes.hasNext()) {
      allEpisodes.next();
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  public void testAsset() throws Exception {
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(1L));
    final MediaPackageElement mpe = mediaPackage.getElements()[0];
    assertTrue(episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).isSome());
    assertEquals(mpe.getChecksum().toString(), episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).get().getChecksum());
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(2L));
    assertTrue(episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).isSome());
    episodeDatabase.storeEpisode(mediaPackage, accessControlList, new Date(), version(3L));
    assertTrue(episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).isSome());
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    penv.close();
    FileUtils.deleteQuietly(new File(storage));
  }
}
