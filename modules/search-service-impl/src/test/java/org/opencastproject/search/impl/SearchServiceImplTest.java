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


package org.opencastproject.search.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opencastproject.security.api.Permissions.Action.READ;
import static org.opencastproject.security.api.Permissions.Action.WRITE;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseImpl;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Tests the functionality of the search service.
 *
 * todo setup scenario where gathering metadata from both the media package and the dublin core is required
 * (StaticMetadataServiceMediaPackageImpl, StaticMetadataServiceDublinCoreImpl)
 */
public class SearchServiceImplTest {

  /** The search service */
  private SearchServiceImpl service = null;

  /** Service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The solr root directory */
  private final String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";

  /** The access control list returned by the mocked authorization service */
  private AccessControlList acl = null;

  /** The authorization service */
  private AuthorizationService authorizationService = null;

  /** Student role */
  private static final String ROLE_STUDENT = "ROLE_STUDENT";

  /** Other student role */
  private static final String ROLE_OTHER_STUDENT = "ROLE_OTHER_STUDENT";

  private final DefaultOrganization defaultOrganization = new DefaultOrganization();

  /** A user with permissions. */
  private final User userWithPermissions = new JaxbUser("sample", "test", defaultOrganization, new JaxbRole(
          ROLE_STUDENT, defaultOrganization), new JaxbRole(ROLE_OTHER_STUDENT, defaultOrganization), new JaxbRole(
                  defaultOrganization.getAnonymousRole(), defaultOrganization));

  /** A user without permissions. */
  private final User userWithoutPermissions = new JaxbUser("sample", "test", defaultOrganization, new JaxbRole(
          "ROLE_NOTHING", defaultOrganization), new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS,
                  defaultOrganization));

  private final User defaultUser = userWithPermissions;
  private Responder<User> userResponder;
  private Responder<Organization> organizationResponder;
  private SearchServiceDatabaseImpl searchDatabase;

  private static class Responder<A> implements IAnswer<A> {
    private A response;

    Responder(A response) {
      this.response = response;
    }

    public void setResponse(A response) {
      this.response = response;
    }

    @Override
    public A answer() throws Throwable {
      return response;
    }
  }

  @Before
  public void setUp() throws Exception {
    EntityManagerFactory emf = newTestEntityManagerFactory(SearchServiceDatabaseImpl.PERSISTENCE_UNIT);
    EntityManager em = emf.createEntityManager();
    // workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.read(EasyMock.anyObject(URI.class)))
        .andAnswer(() -> getClass().getResourceAsStream("/" + EasyMock.getCurrentArguments()[0].toString()))
        .anyTimes();
    EasyMock.replay(workspace);

    // User, organization and service registry
    userResponder = new Responder<User>(defaultUser);
    organizationResponder = new Responder<Organization>(defaultOrganization);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);

    User anonymous = new JaxbUser("anonymous", "test", defaultOrganization, new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, defaultOrganization));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    em.getTransaction().begin();
    Organization defaultOrg = new DefaultOrganization();
    Organization org = new JpaOrganization(defaultOrg.getId(), defaultOrg.getName(), defaultOrg.getServers(),
        defaultOrg.getAdminRole(), defaultOrg.getAnonymousRole(), defaultOrg.getProperties());
    em.merge(org);
    em.getTransaction().commit();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
    .andReturn(org).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    // mpeg7 service
    Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();

    // Persistence storage
    searchDatabase = new SearchServiceDatabaseImpl();
    searchDatabase.setEntityManagerFactory(emf);
    searchDatabase.activate(null);
    searchDatabase.setSecurityService(securityService);

    // search service
    service = new SearchServiceImpl();

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));

    StaticMetadataService mdService = newStaticMetadataService(workspace);

    SeriesService seriesService = EasyMock.createNiceMock(SeriesService.class);
    DublinCoreCatalog seriesCatalog = getSeriesDublinCoreCatalog("/series-dublincore.xml");
    AccessControlList seriesAcl = new AccessControlList();
    EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andReturn(seriesCatalog).anyTimes();
    EasyMock.expect(seriesService.getSeriesAccessControl((String) EasyMock.anyObject())).andReturn(seriesAcl)
            .anyTimes();
    EasyMock.replay(seriesService);

    service.setStaticMetadataService(mdService);
    service.setWorkspace(workspace);
    service.setMpeg7CatalogService(mpeg7CatalogService);
    service.setSecurityService(securityService);
    service.setOrganizationDirectoryService(organizationDirectoryService);
    service.setUserDirectoryService(userDirectoryService);
    service.setServiceRegistry(serviceRegistry);
    service.setPersistence(searchDatabase);
    SolrServer solrServer = SearchServiceImpl.setupSolr(new File(solrRoot));
    service.testSetup(solrServer, new SolrRequester(solrServer, securityService), new SolrIndexManager(solrServer,
            workspace, Arrays.asList(mdService), seriesService, mpeg7CatalogService, securityService));

    // acl
    String anonymousRole = securityService.getOrganization().getAnonymousRole();
    acl = new AccessControlList(new AccessControlEntry(anonymousRole, Permissions.Action.READ.toString(), true));
    authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
    .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    EasyMock.expect(
            authorizationService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();
    service.setAuthorizationService(authorizationService);
    EasyMock.replay(authorizationService);
  }

  private StaticMetadataService newStaticMetadataService(Workspace workspace) {
    StaticMetadataServiceDublinCoreImpl service = new StaticMetadataServiceDublinCoreImpl();
    service.setWorkspace(workspace);
    return service;
  }

  private DublinCoreCatalog getSeriesDublinCoreCatalog(String path) {
    // marshal the local series catalog
    DublinCoreCatalog seriesDc = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream(path);
      seriesDc = DublinCores.read(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
    return seriesDc;
  }

  @After
  public void tearDown() throws Exception {
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
    searchDatabase = null;
    service.deactivate();
    FileUtils.deleteDirectory(new File(solrRoot));
    service = null;
  }

  /**
   * Test whether an empty search index will work.
   */
  @Test
  public void testEmptySearchIndex() {
    SearchResult result = service.getByQuery(new SearchQuery().withId("foo"));
    assertEquals(0, result.size());
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testGetMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    // Make sure it's properly indexed and returned for authorized users
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());

    acl.getEntries().clear();
    acl.getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    job = service.add(mediaPackage);
    barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    // This mediapackage should not be readable by the current user (due to the lack of role ROLE_UNKNOWN)
    q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(0, service.getByQuery(q).size());
  }

  private MediaPackage getMediaPackage(String path) throws MediaPackageException {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream(path);
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
    return mediaPackage;
  }

  /**
   * Tests whether an episode can be found based on its series metadata.
   */
  @Test
  public void testSearchForEpisodeWithSeriesMetadata() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-full.xml");
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    SearchResult episodeMetadataResult = service.getByQuery(new SearchQuery().withText("Vegetation"));
    SearchResult seriesMetadataResult = service.getByQuery(new SearchQuery().withText("Atmospheric Science"));

    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmospheric")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("atmospheric")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("atmospheric science")).size());
    assertEquals(1, episodeMetadataResult.getItems().length);
    assertEquals(1, seriesMetadataResult.getItems().length);
  }

  @Test
  public void testSearchForPartialStrings() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmo")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmos")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmosp")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmosph")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmosphe")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmospher")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmospheri")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Atmospheric")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("atmospheri")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("tmospheric")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("tmospheri")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Vegetatio")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("vege")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("egetatio")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("egetation")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("lecture")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("lectur")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("ecture")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("ectur")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Science")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Scienc")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("ience")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("cienc")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("ducti")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Institute")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("nstitute")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("nstitut")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("Institut")).size());
    assertEquals(1, service.getByQuery(new SearchQuery().withText("2008-03-05")).size());
  }

  @Test
  public void testSorting() throws Exception {
    MediaPackage mediaPackageNewer = getMediaPackage("/manifest-full.xml");
    MediaPackage mediaPackageOlder = getMediaPackage("/manifest-full-older.xml");
    // MH-10573, ensure first job finishes publishing before job2
    Job job = service.add(mediaPackageNewer);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Job job2 = service.add(mediaPackageOlder);
    JobBarrier barrier2 = new JobBarrier(null,serviceRegistry, 1000, job2);
    barrier2.waitForJobs();

    String olderTitle = "Older Recording";
    String newerTitle = "Land and Vegetation: Key players on the Climate Scene";

    SearchQuery query = new SearchQuery();
    query.withSort(SearchQuery.Sort.DATE_CREATED);
    assertEquals(2, service.getByQuery(query).size());
    assertEquals(olderTitle, service.getByQuery(query).getItems()[0].getDcTitle());
    query.withSort(SearchQuery.Sort.DATE_CREATED, false);
    assertEquals(newerTitle, service.getByQuery(query).getItems()[0].getDcTitle());
    // FYI: DATE_MODIFIED is the time of Search update, not DC modified (MH-10573)
    query.withSort(SearchQuery.Sort.DATE_MODIFIED);
    assertEquals(newerTitle, service.getByQuery(query).getItems()[0].getDcTitle());
    query.withSort(SearchQuery.Sort.DATE_MODIFIED, false);
    assertEquals(olderTitle, service.getByQuery(query).getItems()[0].getDcTitle());
    SearchQuery q = new SearchQuery();
    q.withSort(SearchQuery.Sort.TITLE);
    assertEquals(newerTitle, service.getByQuery(q).getItems()[0].getDcTitle());
    query.withSort(SearchQuery.Sort.TITLE, false);
    assertEquals(2, service.getByQuery(q).size());
    assertEquals(olderTitle, service.getByQuery(query).getItems()[0].getDcTitle());
    query.withSort(SearchQuery.Sort.LICENSE);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.SERIES_ID);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.MEDIA_PACKAGE_ID);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.CONTRIBUTOR);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.CREATOR);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.LANGUAGE);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
    query.withSort(SearchQuery.Sort.SUBJECT);
    assertEquals(2, service.getByQuery(query).size()); // Just checking that the search index works for this field
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    // Make sure it's properly indexed and returned
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());

    q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);

    assertEquals(1, service.getByQuery(q).size());

    // Test for various fields
    q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    SearchResult result = service.getByQuery(q);
    assertEquals(1, result.getTotalSize());
    SearchResultItem resultItem = result.getItems()[0];
    assertNotNull(resultItem.getMediaPackage());
    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
  }

  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddFullMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-full.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    // Make sure it's properly indexed and returned
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/2");
    assertEquals(1, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(1, service.getByQuery(q).size());
  }

  /**
   * Test removal from the search index.
   */
  @Test
  public void testDeleteMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();

    // Now take the role away from the user
    userResponder.setResponse(userWithoutPermissions);

    Map<String, Integer> servers = new HashMap<String, Integer>();
    servers.put("http://localhost", 8080);
    organizationResponder.setResponse(new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            DefaultOrganization.DEFAULT_ORGANIZATION_NAME, servers, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, null));

    // Try to delete it
    job = service.delete(mediaPackage.getIdentifier().toString());
    barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to delete mediapackage did not finish", Job.Status.FINISHED, job.getStatus());
    assertEquals("Unauthorized user was able to delete a mediapackage", Boolean.FALSE.toString(),
            job.getPayload());

    // Second try with a "fixed" roleset
    User adminUser = new JaxbUser("admin", "test", defaultOrganization, new JaxbRole(
            defaultOrganization.getAdminRole(), defaultOrganization));
    userResponder.setResponse(adminUser);
    Date deletedDate = new Date();
    job = service.delete(mediaPackage.getIdentifier().toString());
    barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Unauthorized user was able to delete a mediapackage", Job.Status.FINISHED, job.getStatus());

    // Now go back to the original security service and user
    userResponder.setResponse(defaultUser);
    organizationResponder.setResponse(defaultOrganization);

    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(0, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(0, service.getByQuery(q).size());

    q = new SearchQuery();
    q.withDeletedSince(deletedDate);
    assertEquals(1, service.getByQuery(q).size());
  }

  /**
   * Test removal from the search index even when it is missing from database #MH-11616
   */
  @Test
  public void testDeleteIndexNotInDbMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, READ.toString(), true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, WRITE.toString(), true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();

    // Delete the mediapackage from persistence (leave it in index)
    Date dateDeletedFromDb = new Date();
    searchDatabase.deleteMediaPackage(mediaPackage.getIdentifier().toString(), dateDeletedFromDb);

    // Verify it is not marked as deleted
    SearchQuery qDel = new SearchQuery();
    qDel.withDeletedSince(dateDeletedFromDb);
    assertEquals(0, service.getByQuery(qDel).size());

    // Verify that it is still active in index
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId(mediaPackage.getIdentifier().toString());
    assertEquals(1, service.getByQuery(q).size());

    // Try delete it
    job = service.delete(mediaPackage.getIdentifier().toString());
    barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to delete mediapackage did not finish", Job.Status.FINISHED, job.getStatus());

    // Verify that it is now not active in the index
    assertEquals(0, service.getByQuery(q).size());

    // Verify that it is now marked as deleted in the index
    q = new SearchQuery();
    q.withDeletedSince(dateDeletedFromDb);
    assertEquals(1, service.getByQuery(q).size());
  }

  /**
   * Adds a media package with a dublin core catalog for episode and series. Verifies series catalog can be retrieved
   * via search service.
   *
   */
  @Test
  public void testAddSeriesMediaPackage() throws Exception {
    String seriesId = "foobar-series";
    MediaPackage mediaPackage = getMediaPackage("/manifest-full.xml");
    mediaPackage.setSeries(seriesId);

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, 1000, job);
    barrier.waitForJobs();
    assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    User adminUser = new JaxbUser("admin", "test", defaultOrganization,
            new JaxbRole(defaultOrganization.getAdminRole(), defaultOrganization));
    userResponder.setResponse(adminUser);

    // Make sure it's properly indexed and returned
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(false);
    q.includeSeries(true);

    SearchResult result = service.getByQuery(q);
    assertEquals(1, result.size());
    assertEquals(seriesId, result.getItems()[0].getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPopulateIndex() throws Exception {
    // This service registry must return a list of jobs
    List<String> args = new ArrayList<String>();
    args.add(new DefaultOrganization().getId());

    List<Job> jobs = new ArrayList<Job>();
    for (long i = 0; i < 10; i++) {
      MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      mediaPackage.setIdentifier(IdImpl.fromUUID());
      searchDatabase.storeMediaPackage(mediaPackage, acl, new Date());
      String payload = MediaPackageParser.getAsXml(mediaPackage);
      Job job = new JobImpl(i);
      job.setArguments(args);
      job.setPayload(payload);
      job.setStatus(Status.FINISHED);
      jobs.add(job);
    }
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    EasyMock.expect(
            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
                    .andReturn(new JobImpl()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JobImpl()).anyTimes();
    EasyMock.expect(serviceRegistry.getJobs((String) EasyMock.anyObject(), (Status) EasyMock.anyObject()))
    .andReturn(jobs).anyTimes();
    EasyMock.replay(serviceRegistry);

    service.setServiceRegistry(serviceRegistry);

    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(new DefaultOrganization())
    .anyTimes();
    EasyMock.replay(orgDirectory);

    service.setOrganizationDirectoryService(orgDirectory);

    // We should have nothing in the search index
    assertEquals(0, service.getByQuery(new SearchQuery()).size());

    service.populateIndex("System Admin");

    // This time we should have 10 results
    assertEquals(10, service.getByQuery(new SearchQuery()).size());
  }
}
