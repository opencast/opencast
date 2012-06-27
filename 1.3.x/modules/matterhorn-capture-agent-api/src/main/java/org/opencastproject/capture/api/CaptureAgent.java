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

import org.opencastproject.mediapackage.MediaPackage;

import java.util.List;
import java.util.Properties;

/**
 * OSGi service for starting capture (MH-730)
 */
public interface CaptureAgent {
  /**
   * Starting a simple capture.  Uses the machine's default settings for everything and assigns a generated recording ID to the capture.
   * Note that this will not start a second capture if there is already one in progress.  You must first stop the running capture before starting another.
   * @return The recording ID associated with the recording, or null in the case of an error.
   */
  String startCapture();

  /**
   * Starting a simple capture.  Uses the machine's default settings for capture parameters and assigns a generated recording ID to the capture, but uses the metadata passed in via the parameter.
   * Note that this will not start a second capture if there is already one in progress.  You must first stop the running capture before starting another.
   * @param mediaPackage The {@code MediaPackage} of metadata for the capture.
   * @return The recording ID associated with the recording, or null in the case of an error.
   */
  String startCapture(MediaPackage mediaPackage);

  /**
   * Starting a simple capture.  Uses the machine's default metadata and assigns a generated recording ID to the capture if one is not specified in the properties object.  The relevant keys can be found in CaptureParameters.
   * Properties not in the configuration parameter use the machine defaults.  If, for example, you only wish to specify the recording's ID then only set that value in the properties and the machine will use its defeaults for the others. 
   * Note that this will not start a second capture if there is already one in progress.  You must first stop the running capture before starting another.
   * @param configuration {@code Properties} object containing the properties for the recording.
   * @return The recording ID associated with the recording, or null in the case of an error.
   * @see org.opencastproject.capture.CaptureParameters
   */
  String startCapture(Properties configuration);

  /**
   * Starting a simple capture.  Generates a recording ID to the capture if one is not specified in the properties object.  The relevant keys can be found in CaptureParameters.
   * Both parameters - the recording properties and the metadata - are used rather than the machine defaults.
   * Note that this will not start a second capture if there is already one in progress.  You must first stop the running capture before starting another.
   * @param mediaPackage The {@code MediaPackage} of metadata for the capture.
   * @param configuration {@code Properties} object containing the properties for the recording.
   * @return The recording ID associated with the recording, or null in the case of an error.
   * @see org.opencastproject.capture.CaptureParameters
   */
  String startCapture(MediaPackage mediaPackage, Properties configuration);
  
  /**
   * Stops the currently running capture.  Returns true on success.
   * Error conditions occur if gstreamer has an unexpected error, or there is no recording currently in progress.  In this case it will return false.
   * @param immediateIngest True to cause the agent to immediately ingest the capture, false to wait until the normal stop time to ingest.
   * @return True if the capture stopped successfully, false if the recordingID parameter was not the recording currently being captured, or there was an error.
   */
  boolean stopCapture(boolean immediateIngest);

  /**
   * Stops the capture
   * This version takes in a recording ID and only stops the recording if that ID matches the current recording's ID.
   * This is used to prevent accidental stops of recordings.
   * @param recordingID The ID of the recording you wish to stop
   * @param immediateIngest True to cause the agent to immediately ingest the capture, false to wait until the normal stop time to ingest.
   * @return True if the capture stopped successfully, false if the recordingID parameter was not the recording currently being captured, or there was an error.
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture(boolean)
   */
  boolean stopCapture(String recordingID, boolean immediateIngest);

  /**
   * Gets the agent's name.
   * @return The name of the agent.
   */
  String getAgentName();

  /**
   * Gets the machine's current state.
   * This is returning a string so that inter-version compatibility it maintained (eg, a version 2 agent talking to a version 1 core).
   * 
   * @return A state (should be defined in AgentState).
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  String getAgentState();

  /**
   * Gets the agent's capabilities as they appear in the configuration file.
   * @return The agent's capabilities, or null in the case of an error.
   */
  Properties getAgentCapabilities();
  
  /**
   * Returns a pretty-printed version of the agent's default properties.
   * @return The agent's default properties
   */
  @Deprecated
  String getDefaultAgentPropertiesAsString();
  
  /**
   * Returns the agent's default properties.
   * @return The agent's default properties, or null in the case of an error.
   */
  Properties getDefaultAgentProperties();

  /**
   * Returns the current schedule in a {@code List} of {@code ScheduledEvent}s.
   * @return The current schedule in a {@code List} of {@code ScheduledEvent}s or null in the case of an error.
   */
  List<ScheduledEvent> getAgentSchedule();

  /**
   * Forces an update of the schedule data
   */
  void updateSchedule();
}

