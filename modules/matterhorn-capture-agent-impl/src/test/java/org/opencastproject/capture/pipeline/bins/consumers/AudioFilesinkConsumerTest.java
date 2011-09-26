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
package org.opencastproject.capture.pipeline.bins.consumers;

import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Element;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public class AudioFilesinkConsumerTest {

  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  private String bitrateDefault;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      // Gst.deinit();
    }
  }

  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    try {
      @SuppressWarnings("unused")
      Element defaultMuxer = GStreamerElementFactory.getInstance().createElement("Setup for AudioFileSinkBinTest",
              AudioFilesinkConsumer.DEFAULT_MUXER, null);
      Element defaultEncoder = GStreamerElementFactory.getInstance().createElement("Setup for AudioFileSinkBinTest",
              AudioFilesinkConsumer.DEFAULT_ENCODER, null);
      bitrateDefault = defaultEncoder.getPropertyDefaultValue("bitrate").toString();
    } catch (UnableToCreateElementException e) {
      logger.warn("It appears that you have GStreamer installed but not all of the packages that we require. The "
              + "following tests for this class will appear to pass, but in fact are skipped.", e);
      gstreamerInstalled = false;
      return;
    }
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }

  /** Salient encoder properties are codec and bitrate **/
  /** Salient muxer properties are codec and container **/
  private Properties createProperties(String codec, String bitrate, String container) {
    Properties captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, null, codec,
            bitrate, null, container, null);
    return captureDeviceProperties;
  }

  private void checkEncoderProperties(ConsumerBin sinkBin, String codec, String bitrate) {
    Assert.assertTrue(sinkBin.encoder.getName().contains(codec));
    Assert.assertEquals(bitrate, sinkBin.encoder.get("bitrate").toString());
  }

  private void checkMuxerProperties(ConsumerBin sinkBin, String muxer) {
    Assert.assertTrue("The muxer name " + sinkBin.muxer.getName() + " should match the muxer type " + muxer,
            sinkBin.muxer.getName().contains(muxer));
  }

  @Test
  public void nullSettingsForCodecBitrateAndContainerCreatesElementsWithDefaults() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFilesinkConsumer audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, AudioFilesinkConsumer.DEFAULT_ENCODER, bitrateDefault);
    checkMuxerProperties(audioFileSinkBin, AudioFilesinkConsumer.DEFAULT_MUXER);
  }

  @Test
  public void settingBitrateChangesCodecBitrate() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, "320", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFilesinkConsumer audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, AudioFilesinkConsumer.DEFAULT_ENCODER, "320");
  }

  @Test
  public void settingCodecButNotContainerResultsInCorrectCodecAndDefaultMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("vorbisenc", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFilesinkConsumer audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, "vorbisenc", "-1");
    checkMuxerProperties(audioFileSinkBin, AudioFilesinkConsumer.DEFAULT_MUXER);
  }

  @Test
  public void settingCodecToFAACButNotContainerResultsInCorrectCodecAndCorrectMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("faac", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFilesinkConsumer audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    if (audioFileSinkBin != null) {
      checkEncoderProperties(audioFileSinkBin, "faac", "128000");
      checkMuxerProperties(audioFileSinkBin, "mp4mux");
    }
  }

  @Test
  public void settingContainerResultsInCorrectMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, "mpegtsmux");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFilesinkConsumer audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    if (audioFileSinkBin != null) {
      checkEncoderProperties(audioFileSinkBin, AudioFilesinkConsumer.DEFAULT_ENCODER, bitrateDefault);
      checkMuxerProperties(audioFileSinkBin, "mpegtsmux");
    }
  }

  /** Testing permutations of possible file locations for tests **/
  @Test
  public void realFileLocationExampleSetsPropertyAndDoesntThrowException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "/tmp/testpipe/test.mp2";
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    AudioFilesinkConsumer sinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    Assert.assertEquals(location, sinkBin.filesink.get("location"));
  }

  @Test
  public void emptyFileLocationShouldThrowAnException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "";
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    @SuppressWarnings("unused")
    AudioFilesinkConsumer sinkBin = createSinkBinWantException(captureDeviceProperties);
  }

  @Test
  public void nullFileLocationShouldThrowAnException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = null;
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            location, captureDeviceProperties);
    @SuppressWarnings("unused")
    AudioFilesinkConsumer sinkBin = createSinkBinWantException(captureDeviceProperties);
  }

  private AudioFilesinkConsumer createAudioFileSinkBinDontWantException(Properties captureDeviceProperties) {
    AudioFilesinkConsumer audioFileSinkBin = null;
    try {
      audioFileSinkBin = createAudioFileSinkBin(captureDeviceProperties);
    } catch (UnableToCreateElementException e) {
      Assert.assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return audioFileSinkBin;
  }

  private AudioFilesinkConsumer createSinkBinWantException(Properties captureDeviceProperties) {
    AudioFilesinkConsumer audioFileSinkBin = null;
    try {
      audioFileSinkBin = createAudioFileSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {

    }
    return audioFileSinkBin;
  }

  private AudioFilesinkConsumer createAudioFileSinkBin(Properties captureDeviceProperties) throws Exception {
    AudioFilesinkConsumer audioFileSinkBin;
    audioFileSinkBin = new AudioFilesinkConsumer(captureDevice, captureDeviceProperties);
    return audioFileSinkBin;
  }
}
