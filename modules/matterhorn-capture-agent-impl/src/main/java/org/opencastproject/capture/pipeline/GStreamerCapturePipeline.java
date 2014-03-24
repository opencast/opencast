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
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public final class GStreamerCapturePipeline extends GStreamerAbstractPipeline {

  private static final Logger logger = LoggerFactory.getLogger(GStreamerCapturePipeline.class);

  // Used to stop the capture above if something goes wrong at this level.
  private CaptureFailureHandler captureFailureHandler;

  public GStreamerCapturePipeline(CaptureFailureHandler captureFailureHandler) {
   captureDeviceBins = new ArrayList<CaptureDeviceBin>();
   this.captureFailureHandler = captureFailureHandler;
  }

  @Override
  public boolean isMonitoringOnly() {
    return false;
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
      pipeline = create(newRec.getProperties());
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
    if (pipeline.getState(wait * GStreamerCapturePipeline.GST_SECOND) != State.PLAYING) {
      // In case of an error call stop to clean up the pipeline.
      logger.debug("Pipeline was unable to start after " + wait + " seconds.");
      stop(GStreamerCapturePipeline.DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT);
      throw new UnableToStartCaptureException("Unable to start pipeline after " + wait + " seconds.  Aborting!");
    }
    logger.info("{} started.", pipeline.getName());
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
  public Pipeline create(Properties props) {
    properties = props;
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    String[] friendlyNames;
    try {
      friendlyNames = GStreamerPipelineTools.getDeviceNames(props);
    } catch (InvalidCaptureDevicesSpecifiedException e) {
      logger.error(e.getMessage());
      return null;
    }

    String outputDirectory = properties.getProperty(CaptureParameters.RECORDING_ROOT_URL, null);

    devices = GStreamerPipelineTools.initDevices(friendlyNames, outputDirectory, false, properties);
    if (devices == null) {
      // This odd case will be logged why in initDevices.
      return null;
    }

    return startPipeline(devices);
  }

  /**
   * Initializes the pipeline itself, but does not start capturing
   *
   * @param devices
   *          The list of devices to capture from.
   * @return The created {@code Pipeline}, or null in the case of an error.
   */
  private Pipeline startPipeline(List<CaptureDevice> devices) {
    logger.info("Successfully initialised {} devices.", devices.size());
    for (int i = 0; i < devices.size(); i++)
      logger.debug("Device #{}: {}.", i, devices.get(i));

    // setup gstreamer pipeline using capture devices
    Gst.init(); // cannot using gst library without first initialising it

    Pipeline pipeline = new Pipeline("CapturePipeline");
    for (CaptureDevice c : devices) {
      if (!addCaptureDeviceBinsToPipeline(c, pipeline))
        logger.error("Failed to create pipeline for {}.", c);
    }

    pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, pipeline.getName());
    return pipeline;
  }
}
