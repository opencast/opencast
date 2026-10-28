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

package org.opencastproject.capture.admin.api;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

  /**
   * Gets the agent's CA API version.
   *
   * @return The agent's version as an Enum member
   */
  AgentVersion getVersion();

  /**
   * Returns true if the agent supports local capture
   * @return True if supported, false otherwise
   */
  boolean canCaptureLocal();

  /**
   * Returns true if the agent supports starting a local capture in a paused mode
   * Requires {@link canCaptureLocal} to return true
   * @return True if supported, false otherwise
   */
  boolean canStartLocalPaused();

  /**
   * Returns true if the agent supports streaming capture
   * @return True if supported, false otherwise
   */
  boolean canStream();

  /**
   * Returns true if the agent supports starting a streaming capture in a paused mode
   * Requires {@link canStream} to return true
   * @return True if supported, false otherwise
   */
  boolean canStartStreamPaused();

  /**
   * Returns a set of input names as strings.  This may be empty, in which case the CA does not support per-event input
   * configuration.
   *
   * @return The list of input names, if any
   */
  Set<String> getInputs();

  /**
   * Returns a list of stream configuration names.  These are passed to the CA in the event properties, and should be
   * used to control specifics of the stream configuration in a vendor specific way.  This may include, but is not
   * limited to, frame size, bitrate, codec, and other encoding parameters.  If the agent does not support configuring
   * these settings on a per-event basis then this list will be empty.
   *
   * @return The list of names provided by the capture agent, if any.
   */
  Set<String> streamConfigurations();

  /**
   * Returns a list of PTZ camera positions for the agent.  These are passed to the CA in the event properties, and
   * should be used to aim PTZ cameras controlled by the CA in a vendor specific way.  If the agent does not support
   * configuring these settings on a per-event basis then this list will be empty.
   *
   * @return The list of camera positions provided by the capture agent, if any.
   */
  Set<String> devicePositions();

  /**
   * Returns a list of content composition configurations for the agent.  These are passed to the CA in the event
   * properties, and should be used to configure on-device content composition in a vendor specific way.  This may
   * include, but is not limited to, configuring inputs, combining inputs, and applying watermarks or other filters.
   * If the agent does not support configuring these settings on a per-event basis then this list will be empty.
   *
   * @return The list of content configurations provided by the capture agent, if any.
   */
  Set<String> contentConfigurations();

  /**
   * Returns a {@link Map} of vendor extension configurations in a key, value pair.  If none were passed by the vendor
   * then this will return an empty map.
   *
   * @return A key, value map of strings containing the vendor extensions, and their values, if any.
   */
  Map<String, String> vendorKeys();

  /**
   * Returns the agent's vendor for 2.x+ agents, blank otherwise.
   *
   * @return The vendor's name, or an empty string
   */
  String getVendor();

  /**
   * Returns the agent's model for 2.x+ agents, blank otherwise.
   * @return The agent's model, or an empty string
   */
  String getModel();

  /**
   * Returns the agent's hardware revision for 2.x+ agents, blank otherwise.
   * @return The agent's hardware revision, or an empty string
   */
  String getHardware();

  /**
   * Returns the agent's firmware revision for 2.x+ agents, blank otherwise.
   * @return The agent's firmware revision, or an empty string
   */
  String getFirmware();
}
