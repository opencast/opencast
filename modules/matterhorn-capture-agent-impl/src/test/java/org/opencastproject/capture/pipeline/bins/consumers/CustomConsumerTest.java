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

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.gstreamer.Element;
import org.gstreamer.Gst;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


public class CustomConsumerTest {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CustomConsumer.class);
  /** Boolean to set to false if gstreamer is not around. **/
  private static boolean gstreamerInstalled = true;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled  = false;
    }
  }

  @Test
  public void testCustomConsumer() throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (!gstreamerInstalled)
      return;
    String customConsumerString = "queue ! fakesink";

    CaptureDevice captureDevice = new CaptureDevice("/dev/video1", ProducerType.V4LSRC, "vga", "/tmp/testout.mpg");
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER, customConsumerString);

    captureDevice.setProperties(properties);
    CustomConsumer customConsumer = new CustomConsumer(captureDevice, properties);
    Assert.assertEquals(2, customConsumer.getBin().getElements().size());
    Element fakesrc = GStreamerElementFactory.getInstance().createElement("CustomConsumerTest", "fakesrc", "testFakesrc");
    fakesrc.link(customConsumer.getSrc());
  }

  @Test
  public void testCaptureDevicePropertiesSubstitution() throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (!gstreamerInstalled)
      return;
    // Setup strings to test replacement against.
    String location = "/location/";
    String friendlyName = "friendlyName";
    ProducerType type = ProducerType.FILE;
    String outputLocation = "/output/location.mpg";
    String customConsumerString = "queue ! filesink location=${" + CustomConsumer.LOCATION + "}/${"
            + CustomConsumer.FRIENDLY_NAME + "}/${" + CustomConsumer.TYPE + "}/${" + CustomConsumer.OUTPUT_PATH + "}/myCaptureDevice.mpg";
    // The expected string after substitution.
    String expected = "queue ! filesink location=" + location + "/" + friendlyName + "/" + type.toString() + "/" + outputLocation + "/myCaptureDevice.mpg";
    CaptureDevice captureDevice = new CaptureDevice(location, type, friendlyName, outputLocation);
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER, customConsumerString);
    captureDevice.setProperties(properties);
    CustomConsumer customConsumer = new CustomConsumer(captureDevice, properties);
    // Check to make sure the replacement string matches the substitution.
    String replacementString = customConsumer.getAllCustomStringSubstitutions().replacePropertiesInCustomString(customConsumerString);
    Assert.assertTrue("CustomConsumer is not replacing CaptureDevice properties's correctly. \""
            + replacementString + "\" should be \"" + expected + "\"", replacementString.equalsIgnoreCase(expected));
  }

  @Test
  public void testConfigurationManagerPropertiesSubstitution() throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (!gstreamerInstalled)
      return;
    // Setup strings to test replacement against.
    String location = "/location/";
    String friendlyName = "friendlyName";
    ProducerType type = ProducerType.FILE;
    String outputLocation = "/output/location.mpg";
    String id = "thisID";
    String path = "/path/to/nowhere";
    String customConsumerString = "queue ! filesink location=${"
            + CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL + "}/${" + CaptureParameters.RECORDING_ID
            + "}/myCaptureDevice.mpg";
    // The expected string after substitution.
    String expected = "queue ! filesink location=" + path + "/" + id + "/myCaptureDevice.mpg";

    CaptureDevice captureDevice = new CaptureDevice(location, type, friendlyName, outputLocation);
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER, customConsumerString);
    properties.setProperty(CaptureParameters.RECORDING_ID, id);
    properties.setProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, path);
    captureDevice.setProperties(properties);
    CustomConsumer customConsumer = new CustomConsumer(captureDevice, properties);
    // Check to make sure the replacement string matches the substitution.
    String replacementString =  customConsumer.getAllCustomStringSubstitutions().replacePropertiesInCustomString(customConsumerString);
    Assert.assertTrue("CustomConsumer is not replacing ConfigurationManagerProperties correctly. \""
            + replacementString + "\" should be \"" + expected + "\"", replacementString.equalsIgnoreCase(expected));
  }

  @Test
  public void customConsumerStringHandlesBadProperties() throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException, UnableToCreateElementException {
    if (!gstreamerInstalled)
      return;
    // Looking to make sure that it handles bad properties such as empty ${} missing braces ${property and $property} or
    // missing dollar sign {property}
    String badCustomConsumerString = "queue ! filesink location=${}";
    String result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=${location";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=$location}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location={location}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location={}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=$";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location={";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=${fakeProperty}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "queue ! filesink location=${${" + CustomConsumer.LOCATION + "}}";
    result = testBadString(badCustomConsumerString);
    Assert.assertTrue(badCustomConsumerString.equalsIgnoreCase(result));

    badCustomConsumerString = "";
    try {
      result = testBadString(badCustomConsumerString);
      Assert.fail();
    } catch (UnableToCreateElementException exception) {
    }
  }

  private String testBadString(String badCustomConsumerString) throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException, UnableToCreateElementException {
    String location = "/location/";
    String friendlyName = "friendlyName";
    ProducerType type = ProducerType.FILE;
    String outputLocation = "/output/location.mpg";
    CaptureDevice captureDevice = new CaptureDevice(location, type, friendlyName, outputLocation);
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
            + CaptureParameters.CAPTURE_DEVICE_CUSTOM_CONSUMER, badCustomConsumerString);
    captureDevice.setProperties(properties);
    CustomConsumer customConsumer = new CustomConsumer(captureDevice, properties);
    return customConsumer.getAllCustomStringSubstitutions().replacePropertiesInCustomString(badCustomConsumerString);
  }
}
