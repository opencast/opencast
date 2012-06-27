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
package org.opencastproject.workflow.handler;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposeWorkflowOperationHandlerTest {
  private ComposeWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private MediaPackage mpEncode;
  private Job job;
  private Track[] encodedTracks;
  private EncodingProfile[] profileList;

  // mock services and objects
  private EncodingProfile profile = null;
  private ComposerService composerService = null;
  private Workspace workspace = null;

  // constant metadata values
  private static final String PROFILE_ID = "flash.http";
  private static final String SOURCE_TRACK_ID = "compose-workflow-operation-test-source-track-id";
  private static final String ENCODED_TRACK_ID = "compose-workflow-operation-test-encode-track-id";

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = InspectWorkflowOperationHandler.class.getResource("/compose_mediapackage.xml").toURI();
    URI uriMPEncode = InspectWorkflowOperationHandler.class.getResource("/compose_encode_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream());
    encodedTracks = mpEncode.getTracks();

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(uriMP);
    EasyMock.replay(workspace);

    // set up mock receipt
    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(MediaPackageElementParser.getAsXml(encodedTracks[0])).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(0));
    EasyMock.replay(job);

    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    EasyMock.replay(serviceRegistry);

    // set up service
    operationHandler = new ComposeWorkflowOperationHandler();
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testComposeEncodedTrack() throws Exception {
    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.AudioVisual);
    EasyMock.expect(profile.getMimeType()).andReturn(MimeTypes.MPEG4.asString()).times(2);
    profileList = new EncodingProfile[] { profile };
    EasyMock.replay(profile);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.getProfile(PROFILE_ID)).andReturn(profile);
    EasyMock.expect(composerService.encode((Track) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(job);
    EasyMock.replay(composerService);
    operationHandler.setComposerService(composerService);

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/delivery");
    configurations.put("encoding-profile", "flash.http");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertEquals("presenter/delivery", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
    Assert.assertEquals(MimeTypes.MPEG4, trackEncoded.getMimeType());
    Assert.assertEquals(SOURCE_TRACK_ID, trackEncoded.getReference().getIdentifier());
  }

  @Test
  public void testComposeMissingData() throws Exception {
    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getMimeType()).andReturn(MimeTypes.MPEG4.asString()).times(2);
    profileList = new EncodingProfile[] { profile };
    EasyMock.replay(profile);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.listProfiles()).andReturn(profileList);
    EasyMock.expect(
            composerService.encode((Track) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(job);
    EasyMock.replay(composerService);
    operationHandler.setComposerService(composerService);

    Map<String, String> configurations = new HashMap<String, String>();
    try {
      // no source flavour
      getWorkflowOperationResult(mp, configurations);
      Assert.fail("Since neither source audio nor source video flavour is specified exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }

    try {
      // no source flavour
      configurations.put("source-flavor", "presentation/source");
      getWorkflowOperationResult(mp, configurations);
      Assert.fail("Since encoding profile is not specified exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }

  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("compose");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }

}
