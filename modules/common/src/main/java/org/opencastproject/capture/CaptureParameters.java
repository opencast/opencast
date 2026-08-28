/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.capture;

/**
 * Contains properties that the ConfigurationManager refer. These properties should exist in the configuration file on
 * the local machine as well as the centralised server.
 */
public interface CaptureParameters {

  /** Added by Opencast internally, this will appear in your agent properties and tell you which agent version
   * Opencast thinks you are running.  This will be a '1' for pre-CA API 2.x agents, and '2' for CA API 2.x agents.
   */
  String AGENT_VERSION = "opencast.agent.version";

  /** Agent configuration property indicating how the agent was registered */
  String AGENT_REGISTRATION_TYPE = "org.opencastproject.registration.type";

  /** Agent configuration value indicating ad-hoc registration */
  String AGENT_REGISTRATION_TYPE_ADHOC = "ad-hoc";

  /** The key for the workflow definition, if any, in the capture properties attached to the iCal event */
  String INGEST_WORKFLOW_DEFINITION = "org.opencastproject.workflow.definition";

  /** The key for the capture agent vendor's name */
  String VENDOR_NAME = "vendor.name";

  /** The key for the capture agent's model */
  String VENDOR_MODEL = "vendor.model";

  /** The key for the capture agent's firmware information */
  String VENDOR_FIRMWARE = "vendor.firmware";

  /** The key for the capture agent's hardware information */
  String VENDOR_HARDWARE = "vendor.hardware";

  /** A binary flag indicating that the device supports starting a recording paused (ie, without actually capturing) */
  String CAPTURE_LOCAL_STARTPAUSED = "capture.local.startpaused";

  /** A binary flag indicating that whether the device supports streaming as it captures */
  String CAPTURE_STREAM_CAPABLE = "capture.stream.capable";

  /** A binary flag indicating that whether the device supports starting the stream but not sending data initially */
  String CAPTURE_STREAM_STARTPAUSED = "capture.stream.startpaused";

  /** A comma delimited list of bitrates and frame sizes supported by the CA's streaming system */
  String CAPTURE_STREAM_CONFIGURATION = "capture.stream.configuration";

  /** A command delimited list of PTZ positions supported by the CA */
  String CAPTURE_DEVICE_POSITIONS = "capture.device.positions";

  /** A comma delimited list of frame composition options supported by the CA */
  String CAPTURE_DEVICE_CONTENT = "capture.device.content";

  /** The vendor specific extension prefix for anything a CA might need that's not otherwise defined */
  String CAPTURE_EXTENSION_PREFIX = "X-";

  /** A comma delimited list of the friendly names for capturing devices */
  String CAPTURE_DEVICE_NAMES = "capture.device.names";

  /** An integer signaling live streaming for capturing devices */
  String CAPTURE_DEVICE_STREAM = "capture.device.stream";

  /** An integer signaling live streaming recording and uploading for capturing devices */
  String CAPTURE_DEVICE_RECORD = "capture.device.record";

  /** A comma delimited list of the layouts for capturing devices */
  String CAPTURE_DEVICE_LAYOUT = "capture.device.layout";

  /** A comma delimited list of the layouts for capturing devices */
  String CAPTURE_DEVICE_CAMERA_POSITION = "capture.device.cameraPosition";

  /** String prefix used when specify capture device properties */
  String CAPTURE_DEVICE_PREFIX = "capture.device.";

}
