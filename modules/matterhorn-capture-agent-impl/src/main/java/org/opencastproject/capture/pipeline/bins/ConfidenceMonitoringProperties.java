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

import java.io.File;
import java.util.Properties;

public class ConfidenceMonitoringProperties {
  private String imageloc;
  private String device;
  private int interval;
  private int monitoringLength;
  private boolean debug;

  /** Used to store the confidence monitoring properties. **/
  public ConfidenceMonitoringProperties(CaptureDevice captureDevice, Properties properties) {
    if (properties != null) {
      imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
      device = new File(captureDevice.getOutputPath()).getName();
      interval = Integer.parseInt(properties.getProperty(
              CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName()
                      + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));
      monitoringLength = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH, "60"));
      debug = Boolean.parseBoolean(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_DEBUG, "false"));
    }
  }

  public String getImageloc() {
    return imageloc;
  }

  public String getDevice() {
    return device;
  }

  public int getInterval() {
    return interval;
  }

  public int getMonitoringLength() {
    return monitoringLength;
  }

  public boolean isDebug() {
    return debug;
  }
}
