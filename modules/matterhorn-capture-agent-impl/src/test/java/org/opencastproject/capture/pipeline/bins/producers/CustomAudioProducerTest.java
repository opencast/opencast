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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class CustomAudioProducerTest {

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

  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      // Gst.deinit();
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
  private Properties createProperties(String customProducer) {
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2", null);
    Properties captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice,
            customProducer, null, null, null, null, null);
    return captureDeviceProperties;
  }

  @Test
  public void nullSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties(null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2",
            captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioProducer customAudioSrcBin = createCustomAudioSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void garbageSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("This is not really a source");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2",
            captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioProducer customAudioSrcBin = createCustomAudioSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void singleItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("fakesrc");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2",
            captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioProducer customAudioSrcBin = createCustomAudioSrcBinDontWantException(captureDeviceProperties);
  }

  @Test
  public void multiItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createProperties("fakesrc ! queue");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0",
            ProducerType.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2",
            captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioProducer customAudioSrcBin = createCustomAudioSrcBinDontWantException(captureDeviceProperties);
  }

  private CustomAudioProducer createCustomAudioSrcBinDontWantException(Properties captureDeviceProperties) {
    CustomAudioProducer customAudioSrcBin = null;
    try {
      customAudioSrcBin = createCustomAudioSrcBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return customAudioSrcBin;
  }

  private CustomAudioProducer createCustomAudioSrcBinWantException(Properties captureDeviceProperties) {
    CustomAudioProducer customAudioSrcBin = null;
    try {
      customAudioSrcBin = createCustomAudioSrcBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {

    }
    return customAudioSrcBin;
  }

  private CustomAudioProducer createCustomAudioSrcBin(Properties captureDeviceProperties) throws Exception {
    CustomAudioProducer customAudioSrcBin;
    customAudioSrcBin = new CustomAudioProducer(captureDevice, captureDeviceProperties);
    return customAudioSrcBin;
  }
}
