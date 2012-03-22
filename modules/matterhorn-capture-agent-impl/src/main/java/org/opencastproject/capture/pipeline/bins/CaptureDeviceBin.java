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
package org.opencastproject.capture.pipeline.bins;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.consumers.ConsumerBin;
import org.opencastproject.capture.pipeline.bins.consumers.ConsumerFactory;
import org.opencastproject.capture.pipeline.bins.consumers.ConsumerType;
import org.opencastproject.capture.pipeline.bins.consumers.NoConsumerFoundException;
import org.opencastproject.capture.pipeline.bins.producers.NoProducerFoundException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerBin;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Properties;

public class CaptureDeviceBin {
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBin.class);
  // The bin that contains the Producer and Consumers.
  private Bin bin = new Bin();
  // The tee that connects the Producer to the Consumers.
  private Element tee;
  // The Producer for the capture device, known as a src in GStreamer language.
  private ProducerBin producerBin;
  // The collection of Consumers.
  private LinkedList<ConsumerBin> consumerBins = new LinkedList<ConsumerBin>();
  // The properties of the capture device.
  private CaptureDevice captureDevice;
  // Constant that determine whether to use the XV Image sink.
  private static final boolean USE_XV_IMAGE_SINK = false;

  /***
   * Creates a bin that contains a complete pipeline from Producer to all Consumers for a capture device.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if any of the GStreamer Elements for the Producers or Consumers fails to Link together.
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if one of the Producer or Consumers can't create ghost pads used to connect them together.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if setting one of the GStreamer Elements is null when setting one of its Properties.
   * @throws NoConsumerFoundException
   *           Thrown if no consumer can be found for the type specified.
   * @throws NoProducerFoundException
   *           Thrown if no producer can be found for the type specified.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if the captureDevice parameter is null since it is necessary for creating both Producers and
   *           Consumers.
   * @throws UnableToCreateElementException
   *           Thrown if a GStreamer Element can't be created because the platform it is running on doesn't support it
   *           or the necessary GStreamer module is not installed.
   */
  public CaptureDeviceBin(CaptureDevice captureDevice, Properties properties, boolean confidenceOnly)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, NoConsumerFoundException,
          CaptureDeviceNullPointerException, UnableToCreateElementException, NoProducerFoundException {
            
    this.captureDevice = captureDevice;
    if (isFile()) {
      // if captureDevice is a file (or capture card with hardware encoding element) set ProducerType to FILE
      this.captureDevice = new CaptureDevice(captureDevice.getLocation(), 
              ProducerType.FILE, captureDevice.getFriendlyName(), captureDevice.getOutputPath());
      this.captureDevice.setProperties(captureDevice.getProperties());
    }
    createProducer(this.captureDevice, properties);
    
    if (!confidenceOnly) {
      // pass if captureagent should capture
      createConsumers(this.captureDevice, properties);
      createRTPSinkConsumer(captureDevice, properties);
    }
    
    createMontoringConsumer(this.captureDevice, properties);
    linkProducerToConsumers();
  }
  
  /***
   * Creates a bin that contains a complete pipeline from Producer to all Consumers for a capture device.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if any of the GStreamer Elements for the Producers or Consumers fails to Link together.
   * @throws UnableToCreateGhostPadsForBinException
   *           Thrown if one of the Producer or Consumers can't create ghost pads used to connect them together.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if setting one of the GStreamer Elements is null when setting one of its Properties.
   * @throws NoConsumerFoundException
   *           Thrown if no consumer can be found for the type specified.
   * @throws NoProducerFoundException
   *           Thrown if no producer can be found for the type specified.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if the captureDevice parameter is null since it is necessary for creating both Producers and
   *           Consumers.
   * @throws UnableToCreateElementException
   *           Thrown if a GStreamer Element can't be created because the platform it is running on doesn't support it
   */
  public CaptureDeviceBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, NoConsumerFoundException,
          CaptureDeviceNullPointerException, UnableToCreateElementException, NoProducerFoundException {
          
    this(captureDevice, properties, false);
  }
  
  /**
   * Returns true if {@link captureDevice} is a file or 
   * a Hauppauge card and no codec and container properties are set
   * (so that we take advantage of the onboard mpeg encoding  and don't do it in software).
   * 
   * @return true if {@link captureDevice} is a file.
   */
  private boolean isFile() {
    return captureDevice.getName() == ProducerType.FILE 
            || (captureDevice.getName() == ProducerType.HAUPPAUGE_WINTV
            && captureDevice.getProperties().getProperty("codec") == null
            && captureDevice.getProperties().getProperty("container") == null);
  }

  /**
   * Creates the Producer that will be used for this Capture Device
   * 
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Producer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the end of the Producer is unable to ghost its src pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Producer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Producer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Producer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ProducerType that unrecognized.
   * **/
  private void createProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException, NoProducerFoundException {
          
    producerBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
  }

  /**
   * Creates the Consumers that will be used for this Capture Device
   * 
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Consumer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the beginning of the Consumer is unable to ghost its sink pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Consumer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Consumer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Consumer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ConsumerType that unrecognized.
   * **/
  private void createConsumers(CaptureDevice captureDevice, Properties properties) throws NoConsumerFoundException,
          UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    /** This will look like capture capture.device.friendlyName.customConsumer=queue ! xvimagesink **/
    if (properties.get(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER) != null) {
      createCustomConsumer(captureDevice, properties);
    } else {
      createFilesinkConsumer(captureDevice, properties);
      createXVImagesinkConsumer(properties);
    }
  }

  
  /**
   * Creates a filesink Consumer for this Producer that will save the media to a file.
   * 
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Consumer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the beginning of the Consumer is unable to ghost its sink pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Consumer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Consumer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Consumer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ConsumerType that unrecognized.
   * **/
  private void createFilesinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws NoConsumerFoundException, UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    ConsumerBin consumerBin;
    if (isFile()) {
      consumerBin = ConsumerFactory.getInstance().getSink(ConsumerType.FILE_SINK, captureDevice, properties);
    } else if (producerBin.isVideoDevice()) {
      consumerBin = ConsumerFactory.getInstance().getSink(ConsumerType.VIDEO_FILE_SINK, captureDevice, properties);
    } else if (producerBin.isAudioDevice()) {
      consumerBin = ConsumerFactory.getInstance().getSink(ConsumerType.AUDIO_FILE_SINK, captureDevice, properties);
    } else {
      throw new NoConsumerFoundException("Undefined Consumer!");
    }
    consumerBins.add(consumerBin);
  }
  
  /**
   * Creates an RTP Consumer that will stream audio and/or video-data over the network as RTP and RTCP Stream.
   * 
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Consumer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the beginning of the Consumer is unable to ghost its sink pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Consumer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Consumer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Consumer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ConsumerType that unrecognized.
   * **/
  private void createRTPSinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws NoConsumerFoundException, UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    
    ConsumerBin consumerBin = null;
    String prefix = CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName();
    
    if (properties.containsKey(prefix + CaptureParameters.CAPTURE_RTP_AUDIO_CONSUMER_RTP_PORT)
            || properties.containsKey(prefix + CaptureParameters.CAPTURE_RTP_VIDEO_CONSUMER_RTP_PORT)) {
      
      logger.info("Create a {} RTP consumer!", captureDevice.getFriendlyName());
              
      consumerBin = ConsumerFactory.getInstance().getSink(ConsumerType.RTP_SINK, captureDevice, properties);
      consumerBins.add(consumerBin);
    }
  }

  private void createCustomConsumer(CaptureDevice captureDevice, Properties properties)
          throws NoConsumerFoundException, UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    logger.debug("Using Custom Consumer");
    consumerBins.add(ConsumerFactory.getInstance().getSink(ConsumerType.CUSTOM_CONSUMER, captureDevice, properties));
  }
  
  /**
   * Creates an XVImagesink Consumer for this Producer that will show the video input in real time.
   * 
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Consumer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the beginning of the Consumer is unable to ghost its sink pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Consumer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Consumer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Consumer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ConsumerType that unrecognized.
   * **/
  private void createXVImagesinkConsumer(Properties properties) throws NoConsumerFoundException,
          UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    if (USE_XV_IMAGE_SINK) {
      if (producerBin.isVideoDevice()) {
        ConsumerBin xvImageSinkBin = ConsumerFactory.getInstance().getSink(ConsumerType.XVIMAGE_SINK, captureDevice,
                properties);
        consumerBins.add(xvImageSinkBin);
      }
    }
  }
  
  /**
   * Creates an monitoring Consumer for capture device described in {@code captureDevice}.
   * @param captureDevice
   *          The details of the capture device.
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Consumer Elements are unable to link this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the beginning of the Consumer is unable to ghost its sink pad then this Exception is thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If any of the Consumer Elements are null when their properties are set this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           Thrown if parameter captureDevice is null since it is necessary to creating a Consumer.
   * @throws UnableToCreateElementException
   *           Thrown if any of the Consumer Elements can't be created.
   * @throws NoProducerFoundException
   *           Thrown if the captureDevice.getName returns a ConsumerType that unrecognized.
   */
  private void createMontoringConsumer(CaptureDevice captureDevice, Properties properties) 
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException, 
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException, 
          UnableToCreateElementException, NoConsumerFoundException {

    
    if (!producerBin.captureDeviceProperties.isConfidence()) return;
    
    if (producerBin.isAudioDevice()) {
      consumerBins.add(ConsumerFactory.getInstance().getSink(ConsumerType.AUDIO_MONITORING_SINK, captureDevice, properties));
    } 
    if (producerBin.isVideoDevice()) {
      consumerBins.add(ConsumerFactory.getInstance().getSink(ConsumerType.VIDEO_MONITORING_SINK, captureDevice, properties));
    }
  }
  
  /**
   * Link the Producer to a tee and create new pads for the tee to connect it to each Consumer.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If linking between the Producer, Tee or Consumer fails then this Exception is thrown.
   * @throws UnableToCreateElementException
   *           If the tee or queue fails to be created this Exception is thrown,
   * **/
  private void linkProducerToConsumers() throws UnableToLinkGStreamerElementsException, UnableToCreateElementException {
    tee = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.TEE,
            null);
    bin.addMany(producerBin.getBin(), tee);
    linkProducerToTee();
    linkTeeToConsumers();
  }

  /**
   * Link the Producer to the Tee
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if the Producer fails to link to the Tee.
   * **/
  private void linkProducerToTee() throws UnableToLinkGStreamerElementsException {
    if (!Element.linkPads(producerBin.getBin(), ProducerBin.GHOST_PAD_NAME, tee, GStreamerProperties.SINK)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, producerBin.getBin(), tee);
    }
  }

  /**
   * Link the Tee to each of the Consumers
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if the Tee fails to link to one of the Consumers
   * @throws UnableToCreateElementException
   *           Thrown if the queue fails to be created between the tee and the Consumer
   * **/
  private void linkTeeToConsumers() throws UnableToLinkGStreamerElementsException, UnableToCreateElementException {
    for (ConsumerBin sinkBin : consumerBins) {
      addConsumerToBin(sinkBin);
      linkTeeToConsumer(sinkBin);
    }
  }

  /** Place the Consumer into the CaptureDeviceBin **/
  private void addConsumerToBin(ConsumerBin consumerBin) {
    bin.add(consumerBin.getBin());
  }

  /**
   * Link the tee to the Consumer
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if the Consumer fails to link to the tee.
   * **/
  private void linkTeeToConsumer(ConsumerBin consumerBin) throws UnableToLinkGStreamerElementsException,
          UnableToCreateElementException {
    // For each sink we need to request a new pad from the tee so that we can connect the two. The template for creating
    // a new pad is src%d and %d becomes the next int after the number of sinks already connected to the tee
    Pad newTeePad = tee.getRequestPad(GStreamerProperties.SRCTEMPLATE);
    Element queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    if (logger.isTraceEnabled()) {
      BufferThread t = new BufferThread(queue);
      t.run();
    }
    bin.add(queue);
    if (!Element.linkPads(tee, newTeePad.getName(), queue, GStreamerProperties.SINK)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, tee, queue);
    } else if (!Element.linkPads(queue, GStreamerProperties.SRC, consumerBin.getBin(), ConsumerBin.GHOST_PAD_NAME)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, consumerBin.getBin());
    }
  }

  /** Sends an EOS to each of its sources. **/
  public void shutdown() {
    if (producerBin != null) {
      producerBin.shutdown();
    }
  }

  /** Returns the Bin that contains the Producer and Consumers. **/
  public Bin getBin() {
    return bin;
  }
}
