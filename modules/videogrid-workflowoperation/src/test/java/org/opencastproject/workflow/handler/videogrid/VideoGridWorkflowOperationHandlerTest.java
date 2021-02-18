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

package org.opencastproject.workflow.handler.videogrid;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.videogrid.api.VideoGridService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoGridWorkflowOperationHandlerTest {

  private VideoGridWorkflowOperationHandler handler;
  private WorkflowInstanceImpl workflow;
  private WorkflowOperationInstance instance;

  private TrackImpl track;
  private TrackImpl inspectTrack;
  private TrackImpl concatTrack;
  private MediaPackageElement videoGridElement;
  private URI mpSmilURI;
  private EncodingProfile profile;
  private Workspace workspace;
  private Gson gson = new Gson();

  // Constant metadata values
  private static final String ENCODING_PROFILE = "concat-encoding-profile";
  private static final String ENCODING_PROFILE_ID = "concat-samecodec.work";
  private static final String TARGET_FLAVOR_RETURN = "a/b";
  private static final String SOURCE_FLAVOR = "source-flavors";
  private static final String SOURCE_FLAVOR_KEY = "source/*";
  private static final String SOURCE_SMIL_FLAVOR = "source-smil-flavor";
  private static final String SOURCE_SMIL_FLAVOR_KEY = "smil/partial";

  @Before
  public void setUp() throws Exception {

    handler = new VideoGridWorkflowOperationHandler() {
      @Override
      protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
        JobBarrier.Result result = EasyMock.createNiceMock(JobBarrier.Result.class);
        EasyMock.expect(result.isSuccess()).andReturn(true).anyTimes();
        EasyMock.replay(result);
        return result;
      }
    };

    /** Prepare flavor contents **/
    // Smil
    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mpSmilURI = getClass().getResource("/smil_ingest.xml").toURI();

    // Track
    track = new TrackImpl();
    track.setIdentifier("d79699b2-d683-4f0d-95ff-2dc4da3c9c40");                // To match the ID defined in the SMIL
    track.setFlavor(MediaPackageElementFlavor.parseFlavor("source/presenter"));
    VideoStreamImpl videoStream = new VideoStreamImpl("test1");
    videoStream.setFrameWidth(80);
    videoStream.setFrameHeight(30);
    track.setVideo(Arrays.asList(videoStream, null));
    URI trackURI = getClass().getResource("/af3bd8bb-2cd4-4d79-8e64-8d54a96876fe.mp4").toURI();   // Absolute URI
    track.setURI(trackURI);

    // MediaPackage
    MediaPackage mediaPackage = new MediaPackageBuilderImpl().createNew();
    mediaPackage.setIdentifier(new IdImpl("123-456"));
    mediaPackage.add(track);
    mediaPackage.add(mpSmilURI, MediaPackageElement.Type.Catalog, MediaPackageElementFlavor.parseFlavor(SOURCE_SMIL_FLAVOR_KEY));

    /** Create Mocks **/
    instance = EasyMock.createNiceMock(WorkflowOperationInstanceImpl.class);
    EasyMock.expect(instance.getConfiguration("target-flavor")).andReturn(TARGET_FLAVOR_RETURN).anyTimes();

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(mpSmilURI)).andReturn(new File(mpSmilURI)).anyTimes();  // To avoid NullPointerEx when the SMIL is parsed
    EasyMock.expect(workspace.get(trackURI)).andReturn(new File(trackURI)).anyTimes();    // To avoid NullPointerEx when grabbing Absolute Track Path

    workflow = EasyMock.createNiceMock(WorkflowInstanceImpl.class);
    EasyMock.expect(workflow.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflow.getCurrentOperation()).andReturn(instance).anyTimes();

    /** Create Service Mocks **/
    Job videoGridJob = new JobImpl(0);
    List<URI> uris = new ArrayList<URI>();
    uris.add(trackURI);
    videoGridJob.setPayload(gson.toJson(uris));
    VideoGridService videoGridService = EasyMock.createMock(VideoGridService.class);
    EasyMock.expect(videoGridService.createPartialTracks(anyObject(), anyObject())).andReturn(videoGridJob);

    Job inspectJob = new JobImpl(1);
    inspectTrack = new TrackImpl();
    inspectJob.setPayload(MediaPackageElementParser.getAsXml(inspectTrack));
    MediaInspectionService mediaInspectionService = EasyMock.createMock(MediaInspectionService.class);
    EasyMock.expect(mediaInspectionService.enrich(anyObject(), anyBoolean())).andReturn(inspectJob);

    Job concatJob = new JobImpl(2);
    concatTrack = new TrackImpl();
    concatJob.setPayload(MediaPackageElementParser.getAsXml(concatTrack));
    profile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(profile.getIdentifier()).andReturn(ENCODING_PROFILE_ID);
    EasyMock.expect(profile.getApplicableMediaType()).andReturn(EncodingProfile.MediaType.Stream);
    EasyMock.expect(profile.getOutputType()).andReturn(EncodingProfile.MediaType.AudioVisual);
    EasyMock.replay(profile);
    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    EasyMock.expect(composerService.getProfile(ENCODING_PROFILE_ID)).andReturn(profile).anyTimes();
    EasyMock.expect(composerService.getProfile("nothing")).andReturn(null).anyTimes();
    EasyMock.expect(composerService.concat(anyString(), anyObject(), anyBoolean(), anyObject())).andReturn(concatJob);

    EasyMock.replay(videoGridService, workspace, workflow, mediaInspectionService, composerService);

    handler.setVideoGridService(videoGridService);
    handler.setWorkspace(workspace);
    handler.setMediaInspectionService(mediaInspectionService);
    handler.setComposerService(composerService);
  }

  @Test
  public void testComplete() throws Exception {
    EasyMock.expect(instance.getConfiguration(SOURCE_FLAVOR)).andReturn(SOURCE_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(SOURCE_SMIL_FLAVOR)).andReturn(SOURCE_SMIL_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(ENCODING_PROFILE)).andReturn(ENCODING_PROFILE_ID).anyTimes();
    EasyMock.replay(instance);
    WorkflowOperationResult result = handler.start(workflow, null);
    Assert.assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());

    MediaPackage mpNew = result.getMediaPackage();
    Track[] tracks = mpNew.getTracks(MediaPackageElementFlavor.parseFlavor(TARGET_FLAVOR_RETURN));
    Assert.assertEquals(1, tracks.length);
  }

  @Test
  public void testNoTracks() throws Exception {
    EasyMock.expect(instance.getConfiguration(SOURCE_FLAVOR)).andReturn("*/nothing").anyTimes();
    EasyMock.expect(instance.getConfiguration(SOURCE_SMIL_FLAVOR)).andReturn(SOURCE_SMIL_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(ENCODING_PROFILE)).andReturn(ENCODING_PROFILE_ID).anyTimes();
    EasyMock.replay(instance);
    Assert.assertTrue(handler.start(workflow, null).allowsContinue());
  }

  @Test
  public void testNoSmil() {
    EasyMock.expect(instance.getConfiguration(SOURCE_FLAVOR)).andReturn(SOURCE_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(SOURCE_SMIL_FLAVOR)).andReturn("smil/nothing").anyTimes();
    EasyMock.expect(instance.getConfiguration(ENCODING_PROFILE)).andReturn(ENCODING_PROFILE_ID).anyTimes();
    EasyMock.replay(instance);
    try {
      handler.start(workflow, null);
    } catch (Exception e) {
      return;
    }
    // We expect this to fail and the test should never reach this point
    Assert.fail();
  }

  @Test
  public void testNoEncodingProfile() {
    EasyMock.expect(instance.getConfiguration(SOURCE_FLAVOR)).andReturn(SOURCE_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(SOURCE_SMIL_FLAVOR)).andReturn(SOURCE_SMIL_FLAVOR_KEY).anyTimes();
    EasyMock.expect(instance.getConfiguration(ENCODING_PROFILE)).andReturn("nothing").anyTimes();
    EasyMock.replay(instance);
    try {
      handler.start(workflow, null);
    } catch (Exception e) {
      return;
    }
    // We expect this to fail and the test should never reach this point
    Assert.fail();
  }
}
