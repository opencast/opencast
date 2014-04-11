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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static Gstreamer helpers.
 */
public final class GStreamerPipelineTools {

  private static final Logger logger = LoggerFactory.getLogger(GStreamerPipelineTools.class);

  /** Private default constructor is needed for utility classes. **/
  private GStreamerPipelineTools() { }

  /**
   * Double checks that the source location for capture is set, otherwise throws an exception.
   * @param name Friendly name of the capture device.
   * @param srcLoc The source location that should be set to something.
   * @throws CannotFindSourceFileOrDeviceException Thrown if the source location is null or the empty string.
   */
  public static void checkSrcLocationExists(String name, String srcLoc) throws CannotFindSourceFileOrDeviceException {
    if (srcLoc == null || ("").equals(srcLoc)) {
      throw new CannotFindSourceFileOrDeviceException("Unable to create pipeline for " + name
              + " because its source file/device does not exist!");
    }
  }

  /** This is legacy code that determines a Producer from the JV4L library. It really only supports the reference
   * capture devices. **/
  public static ProducerFactory.ProducerType determineSourceFromJ4VLInfo(String srcLoc)
          throws UnrecognizedDeviceException {
    // ALSA source
    if (srcLoc.contains("hw:")) {
      return ProducerFactory.ProducerType.ALSASRC;
    } else if ("dv1394".equals(srcLoc)) {
      return ProducerFactory.ProducerType.DV_1394;
    } else { // V4L devices
      // Attempt to determine what the device is using the JV4LInfo library
      try {
        V4LInfo v4linfo = JV4LInfo.getV4LInfo(srcLoc);
        String deviceString = v4linfo.toString();
        if (deviceString.contains("Hauppauge") || deviceString.contains("WinTV")) {
          return ProducerFactory.ProducerType.HAUPPAUGE_WINTV;
        } else if (deviceString.contains("BT878")) {
          return ProducerFactory.ProducerType.BLUECHERRY_PROVIDEO;
        } else {
          throw new UnrecognizedDeviceException("Do not recognized device: " + srcLoc);

        }
      } catch (JV4LInfoException e) {
        // The v4l device caused an exception
        throw new UnrecognizedDeviceException("Unexpected jv4linfo exception: " + e.getMessage() + " for " + srcLoc);
      }
    }
  }

  /** Creates a CaptureDevice used mostly for testing purposes from its important components.
   * @param srcLoc Where the capture device will capture from e.g. /dev/video0.
   * @param devName The ProducerType of the capture device such as V4L2SRC.
   * @param name The unique name of the capture device.
   * @param outputLoc The output name of the capture media.
   * @param properties Capture device properties
   **/
  public static CaptureDevice createCaptureDevice(String srcLoc, ProducerFactory.ProducerType devName, String name, String outputLoc, Properties properties) {
    CaptureDevice capdev = new CaptureDevice(srcLoc, devName, name, outputLoc);
    String codecProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_CODEC;
    String containerProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name
            + CaptureParameters.CAPTURE_DEVICE_CONTAINER;
    String bitrateProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_BITRATE;
    String quantizerProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_QUANTIZER;
    String bufferProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_BUFFER;
    String bufferCountProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BUFFERS;
    String bufferByteProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BYTES;
    String bufferTimeProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_TIME;
    String framerateProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name
            + CaptureParameters.CAPTURE_DEVICE_FRAMERATE;
    String codec = properties.getProperty(codecProperty);
    String container = properties.getProperty(containerProperty);
    String bitrate = properties.getProperty(bitrateProperty);
    String quantizer = properties.getProperty(quantizerProperty);
    String bufferCount = properties.getProperty(bufferCountProperty);
    String bufferBytes = properties.getProperty(bufferByteProperty);
    String bufferTime = properties.getProperty(bufferTimeProperty);
    String framerate = properties.getProperty(framerateProperty);

    if (codec != null)
      capdev.getProperties().setProperty("codec", codec);
    if (bitrate != null)
      capdev.getProperties().setProperty("bitrate", bitrate);
    if (quantizer != null)
      capdev.getProperties().setProperty("quantizer", quantizer);
    if (container != null)
      capdev.getProperties().setProperty("container", container);
    if (bufferCount != null)
      capdev.getProperties().setProperty("bufferCount", bufferCount);
    if (bufferBytes != null)
      capdev.getProperties().setProperty("bufferBytes", bufferBytes);
    if (bufferTime != null)
      capdev.getProperties().setProperty("bufferTime", bufferTime);
    if (framerate != null)
      capdev.getProperties().setProperty("framerate", framerate);

    return capdev;
  }

  private static String createDevice(String outputDirectory,
          boolean confidenceOnly, ArrayList<CaptureDevice> devices, String name, Properties properties)

          throws InvalidDeviceNameException, UnableToCreateSampleOutputFileException,
          UnrecognizedDeviceException, CannotFindSourceFileOrDeviceException {

    name = name.trim();
    ProducerFactory.ProducerType devName;

    // Get properties from the configuration
    String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_SOURCE;
    String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_DEST;
    String typeProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_TYPE;

    if (outputDirectory == null && !confidenceOnly) {
      logger.warn("Output directory is null, this may not work because we may not be able to write to the current "
              + "output dir!");
    }
    if (!properties.containsKey(outputProperty) && !confidenceOnly) {
      throw new InvalidDeviceNameException("Invalid device name: " + name + ".  No keys named "
              + CaptureParameters.CAPTURE_DEVICE_PREFIX + name + " exist in the properties!");
    }
    String srcLoc = properties.getProperty(srcProperty);
    File outputFile = null;
    if (outputDirectory != null) {
      outputFile = new File(outputDirectory, properties.getProperty(outputProperty));
    }

    logger.debug("Device {} has source at {}.", name, srcLoc);
    if (outputFile != null)
      logger.debug("Device {} has output at {}.", name, outputFile);
    else
      logger.debug("Device {} has no output, because running monitoring only.", name);

    String type = properties.getProperty(typeProperty);
    logger.debug("Device {} has type {}.", name, type);

    String outputLoc = null;
    // Only try and create an output file if this pipeline will *not* be used for confidence monitoring
    if (!confidenceOnly && outputFile != null) {
      try {
        if (!outputFile.createNewFile()) {
          throw new UnableToCreateSampleOutputFileException("Could not create ouput file for "
                  + outputFile.getAbsolutePath()
                  + " because we cannot create a new file possibly because it already exists.");
        }
        outputLoc = outputFile.getAbsolutePath();
      } catch (IOException e) {
        throw new UnableToCreateSampleOutputFileException("An error occured while creating output file for " + name
                + ". " + e.getMessage());
      }
    }

    if (type != null) {
      devName = ProducerFactory.ProducerType.valueOf(type);
      logger.debug("Device {} has been confirmed to be type {}", name, devName.toString());
      /** For certain devices we need to check to make sure that the src is specified, others are exempt. **/
      if (ProducerFactory.getInstance().requiresSrc(devName)) {
        checkSrcLocationExists(name, srcLoc);
      }
    } else {
      logger.debug("Device {} has no type so we will determine it's type.", name);
      checkSrcLocationExists(name, srcLoc);
      if (new File(srcLoc).isFile()) {
        // Non-V4L file. If it exists, assume it is ingestable
        // TODO: Fix security risk. Any file on CaptureAgent filesytem could be ingested
        devName = ProducerFactory.ProducerType.FILE;
        logger.debug("Device {} is a File device.", name);
      } else {
        devName = determineSourceFromJ4VLInfo(srcLoc);
      }
    }
    // devices will store the CaptureDevice list arbitrary order
    CaptureDevice capdev = createCaptureDevice(srcLoc, devName, name, outputLoc, properties);
    if (!devices.add(capdev)) {
      logger.error("Unable to add device: {}.", capdev);
    }
    return name;
  }

  /**
   * Splits the device names from the pipeline's properties.
   *
   * @return The device names to capture from.
   * @throws InvalidCaptureDevicesSpecifiedException
   *           - If there are no capture devices specified in the configuration file we throw an exception.
   */
  public static String[] getDeviceNames(Properties props) throws InvalidCaptureDevicesSpecifiedException {
    // Setup pipeline for all the devices specified
    String deviceNames = props.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (deviceNames == null) {
      throw new InvalidCaptureDevicesSpecifiedException("No capture devices specified in "
              + CaptureParameters.CAPTURE_DEVICE_NAMES);
    }

    // Sanity checks for the device list
    String[] friendlyNames = deviceNames.split(",");
    if (friendlyNames.length < 1) {
      throw new InvalidCaptureDevicesSpecifiedException("Insufficient number of capture devices listed.  Aborting!");
    } else if (friendlyNames.length == 1) {
      // Java gives us an array even if the string being split is blank...
      if (StringUtils.isBlank(friendlyNames[0])) {
        throw new InvalidCaptureDevicesSpecifiedException("Invalid capture device listed in \"" + deviceNames + "\".  Aborting!");
      }
    }

    return friendlyNames;
  }

  /**
   * Returns an {@code ArrayList} of {@code CaptureDevice}s which contain everything the rest of this class needs to
   * start the pipeline
   *
   * @param friendlyNames
   *          The list of friendly names we will be capturing from.
   * @param outputDirectory
   *          The destination directory of the captures.
   * @param confidenceOnly
   *          True to enable confidence monitoring, false otherwise.
   * @return A list of {@code CaptureDevice}s which can be captured from.
   */
  protected static ArrayList<CaptureDevice> initDevices(String[] friendlyNames, String outputDirectory,
          boolean confidenceOnly, Properties properties) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();
    for (String name : friendlyNames) {
      String deviceName = null;
      try {
        deviceName = createDevice(outputDirectory, confidenceOnly, devices, name, properties);
      } catch (CannotFindSourceFileOrDeviceException e) {
        logger.error("Device " + name + " can't find source file or device: ", e);
      } catch (InvalidDeviceNameException e) {
        logger.error("Device " + name + " has invalid device name: " + deviceName, e);
      } catch (UnableToCreateSampleOutputFileException e) {
        logger.error("Device " + name + " unable to create sample output file " + outputDirectory, e);
      } catch (UnrecognizedDeviceException e) {
        logger.error("Device " + name + " is an unrecognized device ", e);
      }
    }

    return devices;
  }
}
