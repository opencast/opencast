/*
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

package org.opencastproject.publication.engage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.publication.api.EngagePublicationChannel;
import org.opencastproject.publication.api.EngagePublicationService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EngagePublicationServiceTest {
  private Organization org;
  private EngagePublicationServiceImpl service;
  private SecurityService securityService;
  private SearchService searchService;
  private DownloadDistributionService distributionService;
  private Capture<MediaPackage> capturePublishedMP;
  private static final long PARENT_JOB_ID = 1L;
  private static final long PUB_JOB_ID = 2L;
  private static final long DIST_JOB_ID = 3L;
  private static final long DIST_MERGE_JOB_ID = 4L;
  private Job parentJob;
  private Job pubJob;
  private Job distJob;
  private Job distMergeJob;

  @Before
  public void setUp() throws Exception {
    parentJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(parentJob.getId()).andReturn(PARENT_JOB_ID).anyTimes();
    // Replay later

    pubJob = mockJob(PUB_JOB_ID, null);
    capturePublishedMP = Capture.newInstance();
    searchService = EasyMock.createNiceMock(SearchService.class);
    EasyMock.expect(searchService.add(EasyMock.capture(capturePublishedMP))).andReturn(pubJob);
    // Replay later

    distJob = mockJob(DIST_JOB_ID, "/distribution_job_payload.txt");
    distMergeJob = mockJob(DIST_MERGE_JOB_ID, "/distribution_merge_job_payload.txt");
    distributionService = EasyMock.createNiceMock(DownloadDistributionService.class);
    // Replay later

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(PARENT_JOB_ID)).andReturn(parentJob).anyTimes();
    EasyMock.expect(serviceRegistry.getJob(PUB_JOB_ID)).andReturn(pubJob).anyTimes();
    EasyMock.expect(serviceRegistry.getJob(DIST_JOB_ID)).andReturn(distJob).anyTimes();
    EasyMock.expect(serviceRegistry.getJob(DIST_MERGE_JOB_ID)).andReturn(distMergeJob).anyTimes();
    EasyMock.replay(serviceRegistry);

    Map<String, String> orgProps = new HashMap<String, String>();
    orgProps.put(EngagePublicationService.ENGAGE_URL_PROPERTY, "https://opencast.edu");
    org = new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            DefaultOrganization.DEFAULT_ORGANIZATION_NAME, null, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, orgProps);
    OrganizationDirectoryService orgService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgService.getOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID)).andReturn(org).anyTimes();
    EasyMock.replay(orgService);

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org);
    EasyMock.replay(securityService);

    service = new EngagePublicationServiceImpl();
    service.setSecurityService(securityService);
    service.setServiceRegistry(serviceRegistry);
    service.setSearchService(searchService);
    service.setDownloadDistributionService(distributionService);
  }

  private Job mockJob(Long jobId, String payloadFile) throws Exception {
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getId()).andReturn(jobId).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getDateStarted()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(0L).anyTimes();
    if (payloadFile != null) {
      URI uriPayload = EngagePublicationServiceTest.class.getResource(payloadFile).toURI();
      String payload = new String(Files.readAllBytes(Paths.get(uriPayload)));
      EasyMock.expect(job.getPayload()).andReturn(payload).anyTimes();
    }
    EasyMock.replay(job);
    return job;
  }

  @Test
  public void testPlayerUrl() throws URISyntaxException {
    URI engageURI = new URI("http://engage.org");
    String mpId = "mp-id";

    MediaPackage mp = EasyMock.createNiceMock(MediaPackage.class);
    Id id = new IdImpl(mpId);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    MediaPackageElement element = EasyMock.createNiceMock(MediaPackageElement.class);
    EasyMock.replay(element, mp);

    // Test configured organization player path
    URI result = service.createEngageUri(engageURI, mp);
    assertEquals(engageURI + "/play/" + mpId, result.toString());
  }

  @Test
  public void testDefaultPlayerPath() throws URISyntaxException {
    URI engageURI = new URI("http://engage.org");
    String mpId = "mp-id";

    MediaPackage mp = EasyMock.createNiceMock(MediaPackage.class);
    Id id = new IdImpl(mpId);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    MediaPackageElement element = EasyMock.createNiceMock(MediaPackageElement.class);
    EasyMock.replay(element, mp);

    // Test default player path
    URI result = service.createEngageUri(engageURI, mp);
    assertEquals(engageURI + EngagePublicationService.PLAYER_PATH + mpId, result.toString());

  }

  @Test
  public void testPublish() throws Exception {
    EasyMock.expect(searchService.get(EasyMock.anyString())).andThrow(new NotFoundException("Not found"));
    EasyMock.replay(searchService);

    EasyMock.expect(distributionService.distribute(EasyMock.anyObject(String.class),
            EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class), EasyMock.anyBoolean()))
            .andReturn(distJob);
    EasyMock.replay(distributionService);

    URI uriMP = EngagePublicationServiceTest.class.getResource("/mp_to_be_published.xml").toURI();
    String mp = new String(Files.readAllBytes(Paths.get(uriMP)));
    EasyMock.expect(parentJob.getArguments())
        .andReturn(Arrays.asList(mp, null, null, null, "engage", null, null, null, null, null, null, null, null));
    EasyMock.replay(parentJob);
    String resultPayload = service.process(parentJob);

    assertNotNull(resultPayload);
    MediaPackage resultMp = MediaPackageParser.getFromXml(resultPayload);

    // Check that result mp has a publication element
    Publication[] pubs = resultMp.getPublications();
    assertEquals(1, pubs.length);
    assertEquals(EngagePublicationChannel.CHANNEL_ID, pubs[0].getChannel());

    // Check that captured published mp has the correct elements
    MediaPackage publishedMP = capturePublishedMP.getValue();
    assertEquals(3, publishedMP.getElements().length);
    Track[] tracks = publishedMP.getTracks();
    assertEquals(1, tracks.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "128ba1e6-4553-45c0-8730-4eb954b9f554/presenter.mp4", tracks[0].getURI().toString());
    Catalog[] catalogs = publishedMP.getCatalogs();
    assertEquals(1, catalogs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "d84b6672-ff84-4df5-9ada-f1cdc0f2d901/dublincore.xml", catalogs[0].getURI().toString());
    Attachment[] attachs = publishedMP.getAttachments();
    assertEquals(1, attachs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "ee8a7e51-0666-45b3-ac5a-77da00b075f4/presenter.jpg", attachs[0].getURI().toString());
  }

  @Test
  public void testPublishMerge() throws Exception {
    URI uriMP = EngagePublicationServiceTest.class.getResource("/mp_already_published.xml").toURI();
    String mp = new String(Files.readAllBytes(Paths.get(uriMP)));

    EasyMock.expect(distributionService.distribute(EasyMock.anyObject(String.class),
            EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class), EasyMock.anyBoolean()))
            .andReturn(distMergeJob);
    EasyMock.replay(distributionService);

    MediaPackage searchMp = MediaPackageParser.getFromXml(mp);
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(searchMp).anyTimes();
    EasyMock.replay(searchService);

    // Do not merge force any flavors
    EasyMock.expect(parentJob.getArguments()).andReturn(
        Arrays.asList(mp, null, EngagePublicationService.PUBLISH_STRATEGY_MERGE, null, "engage", null, null, null, null,
            null, null, "dummy/dummy", null));
    EasyMock.replay(parentJob);
    String resultPayload = service.process(parentJob);

    assertNotNull(resultPayload);

    // Check that result mp has a publication element
    MediaPackage resultMp = MediaPackageParser.getFromXml(resultPayload);
    Publication[] pubs = resultMp.getPublications();
    assertEquals(1, pubs.length);
    assertEquals(EngagePublicationChannel.CHANNEL_ID, pubs[0].getChannel());

    // Check that captured published mp has the correct elements, with the merged captions
    // added to it
    MediaPackage publishedMP = capturePublishedMP.getValue();
    assertEquals(4, publishedMP.getElements().length);
    Track[] tracks = publishedMP.getTracks();
    assertEquals(1, tracks.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "128ba1e6-4553-45c0-8730-4eb954b9f554/presenter.mp4", tracks[0].getURI().toString());
    Catalog[] catalogs = publishedMP.getCatalogs();
    assertEquals(1, catalogs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "d84b6672-ff84-4df5-9ada-f1cdc0f2d901/dublincore.xml", catalogs[0].getURI().toString());
    Attachment[] attachs = publishedMP.getAttachments();
    assertEquals(2, attachs.length);
    attachs = publishedMP.getAttachments(MediaPackageElementFlavor.flavor("presenter", "player+preview"));
    assertEquals(1, attachs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "ee8a7e51-0666-45b3-ac5a-77da00b075f4/presenter.jpg", attachs[0].getURI().toString());
    attachs = publishedMP.getAttachments(MediaPackageElementFlavor.flavor("captions", "dfxp"));
    assertEquals(1, attachs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "bdb7b046-0b08-4ffb-bf02-bc194876e47b/captions.xml", attachs[0].getURI().toString());
  }

  @Test
  public void testPublishMergeForceFlavors() throws Exception {
    EasyMock.expect(distributionService.distribute(EasyMock.anyObject(String.class),
            EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class), EasyMock.anyBoolean()))
            .andReturn(distMergeJob);
    EasyMock.replay(distributionService);

    URI uriMP = EngagePublicationServiceTest.class.getResource("/mp_already_published.xml").toURI();
    String mpXml = new String(Files.readAllBytes(Paths.get(uriMP)));
    MediaPackage mp = MediaPackageParser.getFromXml(mpXml);
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(mp).anyTimes();
    EasyMock.replay(searchService);


    uriMP = EngagePublicationServiceTest.class.getResource("/mp_to_be_published.xml").toURI();
    mpXml = new String(Files.readAllBytes(Paths.get(uriMP)));
    mp = MediaPackageParser.getFromXml(mpXml);
    // Use default merge force flavors. Remove "engage" tag from episode catalog so
    // that it's deleted from published mp.
    MediaPackageElement mpe = mp.getElementById("d84b6672-ff84-4df5-9ada-f1cdc0f2d901");
    mpe.clearTags();
    EasyMock.expect(parentJob.getArguments()).andReturn(
        Arrays.asList(mpXml, null, EngagePublicationService.PUBLISH_STRATEGY_MERGE, null, "engage", null, null, null,
            null, null, null, null, null));
    EasyMock.replay(parentJob);
    String resultPayload = service.process(parentJob);

    assertNotNull(resultPayload);

    // Check that result mp has a publication element
    MediaPackage resultMp = MediaPackageParser.getFromXml(resultPayload);
    Publication[] pubs = resultMp.getPublications();
    assertEquals(1, pubs.length);
    assertEquals(EngagePublicationChannel.CHANNEL_ID, pubs[0].getChannel());

    // Check that captured published mp has the correct elements, with the merged captions
    // added to it and episode catalog removed because of default merge-force-flavors.
    MediaPackage publishedMP = capturePublishedMP.getValue();
    assertEquals(3, publishedMP.getElements().length);
    Track[] tracks = publishedMP.getTracks();
    assertEquals(1, tracks.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "128ba1e6-4553-45c0-8730-4eb954b9f554/presenter.mp4", tracks[0].getURI().toString());
    // Catalog deleted because of merge force flavors
    Catalog[] catalogs = publishedMP.getCatalogs();
    assertEquals(0, catalogs.length);
    Attachment[] attachs = publishedMP.getAttachments();
    assertEquals(2, attachs.length);
    attachs = publishedMP.getAttachments(MediaPackageElementFlavor.flavor("presenter", "player+preview"));
    assertEquals(1, attachs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "ee8a7e51-0666-45b3-ac5a-77da00b075f4/presenter.jpg", attachs[0].getURI().toString());
    attachs = publishedMP.getAttachments(MediaPackageElementFlavor.flavor("captions", "dfxp"));
    assertEquals(1, attachs.length);
    assertEquals("https://distribution.edu/engage-player/0210084b-8927-4675-8f6b-f0417ce8c5a7/"
            + "bdb7b046-0b08-4ffb-bf02-bc194876e47b/captions.xml", attachs[0].getURI().toString());
  }

  @Test
  public void testPublishMergeSkip() throws Exception {
    EasyMock.expect(searchService.get(EasyMock.anyString())).andThrow(new NotFoundException("Not found")).anyTimes();
    EasyMock.replay(searchService);

    URI uriMP = EngagePublicationServiceTest.class.getResource("/mp_to_be_published.xml").toURI();
    String mp = new String(Files.readAllBytes(Paths.get(uriMP)));
    EasyMock.expect(parentJob.getArguments()).andReturn(
        Arrays.asList(mp, null, EngagePublicationService.PUBLISH_STRATEGY_MERGE, null, "engage", null, null, null, null,
            null, null, null, null));
    EasyMock.replay(parentJob);
    String resultPayload = service.process(parentJob);

    assertNull(resultPayload);
  }
}
