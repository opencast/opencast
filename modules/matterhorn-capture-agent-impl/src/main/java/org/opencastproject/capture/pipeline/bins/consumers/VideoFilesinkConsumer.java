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
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.apache.commons.lang.StringUtils;
import org.gstreamer.Element;

import java.util.Properties;

public class VideoFilesinkConsumer extends ConsumerBin {

  public static final String DEFAULT_MUXER = GStreamerElements.MP4MUX;
  public static final String DEFAULT_ENCODER = GStreamerElements.X264ENC;
  public static final String DEFAULT_BITRATE = "2048";
  public static final String DEFAULT_ENCODER_SPEED_PRESET = "1";
  
  /**
   * Pass 0 is CBR (default), Pass 4 is constant quantizer, Pass 5 is constant quality.
   **/
  public static final String DEFAULT_X264_PASS = "5";

  /**
   * VideoFilesinkConsumer dumps the video source into a file. It is used when a Producer has the isVideoFlag set to
   * true. The main difference between this class and the AudioFilesinkConsumer is the defaults for the encoder
   * (default=ffenc_mpeg2video) and muxer(default=mpegpsmux).
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If the Elements for this Consumer cannot be linked together this Exception will be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the getSrc function returns a different element than the Element who has a sink for this Consumer or
   *           there are no sink pads for that Element then this Exception will be thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If setElementProperties is called before createElements this exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           captureDevice parameter is necessary for creating the VideoFilesinkConsumerBin so this Exception is
   *           thrown when that parameter is null.
   * @throws UnableToCreateElementException
   *           If the current machine does not have the necessary GStreamer module installed for elements required by
   *           VideoFilesinkConsumerBin this Exception is thrown. A workaround is to choose a codec and container that
   *           is compatible with the current setup.
   */
  public VideoFilesinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * This creates the Encoder and Muxer that when linked with the queue at the start and the filesink at the end creates
   * a full Consumer solution for outputting video data to a file.
   * 
   * @throws UnableToCreateElementException
   *           If the current install of GStreamer on this machine doesn't support the container or codec that is set
   *           for the encoder or muxer this exception is thrown.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    createEncoder();
    createMuxer();
  }

  /**
   * Creates the encoder to change the raw video data outputted from GStreamer into the default codec if none is
   * specified, or the specified codec in the capture properties file.
   * 
   * @throws UnableToCreateElementException
   *           If the current GStreamer install doesn't support the default codec if no codec is specified or if the
   *           specified codec is not supported by one of the GStreamer modules installed then this Exception is thrown,
   */
  protected void createEncoder() throws UnableToCreateElementException {
    if (captureDeviceProperties.getCodec() != null) {
      /** The user has specified a codec so we will try to create it. **/
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), captureDeviceProperties.getCodec());
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
              captureDeviceProperties.getCodec(), null);
    } else {
      /** No codec is specified by the user so we will create the default encoder. **/
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), DEFAULT_ENCODER);
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_ENCODER,
              null);
    }
  }

  /**
   * Creates the muxer to insert the encoder information into the default container or a user specified container.
   * 
   * @throws UnableToCreateElementException
   *           If the default container or the user specified container is not supported by the GStreamer modules
   *           installed this Exception is thrown.
   **/
  private void createMuxer() throws UnableToCreateElementException {
    if (captureDeviceProperties.getContainer() != null) {
      /** The user has specified a different container than H.264 **/
      logger.debug("{} setting muxing to: {}", captureDevice.getName(), captureDeviceProperties.getContainer());
      muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
              captureDeviceProperties.getContainer(), null);
    } else {
      /** No container is specifed by the user so we are using the DEFAULT_MUXER **/
      logger.debug("{} setting muxing to: {}", captureDevice.getName(), DEFAULT_MUXER);
      muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_MUXER, null);
    }
  }

  /** Adds all of the Elements to the Bin that are necessary to create the VideoFileSinkConsumerBin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, encoder, muxer, filesink);
  }

  /**
   * Sets the user defined properties for the filesink and encoder.
   * 
   * @throws IllegalArgumentException
   *           Thrown if necessary arguments such as file location are not set.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If setElementProperties is called before createElements, the Elements will be null and this Exception is
   *           thrown.
   * **/
  @Override
  protected synchronized void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setFileSinkProperties();
    setEncoderProperties();
  }

  /**
   * Defines the location of the filesink that will be the file that is created by the capture.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the filesink is null at this point this Exception is thrown.
   * @throws IllegalArgumentException
   *           If the file location is set to an empty string then this exception will be thrown, we need a location to
   *           dump the file.
   * **/
  private synchronized void setFileSinkProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    if (filesink == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesink, GStreamerProperties.LOCATION);
    } else if (StringUtils.isEmpty(captureDevice.getOutputPath())) {
      throw new IllegalArgumentException("File location must be set, it cannot be an empty String.");
    } else {
      filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
    }
  }

  /**
   * Sets the bitrate for the encoder if the user specified it. If not it is set to DEFAULT_BITRATE.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If encoder is null at this point this Exception is thrown.
   **/
  private void setEncoderProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (encoder == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(encoder, captureDeviceProperties.getBitrate());
    }

    if (captureDeviceProperties.getBitrate() != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), captureDeviceProperties.getBitrate());
      encoder.set(GStreamerProperties.BITRATE, captureDeviceProperties.getBitrate());
    } else {
      encoder.set(GStreamerProperties.BITRATE, DEFAULT_BITRATE);
    }
   
    if (captureDeviceProperties.getCodec() != null
            && captureDeviceProperties.getCodec().equalsIgnoreCase(GStreamerElements.X264ENC) || StringUtils.trimToNull(captureDeviceProperties.getCodec()) == null) {
      setX264EncoderProperties();
    }

  }

  /** Check and set all of the unique properties for the x264enc gstreamer element. **/
  private void setX264EncoderProperties() {
    setEncoderProperty(GStreamerProperties.INTERLACED);
    setEncoderProperty(GStreamerProperties.NOISE_REDUCTION);
    setEncoderProperty(GStreamerProperties.PASS, DEFAULT_X264_PASS);
    setEncoderProperty(GStreamerProperties.PROFILE);
    setEncoderProperty(GStreamerProperties.QP_MIN);
    setEncoderProperty(GStreamerProperties.QP_MAX);
    setEncoderProperty(GStreamerProperties.QUANTIZER);
    setEncoderProperty(GStreamerProperties.SPEED_PRESET, DEFAULT_ENCODER_SPEED_PRESET);
  }

  /**
   * Sets a property on the encoder using the capture agent device properties.
   * 
   * @param key
   *          The name of the property to set.
   **/
  private void setEncoderProperty(String key) {
	  setEncoderProperty(key, null);
  }
  
  /**
   * Sets a property on the encoder using the capture agent device properties with a possible default value. 
   * 
   * @param key
   *          The name of the property to set.
   **/
  private void setEncoderProperty(String key, String defaultValue) {
    String fullPropertiesKey = CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + "." + key;
    if (properties.containsKey(fullPropertiesKey)) {
      encoder.set(key, properties.get(fullPropertiesKey));
    } else if (StringUtils.trimToNull(defaultValue) != null) {
    	encoder.set(key, defaultValue);
    }
  }
  
  /**
   * Links the queue to the encoder to the muxer to the filesink.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If any of these links fail this Exception is thrown.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!queue.link(encoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, encoder);
    else if (!encoder.link(muxer))
      throw new UnableToLinkGStreamerElementsException(captureDevice, encoder, muxer);
    else if (!muxer.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, muxer, filesink);
  }

  /**
   * Returns the queue as the Sink for this Consumer. Will be used to create the ghostpads to link this Consumer to the
   * Producer.
   **/
  @Override
  public Element getSrc() {
    return queue;
  }
}
