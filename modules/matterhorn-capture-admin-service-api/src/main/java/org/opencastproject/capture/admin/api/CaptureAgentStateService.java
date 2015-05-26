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

package org.opencastproject.capture.admin.api;

import org.opencastproject.util.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * API for the capture-admin service.
 */
public interface CaptureAgentStateService {

  /** Defines the name of the key in the properties file which is used to define the list of sticky agents */
  String STICKY_AGENTS = "capture.admin.sticky.agents";

  /**
   * Returns an agent by its name
   *
   * @param agentName
   *          The name of the agent.
   * @return The agent
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  Agent getAgent(String agentName) throws NotFoundException;

  /**
   * Updates an agent
   *
   * @param agent
   *          the agent to update
   */
  void updateAgent(Agent agent);

  /**
   * Returns the last known agent state by its name
   *
   * @param agentName
   *          The name of the agent.
   * @return The agent state
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  String getAgentState(String agentName) throws NotFoundException;

  /**
   * Sets a given agent's state. Note that this will create the agent if it does not already exist. The state should be
   * defined in {@link org.opencastproject.capture.admin.api.AgentState}.
   *
   * @param agentName
   *          The name of the agent.
   * @param state
   *          The current state of the agent.
   * @see AgentState
   */
  boolean setAgentState(String agentName, String state);

  /**
   *
   * @param agentName
   *          The name of the agent.
   * @param agentUrl
   *          The url of the agent.
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  boolean setAgentUrl(String agentName, String agentUrl) throws NotFoundException;

  /**
   * Remove an agent from the system, if the agent exists.
   *
   * @param agentName
   *          The name of the agent.
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  void removeAgent(String agentName) throws NotFoundException;

  /**
   * Returns the list of known agents that the current user is authorized to schedule.
   *
   * @return A {@link java.util.Map} of name-agent pairs.
   */
  Map<String, Agent> getKnownAgents();

  /**
   * Returns the list of known agent capabilities.
   *
   * @return A {@link java.util.Properties} of name-value capability pairs.
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  Properties getAgentCapabilities(String agentName) throws NotFoundException;

  /**
   * Returns the list of known agent configurations.
   *
   * @return A {@link java.util.Properties} of name-value configuration pairs.
   * @throws NotFoundException
   *           if no agent with the given name has been found
   */
  Properties getAgentConfiguration(String agentName) throws NotFoundException;

  /**
   * Sets the capabilities for the specified agent
   *
   * @param agentName
   * @param capabilities
   * @return One of the constants defined in this class
   */
  boolean setAgentConfiguration(String agentName, Properties capabilities);

  /**
   * Gets the state of a recording, if it exists.
   *
   * @param id
   *          The id of the recording.
   * @return The state of the recording, or null if it does not exist. This should be defined from
   *         {@link org.opencastproject.capture.admin.api.RecordingState}.
   * @see RecordingState
   * @throws NotFoundException
   *           if the recording with the given id has not been found
   */
  Recording getRecordingState(String id) throws NotFoundException;

  /**
   * Updates the state of a recording with the given state, if it exists.
   *
   * @param id
   *          The id of the recording in the system.
   * @param state
   *          The state to set for that recording. This should be defined from
   *          {@link org.opencastproject.capture.admin.api.RecordingState}.
   * @see RecordingState
   */
  boolean setRecordingState(String id, String state);

  /**
   * Removes a recording from the system, if the recording exists.
   *
   * @param id
   *          The id of the recording to remove.
   * @throws NotFoundException
   *           if the recording with the given id has not been found
   */
  void removeRecording(String id) throws NotFoundException;

  /**
   * Gets the state of all recordings in the system.
   *
   * @return A map of recording-state pairs.
   */
  Map<String, Recording> getKnownRecordings();

  /**
   * Gets a Set of ids of all known recordings.
   *
   * @return Set<String> recording ids
   */
  List<String> getKnownRecordingsIds();

}
