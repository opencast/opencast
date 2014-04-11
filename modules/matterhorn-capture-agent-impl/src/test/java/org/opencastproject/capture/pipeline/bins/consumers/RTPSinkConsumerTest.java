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

/**
 * Test RTPSinkConsumer functionality if the Gstreamer-plugins instlled.
 */
public class RTPSinkConsumerTest {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RTPSinkConsumerTest.class);

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  protected String configurationPrefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + "RTPConsumerTestDevice";
  protected CaptureDevice captureDevice = null;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
      gstreamerInstalled = PipelineTestHelpers.testGstreamerElement(GStreamerElements.QUEUE)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.DECODEBIN)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.FFMPEGCOLORSPACE)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.VIDEORATE);

      if (!gstreamerInstalled) {
        logger.info("Skip tests because gstreamer plugins-base are not installed!");
        return;
      }

      gstreamerInstalled = gstreamerInstalled
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPBIN)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPH264PAY)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPMP4GPAY)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.UDPSINK)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPMPAPAY)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.RTPMPVPAY);

      if (!gstreamerInstalled) {
        logger.info("Skip tests because gstreamer plugins-good are not installed!");
        return;
      }

      gstreamerInstalled = gstreamerInstalled
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.FAAC)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.MPEG2ENC);

      if (!gstreamerInstalled) {
        logger.info("Skip tests because gstreamer plugins-bad are not installed!");
        return;
      }

      gstreamerInstalled = gstreamerInstalled
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.FFDEINTERLACE);

      if (!gstreamerInstalled) {
        logger.info("Skip tests because gstreamer plugins-ffmpeg are not installed!");
        return;
      }

      gstreamerInstalled = gstreamerInstalled
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.X264ENC)
              && PipelineTestHelpers.testGstreamerElement(GStreamerElements.TWOLAME);

      if (!gstreamerInstalled) {
        logger.info("Skip tests because gstreamer plugins-ugly are not installed!");
        return;
      }

    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() {
    if (!gstreamerInstalled)
      return;

    Properties properties = new Properties();
    properties.put(configurationPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_RTP_PORT, "5000");
    properties.put(configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_RTP_PORT, "5002");
    captureDevice = PipelineTestHelpers.createCaptureDevice("/dev/null", ProducerType.FILE_DEVICE,
            "RTPConsumerTestDevice", "/tmp/testpipe/test.mp2", properties);
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    captureDevice = null;
  }

  @Test
  public void testCreateRTPConsumerFail() {
    if (!gstreamerInstalled)
      return;

    ConsumerBin consumerBin;
    try {
      captureDevice.getProperties().remove(configurationPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_RTP_PORT);
      captureDevice.getProperties().remove(configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_RTP_PORT);

      // RTPSinkConsumer should not create if audio AND video port is not set!
      consumerBin = ConsumerFactory.getInstance().getSink(
              ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());
      Assert.fail("Test should fail because the audio and video port not set.");
    } catch (Exception ex) { }
  }

  @Test
  public void testHostPortProperties() {
    if (!gstreamerInstalled)
      return;

    try {
      captureDevice.getProperties().put(configurationPrefix + CaptureParameters.CAPTURE_RTP_CONSUMER_HOST, "127.0.0.1");
      RTPSinkConsumer consumerBin = (RTPSinkConsumer) ConsumerFactory.getInstance().getSink(
              ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());

      Assert.assertNotNull(consumerBin);
      Assert.assertEquals(consumerBin.host, "127.0.0.1");
      Assert.assertEquals(consumerBin.audioRtpSink.get("host"), "127.0.0.1");
      Assert.assertEquals(consumerBin.audioRtcpSink.get("host"), "127.0.0.1");
      Assert.assertEquals(consumerBin.videoRtpSink.get("host"), "127.0.0.1");
      Assert.assertEquals(consumerBin.videoRtcpSink.get("host"), "127.0.0.1");

      Assert.assertEquals(consumerBin.audioRtpPort, "5000");
      Assert.assertEquals(consumerBin.audioRtpSink.get("port"), 5000);
      Assert.assertEquals(consumerBin.audioRtcpSink.get("port"), 5001);

      Assert.assertEquals(consumerBin.videoRtpPort, "5002");
      Assert.assertEquals(consumerBin.videoRtpSink.get("port"), 5002);
      Assert.assertEquals(consumerBin.videoRtcpSink.get("port"), 5003);

    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testAudioEncoderProperties() {
    if (!gstreamerInstalled)
      return;

    RTPSinkConsumer consumerBin = null;
    try {
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_ENCODER + ".bitrate", "48000");

      consumerBin = (RTPSinkConsumer) ConsumerFactory.getInstance().getSink(
                    ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());

      Assert.assertNotNull("Audio encoder should not be null!", consumerBin.audioEncoder);
      Assert.assertEquals("Audio encoder property is not set (bitrate should be 48000)!",
              consumerBin.audioEncoder.get("bitrate"), 48000);
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testCustomAudioEncoder() {
    if (!gstreamerInstalled)
      return;

    RTPSinkConsumer consumerBin = null;
    try {
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_ENCODER, GStreamerElements.TWOLAME);
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_PAYLOADER, GStreamerElements.RTPMPAPAY);

      consumerBin = (RTPSinkConsumer) ConsumerFactory.getInstance().getSink(
                    ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());

      Assert.assertNotNull("Audio encoder should not be null!", consumerBin.audioEncoder);
      Assert.assertTrue("Custom audio encoder is not set!", consumerBin.audioEncoder.getName().startsWith(GStreamerElements.TWOLAME));
      Assert.assertTrue("Custom audio payloader is not set!", consumerBin.audioPayloader.getName().startsWith(GStreamerElements.RTPMPAPAY));
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testVideoEncoderProperties() {
    if (!gstreamerInstalled)
      return;

    RTPSinkConsumer consumerBin = null;
    try {
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_ENCODER + ".bitrate", "600");

      consumerBin = (RTPSinkConsumer)ConsumerFactory.getInstance().getSink(
                    ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());
      Assert.assertNotNull("Video encoder should not be null!", consumerBin.videoEncoder);
      Assert.assertEquals("Video encoder property is not set (bitrate should be 600)!",
              consumerBin.videoEncoder.get("bitrate"), 600);
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testCustomVideoEncoder() {
    if (!gstreamerInstalled)
      return;

    RTPSinkConsumer consumerBin = null;
    try {
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_ENCODER, GStreamerElements.MPEG2ENC);
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_PAYLOADER, GStreamerElements.RTPMPVPAY);

      consumerBin = (RTPSinkConsumer)ConsumerFactory.getInstance().getSink(
                    ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());
      Assert.assertNotNull("Video encoder should not be null!", consumerBin.videoEncoder);
      Assert.assertTrue("Custom video encoder is not set!", consumerBin.videoEncoder.getName().startsWith(GStreamerElements.MPEG2ENC));
      Assert.assertTrue("Custom video payloader is not set!", consumerBin.videoPayloader.getName().startsWith(GStreamerElements.RTPMPVPAY));
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }

  @Test
  public void testVideoFramerate() {
    if (!gstreamerInstalled)
      return;

    RTPSinkConsumer consumerBin = null;
    try {
      captureDevice.getProperties().put(
              configurationPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_FRAMERATE, "15");

      consumerBin = (RTPSinkConsumer)ConsumerFactory.getInstance().getSink(
                    ConsumerType.RTP_SINK, captureDevice, captureDevice.getProperties());
      Assert.assertNotNull("Video encoder should not be null!", consumerBin.videoEncoder);
      Caps fpsCaps = consumerBin.videoCapsfilter.getStaticPad(GStreamerProperties.SINK).getCaps();
      Assert.assertTrue(fpsCaps.toString().contains("framerate="));
      Assert.assertTrue(fpsCaps.toString().contains("15/1"));
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }
}
