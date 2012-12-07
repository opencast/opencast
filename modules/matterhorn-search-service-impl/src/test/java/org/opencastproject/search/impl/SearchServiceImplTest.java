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

package org.opencastproject.search.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseImpl;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.SeriesServiceImpl;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workspace.api.Workspace;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

  /** A user with permissions. */
  private final User userWithPermissions = new User("sample", "opencastproject.org", new String[] { ROLE_STUDENT,
          ROLE_OTHER_STUDENT, new DefaultOrganization().getAnonymousRole() });

  /** A user without permissions. */
  private final User userWithoutPermissions = new User("sample", "opencastproject.org", new String[] { "ROLE_NOTHING",
          new DefaultOrganization().getAnonymousRole() });

  private final Organization defaultOrganization = new DefaultOrganization();
  private final User defaultUser = userWithPermissions;

  private Responder<User> userResponder;
  private Responder<Organization> organizationResponder;

  private SearchServiceDatabaseImpl searchDatabase;

  private ComboPooledDataSource pooledDataSource;

  private String storage;

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

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    final File dcFile = new File(getClass().getResource("/dublincore.xml").toURI());
    final File dcSeriesFile = new File(getClass().getResource("/series-dublincore.xml").toURI());
    Assert.assertNotNull(dcFile);

    // workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      public File answer() throws Throwable {
        return EasyMock.getCurrentArguments()[0].toString().contains("series") ? dcSeriesFile : dcFile;
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    // User, organization and service registry
    userResponder = new Responder<User>(defaultUser);
    organizationResponder = new Responder<Organization>(defaultOrganization);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);

    User anonymous = new User("anonymous", DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            new String[] { DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS });
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    // mpeg7 service
    Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();

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

    // Persistence storage
    searchDatabase = new SearchServiceDatabaseImpl();
    searchDatabase.setPersistenceProvider(new PersistenceProvider());
    searchDatabase.setPersistenceProperties(props);
    searchDatabase.activate(null);
    searchDatabase.setSecurityService(securityService);

    // search service
    service = new SearchServiceImpl();

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService);

    StaticMetadataService mdService = newStaticMetadataService(workspace);
    SeriesService seriesService = newSeriesService();
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
    acl = new AccessControlList(new AccessControlEntry(anonymousRole, "read", true));
    authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getAccessControlList((MediaPackage) EasyMock.anyObject())).andReturn(acl)
            .anyTimes();
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

  private SeriesService newSeriesService() {
    SeriesServiceImpl service = new SeriesServiceImpl();
    service.setIndex(new SeriesServiceSolrIndex());
    return service;
  }

  @After
  public void tearDown() throws Exception {
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
    searchDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
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
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

    // Make sure it's properly indexed and returned for authorized users
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());

    acl.getEntries().clear();
    acl.getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    job = service.add(mediaPackage);
    barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

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
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

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
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

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
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

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
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to add mediapckage did not finish", Job.Status.FINISHED, job.getStatus());

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
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    Job job = service.add(mediaPackage);
    JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();

    // Now take the role away from the user
    userResponder.setResponse(userWithoutPermissions);

    Map<String, Integer> servers = new HashMap<String, Integer>();
    servers.put("localhost", 8080);
    Organization org = new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            DefaultOrganization.DEFAULT_ORGANIZATION_NAME, servers, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, null);
    organizationResponder.setResponse(org);

    // Try to delete it
    job = service.delete(mediaPackage.getIdentifier().toString());
    barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Job to delete mediapckage did not finish", Job.Status.FINISHED, job.getStatus());
    Assert.assertEquals("Unauthorized user was able to delete a mediapackage", Boolean.FALSE.toString(),
            job.getPayload());

    // Second try with a "fixed" roleset
    User adminUser = new User("admin", "opencastproject.org", new String[] { new DefaultOrganization().getAdminRole() });
    userResponder.setResponse(adminUser);
    Date deletedDate = new Date();
    job = service.delete(mediaPackage.getIdentifier().toString());
    barrier = new JobBarrier(serviceRegistry, 1000, job);
    barrier.waitForJobs();
    Assert.assertEquals("Unauthorized user was able to delete a mediapackage", Job.Status.FINISHED, job.getStatus());

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
   * Ads a media package with one dublin core for the episode and one for the series.
   * 
   * todo media package needs to return a series id for this test to work
   */
  @Test
  @Ignore
  public void testAddSeriesMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(ROLE_STUDENT, SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    SearchQuery q = new SearchQuery();
    q.includeEpisodes(false);
    q.includeSeries(true);

    SearchResult result = service.getByQuery(q);
    assertEquals(1, result.size());
    assertEquals("foobar-serie", result.getItems()[0].getId());
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
      mediaPackage.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew());
      searchDatabase.storeMediaPackage(mediaPackage, acl, new Date());
      String payload = MediaPackageParser.getAsXml(mediaPackage);
      JaxbJob job = new JaxbJob();
      job.setId(i);
      job.setArguments(args);
      job.setPayload(payload);
      job.setStatus(Status.FINISHED);
      jobs.add(job);
    }
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    EasyMock.expect(
            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JaxbJob()).anyTimes();
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

    service.populateIndex();

    // This time we should have 10 results
    assertEquals(10, service.getByQuery(new SearchQuery()).size());
  }
}
