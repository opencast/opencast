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

/**
 * Interface representing a host.
 */
public interface HostRegistration {

  /**
   * @return the baseUrl for this host
   */
  String getBaseUrl();

  /**
   * @param baseUrl
   *          the baseUrl to set
   */
  void setBaseUrl(String baseUrl);

  /**
   * @return the IP address for this host
   */
  String getIpAddress();

  /**
   * @param address
   *          the IP address to set
   */
  void setIpAddress(String address);

  /**
   * @return the allocated memory of this host
   */
  long getMemory();

  /**
   * @param memory
   *          the memory to set
   */
  void setMemory(long memory);

  /**
   * @return the available cores of this host
   */
  int getCores();

  /**
   * @param cores
   *          the cores to set
   */
  void setCores(int cores);

  /**
   * @return the maxJobs
   */
  int getMaxJobs();

  /**
   * @param maxJobs
   *          the maxJobs to set
   */
  void setMaxJobs(int maxJobs);

  /**
   * @return whether this host is active
   */
  boolean isActive();

  /**
   * @param active
   *          the active status to set
   */
  void setActive(boolean active);

  /**
   * @return whether this host is online
   */
  boolean isOnline();

  /**
   * @param online
   *          the online status to set
   */
  void setOnline(boolean online);

  /**
   * @return the maintenanceMode
   */
  boolean isMaintenanceMode();

  /**
   * @param maintenanceMode
   *          the maintenanceMode to set
   */
  void setMaintenanceMode(boolean maintenanceMode);

}
