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
package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.osgi.service.component.ComponentContext;

import static org.opencastproject.security.util.SecurityUtil.createSystemUser;

/** OSGi implementation of {@link AbstractAclScheduler}. */
public class OsgiAclScheduler extends AbstractAclScheduler {
  private AclServiceFactory aclServiceFactory;
  private SecurityService securityService;
  private OrganizationDirectoryService organizationDirectoryService;
  private ComponentContext cc;

  @Override
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  @Override
  public AclServiceFactory getAclServiceFactory() {
    return aclServiceFactory;
  }

  @Override
  public SecurityContext getAdminContextFor(String orgId) {
    try {
      final Organization org = organizationDirectoryService.getOrganization(orgId);
      return new SecurityContext(securityService, org, createSystemUser(cc, org));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    this.cc = cc;
    //trigger(); todo do not use, rescheduling afterwards does not work
    schedule();
  }

  /** OSGi callback. */
  public void deactivate() {
    shutdown();
  }

  /** OSGi DI callback. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi DI callback. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
