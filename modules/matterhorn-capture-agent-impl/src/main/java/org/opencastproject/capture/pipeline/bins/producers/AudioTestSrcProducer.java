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

import java.util.Properties;

public class AudioTestSrcProducer extends AudioProducer {

  private Element audiotestsrc;

  /**
   * Used to create an audiotestsrc GStreamer Element Producer great for testing the capture agent without needing any
   * devices installed but still gives that authentic capturing experience.
   *
   * @param captureDevice
   *          The details for this capture device.
   * @param properties
   *          The confidence monitoring details for this device.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Shouldn't be thrown by this class as it sets no properties.
   * @throws UnableToCreateGhostPadsForBinException
   *           If an src pad cannot be grabbed from the audiotestsrc this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If anything fails to link, in this case nothing, it throws an exception with the details.
   * @throws CaptureDeviceNullPointerException
   *           If the captureDevice parameter is null then this Exception is thrown
   * @throws UnableToCreateElementException
   *           If the current system cannot create an audiotestsrc Element this Exception is thrown
   */
  public AudioTestSrcProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create an audiotestsrc Element (see gst-inspect for details)
   *
   * @throws UnableToCreateElementException
   *           If the current system cannot create an audiotestsrc Element this Exception is thrown.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    audiotestsrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.AUDIOTESTSRC, null);
  }

  /** Add the audiotestsrc to the bin we will return as a source. **/
  @Override
  protected void addElementsToBin() {
    bin.add(audiotestsrc);
  }

  /**
   * The sink for this source is the audiotestsrc itself that will be used to create the ghost pads for this bin.
   **/
  @Override
  public Pad getSrcPad() {
    return audiotestsrc.getStaticPad(GStreamerProperties.SRC);
  }

}
