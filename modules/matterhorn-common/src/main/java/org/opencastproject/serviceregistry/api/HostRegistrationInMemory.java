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

public class HostRegistrationInMemory implements HostRegistration {

  protected String baseUrl;

  protected float maxLoad;

  protected int cores;

  protected long memory;

  protected boolean online;

  protected boolean active;

  protected boolean maintenanceMode;

  protected String ipAddress;

  public HostRegistrationInMemory(String baseUrl, float maxLoad, int cores, long memory) {
    this.baseUrl = baseUrl;
    this.maxLoad = maxLoad;
    this.online = true;
    this.active = true;
    this.maintenanceMode = false;
    this.cores = cores;
    this.memory = memory;
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
  public float getMaxLoad() {
    return maxLoad;
  }

  @Override
  public void setMaxLoad(float maxLoad) {
    this.maxLoad = maxLoad;
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
  public boolean isOnline() {
    return online;
  }

  @Override
  public void setOnline(boolean online) {
    this.online = online;
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
  public String getIpAddress() {
    return ipAddress;
  }

  @Override
  public void setIpAddress(String address) {
    this.ipAddress = address;
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

}
