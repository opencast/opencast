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

package org.opencastproject.ingest.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private DublinCoreCatalogService dublinCoreService = null;
  private SeriesService seriesService = null;
  private WorkflowService workflowService = null;
  private WorkflowInstance workflowInstance = null;
  private WorkingFileRepository wfr = null;
  private static URI baseDir;
  private static URI urlTrack;
  private static URI urlTrack1;
  private static URI urlTrack2;
  private static URI urlCatalog;
  private static URI urlCatalog1;
  private static URI urlCatalog2;
  private static URI urlAttachment;
  private static URI urlPackage;
  private static URI urlPackageOld;
  private static URI urlTrackNoFilename;

  private static File ingestTempDir;
  private static File packageFile;

  private static long workflowInstanceID = 1L;
  private ServiceRegistryInMemoryImpl serviceRegistry;

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    baseDir = IngestServiceImplTest.class.getResource("/").toURI();
    urlTrack = IngestServiceImplTest.class.getResource("/av.mov").toURI();
    urlTrack1 = IngestServiceImplTest.class.getResource("/vonly.mov").toURI();
    urlTrack2 = IngestServiceImplTest.class.getResource("/aonly.mov").toURI();
    urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml").toURI();
    urlCatalog1 = IngestServiceImplTest.class.getResource("/dublincore.xml").toURI();
    urlCatalog2 = IngestServiceImplTest.class.getResource("/series-dublincore.xml").toURI();
    urlAttachment = IngestServiceImplTest.class.getResource("/cover.png").toURI();
    urlPackage = IngestServiceImplTest.class.getResource("/data.zip").toURI();
    urlPackageOld = IngestServiceImplTest.class.getResource("/data.old.zip").toURI();
    urlTrackNoFilename = IngestServiceImplTest.class.getResource("/av").toURI();

    ingestTempDir = new File(new File(baseDir), "ingest-temp");
    packageFile = new File(ingestTempDir, baseDir.relativize(urlPackage).toString());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setUp() throws Exception {

    FileUtils.forceMkdir(ingestTempDir);

    // set up service and mock workspace
    wfr = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlAttachment);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlPackage);

    EasyMock.expect(
            wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlPackageOld);

    workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowInstanceID);
    EasyMock.expect(workflowInstance.getState()).andReturn(WorkflowState.STOPPED);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(workflowInstance);
    EasyMock.expect(workflowService.getWorkflowDefinitionById((String) EasyMock.anyObject())).andReturn(
            new WorkflowDefinitionImpl());
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance);

    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);

    Properties properties = new Properties();
    properties.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, "sample");
    properties.put("agent-name", "matterhorn-agent");
    EasyMock.expect(schedulerService.getEventCaptureAgentConfiguration(EasyMock.anyLong())).andReturn(properties)
            .anyTimes();
    EasyMock.expect(schedulerService.getEventDublinCore(EasyMock.anyLong()))
            .andReturn(DublinCores.read(urlCatalog1.toURL().openStream())).anyTimes();

    EasyMock.replay(wfr, workflowInstance, workflowService, schedulerService);

    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(), new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization()));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    HttpEntity entity = EasyMock.createMock(HttpEntity.class);
    InputStream is = getClass().getResourceAsStream("/av.mov");
    byte[] movie = IOUtils.toByteArray(is);
    IOUtils.closeQuietly(is);
    EasyMock.expect(entity.getContent()).andReturn(new ByteArrayInputStream(movie)).anyTimes();
    EasyMock.replay(entity);

    StatusLine statusLine = EasyMock.createMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(200).anyTimes();
    EasyMock.replay(statusLine);

    Header contentDispositionHeader = EasyMock.createMock(Header.class);
    EasyMock.expect(contentDispositionHeader.getValue()).andReturn("attachment; filename=fname.mp4").anyTimes();
    EasyMock.replay(contentDispositionHeader);

    HttpResponse httpResponse = EasyMock.createMock(HttpResponse.class);
    EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.expect(httpResponse.getFirstHeader("Content-Disposition")).andReturn(contentDispositionHeader).anyTimes();
    EasyMock.expect(httpResponse.getEntity()).andReturn(entity).anyTimes();
    EasyMock.replay(httpResponse);

    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpGet) EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
    EasyMock.replay(httpClient);

    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(new AccessControlList(), AclScope.Series)).anyTimes();
    EasyMock.replay(authorizationService);

    MediaInspectionService mediaInspectionService = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(mediaInspectionService.enrich(EasyMock.anyObject(MediaPackageElement.class), EasyMock.anyBoolean()))
            .andAnswer(new IAnswer<Job>() {
              private int i = 0;

              @Override
              public Job answer() throws Throwable {
                TrackImpl element = (TrackImpl) EasyMock.getCurrentArguments()[0];
                element.setDuration(20000L);
                if (i % 2 == 0) {
                  element.addStream(new VideoStreamImpl());
                } else {
                  element.addStream(new AudioStreamImpl());
                }
                i++;
                JobImpl succeededJob = new JobImpl();
                succeededJob.setStatus(Status.FINISHED);
                succeededJob.setPayload(MediaPackageElementParser.getAsXml(element));
                return succeededJob;
              }
            }).anyTimes();
    EasyMock.replay(mediaInspectionService);

    service = new IngestServiceImpl();
    service.setHttpClient(httpClient);
    service.setAuthorizationService(authorizationService);
    service.setWorkingFileRepository(wfr);
    service.setWorkflowService(workflowService);
    service.setSecurityService(securityService);
    service.setSchedulerService(schedulerService);
    service.setMediaInspectionService(mediaInspectionService);
    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    serviceRegistry.registerService(service);
    service.setServiceRegistry(serviceRegistry);
    service.defaultWorkflowDefinionId = "sample";
    serviceRegistry.registerService(service);
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(ingestTempDir);
  }

  @Test
  public void testThinClient() throws Exception {
    MediaPackage mediaPackage = null;

    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, null, mediaPackage);
    mediaPackage = service.addCatalog(urlCatalog1, MediaPackageElements.EPISODE, mediaPackage);
    mediaPackage = service.addAttachment(urlAttachment, MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR, mediaPackage);
    WorkflowInstance instance = service.ingest(mediaPackage);
    Assert.assertEquals(1, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mediaPackage.getAttachments().length);
    Assert.assertEquals(workflowInstanceID, instance.getId());
  }

  @Test
  public void testThickClient() throws Exception {

    FileUtils.copyURLToFile(urlPackage.toURL(), packageFile);

    InputStream packageStream = null;
    try {
      packageStream = urlPackage.toURL().openStream();
      WorkflowInstance instance = service.addZippedMediaPackage(packageStream);

      // Assert.assertEquals(2, mediaPackage.getTracks().length);
      // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
      Assert.assertEquals(workflowInstanceID, instance.getId());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      IOUtils.closeQuietly(packageStream);
    }

  }

  @Test
  public void testThickClientOldMP() throws Exception {

    FileUtils.copyURLToFile(urlPackageOld.toURL(), packageFile);

    InputStream packageStream = null;
    try {
      packageStream = urlPackageOld.toURL().openStream();
      WorkflowInstance instance = service.addZippedMediaPackage(packageStream);

      // Assert.assertEquals(2, mediaPackage.getTracks().length);
      // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
      Assert.assertEquals(workflowInstanceID, instance.getId());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      IOUtils.closeQuietly(packageStream);
    }

  }

  @Test
  public void testStartOver() throws Exception {
    MediaPackage mediaPackage = null;
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("archive", "true");

    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, null, mediaPackage);
    mediaPackage = service.addAttachment(urlAttachment, MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR, mediaPackage);

    service.ingest(mediaPackage, null, properties, 121L);

    Assert.assertEquals(1, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mediaPackage.getAttachments().length);
  }

  @Test
  public void testContentDisposition() throws Exception {
    MediaPackage mediaPackage = null;

    mediaPackage = service.createMediaPackage();
    try {
      mediaPackage = service.addTrack(URI.create("http://www.test.com/testfile"), null, mediaPackage);
    } catch (Exception e) {
      Assert.fail("Unable to read content dispostion filename!");
    }

    try {
      mediaPackage = service.addTrack(urlTrackNoFilename, null, mediaPackage);
      Assert.fail("Allowed adding content without filename!");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testSmilCreation() throws Exception {
    service.setWorkingFileRepository(new WorkingFileRepositoryMockImpl());

    URI presenterUri = URI.create("http://localhost:8080/presenter.mp4");
    URI presenterUri2 = URI.create("http://localhost:8080/presenter2.mp4");
    URI presentationUri = URI.create("http://localhost:8080/presentation.mp4");

    MediaPackage mediaPackage = service.createMediaPackage();
    Catalog[] catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL);
    Assert.assertEquals(0, catalogs.length);

    mediaPackage = service.addPartialTrack(presenterUri, MediaPackageElements.PRESENTER_SOURCE_PARTIAL, 60000L,
            mediaPackage);
    mediaPackage = service.addPartialTrack(presenterUri2, MediaPackageElements.PRESENTER_SOURCE_PARTIAL, 120000L,
            mediaPackage);
    mediaPackage = service.addPartialTrack(presentationUri, MediaPackageElements.PRESENTATION_SOURCE_PARTIAL, 0L,
            mediaPackage);

    catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL);
    Assert.assertEquals(0, catalogs.length);

    service.ingest(mediaPackage);
    catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL);
    Assert.assertEquals(1, catalogs.length);

    Assert.assertEquals(MimeTypes.SMIL, catalogs[0].getMimeType());
    Either<Exception, Document> eitherDoc = XmlUtil.parseNs(new InputSource(catalogs[0].getURI().toURL().openStream()));
    Assert.assertTrue(eitherDoc.isRight());
    Document document = eitherDoc.right().value();
    Assert.assertEquals(1, document.getElementsByTagName("par").getLength());
    Assert.assertEquals(2, document.getElementsByTagName("seq").getLength());
    Assert.assertEquals(2, document.getElementsByTagName("video").getLength());
    Assert.assertEquals(1, document.getElementsByTagName("audio").getLength());

  }

  /**
   * Test four cases: 1) If no config file 2) If config file but no key 3) If key and false value 4) If key and true
   * value
   *
   * @throws Exception
   */
  @Test
  public void testVarySeriesOverwriteConfiguration() throws Exception {
    boolean isOverwriteSeries;
    Dictionary<String, String> properties = new Hashtable<String, String>();

    // Test with no properties
    // NOTE: This test only works if the serivce.update() was not triggered by any previous tests
    testSeriesUpdateNewAndExisting(null);

    // Test with properties and no key
    testSeriesUpdateNewAndExisting(properties);

    // Test with properties and key is true
    isOverwriteSeries = true;
    properties.put(IngestServiceImpl.PROPKEY_OVERWRITE_SERIES, String.valueOf(isOverwriteSeries));
    testSeriesUpdateNewAndExisting(properties);

    // Test series overwrite key is false
    isOverwriteSeries = false;
    properties.put(IngestServiceImpl.PROPKEY_OVERWRITE_SERIES, String.valueOf(isOverwriteSeries));
    testSeriesUpdateNewAndExisting(properties);
  }

  @Test
  public void testFailedJobs() throws Exception {
    Assert.assertEquals(0, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size());
    Assert.assertEquals(0, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size());
    service.addTrack(urlTrack, MediaPackageElements.PRESENTATION_SOURCE, service.createMediaPackage());
    Assert.assertEquals(1, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size());
    Assert.assertEquals(0, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size());
    try {
      service.addTrack(URI.create("file//baduri"), MediaPackageElements.PRESENTATION_SOURCE,
              service.createMediaPackage());
    } catch (Exception e) {
      // Ignore exception
    }
    Assert.assertEquals(1, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size());
    Assert.assertEquals(1, serviceRegistry.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size());
  }

  /**
   * Test method for {@link org.opencastproject.ingest.impl.IngestServiceImpl#updateSeries(java.net.URI)}
   */
  private void testSeriesUpdateNewAndExisting(Dictionary<String, String> properties) throws Exception {

    // default expectation for series overwrite is True
    boolean isExpectSeriesOverwrite = true;

    if (properties != null) {
      service.updated(properties);
      try {
        boolean testForValue = Boolean.parseBoolean(((String) properties.get(IngestServiceImpl.PROPKEY_OVERWRITE_SERIES))
                .trim());
        isExpectSeriesOverwrite = testForValue;
      } catch (Exception e) {
        // If key or value not found or not boolean, use the default overwrite expectation
      }
    }

    // Get test series dublin core for the mock return value
    File catalogFile = new File(urlCatalog2);
    if (!catalogFile.exists() || !catalogFile.canRead())
      throw new Exception("Unable to access test catalog " + urlCatalog2.getPath());
    FileInputStream in = new FileInputStream(catalogFile);
    DublinCoreCatalog series = DublinCores.read(in);
    IOUtils.closeQuietly(in);

    // Set dublinCore service to return test dublin core
    dublinCoreService = org.easymock.EasyMock.createNiceMock(DublinCoreCatalogService.class);
    org.easymock.EasyMock.expect(dublinCoreService.load((InputStream) EasyMock.anyObject()))
            .andReturn(series).anyTimes();
    org.easymock.EasyMock.replay(dublinCoreService);
    service.setDublinCoreService(dublinCoreService);

    // Test with mock found series
    seriesService = EasyMock.createNiceMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andReturn(series).once();
    EasyMock.expect(seriesService.updateSeries(series)).andReturn(series).once();
    EasyMock.replay(seriesService);
    service.setSeriesService(seriesService);

    // This is true or false depending on the isOverwrite value
    Assert.assertEquals("Desire to update series is " + String.valueOf(isExpectSeriesOverwrite) + ".",
            isExpectSeriesOverwrite,
            service.updateSeries(urlCatalog2));

    // Test with mock not found exception
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.updateSeries(series)).andReturn(series).once();
    EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    service.setSeriesService(seriesService);

    // This should be true, i.e. create new series, in all cases
    Assert.assertEquals("Always create a new series catalog.", true, service.updateSeries(urlCatalog2));

  }

  /**
   * Mockup Impl stub of the WorkingFileRepository API for Ingest unit tests
   *
   */
  class WorkingFileRepositoryMockImpl implements WorkingFileRepository {

    @Override
    public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in)
            throws IOException {
      File file = new File(FileUtils.getTempDirectory(), mediaPackageElementID);
      file.deleteOnExit();
      FileUtils.write(file, IOUtils.toString(in), "UTF-8");
      return file.toURI();
    }

    @Override
    public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
      File file = new File(FileUtils.getTempDirectory(), mediaPackageElementID);
      return new FileInputStream(file);
    }

    @Override
    public Option<Long> getTotalSpace() {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public Option<Long> getUsableSpace() {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public Option<Long> getUsedSpace() {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI copyTo(String arg0, String arg1, String arg2, String arg3, String arg4)
            throws NotFoundException, IOException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public boolean delete(String arg0, String arg1) throws IOException {
      // TODO change mock stub as needed for future unit tests
      return false;
    }

    @Override
    public boolean deleteFromCollection(String arg0, String arg1) throws IOException {
      // TODO change mock stub as needed for future unit tests
      return false;
    }

    @Override
    public URI getBaseUri() {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI[] getCollectionContents(String arg0) throws NotFoundException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public long getCollectionSize(String arg0) throws NotFoundException {
      // TODO change mock stub as needed for future unit tests
      return 0;
    }

    @Override
    public URI getCollectionURI(String arg0, String arg1) throws IllegalArgumentException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public String getDiskSpace() {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public InputStream getFromCollection(String arg0, String arg1) throws NotFoundException, IOException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI getURI(String arg0, String arg1) throws IllegalArgumentException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI getURI(String arg0, String arg1, String arg2) throws IllegalArgumentException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI moveTo(String arg0, String arg1, String arg2, String arg3, String arg4)
            throws NotFoundException, IOException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

    @Override
    public URI putInCollection(String arg0, String arg1, InputStream arg2) throws IOException {
      // TODO change mock stub as needed for future unit tests
      return null;
    }

  }
}

