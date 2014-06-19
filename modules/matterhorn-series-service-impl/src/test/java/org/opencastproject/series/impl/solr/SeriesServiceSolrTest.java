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
package org.opencastproject.series.impl.solr;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Tests indexing: indexing, removing, retrieving, merging and searching.
 *
 */
public class SeriesServiceSolrTest {

  private SeriesServiceSolrIndex index;
  private DublinCoreCatalogService dcService;
  private DublinCoreCatalog testCatalog;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", new DefaultOrganization(), new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE,
            new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    index = new SeriesServiceSolrIndex();
    index.solrRoot = PathSupport.concat("target", Long.toString(System.currentTimeMillis()));
    dcService = new DublinCoreCatalogService();
    index.setDublinCoreService(dcService);
    index.setSecurityService(securityService);
    index.activate(null);

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      testCatalog = dcService.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testIndexing() throws Exception {
    index.updateIndex(testCatalog);
    Assert.assertTrue("Index should contain one instance", index.count() == 1);
  }

  @Test
  public void testDeletion() throws Exception {
    index.updateIndex(testCatalog);
    index.delete(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertTrue("Index should be empty", index.count() == 0);
  }

  @Test
  public void testMergingAndRetrieving() throws Exception {
    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Test Title");

    index.updateIndex(testCatalog);
    index.updateIndex(secondCatalog);
    Assert.assertTrue("Index should contain one instance", index.count() == 1);

    DublinCoreCatalog returnedCatalog = index.getDublinCore(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertTrue("Unexpected Dublin Core",
            "Test Title".equals(returnedCatalog.getFirst(DublinCore.PROPERTY_TITLE)));
  }

  @Test
  public void testSearchingByTitleAndFullText() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "Cats and Dogs");
    firstCatalog.add(DublinCore.PROPERTY_DESCRIPTION, "This lecture tries to give an explanation...");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Nature of Dogs");
    secondCatalog.add(DublinCore.PROPERTY_DESCRIPTION, "Why do dogs chase cats?");

    index.updateIndex(firstCatalog);
    index.updateIndex(secondCatalog);

    SeriesQuery q = new SeriesQuery().setSeriesTitle("cat");
    DublinCoreCatalogList result = index.search(q);
    Assert.assertTrue("Only one title contains 'cat'", result.size() == 1);

    q = new SeriesQuery().setSeriesTitle("dog");
    result = index.search(q);
    Assert.assertTrue("Both titles contains 'dog'", result.size() == 2);

    q = new SeriesQuery().setText("cat");
    result = index.search(q);
    Assert.assertTrue("Both Dublin Cores contains 'cat'", result.size() == 2);
  }

  @Test
  public void testCreatedRangedTest() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "Cats and Dogs");
    firstCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-03");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Nature of Dogs");
    secondCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-05");

    DublinCoreCatalog thirdCatalog = dcService.newInstance();
    thirdCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/3");
    thirdCatalog.add(DublinCore.PROPERTY_TITLE, "Nature");
    thirdCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-07");

    index.updateIndex(firstCatalog);
    index.updateIndex(secondCatalog);
    index.updateIndex(thirdCatalog);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SeriesQuery q = new SeriesQuery().setCreatedFrom(sdf.parse("2007-05-02")).setCreatedTo(sdf.parse("2007-05-06"));
    DublinCoreCatalogList result = index.search(q);
    Assert.assertTrue("Two series satisfy time range", result.size() == 2);
  }

  @Test
  public void testAccessControlManagment() throws Exception {
    // sample access control list
    AccessControlList accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("admin", "delete", true));

    index.updateIndex(testCatalog);
    String seriesID = testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    index.updateSecurityPolicy(seriesID, accessControlList);

    AccessControlList retrievedACL = index.getAccessControl(seriesID);
    Assert.assertNotNull(retrievedACL);
    acl = retrievedACL.getEntries();
    Assert.assertEquals(acl.size(), 1);
    Assert.assertEquals(acl.get(0).getRole(), "admin");

    try {
      index.updateSecurityPolicy("failid", accessControlList);
      Assert.fail("Should fail when indexing ACL to nonexistent series");
    } catch (NotFoundException e) {
      // expected
    }
  }


  @Test
  public void testAccessControlManagmentRewrite() throws Exception {
    // sample access control list
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("anonymous", new DefaultOrganization(), new JaxbRole("ROLE_ANONYMOUS",
            new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    index = new SeriesServiceSolrIndex();
    index.solrRoot = PathSupport.concat("target", Long.toString(System.currentTimeMillis()));
    dcService = new DublinCoreCatalogService();
    index.setDublinCoreService(dcService);
    index.setSecurityService(securityService);
    index.activate(null);

    AccessControlList accessControlList = new AccessControlList();
    List<AccessControlEntry> acl = accessControlList.getEntries();
    acl.add(new AccessControlEntry("ROLE_ANONYMOUS", "read", true));

    index.updateIndex(testCatalog);
    String seriesID = testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    index.updateSecurityPolicy(seriesID, accessControlList);

    SeriesQuery q = new SeriesQuery();
    DublinCoreCatalogList result = index.search(q);
    Assert.assertTrue("Only one anomymous series", result.size() == 1);


    index.updateSecurityPolicy(seriesID, new AccessControlList());
    q = new SeriesQuery();
    result = index.search(q);
    Assert.assertTrue("No anomymous series", result.size() == 0);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    index.deactivate();
    FileUtils.deleteDirectory(new File(index.solrRoot));
    index = null;
  }

}
