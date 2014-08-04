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

  /**
   * The maximum number of concurrent jobs this host can run. Typically, this is the number of cores available to the
   * JVM.
   */
  @XmlElement(name = "max_jobs")
  protected int maxJobs;

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
   * @param maxJobs
   *          the maximum number of concurrent jobs that this host can run.
   * @param online
   *          whether the host is online and available for service
   * @param online
   *          whether the host is in maintenance mode
   */
  public JaxbHostRegistration(String baseUrl, int maxJobs, boolean online, boolean maintenance) {
    this.baseUrl = baseUrl;
    this.maxJobs = maxJobs;
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
    this.maxJobs = hostRegistration.getMaxJobs();
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
   * @see org.opencastproject.serviceregistry.api.HostRegistration#getMaxJobs()
   */
  @Override
  public int getMaxJobs() {
    return maxJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.HostRegistration#setMaxJobs(int)
   */
  @Override
  public void setMaxJobs(int maxJobs) {
    this.maxJobs = maxJobs;
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

}
