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
public class BlueCherryBT878Producer extends V4L2Producer {
  private Element v4l2src;

  /**
   * Creates a Producer specifically designed to handle a BlueCherry Provideo Card using the BT 878 chipset. Mainly a
   * duplicate of the V4L2Producer but left as a legacy class so that our install scripts do not have to change yet.
   *
   * @param captureDevice
   *          The Bluecherry {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the setProperties function is called while any of the salient GStreamer Elements are null this
   *           Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the FPS filter's src pad cannot be used as a ghost pad for this bin this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           When a GStreamer Element won't link to another this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is required to create this Blue Cherry Producer so an Exception will be
   *           thrown if it is null.
   * @throws UnableToCreateElementException
   *           If the current system doesn't support the GStreamer Module required to create all of the Elements of this
   *           class then this Exception is thrown. -
   */
  public BlueCherryBT878Producer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create the v4l2src Element
   *
   * @throws UnableToCreateElementException
   *           If the current system cannot create the v4l2src then this Exception is thrown. Since this source is
   *           supported only on Linux a Windows or Mac system will throw this Exception.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    v4l2src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.V4L2SRC, null);
  }

  /**
   * Set the location of the v4l2src
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the v4l2src is null then this Exception is thrown
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    if (v4l2src == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(v4l2src, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("BlueCherryBT838Producer cannot read from location "
              + captureDevice.getLocation());
    }
    v4l2src.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
  }

  /** Add the v4l2src, queue, videorate corrector and fpsfilter to the bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  /**
   * Link together all of the {@code Element}s in this bin so that we can use them as a Producer.
   *
   * @throws UnableToLinkGStreamerElementsException
   *           When something in the bin doesn't link together correctly we throw an exception.
   **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!v4l2src.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, queue);
    } else if (!queue.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    } else if (!videorate.link(fpsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
  }

  /** Returns the fpsfilter element as the sink for this source bin since it is the last part of this source. **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }
}
