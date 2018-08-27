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
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiEncodeWorkflowOperationHandlerTest {
  private MultiEncodeWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private MediaPackage mpEncode;
  private Job job;
  private Job job2;
  private Track[] sourceTracks;
  private Track[] encodedTracks;
  private Track[] encodedTracks2;
  private EncodingProfile[] profileList;

  // mock services and objects
  private EncodingProfile profile = null;
  private EncodingProfile profile2 = null;
  private EncodingProfile profile3 = null;
  private ComposerService composerService = null;
  private Workspace workspace = null;

  // constant metadata values
  private static final String PROFILE2_ID = "h264-low.http";
  private static final String PROFILE1_ID = "flv.rtmp";
  private static final String PROFILE3_ID = "webm.preview";
  private static final String SUFFIX2 = "-low.mp4";
  private static final String SUFFIX1 = ".http.flv";
  private static final String SUFFIX3 = ".webm";
  private static final String SOURCE_TRACK_ID1 = "multiencode-workflow-operation-test-source-track-id1";
  private static final String SOURCE_TRACK_ID2 = "multiencode-workflow-operation-test-source-track-id2";
  private static final String ENCODED_TRACK_ID1 = "multiencode-workflow-operation-test-encode-track-id1";
  private static final String ENCODED_TRACK_ID2 = "multiencode-workflow-operation-test-encode-track-id2";
  private static final String ENCODED_TRACK_ID3 = "multiencode-workflow-operation-test-encode-track-id3";
  private static final String ENCODED_TRACK_ID4 = "multiencode-workflow-operation-test-encode-track-id4";

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = InspectWorkflowOperationHandler.class.getResource("/multiencode_mediapackage.xml").toURI(); // has 2
    // tracks
    URI uriMPEncode = InspectWorkflowOperationHandler.class.getResource("/multiencode_results_mediapackage.xml")
            .toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpEncode = builder.loadFromXml(uriMPEncode.toURL().openStream());
    // String theString = IOUtils.toString(uriMPEncode.toURL().openStream());
    sourceTracks = mp.getTracks();
    encodedTracks = mpEncode.getTracks();
    encodedTracks2 = new Track[encodedTracks.length];
    encodedTracks2[0] = (Track) encodedTracks[0].clone();
    encodedTracks2[1] = (Track) encodedTracks[1].clone();
    encodedTracks2[0].setURI(new URI("media" + SUFFIX3));
    encodedTracks2[0].setIdentifier(ENCODED_TRACK_ID3);
    encodedTracks2[1].setIdentifier(ENCODED_TRACK_ID4);
    String encodedXml = MediaPackageElementParser.getArrayAsXml(Arrays.asList(encodedTracks));
    String encodedXml2 = MediaPackageElementParser.getArrayAsXml(Arrays.asList(encodedTracks2));
    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(uriMP);
    EasyMock.replay(workspace);

    // set up mock receipt
    job = EasyMock.createNiceMock(Job.class);
    job2 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(0));
    EasyMock.expect(job.getPayload()).andReturn(encodedXml).anyTimes();
    EasyMock.expect(job2.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(job2.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job2.getDateStarted()).andReturn(new Date());
    EasyMock.expect(job2.getQueueTime()).andReturn(new Long(0));
    EasyMock.expect(job2.getPayload()).andReturn(encodedXml2).anyTimes();
    EasyMock.replay(job, job2);

    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job2);
    EasyMock.replay(serviceRegistry);

    // set up mock profiles
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andStubReturn(PROFILE1_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andStubReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andStubReturn(MediaType.AudioVisual);
    EasyMock.expect(profile.getSuffix()).andStubReturn(SUFFIX1);
    profile2 = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile2.getIdentifier()).andStubReturn(PROFILE2_ID);
    EasyMock.expect(profile2.getApplicableMediaType()).andStubReturn(MediaType.Stream);
    EasyMock.expect(profile2.getOutputType()).andStubReturn(MediaType.Visual);
    EasyMock.expect(profile2.getSuffix()).andStubReturn(SUFFIX2);
    profile3 = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile3.getIdentifier()).andStubReturn(PROFILE3_ID);
    EasyMock.expect(profile3.getApplicableMediaType()).andStubReturn(MediaType.Stream);
    EasyMock.expect(profile3.getOutputType()).andStubReturn(MediaType.Visual);
    EasyMock.expect(profile3.getSuffix()).andStubReturn(SUFFIX3);
    profileList = new EncodingProfile[] { profile, profile2, profile3 };
    EasyMock.replay(profile, profile2, profile3);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.getProfile(PROFILE1_ID)).andStubReturn(profile);
    EasyMock.expect(composerService.getProfile(PROFILE2_ID)).andStubReturn(profile2);
    EasyMock.expect(composerService.getProfile(PROFILE3_ID)).andStubReturn(profile3);
    EasyMock.expect(composerService.multiEncode((Track) EasyMock.anyObject(), (List<String>) EasyMock.anyObject()))
            .andReturn(job);
    EasyMock.expect(composerService.multiEncode((Track) EasyMock.anyObject(), (List<String>) EasyMock.anyObject()))
            .andReturn(job2);
    EasyMock.replay(composerService);

    // set up services
    operationHandler = new MultiEncodeWorkflowOperationHandler();
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
    operationHandler.setComposerService(composerService);
    operationHandler.setJobBarrierPollingInterval(0);
  }

  @Test
  public void testComposeEncodedTrackTwoFlavorsNoTags() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presenter/source;presentation/source");
    configurations.put("target-flavors", "presenter/work;presentation/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE3_ID + ";" + PROFILE2_ID + "," + PROFILE3_ID);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;

    Assert.assertTrue(tracksEncoded.length == 6);
    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertEquals(0, trackEncoded.getTags().length);
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertEquals(0, trackEncoded.getTags().length);
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackTwoFlavorsOneProfileSet() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presenter/source;presentation/source");
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE2_ID);
    configurations.put("target-tags", "1,2"); // 2 tags, no profile tag

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;

    Assert.assertTrue(tracksEncoded.length == 6);
    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertEquals(2, trackEncoded.getTags().length);
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertEquals(2, trackEncoded.getTags().length);
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackTwoFlavors() throws Exception {
    // operation configuration
    String targetTags = "archive,work";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presenter/source;presentation/source");
    configurations.put("target-tags", targetTags); // one
    configurations.put("target-flavors", "presenter/work;presentation/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE3_ID + ";" + PROFILE2_ID + "," + PROFILE3_ID);
    configurations.put("tag-with-profile", "true");
    String[] targetTags1 = { "archive", PROFILE1_ID, "work" };
    String[] targetTags2 = { "archive", PROFILE3_ID, "work" };

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;

    Assert.assertTrue(tracksEncoded.length == 6);
    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags1, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags2, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackWildCardFlavors() throws Exception {
    // operation configuration
    String targetTags = "archive,work";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "*/source");
    configurations.put("target-tags", targetTags); // one
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE3_ID);
    configurations.put("tag-with-profile", "true");
    String[] targetTags1 = { "archive", PROFILE1_ID, "work" };
    String[] targetTags2 = { "archive", PROFILE3_ID, "work" };

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;

    Assert.assertTrue(tracksEncoded.length == 6);
    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags1, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags2, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackOneFlavor() throws Exception {
    // operation configuration - single segment
    String targetTags = "archive,work";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presentation/source");
    configurations.put("target-tags", targetTags); // one
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE2_ID);
    configurations.put("tag-with-profile", "true");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;

    Assert.assertTrue(tracksEncoded.length == 4);
    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    String[] tags = (PROFILE1_ID + "," + targetTags).split(",");
    Arrays.sort(tags);
    Assert.assertArrayEquals(tags, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    tags = (PROFILE2_ID + "," + targetTags).split(",");
    Arrays.sort(tags);
    Assert.assertArrayEquals(tags, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackTags() throws Exception {
    // operation configuration
    String targetTags = "archive;work";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presenter/source;presentation/source");
    configurations.put("target-tags", targetTags); // one
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE3_ID);
    configurations.put("tag-with-profile", "true");
    String[] targetTags1 = { "archive", PROFILE1_ID };
    String[] targetTags2 = { PROFILE3_ID, "work" };

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;
    Assert.assertTrue(tracksEncoded.length == 6);

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags1, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID3);
    Assert.assertTrue("presentation/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags2, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID2.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackSourceTagAndFlavor() throws Exception {
    // operation configuration
    String[] targetTags1 = { "archive", PROFILE1_ID };
    String[] targetTags2 = { "archive", PROFILE2_ID };
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavors", "presenter/source;presentation/source");
    configurations.put("source-tags", "source"); // one
    configurations.put("target-tags", "archive"); // one
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE2_ID);
    configurations.put("tag-with-profile", "true");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;
    Assert.assertTrue(tracksEncoded.length == 4);

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags1, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags2, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeEncodedTrackSourceTagsOnly() throws Exception {
    // operation configuration
    String targetTags = "archive,work";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-tags", "source,archive"); // Should pick both tracks
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavors", "*/work");
    configurations.put("encoding-profiles", PROFILE1_ID + "," + PROFILE2_ID);
    configurations.put("tag-with-profile", "true");
    String[] targetTags1 = { "archive", PROFILE1_ID, "work" };
    String[] targetTags2 = { "archive", PROFILE2_ID, "work" };

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    // check track metadata
    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracksEncoded = mpNew.getTracks();
    Track trackEncoded;
    Assert.assertTrue(tracksEncoded.length == 6);

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID1);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags1, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));

    trackEncoded = mpNew.getTrack(ENCODED_TRACK_ID2);
    Assert.assertTrue("presenter/work".equals(trackEncoded.getFlavor().toString()));
    Assert.assertArrayEquals(targetTags2, trackEncoded.getTags());
    Assert.assertTrue(SOURCE_TRACK_ID1.equals(trackEncoded.getReference().getIdentifier()));
  }

  @Test
  public void testComposeMissingProfile() throws Exception {
    // set up mock profile
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(PROFILE1_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(MediaType.Stream);
    profileList = new EncodingProfile[] { profile };
    EasyMock.replay(profile);

    // set up mock composer service
    composerService = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composerService.listProfiles()).andReturn(profileList);
    EasyMock.expect(composerService.multiEncode((Track) EasyMock.anyObject(), (List<String>) EasyMock.anyObject()))
            .andReturn(job).anyTimes();
    EasyMock.replay(composerService);
    operationHandler.setComposerService(composerService);

    Map<String, String> configurations = new HashMap<String, String>();
    try {
      // no encoding profile
      configurations.put("source-flavors", "multitrack/source");
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
    operation.setTemplate("multiencode");
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
