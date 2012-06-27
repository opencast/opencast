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
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import java.util.HashSet;
import java.util.Properties;

public final class ProducerFactory {
  /** The actual singleton factory **/
  private static ProducerFactory factory;

  /**
   * The gstreamer sources that are currently supported and tested with this code
   */
  public enum ProducerType {
    ALSASRC, /* Linux sound capture */
    AUDIOTESTSRC, /* Built in gstreamer audio test src */
    BLUECHERRY_PROVIDEO, /* Bluecherry ProVideo-143 */
    CUSTOM_VIDEO_SRC, /* Allows the user to specify their producer with gstreamer command line syntax */
    CUSTOM_AUDIO_SRC, /* Allows the user to specify their producer with gstreamer command line syntax */
    DV_1394, /* A DV camera that runs over firewire */
    EPIPHAN_VGA2USB, /* Epiphan VGA2USB frame grabber */
    FILE, /* A media file on the filesystem or a file device that requires no decoding */
    FILE_DEVICE, /* Generic file device source (such as a Hauppauge card that produces an MPEG file) */
    HAUPPAUGE_WINTV, /* Hauppauge devices */
    PULSESRC, /* Linux sound capture */
    V4LSRC, /* Generic v4l source */
    V4L2SRC, /* Generic v4l2 source */
    VIDEOTESTSRC /* Built in gstreamer video test src */    
  }
  
  private HashSet<ProducerType> producersWithoutSourceRequirement = new HashSet<ProducerType>();
  
  /** Singleton factory pattern ensuring only one instance of the factory is created even with multiple threads. **/
  public static synchronized ProducerFactory getInstance() {
    if (factory == null) {
      factory = new ProducerFactory();
    }
    return factory;
  }

  /** Constructor made private so that the number of Factories can be kept to one. **/
  private ProducerFactory() {
    // Producers that don't require a source. 
    producersWithoutSourceRequirement.add(ProducerType.ALSASRC);
    producersWithoutSourceRequirement.add(ProducerType.AUDIOTESTSRC);
    producersWithoutSourceRequirement.add(ProducerType.CUSTOM_AUDIO_SRC);
    producersWithoutSourceRequirement.add(ProducerType.CUSTOM_VIDEO_SRC);
    producersWithoutSourceRequirement.add(ProducerType.PULSESRC);
    producersWithoutSourceRequirement.add(ProducerType.VIDEOTESTSRC);
  }

  /**
   * Returns the Producer corresponding to the ProducerType
   * 
   * @param captureDevice
   *          The properties of the capture device such as container, codec, bitrate, enabled confidence monitoring etc.
   *          used to initialize the Producer.
   * @param properties
   *          Some individual properties of the confidence monitoring.
   * @throws UnableToLinkGStreamerElementsException
   *           If the ProducerBin has problems linking its Elements together this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If ghostpads cannot be created to link Producers to Consumers this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the creation of the Consumer somehow calls the setElementProperties before it calls createElements the
   *           Elements will be null and this Exception will be thrown.
   * @throws CaptureDeviceNullPointerException
   *           If a null CaptureDevice is passed to this ProducerFactory then this Exception will be thrown since there
   *           are certain settings that are needed to create a Consumer in this parameter.
   * @throws UnableToCreateElementException
   *           If an Element for the ProducerBin cannot be created because this machine does not have the necessary
   *           GStreamer module installed or it is an OS that this Producer doesn't support this Exception is thrown.
   * @throws NoProducerFoundException
   *           If no Producer can be found for this particular ProducerType then this Exception will be thrown.
   **/
  public ProducerBin getProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException, NoProducerFoundException {
    if (captureDevice.getName() == ProducerType.EPIPHAN_VGA2USB)
      return new EpiphanVGA2USBV4LProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.HAUPPAUGE_WINTV)
      return new HauppaugePVR350VideoProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.FILE)
      return new UndecodedFileProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.FILE_DEVICE)
      return new FileProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.BLUECHERRY_PROVIDEO)
      return new BlueCherryBT878Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.ALSASRC)
      return new AlsaProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.PULSESRC)
      return new PulseAudioProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.AUDIOTESTSRC)
      return new AudioTestSrcProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.DV_1394)
      return new DV1394Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.VIDEOTESTSRC)
      return new VideoTestSrcProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.V4LSRC)
      return new V4LProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.V4L2SRC)
      return new V4L2Producer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.CUSTOM_VIDEO_SRC)
      return new CustomVideoProducer(captureDevice, properties);
    else if (captureDevice.getName() == ProducerType.CUSTOM_AUDIO_SRC)
      return new CustomAudioProducer(captureDevice, properties);
    else {
      throw new NoProducerFoundException("No valid Producer found for device " + captureDevice.getName());
    }
  }
  
  /**
   * Returns true if the ProducerType does require a source to create, returns false if ProducerType is null, doesn't
   * exist or doesn't require the source location.
   * 
   * @param type The type of Producer that needs to be checked whether it requires a source. 
   * @return Returns true if it requires a source, false otherwise. 
   */
  public boolean requiresSrc(ProducerType type) {
    if (type == null) {
      return false;
    }
    return !producersWithoutSourceRequirement.contains(type);
  }
}
