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
import org.opencastproject.util.XProperties;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.GstException;

import java.util.Properties;

/**
 * CustomConsumer is for the gstreamer knowledgable user who wants to use their own consumer using gstreamer CLI style
 * syntax. It also provides the user with the ability to user the properties found in the Capture Agent's
 * ConfigurationManager such as the location that the captures are normally stored using ${propertyName} syntax.
 **/
public class CustomConsumer extends ConsumerBin {
  /** Friendly is the name given by the user for the capture device. **/
  public static final String FRIENDLY_NAME = "friendlyName";
  /** Friendly is the name given by the user for the capture device. **/
  public static final String OUTPUT_PATH = "outputPath";
  /** The location of the device e.g. /dev/video0 **/
  public static final String LOCATION = "location";
  /**
   * The String representation of the ProducerType so that we can identify the device as a v4lsrc, v4l2src, filesrc etc.
   **/
  public static final String TYPE = "type";

  /**
   * Determines whether to create ghostpads for the bin or just leave the pads as they are. This is actually a constant
   * and must be left as true for this class so that the created Bin from Bin.launch will be able to be connected to its
   * related Producer in CaptureDeviceBin.
   **/
  private static final boolean LINK_UNUSED_GHOST_PADS = true;

  /**
   * CustomConsumer allows the user to specify at run time a custom gstreamer pipeline that will act as the consumer for
   * the Capture Device.
   *
   * @throws UnableToLinkGStreamerElementsException
   *           Not actually thrown by this class, just inherited.
   * @throws UnableToCreateGhostPadsForBinException
   *           Not actually thrown by this class, just inherited.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Not actually thrown by this class, just inherited.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is required to create this custom Producer so an Exception is thrown when it
   *           is null.
   * @throws UnableToCreateElementException
   *           Thrown if the pipeline specified to be used in customConsumer string in the properties file is null or
   *           causes a gstreamer exception.
   **/
  public CustomConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Adds the friendly name, output path, location of the capture device and type to the properties collection.
   *
   * @return Returns an XProperties that contains all of the XProperties from ConfigurationManager and the mentioned
   *         properties above.
   **/
  public XProperties getAllCustomStringSubstitutions() {
    XProperties allProperties = new XProperties();
    allProperties.putAll(properties);
    allProperties.put(FRIENDLY_NAME, captureDevice.getFriendlyName());
    allProperties.put(OUTPUT_PATH, captureDevice.getOutputPath());
    allProperties.put(LOCATION, captureDevice.getLocation());
    allProperties.put(TYPE, captureDevice.getName());
    return allProperties;
  }

  @Override
  public Element getSrc() {
    return getBin();
  }

  /**
   * Creates the Bin for this class using the GStreamer Java Bin.launch command. Users can specify an Consumer in this
   * way using a gst-launch like syntax (e.g. "fakesrc ! fakesink")
   *
   * @throws UnableToCreateElementException
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    String customConsumer = getAllCustomStringSubstitutions().replacePropertiesInCustomString(
            captureDeviceProperties.getCustomConsumer());
    logger.info("Custom Consumer is going to use Pipeline: \"" + customConsumer + "\"");
    if (captureDeviceProperties.getCustomConsumer() == null) {
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), "Custom Consumer because it was null.");
    }
    try {
      bin = Bin.launch(customConsumer, LINK_UNUSED_GHOST_PADS);
    } catch (GstException exception) {
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), "Custom Consumer had exception "
              + exception.getMessage());
    }
  }

  /**
   * Need an empty method for createGhostPads because the Bin.launch will create the ghost pads all on its own so we
   * don't have to manually do it.
   **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
  }
}
