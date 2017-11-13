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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link PartialImportWorkflowOperationHandler}
 */
public class PartialImportWorkflowOperationHandlerTest {

  // Target flavors
  private static final String PRESENTER_TARGET_FLAVOR_STRING = "presenter/target";
  private static final MediaPackageElementFlavor PRESENTER_TARGET_FLAVOR = MediaPackageElementFlavor
          .parseFlavor(PRESENTER_TARGET_FLAVOR_STRING);
  private static final String PRESENTATION_TARGET_FLAVOR_STRING = "presentation/target";
  private static final MediaPackageElementFlavor PRESENTATION_TARGET_FLAVOR = MediaPackageElementFlavor
          .parseFlavor(PRESENTATION_TARGET_FLAVOR_STRING);
  private static List<String> defaultExtensions = new ArrayList<String>();
  private static List<String> moreExtensions = new ArrayList<String>();

  @BeforeClass
  public static void setUpClass() {
    defaultExtensions.add("mp4");
    moreExtensions.add("mp4");
    moreExtensions.add("mov");
  }

  @Test
  public void trackNeedsTobeEncodedToStandardInputMp4ReturnsFalse() throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getURI())
            .andReturn(
                    new URI(
                            "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mp4"))
            .anyTimes();
    EasyMock.replay(track);
    boolean result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions);
    assertFalse(result);
  }

  @Test
  public void trackNeedsTobeEncodedToStandardInputNoExtensionReturnsTrue() throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getURI())
            .andReturn(
                    new URI(
                            "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio"))
            .anyTimes();
    EasyMock.replay(track);
    boolean result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions);
    assertTrue(result);
  }

  @Test
  public void trackNeedsTobeEncodedToStandardInputOnlyPeriodExtensionReturnsTrue() throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getURI())
            .andReturn(
                    new URI(
                            "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio."))
            .anyTimes();
    EasyMock.replay(track);
    boolean result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions);
    assertTrue(result);
  }

  @Test
  public void trackNeedsTobeEncodedToStandardInputMovExtensionOnlyMp4AllowedReturnsTrue() throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getURI())
            .andReturn(
                    new URI(
                            "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mov"))
            .anyTimes();
    EasyMock.replay(track);
    boolean result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, defaultExtensions);
    assertTrue(result);
  }

  @Test
  public void trackNeedsTobeEncodedToStandardInputMovMp4AndMovAllowedReturnsFalse() throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getURI())
            .andReturn(
                    new URI(
                            "http://mh-allinone.localdomain/files/mediapackage/4631bade-04ae-4369-a38f-63a9a0f2e5bf/9404c35b-9463-4932-ad88-0f7030c2448e/audio.mov"))
            .anyTimes();
    EasyMock.replay(track);
    boolean result = PartialImportWorkflowOperationHandler.trackNeedsTobeEncodedToStandard(track, moreExtensions);
    assertFalse(result);
  }

  private Track createTrack(MediaPackageElementFlavor flavor, String filename, boolean video, boolean audio)
          throws URISyntaxException {
    Track track = EasyMock.createMock(Track.class);
    EasyMock.expect(track.getFlavor()).andReturn(flavor).anyTimes();
    EasyMock.expect(track.hasAudio()).andReturn(audio).anyTimes();
    EasyMock.expect(track.hasVideo()).andReturn(video).anyTimes();
    EasyMock.expect(track.getURI()).andReturn(new URI(filename)).anyTimes();
    EasyMock.replay(track);
    return track;
  }

  @Test
  public void getRequiredExtensionsInput3ExtensionsExpect3InList() {
    WorkflowOperationInstance operation = EasyMock.createMock(WorkflowOperationInstance.class);
    EasyMock.expect(operation.getConfiguration("required-extensions")).andReturn("mp4,mov,m4a");
    EasyMock.replay(operation);
    PartialImportWorkflowOperationHandler handler = new PartialImportWorkflowOperationHandler();
    List<String> result = handler.getRequiredExtensions(operation);
    assertEquals("There should be 3 required extensions", 3, result.size());
  }

  @Test
  public void checkForMuxingInputPresenterVideoPresenterAudioNoAudioSuffixExpectsNoMux() throws EncoderException,
          MediaPackageException, WorkflowOperationException, NotFoundException, ServiceRegistryException, IOException,
          URISyntaxException {
    // Setup tracks
    Track audioTrack = createTrack(PRESENTER_TARGET_FLAVOR, "audio.mp4", false, true);
    Track videoTrack = createTrack(PRESENTER_TARGET_FLAVOR, "video.mp4", true, false);
    Track[] tracks = { audioTrack, videoTrack };
    // Setup media package
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();

    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    // Replay all mocks
    EasyMock.replay(composerService, mediaPackage);
    // Make sure that the composer service was not called.
    EasyMock.verify(composerService);

    PartialImportWorkflowOperationHandler handler = new PartialImportWorkflowOperationHandler();
    handler.setComposerService(composerService);
    handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
            new ArrayList<MediaPackageElement>());
  }

  @Test
  public void checkForMuxingInputPresentationVideoPresentationAudioExpectsNoMux() throws EncoderException,
          MediaPackageException, WorkflowOperationException, NotFoundException, ServiceRegistryException, IOException,
          URISyntaxException {
    // Setup tracks
    Track audioTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "audio.mp4", false, true);
    Track videoTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "video.mp4", true, false);
    Track[] tracks = { audioTrack, videoTrack };
    // Setup media package
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();

    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    // Replay all mocks
    EasyMock.replay(composerService, mediaPackage);
    // Make sure that the composer service was not called.
    EasyMock.verify(composerService);

    PartialImportWorkflowOperationHandler handler = new PartialImportWorkflowOperationHandler();
    handler.setComposerService(composerService);
    handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
            new ArrayList<MediaPackageElement>());
  }

  @Test
  public void checkForMuxingInputPresenterVideoPresentationAudioExpectsMux() throws EncoderException,
          MediaPackageException, WorkflowOperationException, NotFoundException, ServiceRegistryException, IOException,
          URISyntaxException {
    // Setup tracks
    Track audioTrack = createTrack(PRESENTER_TARGET_FLAVOR, "audio.mp4", false, true);
    Track videoTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "video.mp4", true, false);
    Track[] tracks = { audioTrack, videoTrack };
    // Setup media package
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    // Create a Job for the mux Job to return.
    Job muxJob = EasyMock.createMock(Job.class);
    EasyMock.expect(muxJob.getId()).andReturn(1L);
    // Create the composer service to track muxing of tracks.
    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    EasyMock.expect(composerService.mux(videoTrack, audioTrack, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE))
            .andReturn(muxJob);
    // Service Registry
    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(muxJob);
    // Replay all mocks
    EasyMock.replay(composerService, mediaPackage, serviceRegistry);

    TestPartialImportWorkflowOperationHandler handler = new TestPartialImportWorkflowOperationHandler(videoTrack,
            audioTrack);
    handler.setComposerService(composerService);
    handler.setServiceRegistry(serviceRegistry);
    handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
            new ArrayList<MediaPackageElement>());

  }

  @Test
  public void checkForMuxingInputPresentationVideoPresenterAudioExpectsMux() throws EncoderException,
          MediaPackageException, WorkflowOperationException, NotFoundException, ServiceRegistryException, IOException,
          URISyntaxException {
    // Setup tracks
    Track audioTrack = createTrack(PRESENTATION_TARGET_FLAVOR, "audio.mp4", false, true);
    Track videoTrack = createTrack(PRESENTER_TARGET_FLAVOR, "video.mp4", true, false);
    Track[] tracks = { audioTrack, videoTrack };
    // Setup media package
    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    // Create a Job for the mux Job to return.
    Job muxJob = EasyMock.createMock(Job.class);
    EasyMock.expect(muxJob.getId()).andReturn(1L);
    // Create the composer service to track muxing of tracks.
    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    EasyMock.expect(composerService.mux(videoTrack, audioTrack, PrepareAVWorkflowOperationHandler.MUX_AV_PROFILE))
            .andReturn(muxJob);
    // Service Registry
    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(muxJob);
    // Replay all mocks
    EasyMock.replay(composerService, mediaPackage, serviceRegistry);

    TestPartialImportWorkflowOperationHandler handler = new TestPartialImportWorkflowOperationHandler(videoTrack,
            audioTrack);
    handler.setComposerService(composerService);
    handler.setServiceRegistry(serviceRegistry);
    handler.checkForMuxing(mediaPackage, PRESENTATION_TARGET_FLAVOR, PRESENTER_TARGET_FLAVOR, false,
            new ArrayList<MediaPackageElement>());
  }

  @Test
  public void testDetermineDimension() throws Exception {
    // Setup tracks
    VideoStreamImpl videoStream = new VideoStreamImpl("test1");
    videoStream.setFrameWidth(80);
    videoStream.setFrameHeight(30);
    VideoStreamImpl videoStream2 = new VideoStreamImpl("test2");
    videoStream2.setFrameWidth(101);
    videoStream2.setFrameHeight(50);

    TrackImpl videoTrack = new TrackImpl();
    videoTrack.setURI(URI.create("/test"));
    videoTrack.setVideo(Collections.list((VideoStream) videoStream));

    TrackImpl videoTrack2 = new TrackImpl();
    videoTrack2.setURI(URI.create("/test"));
    videoTrack2.setVideo(Collections.list((VideoStream) videoStream2));

    List<Track> tracks = Collections.list((Track) videoTrack, (Track) videoTrack2);

    EncodingProfileImpl encodingProfile = new EncodingProfileImpl();
    encodingProfile.setIdentifier("test");

    ComposerService composerService = EasyMock.createMock(ComposerService.class);
    EasyMock.expect(
            composerService.concat(encodingProfile.getIdentifier(), Dimension.dimension(101, 50),
                    false,
                    tracks.toArray(new Track[tracks.size()]))).andReturn(null).once();
    EasyMock.expect(
            composerService.concat(encodingProfile.getIdentifier(), Dimension.dimension(100, 50),
                    false,
                    tracks.toArray(new Track[tracks.size()]))).andReturn(null).once();
    EasyMock.replay(composerService);

    PartialImportWorkflowOperationHandler handler = new PartialImportWorkflowOperationHandler();
    handler.setComposerService(composerService);
    handler.startConcatJob(encodingProfile, tracks, -1.0F, false);
    handler.startConcatJob(encodingProfile, tracks, -1.0F, true);
  }

  /**
   * Test class to verify that muxing is done as expected without circumventing the service registry.
   */
  private class TestPartialImportWorkflowOperationHandler extends PartialImportWorkflowOperationHandler {
    private Track expectedVideo = null;
    private Track expectedAudio = null;

    TestPartialImportWorkflowOperationHandler() {
      super();
    }

    TestPartialImportWorkflowOperationHandler(Track expectedVideo, Track expectedAudio) {
      this.expectedVideo = expectedVideo;
      this.expectedAudio = expectedAudio;
    }

    @Override
    protected long mux(MediaPackage mediaPackage, Track video, Track audio, List<MediaPackageElement> elementsToClean)
            throws EncoderException, MediaPackageException, WorkflowOperationException, NotFoundException,
            ServiceRegistryException, IOException {
      if (expectedVideo == null || expectedAudio == null) {
        Assert.fail("This test was not expected to mux a video and audio track together.");
      } else if (expectedVideo != video || expectedAudio != audio) {
        Assert.fail("The expected tracks are not being muxed together.");
      }
      return 100L;
    }

    @Override
    protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
      JobBarrier.Result result = EasyMock.createMock(JobBarrier.Result.class);
      EasyMock.expect(result.isSuccess()).andReturn(true);
      EasyMock.replay(result);
      return result;
    }
  }
}
