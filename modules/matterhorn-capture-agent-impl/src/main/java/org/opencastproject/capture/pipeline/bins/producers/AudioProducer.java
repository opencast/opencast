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

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.apache.commons.lang.StringUtils;
import org.gstreamer.Element;
import org.gstreamer.Pad;

import java.util.Properties;

public abstract class AudioProducer extends ProducerBin {
  /**
   * Audio convert is used to convert any input audio into a format usable by gstreamer. Might not be strictly
   * necessary.
   **/
  protected Element audioconvert;
  /**
   * Volume is used to control the volume level of the audio capture.
   */
  protected Element volume;
  /**
   * The volume level to set the volume element to.
   */
  private double volumeLevel = 1.0;

  /**
   * Super class for all audio Producers whether they are test, pulse, alsa or other. To create a descendant to create a
   * new audio Producer:
   * 
   * 1. Override createElements to add any new Elements you might need, setElementProperties to set any specific
   * properties on the newly created Elements, addElementsToBin to add these new Elements to the Bin and finally
   * linkElements to link your new Elements together and possibly to the queue and audio converter.
   * 
   * 2. If you are using audioconverter as the last Element in your Bin leave it as the getSrcPad target. If you have a
   * new Element as the last one in your Bin (that will act as the source for the Consumers) please return it's src pads
   * as the ones to ghost so that it can be connected to the Consumers.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If setProperties is called before createElements this Exception is thrown because all of the Elements
   *           will be null.
   * @throws UnableToCreateGhostPadsForBinException
   *           If getSrcPad doesn't return a Pad that can be used as a ghostpad for this Bin then this Exception is
   *           thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If any Element isn't able to link to another then this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The parameter captureDevice has some necessary properties such as where to place the resulting file. If
   *           it is null we cannot create this Producer and therefore this Exception is thrown.
   * @throws UnableToCreateElementException
   *           If for any reason an Element is attempted to be created that this particular machine doesn't have the
   *           module necessary installed this Exception will bet thrown.
   **/
  public AudioProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create all the common element for audio sources an audio converter and a volume control.
   * 
   * @throws UnableToCreateElementException
   *           If the necessary module to create an audioconverter isn't present then this Exception is thrown.
   **/
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    audioconvert = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.AUDIOCONVERT, null);
    volume = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VOLUME, null);
  }

  /**
   * @see org.opencastproject.capture.pipeline.bins.PartialBin#setElementProperties()
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setVolumeProperties();
  }

  private void setVolumeProperties() {
    String volumeString = StringUtils.trimToNull(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX
            + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_VOLUME));
    if (volumeString != null) {
      volumeLevel = Double.parseDouble(volumeString);
    }
    volume.set(CaptureParameters.CAPTURE_DEVICE_VOLUME.substring(1), volumeLevel);
    logger.debug("Using a volume level of " + volumeLevel);
  }

  /**
   * Returns the sink for this source so that we can create ghost pads for the bin. These ghost pads are then used to
   * link the Producer to a tee, and from that tee to all of the Consumers we might need.
   */
  @Override
  protected Pad getSrcPad() {
    return audioconvert.getStaticPad(GStreamerProperties.SRC);
  }

  /** This is used by CaptureDeviceBin to know to use a AudioConsumer **/
  @Override
  public boolean isAudioDevice() {
    return true;
  }
}
