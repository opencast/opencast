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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
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
    operationHandler.setJobBarrierPollingInterval(0);
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
  public void testResolutionByTrackMandatory() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("source-flavor-part-1-mandatory", "true");
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
  public void testResolutionByTrackNotMandatory() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("source-flavor-part-1-mandatory", "false");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "part-1");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testFrameRateFixedValue() throws Exception {
    createTestFrameRateWithValue("25", 25.0f);
  }

  @Test
  public void testFrameRateFixedDecimalValue() throws Exception {
    createTestFrameRateWithValue("25.000", 25.0f);
  }

  @Test
  public void testFrameRatePartValue() throws Exception {
    Track part1 = (Track) mp.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("presenter/source"))[0];
    VideoStream[] videoStreams = TrackSupport.byType(part1.getStreams(), VideoStream.class);
    createTestFrameRateWithValue("part-1", videoStreams[0].getFrameRate());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testFrameRateInvalidDigitPartValue() throws Exception {
    createTestFrameRateWithValue("part-10", Float.MAX_VALUE);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testFrameRateInvalidPartValue() throws Exception {
    createTestFrameRateWithValue("part-foo", Float.MAX_VALUE);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testFrameRateInvalidValue() throws Exception {
    createTestFrameRateWithValue("foo", Float.MAX_VALUE);
  }

  protected void createTestFrameRateWithValue(String frameRateValue, float expectedFrameRateValue) throws Exception {
    setMockupsWithFrameRate(expectedFrameRateValue);

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "presenter/source");
    configurations.put("source-flavor-part-1-mandatory", "true");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "part-1");
    configurations.put("output-framerate", frameRateValue);

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
    configurations.put("source-tags-part-0", "part0,part0b");
    configurations.put("source-tags-part-1", "part1");
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
  public void testConcatMandatoryCheck() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "test/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testConcatOptionalCheck() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-part-1", "test/source");
    configurations.put("source-flavor-part-1-mandatory", "false");
    configurations.put("source-flavor-part-2", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  @Test
  public void testConcatLessTracks() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracks = mpNew.getTracks(MediaPackageElementFlavor.parseFlavor("presenter/concat"));
    Assert.assertEquals(1, tracks.length);
    Assert.assertArrayEquals(StringUtils.split(targetTags, ","), tracks[0].getTags());
  }


@Test
  public void testConcatNumberedFiles() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-numbered-files", "*/source");
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("target-tags", targetTags);
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertEquals("presenter/concat", trackEncoded.getFlavor().toString());
    Assert.assertArrayEquals(StringUtils.split(targetTags, ","), trackEncoded.getTags());
  }

  @Test
  public void testConcatSingleNumberedFiles() throws Exception {
    setMockups();

    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-numbered-files", "presenter/source");
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("target-tags", targetTags);
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracks = mpNew.getTracks(MediaPackageElementFlavor.parseFlavor("presenter/concat"));
    Track trackEncoded = tracks[0]; // mpNew.getTrack(ENCODED_TRACK_ID);
    Assert.assertArrayEquals(StringUtils.split(targetTags, ","), trackEncoded.getTags());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testConcatNumberedPrefixedFiles() throws Exception {
    setMockups();

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor-part-0", "presentation/source");
    configurations.put("source-flavor-numbered-files", "*/source");
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test(expected = WorkflowOperationException.class)
  public void testConcatNumberedTaggedFiles() throws Exception {
    setMockups();

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-tags-part-1", "part1");
    configurations.put("source-flavor-numbered-files", "*/source");
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("output-resolution", "1900x1080");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testConcatPrefixSameCodecFiles() throws Exception {
    setMockups();

    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-tags-part-1", "part1");
    configurations.put("source-tags-part-2", "part2");
    configurations.put("target-flavor", "presenter/concat");
    configurations.put("encoding-profile", "concat");
    configurations.put("same-codec", "true");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  private void setMockups() throws EncoderException, MediaPackageException {
    setMockupsWithFrameRate(-1.0f);
  }

  private void setMockupsWithFrameRate(float expectedFramerate) throws EncoderException, MediaPackageException {
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
    if (expectedFramerate > 0) {
      EasyMock.expect(composerService.concat(
              (String) EasyMock.anyObject(), (Dimension) EasyMock.anyObject(),
              EasyMock.eq(expectedFramerate),
              EasyMock.anyBoolean(),(Track) EasyMock.anyObject(), (Track) EasyMock.anyObject())).andReturn(job);
    } else {
      EasyMock.expect(composerService.concat(
              (String) EasyMock.anyObject(), (Dimension) EasyMock.anyObject(), EasyMock.anyBoolean(),
              (Track) EasyMock.anyObject(), (Track) EasyMock.anyObject())).andReturn(job);
    }
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
