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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import java.util.Properties;

/**
 * PipelineTestHelpers is a collection of handy helper functions for setting up tests that are common to many of the
 * tests.
 */
public final class PipelineTestHelpers {

  /** TODO Remove this in favor of test parameters. **/
  public static final String V4L_LOCATION = "/dev/vga";
  public static final String V4L2_LOCATION = "/dev/video2";
  public static final String HAUPPAGE_LOCATION = "/dev/video0";

  /** The cached name of the operating system. **/
  private static String operatingSystemName = null;

  private PipelineTestHelpers() {
    
  }
  
  /** Returns a string of the operating system. **/
  public static String getOsName() {
    if (operatingSystemName == null) {
      operatingSystemName = System.getProperty("os.name");
    }
    return operatingSystemName;
  }

  /** Returns true if the platform is Windows. **/
  public static boolean isWindows() {
    return getOsName().startsWith("Windows");
  }

  /** Returns true if the platform is Linux. **/
  public static boolean isLinux() {
    return getOsName().startsWith("Linux");
  }

  /**
   * Creates a new Properties file with all of the properties required to adequately test Bins.
   * 
   * @param captureDevice
   *          The capture device specific properties such as location of the source to capture from, the friendly name
   *          of the capture device etc.
   * @param customProducer
   *          The optional custom pipeline that specifies a device to capture from.
   * @param codec
   *          The codec to encode the output media to.
   * @param bitrate
   *          The bitrate to capture from the source.
   * @param quantizer
   *          If the codec is x264, what level to set the quantizer to.
   * @param container
   *          The container to dump the codec to.
   * @param bufferCount
   *          The number of buffers that the queue will have.
   * @param bufferBytes
   *          The number of bytes that each queue will have.
   * @param bufferTime
   *          The maxiumum amount of time that the media can be inside the queue.
   * @param framerate
   *          The framerate of the video.
   * @return A Properties file with all of the properties needed to test the agent.
   */
  public static Properties createCaptureDeviceProperties(CaptureDevice captureDevice, String customProducer,
          String codec, String bitrate, String quantizer, String container, String framerate) {
    Properties properties = new Properties();
    if (customProducer != null)
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
              + CaptureParameters.CAPTURE_DEVICE_CUSTOM_PRODUCER, customProducer);
    if (codec != null)
      properties.setProperty("codec", codec);
    if (bitrate != null)
      properties.setProperty("bitrate", bitrate);
    if (quantizer != null)
      properties.setProperty("quantizer", quantizer);
    if (container != null)
      properties.setProperty("container", container);
    if (framerate != null)
      properties.setProperty("framerate", framerate);
    return properties;
  }

  public static Properties createQueueProperties(String bufferCount, String bufferBytes, String bufferTime) {
    Properties properties = new Properties();
    if (bufferCount != null)
      properties.setProperty("bufferCount", bufferCount);
    if (bufferBytes != null)
      properties.setProperty("bufferBytes", bufferBytes);
    if (bufferTime != null)
      properties.setProperty("bufferTime", bufferTime);
    return properties;
  }

  /**
   * Creates a CaptureDevice object along with its properties.
   * 
   * @param sourceLocation
   * @param sourceDeviceName
   * @param friendlyName
   * @param outputLocation
   * @param captureDeviceProperties
   * @return
   */
  public static CaptureDevice createCaptureDevice(String sourceLocation, ProducerType sourceDeviceName,
          String friendlyName, String outputLocation, Properties captureDeviceProperties) {
    CaptureDevice captureDevice = new CaptureDevice(sourceLocation, sourceDeviceName, friendlyName, outputLocation);
    captureDevice.setProperties(captureDeviceProperties);
    return captureDevice;
  }

  public static Properties createConfidenceMonitoringProperties() {
    // setup testing properties
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    return properties;
  }

  public static boolean testGstreamerElement(String element) {
    try {
      GStreamerElementFactory.getInstance().createElement("Element Creation Test", element, null);
      return true;
    } catch (UnableToCreateElementException e) {
      return false;
    } 
  }
  
}
