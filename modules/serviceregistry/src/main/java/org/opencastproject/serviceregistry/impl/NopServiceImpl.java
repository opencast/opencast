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

package org.opencastproject.serviceregistry.impl;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.OsgiAbstractJobProducer;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.NopService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.Log;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * No operation service.
 * <p>
 * This dummy service just exists for creating jobs for testing purposes.
 */
@Component(
  property = {
    "service.description=No operation service"
  },
  immediate = true,
  service = { NopService.class }
)
public final class NopServiceImpl extends OsgiAbstractJobProducer implements NopService {
  private static final Log log = Log.mk(NopServiceImpl.class);

  public static final String PAYLOAD = "NopServicePayload";

  public NopServiceImpl() {
    super("org.opencastproject.nop");
  }

  @Override
  @Reference(name = "serviceRegistry")
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

  @Override
  @Reference(name = "securityService")
  public void setSecurityService(SecurityService securityService) {
    super.setSecurityService(securityService);
  }

  @Override
  @Reference(name = "userDirectoryService")
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    super.setUserDirectoryService(userDirectoryService);
  }

  @Override
  @Reference(name = "organizationDirectoryService")
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    super.setOrganizationDirectoryService(organizationDirectoryService);
  }

  @Override protected String process(Job job) throws Exception {
    log.info("Processing job %d", job.getId());
    return PAYLOAD;
  }

  @Override public Job nop() {
    try {
      return getServiceRegistry().createJob(getJobType(), "nop");
    } catch (ServiceRegistryException e) {
      return chuck(e);
    }
  }
}
