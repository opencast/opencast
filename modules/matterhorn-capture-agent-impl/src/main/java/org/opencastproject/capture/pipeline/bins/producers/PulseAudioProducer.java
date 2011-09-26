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

/**
 * TODO: Comment me!
 */
public class PulseAudioProducer extends AudioProducer {

  protected Element pulseAudioSrc;

  /**
   * PulseAudioProducer captures from a pulse audio source such as a desktop linux distro.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Not thrown by this Producer
   * @throws UnableToCreateGhostPadsForBinException
   *           If the pulsesrc does not provide its src pads then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Not thrown by this class
   * @throws CaptureDeviceNullPointerException
   *           Parameter captureDevice is mandatory, thrown if it is null
   * @throws UnableToCreateElementException
   *           If the pulsesrc cannot be created this Exception is thrown
   * **/
  public PulseAudioProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create the pulseAudioSrc
   * 
   * @throws UnableToCreateElementException
   *           Thrown if cannot create pulsesrc
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    pulseAudioSrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.PULSESRC, null);
  }

  /** Return the pulsesrc src pad so that it can be ghosted for this Producer. **/
  @Override
  public Pad getSrcPad() {
    return pulseAudioSrc.getStaticPad(GStreamerProperties.SRC);
  }

  /** Add the pulse source to the bin. **/
  @Override
  protected void addElementsToBin() {
    bin.add(pulseAudioSrc);
  }

}
