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

import org.opencastproject.capture.pipeline.bins.BufferThread;
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

import java.util.Properties;

public abstract class ConsumerBin extends PartialBin {
  public static final String GHOST_PAD_NAME = GStreamerProperties.SINK;
  protected Element queue;
  protected Element encoder;
  protected Element muxer;
  protected Element filesink;

  /**
   * ConsumerBin is the ancestor for all Consumers and is meant to be inherited. To inherit from ConsumerBin:
   * 
   * 1. In the same pattern for the rest of the descendants of PartialBin make sure you create any additional Elements
   * you need in an overridden createElements, set any of the properties for the Elements in overridden setProperties,
   * add all Elements into the Bin with overridden addElementsToBin and finally link your Elements together with
   * linkElements.
   * 
   * 2. Make sure you return the first Element (the Element with an available sink pad) in your Bin in the getSrc
   * method. This will be used to create the ghost pads for the bin so that it can be linked to the Producer.
   * 
   * @param captureDevice
   *          This is the properties such as codec, container, bitrate etc. of the capture device output.
   * 
   * @param properties
   *          This is the confidence monitoring properties
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If there is a problem linking any of the Elements together that make up the SinkBin this Exception will
   *           be thrown
   * @throws UnableToCreateGhostPadsForBinException
   *           If the getSrc function returns an Element that cannot be used to create a ghostpad for this SinkBin, then
   *           this Exception is thrown. This could be due to the src being null or one that doesn't have the pads
   *           available to ghost.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the setElementPropeties is called before the createElements function then the Elements will not be
   *           created, show up as null and this Exception will be thrown.
   * @throws CaptureDeviceNullPointerException
   *           Because there are essential properties that we can't infer, such as output location, if the CaptureDevice
   *           parameter is null this exception is thrown.
   * @throws UnableToCreateElementException
   *           If the current setup is not able to create an Element because either that Element is specific to another
   *           OS or because the module for that Element is not installed this Exception is thrown.
   */
  public ConsumerBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
    addTrace();
  }

  /**
   * Creates a queue so that the tee won't pause this Bin automatically and a filesink to dump the data created by the
   * producer.
   * 
   * @throws UnableToCreateElementException
   *           This function will not actually throw this Exception personally because both queue and filesink are part
   *           of the GStreamer core module. A basic GStreamer exception will be thrown before this when the GST.init is
   *           called.
   **/
  protected void createElements() throws UnableToCreateElementException {
    createQueue();
    createFileSink();
  }

  /**
   * Creates the queue that prevents us dropping frames and unpauses the tee used to connect all consumers. Otherwise
   * the tee will not unpause this Bin and only other Consumers will be able to use the data the Producers create.
   * 
   * @throws UnableToCreateElementException
   *           If the current system does not have the correct GStreamer module for queue installed this Exception will
   *           be thrown.
   **/
  private void createQueue() throws UnableToCreateElementException {
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, captureDevice.getFriendlyName());
  }

  /**
   * Creates the filesink that will be the output of the capture device
   * 
   * @throws UnableToCreateElementException
   *           If the current system does not have the correct GStreamer module for filesink installed this Exception
   *           will be thrown.
   **/
  private void createFileSink() throws UnableToCreateElementException {
    filesink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESINK, null);
  }

  /**
   * Sets the queue size for the sink so that we don't lose any information.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the queue is null because this function is called before createElements this exception is thrown.
   * @throws IllegalArgumentException
   **/
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {
    setQueueProperties();
  }

  /**
   * Sets the size of the queue that acts as a buffer so that we don't lose frames or audio bits.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the queue is null because this function is called before createElements this exception is thrown.
   **/
  private void setQueueProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (queue == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(queue, "all properties.");
    }
    synchronized (queue) {
      if (captureDeviceProperties.getBufferCount() != null) {
        logger.debug("{} bufferCount is being set to {}.", captureDevice.getName(),
                captureDeviceProperties.getBufferCount());
        queue.set(GStreamerProperties.MAX_SIZE_BUFFERS, captureDeviceProperties.getBufferCount());
      }
      if (captureDeviceProperties.getBufferBytes() != null) {
        logger.debug("{} bufferBytes is being set to {}.", captureDevice.getName(),
                captureDeviceProperties.getBufferBytes());
        queue.set(GStreamerProperties.MAX_SIZE_BYTES, captureDeviceProperties.getBufferBytes());
      }
      if (captureDeviceProperties.getBufferTime() != null) {
        logger.debug("{} bufferTime is being set to {}.", captureDevice.getName(),
                captureDeviceProperties.getBufferTime());
        queue.set(GStreamerProperties.MAX_SIZE_TIME, captureDeviceProperties.getBufferTime());
      }
    }
  }

  /**
   * Creates the ghost pad that will connect this Consumer to the Producer.
   * 
   * @throws UnableToCreateGhostPadsForBinException
   *           If the getSrc function returns a null Element or there is a problem adding the pad as a ghost pad (maybe
   *           no sink pads are available) an Exception is thrown.
   **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
    Pad ghostPadElement = getSrc().getStaticPad(GStreamerProperties.SINK);
    if (ghostPadElement != null && !bin.addPad(new GhostPad(GHOST_PAD_NAME, ghostPadElement))) {
      throw new UnableToCreateGhostPadsForBinException("Could not create new Ghost Pad with " + this.getSrc().getName()
              + " in this SinkBin.");
    }
  }

  /** Is used by createGhostPads, set to the first Element in your Bin so that we can link your SinkBin to Sources. **/
  public abstract Element getSrc();

  /** If the system is setup to have trace level error tracking we will add a thread to monitor the queue **/
  private void addTrace() {
    if (logger.isTraceEnabled()) {
      Thread traceThread = new Thread(new BufferThread(queue));
      traceThread.start();
    }
  }
}
