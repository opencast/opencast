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

package org.opencastproject.workflow.handler.sox;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.sox.api.SoxService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

public class NormalizeAudioWorkflowOperationHandlerTest {

  private NormalizeAudioWorkflowOperationHandler operationHandler;
  private MediaPackage mp;
  private URI uriMP;
  private WorkflowInstance instance;
  private WorkflowOperationInstance operationInstance = new WorkflowOperationInstance();

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    uriMP = NormalizeAudioWorkflowOperationHandler.class.getResource("/sox_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    URI soxTrackUri = NormalizeAudioWorkflowOperationHandler.class.getResource("/sox-track.xml").toURI();
    URI normalizedTrackUri = NormalizeAudioWorkflowOperationHandler.class.getResource("/normalized-track.xml").toURI();
    URI soxEncodeUri = NormalizeAudioWorkflowOperationHandler.class.getResource("/sox-encode-track.xml").toURI();
    URI soxMuxUri = NormalizeAudioWorkflowOperationHandler.class.getResource("/sox-mux-track.xml").toURI();

    String soxTrackXml = IOUtils.toString(soxTrackUri.toURL().openStream());
    String encodeTrackXml = IOUtils.toString(soxEncodeUri.toURL().openStream());
    String normalizedTrackXml = IOUtils.toString(normalizedTrackUri.toURL().openStream());
    String muxedTrackXml = IOUtils.toString(soxMuxUri.toURL().openStream());

    instance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(instance.getMediaPackage()).andReturn(mp).anyTimes();
    EasyMock.expect(instance.getCurrentOperation()).andReturn(operationInstance).anyTimes();
    EasyMock.replay(instance);

    DefaultOrganization org = new DefaultOrganization();
    User anonymous = new JaxbUser("anonymous", "test", org, new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, org));
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

    IncidentService incidentService = EasyMock.createNiceMock(IncidentService.class);
    EasyMock.replay(incidentService);

    ServiceRegistry serviceRegistry = new ServiceRegistryInMemoryImpl(null, securityService, userDirectoryService,
            organizationDirectoryService, incidentService);

    Job analyzeJob = serviceRegistry.createJob(SoxService.JOB_TYPE, "Analyze", null, soxTrackXml, false);
    analyzeJob.setStatus(Status.FINISHED);
    analyzeJob = serviceRegistry.updateJob(analyzeJob);

    Job normalizeJob = serviceRegistry.createJob(SoxService.JOB_TYPE, "Normalize", null, normalizedTrackXml, false);
    normalizeJob.setStatus(Status.FINISHED);
    normalizeJob = serviceRegistry.updateJob(normalizeJob);

    Job encodeJob = serviceRegistry.createJob(ComposerService.JOB_TYPE, "Encode", null, encodeTrackXml, false);
    encodeJob.setStatus(Status.FINISHED);
    encodeJob = serviceRegistry.updateJob(encodeJob);

    Job muxJob = serviceRegistry.createJob(ComposerService.JOB_TYPE, "Mux", null, muxedTrackXml, false);
    muxJob.setStatus(Status.FINISHED);
    muxJob = serviceRegistry.updateJob(muxJob);

    SoxService sox = EasyMock.createNiceMock(SoxService.class);
    EasyMock.expect(sox.analyze((Track) EasyMock.anyObject())).andReturn(analyzeJob).anyTimes();
    EasyMock.expect(sox.normalize((Track) EasyMock.anyObject(), (Float) EasyMock.anyObject())).andReturn(normalizeJob)
            .anyTimes();
    EasyMock.replay(sox);

    ComposerService composer = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composer.encode((Track) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(encodeJob);
    EasyMock.expect(
            composer.mux((Track) EasyMock.anyObject(), (Track) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(muxJob);
    EasyMock.replay(composer);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(new URI("fooVideo.flv"));
    EasyMock.replay(workspace);

    // set up the handler
    operationHandler = new NormalizeAudioWorkflowOperationHandler();
    operationHandler.setJobBarrierPollingInterval(0);
    operationHandler.setComposerService(composer);
    operationHandler.setSoxService(sox);
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testAudioVideo() throws Exception {
    operationInstance.setConfiguration("source-tags", "");
    operationInstance.setConfiguration("source-flavor", "*/video-audio");
    operationInstance.setConfiguration("source-flavors", "");
    operationInstance.setConfiguration("target-flavor", "*/normalized");
    operationInstance.setConfiguration("target-tags", "norm");
    operationInstance.setConfiguration("force-transcode", "false");
    operationInstance.setConfiguration("target-decibel", "-30");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.getMediaPackage()
            .getElements().length);
    Track[] tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "normalized"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    Assert.assertTrue(tracks[0].containsTag("norm"));
    TrackImpl audioVideo = (TrackImpl) tracks[0];
    Assert.assertEquals(-30f, audioVideo.getAudio().get(0).getRmsLevDb().floatValue(), 0.001d);

    tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "video-audio"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    audioVideo = (TrackImpl) tracks[0];
    Assert.assertNull(audioVideo.getAudio().get(0).getRmsLevDb());
  }

  @Test
  public void testAudioContainer() throws Exception {
    operationInstance.setConfiguration("source-tags", "");
    operationInstance.setConfiguration("source-flavor", "*/container-audio");
    operationInstance.setConfiguration("source-flavors", "");
    operationInstance.setConfiguration("target-flavor", "*/normalized");
    operationInstance.setConfiguration("target-tags", "norm");
    operationInstance.setConfiguration("force-transcode", "true");
    operationInstance.setConfiguration("target-decibel", "-30");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.getMediaPackage()
            .getElements().length);
    Track[] tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "normalized"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    Assert.assertTrue(tracks[0].containsTag("norm"));
    TrackImpl audioVideo = (TrackImpl) tracks[0];
    Assert.assertEquals(-30f, audioVideo.getAudio().get(0).getRmsLevDb().floatValue(), 0.001d);

    tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "container-audio"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    audioVideo = (TrackImpl) tracks[0];
    Assert.assertNull(audioVideo.getAudio().get(0).getRmsLevDb());
  }

  @Test
  public void testAudio() throws Exception {
    operationInstance.setConfiguration("source-tags", "");
    operationInstance.setConfiguration("source-flavor", "*/audio");
    operationInstance.setConfiguration("source-flavors", "");
    operationInstance.setConfiguration("target-flavor", "*/normalized");
    operationInstance.setConfiguration("target-tags", "norm");
    operationInstance.setConfiguration("force-transcode", "true");
    operationInstance.setConfiguration("target-decibel", "-30");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.getMediaPackage()
            .getElements().length);
    Track[] tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "normalized"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    Assert.assertTrue(tracks[0].containsTag("norm"));
    TrackImpl audioVideo = (TrackImpl) tracks[0];
    Assert.assertEquals(-30f, audioVideo.getAudio().get(0).getRmsLevDb().floatValue(), 0.001d);

    tracks = result.getMediaPackage().getTracks(new MediaPackageElementFlavor("presentation", "audio"));
    Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.length);
    audioVideo = (TrackImpl) tracks[0];
    Assert.assertNull(audioVideo.getAudio().get(0).getRmsLevDb());
  }

}
