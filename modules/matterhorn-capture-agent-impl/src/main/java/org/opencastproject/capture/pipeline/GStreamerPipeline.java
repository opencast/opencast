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
import org.opencastproject.capture.impl.CaptureFailureHandler;
import org.opencastproject.capture.impl.RecordingImpl;
import org.opencastproject.capture.impl.UnableToStartCaptureException;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBin;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.apache.commons.lang.StringUtils;
import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public final class GStreamerPipeline {

  private static final int WAIT_FOR_NULL_SLEEP_TIME = 1000;
  private static final Logger logger = LoggerFactory.getLogger(GStreamerPipeline.class);
  // Capture properties. 
  private static Properties properties;
  // List of captureDeviceBins inside the pipeline so that we can send each an EOS. 
  private ArrayList<CaptureDeviceBin> captureDeviceBins;
  // Pipeline used to capture. 
  private Pipeline pipeline;
  // Used to stop the capture above if something goes wrong at this level. 
  private CaptureFailureHandler captureFailureHandler;
  /** The amount of time to wait until shutting down the pipeline forcefully.**/ 
  public static final long DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT = 60000L;
  /**
   * The number of nanoseconds in a second. This is a borrowed constant from gStreamer and is used in the pipeline
   * initialisation routines
   */
  public static final long GST_SECOND = 1000000000L;
  
  public GStreamerPipeline(CaptureFailureHandler captureFailureHandler) {
   captureDeviceBins = new ArrayList<CaptureDeviceBin>(); 
   this.captureFailureHandler = captureFailureHandler;
  }
  
  /**
   * Creates the gStreamer pipeline and blocks until it starts successfully
   * 
   * @param newRec
   *          The RecordingImpl of the capture we wish to perform.
   * @return The recording ID (equal to newRec.getID()) or null in the case of an error
   */
  public void start(RecordingImpl newRec) {
    // Create the pipeline
    try {
      pipeline = create(newRec.getProperties(), false);
    } catch (UnsatisfiedLinkError e) {
      throw new UnableToStartCaptureException(e.getMessage() + " : please add libjv4linfo.so to /usr/lib to correct this issue.");
    }

    // Check if the pipeline came up ok
    if (pipeline == null) {
      //logger.error("Capture {} could not start, pipeline was null!", newRec.getID());
      captureFailureHandler.resetOnFailure(newRec.getID());
      throw new UnableToStartCaptureException("Capture " +  newRec.getID() + " could not start, pipeline was null!");
    }

    logger.info("Initializing devices for capture.");

    hookUpBus();

    // Grab time to wait for pipeline to start
    int wait;
    String waitProp = newRec.getProperty(CaptureParameters.CAPTURE_START_WAIT);
    if (waitProp != null) {
      wait = Integer.parseInt(waitProp);
    } else {
      wait = 15; // Default taken from gstreamer docs
    }

    pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, pipeline.getName());
    // Try and start the pipeline
    pipeline.play();
    if (pipeline.getState(wait * GStreamerPipeline.GST_SECOND) != State.PLAYING) {
      // In case of an error call stop to clean up the pipeline.  
      logger.debug("Pipeline was unable to start after " + wait + " seconds.");
      stop(GStreamerPipeline.DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT);
      throw new UnableToStartCaptureException("Unable to start pipeline after " + wait + " seconds.  Aborting!");
    }
    logger.info("{} started.", pipeline.getName());
  }

  private void hookUpBus() {
    logger.debug("Starting to hookup GStreamer Pipeline bus. ");
    // Hook up the shutdown handlers
    Bus bus = pipeline.getBus();
    bus.connect(new Bus.EOS() {
      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.EOS#endOfStream(org.gstreamer.GstObject)
       */
      public void endOfStream(GstObject arg0) {
        logger.debug("Pipeline received EOS.");
        pipeline.setState(State.NULL);
        pipeline = null;
      }
    });
    bus.connect(new Bus.ERROR() {
      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.ERROR#errorMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      public void errorMessage(GstObject obj, int retCode, String msg) {
        logger.warn("{}: {}", obj.getName(), msg);
      }
    });
    bus.connect(new Bus.WARNING() {
      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.WARNING#warningMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      public void warningMessage(GstObject obj, int retCode, String msg) {
        logger.warn("{}: {}", obj.getName(), msg);
      }
    });
    logger.debug("Successfully hooked up GStreamer Pipeline bus to Log4J.");
  }
  
  /**
   * This method waits until the pipeline has had an opportunity to shutdown and if it surpasses the maximum timeout
   * value it will be manually stopped.
   */
  public void stop(long timeout) {
    // We must stop the capture as soon as possible, then check whatever needed
    for (CaptureDeviceBin captureDeviceBin : captureDeviceBins) {
      captureDeviceBin.shutdown();
    }
    
    long startWait = System.currentTimeMillis();
   
   
    while (pipeline != null && (pipeline.getState() != State.PAUSED || pipeline.getState() != State.NULL)) {
      try {
        Thread.sleep(WAIT_FOR_NULL_SLEEP_TIME);
      } catch (InterruptedException e) {
      }
      // If we've timed out then force kill the pipeline
      if (System.currentTimeMillis() - startWait >= timeout) {
        if (pipeline != null) {
          logger.debug("The pipeline took too long to shut down, now sending State.NULL.");
          pipeline.setState(State.NULL);
        }
        pipeline = null;
      }
    }
    
    if (pipeline != null) {
      pipeline.setState(State.NULL);
    }
    pipeline = null;
  }
  
  public boolean isPipelineNull() {
    return pipeline == null;
  }

  /**
   * Create a bin that contains multiple pipelines using each source in the properties object as the gstreamer source
   * 
   * @param props
   *          {@code Properties} object defining sources
   * @return The {@code Pipeline} to control the pipelines
   * @throws Exception
   * @throws UnsupportedDeviceException
   */
  public Pipeline create(Properties props, boolean confidence) {
    properties = props;
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    String[] friendlyNames;
    try {
      friendlyNames = getDeviceNames(props);
    } catch (InvalidCaptureDevicesSpecifiedException e) {
      logger.error(e.getStackTrace().toString());
      return null;
    }

    String outputDirectory = properties.getProperty(CaptureParameters.RECORDING_ROOT_URL);

    devices = initDevices(friendlyNames, outputDirectory, confidence);
    if (devices == null) {
      // This odd case will be logged why in initDevices.
      return null;
    }

    return startPipeline(devices, confidence);
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
        throw new InvalidCaptureDevicesSpecifiedException("Invalid capture device listed.  Aborting!");
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
   * @param confidence
   *          True to enable confidence monitoring, false otherwise.
   * @return A list of {@code CaptureDevice}s which can be captured from.
   * @throws InvalidDeviceNameException
   *           The device name specified in the list of devices could not be found in the properties list.
   * @throws UnrecognizedDeviceException
   *           JV4L could fail top recognise the device after the type was not specified.
   * @throws UnableToCreateSampleOutputFileException
   *           Failure while trying to create a test capture file.
   */
  protected static ArrayList<CaptureDevice> initDevices(String[] friendlyNames, String outputDirectory,
          boolean confidence) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();
    for (String name : friendlyNames) {
      String deviceName = null;
      try {
        deviceName = createDevice(outputDirectory, confidence, devices, name);
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

  private static String createDevice(String outputDirectory, boolean confidence, ArrayList<CaptureDevice> devices,
          String name) throws InvalidDeviceNameException, UnableToCreateSampleOutputFileException,
          UnrecognizedDeviceException, CannotFindSourceFileOrDeviceException {
    name = name.trim();
    ProducerType devName;

    // Get properties from the configuration
    String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_SOURCE;
    String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_DEST;
    String typeProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_TYPE;

    if (outputDirectory == null && !confidence) {
      logger.warn("Output directory is null, this may not work because we may not be able to write to the current "
              + "output dir!");
    }
    if (!properties.containsKey(outputProperty)) {
      throw new InvalidDeviceNameException("Invalid device name: " + name + ".  No keys named "
              + CaptureParameters.CAPTURE_DEVICE_PREFIX + name + " exist in the properties!");
    }
    String srcLoc = properties.getProperty(srcProperty);
    File outputFile = new File(outputDirectory, properties.getProperty(outputProperty));

    logger.debug("Device {} has source at {}.", name, srcLoc);
    logger.debug("Device {} has output at {}.", name, outputFile);

    String type = properties.getProperty(typeProperty);
    logger.debug("Device {} has type {}.", name, type);

    // Only try and create an output file if this pipeline will *not* be used for confidence monitoring
    if (!confidence) {
      try {
        if (!outputFile.createNewFile()) {
          throw new UnableToCreateSampleOutputFileException("Could not create ouput file for " 
                  + outputFile.getAbsolutePath()
                  + " because we cannot create a new file possibly because it already exists.");
        }
      } catch (IOException e) {
        throw new UnableToCreateSampleOutputFileException("An error occured while creating output file for " + name
                + ". " + e.getMessage());
      }
    }
    String outputLoc = outputFile.getAbsolutePath();

    

    if (type != null) {
      devName = ProducerType.valueOf(type);
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
        devName = ProducerType.FILE;
        logger.debug("Device {} is a File device.", name);
      } else {
        devName = determineSourceFromJ4VLInfo(srcLoc);
      }
    }
    // devices will store the CaptureDevice list arbitrary order
    CaptureDevice capdev = createCaptureDevice(srcLoc, devName, name, outputLoc);
    if (!devices.add(capdev)) {
      logger.error("Unable to add device: {}.", capdev);
    }
    return name;
  }

  
  /**
   * Double checks that the source location for capture is set, otherwise throws an exception. 
   * @param name Friendly name of the capture device. 
   * @param srcLoc The source location that should be set to something. 
   * @throws CannotFindSourceFileOrDeviceException Thrown if the source location is null or the empty string. 
   */
  static void checkSrcLocationExists(String name, String srcLoc) throws CannotFindSourceFileOrDeviceException {
    if (srcLoc == null || ("").equals(srcLoc)) {
      throw new CannotFindSourceFileOrDeviceException("Unable to create pipeline for " + name
              + " because its source file/device does not exist!");
    }
  }

  /** This is legacy code that determines a Producer from the JV4L library. It really only supports the reference
   * capture devices. **/
  static ProducerType determineSourceFromJ4VLInfo(String srcLoc)
          throws UnrecognizedDeviceException {
    // ALSA source
    if (srcLoc.contains("hw:")) {
      return ProducerType.ALSASRC;
    } else if ("dv1394".equals(srcLoc)) {
      return ProducerType.DV_1394;
    } else { // V4L devices
      // Attempt to determine what the device is using the JV4LInfo library
      try {
        V4LInfo v4linfo = JV4LInfo.getV4LInfo(srcLoc);
        String deviceString = v4linfo.toString();
        if (deviceString.contains("Hauppauge") || deviceString.contains("WinTV")) {
          return ProducerType.HAUPPAUGE_WINTV;
        } else if (deviceString.contains("BT878")) {
          return ProducerType.BLUECHERRY_PROVIDEO;
        } else {
          throw new UnrecognizedDeviceException("Do not recognized device: " + srcLoc);

        }
      } catch (JV4LInfoException e) {
        // The v4l device caused an exception
        throw new UnrecognizedDeviceException("Unexpected jv4linfo exception: " + e.getMessage() + " for " + srcLoc);
      }
    }
  }

  /**
   * Initializes the pipeline itself, but does not start capturing
   * 
   * @param devices
   *          The list of devices to capture from.
   * @param confidence
   *          True to enable confidence monitoring.
   * @return The created {@code Pipeline}, or null in the case of an error.
   */
  private Pipeline startPipeline(ArrayList<CaptureDevice> devices, boolean confidence) {
    logger.info("Successfully initialised {} devices.", devices.size());
    for (int i = 0; i < devices.size(); i++)
      logger.debug("Device #{}: {}.", i, devices.get(i));

    // setup gstreamer pipeline using capture devices
    Gst.init(); // cannot using gst library without first initialising it

    Pipeline pipeline = new Pipeline();
    for (CaptureDevice c : devices) {
      if (!addCaptureDeviceBinsToPipeline(c, pipeline))
        logger.error("Failed to create pipeline for {}.", c);
    }

    pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, pipeline.getName());
    return pipeline;
  }

  /** Creates a CaptureDevice used mostly for testing purposes from its important components. 
   * @param srcLoc Where the capture device will capture from e.g. /dev/video0.
   * @param devName The ProducerType of the capture device such as V4L2SRC.
   * @param name The unique name of the capture device.
   * @param outputLoc The output name of the capture media. **/
  public static CaptureDevice createCaptureDevice(String srcLoc, ProducerType devName, String name, String outputLoc) {
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

  /**
   * addPipeline will add a pipeline for the specified capture device to the bin.
   * 
   * @param captureDevice
   *          {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  protected boolean addCaptureDeviceBinsToPipeline(CaptureDevice captureDevice, Pipeline pipeline) {

    CaptureDeviceBin captureDeviceBin = null;
    try {
      captureDeviceBin = new CaptureDeviceBin(captureDevice, properties);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    pipeline.add(captureDeviceBin.getBin());
    // Add them to a list so that we can send EOS's to their source Elements.
    captureDeviceBins.add(captureDeviceBin);
    return true;
  }
}
