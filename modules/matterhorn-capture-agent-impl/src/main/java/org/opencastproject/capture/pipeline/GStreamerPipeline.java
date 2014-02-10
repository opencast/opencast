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

import org.opencastproject.capture.impl.MonitoringListener;

/**
 * GStreamer pipeline wrapper class schould implement this interface.
 */
public interface GStreamerPipeline {

  /**
   * Returns true if Gstreamer Pipeline is null.
   * @return true if Gstreamer Pipeline is null
   */
  boolean isPipelineNull();

  /**
   * Returns true if confidence monitoring enabled.
   * @return true if confidence monitoring enabled
   */
  boolean isMonitoringEnabled();

  /**
   * Returns true if only monitoring Pipeline is running (without capture).
   * @return true if only monitoring Pipeline is running (without capture)
   */
  boolean isMonitoringOnly();

  /**
   * Stop Pipeline.
   */
  void stop();

  /**
   * This method waits until the pipeline has had an opportunity to shutdown and if it surpasses the maximum timeout
   * value it will be manually stopped.
   */
  void stop(long timeout);

  /**
   * Seta for monitoring listener.
   * @param rmsValueGrabber monitoring listener
   */
  void setMonitoringListener(MonitoringListener monitoringListener);
}
