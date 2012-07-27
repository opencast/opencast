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

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Element;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class VideoFilesinkConsumerTest {

  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;

    try {
      @SuppressWarnings("unused")
      Element defaultMuxer = GStreamerElementFactory.getInstance().createElement("Setup for VideoFileSinkBinTest",
              VideoFilesinkConsumer.DEFAULT_MUXER, null);
      @SuppressWarnings("unused")
      Element defaultEncoder = GStreamerElementFactory.getInstance().createElement("Setup for VideoFileSinkBinTest",
              VideoFilesinkConsumer.DEFAULT_ENCODER, null);
    } catch (UnableToCreateElementException e) {
      logger.warn("It appears that you have GStreamer installed but not all of the packages that we require.", e);
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

  private void checkX264EncoderProperties(ConsumerBin sinkBin, Map<String, String> map) {
    for (String key : map.keySet()) {
      Assert.assertEquals("x264enc property " + key + " was not set properly." + sinkBin.encoder.get(key).toString(),
              map.get(key), sinkBin.encoder.get(key).toString());
    }
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
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFilesinkConsumer videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFilesinkConsumer.DEFAULT_ENCODER, VideoFilesinkConsumer.DEFAULT_BITRATE);
    checkMuxerProperties(videoFileSinkBin, VideoFilesinkConsumer.DEFAULT_MUXER);
  }

  @Test
  public void settingBitrateChangesCodecBitrate() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, "1024", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFilesinkConsumer videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFilesinkConsumer.DEFAULT_ENCODER, "1024");
  }
  
  @Test
  public void x264EncoderPropertiesCanBeSet() throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (!gstreamerInstalled && PipelineTestHelpers.testGstreamerElement(GStreamerElements.X264ENC)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.MP4MUX))
      return;
    String friendlyName = "Friendly Name";
    Properties captureDeviceProperties = createProperties(GStreamerElements.X264ENC,
            VideoFilesinkConsumer.DEFAULT_BITRATE, GStreamerElements.MP4MUX);
    HashMap<String, String> x264Properties = new HashMap<String, String>();
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.INTERLACED,
            "true");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "."
            + GStreamerProperties.NOISE_REDUCTION, "500");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.PASS, "5");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.QP_MIN, "20");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.QP_MAX, "23");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.QUANTIZER,
            "25");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.SPEED_PRESET,
            "4");
    x264Properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName + "." + GStreamerProperties.PROFILE, "3");
    captureDeviceProperties.putAll(x264Properties);

    HashMap<String, String> encoderProperties = new HashMap<String, String>();
    encoderProperties.put(GStreamerProperties.INTERLACED, "true");
    encoderProperties.put(GStreamerProperties.NOISE_REDUCTION, "500");
    encoderProperties.put(GStreamerProperties.PASS, "5");
    encoderProperties.put(GStreamerProperties.QP_MIN, "20");
    encoderProperties.put(GStreamerProperties.QP_MAX, "23");
    encoderProperties.put(GStreamerProperties.QUANTIZER, "25");
    encoderProperties.put(GStreamerProperties.SPEED_PRESET, "4");
    encoderProperties.put(GStreamerProperties.PROFILE, "3");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, friendlyName,
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFilesinkConsumer videoFileSinkBin = new VideoFilesinkConsumer(captureDevice, captureDeviceProperties);
    checkX264EncoderProperties(videoFileSinkBin, encoderProperties);
  }
  
  @Test
  public void settingCodecButNotContainerResultsInCorrectCodecAndDefaultMuxer() {
    if (!gstreamerInstalled && PipelineTestHelpers.testGstreamerElement(GStreamerElements.X264ENC)
            && PipelineTestHelpers.testGstreamerElement(VideoFilesinkConsumer.DEFAULT_MUXER))
      return;
    Properties captureDeviceProperties = createProperties(GStreamerElements.X264ENC, "4096", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFilesinkConsumer videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, GStreamerElements.X264ENC, "4096");
    checkMuxerProperties(videoFileSinkBin, VideoFilesinkConsumer.DEFAULT_MUXER);
  }

  @Test
  public void settingContainerResultsInCorrectMuxer() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null, null, "mpegtsmux");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    VideoFilesinkConsumer videoFileSinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(videoFileSinkBin, VideoFilesinkConsumer.DEFAULT_ENCODER, VideoFilesinkConsumer.DEFAULT_BITRATE);
    checkMuxerProperties(videoFileSinkBin, "mpegtsmux");
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
    VideoFilesinkConsumer sinkBin = createVideoFileSinkBinDontWantException(captureDeviceProperties);
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
    VideoFilesinkConsumer sinkBin = createVideoSinkBinWantException(captureDeviceProperties);
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
    VideoFilesinkConsumer sinkBin = createVideoSinkBinWantException(captureDeviceProperties);
  }

  private VideoFilesinkConsumer createVideoFileSinkBinDontWantException(Properties captureDeviceProperties) {
    VideoFilesinkConsumer videoFileSinkBin = null;
    try {
      videoFileSinkBin = createVideoFileSinkBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return videoFileSinkBin;
  }

  private VideoFilesinkConsumer createVideoSinkBinWantException(Properties captureDeviceProperties) {
    VideoFilesinkConsumer videoFileSinkBin = null;
    try {
      videoFileSinkBin = createVideoFileSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {

    }
    return videoFileSinkBin;
  }

  private VideoFilesinkConsumer createVideoFileSinkBin(Properties captureDeviceProperties) throws Exception {
    VideoFilesinkConsumer videoFileSinkBin;
    videoFileSinkBin = new VideoFilesinkConsumer(captureDevice, captureDeviceProperties);
    return videoFileSinkBin;
  }
}
