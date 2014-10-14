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
package org.opencastproject.job.api;

import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

/**
 * Refined implementation of {@link org.opencastproject.job.api.AbstractJobProducer} suitable for use in an
 * OSGi environment.
 * <p/>
 * OSGi dependency injection methods are provided to reduce the amount of boilerplate code needed per
 * service implementation.
 */
public abstract class OsgiAbstractJobProducer extends AbstractJobProducer {
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;
  private OrganizationDirectoryService organizationDirectoryService;

  protected OsgiAbstractJobProducer(String jobType) {
    super(jobType);
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Override
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }
}
