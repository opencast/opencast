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
package org.opencastproject.capture.admin.api;

import java.util.Properties;

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from.
 */
public interface Agent {

  /**
   * Gets the name of the agent.
   * 
   * @return The name of the agent.
   */
  String getName();

  /**
   * Sets the state of the agent, and updates the time it was last heard from.
   * 
   * @param newState
   *          The new state of the agent. This should defined from the constants in
   *          {@link org.opencastproject.capture.admin.api.AgentState}. This can be equal to the current one if the goal
   *          is to update the timestamp.
   * @see AgentState
   */
  void setState(String newState);

  /**
   * Gets the state of the agent.
   * 
   * @return The state of the agent. This should be defined from the constants in
   *         {@link org.opencastproject.capture.admin.api.AgentState}.
   * @see AgentState
   */
  String getState();

  /**
   * Sets the url of the agent.
   * 
   * @param agentUrl
   *          The url of the agent as determined by the referer header field of its request while registering
   */
  void setUrl(String agentUrl);

  /**
   * Gets the url of the agent.
   * 
   * @return the url of the agent.
   */
  String getUrl();

  /**
   * Sets the time at which the agent last checked in.
   * 
   * @param time
   *          The number of milliseconds since 1970 when the agent last checked in.
   */
  void setLastHeardFrom(Long time);

  /**
   * Gets the time at which the agent last checked in.
   * 
   * @return The number of milliseconds since 1970 when the agent last checked in.
   */
  Long getLastHeardFrom();

  /**
   * Gets the capture agent's capability list.
   * 
   * @return The agent's capabilities, or null if there is an error.
   */
  Properties getCapabilities();

  /**
   * Gets the capture agent's full configuration list.
   * 
   * @return The agent's configuration, or null if there is an error.
   */
  Properties getConfiguration();

  /**
   * Sets the capture agent's configuration list.
   * 
   * @param configuration
   *          The agent's configuration.
   */
  void setConfiguration(Properties configuration);
}
