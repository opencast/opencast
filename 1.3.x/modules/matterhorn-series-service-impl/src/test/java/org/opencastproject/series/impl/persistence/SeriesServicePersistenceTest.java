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
package org.opencastproject.series.impl.persistence;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 * 
 */
public class SeriesServicePersistenceTest {

  private ComboPooledDataSource pooledDataSource;
  private SeriesServiceDatabaseImpl seriesDatabase;
  private String storage;

  private DublinCoreCatalog testCatalog;

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

    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new User("admin", SecurityConstants.DEFAULT_ORGANIZATION_ID,
            new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE });
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    seriesDatabase = new SeriesServiceDatabaseImpl();
    seriesDatabase.setPersistenceProvider(new PersistenceProvider());
    seriesDatabase.setPersistenceProperties(props);
    DublinCoreCatalogService dcService = new DublinCoreCatalogService();
    seriesDatabase.setDublinCoreService(dcService);
    seriesDatabase.setSecurityService(securityService);
    seriesDatabase.activate(null);

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      testCatalog = dcService.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    seriesDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
  }

  @Test
  public void testAdding() throws Exception {
    seriesDatabase.storeSeries(testCatalog);
  }

  @Test
  public void testMerging() throws Exception {
    seriesDatabase.storeSeries(testCatalog);
    seriesDatabase.storeSeries(testCatalog);
  }

  @Test
  public void testDeleting() throws Exception {
    seriesDatabase.storeSeries(testCatalog);
    seriesDatabase.deleteSeries(testCatalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER));
  }

  @Test
  public void testRetrieving() throws Exception {
    seriesDatabase.storeSeries(testCatalog);
    DublinCoreCatalog[] series = seriesDatabase.getAllSeries();
    Assert.assertTrue("Exactly one series should be returned", series.length == 1);
    seriesDatabase.deleteSeries(testCatalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER));
    series = seriesDatabase.getAllSeries();
    Assert.assertTrue("Exactly zero series should be returned", series.length == 0);
  }

  @Test
  public void testAccessControlManagment() throws Exception {
    // sample access control list
    AccessControlList accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "delete", true));

    seriesDatabase.storeSeries(testCatalog);
    String seriesID = testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    seriesDatabase.storeSeriesAccessControl(seriesID, accessControlList);

    AccessControlList retrievedACL = seriesDatabase.getAccessControlList(seriesID);
    Assert.assertNotNull(retrievedACL);
    acl = retrievedACL.getEntries();
    Assert.assertEquals(acl.size(), 1);
    Assert.assertEquals(acl.get(0).getRole(), "admin");

    try {
      seriesDatabase.storeSeriesAccessControl("failid", accessControlList);
      Assert.fail("Should fail when adding ACL to nonexistent series");
    } catch (NotFoundException e) {
      // expected
    }
  }

}
