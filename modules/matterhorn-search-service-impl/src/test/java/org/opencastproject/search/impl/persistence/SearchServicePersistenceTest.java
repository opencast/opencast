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
package org.opencastproject.search.impl.persistence;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Tuple;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class SearchServicePersistenceTest {

  private ComboPooledDataSource pooledDataSource;
  private SearchServiceDatabaseImpl searchDatabase;
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

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + currentTime);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new User("admin", SecurityConstants.DEFAULT_ORGANIZATION_ID,
            new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE });
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    searchDatabase = new SearchServiceDatabaseImpl();
    searchDatabase.setPersistenceProvider(new PersistenceProvider());
    searchDatabase.setPersistenceProperties(props);
    searchDatabase.setSecurityService(securityService);
    searchDatabase.activate(null);

    mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "write", true));
  }

  @Test
  public void testAdding() throws Exception {
    Date modifictaionDate = new Date();
    searchDatabase.storeMediaPackage(mediaPackage, accessControlList, modifictaionDate);

    Iterator<Tuple<MediaPackage, String>> mediaPackages = searchDatabase.getAllMediaPackages();
    while (mediaPackages.hasNext()) {
      Tuple<MediaPackage, String> mediaPackage = mediaPackages.next();

      String mediaPackageId = mediaPackage.getA().getIdentifier().toString();

      AccessControlList acl = searchDatabase.getAccessControlList(mediaPackageId);
      Assert.assertEquals(accessControlList.getEntries().size(), acl.getEntries().size());
      Assert.assertEquals(accessControlList.getEntries().get(0), acl.getEntries().get(0));
      Assert.assertNull(searchDatabase.getDeletionDate(mediaPackageId));
      Assert.assertEquals(modifictaionDate, searchDatabase.getModificationDate(mediaPackageId));
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

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    searchDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
  }

}
