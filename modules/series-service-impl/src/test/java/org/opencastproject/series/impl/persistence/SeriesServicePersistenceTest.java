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

package org.opencastproject.series.impl.persistence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 *
 */
public class SeriesServicePersistenceTest {

  private SeriesServiceDatabaseImpl seriesDatabase;

  private static final String ELEMENT_TYPE = "testelement";
  private static final byte[] ELEMENT_DATA_1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
  private static final byte[] ELEMENT_DATA_2 = "0123456789".getBytes();

  private DublinCoreCatalog testCatalog;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    seriesDatabase = new SeriesServiceDatabaseImpl();
    seriesDatabase.setEntityManagerFactory(newTestEntityManagerFactory(SeriesServiceDatabaseImpl.PERSISTENCE_UNIT));
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

    List series = seriesDatabase.getAllSeries();
    assertTrue("Exactly one series should be returned", series.size() == 1);
    seriesDatabase.deleteSeries(testCatalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER));
    series = seriesDatabase.getAllSeries();
    assertTrue("Exactly zero series should be returned", series.isEmpty());
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
    assertNotNull(retrievedACL);
    acl = retrievedACL.getEntries();
    assertEquals(acl.size(), 1);
    assertEquals(acl.get(0).getRole(), "admin");

    try {
      seriesDatabase.storeSeriesAccessControl("failid", accessControlList);
      fail("Should fail when adding ACL to nonexistent series");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testAddUpdateAndDeleteSeriesElement() throws Exception {
    seriesDatabase.storeSeries(testCatalog);
    final String seriesId = testCatalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER);

    assertTrue(seriesDatabase.storeSeriesElement(seriesId, ELEMENT_TYPE, ELEMENT_DATA_1));
    assertArrayEquals(ELEMENT_DATA_1, seriesDatabase.getSeriesElement(seriesId, ELEMENT_TYPE).get());

    assertTrue(seriesDatabase.storeSeriesElement(seriesId, ELEMENT_TYPE, ELEMENT_DATA_2));
    assertArrayEquals(ELEMENT_DATA_2, seriesDatabase.getSeriesElement(seriesId, ELEMENT_TYPE).get());

    assertTrue(seriesDatabase.deleteSeriesElement(seriesId, ELEMENT_TYPE));
    assertFalse(seriesDatabase.deleteSeriesElement(seriesId, ELEMENT_TYPE));
    assertEquals(Opt.none(), seriesDatabase.getSeriesElement(seriesId, ELEMENT_TYPE));
  }

}
