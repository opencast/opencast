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
import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;
import org.opencastproject.util.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Abstract class for Epiphan producer bins testing. (De)Initialize a JUnit test environment.
 */
public abstract class EpiphanVGA2USBV4LTest {

  private static final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LTest.class);

  /** True to run the tests */
  protected static boolean gstreamerInstalled = true;

  /** Location of Epiphan VGA2USB device */
  protected static String epiphanLocation = null;

  /** Properties specifically designed for unit testing */
  protected Properties properties;

  /** Capture Device for unit testing */
  protected CaptureDevice captureDevice;

  /** Capture Device Properties created for unit testing **/
  protected Properties captureDeviceProperties;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;

    if (System.getProperty("testEpiphan") != null) {
      String epiphanDeviceLocation = System.getProperty("testEpiphan");
      if (new File(epiphanDeviceLocation).exists()) {
        epiphanLocation = epiphanDeviceLocation;
        logger.info("Testing Epiphan card at: " + epiphanDeviceLocation);
      } else {
        logger.error("File does not exist: " + epiphanDeviceLocation);
        return;
      }
    } else {
      logger.error("'testEpiphan' property does not set! Make sure this property is set to Epiphan VGA2USB location.");
      return;
    }

    File tmpDir = new File("./target", "testpipe");
    if (!tmpDir.exists())
      tmpDir.mkdir();

    captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice, null, null, null, null,
            null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice(epiphanLocation,
            ProducerType.EPIPHAN_VGA2USB, "Epiphan VGA 2 USB", 
            new File(tmpDir, "test.mpg").getAbsolutePath(), captureDeviceProperties);

    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION,
            new File(tmpDir, "confidence").getAbsolutePath());
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG, "false");
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    properties = null;
    captureDevice = null;
    FileUtils.deleteQuietly(new File("./target", "testpipe"));
  }

  /**
   * Returns true, if gstreamer installed and epiphan device location is set
   * 
   * @return true, if tests can be started
   */
  protected boolean readyTestEnvironment() {
    return gstreamerInstalled && epiphanLocation != null;
  }

  /**
   * Creates EpiphanVGA2USBV4LProducer.
   * 
   * @param captureDevice
   *          CaptureDevice for testing Epiphan VGA2USB device
   * @param properties
   *          Properties for unit testing
   * @return EpiphanVGA2USBV4LProducer if captureDevice not null and properties are set
   * 
   * @throws UnableToLinkGStreamerElementsException
   * @throws UnableToCreateGhostPadsForBinException
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   * @throws CaptureDeviceNullPointerException
   * @throws UnableToCreateElementException
   * @throws NoProducerFoundException
   */
  protected static EpiphanVGA2USBV4LProducer getEpiphanVGA2USBV4LProducer(CaptureDevice captureDevice,
          Properties properties) throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException, NoProducerFoundException {

    ProducerBin epiphanBin = ProducerFactory.getInstance().getProducer(captureDevice, properties);
    if (epiphanBin instanceof EpiphanVGA2USBV4LProducer)
      return (EpiphanVGA2USBV4LProducer) epiphanBin;
    else {
      throw new NoProducerFoundException("Created ProducerBin is not a EpiphanVGA2USBV4LProducer");
    }
  }
}
