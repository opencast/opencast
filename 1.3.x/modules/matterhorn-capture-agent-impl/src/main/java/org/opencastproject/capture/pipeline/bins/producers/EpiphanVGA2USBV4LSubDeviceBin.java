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

import org.gstreamer.Bus.EOS;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.AppSink;
import org.gstreamer.event.EOSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epiphan VGA2USB device sub bin to use in {@link EpiphanVGA2USBV4LProducer}. Creates a bin with v4lsrc Element to grab
 * signal from Epiphan device, videoscale Element to rescale video frame if needed (by replugging the cable with another
 * resolution as captured file) and AppSink Element to connect with {@link EpiphanVGA2USBV4LProducer}.
 */
public class EpiphanVGA2USBV4LSubDeviceBin extends EpiphanVGA2USBV4LSubAbstractBin {

  private static final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LSubDeviceBin.class);
  
  /** CaptureDevice */
  private CaptureDevice captureDevice;

  /** Caps */
  private String caps = null;

  /** Elements */
  private Element src;
  
  private Element colorspace;
  
  private Element videoscale;
  
  private Element capsfilter;

  /** AppSink, the last element. */
  private AppSink sink;

  /** True if no VGA signal detected. Will be set automaticly, do not set it manually! */
  private static boolean broken = false;

  /**
   * Constructor. Creates Epiphan VGA2USB device sub bin.
   * 
   * @param captureDevice
   *          CaptureDevice
   * @param caps
   *          Caps
   * @trows UnableToCreateElementException If the required GStreamer Modules are not installed to create all of the
   *        Elements this Exception will be thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSubDeviceBin(CaptureDevice captureDevice, String caps) throws UnableToCreateElementException,
          UnableToLinkGStreamerElementsException {

    super(captureDevice.getFriendlyName() + "_SubDeviceBin");
    this.captureDevice = captureDevice;
    this.caps = caps;

    createElements();
    setElementProperties();
    linkElements();
    setEOSListener();
    bin.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, bin.getName(), false);
  }
  
  /**
   * @return the src
   */
  public Element getSource() {
    return src;
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#getSink()
   */
  @Override
  public AppSink getSink() {
    if (!isBroken())
      return sink;
    else
      return null;
  }

  /**
   * Returns Caps.
   * 
   * @return Caps.
   */
  public String getCaps() {
    return caps;
  }
  
  /**
   * @return the colorspace
   */
  public Element getColorspace() {
    return colorspace;
  }
  
  /**
   * @return the videoscale
   */
  public Element getVideoscale() {
    return videoscale;
  }
  
  /**
   * @return the capsfilter
   */
  public Element getCapsfilter() {
    return capsfilter;
  }

  /**
   * Create elements and add these to bin.
   * 
   * @throws UnableToCreateElementException
   *           If any of the Elements fail to be created because the GStreamer module for the Element isn't present then
   *           this Exception will be thrown.
   */
  protected void createElements() throws UnableToCreateElementException {
    src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.V4LSRC, captureDevice.getLocation() + "_v4lsrc");
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, captureDevice.getLocation() + "_ffmpegcolorspace");
    videoscale = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFVIDEOSCALE, captureDevice.getLocation() + "_ffvideoscale");
    capsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, captureDevice.getLocation() + "_v4l_caps");
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.APPSINK, captureDevice.getLocation() + "_appsink");

    bin.addMany(src, colorspace, videoscale, capsfilter, sink);
  }

  /**
   * Set element properties.
   */
  protected void setElementProperties() {
    src.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
    src.set(GStreamerProperties.DO_TIMESTAP, "false");
    sink.set(GStreamerProperties.EMIT_SIGNALS, "false");
    sink.set(GStreamerProperties.DROP, "true");
    sink.set(GStreamerProperties.MAX_BUFFERS, "1");
    if (caps != null && !caps.isEmpty()) {
      capsfilter.setCaps(Caps.fromString(caps));
      sink.setCaps(Caps.fromString(caps));
    }
  }

  /**
   * Link elements together.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If Elements can not be linked together.
   */
  protected void linkElements() throws UnableToLinkGStreamerElementsException {

    if (!src.link(colorspace)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, colorspace);
    }
    if (!colorspace.link(videoscale)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, colorspace, videoscale);
    }
    if (!videoscale.link(capsfilter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoscale, capsfilter);
    }
    if (!capsfilter.link(sink)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, capsfilter, sink);
    }
  }

  /**
   * Remove all elements from bin.
   */
  protected void removeElements() {
    bin.removeMany(src, colorspace, videoscale, capsfilter, sink);
  }

  /**
   * Start bin and manage vga-signal-broken state.
   * 
   * @param time
   *          time to check, if bin is started, -1 skip checks.
   * @return true, if bin is started.
   */
  @Override
  public boolean start(long time) {
    if (setState(State.PLAYING, time)) {
      broken = false;
      return true;
    } else {
      stop();
      broken = true;
      return false;
    }
  }

  /**
   * Return broken state.
   * 
   * @return true if vga signal is broken.
   */
  protected synchronized boolean isBroken() {
    return broken;
  }

  /**
   * Set Bus EOS Listener to stop bin and set broken state.
   */
  protected void setEOSListener() {
    sink.getBus().connect(new EOS() {

      @Override
      public void endOfStream(GstObject source) {
        broken = true;
        bin.setState(State.NULL);
      }
    });
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
