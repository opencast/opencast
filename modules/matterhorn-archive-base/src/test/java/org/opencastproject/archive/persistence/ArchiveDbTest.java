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
package org.opencastproject.archive.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.archive.api.Version.version;
import static org.opencastproject.archive.base.ArchiveBase.mkPartial;
import static org.opencastproject.archive.base.ArchiveBase.rewriteAssetsForArchival;
import static org.opencastproject.mediapackage.MediaPackageSupport.copy;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.archive.base.persistence.AbstractArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.persistence.Episode;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceEnvs;

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
public class ArchiveDbTest {
  private ArchiveDb episodeDatabase;
  private PersistenceEnv penv;
  private String storage;

  private AccessControlList accessControlList;
  private SecurityService securityService;

  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = SecurityUtil.createSystemUser("admin", new DefaultOrganization());
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    penv = PersistenceEnvs.testPersistenceEnv("org.opencastproject.archive.base.persistence");
    episodeDatabase = new AbstractArchiveDb() {
      @Override
      protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override
      protected SecurityService getSecurityService() {
        return securityService;
      }
    };

    accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "write", true));
  }

  @Test
  public void testAdding() throws Exception {
    Date modificationDate = new Date();
    final MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, modificationDate, version(1L));

    Iterator<Episode> allEpisodes = episodeDatabase.getAllEpisodes();
    while (allEpisodes.hasNext()) {
      final Episode e = allEpisodes.next();

      String mpId = e.getMediaPackage().getIdentifier().toString();

      AccessControlList acl = e.getAcl();
      assertEquals(accessControlList.getEntries().size(), acl.getEntries().size());
      assertEquals(accessControlList.getEntries().get(0), acl.getEntries().get(0));
      assertFalse(e.isDeleted());
      assertEquals(some(true), episodeDatabase.isLatestVersion(mpId, e.getVersion()));
      assertEquals(modificationDate, e.getModificationDate());
      assertEquals(securityService.getOrganization().getId(), e.getOrganization());
    }
  }

  @Test
  public void testVersionAdding() throws Exception {
    final MediaPackage mp1 = loadFromClassPath("/manifest-simple.xml");
    final MediaPackage mp2 = copy(mp1);
    rewriteAssetsForArchival(mkPartial(mp1), version(1L));
    episodeDatabase.storeEpisode(mkPartial(mp1), accessControlList, new Date(), version(1L));
    assertEquals(some(true), episodeDatabase.isLatestVersion(mp1.getIdentifier().toString(), version(1L)));
    rewriteAssetsForArchival(mkPartial(mp2), version(2L));
    episodeDatabase.storeEpisode(mkPartial(mp2), accessControlList, new Date(), version(2L));
    assertEquals(some(false), episodeDatabase.isLatestVersion(mp2.getIdentifier().toString(), version(1L)));
  }

  @Test
  public void testDeleting() throws Exception {
    final MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    assertTrue("Media package is supposed to have elements", mediaPackage.getElements().length > 0);
    final Checksum checksum = mediaPackage.getElements()[0].getChecksum();
    assertNotNull("Media package elements are supposed to have checksums", checksum);
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, new Date(), version(1L));
    assertTrue("There should be one asset with checksum " + checksum,
            episodeDatabase.findAssetByChecksum(checksum.toString()).isSome());
    Date deletionDate = new Date();
    episodeDatabase.deleteEpisode(mediaPackage.getIdentifier().toString(), deletionDate);
    assertEquals(deletionDate, episodeDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()).get());
    assertTrue("Asset with checksum " + checksum + " should have been deleted",
            episodeDatabase.findAssetByChecksum(checksum.toString()).isNone());
  }

  @Test
  public void testRetrieving() throws Exception {
    final MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, new Date(), version(1L));

    assertTrue(episodeDatabase.getEpisode(mediaPackage.getIdentifier().toString(), version(0L)).isNone());
    assertTrue(episodeDatabase.getEpisode(mediaPackage.getIdentifier().toString(), version(1L)).isSome());

    Date deletionDate = new Date();
    episodeDatabase.deleteEpisode(mediaPackage.getIdentifier().toString(), deletionDate);
    assertEquals(deletionDate, episodeDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()).get());

    Iterator<Episode> allEpisodes = episodeDatabase.getAllEpisodes();
    int i = 0;
    while (allEpisodes.hasNext()) {
      allEpisodes.next();
      i++;
    }
    assertEquals(1, i);
  }

  @Test
  public void testAsset() throws Exception {
    final MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, new Date(), version(1L));
    final MediaPackageElement mpe = mediaPackage.getElements()[0];
    assertTrue(episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).isSome());
    assertEquals(mpe.getChecksum().toString(), episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).get()
            .getChecksum());
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, new Date(), version(2L));
    assertTrue(episodeDatabase.findAssetByChecksum(mpe.getChecksum().toString()).isSome());
    episodeDatabase.storeEpisode(mkPartial(mediaPackage), accessControlList, new Date(), version(3L));
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
