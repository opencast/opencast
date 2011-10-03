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

import org.gstreamer.Bin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public abstract class PartialBin {
  protected Bin bin = new Bin();
  protected static final Logger logger = LoggerFactory.getLogger(PartialBin.class);
  protected CaptureDevice captureDevice;
  protected Properties properties;
  protected CaptureDeviceProperties captureDeviceProperties;
  protected ConfidenceMonitoringProperties confidenceMonitoringProperties;

  /**
   * PartialBin is an abstract class used to consolidate common attributes to both Producers and Consumers and the
   * pattern for their Element creation
   * 
   * @param captureDevice
   *          The properties of the capture device such as location, output file location, codec, confidence monitoring
   *          settings etc.
   * @param properties
   *          The confidence monitoring settings.
   * @throws UnableToLinkGStreamerElementsException
   *           If any GStreamer Elements fail to be linked together this is the Exception thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If an Element that has been designated to have its pads ghosted for the entire bin fails, this is the
   *           Exception thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If an element is null when trying to set some of its properties this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The parameter captureDevice is required to create any Consumer or Producer so an Exception is thrown when
   *           it is null.
   * @throws UnableToCreateElementException
   *           If a GStreamer Element fails to be created this Exception is thrown.
   **/
  public PartialBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (captureDevice != null) {
      this.captureDevice = captureDevice;
    } else {
      throw new CaptureDeviceNullPointerException();
    }
    this.properties = properties;
    getCaptureProperties();
    createElements();
    setElementProperties();
    addElementsToBin();
    createGhostPads();
    linkElements();
  }

  /**
   * Renders the capture device properties into usable data objects.
   **/
  private void getCaptureProperties() {
    captureDeviceProperties = new CaptureDeviceProperties(captureDevice, properties);
    confidenceMonitoringProperties = new ConfidenceMonitoringProperties(captureDevice, properties);
  }

  /**
   * Used by subclasses to create any additional GStreamer Elements they might need. This function is implemented as a
   * blank one here so that they don't have to use this necessarily. They should throw UnableToCreateElementException if
   * they are unable to create the GStreamer Elements.
   **/
  protected void createElements() throws UnableToCreateElementException {
  }

  /**
   * Used by subclasses to set the properties of their GStreamer Elements. It is blank so that subclasses don't have to
   * implement it necessarily. Subclases should throw UnableToSetElementPropertyBecauseElementWasNullException if the
   * Element is null when trying to set a property.
   **/
  protected void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
  }

  /**
   * Descendants will use this to add all the elements they create to the Bin.
   **/
  protected void addElementsToBin() {

  }

  /**
   * Descendants will use this to create ghost pads for their bins so that they can be connected togther without knowing
   * the implementation details.
   **/
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {

  }

  /**
   * Descendants will implement this to link all of their elements together in the proper order.
   **/
  protected void linkElements() throws UnableToLinkGStreamerElementsException {

  }

  /** Returns the GStreamer Bin containing all of the Elements. **/
  public Bin getBin() {
    return bin;
  }

  /** Should be set to true if a Producer generates audio data so that an audio Consumer can be attached **/
  public boolean isAudioDevice() {
    return false;
  }

  /** Should be set to true if a Producer generates video data so that an video Consumer can be attached **/
  public boolean isVideoDevice() {
    return false;
  }
}
