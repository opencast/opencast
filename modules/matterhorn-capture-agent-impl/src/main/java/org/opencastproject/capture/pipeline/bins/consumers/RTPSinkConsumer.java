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
 * RTPSinkConsumer send an audio and/or video stream as RTP/RTCP over network.
 * 
 * Please note, the gstreamer RTP plugind are currently under devellopment and may be not stable.
 */
public class RTPSinkConsumer extends ConsumerBin {
  
  protected String configPrefix;
  /**
   * Hostname to send stream(s) to (streaming server name like Wowza). 
   */
  protected String host;
  /**
   * Port number where to send the audio stream to.
   * {@link #audioRtpPort} + 1 will be set for outgoing RTCP messages.
   * <br>
   * Please note, these ports may not be used by other Application!
   */
  protected String audioRtpPort;
  /**
   * Port number where to send the video stream to.
   * {@link #videoRtpPort} + 1 will be set for outgoing RTCP messages.
   * <br>
   * Please note, these ports may not be used by other Application!
   */
  protected String videoRtpPort;
  
  protected Element decodebin;
  protected Element rtpbin;

  protected Element audioCapsfilter;
  protected Element audioEncoder;
  protected Element audioPayloader;
  protected Element audioRtpSink;
  protected Element audioRtcpSink;
  
  protected Element videoColorspacePre;
  protected Element videoDeinterlace;
  protected Element videoColorspacePost;
  protected Element videoVideorate;
  protected Element videoCapsfilter;
  protected Element videoEncoder;
  protected Element videoPayloader;
  protected Element videoRtpSink;
  protected Element videoRtcpSink;
  
  public RTPSinkConsumer(CaptureDevice captureDevice, Properties properties) 
          throws UnableToLinkGStreamerElementsException, 
          UnableToCreateGhostPadsForBinException, 
          UnableToSetElementPropertyBecauseElementWasNullException, 
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    
    super(captureDevice, properties);
    encoder = null;
    muxer = null;
    filesink = null;
    
    getBin().setName(captureDevice.getName() + "_RTP_SINK");
    getBin().debugToDotFile(Bin.DEBUG_GRAPH_SHOW_ALL, getBin().getName());
  }
  
  /**
   * Creates all the needed Gstreamer Elements.
   * <br>
   * All shared elements: queue, decodebin, rtpbin.<br>
   * All audio elements: capsfilter, encoder, payloader, (rtp)udpsink, (rtcp)udpsink.<br>
   * All video elements: 2 * colorspace, deinterlace, videorate, capsfilter, 
   * encoder, payloader, (rtp)udpsink, (rtcp)udpsink.<br>
   * 
   * @throws UnableToCreateElementException 
   *   Thrown if the Element cannot be created because the Element doesn't exist on this machine.
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    
    decodebin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DECODEBIN, null);
    rtpbin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.RTPBIN, captureDevice.getFriendlyName() + "_rtpbin");
    
    configPrefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName();
    host = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_CONSUMER_HOST, "127.0.0.1");
    audioRtpPort = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_RTP_PORT);
    videoRtpPort = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_RTP_PORT);
    
    if (audioRtpPort != null) {
      createAudioElements();
    }
    if (videoRtpPort != null) {
      createVideoElements();
    }
  }

  private void createAudioElements() throws UnableToCreateElementException {
    audioCapsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
    audioRtpSink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, captureDevice.getFriendlyName() + "_audio_rtpsink");
    audioRtcpSink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, captureDevice.getFriendlyName() + "_audio_rtcpsink");
    
    String encoderProp = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_ENCODER, GStreamerElements.FAAC);
    String payloaderProp = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_PAYLOADER, GStreamerElements.RTPMP4GPAY);
    
    audioEncoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), encoderProp, null);
    audioPayloader = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), payloaderProp, null);
  }

  private void createVideoElements() throws UnableToCreateElementException {
    videoColorspacePre = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
    videoDeinterlace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFDEINTERLACE, null);
    videoColorspacePost = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
    videoVideorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEORATE, null);
    videoCapsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
    videoRtpSink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, captureDevice.getFriendlyName() + "_video_rtpsink");
    videoRtcpSink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.UDPSINK, captureDevice.getFriendlyName() + "_video_rtcpsink");
    
    String encoderProp = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_ENCODER, GStreamerElements.X264ENC);
    String payloaderProp = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_PAYLOADER, GStreamerElements.RTPH264PAY);
    
    videoEncoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), encoderProp, null);
    videoPayloader = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), payloaderProp, null);
  }
  
  /**
   * Sets the properties on the elements specified in the capture agent's property file.
   * <br>
   * To create an audio/video RTP stream the 
   * {@see CaptureParameters#CAPTURE_RTP_AUDIO_CONSUMER_RTP_PORT}/{@see CaptureParameters#CAPTURE_RTP_VIDEO_CONSUMER_RTP_PORT}
   * property should set for each device!
   * <br>
   * To set a different encoder (default: {@value GStreamerElements#FAAC} for audio 
   * and {@value GStreamerElements#X264ENC} for video) use the 
   * {@see CaptureParameters#CAPTURE_RTP_AUDIO_CONSUMER_ENCODER}/{@see CaptureParameters#CAPTURE_RTP_VIDEO_CONSUMER_ENCODER}
   * property.
   * <br>
   * To set a different payloader (default: {@value GStreamerElements#RTPMP4APAY} for audio 
   * and {@value GStreamerElements#RTPH264PAY} for video) use the 
   * {@see CaptureParameters#CAPTURE_RTP_AUDIO_CONSUMER_PAYLOADER}/{@see CaptureParameters#CAPTURE_RTP_VIDEO_CONSUMER_PAYLOADER}
   * property.
   * 
   * @throws IllegalArgumentException
   *           If one of the settings is invalid (e.g. misspelling, number instead of string, empty String etc.) this
   *           Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If one of the nessesary settings is null and doesn't have a default this Exception is thrown.
   */
  @Override
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    
    if (audioRtpPort == null && videoRtpPort == null) {
      throw new IllegalArgumentException("Audio and/or video RTP port should set to use the RTPSinkConsumer!"
              + " Check your device configuration file for this settings on capturedevice '"
              + captureDevice.getFriendlyName() + "'.");
    }
    
    if (audioRtpPort != null) {
      setAudioElementProperties();
    }
    if (videoRtpPort != null) {
      setVideoElementProperties();
    }
  }

  private void setAudioElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    
    // rtp properties
    audioRtpSink.set("sync", "false");
    audioRtpSink.set("async", "false");
    audioRtpSink.set("port", audioRtpPort);
    
    // rtcp properties
    audioRtcpSink.set("sync", "false");
    audioRtcpSink.set("async", "false");
    audioRtcpSink.set("port", Integer.parseInt(audioRtpPort) + 1);

    if (host != null) {
      audioRtpSink.set("host", host);
      audioRtcpSink.set("host", host);
    }
    
    audioCapsfilter.setCaps(Caps.fromString(GStreamerProperties.AUDIO_X_RAW_INT));
    
    // encoder properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_ENCODER + ".")) {
        String property = key.toString().replace(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_ENCODER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set audio encoder property: {} to {}", property, properties.get(key));
          audioEncoder.set(property, properties.get(key));
        }
      }
    }
    
    // payloader properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_PAYLOADER + ".")) {
        String property = key.toString().replace(configPrefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_PAYLOADER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set audio payloader property: {} to {}", property, properties.get(key));
          audioPayloader.set(property, properties.get(key));
        }
      }
    }
  }

  private void setVideoElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    
    // rtp properties
    videoRtpSink.set("sync", "false");
    videoRtpSink.set("async", "false");
    videoRtpSink.set("port", videoRtpPort);
    
    // rtcp properties
    videoRtcpSink.set("sync", "false");
    videoRtcpSink.set("async", "false");
    videoRtcpSink.set("port", Integer.parseInt(videoRtpPort) + 1);

    if (host != null) {
      videoRtpSink.set("host", host);
      videoRtcpSink.set("host", host);
    }
    
    videoCapsfilter.setCaps(Caps.fromString(GStreamerProperties.VIDEO_X_RAW_YUV));
    
    // set framerate if configured
    String framerate = properties.getProperty(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_FRAMERATE);
    if (framerate != null) {
      try {
        Integer framerateInt = Integer.parseInt(framerate);
        if (framerateInt < 1) {
          logger.warn("Framerate shoud be greather then 0 (current: " + framerateInt + "/1)!");
        } else {
          logger.info("Set framerate to " + framerateInt + "/1!");
          Caps fpsCaps = new Caps(GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.FRAMERATE + "="
                + framerateInt + "/1");
          videoCapsfilter.setCaps(fpsCaps);
        }
      } catch (NumberFormatException e) {
        logger.error("Can not parse framerate!", e);
      }
    }
    
    // encoder properties (H264)
    if (videoEncoder.getName().contains("264")) {
      String bitrate = captureDeviceProperties.getBitrate();
      try {
        Integer.parseInt(bitrate);
      } catch (NumberFormatException ex) {
        bitrate = "300";
      }
      videoEncoder.set("bitrate", bitrate);
      videoEncoder.set("byte-stream", "true");
      videoEncoder.set("rc-lookahead", "5");
      videoEncoder.set("profile", "1");
    }
    
    // encoder properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_ENCODER + ".")) {
        String property = key.toString().replace(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_ENCODER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set video encoder property: {} to {}", property, properties.get(key));
          videoEncoder.set(property, properties.get(key));
        }
      }
    }
    
    // payloader properties
    for (Object key : properties.keySet()) {
      if (key.toString().startsWith(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_PAYLOADER + ".")) {
        String property = key.toString().replace(configPrefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_PAYLOADER + ".", "");
        if (!property.isEmpty()) {
          logger.info("Set video payloader property: {} to {}", property, properties.get(key));
          videoPayloader.set(property, properties.get(key));
        }
      }
    }
  }
  
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, decodebin, rtpbin);
    
    if (audioRtpPort != null) {
      addAudioElementsToBin();
    }
    if (videoRtpPort != null) {
      addVideoElementsToBin();
    }
  }

  private void addAudioElementsToBin() {
    bin.addMany(audioCapsfilter, audioEncoder, audioPayloader, audioRtpSink, audioRtcpSink);
  }

  private void addVideoElementsToBin() {
    bin.addMany(videoVideorate, videoCapsfilter, videoColorspacePre, videoDeinterlace, 
            videoColorspacePost, videoEncoder, videoPayloader, videoRtpSink, videoRtcpSink);
  }
  
  /**
   * Link all the Elements. 
   * 
   * @throws UnableToLinkGStreamerElementsException 
   *  If any of these links fail this Exception is thrown.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    // RTP Pipeline:
    //
    // queue -> decodebin 
    //   if audio { -> (audio)capsfilter -> encoder -> (audio) payloader -> }
    //   if video { -> colorspace -> deinterlace -> colorspace -> 
    //              -> videorate -> capsfilter -> encoder -> payloader -> }
    // rtpbin 
    //   if audio { -> (audio rtp) udpsink 
    //              -> (audio rtpc) udpsink }
    //   if video { -> (video rtp) udpsink 
    //              -> (video rtpc) udpsink }
    
    if (!queue.link(decodebin)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, decodebin);
    }
    
    if (audioRtpPort != null) {
      linkAudioElements();
    }
    if (videoRtpPort != null) {
      linkVideoElements();
    }
  }

  private void linkAudioElements() throws UnableToLinkGStreamerElementsException {
    
    rtpbin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element source, Pad pad) {

        if (pad.getDirection() == PadDirection.SRC
                && pad.getName().startsWith("send_rtp_src_")
                && !audioRtpSink.getStaticPad(GStreamerProperties.SINK).isLinked()) {
          
          pad.link(audioRtpSink.getStaticPad(GStreamerProperties.SINK));
        }
      }
    });
    
    decodebin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element src, Pad pad) {
        Pad audioCapsfilterPad = audioCapsfilter.getStaticPad(GStreamerProperties.SINK);
        if (pad.getDirection() == PadDirection.SRC 
              && pad.acceptCaps(audioCapsfilterPad.getCaps())) {

          if (pad.link(audioCapsfilterPad) != PadLinkReturn.OK) {
            logger.error("Can not link {} to {}!", new Object[] {
              pad.getParent().getName(), audioCapsfilter.getName()
            });
          }
        }
      }
    });
    
    if (!audioCapsfilter.link(audioEncoder)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, audioCapsfilter, audioEncoder);
    }
    
    if (!audioEncoder.link(audioPayloader)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, audioEncoder, audioPayloader);
    }
    
    if (!Element.linkPads(audioPayloader, GStreamerProperties.SRC, rtpbin, "send_rtp_sink_%d")) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, audioPayloader, rtpbin);
    }
    
    if (!Element.linkPads(rtpbin, "send_rtcp_src_%d", audioRtcpSink, GStreamerProperties.SINK)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, rtpbin, audioRtcpSink);
    }
  }

  private void linkVideoElements() throws UnableToLinkGStreamerElementsException {
    
    rtpbin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element source, Pad pad) {

        if (pad.getDirection() == PadDirection.SRC
                && pad.getName().startsWith("send_rtp_src_")
                && !videoRtpSink.getStaticPad(GStreamerProperties.SINK).isLinked()) {

          pad.link(videoRtpSink.getStaticPad(GStreamerProperties.SINK));
        }
      }
    });
    
    decodebin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element src, Pad pad) {
        
        // use caps from capsfilter element because it was set before
        if (pad.getDirection() == PadDirection.SRC 
              && pad.acceptCaps(videoCapsfilter.getStaticPad(GStreamerProperties.SRC).getCaps())) {
          
          Pad videoColorspacePrePad = videoColorspacePre.getStaticPad(GStreamerProperties.SINK);
          if (pad.link(videoColorspacePrePad) != PadLinkReturn.OK) {
            logger.error("Can not link {} to {}!", new Object[] {
              pad.getParent().getName(), videoColorspacePre.getName()
            });
          }
        }
      }
    });
    
    if (!videoColorspacePre.link(videoDeinterlace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoColorspacePre, videoDeinterlace);
    }
    
    if (!videoDeinterlace.link(videoColorspacePost)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoDeinterlace, videoColorspacePost);
    }
    
    if (!videoColorspacePost.link(videoVideorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoColorspacePost, videoVideorate);
    }
    
    if (!videoVideorate.link(videoCapsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoVideorate, videoCapsfilter);
    }
    
    if (!videoCapsfilter.link(videoEncoder)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoCapsfilter, videoEncoder);
    }
    
    if (!videoEncoder.link(videoPayloader)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoEncoder, videoPayloader);
    }
    
    if (!Element.linkPads(videoPayloader, GStreamerProperties.SRC, rtpbin, "send_rtp_sink_%d")) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoPayloader, rtpbin);
    }
    
    if (!Element.linkPads(rtpbin, "send_rtcp_src_%d", videoRtcpSink, GStreamerProperties.SINK)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, rtpbin, videoRtcpSink);
    }
  }

  @Override
  public Element getSrc() {
    return queue;
  }
  
  @Override
  public boolean isAudioDevice() {
    return audioRtpPort != null;
  }
  
  @Override
  public boolean isVideoDevice() {
    return videoRtpPort != null;
  }
}
