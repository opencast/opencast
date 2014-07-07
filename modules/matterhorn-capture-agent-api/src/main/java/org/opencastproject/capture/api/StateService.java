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

import org.opencastproject.capture.admin.api.Recording;

import java.util.Map;

/**
 * Service for querying the capture agent's current state (MH-58).
 */
public interface StateService {

  /**
   * Gets the agent's name
   *
   * @return The name of the agent as defined in the properties file with the appropriate key
   * @see org.opencastproject.capture.CaptureParameters#AGENT_NAME
   */
  String getAgentName();

  /**
   * Gets the state of the agent.
   * This is returning a string so that inter-version compatibility it maintained (eg, a version 2 agent talking to a version 1 core)
   *
   * @return The state of the agent.  Should be defined in AgentState.  May be null in cases where the service implementation is not ready yet.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  String getAgentState();

  /**
   * Gets a map of recording ID and Recording pairs containing all of the recordings the system is aware of.
   * The recording ID is either the DTSTART field in the scheduler iCal feed, or Unscheduled-$AGENTID-$TIMESTAMP if the recording was unscheduled
   * This is returning a string so that inter-version compatibility it maintained (eg, a version 2 agent talking to a version 1 core)
   *
   * @return A map of recording ID-state pairs.  May be null if the implementation is not active yet.
   */
  Map<String, AgentRecording> getKnownRecordings();

  /**
   * Gets the state of a recording.
   *
   * @param recordingID The ID of the recording in question.
   * @return A state defined in RecordingState.  May return null if the implementation is not active.
   * @see org.opencastproject.capture.admin.api.RecordingState
   */
  Recording getRecordingState(String recordingID);
}

