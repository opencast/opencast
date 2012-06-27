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

public class AudioFilesinkConsumer extends ConsumerBin {

  public static final String DEFAULT_ENCODER = GStreamerElements.TWOLAME;
  public static final String DEFAULT_MUXER = GStreamerElements.CAPSFILTER;

  /**
   * AudioFilesinkConsumer transfers the audio source into a file. It is used when a Producer has the isAudioFlag set to
   * true. The main difference between this class and the VideoFilesinkConsumer is the defaults for the encoder
   * (default=twolame) and muxer(default=capsfilter).
   * 
   * @param captureDevice
   *          The settings for codec, bitrate, container, confidence monitoring etc.
   * @param properties
   *          The details of the confidence monitoring
   * @throws UnableToLinkGStreamerElementsException
   *           If one of the elements this class uses does not create its sources and sinks properly this exception is
   *           thrown when a problem is detected while trying to link the elements.
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if the source is null, is already linked or unable to create ghostpads
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the elements are not created before their properties are set they will be null and this exception will
   *           be thrown.
   * @throws CaptureDeviceNullPointerException
   *           We cannot create sources or sinks without proper information contained in the CaptureDevice parameter,
   *           therefore this cannot be null and an exception will be thrown.
   * @throws UnableToCreateElementException
   *           If GStreamer is installed but the module that this element comes from is not (please see
   *           {@code GStreamerElements} for which modules Elements are from) or the Element is platform specific to
   *           another OS (e.g. XVImageSink is available only on Linux) this Exception is thrown.
   * 
   */
  public AudioFilesinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Creates the encoder and muxer required to put audio data into a file.
   * 
   * @throws UnableToCreateElementException
   *           If the current machine doesn't have the required module to support the codec or container specified this
   *           Exception is throw. If no codec or container is specified that means your system needs to specify one
   *           that is compatible with the GStreamer Modules you have installed. If you don't wish to enable bad or ugly
   *           plugins please specify a codec in the capture agent's properties file.
   **/
  @Override
  protected synchronized void createElements() throws UnableToCreateElementException {
    super.createElements();
    createEncoder();
    createMuxer();
  }

  /**
   * Creates a user specified encoder if a codec is specified otherwise it uses the default codec in the constant
   * DEFAULT_ENCODER
   * 
   * @throws UnableToCreateElementException
   *           This Exception is thrown because you don't have the GStreamer module required for the codec (e.g. bad or
   *           ugly plugins). Please specify one that your system does support. For a list of all supported Elements
   *           please see http://gstreamer.freedesktop.org/documentation/plugins.html. To check to see which plugins are
   *           installed on your system run gst-inspect on the command line.
   **/
  protected synchronized void createEncoder() throws UnableToCreateElementException {
    if (captureDeviceProperties.getCodec() != null) {
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), captureDeviceProperties.getCodec());
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
              captureDeviceProperties.getCodec(), null);
    } else {
      logger.debug("{} setting encoder to: {}", captureDevice.getName(), DEFAULT_ENCODER);
      encoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_ENCODER,
              null);
    }
  }

  /**
   * Creates a user specified muxer if a container is specified otherwise it uses the default muxer in the constant
   * DEFAULT_MUXER
   * 
   * @throws UnableToCreateElementException
   *           This Exception is thrown because you don't have the GStreamer module required for the muxer (e.g. bad or
   *           ugly plugins). Please specify one that your system does support. For a list of all supported Elements
   *           please see http://gstreamer.freedesktop.org/documentation/plugins.html. To check to see which plugins are
   *           installed on your system run gst-inspect on the command line.
   **/
  private synchronized void createMuxer() throws UnableToCreateElementException {
    if (captureDeviceProperties.getCodec() != null) {
      if (captureDeviceProperties.getCodec().equalsIgnoreCase(GStreamerElements.FAAC))
        muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
                GStreamerElements.MP4MUX, null);
      else
        muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_MUXER,
                null);
    } else {
      muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), DEFAULT_MUXER, null);
    }

    if (captureDeviceProperties.getContainer() != null) {
      logger.debug("{} setting muxing to: {}", captureDevice.getName(), captureDeviceProperties.getContainer());
      muxer = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
              captureDeviceProperties.getContainer(), null);
    }
  }

  /** Adds all the elements required to put audio data into a file. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, encoder, muxer, filesink);
  }

  /**
   * Sets the properties on the elements specified in the capture agent's property file.
   * 
   * @throws IllegalArgumentException
   *           If one of the settings is invalid (e.g. misspelling, number instead of string, empty String etc.) this
   *           Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If one of the nessesary settings is null and doesn't have a default this Exception is thrown.
   * **/
  @Override
  protected synchronized void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setEncoderProperties();
    setFileSinkProperties();
  }

  /** Sets the bitrate property for this encoder. **/
  private synchronized void setEncoderProperties() {
    if (captureDeviceProperties.getBitrate() != null) {
      logger.debug("{} setting bitrate to: {}", captureDevice.getName(), captureDeviceProperties.getBitrate());
      encoder.set(GStreamerProperties.BITRATE, captureDeviceProperties.getBitrate());
    }
  }

  /**
   * Sets the filesink's location to put the data.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           We need a place to store the audio data coming in and no way to know where to place it by default so an
   *           exception is thrown when the location to send it is null.
   * @throws IllegalArgumentException
   *           If the filesink location is an empty String an IllegalArgumentException is thrown because we do not know
   *           where to put audio data by defauilt.
   * 
   * **/
  private synchronized void setFileSinkProperties() throws UnableToSetElementPropertyBecauseElementWasNullException,
          IllegalArgumentException {
    if (filesink == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesink, GStreamerProperties.LOCATION);
    } else if (StringUtils.isBlank(captureDevice.getOutputPath())) {
      throw new IllegalArgumentException("File location must be set, it cannot be an empty String.");
    } else {
      filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
    }
  }

  /** Links all the Elements needed to make an audio filesink. **/
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
   * Returns the first Element in the file sink chain so that the ghostpads can be created and used to link this sink to
   * the src.
   **/
  @Override
  public Element getSrc() {
    return queue;
  }
}
