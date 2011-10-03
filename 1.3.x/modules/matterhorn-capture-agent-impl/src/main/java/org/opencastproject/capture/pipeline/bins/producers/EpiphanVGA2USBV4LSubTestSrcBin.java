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

/**
 * Videotestsrc sub bin to use in {@link EpiphanVGA2USBV4LProducer}. Creates a bin with videotestsrc Element to grab
 * signal from and AppSink Element to connect with {@link EpiphanVGA2USBV4LProducer}.
 */
public class EpiphanVGA2USBV4LSubTestSrcBin extends EpiphanVGA2USBV4LSubAbstractBin {
  
  private static final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LSubTestSrcBin.class);
  
  /** CaptureDevice */
  private CaptureDevice captureDevice;

  /** Caps */
  protected String caps = null;

  /** Elements */
  private Element src;

  private Element capsFilter;

  /** AppSink, the last element */
  private AppSink sink = null;

  /**
   * Constructor. Creates a videotestsrc sub bin.
   * 
   * @param captureDevice
   *          CaptureDevice
   * @param caps
   *          Caps
   * @throws UnableToCreateElementException
   *           If the required GStreamer Modules are not installed to create all of the Elements this Exception will be
   *           thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSubTestSrcBin(CaptureDevice captureDevice, String caps)
          throws UnableToCreateElementException, UnableToLinkGStreamerElementsException {

    this.captureDevice = captureDevice;
    this.caps = caps;

    createElements();
    setElementProperties();
    linkElements();

    bin.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, bin.getName(), false);
  }

  /**
   * Returns the capture defice.
   * 
   * @return the capture device
   */
  public CaptureDevice getCaptureDevice() {
    return captureDevice;
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
   * Returns the source.
   * 
   * @return the source
   */
  public Element getSource() {
    return src;
  }

  /**
   * Returns the caps filter.
   * 
   * @return the capsFilter
   */
  public Element getCapsFilter() {
    return capsFilter;
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
            GStreamerElements.VIDEOTESTSRC, null);
    capsFilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.APPSINK, null);
    bin.addMany(src, capsFilter, sink);
  }

  /**
   * Set bin element properties.
   */
  protected void setElementProperties() {
    src.set(GStreamerProperties.PATTERN, "0");
    src.set(GStreamerProperties.DO_TIMESTAP, "false");
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
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(capsFilter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, capsFilter);
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
    bin.removeMany(src, capsFilter, sink);
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
