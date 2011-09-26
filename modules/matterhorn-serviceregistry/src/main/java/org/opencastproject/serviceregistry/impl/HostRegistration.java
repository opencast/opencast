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
package org.opencastproject.serviceregistry.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A record of a host providing Matterhorn services.
 */
@Entity(name = "HostRegistration")
@Table(name = "HOST_REGISTRATION", uniqueConstraints = @UniqueConstraint(columnNames = "HOST"))
@NamedQueries({
        @NamedQuery(name = "HostRegistration.cores", query = "SELECT sum(hr.maxJobs) FROM HostRegistration hr"),
        @NamedQuery(name = "HostRegistration.byHostName", query = "SELECT hr from HostRegistration hr where hr.baseUrl = :host") })
public class HostRegistration {

  /** No-arg constructor needed by JPA */
  public HostRegistration() {
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
  public HostRegistration(String baseUrl, int maxJobs, boolean online, boolean maintenance) {
    this.baseUrl = baseUrl;
    this.maxJobs = maxJobs;
    this.online = online;
    this.maintenanceMode = maintenance;
  }

  /** The primary key identifying this host */
  @Id
  @Column
  @GeneratedValue
  protected Long id;

  /** The base URL for this host */
  @Column(name = "HOST", nullable = false)
  protected String baseUrl;

  /**
   * The maximum number of concurrent jobs this host can run. Typically, this is the number of cores available to the
   * JVM.
   */
  @Column(name = "MAX_JOBS", nullable = false)
  protected int maxJobs;

  /** Whether this host is available */
  @Column(name = "ONLINE", nullable = false)
  protected boolean online;

  /** Whether this host is in maintenance mode */
  @Column(name = "MAINTENANCE", nullable = false)
  protected boolean maintenanceMode;

  /**
   * Gets the primary key for this host registration.
   * 
   * @return the primary key
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the primary key for this host registration.
   * 
   * @param id
   *          the primary key
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the baseUrl for this host
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * @param baseUrl
   *          the baseUrl to set
   */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * @return the maxJobs
   */
  public int getMaxJobs() {
    return maxJobs;
  }

  /**
   * @param maxJobs
   *          the maxJobs to set
   */
  public void setMaxJobs(int maxJobs) {
    this.maxJobs = maxJobs;
  }

  /**
   * @return whether this host is online
   */
  public boolean isOnline() {
    return online;
  }

  /**
   * @param online
   *          the online status to set
   */
  public void setOnline(boolean online) {
    this.online = online;
  }

  /**
   * @return the maintenanceMode
   */
  public boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  /**
   * @param maintenanceMode
   *          the maintenanceMode to set
   */
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
    return baseUrl.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof HostRegistration) {
      return baseUrl.equals(((HostRegistration) o).baseUrl);
    }
    return false;
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
