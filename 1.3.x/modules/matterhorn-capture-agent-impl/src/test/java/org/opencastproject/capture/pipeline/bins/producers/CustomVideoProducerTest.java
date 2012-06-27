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

import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

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

public class CustomVideoProducerTest {

  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** Properties specifically designed for unit testing */
  // private static Properties properties = null;

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
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", null);
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }

  /** Salient encoder properties are codec and bitrate **/
  /** Salient muxer properties are codec and container **/
  private Properties createProperties(String customSource) {
    Properties captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, customSource,
            null, null, null, null, null);
    return captureDeviceProperties;
  }

  @Test
  public void nullSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoProducer customVideoSrcBin = createCustomVideoSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void garbageSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("This is not really a source");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoProducer customVideoSrcBin = createCustomVideoSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void singleItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("fakesrc");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoProducer customVideoSrcBin = createCustomVideoSrcBinDontWantException(captureDeviceProperties);
  }

  @Test
  public void multiItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("fakesrc ! queue");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC,
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoProducer customVideoSrcBin = createCustomVideoSrcBinDontWantException(captureDeviceProperties);
  }

  private CustomVideoProducer createCustomVideoSrcBinDontWantException(Properties captureDeviceProperties) {
    CustomVideoProducer customVideoSrcBin = null;
    try {
      customVideoSrcBin = createCustomVideoSrcBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return customVideoSrcBin;
  }

  private CustomVideoProducer createCustomVideoSrcBinWantException(Properties captureDeviceProperties) {
    CustomVideoProducer customVideoSrcBin = null;
    try {
      customVideoSrcBin = createCustomVideoSrcBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {

    }
    return customVideoSrcBin;
  }

  private CustomVideoProducer createCustomVideoSrcBin(Properties captureDeviceProperties) throws Exception {
    CustomVideoProducer customVideoSrcBin;
    customVideoSrcBin = new CustomVideoProducer(captureDevice, captureDeviceProperties);
    return customVideoSrcBin;
  }
}
