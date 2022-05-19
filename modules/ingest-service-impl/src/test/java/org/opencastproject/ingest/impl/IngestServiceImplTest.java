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
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.scheduler.api.SchedulerService;
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
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easymock.Capture;
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
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private DublinCoreCatalogService dublinCoreService = null;
  private SeriesService seriesService = null;
  private WorkflowService workflowService = null;
  private WorkflowInstance workflowInstance = null;
  private WorkingFileRepository wfr = null;
  private CloseableHttpResponse httpResponse = null;
  private CloseableHttpClient credClient = null;
  private CloseableHttpClient noCredClient = null;
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

  private MediaPackage ingestMediaPackage;
  private MediaPackage schedulerMediaPackage;

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

  @Before
  public void setUp() throws Exception {
    //This looks dumb, but is required so we can override things in testAuthWhitelist
    setupService();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setupService() throws Exception {
    schedulerMediaPackage = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/source-manifest.xml"), "UTF-8"));

    ingestMediaPackage = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/target-manifest.xml"), "UTF-8"));

    FileUtils.forceMkdir(ingestTempDir);

    // set up service and mock workspace
    wfr = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlTrack);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlAttachment);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(wfr.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlPackage);

    EasyMock.expect(wfr.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())).andReturn(urlPackageOld);

    workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowInstanceID);
    EasyMock.expect(workflowInstance.getState()).andReturn(WorkflowState.STOPPED);

    final Capture<MediaPackage> mp = EasyMock.newCapture();
    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.start((WorkflowDefinition) EasyMock.anyObject(), EasyMock.capture(mp),
            (Map) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(workflowInstance.getMediaPackage()).andAnswer(mp::getValue).anyTimes();
    EasyMock.expect(workflowService.start((WorkflowDefinition) EasyMock.anyObject(),
            (MediaPackage) EasyMock.anyObject(), (Map) EasyMock.anyObject())).andReturn(workflowInstance).anyTimes();
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(workflowInstance).anyTimes();
    EasyMock.expect(workflowService.getWorkflowDefinitionById((String) EasyMock.anyObject()))
            .andReturn(new WorkflowDefinitionImpl()).anyTimes();
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance).anyTimes();

    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);

    Map<String, String> properties = new HashMap<>();
    properties.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, "sample");
    properties.put("agent-name", "matterhorn-agent");
    EasyMock.expect(schedulerService.getCaptureAgentConfiguration(EasyMock.anyString())).andReturn(properties)
            .anyTimes();
    EasyMock.expect(schedulerService.getDublinCore(EasyMock.anyString()))
            .andReturn(DublinCores.read(urlCatalog1.toURL().openStream())).anyTimes();
    EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andReturn(schedulerMediaPackage).anyTimes();

    EasyMock.replay(wfr, workflowInstance, workflowService, schedulerService);

    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(),
            new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization(), "test"));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject())).andReturn(organization)
            .anyTimes();
    EasyMock.expect(organizationDirectoryService.getOrganization((URL) EasyMock.anyObject())).andReturn(organization)
            .anyTimes();
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

    httpResponse = EasyMock.createMock(CloseableHttpResponse.class);
    EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.expect(httpResponse.getFirstHeader("Content-Disposition")).andReturn(contentDispositionHeader).anyTimes();
    EasyMock.expect(httpResponse.getEntity()).andReturn(entity).anyTimes();
    EasyMock.replay(httpResponse);

    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpGet) EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
    EasyMock.replay(httpClient);

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

    if (null == service) {
      service = new IngestServiceImpl() {

        //These are overriden so that we get mock requests, not *actual* requests
        @Override
        protected CloseableHttpClient getAuthedHttpClient() {
          CloseableHttpClient client = EasyMock.createMock(CloseableHttpClient.class);
          try {
            EasyMock.expect(client.execute((HttpGet) EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
            client.close();
            EasyMock.expectLastCall().once();
          } catch (Exception e) { }
          EasyMock.replay(client);
          return client;
        }
        @Override
        protected CloseableHttpClient getNoAuthHttpClient() {
          CloseableHttpClient client = EasyMock.createMock(CloseableHttpClient.class);
          try {
            EasyMock.expect(client.execute((HttpGet) EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
            client.close();
            EasyMock.expectLastCall().once();
          } catch (Exception e) { }
          EasyMock.replay(client);
          return client;
        }
      };
    }
    service.setHttpClient(httpClient);
    service.setOrganizationDirectoryService(organizationDirectoryService);
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
    Dictionary<String, String> p = new Hashtable<>();
    p.put(IngestServiceImpl.DOWNLOAD_SOURCE, "http://localhost.*|http://www.test.com/.*");
    service.updated(p);
    service.testMode = true;
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(ingestTempDir);
  }

  @Test
  public void testThinClient() throws Exception {
    MediaPackage mediaPackage = null;

    // Use default properties
    Dictionary<String, String> properties = new Hashtable<>();
    service.updated(properties);

    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, MediaPackageElements.PRESENTATION_SOURCE, mediaPackage);
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
  public void testContentDisposition() throws Exception {
    MediaPackage mediaPackage = null;

    mediaPackage = service.createMediaPackage();
    try {
      mediaPackage = service.addTrack(URI.create("http://www.test.com/testfile"), null, mediaPackage);
    } catch (Exception e) {
      Assert.fail("Unable to read content dispostion filename: " + e.getMessage());
    }

    try {
      mediaPackage = service.addTrack(urlTrackNoFilename, null, mediaPackage);
      Assert.fail("Allowed adding content without filename!");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  private void testAuthWhitelist(String url, String regex, boolean shouldFail, boolean shouldSendAuth, boolean shouldTouchMocks) throws Exception {
    credClient = EasyMock.createNiceMock(CloseableHttpClient.class);
    noCredClient = EasyMock.createNiceMock(CloseableHttpClient.class);

    //There's one case (accessing the filesystem) where we *don't* expect the mocks to be used
    if (shouldTouchMocks) {
      if (shouldSendAuth) {
        EasyMock.expect(credClient.execute(EasyMock.anyObject())).andReturn(httpResponse).once();
        credClient.close();
        EasyMock.expectLastCall().once();
      } else {
        EasyMock.expect(noCredClient.execute(EasyMock.anyObject())).andReturn(httpResponse).once();
        noCredClient.close();
        EasyMock.expectLastCall().once();
      }
    }
    EasyMock.replay(noCredClient, credClient);

    //Recreate the service so we use our own, custom mocks
    service = new IngestServiceImpl() {
      @Override
      protected CloseableHttpClient getAuthedHttpClient() {
        return credClient;
      }

      @Override
      protected CloseableHttpClient getNoAuthHttpClient() {
        return noCredClient;
      }
    };
    setupService();

    MediaPackage mediaPackage = service.createMediaPackage();

    Dictionary<String, String> props = new Hashtable<>();
    props.put(IngestServiceImpl.DOWNLOAD_SOURCE, regex);
    service.updated(props);

    try {
      service.addTrack(URI.create(url), null, mediaPackage);
    } catch (IOException e) {
      if (!shouldFail) {
        Assert.fail("Should not have failed!");
      }
    }
    EasyMock.verify(credClient, noCredClient);
    EasyMock.reset(credClient, noCredClient);
  }

  @Test
  public void testAuthWhitelist() throws Exception {
    //Test fetching something from something known to be inside the cluster.  This should use the default service-wide trusted client
    testAuthWhitelist("http://localhost/testfile", "", false, true, false);

    //Clear the whitelist, this should *never* send digest auth when fetching files
    testAuthWhitelist("http://www.example.org/testfile", "", false, false, true);
    //Non-matching regex
    testAuthWhitelist("http://www.example.org/testfile", "http://localhost.*", true, false, true);
    //Matching regex
    testAuthWhitelist("http://www.example.org/testfile", "http://localhost.*|http://www.example.org/.*", false, true, true);

    //Local filesystem should be actively rejected.  This file needs to *not* be in the resources directory (look in the impl for why), and needs to be readable by the user running the test
    //NB: This is a horrible, horrible hack, but it's the only way I can think of to get *out* of test-classes.  If you try and ../ your way up above that getResource NPEs, as expected.
    testAuthWhitelist(getClass().getResource("./../../../../").toURI().resolve("../../pom.xml").toString(), ".*", true, false, false);
    //Test to ensure we can't use '..' to get around filters.  Removing the ".." works as expected, see below
    testAuthWhitelist(getClass().getResource("./../impl/IngestServiceImplTest.class").toURI().toString(), ".*", true, false, false);
    testAuthWhitelist(getClass().getResource("./IngestServiceImplTest.class").toURI().toString(), ".*", false, false, false);
  }


  @Test
  public void testSmilCreation() throws Exception {
    service.setWorkingFileRepository(new WorkingFileRepositoryImpl() {
      @Override
      public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in)
              throws IOException {
        File file = new File(FileUtils.getTempDirectory(), mediaPackageElementID);
        file.deleteOnExit();
        FileUtils.write(file, IOUtils.toString(in), "UTF-8");
        return file.toURI();
      }

      @Override
      public InputStream get(String mediaPackageID, String mediaPackageElementID)
              throws NotFoundException, IOException {
        File file = new File(FileUtils.getTempDirectory(), mediaPackageElementID);
        return new FileInputStream(file);
      }
    });

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

    FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl.class, "skipCatalogs", true),
            service, false, true);
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

  @Test
  public void testMergeScheduledMediaPackage() throws Exception {
    MediaPackage ingestMediaPackage = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/source-manifest-partial.xml"), "UTF-8"));

    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(IngestServiceImpl.SKIP_ATTACHMENTS_KEY, "false");
    properties.put(IngestServiceImpl.SKIP_CATALOGS_KEY, "false");
    properties.put(IngestServiceImpl.ADD_ONLY_NEW_FLAVORS_KEY, "false");
    service.updated(properties);

    MediaPackage mergedMediaPackage = service.ingest(ingestMediaPackage).getMediaPackage();
    Assert.assertEquals(4, mergedMediaPackage.getTracks().length);
    Track track = mergedMediaPackage.getTrack("track-1");
    Assert.assertEquals("/vonlya1.mov", track.getURI().toString());
    Assert.assertEquals(4, mergedMediaPackage.getCatalogs().length);
    Assert.assertEquals(2, mergedMediaPackage.getAttachments().length);
    Attachment attachment = mergedMediaPackage.getAttachment("cover");
    Assert.assertEquals("attachments/cover.png", attachment.getURI().toString());

    // Validate fields
    Assert.assertEquals(10045L, mergedMediaPackage.getDuration().doubleValue(), 0L);
    Assert.assertEquals("t2", mergedMediaPackage.getTitle());
    Assert.assertEquals("s2", mergedMediaPackage.getSeries());
    Assert.assertEquals("st2", mergedMediaPackage.getSeriesTitle());
    Assert.assertEquals("l2", mergedMediaPackage.getLicense());
    Assert.assertEquals(1, mergedMediaPackage.getSubjects().length);
    Assert.assertEquals("s2", mergedMediaPackage.getSubjects()[0]);
    Assert.assertEquals(1, mergedMediaPackage.getContributors().length);
    Assert.assertEquals("sd2", mergedMediaPackage.getContributors()[0]);
    Assert.assertEquals(1, mergedMediaPackage.getCreators().length);
    Assert.assertEquals("p2", mergedMediaPackage.getCreators()[0]);


  }

  @Test
  public void testOverwriteAndNoSkip() throws Exception {
    MediaPackage ingestMediaPackage = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/source-manifest-partial.xml"), "UTF-8"));

    Dictionary<String, String> properties = new Hashtable<>();

    MediaPackage mergedMediaPackage = service.ingest(ingestMediaPackage).getMediaPackage();

    // check element skipping
    properties.put(IngestServiceImpl.SKIP_ATTACHMENTS_KEY, "true");
    properties.put(IngestServiceImpl.SKIP_CATALOGS_KEY, "true");
    properties.put(IngestServiceImpl.ADD_ONLY_NEW_FLAVORS_KEY, "true");
    service.updated(properties);

    // Existing Opencast mp has 3 catalogs and 1 attachment, the ingest mp has 4 and 2.
    mergedMediaPackage = service.ingest(ingestMediaPackage).getMediaPackage();
    Assert.assertEquals(0, mergedMediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mergedMediaPackage.getAttachments().length);
  }

  @Test
  public void testVaryEpisodeWithOnlyNewFlavorsFalse() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    boolean isAddOnlyNew;
    // Test with properties and key is false
    properties.put(IngestServiceImpl.ADD_ONLY_NEW_FLAVORS_KEY, "false");
    service.updated(properties);
    isAddOnlyNew = service.isAddOnlyNew;
    Assert.assertFalse("Updated overwrite property to false", isAddOnlyNew);
    testEpisodeUpdateNewAndExisting();
  }

  @Test
  public void testVaryEpisodeWithOnlyNewFlavorsMissingParam() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    boolean isAddOnlyNew;
    // Test with properties file but no overwrite param
    properties = new Hashtable<>();
    properties.put("blahblah", "blahblah");
    service.updated(properties);
    isAddOnlyNew = service.isAddOnlyNew;
    Assert.assertTrue("Is Add Only New defaults to true when param is not found (i.e. commented out)", isAddOnlyNew);

  }

  @Test
  public void testNoPropertiesEpisodeOverwriteParam() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    boolean isAddOnlyNew;
    properties = new Hashtable<>();
    service.updated(properties);
    isAddOnlyNew = service.isAddOnlyNew;
    Assert.assertTrue("Overwrite property defaults to true when param is commented out", isAddOnlyNew);
  }


  @Test
  public void testLegacyMediaPackageId() throws Exception {
    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);

    Map<String, String> properties = new HashMap<String, String>();
    properties.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, "sample");
    properties.put("agent-name", "matterhorn-agent");
    EasyMock.expect(schedulerService.getCaptureAgentConfiguration(EasyMock.anyString())).andReturn(properties)
            .anyTimes();
    EasyMock.expect(schedulerService.getDublinCore(EasyMock.anyString()))
            .andReturn(DublinCores.read(urlCatalog1.toURL().openStream())).anyTimes();
    EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andThrow(new NotFoundException()).once();
    EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andReturn(schedulerMediaPackage).once();
    EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andThrow(new NotFoundException())
            .anyTimes();
    EasyMock.replay(schedulerService);
    service.setSchedulerService(schedulerService);

    final Capture<Map<String, String>> captureConfig = EasyMock.newCapture();
    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.start(EasyMock.anyObject(WorkflowDefinition.class),
            EasyMock.anyObject(MediaPackage.class), EasyMock.capture(captureConfig)))
            .andReturn(new WorkflowInstanceImpl()).once();
    EasyMock.replay(workflowService);
    service.setWorkflowService(workflowService);

    Map<String, String> wfConfig = new HashMap<>();
    wfConfig.put(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY, "6f7a7850-3232-4719-9064-24c9bad2832f");
    service.ingest(ingestMediaPackage, null, wfConfig);
    Assert.assertFalse(captureConfig.getValue().containsKey(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY));
  }

  /**
   * Test four cases: 1) If no config file 2) If config file but no key 3) If key and false value 4) If key and true
   * value
   *
   * @throws Exception
   */
  @Test
  public void testVarySeriesOverwriteConfiguration() throws Exception {
    boolean isAddOnlyNewSeries;
    Dictionary<String, String> properties = makeIngestProperties();

    // Test with no properties
    // NOTE: This test only works if the serivce.update() was not triggered by any previous tests
    testSeriesUpdateNewAndExisting(null);

    // Test with properties and no key
    testSeriesUpdateNewAndExisting(properties);

    // Test with properties and key is true
    isAddOnlyNewSeries = true;
    properties.put(IngestServiceImpl.ADD_ONLY_NEW_FLAVORS_KEY, String.valueOf(isAddOnlyNewSeries));
    testSeriesUpdateNewAndExisting(properties);

    // Test series overwrite key is false
    isAddOnlyNewSeries = false;
    properties.put(IngestServiceImpl.ADD_ONLY_NEW_FLAVORS_KEY, String.valueOf(isAddOnlyNewSeries));
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

  private void testEpisodeUpdateNewAndExisting() throws Exception {
    boolean isAddOnlyNew = service.isAddOnlyNew;
    MediaPackage partialIngestMediaPackage = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/source-manifest-partial.xml"), "UTF-8"));

    WorkflowInstance instance = service.ingest(partialIngestMediaPackage);

    MediaPackage mergedMediaPackage = instance.getMediaPackage();
    Assert.assertEquals(4, mergedMediaPackage.getTracks().length);
    Track track = mergedMediaPackage.getTrack("track-1");
    Assert.assertEquals("/vonlya1.mov", track.getURI().toString());
    // Existing mp has 3 catalogs 1 attachments, ingest mp has 4 and 2.
    Assert.assertEquals(4, mergedMediaPackage.getCatalogs().length);
    Assert.assertEquals(2, mergedMediaPackage.getAttachments().length);
    Attachment attachment = mergedMediaPackage.getAttachment("cover");

    // The live pub is always omitted, regardless of state of isAddOnlyNew
    Publication[] pubs = this.schedulerMediaPackage.getPublications();
    Assert.assertEquals("Pub is part of asset managed mediapackage", 1, pubs.length);
    Publication[] pubs2 = mergedMediaPackage.getPublications();
    Assert.assertEquals("Pub is not added back into ingested mediapackage", 0, pubs2.length);

    // Validate fields
    if (!isAddOnlyNew) { // overwrite
      Assert.assertEquals("attachments/cover.png", attachment.getURI().toString());
      Assert.assertEquals("t2", mergedMediaPackage.getTitle());
      Assert.assertEquals("t2", mergedMediaPackage.getTitle());
      Assert.assertEquals("s2", mergedMediaPackage.getSeries());
      Assert.assertEquals("st2", mergedMediaPackage.getSeriesTitle());
      Assert.assertEquals("l2", mergedMediaPackage.getLicense());
      Assert.assertEquals(1, mergedMediaPackage.getSubjects().length);
      Assert.assertEquals("s2", mergedMediaPackage.getSubjects()[0]);
      Assert.assertEquals(1, mergedMediaPackage.getContributors().length);
      Assert.assertEquals("sd2", mergedMediaPackage.getContributors()[0]);
      Assert.assertEquals(1, mergedMediaPackage.getCreators().length);
      Assert.assertEquals("p2", mergedMediaPackage.getCreators()[0]);

    } else { // no overwrite
      Assert.assertEquals("/cover.png", attachment.getURI().toString());
      Assert.assertEquals("t1", mergedMediaPackage.getTitle());
      Assert.assertEquals("t1", mergedMediaPackage.getTitle());
      Assert.assertEquals("s1", mergedMediaPackage.getSeries());
      Assert.assertEquals("st1", mergedMediaPackage.getSeriesTitle());
      Assert.assertEquals("l1", mergedMediaPackage.getLicense());
      Assert.assertEquals(1, mergedMediaPackage.getSubjects().length);
      Assert.assertEquals("s1", mergedMediaPackage.getSubjects()[0]);
      Assert.assertEquals(1, mergedMediaPackage.getContributors().length);
      Assert.assertEquals("sd1", mergedMediaPackage.getContributors()[0]);
      Assert.assertEquals(1, mergedMediaPackage.getCreators().length);
      Assert.assertEquals("p1", mergedMediaPackage.getCreators()[0]);
    }

    Assert.assertEquals(10045L, mergedMediaPackage.getDuration().doubleValue(), 0L);

  }

  /**
   * Utility to set the default required properties
   *
   * @return default properties
   */
  private Dictionary<String, String> makeIngestProperties() {
    Dictionary<String, String> properties = new Hashtable<>();
    String downloadPassword = "CHANGE_ME";
    String downloadSource = "http://localhost";
    String downloadUser = "opencast_system_account";

    properties.put(IngestServiceImpl.DOWNLOAD_PASSWORD, downloadPassword);
    properties.put(IngestServiceImpl.DOWNLOAD_SOURCE, downloadSource);
    properties.put(IngestServiceImpl.DOWNLOAD_USER, downloadUser);

    return properties;
  }

  /**
   * Test method for {@link org.opencastproject.ingest.impl.IngestServiceImpl#updateSeries(java.net.URI)}
   */
  private void testSeriesUpdateNewAndExisting(Dictionary<String, String> properties) throws Exception {

    // default expectation for series overwrite
    boolean isUpdateSeries = IngestServiceImpl.DEFAULT_ALLOW_SERIES_MODIFICATIONS;

    if (properties != null) {
      service.updated(properties);
      try {
        boolean testForValue = Boolean.parseBoolean(properties.get(IngestServiceImpl.MODIFY_OPENCAST_SERIES_KEY).trim());
        isUpdateSeries = testForValue;
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
    org.easymock.EasyMock.expect(dublinCoreService.load((InputStream) EasyMock.anyObject())).andReturn(series)
            .anyTimes();
    org.easymock.EasyMock.replay(dublinCoreService);
    service.setDublinCoreService(dublinCoreService);

    // Test with mock found series
    seriesService = EasyMock.createNiceMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andReturn(series).once();
    EasyMock.expect(seriesService.updateSeries(series)).andReturn(series).once();
    EasyMock.replay(seriesService);
    service.setSeriesService(seriesService);

    // This is true or false depending on the isAddOnlyNew value
    Assert.assertEquals("Desire to update series is " + isUpdateSeries + ".",
            isUpdateSeries, service.updateSeries(urlCatalog2));

    // Test with mock not found exception
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.updateSeries(series)).andReturn(series).once();
    EasyMock.expect(seriesService.getSeries((String) EasyMock.anyObject())).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    service.setSeriesService(seriesService);

    // This should be true, i.e. create new series, in all cases
    Assert.assertEquals("Always create a new series catalog.", true, service.updateSeries(urlCatalog2));
  }

}
