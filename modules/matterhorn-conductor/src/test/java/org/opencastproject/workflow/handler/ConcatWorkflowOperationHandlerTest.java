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
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
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

public class ConcatWorkflowOperationHandlerTest {
  private ConcatWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private MediaPackage mpEncode;
  private Job job;
  private Track[] encodedTracks;

  // mock services and objects
  private EncodingProfile profile = null;
  private ComposerService composerService = null;
  private Workspace workspace = null;

  // constant metadata values
  private static final String PROFILE_ID = "concat";
  private static final String ENCODED_TRACK_ID = "concatenated-workflow-operation-test-encode-track-id";

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = InspectWorkflowOperationHandler.class.getResource("/concat_mediapackage.xml").toURI();
    URI uriMPEncode = InspectWorkflowOperationHandler.class.getResource("/concatenated_mediapackage.xml").toURI();
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
    operationHandler = new ConcatWorkflowOperationHandler();
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testConcat2EncodedTracksWithFlavor() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertEquals("presenter/concat", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
  }

  @Test
  public void testResolutionByTrack() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "part-1");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertEquals("presenter/concat", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
  }

  @Test
  public void testConcat2EncodedTracksWithTags() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("source-tag-part-0", "part0");
    configurations.put("source-tag-part-1", "part1");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertEquals("presenter/concat", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(targetTags.split("\\W"), trackEncoded.getTags());
  }

  @Test
  public void testConcat2EncodedTracksWithSameFlavor() throws Exception {
    setMockups();
    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presenter/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("source-tag-part-0", "part0");
    configurations.put("source-tag-part-1", "part1");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    try {
      // run the operation handler
      getWorkflowOperationResult(mp, configurations);
      Assert.fail();
    } catch (WorkflowOperationException e) {
      Assert.assertNotNull("Does not support two inputs with same flavor!", e);
    }
  }

  private void setMockups() throws EncoderException, MediaPackageException {
    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.AudioVisual);
    EasyMock.expect(profile.getMimeType()).andReturn(MimeTypes.MPEG4.toString()).times(2);
    EasyMock.replay(profile);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.getProfile(PROFILE_ID)).andReturn(profile);
    EasyMock.expect(
            composerService.concat((String) EasyMock.anyObject(), (Dimension) EasyMock.anyObject(),
                    (Track) EasyMock.anyObject(), (Track) EasyMock.anyObject())).andReturn(job);
    EasyMock.replay(composerService);
    operationHandler.setComposerService(composerService);
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("concat");
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
