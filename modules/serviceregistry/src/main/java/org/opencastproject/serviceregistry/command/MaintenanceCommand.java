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

import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * An interactive shell command for putting Maintainable services in and out of maintenance mode
 *
 */
@Component(
  property = {
    "service.description=Maintenance Command (m:set id true|false, m:list)",
    "osgi.command.scope=maintain",
    "osgi.command.function=set",
    "osgi.command.function=list"
  },
  immediate = true,
  service = { MaintenanceCommand.class }
)
public class MaintenanceCommand {
  protected ServiceRegistry serviceRegistry;

  @Reference(name = "remoteServiceManager")
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

}
