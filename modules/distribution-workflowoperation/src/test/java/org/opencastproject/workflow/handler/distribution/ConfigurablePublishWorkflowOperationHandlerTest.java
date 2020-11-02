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
package org.opencastproject.workflow.handler.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeType;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ConfigurablePublishWorkflowOperationHandlerTest {
  private Organization org;
  private String examplePlayer = "/engage/theodul/ui/core.html?id=";

  @Before
  public void setUp() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(ConfigurablePublishWorkflowOperationHandler.PLAYER_PROPERTY, examplePlayer);
    org = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(org.getProperties()).andStubReturn(properties);
    EasyMock.replay(org);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testNoChannelIdThrowsException() throws WorkflowOperationException {
    MediaPackage mediapackage = EasyMock.createNiceMock(MediaPackage.class);
    WorkflowOperationInstance workflowOperationInstance = EasyMock.createNiceMock(WorkflowOperationInstance.class);
    WorkflowInstance workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andStubReturn(mediapackage);
    EasyMock.expect(workflowInstance.getCurrentOperation()).andStubReturn(workflowOperationInstance);
    JobContext jobContext = EasyMock.createNiceMock(JobContext.class);

    EasyMock.replay(jobContext, mediapackage, workflowInstance, workflowOperationInstance);

    ConfigurablePublishWorkflowOperationHandler configurePublish = new ConfigurablePublishWorkflowOperationHandler();
    configurePublish.start(workflowInstance, jobContext);
  }

  @Test
  public void testNormal() throws WorkflowOperationException, URISyntaxException, DistributionException,
          MediaPackageException {
    String channelId = "engage-player";

    String attachmentId = "attachment-id";
    String catalogId = "catalog-id";
    String trackId = "track-id";

    Attachment attachment = new AttachmentImpl();
    attachment.addTag("engage-download");
    attachment.setIdentifier(attachmentId);
    attachment.setURI(new URI("http://api.com/attachment"));

    Catalog catalog = CatalogImpl.newInstance();
    catalog.addTag("engage-download");
    catalog.setIdentifier(catalogId);
    catalog.setURI(new URI("http://api.com/catalog"));

    Track track = new TrackImpl();
    track.addTag("engage-streaming");
    track.addTag("engage-download");
    track.setIdentifier(trackId);
    track.setURI(new URI("http://api.com/track"));

    Publication publicationtest = new  PublicationImpl(trackId, channelId, new URI("http://api.com/publication"),MimeType.mimeType(trackId, trackId));

    Track unrelatedTrack = new TrackImpl();
    unrelatedTrack.addTag("unrelated");

    Capture<MediaPackageElement> capturePublication = Capture.newInstance();

    MediaPackage mediapackageClone = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediapackageClone.getElements()).andStubReturn(
            new MediaPackageElement[] { attachment, catalog, track, unrelatedTrack });
    EasyMock.expect(mediapackageClone.getIdentifier()).andStubReturn(new IdImpl("mp-id-clone"));
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackageClone);

    MediaPackage mediapackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediapackage.getElements()).andStubReturn(
            new MediaPackageElement[] { attachment, catalog, track, unrelatedTrack });
    EasyMock.expect(mediapackage.clone()).andStubReturn(mediapackageClone);
    EasyMock.expect(mediapackage.getIdentifier()).andStubReturn(new IdImpl("mp-id"));
    mediapackage.add(EasyMock.capture(capturePublication));
    mediapackage.add(publicationtest);
    EasyMock.expect(mediapackage.getPublications()).andStubReturn(new Publication[] {publicationtest});
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackage);

    WorkflowOperationInstance op = EasyMock.createNiceMock(WorkflowOperationInstance.class);
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.CHANNEL_ID_KEY)).andStubReturn(
            channelId);
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.MIME_TYPE)).andStubReturn(
            "text/html");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.URL_PATTERN)).andStubReturn(
            "http://api.opencast.org/api/events/${event_id}");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.DOWNLOAD_SOURCE_TAGS)).andStubReturn(
            "engage-download");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.STREAMING_SOURCE_TAGS)).andStubReturn(
            "engage-streaming");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.CHECK_AVAILABILITY)).andStubReturn(
            "true");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.STRATEGY)).andStubReturn(
            "retract");
    EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.MODE)).andStubReturn(
            "single");
    EasyMock.replay(op);

    WorkflowInstance workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andStubReturn(mediapackage);
    EasyMock.expect(workflowInstance.getCurrentOperation()).andStubReturn(op);
    EasyMock.replay(workflowInstance);

    JobContext jobContext = EasyMock.createNiceMock(JobContext.class);
    EasyMock.replay(jobContext);

    Job attachmentJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(attachmentJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(attachment));
    EasyMock.replay(attachmentJob);

    Job catalogJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(catalogJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(catalog));
    EasyMock.replay(catalogJob);

    Job trackJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(trackJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(track)).anyTimes();
    EasyMock.replay(trackJob);

    Job retractJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(retractJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(track));
    EasyMock.replay(retractJob);

    DownloadDistributionService downloadDistributionService = EasyMock.createNiceMock(DownloadDistributionService.class);
    // Make sure that all of the elements are distributed.
    EasyMock.expect(downloadDistributionService.distribute(channelId, mediapackage, attachmentId, true)).andReturn(
            attachmentJob);
    EasyMock.expect(downloadDistributionService.distribute(channelId, mediapackage, catalogId, true)).andReturn(catalogJob);
    EasyMock.expect(downloadDistributionService.distribute(channelId, mediapackage, trackId, true)).andReturn(trackJob);
    EasyMock.expect(downloadDistributionService.retract(channelId, mediapackage, channelId)).andReturn(retractJob);
    EasyMock.replay(downloadDistributionService);

    StreamingDistributionService streamingDistributionService = EasyMock.createNiceMock(StreamingDistributionService.class);
    EasyMock.expect(streamingDistributionService.publishToStreaming()).andReturn(true).atLeastOnce();
    EasyMock.expect(streamingDistributionService.distribute(channelId, mediapackage, trackId)).andReturn(trackJob).atLeastOnce();
    EasyMock.expect(streamingDistributionService.retract(channelId, mediapackage, channelId)).andReturn(retractJob).atLeastOnce();
    EasyMock.replay(streamingDistributionService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andStubReturn(org);
    EasyMock.replay(securityService);

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.replay(serviceRegistry);

    // Override the waitForStatus method to not block the jobs
    ConfigurablePublishWorkflowOperationHandler configurePublish = new ConfigurablePublishWorkflowOperationHandler() {
      @Override
      protected Result waitForStatus(long timeout, Job... jobs) {
        HashMap<Job, Status> map = Stream.mk(jobs).foldl(new HashMap<Job, Status>(),
                new Fn2<HashMap<Job, Status>, Job, HashMap<Job, Status>>() {
                  @Override
                  public HashMap<Job, Status> apply(HashMap<Job, Status> a, Job b) {
                    a.put(b, Status.FINISHED);
                    return a;
                  }
                });
        return new Result(map);
      }
    };

    configurePublish.setDownloadDistributionService(downloadDistributionService);
    configurePublish.setStreamingDistributionService(streamingDistributionService);
    configurePublish.setSecurityService(securityService);
    configurePublish.setServiceRegistry(serviceRegistry);

    WorkflowOperationResult result = configurePublish.start(workflowInstance, jobContext);
    assertNotNull(result.getMediaPackage());

    assertTrue("The publication element has not been added to the mediapackage.", capturePublication.hasCaptured());
    assertTrue("Some other type of element has been added to the mediapackage instead of the publication element.",
            capturePublication.getValue().getElementType().equals(MediaPackageElement.Type.Publication));
    Publication publication = (Publication) capturePublication.getValue();
    assertEquals(1, publication.getAttachments().length);
    assertNotEquals(attachment.getIdentifier(), publication.getAttachments()[0].getIdentifier());
    attachment.setIdentifier(publication.getAttachments()[0].getIdentifier());
    assertEquals(attachment, publication.getAttachments()[0]);

    assertEquals(1, publication.getCatalogs().length);
    assertNotEquals(catalog.getIdentifier(), publication.getCatalogs()[0].getIdentifier());
    catalog.setIdentifier(publication.getCatalogs()[0].getIdentifier());
    assertEquals(catalog, publication.getCatalogs()[0]);

    assertEquals(2, publication.getTracks().length); //one streaming, one download
    for (Track t: publication.getTracks()) {
      assertNotEquals(track.getIdentifier(), t.getIdentifier());
      track.setIdentifier(t.getIdentifier());
      assertEquals(track, t);
    }

  }

  @Test
  public void testTemplateReplacement() throws WorkflowOperationException, URISyntaxException {
    URI elementUri = new URI("http://element.com/path/to/element/element.mp4");
    String mpId = "mp-id";
    String pubUUID = "test-uuid";
    String seriesId = "series-id";

    MediaPackage mp = EasyMock.createNiceMock(MediaPackage.class);
    Id id = new IdImpl(mpId);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    MediaPackageElement element = EasyMock.createNiceMock(MediaPackageElement.class);
    EasyMock.expect(element.getURI()).andStubReturn(elementUri);
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andStubReturn(org);
    EasyMock.replay(element, mp, securityService);

    // Test player path and mediapackage id
    ConfigurablePublishWorkflowOperationHandler configurePublish = new ConfigurablePublishWorkflowOperationHandler();
    configurePublish.setSecurityService(securityService);
    URI result = configurePublish.populateUrlWithVariables("${player_path}${event_id}", mp, pubUUID);
    assertEquals(examplePlayer + "mp-id", result.toString());

    // Test without series
    configurePublish = new ConfigurablePublishWorkflowOperationHandler();
    configurePublish.setSecurityService(securityService);
    result = configurePublish.populateUrlWithVariables("${series_id}/${event_id}", mp, pubUUID);
    assertEquals("/mp-id", result.toString());

    // Test with series
    mp = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mp.getIdentifier()).andStubReturn(id);
    EasyMock.expect(mp.getSeries()).andStubReturn(seriesId);
    EasyMock.replay(mp);

    configurePublish = new ConfigurablePublishWorkflowOperationHandler();
    configurePublish.setSecurityService(securityService);
    result = configurePublish.populateUrlWithVariables("${series_id}/${event_id}", mp, pubUUID);
    assertEquals("series-id/mp-id", result.toString());

    // Test publication uuid
    configurePublish = new ConfigurablePublishWorkflowOperationHandler();
    configurePublish.setSecurityService(securityService);
    result = configurePublish.populateUrlWithVariables("${publication_id}", mp, pubUUID);
    assertEquals(pubUUID, result.toString());
  }
}
