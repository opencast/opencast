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

package org.opencastproject.serviceregistry.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A record of a host.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "host", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "host", namespace = "http://serviceregistry.opencastproject.org")
public class JaxbHostRegistration implements HostRegistration {

  /**
   * The base URL for this host. The length was chosen this short because MySQL complains when trying to create an index
   * larger than this
   */
  @XmlElement(name = "base_url")
  protected String baseUrl;

  @XmlElement(name = "address")
  protected String address;

  @XmlElement(name = "node_name")
  protected String nodeName;

  @XmlElement(name = "memory")
  protected long memory;

  @XmlElement(name = "cores")
  protected int cores;

  /**
   * The maximum load this host can run.  This is not necessarily 1-to-1 with the number of jobs.
   */
  @XmlElement(name = "max_load")
  protected float maxLoad;

  @XmlElement(name = "online")
  protected boolean online;

  @XmlElement(name = "active")
  protected boolean active;

  @XmlElement(name = "maintenance")
  protected boolean maintenanceMode;

  /**
   * Creates a new host registration which is online and not in maintenance mode.
   */
  public JaxbHostRegistration() {
    this.online = true;
    this.active = true;
    this.maintenanceMode = false;
  }

  /**
   * Constructs a new HostRegistration with the parameters provided
   *
   * @param baseUrl
   *          the base URL for this host
   * @param address
   *          the IP address for this host
   * @param nodeName
   *          descriptive node name for this host
   * @param memory
   *          the allocated memory of this host
   * @param cores
   *          the available cores of this host
   * @param maxLoad
   *          the maximum load that this host can support
   * @param online
   *          whether the host is online and available for service
   * @param online
   *          whether the host is in maintenance mode
   */
  public JaxbHostRegistration(String baseUrl, String address, String nodeName, long memory, int cores, float maxLoad, boolean online,
          boolean maintenance) {
    this.baseUrl = baseUrl;
    this.address = address;
    this.nodeName = nodeName;
    this.memory = memory;
    this.cores = cores;
    this.maxLoad = maxLoad;
    this.online = online;
    this.maintenanceMode = maintenance;
    this.active = true;
  }

  /**
   * Creates a new JAXB host registration based on an existing host registration
   *
   * @param hostRegistration
   */
  public JaxbHostRegistration(HostRegistration hostRegistration) {
    this.baseUrl = hostRegistration.getBaseUrl();
    this.address = hostRegistration.getIpAddress();
    this.nodeName = hostRegistration.getNodeName();
    this.memory = hostRegistration.getMemory();
    this.cores = hostRegistration.getCores();
    this.maxLoad = hostRegistration.getMaxLoad();
    this.online = hostRegistration.isOnline();
    this.active = hostRegistration.isActive();
    this.maintenanceMode = hostRegistration.isMaintenanceMode();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getBaseUrl()
   */
  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setBaseUrl(String)
   */
  @Override
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getIpAddress()
   */
  @Override
  public String getIpAddress() {
    return address;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setIpAddress(String)
   */
  @Override
  public void setIpAddress(String address) {
    this.address = address;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getMemory()
   */
  @Override
  public long getMemory() {
    return memory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setMemory(long)
   */
  @Override
  public void setMemory(long memory) {
    this.memory = memory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getCores()
   */
  @Override
  public int getCores() {
    return cores;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setCores(int)
   */
  @Override
  public void setCores(int cores) {
    this.cores = cores;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getMaxLoad()
   */
  @Override
  public float getMaxLoad() {
    return maxLoad;
  }

  @Override
  public void setMaxLoad(float maxLoad) {
    this.maxLoad = maxLoad;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#isActive()
   */
  @Override
  public boolean isActive() {
    return active;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setActive(boolean)
   */
  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#isOnline()
   */
  @Override
  public boolean isOnline() {
    return online;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setOnline(boolean)
   */
  @Override
  public void setOnline(boolean online) {
    this.online = online;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#isMaintenanceMode()
   */
  @Override
  public boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setMaintenanceMode(boolean)
   */
  @Override
  public void setMaintenanceMode(boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HostRegistration))
      return false;
    HostRegistration registration = (HostRegistration) obj;
    return baseUrl.equals(registration.getBaseUrl());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return baseUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getNodeName()
   */
  @Override
  public String getNodeName() {
    return nodeName;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setNodeName(String)
   */
  @Override
  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }
}
