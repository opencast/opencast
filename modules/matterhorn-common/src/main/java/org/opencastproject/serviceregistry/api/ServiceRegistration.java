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

import java.util.Date;

/**
 * Manages clustered services and the Jobs they may create to enable asynchronous job handling.
 */
public interface ServiceRegistration {

  /**
   * @return the type of service
   */
  String getServiceType();

  /**
   * @return the host providing the service endpoint.
   */
  String getHost();

  /**
   * @return The relative path to the service endpoint.
   */
  String getPath();

  /**
   * @return Whether the service performs long running operations using Jobs.
   */
  boolean isJobProducer();

  /**
   * @return Whether the service is online
   */
  boolean isOnline();

  /**
   * Whether the service is in maintenance mode. If a server was in maintenance mode when shut down, it will remain in
   * maintenance mode when it comes back online
   */
  boolean isInMaintenanceMode();

  /**
   * Gets the last time the service has been declared online
   * 
   * @return the onlineFrom
   */
  Date getOnlineFrom();

  /**
   * Gets the current state of the service
   * 
   * @return current state
   */
  ServiceState getServiceState();

  /**
   * Gets the last date when state was changed
   * 
   * @return last date when state was changed
   */
  Date getStateChanged();

  /**
   * Gets the job signature which changed last time the service state to error.
   * 
   * @return the signature from error state trigger job
   */
  int getErrorStateTrigger();

  /**
   * Gets the job signature which changed last time the service state to warning
   * 
   * @return the signature from warning state trigger job
   */
  int getWarningStateTrigger();

}
