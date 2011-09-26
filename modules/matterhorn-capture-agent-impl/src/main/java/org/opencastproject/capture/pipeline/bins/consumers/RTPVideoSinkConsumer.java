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
import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.PadLinkReturn;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/**
 * RTPVideoSinkConsumer send a video stream (created by video producer) as RTP over network. 
 * You must set the output port property for rtp/rtcp and an input port for incomint RTCP messages in your 
 * configuration file for each capture devices.
 * 
 * Please note, the gstreamer RTP plugin is currently under devellopment and may be not stable.
 */
public class RTPVideoSinkConsumer extends ConsumerBin {

  protected Element decodebin;
  protected Element colorspace;
  protected Element videorate;
  protected Element capsfilter;
  protected Element rtpbin;
  protected Element rtpsink;
  protected Element rtcpsink;
  protected Element rtcpsrc;
  
  public RTPVideoSinkConsumer(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
    getBin().setName(captureDevice.getName() + "_RTP_VIDEO_SINK");
    getBin().debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, getBin().getName());
  }
  
  /**
   * Creates queue, encoder, rtp-muxer, rtpbin and the udp sinks to send a RTP stream over network.
   * 
   * @throws UnableToCreateElementException
   *           A basic GStreamer exception will be thrown before this when the GST.init is
   *           called.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    decodebin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DECODEBIN, null);
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
    videorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEORATE, null);
    capsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
    rtpbin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.RTPBIN, null);
    rtpsink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, null);
    rtcpsink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, null);
    rtcpsrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSRC, null);
    
    String prefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName();
    String encoderProp = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_ENCODER, GStreamerElements.X264ENC);
    String muxerProp = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_PAYLOADER, GStreamerElements.RTPH264PAY);
    
    encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), encoderProp, null);
    muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), muxerProp, null);
    
  }
  
  /**
   * Set the rtp/rtcp properties like port host, encoder and payloader properties.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the queue is null because this function is called before createElements this exception is thrown.
   * @throws IllegalArgumentException
   *           If RTP, RTCP-in or RTCP-out port not set.
   **/
  @Override
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    
    String prefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName();
    String host = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_HOST, "127.0.0.1");
    String rtpOut = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTP_PORT);
    String rtcpOut = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTCP_PORT_OUT);
    String rtcpIn = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_RTCP_PORT_IN);
    String framerate = properties.getProperty(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_FRAMERATE);
    
    if (rtpOut == null || rtpOut.isEmpty() || rtcpOut == null || rtcpOut.isEmpty() || rtcpIn == null || rtcpIn.isEmpty()) {
      throw new IllegalArgumentException("Some of RTP properties are not set!");
    }
    
    // rtp properties
    rtpsink.set("ts-offset", "0");
    rtpsink.set("host", host);
    rtpsink.set("port", rtpOut);
    rtpsink.set("sync", "false");
    rtpsink.set("async", "false");
    
    //rtcp properties
    rtcpsink.set("sync", "false");
    rtcpsink.set("async", "false");
    rtcpsink.set("host", host);
    rtcpsink.set("port", rtcpOut);
    
    rtcpsrc.set("uri", "udp://" + host);
    rtcpsrc.set("port", rtcpIn);

    // encoder properties (H264)
    if (encoder.getName().contains("264")) {
      String bitrate = captureDeviceProperties.getBitrate();
      try {
        Integer.parseInt(bitrate);
      } catch (NumberFormatException ex) {
        bitrate = "300";
      }
      encoder.set("bitrate", bitrate);
      encoder.set("byte-stream", "true");
    }
    
    // encoder properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_ENCODER + ".")) {
        String property = key.toString().replace(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_ENCODER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set encoder property: {} to {}", property, properties.get(key));
          encoder.set(property, properties.get(key));
        }
      }
    }
    
    // muxer properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_PAYLOADER + ".")) {
        String property = key.toString().replace(prefix + CaptureParameters.CAPTURE_RTP_CONSUMER_PAYLOADER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set payloader property: {} to {}", property, properties.get(key));
          muxer.set(property, properties.get(key));
        }
      }
    }
    
    if (framerate != null) {
      try {
        Integer framerateInt = Integer.parseInt(framerate);
        if (framerateInt < 1) {
          logger.warn("Framerate shoud be greather then 0 (current: " + framerateInt + ")!");
        } else {
          logger.info("Set framerate to " + framerateInt + "/1!");
          Caps fpsCaps = new Caps(GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.FRAMERATE + "="
                + framerateInt + "/1");
          capsfilter.setCaps(fpsCaps);
        }
      } catch (NumberFormatException e) {
        logger.error("Can not parse framerate!", e);
      }
    }
  }
  
  /** Adds all of the Elements to the Bin that are necessary to create the RTPVideoSinkConsumer. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, decodebin, colorspace, videorate, capsfilter, 
            encoder, muxer, rtpbin, rtpsink, rtcpsink, rtcpsrc);
  }
  
  /**
   * Links the gstreamer elements together.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If any of these links fail this Exception is thrown.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    // queue -> {decodebin} -> colorspace -> videorate -> capsfilter -> 
    // encoder -> payloader -> {rtpbin}
    //
    // {rtpbin} -> udpsink (rtp stream out)
    // {rtpbin} -> udpsink (rtcp stream out)
    // {rtpbin} <- udpsrc  (rtcp messages in)
    
    decodebin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element src, Pad pad) {
        if (pad.getDirection() == PadDirection.SRC) {
          pad.link(colorspace.getStaticPad(GStreamerProperties.SINK));
        }
      }
    });
    
    rtpbin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element source, Pad pad) {

        if (pad.getName().startsWith("send_rtp_src")) {
          if (pad.link(rtpsink.getStaticPad("sink")) != PadLinkReturn.OK) {
            logger.error("Can not link rtpbin to rtpsink");
          }
        }
      }
    });
    
    if (!queue.link(decodebin)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, decodebin);
    }

    if (!colorspace.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, colorspace, videorate);
    }
    
    if (!videorate.link(capsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, capsfilter);
    }
    
    if (!capsfilter.link(encoder)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, capsfilter, encoder);
    }

    if (!encoder.link(muxer)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, encoder, muxer);
    }
    
    Pad rtpbinPad = rtpbin.getRequestPad("send_rtp_sink_%d");
    PadLinkReturn linkStatus;
    if (rtpbinPad != null) {
      linkStatus = muxer.getStaticPad("src").link(rtpbinPad);
      if (linkStatus != PadLinkReturn.OK && linkStatus != PadLinkReturn.WAS_LINKED) {
        throw new UnableToLinkGStreamerElementsException(captureDevice, muxer, rtpbin);
      }
    } else {
      logger.error("Unable to get requested Pad send_rtp_sink_%d from rtpbin");
    }
    
    rtpbinPad = rtpbin.getRequestPad("send_rtcp_src_%d");
    if (rtpbinPad != null) {
      linkStatus = rtpbinPad.link(rtcpsink.getStaticPad("sink"));
      if (linkStatus != PadLinkReturn.OK && linkStatus != PadLinkReturn.WAS_LINKED) {
        throw new UnableToLinkGStreamerElementsException(captureDevice, rtpbin, rtcpsink);
      }
    } else {
      logger.error("Unable to get requested Pad send_rtcp_src_%d from rtpbin");
    }
    
    rtpbinPad = rtpbin.getRequestPad("recv_rtcp_sink_%d");
    if (rtpbinPad != null) {
      linkStatus = rtcpsrc.getStaticPad("src").link(rtpbinPad);
      if (linkStatus != PadLinkReturn.OK && linkStatus != PadLinkReturn.WAS_LINKED) {
        throw new UnableToLinkGStreamerElementsException(captureDevice, rtcpsrc, rtpbin);
      }
    } else {
      logger.error("Unable to get requested Pad recv_rtcp_sink_%d from rtpbin");
    }
  }
  
  /** Is used by createGhostPads, set to the first Element in your Bin so that we can link your SinkBin to Sources. **/
  @Override
  public Element getSrc() {
    return queue;
  }

  @Override
  public boolean isAudioDevice() {
    return false;
  }
  
  @Override
  public boolean isVideoDevice() {
    return true;
  }
}
