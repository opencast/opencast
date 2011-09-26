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
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.gstreamer.Element;
import org.gstreamer.Pad;

import java.io.File;
import java.util.Properties;

/**
 * TODO: Comment me!
 */
public class V4L2Producer extends VideoProducer {

  protected Element v4l2src;

  /**
   * V4L2Producer captures from a generic V4L2 src such as a webcam.
   * 
   * @param captureDevice
   *          The details of the capture device such as location, bitrate, container etc.
   * @param propeties
   *          The confidence monitoring settings
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if the GStreamer Elements cannot be linked together
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if the pads from the fpsfilter cannot be ghosted
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if the device location cannot be read.
   * @throws CaptureDeviceNullPointerException
   *           captureDevice parameter is necessary, thrown if it is null
   * @throws UnableToCreateElementException
   *           If an Element's module is not installed for the local GStreamer this Exception is thrown.
   * **/
  public V4L2Producer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Creates the v4l2src.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if the necessary module for v4l2src is not installed or the platform doesn't support v4l2.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    v4l2src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.V4L2SRC, null);
  }

  /**
   * Set the device location of the v4l2src
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the v4l2src is null then this Exception is thrown
   * @throws IllegalArgumentException
   *           Thrown if the device location cannot be read.
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    if (v4l2src == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(v4l2src, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("V4L2Producer cannot read from location " + captureDevice.getLocation());
    }
    v4l2src.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
  }

  /** Add all of the necessary Elements to the Bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  /** Link all of the Elements together **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!v4l2src.link(queue))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, queue);
    else if (!queue.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }

  /** Return the src pad of the fpsfilter to create a ghost pad for this bin and connect it to the Consumers **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }
}
