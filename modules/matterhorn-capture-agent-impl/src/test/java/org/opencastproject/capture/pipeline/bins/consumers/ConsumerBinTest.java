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
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Element;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
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
import java.util.List;
import java.util.Properties;

public class ConsumerBinTest {

  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  private String maxSizeBuffersDefault;

  private String maxSizeBytesDefault;

  private String maxSizeTimeDefault;

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
      Element testQueue = GStreamerElementFactory.getInstance().createElement("SinkBinTest", GStreamerElements.QUEUE,
              null);
      maxSizeBuffersDefault = testQueue.getPropertyDefaultValue("max-size-buffers").toString();
      maxSizeBytesDefault = testQueue.getPropertyDefaultValue("max-size-bytes").toString();
      maxSizeTimeDefault = testQueue.get("max-size-time").toString();
    } catch (UnableToCreateElementException e) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }

  /** Salient queue properties to test are bufferCount, bufferBytes and bufferTime. **/
  private Properties createQueueProperties(String bufferCount, String bufferBytes, String bufferTime) {
    Properties captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, null, null,
            null, null, null, null);
    captureDeviceProperties.putAll(PipelineTestHelpers.createQueueProperties(bufferCount, bufferBytes, bufferTime));
    return captureDeviceProperties;
  }

  @Test
  public void testSetQueuePropertiesAllNull() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, maxSizeTimeDefault);
  }

  /** Test setting the max buffer size of the queue to different values **/
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToBelowMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties("-1", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "-1", maxSizeBytesDefault, maxSizeTimeDefault);
  }

  @Test
  public void testSetQueuePropertieMaxSizeBuffersToMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties("0", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "0", maxSizeBytesDefault, maxSizeTimeDefault);
  }

  @Test
  public void testSetQueuePropertieMaxSizeBuffersToNormalValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties("250", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "250", maxSizeBytesDefault, maxSizeTimeDefault);
  }

  @Test
  public void testSetQueuePropertieMaxSizeBuffersToMaximumValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties("" + Integer.MAX_VALUE, null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "" + Integer.MAX_VALUE, maxSizeBytesDefault, maxSizeTimeDefault);
  }

  @Test
  public void testSetQueuePropertieMaxSizeBuffersToGarbageValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties("Invalid String", null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    ConsumerBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }

  /** Test setting the max bytes size of the queue to different values **/
  @Test
  public void testSetQueuePropertieMaxSizeBytesToBelowMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "-1", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "-1", maxSizeTimeDefault);

  }

  @Test
  public void testSetQueuePropertieMaxSizeBytesToMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "0", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "0", maxSizeTimeDefault);
  }

  @Test
  public void testSetQueuePropertieMaxSizeBytesToNormalValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "12485760", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "12485760", maxSizeTimeDefault);

  }

  @Test
  public void testSetQueuePropertieMaxSizeBytesToMaximumValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "" + Integer.MAX_VALUE, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "" + Integer.MAX_VALUE, maxSizeTimeDefault);

  }

  @Test
  public void testSetQueuePropertieMaxSizeBytesToGarbageValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "Invalid String", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    ConsumerBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }

  /** Test setting the max time size of the queue to different values **/
  @Test
  public void testSetQueuePropertieMaxSizeTimeToBelowMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "-1");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "-1");

  }

  @Test
  public void testSetQueuePropertieMaxSizeTimeToMinimum() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "0");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "0");
  }

  @Test
  public void testSetQueuePropertieMaxSizeTimeToNormalValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "1000000");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "1000000");

  }

  @Test
  public void testSetQueuePropertieMaxSizeTimeToMaximumValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "" + Integer.MAX_VALUE);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "" + Integer.MAX_VALUE);

  }

  @Test
  public void testSetQueuePropertieMaxSizeTimeToGarbageValue() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, "Invalid String", null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    ConsumerBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }

  @Test
  public void testGhostPadIsCreated() {
    if (!gstreamerInstalled)
      return;
    Properties captureDeviceProperties = createQueueProperties(null, null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/video0", ProducerType.VIDEOTESTSRC, "Friendly Name",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    ConsumerBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    List<Pad> binPads = sinkBin.getBin().getPads();
    Assert.assertEquals(1, binPads.size());
    Assert.assertEquals(ConsumerBin.GHOST_PAD_NAME, binPads.get(0).getName());
  }

  private void checkQueueProperties(ConsumerBin sinkBin, String expectedMaxSizeBuffer, String expectedMaxSizeBytes,
          String expectedMaxSizeTime) {
    Assert.assertEquals(expectedMaxSizeBuffer, sinkBin.queue.get("max-size-buffers").toString());
    Assert.assertEquals(expectedMaxSizeBytes, sinkBin.queue.get("max-size-bytes").toString());
    Assert.assertEquals(expectedMaxSizeTime, sinkBin.queue.get("max-size-time").toString());
  }

  private ConsumerBin createSinkBinDontWantException(Properties captureDeviceProperties) {
    ConsumerBin sinkBin = null;
    try {
      sinkBin = createSinkBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return sinkBin;
  }

  private ConsumerBin createSinkBinWantException(Properties captureDeviceProperties) {
    ConsumerBin sinkBin = null;
    try {
      sinkBin = createSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {

    }
    return sinkBin;
  }

  private ConsumerBin createSinkBin(Properties captureDeviceProperties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    ConsumerBin sinkBin;
    sinkBin = new ConsumerBin(captureDevice, captureDeviceProperties) {

      @Override
      protected void linkElements() throws UnableToLinkGStreamerElementsException {

      }

      @Override
      protected void addElementsToBin() {

      }

      @Override
      public Element getSrc() {
        return queue;
      }
    };
    return sinkBin;
  }
}
