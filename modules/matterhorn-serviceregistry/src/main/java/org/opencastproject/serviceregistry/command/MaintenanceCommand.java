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

package org.opencastproject.serviceregistry.command;

import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

/**
 * An interactive shell command for putting Maintainable services in and out of maintenance mode
 *
 */
public class MaintenanceCommand {
  protected ServiceRegistry serviceRegistry;

  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

  public String set(String baseUrl, boolean maintenanceMode) {
    try {
      serviceRegistry.setMaintenanceStatus(baseUrl, maintenanceMode);
      if (maintenanceMode) {
        return baseUrl + " is now in maintenance mode\n";
      } else {
        return baseUrl + " has returned to service\n";
      }
    } catch (ServiceRegistryException e) {
      return "Error setting maintenance mode: " + e.getMessage() + "\n";
    } catch (NotFoundException e) {
      return "Error setting maintenance mode, host " + baseUrl + " not found";
    }
  }

  public String list() {
    try {
      StringBuilder sb = new StringBuilder();
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrations()) {
        sb.append(reg.getServiceType());
        sb.append("@");
        sb.append(reg.getHost());
        if (reg.isInMaintenanceMode()) {
          sb.append(" (maintenance mode)");
        }
        sb.append("\n");
      }
      return sb.toString();
    } catch (ServiceRegistryException e) {
      return "Error: " + e.getMessage() + "\n";
    }
  }

}
