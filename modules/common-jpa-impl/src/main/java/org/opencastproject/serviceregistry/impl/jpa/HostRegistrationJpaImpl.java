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

package org.opencastproject.serviceregistry.impl.jpa;

import org.opencastproject.serviceregistry.api.HostRegistration;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A record of a host providing Opencast services.
 */
@Entity(name = "HostRegistration")
@Access(AccessType.FIELD)
@Table(name = "oc_host_registration", uniqueConstraints = @UniqueConstraint(columnNames = "host"))
@NamedQueries({
        @NamedQuery(name = "HostRegistration.getMaxLoad", query = "SELECT sum(hr.maxLoad) FROM HostRegistration hr where hr.active = true"),
        @NamedQuery(name = "HostRegistration.getMaxLoadByHostName", query = "SELECT hr.maxLoad FROM HostRegistration hr where hr.baseUrl = :host and hr.active = true"),
  @NamedQuery(name = "HostRegistration.byHostName", query = "SELECT hr from HostRegistration hr where hr.baseUrl = :host"),
  @NamedQuery(name = "HostRegistration.getAll", query = "SELECT hr FROM HostRegistration hr where hr.active = true") })
public class HostRegistrationJpaImpl implements HostRegistration {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "host", nullable = false, length = 255)
  private String baseUrl;

  @Column(name = "address", nullable = false, length = 39)
  private String ipAddress;

  @Column(name = "node_name", length = 255)
  private String nodeName;

  @Column(name = "memory", nullable = false)
  private long memory;

  @Column(name = "cores", nullable = false)
  private int cores;

  /**
   * The maximum load this host can run.  This is not necessarily 1-to-1 with the number of jobs.
   */
  @Column(name = "max_load", nullable = false)
  private float maxLoad;

  @Column(name = "online", nullable = false)
  private boolean online = true;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "maintenance", nullable = false)
  private boolean maintenanceMode = false;;

  /**
   * Creates a new host registration which is online
   */
  public HostRegistrationJpaImpl() {
  }

  public HostRegistrationJpaImpl(String baseUrl, String address, String nodeName, long memory, int cores, float maxLoad, boolean online,
          boolean maintenance) {
    this.baseUrl = baseUrl;
    this.ipAddress = address;
    this.nodeName = nodeName;
    this.memory = memory;
    this.cores = cores;
    this.maxLoad = maxLoad;
    this.online = online;
    this.maintenanceMode = maintenance;
    this.active = true;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  @Override
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String getIpAddress() {
    return ipAddress;
  }

  @Override
  public void setIpAddress(String address) {
    this.ipAddress = address;
  }

  @Override
  public String getNodeName() {
    return nodeName;
  }

  @Override
  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  @Override
  public long getMemory() {
    return memory;
  }

  @Override
  public void setMemory(long memory) {
    this.memory = memory;
  }

  @Override
  public int getCores() {
    return cores;
  }

  @Override
  public void setCores(int cores) {
    this.cores = cores;
  }

  @Override
  public float getMaxLoad() {
    return maxLoad;
  }

  @Override
  public void setMaxLoad(float maxLoad) {
    this.maxLoad = maxLoad;
  }

  @Override
  public boolean isOnline() {
    return online;
  }

  @Override
  public void setOnline(boolean online) {
    this.online = online;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  @Override
  public void setMaintenanceMode(boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HostRegistration))
      return false;
    HostRegistration registration = (HostRegistration) obj;
    return baseUrl.equals(registration.getBaseUrl());
  }

  @Override
  public String toString() {
    return baseUrl;
  }

}
