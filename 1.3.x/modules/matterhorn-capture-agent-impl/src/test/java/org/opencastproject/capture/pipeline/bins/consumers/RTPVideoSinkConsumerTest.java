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

import java.util.Properties;
import org.gstreamer.Caps;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTPVideoSinkConsumerTest {
  
  private static String friendlyName = "RTPConsumerTestDevice";
  
  /** Capture Device Properties created for unit testing **/
  private CaptureDevice captureDevice = null;

  /** True to run the tests */
  private static boolean gstreamerInstalled;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RTPVideoSinkConsumerTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
      gstreamerInstalled = testRTPElementsExists();
      if (!gstreamerInstalled) {
        logger.warn("Minimum one of the Gstreamer Elements are not installed. "
                + "Please install gstreamer-plugins (base, good and ugly).");
      }
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
  
  public static boolean testRTPElementsExists() {
    return PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPH264PAY)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPBIN)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.UDPSINK) 
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.UDPSRC)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.X264ENC)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.VIDEORATE)
            && PipelineTestHelpers.testGstreamerElement(GStreamerElements.FFMPEGCOLORSPACE);
  }
  
  public static RTPVideoSinkConsumer createRTPVideoSinkConsumer(CaptureDevice captureDevice) {
    RTPVideoSinkConsumer rtpConsumer = null;
    try {
      rtpConsumer = new RTPVideoSinkConsumer(captureDevice, captureDevice.getProperties());
    } catch (Exception e) { 
      logger.error("Can not create RTPVideoSinkConsumer", e);
    }
    return rtpConsumer;
  }
  
  public static Properties createDefaultProperties() {
    Properties properties = new Properties();
    String prefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName;
    properties.put(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTP_PORT, "5000");
    properties.put(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTCP_PORT_OUT, "5001");
    properties.put(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTCP_PORT_IN, "5005");
    return properties;
  }
  
  @Before
  public void setUp() {
    if (!gstreamerInstalled) {
      return;
    }
    captureDevice = PipelineTestHelpers.createCaptureDevice(null, ProducerType.VIDEOTESTSRC, 
            friendlyName, "/tmp/testpipe/test.mp2", createDefaultProperties());
  }
  
  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }
  
  @Test
  public void createRTPVideoSinkConsumerSuccess() {
    if (!gstreamerInstalled) 
      return;
    
    Assert.assertNotNull("Can not create " + RTPVideoSinkConsumer.class.getName() + "!", 
            createRTPVideoSinkConsumer(captureDevice));
  }
  
  @Test
  public void createRTPVideoSinkConsumerFail() {
    if (!gstreamerInstalled) 
      return;
    
    CaptureDevice captureDeviceFail = PipelineTestHelpers.createCaptureDevice(
            captureDevice.getLocation(), ProducerType.VIDEOTESTSRC, 
            captureDevice.getFriendlyName(), captureDevice.getOutputPath(), new Properties());
    
    Assert.assertNull(RTPVideoSinkConsumer.class.getName() 
            + " was created, but schould be null (required properties are not set)!", 
            createRTPVideoSinkConsumer(captureDeviceFail));
  }
  
  @Test
  public void testElementsCreated() {
    if (!gstreamerInstalled)
      return;
    
    RTPVideoSinkConsumer rtpConsumer = createRTPVideoSinkConsumer(captureDevice);
    Assert.assertNotNull(rtpConsumer.queue);
    Assert.assertNotNull(rtpConsumer.decodebin);
    Assert.assertNotNull(rtpConsumer.colorspace);
    Assert.assertNotNull(rtpConsumer.videorate);
    Assert.assertNotNull(rtpConsumer.capsfilter);
    Assert.assertNotNull(rtpConsumer.encoder);
    Assert.assertNotNull(rtpConsumer.muxer);
    Assert.assertNotNull(rtpConsumer.rtpbin);
    Assert.assertNotNull(rtpConsumer.rtpsink);
    Assert.assertNotNull(rtpConsumer.rtcpsink);
    Assert.assertNotNull(rtpConsumer.rtcpsrc);
  }
  
  @Test
  public void testElementProperties() {
    if (!gstreamerInstalled)
      return;
    
    RTPVideoSinkConsumer rtpConsumer = createRTPVideoSinkConsumer(captureDevice);
    
    if (rtpConsumer == null) return; 
    
    Assert.assertEquals(rtpConsumer.rtpsink.get("ts-offset").toString(), "0");
    Assert.assertEquals(rtpConsumer.rtpsink.get("port").toString(), "5000");
    Assert.assertEquals(rtpConsumer.rtpsink.get("sync").toString(), "false");
    Assert.assertEquals(rtpConsumer.rtpsink.get("async").toString(), "false");
    
    Assert.assertEquals(rtpConsumer.rtcpsink.get("port").toString(), "5001");
    Assert.assertEquals(rtpConsumer.rtcpsink.get("sync").toString(), "false");
    Assert.assertEquals(rtpConsumer.rtcpsink.get("async").toString(), "false");
    
    Assert.assertEquals(rtpConsumer.rtcpsrc.get("port").toString(), "5005");
  }
  
  @Test
  public void testCustomEncoderPayloder() {
    if (!gstreamerInstalled)
      return;
    
    if (!PipelineTestHelpers.testGstreamerElement(GStreamerElements.FFENC_MPEG2VIDEO) 
            || !PipelineTestHelpers.testGstreamerElement("rtpmpvpay")) {
      
      logger.info("Skip testing custom encoder/payloader, because Gstreamer elements are not installed!");
      return;
    }
    
    Properties properties = captureDevice.getProperties();
    properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName 
            + CaptureParameters.CAPTURE_RTP_CONSUMER_ENCODER, GStreamerElements.FFENC_MPEG2VIDEO);
    properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName 
            + CaptureParameters.CAPTURE_RTP_CONSUMER_ENCODER + ".bitrate", "100");
    
    properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName 
            + CaptureParameters.CAPTURE_RTP_CONSUMER_PAYLOADER, "rtpmpvpay");
    properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName 
            + CaptureParameters.CAPTURE_RTP_CONSUMER_PAYLOADER + ".timestamp-offset", "60");
    
    
    CaptureDevice customCaptureDevice = PipelineTestHelpers.createCaptureDevice(
            captureDevice.getLocation(), ProducerType.VIDEOTESTSRC, 
            captureDevice.getFriendlyName(), captureDevice.getOutputPath(), properties);
    
    RTPVideoSinkConsumer rtpConsumer = createRTPVideoSinkConsumer(customCaptureDevice);
    
    if (rtpConsumer == null) return;
    
    Assert.assertNotNull(rtpConsumer);
    Assert.assertTrue(rtpConsumer.encoder.getName().startsWith(GStreamerElements.FFENC_MPEG2VIDEO));
    Assert.assertEquals(rtpConsumer.encoder.get("bitrate").toString(), "100");
    
    Assert.assertTrue(rtpConsumer.muxer.getName().startsWith("rtpmpvpay"));
    Assert.assertEquals(rtpConsumer.muxer.get("timestamp-offset").toString(), "60");
  }
  
  @Test
  public void testCustomFramerate() {
    if (!gstreamerInstalled)
      return;
    
    Properties properties = captureDevice.getProperties();
    properties.put(CaptureParameters.CAPTURE_DEVICE_PREFIX + friendlyName 
            + CaptureParameters.CAPTURE_RTP_CONSUMER_FRAMERATE, "15");
    
    CaptureDevice customCaptureDevice = PipelineTestHelpers.createCaptureDevice(
            captureDevice.getLocation(), ProducerType.VIDEOTESTSRC, 
            captureDevice.getFriendlyName(), captureDevice.getOutputPath(), properties);
    
    RTPVideoSinkConsumer rtpConsumer = createRTPVideoSinkConsumer(customCaptureDevice);
    
    if (rtpConsumer == null) return;
    
    Assert.assertNotNull(rtpConsumer);
    Caps fpsCaps = rtpConsumer.capsfilter.getStaticPad(GStreamerProperties.SINK).getCaps(); 
    Assert.assertTrue(fpsCaps.toString().contains("framerate="));
    Assert.assertTrue(fpsCaps.toString().contains("15/1"));
  }
}
