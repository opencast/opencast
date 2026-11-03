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

package org.opencastproject.capture.admin.impl;

import static org.opencastproject.capture.admin.api.CaptureAgentStateService.BOOLEAN_OFF;
import static org.opencastproject.capture.admin.api.CaptureAgentStateService.BOOLEAN_ON;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from.
 */
@Entity @IdClass(AgentImplId.class)
@Table(name = "oc_capture_agent_state")
@NamedQueries({
  @NamedQuery(name = "Agent.get", query = "select a from AgentImpl a where a.name = :id and a.organization = :org"),
  @NamedQuery(name = "Agent.byOrganization", query = "SELECT a FROM AgentImpl a where a.organization = :org")
})
public class AgentImpl implements Agent {

  private static final Logger log = LoggerFactory.getLogger(AgentImpl.class);

  /**
   * The name of the agent.
   */
  @Id
  @Column(name = "id", length = 128)
  protected String name;

  /**
   * The state of the agent. This should be defined from the constants in AgentState.
   */
  @Lob
  @Column(name = "state", nullable = false, length = 65535)
  protected String state;

  /**
   * The URL of the agent. This is determined from the referer header parameter when the agent is registered.
   */
  @Lob
  @Column(name = "url", length = 65535)
  protected String url;

  @Id
  @Column(name = "organization", length = 128)
  protected String organization;

  /**
   * The time at which the agent last checked in with this service. Note that this is an absolute timestamp (ie,
   * milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in).
   */
  @Column(name = "last_heard_from", nullable = false)
  protected Long lastHeardFrom;

  /** The roles allowed to schedule this agent */
  @ElementCollection
  @Column(name = "role", nullable = false)
  @CollectionTable(name = "oc_capture_agent_role", indexes = {
      @Index(name = "IX_oc_capture_agent_role_pk", columnList = "id, organization")
    }, uniqueConstraints = {
      @UniqueConstraint(name = "UNQ_capture_agent_role_id_org_role", columnNames = {"id", "organization", "role"})
    }, joinColumns = {
      @JoinColumn(name = "id", referencedColumnName = "id", nullable = false),
      @JoinColumn(name = "organization", referencedColumnName = "organization", nullable = false) })
  protected Set<String> schedulerRoles = new HashSet<>();

  /**
   * The capabilities the agent has Capabilities are the devices this agent can record from, with a friendly name
   * associated to determine their nature (e.g. PRESENTER --&gt; dev/video0)
   */
  @Lob
  @Column(name = "configuration", nullable = true, length = 65535)
  protected String configurationString;

  // Private var to store the caps as a properties object.
  @Transient
  private Properties capabilitiesProperties;

  // Private var to store the configuration as a properties object.
  @Transient
  private Properties configurationProperties;

  /**
   * Required 0-arg constructor for JAXB, creates a blank agent.
   */
  public AgentImpl() {
  };

  /**
   * Builds a representation of the agent.
   *
   * @param agentName
   *          The name of the agent.
   * @param organization
   *          The organization identifier to which this agent belongs
   * @param agentState
   *          The state of the agent. This should be defined from the constants in AgentState
   * @param configuration
   *          The configuration of the agent.
   */
  public AgentImpl(String agentName, String organization, String agentState, String agentUrl, Properties configuration) {
    name = agentName;
    this.setState(agentState);
    this.setUrl(agentUrl);
    this.setOrganization(organization);
    // Agents with no capabilities are allowed. These can/will be updated after the agent is built if necessary.
    setConfiguration(configuration);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#setState(java.lang.String)
   */
  public void setState(String newState) {
    state = newState;
    setLastHeardFrom(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getState()
   */
  public String getState() {
    return state;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#setUrl(java.lang.String)
   */
  public void setUrl(String agentUrl) {
    url = agentUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getUrl()
   */
  public String getUrl() {
    return url;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#setLastHeardFrom(java.lang.Long)
   */
  public void setLastHeardFrom(Long time) {
    lastHeardFrom = time;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getLastHeardFrom()
   */
  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getCapabilities()
   */
  public Properties getCapabilities() {
    return capabilitiesProperties;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#getConfiguration()
   */
  public Properties getConfiguration() {
    return configurationProperties;
  }

  /**
   * @return the schedulerRoles
   */
  public Set<String> getSchedulerRoles() {
    return schedulerRoles;
  }

  /**
   * @param schedulerRoles
   *          the schedulerRoles to set
   */
  public void setSchedulerRoles(Set<String> schedulerRoles) {
    this.schedulerRoles = schedulerRoles;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization
   *          the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  private boolean getKeyBoolean(Properties properties, String key) {
    if (!properties.containsKey(key)) {
      return false;
    }
    // we support true, or false.  No quotes.
    String value = getKey(properties, key, 5);
    // If the key is defined, it *must* have a value, and it *must* be one of the two things
    if (value.isEmpty() || !(BOOLEAN_ON.equals(value) || BOOLEAN_OFF.equals(value))) {
      throw new IllegalArgumentException("Invalid value for " + key);
    }
    return BOOLEAN_ON.equals(value);
  }

  private String getKey(Properties properties, String key) {
    return getKey(properties, key, 256);
  }

  private String getKey(Properties properties, String key, int maxLength) {
    String value = properties.getProperty(key);
    if (value != null && value.length() > maxLength) {
      throw new IllegalArgumentException("Maximum length of " + maxLength + " exceeded for key " + key);
    }
    return value;
  }

  private Properties parseCapabilities(Properties configuration) {
    Properties deviceCaps = new Properties();
    String names = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    deviceCaps.put(CaptureParameters.CAPTURE_DEVICE_NAMES, names);
    // Get the names and setup a hash map of them
    String[] friendlyNames = deviceCaps.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
    HashMap<String, Integer> propertyCounts = new HashMap<String, Integer>();
    for (String name : friendlyNames) {
      propertyCounts.put(name, 0);
    }

    // For each key
    for (String key : configuration.stringPropertyNames()) {
      // For each device
      for (String name : friendlyNames) {
        String check = CaptureParameters.CAPTURE_DEVICE_PREFIX + name;
        // If the key looks like a device prefix + the name, copy it
        if (key.startsWith(check)) {
          String property = configuration.getProperty(key);
          if (property == null) {
            log.warn("Capture agent '{}': ignoring capability key '{}' because its value is null", this.name, key);
            continue;
          }
          deviceCaps.setProperty(key, property);
          propertyCounts.put(name, propertyCounts.get(name) + 1);
        }
      }
    }
    return deviceCaps;
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#canCaptureLocal()
   */
  @Override
  public boolean canCaptureLocal() {
    if (getVersion().equals(AgentVersion.VERSION_1)) {
      // Version 1.0 agents *must* support local capture
      return true;
    }
    return capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_LOCAL_CAPABLE, BOOLEAN_OFF).equals(BOOLEAN_ON);
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#canStartLocalPaused()
   */
  @Override
  public boolean canStartLocalPaused() {
    return canCaptureLocal()
        && capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, BOOLEAN_OFF).equals(BOOLEAN_ON);
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#canStream()
   */
  @Override
  public boolean canStream() {
    return capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, BOOLEAN_OFF).equals(BOOLEAN_ON);
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#canStartStreamPaused()
   */
  @Override
  public boolean canStartStreamPaused() {
    return canStream()
        && capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, BOOLEAN_OFF).equals(BOOLEAN_ON);
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#canStartStreamPaused()
   */
  @Override
  public Set<String> getInputs() {
    return Arrays.stream(capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "")
        .split(","))
        .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#streamConfigurations()
   */
  @Override
  public Set<String> streamConfigurations() {
    return Arrays.stream(capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "")
        .split(","))
        .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#devicePositions()
   */
  @Override
  public Set<String> devicePositions() {
    return Arrays.stream(capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "")
        .split(","))
        .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#contentConfigurations()
   */
  @Override
  public Set<String> contentConfigurations() {
    return Arrays.stream(capabilitiesProperties.getProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "")
            .split(","))
        .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#vendorKeys()
   */
  @Override
  public Map<String, String> vendorKeys() {
    return capabilitiesProperties.stringPropertyNames().stream()
        .filter(s -> s.startsWith(CaptureParameters.CAPTURE_EXTENSION_PREFIX))
        .collect(Collectors.toUnmodifiableMap(
            key -> key,
            capabilitiesProperties::getProperty));
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#getVendor()
   */
  @Override
  public String getVendor() {
    return configurationProperties.getProperty(CaptureParameters.VENDOR_NAME, "");
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#getModel()
   */
  @Override
  public String getModel() {
    return configurationProperties.getProperty(CaptureParameters.VENDOR_MODEL, "");
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#getHardware()
   */
  @Override
  public String getHardware() {
    return configurationProperties.getProperty(CaptureParameters.VENDOR_HARDWARE, "");
  }

  /**
   * {@inheritDoc}
   *
   * @see Agent#getFirmware()
   */
  @Override
  public String getFirmware() {
    return configurationProperties.getProperty(CaptureParameters.VENDOR_FIRMWARE, "");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.capture.admin.api.Agent#setConfiguration(java.util.Properties)
   */
  public void setConfiguration(Properties configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("configuration cannot be null");
    }

    // Set the configuration variables
    configurationProperties = new Properties();
    configurationProperties.putAll(configuration);

    Set<String> requiredKeys2x = Set.of(
      CaptureParameters.VENDOR_NAME,
      CaptureParameters.VENDOR_MODEL,
      CaptureParameters.VENDOR_FIRMWARE,
      CaptureParameters.VENDOR_HARDWARE);

    Set<String> configs = configuration.stringPropertyNames().stream()
        .filter(requiredKeys2x::contains).collect(Collectors.toSet());
    // Ensure there are four strings, and that they're all between 1 and at most 256 characters
    boolean hasRequired2xKeys = configs.size() == 4
        // Strings must be at least 1 character, and less than 256
        && configs.stream()
            .map(configuration::getProperty)
            .map(String::length)
            .allMatch(i -> i > 0 && i <= 256);

    if (hasRequired2xKeys) {
      set2xAgentConfiguration(configuration);
      configurationProperties.putAll(capabilitiesProperties);
    } else {
      set1xAgentConfiguration(configuration);
    }

    try {
      StringWriter sw = new StringWriter();
      configuration.store(sw, "");
      this.configurationString = sw.toString();
    } catch (IOException e) {
      log.warn("Unable to store agent " + "'s capabilities to the database, IO exception occurred.", e);
    }
  }

  private void set1xAgentConfiguration(Properties configuration) {
    // Figure out the capabilities variables

    capabilitiesProperties = new Properties();
    capabilitiesProperties.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());
    configurationProperties.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_1.toString());

    // Parse names
    String names = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (names == null) {
      log.debug("Capture agent '{}' failed to provide device names ({})", name, CaptureParameters.CAPTURE_DEVICE_NAMES);
    } else {
      Properties devices = parseCapabilities(configuration);
      for (String key : devices.stringPropertyNames()) {
        capabilitiesProperties.put(key, devices.getProperty(key));
      }
    }

    // Parse stream
    String stream = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_STREAM);
    if (stream == null) {
      log.debug("Capture agent '{}' failed to provide stream ({})", name, CaptureParameters.CAPTURE_DEVICE_STREAM);
    } else {
      // Validate
      String[] streamOptions = stream.split(",");
      Set<String> validOptions = Set.of("0", "1");
      boolean allFound = Arrays.stream(streamOptions)
          .map(String::trim)
          .allMatch(validOptions::contains);
      if (!allFound) {
        log.error("Capture agent '{}' failed to provide valid options for stream ({})", stream, CaptureParameters.CAPTURE_DEVICE_STREAM);
      }
      capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_STREAM, stream);
    }

    // Parse record
    String record = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_RECORD);
    if (stream == null) {
      log.debug("Capture agent '{}' failed to provide record ({})", name, CaptureParameters.CAPTURE_DEVICE_RECORD);
    } else {
      // Validate
      String[] recordOptions = stream.split(",");
      Set<String> validOptions = Set.of("0", "1");
      boolean allFound = Arrays.stream(recordOptions)
          .map(String::trim)
          .allMatch(validOptions::contains);
      if (!allFound) {
        log.error("Capture agent '{}' failed to provide valid options for stream ({})", record, CaptureParameters.CAPTURE_DEVICE_RECORD);
      }
      capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_RECORD, record);
    }

    // Parse layout
    String layout = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_LAYOUT);
    if (layout == null) {
      log.debug("Capture agent '{}' failed to provide layout ({})", name, CaptureParameters.CAPTURE_DEVICE_LAYOUT);
    } else {
      capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_LAYOUT, layout);
    }

    // Parse camera position
    String cameraPosition = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_CAMERA_POSITION);
    if (cameraPosition == null) {
      log.debug("Capture agent '{}' failed to provide camera position ({})", name, CaptureParameters.CAPTURE_DEVICE_CAMERA_POSITION);
    } else {
      capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_CAMERA_POSITION, cameraPosition);
    }
  }

  private void set2xAgentConfiguration(Properties configuration) {
    // Figure out the capabilities variables

    capabilitiesProperties = new Properties();
    capabilitiesProperties.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_2.toString());
    configurationProperties.put(CaptureParameters.AGENT_VERSION, AgentVersion.VERSION_2.toString());

    // Parse names
    if (!configuration.containsKey(CaptureParameters.CAPTURE_DEVICE_NAMES)) {
      log.debug("Capture agent '{}' failed to provide device names ({})", name, CaptureParameters.CAPTURE_DEVICE_NAMES);
    } else {
      Set<String> names = Arrays.stream(configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "")
          .split(","))
          .map(String::trim)
          .collect(Collectors.toSet());
      if (names.isEmpty()) {
        throw new IllegalArgumentException("Agent provided devices key, but no devices");
      } else if (names.stream().anyMatch(String::isBlank)) {
        throw new IllegalArgumentException("Agent device names contains a blank name");
      } else if (names.stream().anyMatch(s -> s.length() > 256)) {
        throw new IllegalArgumentException("Agent device names too long");
      } else {
        // We rebuild the string here since we need everything in this Properties to be string-able for sanity
        capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, String.join(",", names));

        Properties devices = parseCapabilities(configuration);
        for (String key : devices.stringPropertyNames()) {
          capabilitiesProperties.put(key, devices.getProperty(key));
        }
      }
    }

    boolean supportLocalCapture = getKeyBoolean(configuration, CaptureParameters.CAPTURE_LOCAL_CAPABLE);
    boolean supportLocalStartPaused =
        supportLocalCapture && getKeyBoolean(configuration, CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED);
    boolean supportStreaming = getKeyBoolean(configuration, CaptureParameters.CAPTURE_STREAM_CAPABLE);
    boolean supportStreamStartPaused =
        supportStreaming && getKeyBoolean(configuration, CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED);

    capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_LOCAL_CAPABLE, supportLocalCapture ? BOOLEAN_ON : BOOLEAN_OFF);
    if (supportLocalCapture) {
      capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_LOCAL_CANSTARTPAUSED, supportLocalStartPaused
          ? BOOLEAN_ON
          : BOOLEAN_OFF);
    }
    capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_STREAM_CAPABLE, supportStreaming ? BOOLEAN_ON : BOOLEAN_OFF);
    if (supportStreaming) {
      capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_STREAM_CANSTARTPAUSED, supportStreamStartPaused
          ? BOOLEAN_ON
          : BOOLEAN_OFF);
    }
    if (!supportLocalCapture && !supportStreaming) {
      // This is *deliberately* IllegalArgumentException.
      // All other cases are a configuration error which yields an agent which may or may not do what you want
      // *This* case is a completely broken agent that can't be trusted to record locally *OR* stream to the network
      // thus, we throw this exception and catch it in the endpoint so we can return a 400.
      // Earlier approaches here used a custom exception class, except that this method gets called from inside
      // functional code that can't have thrown exception.  Given that this is the sole case where we actually require
      // an exception, I'm doing it this way.
      throw new IllegalArgumentException("Invalid agent configuration: Supports neither streaming nor local capture");
    }

    if (configuration.containsKey(CaptureParameters.CAPTURE_STREAM_CONFIGURATION)) {
      //GDLGDL: Do we want to define a maximum number of options here?
      TreeSet<String> agentStreamConfigs =
          Arrays.stream(configuration.getProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION, "")
              .split(","))
              .map(String::trim)
              .collect(Collectors.toCollection(TreeSet::new));
      // If someone passes a blank string, the length here is still 1...
      if (agentStreamConfigs.stream().anyMatch(String::isEmpty)) {
        throw new IllegalArgumentException("Stream configs have a configuration which is empty");
      } else if (agentStreamConfigs.stream().anyMatch(s -> s.length() > 32)) {
        throw new IllegalArgumentException("Stream configs have a configuration which is too long");
      }
      capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_STREAM_CONFIGURATION,
          String.join(",", agentStreamConfigs));
    }

    if (configuration.containsKey(CaptureParameters.CAPTURE_DEVICE_POSITIONS)) {
      //GDLGDL: Do we want to define a maximum number of options here?
      TreeSet<String> agentDevicePositions =
          Arrays.stream(configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS, "")
              .split(","))
              .map(String::trim)
              .collect(Collectors.toCollection(TreeSet::new));
      // If someone passes a blank string, the length here is still 1...
      if (agentDevicePositions.stream().anyMatch(String::isEmpty)) {
        throw new IllegalArgumentException("Position configs have a configuration which is empty");
      } else if (agentDevicePositions.stream().anyMatch(s -> s.length() > 256)) {
        throw new IllegalArgumentException("Position configs have a configuration which is too long");
      }
      capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_DEVICE_POSITIONS,
          String.join(",", agentDevicePositions));
    }

    if (configuration.containsKey(CaptureParameters.CAPTURE_DEVICE_CONTENT)) {
      //GDLGDL: Do we want to define a maximum number of options here?
      TreeSet<String> agentContent =
          Arrays.stream(configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT, "")
                  .split(","))
              .map(String::trim)
              .collect(Collectors.toCollection(TreeSet::new));
      // If someone passes a blank string, the length here is still 1...
      if (agentContent.stream().anyMatch(String::isEmpty)) {
        throw new IllegalArgumentException("Content configs have a configuration which is empty");
      } else if (agentContent.stream().anyMatch(s -> s.length() > 256)) {
        throw new IllegalArgumentException("Content configs have a configuration which is too long");
      }
      capabilitiesProperties.setProperty(CaptureParameters.CAPTURE_DEVICE_CONTENT,
          String.join(",", agentContent));
    }

    // These, assuming they match the regex, are passed through unmodified since they're vendor tools
    Set<String> vendorKeys = configuration.stringPropertyNames().stream()
        .filter(s -> s.startsWith(CaptureParameters.CAPTURE_EXTENSION_PREFIX))
        .filter(s -> s.matches("X-[A-Za-z0-9._-]{1,256}"))
        .collect(Collectors.toSet());
    for (String key : vendorKeys) {
      String value = configuration.getProperty(key);
      if (value.isEmpty()) {
        throw new IllegalArgumentException("Vendor extension keys have an empty value");
      } else if (value.length() > 256) {
        throw new IllegalArgumentException("Vendor extension key value is too long");
      }
      capabilitiesProperties.setProperty(key, value.trim());
    }
  }

  public AgentVersion getVersion() {
    if (capabilitiesProperties != null) {
      return AgentVersion.fromVersion(capabilitiesProperties.getProperty(CaptureParameters.AGENT_VERSION));
    }
    return AgentVersion.VERSION_1;
  }

  /**
   * Post load method to load the capabilities from a string to the properties object
   */
  @SuppressWarnings("unused")
  @PostLoad
  private void loadCaps() {
    configurationProperties = new Properties();
    if (configurationString == null) {
      log.info("No configuration properties set yet for '{}'", name);
    } else {
      try {
        configurationProperties.load(new StringReader(this.configurationString));
        this.setConfiguration(configurationProperties);
      } catch (IOException e) {
        log.warn("Unable to load agent " + name + "'s capabilities, IO exception occurred.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof AgentImpl) {
      return name.equals(((AgentImpl) o).name);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
