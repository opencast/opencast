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
package org.opencastproject.capture.pipeline.bins.producers;

import static org.easymock.EasyMock.createMock;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Gst;
import org.gstreamer.GstException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

@Ignore
public class ProducerFactoryTest {

  private CaptureAgent captureAgentMock;

  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** Properties specifically designed for unit testing */
  private static Properties properties = null;

  private Properties captureDeviceProperties;

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

    captureAgentMock = createMock(CaptureAgent.class);
    captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, null, null, null, null,
            null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.V4L2SRC, "Friendly Name", "/tmp/testpipe/test.mp2",
            captureDeviceProperties);

    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    properties = null;
    captureDevice = null;
    // FileUtils.deleteQuietly(new File(SysteCm.getProperty("java.io.tmpdir"), "capture-agent-test"));
  }

  @Test
  public void testVideoTestSrc() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an videotestsrc card.
      captureDevice = PipelineTestHelpers.createCaptureDevice(null, ProducerType.VIDEOTESTSRC,
              "Video Test Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof VideoTestSrcProducer);
      checkCorrectnessOfVideoSource(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testExistingV4LSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an v4lsource
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.V4LSRC,
              "V4L Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof V4LProducer);
      checkCorrectnessOfVideoSource(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testMissingV4LSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an v4lsource
      captureDevice = PipelineTestHelpers.createCaptureDevice("/woot!/video0", ProducerType.V4LSRC,
              "V4L Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
      @SuppressWarnings("unused")
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testExistingHauppaugeSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an hauppage source.
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
              ProducerType.HAUPPAUGE_WINTV, "Hauppage Source", "/tmp/testpipe/test.mpeg",
              captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof HauppaugePVR350VideoProducer);
      checkCorrectnessOfVideoSource(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testMissingHauppaugeSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an hauppage source.
      captureDevice = PipelineTestHelpers.createCaptureDevice("/woot!/video0",
              ProducerType.HAUPPAUGE_WINTV, "Hauppage Source", "/tmp/testpipe/test.mpeg",
              captureDeviceProperties);
      @SuppressWarnings("unused")
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testExistingBlueCherrySource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an bluecherry card.
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
              ProducerType.BLUECHERRY_PROVIDEO, "Bluecherry Source", "/tmp/testpipe/test.mpeg",
              captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof BlueCherryBT878Producer);
      checkCorrectnessOfVideoSource(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testMissingBlueCherrySource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an bluecherry card.
      captureDevice = PipelineTestHelpers.createCaptureDevice("/woot!/video0", ProducerType.BLUECHERRY_PROVIDEO,
              "Bluecherry Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof BlueCherryBT878Producer);
      checkCorrectnessOfVideoSource(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testExistingAlsaSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an alsa source
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.ALSASRC, "Alsa Source",
              "/tmp/testpipe/test.mp2", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof AlsaProducer);
      // Check the actual correctness of the object
      checkCorrectnessOfAudioDevice(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testMissingAlsaSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      // Setup properties for an alsa source
      captureDevice = PipelineTestHelpers.createCaptureDevice("/woot!/video0", ProducerType.ALSASRC, "Alsa Source",
              "/tmp/testpipe/test.mp2", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof AlsaProducer);
      // Check the actual correctness of the object
      checkCorrectnessOfAudioDevice(srcBin);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testExistingV4L2CustomVideoSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, "v4l2src device="
              + PipelineTestHelpers.V4L2_LOCATION, null, null, null, null, null);
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video1", ProducerType.CUSTOM_VIDEO_SRC,
              "Custom Video Bin Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof CustomVideoProducer);
      // Check the actual correctness of the object
    } catch (GstException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testExistingPulseCustomAudioSource() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, "pulsesrc", null, null,
              null, null, null);
      captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.CUSTOM_AUDIO_SRC,
              "Custom Audio Bin Source", "/tmp/testpipe/test.mp2", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof CustomAudioProducer);
      // Check the actual correctness of the object
    } catch (GstException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  public void testFileSrcBin() throws Exception {

    if (!gstreamerInstalled)
      return;
    try {
      captureDevice = PipelineTestHelpers.createCaptureDevice(PipelineTestHelpers.HAUPPAGE_LOCATION,
              ProducerType.FILE_DEVICE, "File Device Source", "/tmp/testpipe/test.mp2", captureDeviceProperties);
      ProducerBin srcBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
      // Make sure we got the right object back
      Assert.assertTrue(srcBin instanceof FileProducer);
      // Check the actual correctness of the object
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    } catch (IllegalArgumentException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  private void checkCorrectnessOfVideoSource(ProducerBin srcBin) {
    // Check to make sure the sink exists and is not null.
    try {
      Assert.assertTrue(srcBin.getSrcPad() != null);
    } catch (UnableToCreateGhostPadsForBinException e) {
      Assert.fail(e.getMessage());
      e.printStackTrace();
    }
    // Check to make sure that this is a video device
    Assert.assertTrue(srcBin.isVideoDevice());
    // Check to make sure that isn't an audio device
    Assert.assertTrue(!srcBin.isAudioDevice());
  }

  private void checkCorrectnessOfAudioDevice(ProducerBin srcBin) {
    // Check to make sure the sink exists and is not null.
    try {
      Assert.assertTrue(srcBin.getSrcPad() != null);
    } catch (UnableToCreateGhostPadsForBinException e) {
      Assert.fail(e.getMessage());
      e.printStackTrace();
    }
    // Check to make sure that this is an audio device
    Assert.assertTrue(srcBin.isAudioDevice());
    // Check to make sure that isn't a video device
    Assert.assertTrue(!srcBin.isVideoDevice());
  }
}
