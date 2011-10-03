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

import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.gstreamer.event.EOSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Png-sub-bin to use in {@link EpiphanVGA2USBV4LProducer}. 
 * Creates a bin which converts a png image to a video stream.
 * AppSink is the last Element where data can be grabbed from.
 */
public class EpiphanVGA2USBV4LSubPngBin extends EpiphanVGA2USBV4LSubAbstractBin {

  private static final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LSubPngBin.class);
  
  /** CaptureDevice */
  private CaptureDevice captureDevice;

  /** Fallback image path (png) */
  private String imagePath = null;

  /** Caps */
  private String caps = null;

  /** Bin elements */
  private Element src;

  private Element pngdec;

  private Element colorspace;

  private Element scale;

  private Element capsFilter;

  /** AppSink, the last element. */
  private AppSink sink = null;

  /**
   * Constructor. Creates a image sub-bin.
   * 
   * @param captureDevice
   *          CaptureDevice
   * @param caps
   *          Caps
   * @param imagePath
   *          path to png file
   * @throws UnableToCreateElementException
   *           If the required GStreamer Modules are not installed to create all of the Elements or image path is not
   *           exist, this Exception will be thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSubPngBin(CaptureDevice captureDevice, String caps, String imagePath)
          throws UnableToCreateElementException, UnableToLinkGStreamerElementsException {

    this.captureDevice = captureDevice;
    this.caps = caps;

    this.imagePath = imagePath;
    if (imagePath == null || !new File(imagePath).isFile())
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), GStreamerElements.MULTIFILESRC);

    createElements();
    setElementProperties();
    linkElements();

    bin.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, bin.getName(), false);
  }

  /**
   * Returns the app source.
   * 
   * @return the source
   */
  public Element getSource() {
    return src;
  }

  /**
   * Returns the caps.
   * 
   * @return the caps
   */
  public String getCaps() {
    return caps;
  }

  /**
   * Returns the caps filter.
   * 
   * @return the caps filter
   */
  public Element getCapsFilter() {
    return capsFilter;
  }

  /**
   * Returns the png dec.
   * 
   * @return the pngdec
   */
  public Element getPngdec() {
    return pngdec;
  }

  /**
   * Returns the colorspace.
   * 
   * @return the colorspace
   */
  public Element getColorspace() {
    return colorspace;
  }

  /**
   * Returns the scale.
   * 
   * @return the scale
   */
  public Element getScale() {
    return scale;
  }

  /**
   * Create elements and add these to bin.
   * 
   * @throws UnableToCreateElementException
   *           If any of the Elements fail to be created because the GStreamer module for the Element isn't present then
   *           this Exception will be thrown.
   */
  private void createElements() throws UnableToCreateElementException {
    src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MULTIFILESRC, null);
    pngdec = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.PNGDEC, null);
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
    scale = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFVIDEOSCALE, null);
    capsFilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.APPSINK, null);
    bin.addMany(src, pngdec, colorspace, scale, capsFilter, sink);
  }

  /**
   * Set bin element properties.
   */
  private void setElementProperties() {
    src.set(GStreamerProperties.LOCATION, imagePath);
    src.setCaps(Caps.fromString("image/png, framerate=(fraction)25/1"));
    capsFilter.setCaps(Caps.fromString(caps));
    sink.set(GStreamerProperties.EMIT_SIGNALS, "false");
    sink.set(GStreamerProperties.DROP, "false");
    sink.set(GStreamerProperties.ASYNC, "true");
    sink.set(GStreamerProperties.MAX_BUFFERS, "5");
    sink.setCaps(Caps.fromString(caps));
  }

  /**
   * Link elements together.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If Elements can not be linked together.
   */
  private void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(pngdec)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, pngdec);
    }
    if (!pngdec.link(colorspace)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, pngdec, colorspace);
    }
    if (!colorspace.link(scale)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, colorspace, scale);
    }
    if (!scale.link(capsFilter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, scale, capsFilter);
    }
    if (!capsFilter.link(sink)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, capsFilter, sink);
    }
  }

  /**
   * Remove all elements from bin.
   */
  protected void removeElements() {
    bin.removeMany(src, pngdec, colorspace, scale, capsFilter, sink);
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#getSink()
   */
  @Override
  public AppSink getSink() {
    return sink;
  }
  
  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#shutdown()
   */
  @Override
  public void shutdown() {
    logger.info("Sending EOS to stop " + src.getName());
    src.sendEvent(new EOSEvent());
  }
}
