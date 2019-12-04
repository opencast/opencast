/**
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

package org.opencastproject.search.impl.persistence;

import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class SearchServicePersistenceTest {

  private SearchServiceDatabaseImpl searchDatabase;
  private MediaPackage mediaPackage;
  private AccessControlList accessControlList;
  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    EntityManagerFactory emf = newTestEntityManagerFactory(SearchServiceDatabaseImpl.PERSISTENCE_UNIT);
    EntityManager em = emf.createEntityManager();
    securityService = EasyMock.createNiceMock(SecurityService.class);
    DefaultOrganization defaultOrganization = new DefaultOrganization();
    em.getTransaction().begin();
    Organization org = new JpaOrganization(defaultOrganization.getId(), defaultOrganization.getName(),
        defaultOrganization.getServers(), defaultOrganization.getAdminRole(), defaultOrganization.getAnonymousRole(),
        defaultOrganization.getProperties());
    em.merge(org);
    em.getTransaction().commit();
    User user = new JaxbUser("admin", "test", defaultOrganization, new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE,
            defaultOrganization));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    searchDatabase = new SearchServiceDatabaseImpl();
    searchDatabase.setEntityManagerFactory(emf);
    searchDatabase.setSecurityService(securityService);
    searchDatabase.activate(null);

    mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", Permissions.Action.WRITE.toString(), true));
  }

  @Test
  public void testAdding() throws Exception {
    int mpCount = searchDatabase.countMediaPackages();
    Date modificationDate = new Date();
    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, modificationDate);
    Assert.assertEquals(searchDatabase.countMediaPackages(), mpCount + 1);

    Iterator<Tuple<MediaPackage, String>> mediaPackages = searchDatabase.getAllMediaPackages();
    while (mediaPackages.hasNext()) {
      Tuple<MediaPackage, String> mediaPackage = mediaPackages.next();

      String mediaPackageId = mediaPackage.getA().getIdentifier().toString();

      AccessControlList acl = searchDatabase.getAccessControlList(mediaPackageId);
      Assert.assertEquals(accessControlList.getEntries().size(), acl.getEntries().size());
      Assert.assertEquals(accessControlList.getEntries().get(0), acl.getEntries().get(0));
      Assert.assertNull(searchDatabase.getDeletionDate(mediaPackageId));
      Assert.assertEquals(modificationDate, searchDatabase.getModificationDate(mediaPackageId));
      Assert.assertEquals(mediaPackage.getA(), searchDatabase.getMediaPackage(mediaPackageId));
      Assert.assertEquals(securityService.getOrganization().getId(), mediaPackage.getB());
      Assert.assertEquals(securityService.getOrganization().getId(), searchDatabase.getOrganizationId(mediaPackageId));
    }
  }

  @Test
  public void testUpdateMediaPackage() throws Exception {
    Date now = new Date();
    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, now);
    Assert.assertEquals(now, searchDatabase.getModificationDate(mediaPackage.getIdentifier().toString()));
    Date otherNow = new Date();
    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, otherNow);
    Assert.assertEquals(otherNow, searchDatabase.getModificationDate(mediaPackage.getIdentifier().toString()));
  }

  @Test
  public void testDeleting() throws Exception {
    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, new Date());
    Date deletionDate = new Date();
    searchDatabase.deleteMediaPackage(mediaPackage.getIdentifier().toString(), deletionDate);
    Assert.assertEquals(deletionDate, searchDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()));
  }

  @Test
  public void testRetrieving() throws Exception {
    boolean exception = false;
    MediaPackage episode;
    try {
      episode = searchDatabase.getMediaPackage(mediaPackage.getIdentifier().toString());
    } catch (NotFoundException e) {
      exception = true;
    }
    Assert.assertTrue(exception);

    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, new Date());

    episode = searchDatabase.getMediaPackage(mediaPackage.getIdentifier().toString());
    Assert.assertNotNull(episode);

    Date deletionDate = new Date();
    searchDatabase.deleteMediaPackage(mediaPackage.getIdentifier().toString(), deletionDate);
    episode = searchDatabase.getMediaPackage(mediaPackage.getIdentifier().toString());
    Assert.assertEquals(deletionDate, searchDatabase.getDeletionDate(mediaPackage.getIdentifier().toString()));

    Iterator<Tuple<MediaPackage, String>> allMediaPackages = searchDatabase.getAllMediaPackages();
    int i = 0;
    while (allMediaPackages.hasNext()) {
      allMediaPackages.next();
      i++;
    }
    Assert.assertEquals(1, i);

    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, new Date());

    allMediaPackages = searchDatabase.getAllMediaPackages();
    i = 0;
    while (allMediaPackages.hasNext()) {
      allMediaPackages.next();
      i++;
    }
    Assert.assertEquals(1, i);
  }

}
