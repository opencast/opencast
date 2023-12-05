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

import java.util.Map;
import java.util.Properties;

/**
 * API for the capture-admin service.
 */
public interface CaptureAgentStateService {

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

}
