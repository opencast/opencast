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

import java.util.ArrayList;
import java.util.Properties;
import org.gstreamer.Bus;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.ValueList;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.impl.MonitoringListener;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBin;
import org.opencastproject.capture.pipeline.bins.consumers.AudioMonitoringConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Gstreamer Pipeline wrapper.
 */
public abstract class GStreamerAbstractPipeline implements GStreamerPipeline {

  private static final Logger logger = LoggerFactory.getLogger(GStreamerAbstractPipeline.class);

  /** The amount of time to wait until shutting down the pipeline forcefully.**/
  public static final long DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT = 60000L;
  /** Wait intervall time to check pipeline state is null. **/
  protected static final int WAIT_FOR_NULL_SLEEP_TIME = 1000;
  /** Maximum count of rms values to store for each audio device. **/
  private static final int MAX_STORED_RMS_VALUES = 100;
  /**
   * The number of nanoseconds in a second. This is a borrowed constant from GStreamer and is used in the pipeline
   * initialisation routines
   */
  public static final long GST_SECOND = 1000000000L;
  // Capture properties.
  protected Properties properties;
  // List of captureDeviceBins inside the pipeline so that we can send each an EOS.
  protected ArrayList<CaptureDeviceBin> captureDeviceBins;
  // Pipeline used to capture.
  protected Pipeline pipeline;
  // Monitoring listener class.
  protected MonitoringListener monitoringListener = null;

  public GStreamerAbstractPipeline() {
  }

  @Override
  public boolean isPipelineNull() {
    return pipeline == null;
  }

  @Override
  public boolean isMonitoringEnabled() {
    return properties != null && Boolean.valueOf(
            properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false"));
  }

  @Override
  public void setMonitoringListener(MonitoringListener monitoringListener) {
    this.monitoringListener = monitoringListener;
  }

  protected void hookUpBus() {
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
    bus.connect("element", new Bus.MESSAGE() {

      @Override
      public void busMessage(Bus bus, Message msg) {
        Structure msgStructure = msg.getStructure();
        if (monitoringListener != null
                && msgStructure != null
                && msgStructure.hasName("level")
                && msgStructure.hasField("rms")) {

          // message data should be like that:
          //
          // level, endtime=(guint64)60103401360, timestamp=(guint64)55094784580,
          // stream-time=(guint64)55094784580,
          // running-time=(guint64)55094784580, duration=(guint64)5008616780,
          // rms=(double){ -40.952087684510758, -40.984825946785662 },
          // peak=(double){ -36.329598612473987, -36.346987786726558 },
          // decay=(double){ -36.576273313948491, -36.558419474901676 };

          // get element name to determine witch audio device bin sent these values
          String deviceFriendlyName =
                  msg.getSource().getName().replaceFirst(AudioMonitoringConsumer.LEVEL_NAME_PREFIX, "");

          if (deviceFriendlyName != null) {
            ValueList rmsChanelList = msgStructure.getValueList("rms");

            monitoringListener.addRmsValue(deviceFriendlyName, System.currentTimeMillis(), rmsChanelList.getDouble(0));
          }
        }
      }
    });
    logger.debug("Successfully hooked up GStreamer Pipeline bus to Log4J.");
  }

  @Override
  public void stop() {
    stop(DEFAULT_PIPELINE_SHUTDOWN_TIMEOUT);
  }

  /**
   * This method waits until the pipeline has had an opportunity to shutdown and if it surpasses the maximum timeout
   * value it will be manually stopped.
   */
  @Override
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
      captureDeviceBin = new CaptureDeviceBin(captureDevice, properties, isMonitoringOnly(), monitoringListener);
    } catch (Exception e) {
      return false;
    }
    pipeline.add(captureDeviceBin.getBin());
    // Add them to a list so that we can send EOS's to their source Elements.
    captureDeviceBins.add(captureDeviceBin);

    return true;
  }
}
