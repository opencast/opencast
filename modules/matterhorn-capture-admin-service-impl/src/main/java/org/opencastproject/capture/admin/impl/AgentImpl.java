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
package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from.
 */
@Entity
@Table(name = "mh_capture_agent_state")
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
  @Column(name = "role")
  @CollectionTable(name = "mh_capture_agent_role", joinColumns = { @JoinColumn(name = "id", referencedColumnName = "id"),
          @JoinColumn(name = "organization", referencedColumnName = "organization") })
  protected Set<String> schedulerRoles = new HashSet<String>();

  /**
   * The capabilities the agent has Capabilities are the devices this agent can record from, with a friendly name
   * associated to determine their nature (e.g. PRESENTER --> dev/video0)
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#setConfiguration(java.util.Properties)
   */
  public void setConfiguration(Properties configuration) {
    if (configuration == null) {
      return;
    }

    // Set the configuration variables
    configurationProperties = configuration;
    try {
      StringWriter sw = new StringWriter();
      configuration.store(sw, "");
      this.configurationString = sw.toString();
    } catch (IOException e) {
      log.warn("Unable to store agent " + "'s capabilities to the database, IO exception occurred.", e);
    }

    // Figure out the capabiltiies variables

    capabilitiesProperties = new Properties();

    String names = configuration.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (names == null) {
      log.warn("Null friendly name list for agent " + name + ".  Capabilities filtering aborted.");
      return;
    }
    capabilitiesProperties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, names);
    // Get the names and setup a hash map of them
    String[] friendlyNames = names.split(",");
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
        if (key.contains(check)) {
          String property = configuration.getProperty(key);
          if (property == null) {
            log.error("Unable to expand variable in value for key {}, returning null!", key);
            capabilitiesProperties = null;
            return;
          }
          capabilitiesProperties.setProperty(key, property);
          propertyCounts.put(name, propertyCounts.get(name) + 1);
        }
      }
    }
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
