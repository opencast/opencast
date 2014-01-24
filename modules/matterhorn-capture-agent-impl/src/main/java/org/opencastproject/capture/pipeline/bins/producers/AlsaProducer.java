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

import java.util.Properties;

public class AlsaProducer extends AudioProducer {
  private Element alsasrc;

  /**
   * Creates a ProducerBin specifically designed to capture from an ALSA source.
   * 
   * @param captureDevice
   *          The ALSA source {@code CaptureDevice} to create Producer around
   * @param properties
   *          The {@code Properties} properties such as confidence monitoring necessary to create the source.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If setElementProperties is called before createElements the GStreamer Elements will be null and this
   *           Exception will be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the Element returned by getSrcPad is not able to create GhostPads this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Elements necessary for this producer cannot be linked together this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           If the captureDevice parameter is null this Exception is thrown because some of those properties are
   *           necessary for creating this Producer.
   * @throws UnableToCreateElementException
   *           If the platform is not Linux, the alsasrc Element will not be able to be created and this Exception will
   *           be thrown .
   */
  public AlsaProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create all the elements required for an ALSA source
   * 
   * @throws UnableToCreateElementException
   *           If the platform is not Linux, or the correct module is not installed to support an alsasrc this Exception
   *           is thrown.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    alsasrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.ALSASRC, null);
  }

  /**
   * Set the correct location for the ALSA source
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the alsasrc Element is null this Exception is thrown
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    if (alsasrc != null) {
      if (captureDevice.getLocation() != null) {
        alsasrc.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
      }
    } 
    else {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(alsasrc, GStreamerProperties.DEVICE);
    }
  }

  /** Add all the necessary elements to the bin. **/
  protected void addElementsToBin() {
    bin.addMany(alsasrc, queue, volume, audioconvert);
  }

  /**
   * Link all the necessary elements together.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the elements will not link together it throws an exception.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!alsasrc.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, alsasrc, queue);
    } else if (!queue.link(volume)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, volume);
    } else if (!volume.link(audioconvert)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, volume, audioconvert);
    }
  }

}
