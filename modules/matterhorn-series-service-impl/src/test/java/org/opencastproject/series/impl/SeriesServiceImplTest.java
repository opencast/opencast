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

package org.opencastproject.series.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Collections.list;

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.impl.persistence.SeriesServiceDatabaseImpl;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import com.entwinemedia.fn.data.Opt;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for Series Service.
 *
 */
public class SeriesServiceImplTest {

  private ComboPooledDataSource pooledDataSource;
  private SeriesServiceDatabaseImpl seriesDatabase;
  private String storage;

  private SeriesServiceSolrIndex index;
  private DublinCoreCatalogService dcService;
  private String root;

  private SeriesServiceImpl seriesService;

  private DublinCoreCatalog testCatalog;
  private DublinCoreCatalog testCatalog2;

  private static final String ELEMENT_TYPE = "testelement";
  private static final byte[] ELEMENT_DATA_1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
  private static final byte[] ELEMENT_DATA_2 = "0123456789".getBytes();

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
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    seriesDatabase = new SeriesServiceDatabaseImpl();
    seriesDatabase.setPersistenceProvider(new PersistenceProvider());
    seriesDatabase.setPersistenceProperties(props);
    dcService = new DublinCoreCatalogService();
    seriesDatabase.setDublinCoreService(dcService);
    seriesDatabase.activate(null);
    seriesDatabase.setSecurityService(securityService);

    root = PathSupport.concat("target", Long.toString(currentTime));
    index = new SeriesServiceSolrIndex(root);
    index.setDublinCoreService(dcService);
    index.setSecurityService(securityService);
    index.activate(null);

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    seriesService = new SeriesServiceImpl();
    seriesService.setPersistence(seriesDatabase);
    seriesService.setIndex(index);
    seriesService.setSecurityService(securityService);
    seriesService.setMessageSender(messageSender);

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty((String) EasyMock.anyObject())).andReturn("System Admin");
    EasyMock.replay(bundleContext);

    ComponentContext componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(componentContext);

    seriesService.activate(componentContext);

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      testCatalog = dcService.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
    try {
      in = getClass().getResourceAsStream("/dublincore2.xml");
      testCatalog2 = dcService.load(in);
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
    seriesDatabase = null;
    index.deactivate();
    FileUtils.deleteQuietly(new File(root));
    index = null;
  }

  @Test
  public void testSeriesManagement() throws Exception {
    testCatalog.set(DublinCore.PROPERTY_TITLE, "Some title");
    seriesService.updateSeries(testCatalog);
    DublinCoreCatalog retrivedSeries = seriesService.getSeries(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertEquals("Some title", retrivedSeries.getFirst(DublinCore.PROPERTY_TITLE));

    testCatalog.set(DublinCore.PROPERTY_TITLE, "Some other title");
    seriesService.updateSeries(testCatalog);
    retrivedSeries = seriesService.getSeries(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertEquals("Some other title", retrivedSeries.getFirst(DublinCore.PROPERTY_TITLE));

    seriesService.deleteSeries(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    try {
      seriesService.getSeries(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      Assert.fail("Series should not be available after removal.");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testSorting() throws Exception {
    seriesService.updateSeries(testCatalog);
    seriesService.updateSeries(testCatalog2);
    {
      SeriesQuery q = new SeriesQuery().withSort(SeriesQuery.Sort.TITLE, true);
      DublinCoreCatalogList r = seriesService.getSeries(q);
      Assert.assertEquals(2, r.getCatalogList().size());
      Assert.assertEquals("ABC", r.getCatalogList().get(0).getFirst(DublinCore.PROPERTY_TITLE));
    }
    {
      SeriesQuery q = new SeriesQuery().withSort(SeriesQuery.Sort.TITLE, false);
      DublinCoreCatalogList r = seriesService.getSeries(q);
      Assert.assertEquals(2, r.getCatalogList().size());
      Assert.assertEquals("Land and Vegetation: Key players on the Climate Scene",
              r.getCatalogList().get(0).getFirst(DublinCore.PROPERTY_TITLE));
    }
    {
      SeriesQuery q = new SeriesQuery().withSort(SeriesQuery.Sort.SUBJECT, true);
      DublinCoreCatalogList r = seriesService.getSeries(q);
      Assert.assertEquals(2, r.getCatalogList().size());
      Assert.assertEquals("climate, land, vegetation", r.getCatalogList().get(0).getFirst(DublinCore.PROPERTY_SUBJECT));
    }
    {
      SeriesQuery q = new SeriesQuery().withSort(SeriesQuery.Sort.SUBJECT, false);
      DublinCoreCatalogList r = seriesService.getSeries(q);
      Assert.assertEquals(2, r.getCatalogList().size());
      Assert.assertEquals("x, y, z", r.getCatalogList().get(0).getFirst(DublinCore.PROPERTY_SUBJECT));
    }
  }

  @Test
  public void testSeriesQuery() throws Exception {
    testCatalog.set(DublinCore.PROPERTY_TITLE, "Some title");
    seriesService.updateSeries(testCatalog);
    SeriesQuery q = new SeriesQuery().setSeriesTitle("other");
    List<DublinCoreCatalog> result = seriesService.getSeries(q).getCatalogList();
    Assert.assertEquals(0, result.size());

    testCatalog.set(DublinCore.PROPERTY_TITLE, "Some other title");
    seriesService.updateSeries(testCatalog);
    result = seriesService.getSeries(q).getCatalogList();
    Assert.assertEquals(1, result.size());
  }

  @Test
  public void testAddingSeriesWithoutID() throws Exception {
    testCatalog.remove(DublinCore.PROPERTY_IDENTIFIER);
    DublinCoreCatalog newSeries = seriesService.updateSeries(testCatalog);
    Assert.assertNotNull("New series DC should be returned", newSeries);
    String id = newSeries.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    Assert.assertNotNull("New series should have id set", id);
  }

  @Test
  public void testACLManagement() throws Exception {
    // sample access control list
    AccessControlList accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "delete", true));

    try {
      seriesService.updateAccessControl("failid", accessControlList);
      Assert.fail("Should fail when adding ACL to nonexistent series,");
    } catch (NotFoundException e) {
      // expected
    }

    seriesService.updateSeries(testCatalog);
    seriesService.updateAccessControl(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER), accessControlList);
    AccessControlList retrievedACL = seriesService.getSeriesAccessControl(testCatalog
            .getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertNotNull(retrievedACL);
    acl = retrievedACL.getEntries();
    Assert.assertEquals(acl.size(), 1);
    Assert.assertEquals("admin", acl.get(0).getRole());

    acl = accessControlList.getEntries();
    acl.clear();
    acl.add(new AccessControlEntry("student", Permissions.Action.READ.toString(), true));
    seriesService.updateAccessControl(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER), accessControlList);
    retrievedACL = seriesService.getSeriesAccessControl(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertNotNull(retrievedACL);
    acl = retrievedACL.getEntries();
    Assert.assertEquals(acl.size(), 1);
    Assert.assertEquals("student", acl.get(0).getRole());
  }

  @Test
  public void testDublinCoreCatalogEquality1() {
    DublinCoreCatalog a = DublinCores.mkOpencast();
    DublinCoreCatalog b = DublinCores.mkOpencast();
    a.set(DublinCore.PROPERTY_IDENTIFIER, "123");
    assertFalse(SeriesServiceImpl.equals(a, b));
    b.set(DublinCore.PROPERTY_IDENTIFIER, "123");
    assertTrue(SeriesServiceImpl.equals(a, b));
    a.set(DublinCore.PROPERTY_CONTRIBUTOR, list(DublinCoreValue.mk("Peter"), DublinCoreValue.mk("Paul")));
    b.set(DublinCore.PROPERTY_CONTRIBUTOR, list(DublinCoreValue.mk("Paul"), DublinCoreValue.mk("Peter")));
    assertFalse(SeriesServiceImpl.equals(a, b));
    //
    b.set(DublinCore.PROPERTY_CONTRIBUTOR, list(DublinCoreValue.mk("Peter"), DublinCoreValue.mk("Paul")));
    assertTrue(SeriesServiceImpl.equals(a, b));
    //
    a.set(DublinCore.PROPERTY_SPATIAL, "room1");
    a.set(DublinCore.PROPERTY_DESCRIPTION, "this is a test lecture");
    b.set(DublinCore.PROPERTY_DESCRIPTION, "this is a test lecture");
    b.set(DublinCore.PROPERTY_SPATIAL, "room1");
    assertTrue(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testDublinCoreCatalogEquality2() {
    DublinCoreCatalog a = DublinCores.mkOpencast();
    DublinCoreCatalog b = DublinCores.mkOpencast();
    a.set(DublinCore.PROPERTY_DESCRIPTION, "this is a test lecture");
    a.set(DublinCore.PROPERTY_SPATIAL, "room1");
    a.set(DublinCore.PROPERTY_IDENTIFIER, "123");
    a.set(DublinCore.PROPERTY_CONTRIBUTOR, list(DublinCoreValue.mk("Peter"), DublinCoreValue.mk("Paul")));
    b.set(DublinCore.PROPERTY_CONTRIBUTOR, list(DublinCoreValue.mk("Peter"), DublinCoreValue.mk("Paul")));
    b.set(DublinCore.PROPERTY_DESCRIPTION, "this is a test lecture");
    b.set(DublinCore.PROPERTY_SPATIAL, "room1");
    b.set(DublinCore.PROPERTY_IDENTIFIER, "123");
    assertTrue(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testDublinCoreCatalogPreservation() throws Exception {
    seriesService.updateSeries(testCatalog2);
    DublinCoreCatalog dc = seriesService.getSeries("10.0000/5820");
    assertTrue(SeriesServiceImpl.equals(testCatalog2, testCatalog2));
    assertTrue(SeriesServiceImpl.equals(dc, dc));
    assertTrue(SeriesServiceImpl.equals(testCatalog2, dc));
  }

  @Test
  public void testACLEquality1() {
    AccessControlList a = new AccessControlList(new AccessControlEntry("a", Permissions.Action.READ.toString(), true),
            new AccessControlEntry("b", Permissions.Action.WRITE.toString(), false));
    AccessControlList b = new AccessControlList(
            new AccessControlEntry("b", Permissions.Action.WRITE.toString(), false), new AccessControlEntry("a",
                    Permissions.Action.READ.toString(), true));
    assertTrue(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testACLEquality2() {
    AccessControlList a = new AccessControlList();
    AccessControlList b = new AccessControlList();
    assertTrue(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testACLEquality3() {
    AccessControlList a = new AccessControlList();
    AccessControlList b = new AccessControlList(new AccessControlEntry("b", Permissions.Action.WRITE.toString(), false));
    assertFalse(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testACLEquality4() {
    AccessControlList a = new AccessControlList(new AccessControlEntry("b", Permissions.Action.WRITE.toString(), false));
    AccessControlList b = new AccessControlList(
            new AccessControlEntry("b", Permissions.Action.WRITE.toString(), false), new AccessControlEntry("b",
                    Permissions.Action.READ.toString(), false));
    assertFalse(SeriesServiceImpl.equals(a, b));
  }

  @Test
  public void testSeriesElements() throws Exception {
    seriesService.updateSeries(testCatalog);
    final String seriesId = testCatalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER);

    assertTrue(seriesService.addSeriesElement(seriesId, ELEMENT_TYPE, ELEMENT_DATA_1));
    assertFalse(seriesService.addSeriesElement(seriesId, ELEMENT_TYPE, ELEMENT_DATA_1));
    assertEquals(ELEMENT_DATA_1, seriesService.getSeriesElementData(seriesId, ELEMENT_TYPE).get());

    assertTrue(seriesService.updateSeriesElement(seriesId, ELEMENT_TYPE, ELEMENT_DATA_2));
    assertEquals(ELEMENT_DATA_2, seriesService.getSeriesElementData(seriesId, ELEMENT_TYPE).get());

    assertTrue(seriesService.deleteSeriesElement(seriesId, ELEMENT_TYPE));
    assertFalse(seriesService.deleteSeriesElement(seriesId, ELEMENT_TYPE));
    assertEquals(Opt.none(), seriesService.getSeriesElementData(seriesId, ELEMENT_TYPE));
  }

}
