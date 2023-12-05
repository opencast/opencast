/**
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

  /** Agent configuration property indicating how the agent was registered */
  String AGENT_REGISTRATION_TYPE = "org.opencastproject.registration.type";

  /** Agent configuration value indicating ad-hoc registration */
  String AGENT_REGISTRATION_TYPE_ADHOC = "ad-hoc";

  /** The key for the workflow definition, if any, in the capture properties attached to the iCal event */
  String INGEST_WORKFLOW_DEFINITION = "org.opencastproject.workflow.definition";

  /** A comma delimited list of the friendly names for capturing devices */
  String CAPTURE_DEVICE_NAMES = "capture.device.names";

  /** String prefix used when specify capture device properties */
  String CAPTURE_DEVICE_PREFIX = "capture.device.";

}
