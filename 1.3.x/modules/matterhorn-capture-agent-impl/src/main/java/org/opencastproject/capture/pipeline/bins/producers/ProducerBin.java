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
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.PartialBin;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.gstreamer.Element;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.gstreamer.event.EOSEvent;

import java.util.Properties;

/**
 * Producer Bin is the ancestor for every device or file supported but gstreamer capture. 
 */
public abstract class ProducerBin extends PartialBin {
  
  public static final String GHOST_PAD_NAME = GStreamerProperties.SRC;
  protected Element queue;

  /**
   * ProducerBin is the super class for all sources for matterhorn including both audio and video sources.
   * 
   * To inherit to create a new ProducerBin:
   * 
   * 1. Override createElements to create any additional GStreamer Elements, override setElementProperties to set any
   * properties you need to on the aforementioned GStreamer Elements, Add all the Elements you need in the Bin by
   * overriding addElementsToBin, finally override linkElements and link together all of the Elements you need to
   * capture from the source.
   * 
   * 2. Override getSrcPad with the last GStreamer Element in your pipeline to get data from this source and change it
   * into something the gstreamer pipeline can use raw. This will be used to create the ghostpads for the ProducerBin
   * which will be used to connect this Producer to its Consumers such as filesinks, confidence monitoring etc.
   * 
   * @param captureDevice
   *          The details of the capture device in the configuration file we are dealing with.
   * @param confidenceMonitoringProperties
   *          The details in the configuration file about confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any properties are set on a null Element then this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the Producer is unable to create ghost pads for its Bin to link it to the Consumers then this
   *           Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if any of the necessary GStreamer Elements are unable to link to each other.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is necessary so this Exception is thrown if it is null.
   * @throws UnableToCreateElementException
   *           If any necessary GStreamer Element is unable to be created then this Exception is thrown.
   */
  public ProducerBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create all elements necessary by all capture devices, in this case a queue.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if the queue cannot be created.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
  }

  /**
   * Create the Ghost Pads necessary to link the source to the tee in the @code{CaptureDeviceBin}.
   * 
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if Ghost Pads cannot be created.
   **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
    Pad ghostPadElement = this.getSrcPad();
    if (ghostPadElement == null || !bin.addPad(new GhostPad(GHOST_PAD_NAME, ghostPadElement))) {
      throw new UnableToCreateGhostPadsForBinException("Could not create new Ghost Pad with " + this.getSrcPad());
    }
  }

  /**
   * Abstract method so that we can get Element we will use to create the ghost pads for this Producer. If you are
   * creating a Producer just return the last Element in your pipeline with an available src pad.
   * 
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if the ghost pads cannot be created
   */
  protected abstract Pad getSrcPad() throws UnableToCreateGhostPadsForBinException;
  
  /** Sends an EOS event to all of the sources within the Bin. **/
  public void shutdown() {
    /**
     * Send an EOS to all of the source elements for this Bin.
     **/
    for (Element element : bin.getSources()) {
      logger.info("Sending EOS to stop " + element.getName());
      element.sendEvent(new EOSEvent());
    }
  }
}
