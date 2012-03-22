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
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import java.util.Properties;

public final class ConsumerFactory {
  /** The actual singleton factory **/
  private static ConsumerFactory factory;

  /** Singleton factory pattern ensuring only one instance of the factory is created even with multiple threads. **/
  public static synchronized ConsumerFactory getInstance() {
    if (factory == null) {
      factory = new ConsumerFactory();
    }
    return factory;
  }

  /** Constructor made private so that the number of Factories can be kept to one. **/
  private ConsumerFactory() {
  }

  /**
   * Returns the Consumer corresponding to the ConsumerType
   * 
   * @param consumerType
   *          The enum value of the Consumer to create.
   * @param captureDevice
   *          The properties of the capture device such as container, codec, bitrate, enabled confidence monitoring etc.
   *          used to initialize the Consumer.
   * @param properties
   *          Some individual properties of the confidence monitoring.
   * @throws NoConsumerFoundException
   *           If no consumer can be found for this particular ConsumerType then this Exception will be thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If the ConsumerBin has problems linking its Elements together this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If ghostpads cannot be created to link Producers to Consumers this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the creation of the Consumer somehow calls the setElementProperties before it calls createElements the
   *           Elements will be null and this Exception will be thrown.
   * @throws CaptureDeviceNullPointerException
   *           If a null CaptureDevice is passed to this ConsumerFactory then this Exception will be thrown since there
   *           are certain settings that are needed to create a Consumer in this parameter.
   * @throws UnableToCreateElementException
   *           If an Element for the ConsumerBin cannot be created because this machine does not have the necessary
   *           GStreamer module installed or it is an OS that this Consumer doesn't support this Exception is thrown.
   **/
  public ConsumerBin getSink(ConsumerType consumerType, CaptureDevice captureDevice, Properties properties)
          throws NoConsumerFoundException, UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (consumerType == ConsumerType.AUDIO_FILE_SINK)
      return new AudioFilesinkConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.XVIMAGE_SINK)
      return new XVImagesinkConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.FILE_SINK)
      return new FilesinkConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.VIDEO_FILE_SINK)
      return new VideoFilesinkConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.CUSTOM_CONSUMER)
      return new CustomConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.AUDIO_MONITORING_SINK)
      return new AudioMonitoringConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.VIDEO_MONITORING_SINK)
      return new VideoMonitoringConsumer(captureDevice, properties);
    else if (consumerType == ConsumerType.RTP_SINK)
      return new RTPSinkConsumer(captureDevice, properties);
    else {
      throw new NoConsumerFoundException("No valid SinkBin found for device " + consumerType);
    }
  }
}
