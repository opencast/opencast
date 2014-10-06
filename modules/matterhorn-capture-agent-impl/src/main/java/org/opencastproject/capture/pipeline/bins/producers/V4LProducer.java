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
 * This V4LProducer supports capturing from a v4l device.
 */
public class V4LProducer extends VideoProducer {

  protected Element v4lsrc;

  /**
   * V4LProducer captures from a generic V4L src.
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
  public V4LProducer(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create the v4lsrc.
   *
   * @throws UnableToCreateElementException
   *           Thrown if the module for v4l is not installed or the current machine doens't support v4l.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    v4lsrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.V4LSRC, null);
  }

  /**
   * Set the device location of the v4lsrc
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the v4l2src is null then this Exception is thrown
   * @throws IllegalArgumentException
   *           Thrown if location cannot be read from.
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    if (v4lsrc == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(v4lsrc, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("V4LProducer cannot read from location " + captureDevice.getLocation());
    }
    v4lsrc.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
  }

  /** Add all of the necessary GStreamer Elements to the Bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4lsrc, queue, videorate, fpsfilter);
  }

  /**
   * Link all of the GStreamer Elements together.
   *
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Elements cannot be linked together this Exception is thrown.
   **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!v4lsrc.link(queue))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lsrc, queue);
    else if (!queue.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }

  /** Returns the src pad of the fpsfilter to create ghost pads for this Producer. **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }
}
