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
package org.opencastproject.oaipmh.persistence.impl;

import static org.opencastproject.oaipmh.persistence.QueryBuilder.query;
import static org.opencastproject.oaipmh.persistence.QueryBuilder.queryRepo;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Date;

/**
 * Tests persistence: storing, merging, retrieving and removing.
 */
public class OaiPmhPersistenceTest {

  public static final String REPOSITORY_ID_1 = "repo-1";
  public static final String REPOSITORY_ID_2 = "repo-2";
  private OaiPmhDatabaseImpl oaiPmhDatabase;

  private MediaPackage mp1;
  private MediaPackage mp2;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = SecurityUtil.createSystemUser("admin", new DefaultOrganization());
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    mp1 = MediaPackageSupport.loadFromClassPath("/mp1.xml");
    mp2 = MediaPackageSupport.loadFromClassPath("/mp2.xml");

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.read(uri("series-dublincore.xml")))
            .andAnswer(() -> getClass().getResourceAsStream("/series-dublincore.xml")).anyTimes();
    EasyMock.expect(workspace.read(uri("episode-dublincore.xml")))
            .andAnswer(() -> getClass().getResourceAsStream("/episode-dublincore.xml")).anyTimes();
    EasyMock.expect(workspace.read(uri("mpeg7.xml")))
            .andAnswer(() -> getClass().getResourceAsStream("/mpeg7.xml")).anyTimes();
    EasyMock.expect(workspace.read(uri("series-xacml.xml")))
            .andAnswer(() -> getClass().getResourceAsStream("/series-xacml.xml")).anyTimes();
    EasyMock.replay(workspace);

    oaiPmhDatabase = new OaiPmhDatabaseImpl();
    oaiPmhDatabase.setEntityManagerFactory(newTestEntityManagerFactory(OaiPmhDatabaseImpl.PERSISTENCE_UNIT_NAME));
    oaiPmhDatabase.setSecurityService(securityService);
    oaiPmhDatabase.setWorkspace(workspace);
    oaiPmhDatabase.activate(null);
  }

  @Test
  public void testAdding() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_2);
  }

  @Test
  public void testMerging() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);
    oaiPmhDatabase.store(mp2, REPOSITORY_ID_1);
  }

  @Test
  public void testDeleting() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);

    boolean failed = false;
    try {
      oaiPmhDatabase.delete(mp1.getIdentifier().toString(), "abc");
    } catch (NotFoundException e) {
      failed = true;
    }
    Assert.assertTrue(failed);

    oaiPmhDatabase.delete(mp1.getIdentifier().toString(), REPOSITORY_ID_1);

    SearchResult search = oaiPmhDatabase.search(query().mediaPackageId(mp1).build());
    Assert.assertEquals(1, search.size());
    Assert.assertTrue(search.getItems().get(0).isDeleted());
  }

  @Test
  public void testRetrieving() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);

    SearchResult search = oaiPmhDatabase.search(query().mediaPackageId(mp1).build());
    Assert.assertEquals(1, search.size());
    SearchResultItem searchResultItem = search.getItems().get(0);
    Assert.assertEquals(REPOSITORY_ID_1, searchResultItem.getRepository());
    Assert.assertEquals(mp1.getIdentifier().toString(), searchResultItem.getId());
    Assert.assertEquals(mp1, searchResultItem.getMediaPackage());
    Assert.assertFalse(searchResultItem.isDeleted());
    Assert.assertEquals(DefaultOrganization.DEFAULT_ORGANIZATION_ID, searchResultItem.getOrganization());
    Assert.assertTrue(searchResultItem.getModificationDate() != null);
    Date modificationDate = searchResultItem.getModificationDate();
    Assert.assertEquals(2, searchResultItem.getElements().size());
    for (SearchResultElementItem catalogItem : searchResultItem.getElements()) {
      Assert.assertNotNull(catalogItem.getFlavor());
      Assert.assertNotNull(catalogItem.getXml());
      Assert.assertEquals("dublincore/episode".equals(catalogItem.getFlavor().toLowerCase()),
              catalogItem.isEpisodeDublinCore());
      Assert.assertEquals("dublincore/series".equals(catalogItem.getFlavor().toLowerCase()),
              catalogItem.isSeriesDublinCore());
    }

    Date dateBeforeStoring = new Date();

    oaiPmhDatabase.store(mp1, REPOSITORY_ID_2);

    search = oaiPmhDatabase.search(query().mediaPackageId(mp1).build());
    Assert.assertEquals(2, search.size());

    search = oaiPmhDatabase.search(query().mediaPackageId(mp1).repositoryId(REPOSITORY_ID_2).build());
    Assert.assertEquals(1, search.size());
    searchResultItem = search.getItems().get(0);
    Assert.assertEquals(REPOSITORY_ID_2, searchResultItem.getRepository());
    Assert.assertEquals(mp1.getIdentifier().toString(), searchResultItem.getId());
    Assert.assertEquals(mp1, searchResultItem.getMediaPackage());
    Assert.assertFalse(searchResultItem.isDeleted());
    Assert.assertEquals(DefaultOrganization.DEFAULT_ORGANIZATION_ID, searchResultItem.getOrganization());
    Assert.assertTrue(searchResultItem.getModificationDate() != null);
    Assert.assertTrue(searchResultItem.getModificationDate().after(modificationDate));
    Assert.assertEquals(2, searchResultItem.getElements().size());
    for (SearchResultElementItem catalogItem : searchResultItem.getElements()) {
      Assert.assertNotNull(catalogItem.getFlavor());
      Assert.assertNotNull(catalogItem.getXml());
      Assert.assertEquals("dublincore/episode".equals(catalogItem.getFlavor().toLowerCase()),
              catalogItem.isEpisodeDublinCore());
      Assert.assertEquals("dublincore/series".equals(catalogItem.getFlavor().toLowerCase()),
              catalogItem.isSeriesDublinCore());
    }

    search = oaiPmhDatabase.search(query().mediaPackageId(mp1).repositoryId(REPOSITORY_ID_1).build());
    Assert.assertEquals(1, search.size());

    search = oaiPmhDatabase.search(query().mediaPackageId(mp1).repositoryId(REPOSITORY_ID_2).build());
    Assert.assertEquals(1, search.size());

    search = oaiPmhDatabase
            .search(query().mediaPackageId(mp1).repositoryId(REPOSITORY_ID_2).modifiedAfter(new Date()).build());
    Assert.assertEquals(0, search.size());

    search = oaiPmhDatabase
            .search(query().mediaPackageId(mp1).repositoryId(REPOSITORY_ID_2).modifiedAfter(dateBeforeStoring).build());
    Assert.assertEquals(1, search.size());

    search = oaiPmhDatabase.search(query().modifiedAfter(dateBeforeStoring).build());
    Assert.assertEquals(1, search.size());

    Date dateBeforeDeletion = new Date();

    oaiPmhDatabase.delete(mp2.getIdentifier().toString(), REPOSITORY_ID_2);

    Thread.sleep(10);

    search = oaiPmhDatabase.search(query().modifiedAfter(new Date()).build());
    Assert.assertEquals(0, search.size());

    search = oaiPmhDatabase.search(query().modifiedAfter(dateBeforeDeletion).build());
    Assert.assertEquals(1, search.size());
  }

  @Test
  public void testLimitOffset() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);
    MediaPackage mp2 = (MediaPackage) mp1.clone();
    mp2.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew());
    oaiPmhDatabase.store(mp2, REPOSITORY_ID_2);
    SearchResult search = oaiPmhDatabase.search(query().limit(2).build());
    Assert.assertEquals(2, search.size());

    search = oaiPmhDatabase.search(query().limit(1).build());
    Assert.assertEquals(1, search.size());
    Assert.assertEquals(mp1.getIdentifier().toString(), search.getItems().get(0).getId());

    search = oaiPmhDatabase.search(query().offset(1).build());
    Assert.assertEquals(1, search.size());
    Assert.assertEquals(mp2.getIdentifier().toString(), search.getItems().get(0).getId());
  }

  @Test
  public void testRemovalOfOrphanedElements() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);
    int count = 0;
    for (SearchResultItem searchResultItem : oaiPmhDatabase
            .search(queryRepo(REPOSITORY_ID_1).mediaPackageId(mp1).build()).getItems()) {
      count++;
      Assert.assertNotNull(searchResultItem.getMediaPackage());
      Assert.assertNotNull(searchResultItem.getMediaPackage().getElementById("catalog-1"));
    }
    Assert.assertEquals(1, count);

    mp1.removeElementById("catalog-1");
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);
    count = 0;
    for (SearchResultItem searchResultItem : oaiPmhDatabase
            .search(queryRepo(REPOSITORY_ID_1).mediaPackageId(mp1).build()).getItems()) {
      count++;
      Assert.assertNotNull(searchResultItem.getMediaPackage());
      Assert.assertNull(searchResultItem.getMediaPackage().getElementById("catalog-1"));
    }
    Assert.assertEquals(1, count);
  }

  @Test
  public void testModifyElement() throws Exception {
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);

    MediaPackageElement catalog1 = mp1.getElementById("catalog-1");
    catalog1.setURI(URI.create("foo.xml"));
    catalog1.setChecksum(null);
    oaiPmhDatabase.store(mp1, REPOSITORY_ID_1);

    int count = 0;
    for (SearchResultItem searchResultItem : oaiPmhDatabase
            .search(queryRepo(REPOSITORY_ID_1).mediaPackageId(mp1).build()).getItems()) {
      count++;
      Assert.assertNotNull(searchResultItem.getMediaPackage());
      MediaPackageElement mpe = searchResultItem.getMediaPackage().getElementById("catalog-1");
      Assert.assertNotNull(mpe);
      Assert.assertEquals("foo.xml", mpe.getURI().toString());
    }
    Assert.assertEquals(1, count);
  }
}
