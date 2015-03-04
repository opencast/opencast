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

import org.opencastproject.serviceregistry.api.JaxbHostRegistration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A record of a host providing Matterhorn services.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "host", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "host", namespace = "http://serviceregistry.opencastproject.org")
@Entity(name = "HostRegistration")
@Table(name = "mh_host_registration", uniqueConstraints = @UniqueConstraint(columnNames = "host"))
@NamedQueries({
        @NamedQuery(name = "HostRegistration.cores", query = "SELECT sum(hr.maxJobs) FROM HostRegistration hr where hr.active = true"),
        @NamedQuery(name = "HostRegistration.byHostName", query = "SELECT hr from HostRegistration hr where hr.baseUrl = :host"),
        @NamedQuery(name = "HostRegistration.getAll", query = "SELECT hr FROM HostRegistration hr where hr.active = true") })
public class HostRegistrationJpaImpl extends JaxbHostRegistration {

  /** The primary key identifying this host */
  private Long id;

  /**
   * Creates a new host registration which is online
   */
  public HostRegistrationJpaImpl() {
    super();
  }

  public HostRegistrationJpaImpl(String baseUrl, String address, long memory, int cores, int maxJobs, boolean online,
          boolean maintenance) {
    super(baseUrl, address, memory, cores, maxJobs, online, maintenance);
  }

  @Id
  @Column(name = "id")
  @GeneratedValue
  public Long getId() {
    return id;
  }

  @Override
  @Column(name = "host", nullable = false, length = 255)
  @XmlElement(name = "base_url")
  public String getBaseUrl() {
    return super.getBaseUrl();
  }

  @Override
  @Column(name = "address", nullable = false, length = 39)
  @XmlElement(name = "address")
  public String getIpAddress() {
    return super.getIpAddress();
  }

  @Override
  @Column(name = "memory", nullable = false)
  @XmlElement(name = "memory")
  public long getMemory() {
    return super.getMemory();
  }

  @Override
  @Column(name = "cores", nullable = false)
  @XmlElement(name = "cores")
  public int getCores() {
    return super.getCores();
  }

  @Override
  @Column(name = "max_jobs", nullable = false)
  @XmlElement(name = "max_jobs")
  public int getMaxJobs() {
    return super.getMaxJobs();
  }

  @Override
  @Column(name = "online", nullable = false)
  @XmlElement(name = "online")
  public boolean isOnline() {
    return super.isOnline();
  }

  @Override
  @Column(name = "active", nullable = false)
  @XmlElement(name = "active")
  public boolean isActive() {
    return super.isActive();
  }

  @Column(name = "maintenance", nullable = false)
  @XmlElement(name = "maintenance")
  @Override
  public boolean isMaintenanceMode() {
    return super.isMaintenanceMode();
  }

  /**
   * Sets the primary key identifier.
   *
   * @param id
   *          the identifier
   */
  public void setId(Long id) {
    this.id = id;
  }

}
