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
package org.opencastproject.capture.api;

import java.util.List;

/**
 * OSGi service for confidence monitoring
 */
public interface ConfidenceMonitor {

  /**
   * Return the JPEG image monitor associated with the device
   *
   * @param friendlyName Friendly name of device to get source from
   * @return a byte array in jpeg form
   */
  byte[] grabFrame(String friendlyName);

  /**
   * Return all RMS values from device 'name' that occur after Unix time
   * 'timestamp'
   *
   * @param friendlyName The friendly name of the device
   * @param timestamp Unix time in milliseconds marking start of RMS data
   * @return A List of RMS values that occur *after* timestamp
   *
   * @deprecated timestamp format changed to long see: {@see getRMSValues(String, long)}
   */
  @Deprecated
  List<Double> getRMSValues(String friendlyName, double timestamp);

  /**
   * Return all RMS values from device 'name' that occur after Unix time
   * 'timestamp'
   *
   * @param friendlyName The friendly name of the device
   * @param timestamp Unix time in milliseconds marking start of RMS data
   * @return A List of RMS values that occur *after* timestamp
   */
  List<Double> getRMSValues(String friendlyName, long timestamp);

  /**
   * Provide access to the devices on the capture box
   *
   * @return the list of friendly device names associated with the capture agent
   */
  List<String> getFriendlyNames();

  /**
   * Retrieves the url of the core stored in the capture properties file.
   *
   * @return URL of matterhorn core
   */
  String getCoreUrl();

  /**
   * Begin monitoring devices without recording.
   * @return true if successfully started
   */
  boolean startMonitoring();

  /**
   * Stop monitoring devices.
   */
  void stopMonitoring();
}
