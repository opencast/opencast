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
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.gstreamer.Element;

import java.util.Properties;

/**
 * TODO: Comment me!
 */
public class XVImagesinkConsumer extends ConsumerBin {
  private Element xvimagesink;

  /**
   * Available in Linux only. Streams Producer video data in real time to the screen so that it can be watched by using
   * the GStreamer Element xvimagesink.
   *
   * @throws UnableToCreateElementException
   *           If the platform is not Linux or the gst-plugins-base is not installed this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           Since there is only one Element involved for this Consumer, this Exception should never be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If ConsumerBin is unable to create the ghost pads from the xvimagesink this Exception will be thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           No properties are set for this Consumer, this Exception should never be thrown.
   * @throws CaptureDeviceNullPointerException
   *           If the captureDevice parameter is null then this Exception is thrown.
   **/
  public XVImagesinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create the XVImageSink.
   *
   * @throws UnableToCreateElementException
   *           If the OS is not Linux or the gst-plugins-bas is not installed.
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    xvimagesink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.XVIMAGESINK, null);
  }

  /** Adds the xvimagesink to the bin **/
  @Override
  protected void addElementsToBin() {
    bin.add(xvimagesink);
  }

  /** Returns the xvimagesink as the source so that we can attach the ghostpads of this bin to a Producer **/
  @Override
  public Element getSrc() {
    return xvimagesink;
  }

}
