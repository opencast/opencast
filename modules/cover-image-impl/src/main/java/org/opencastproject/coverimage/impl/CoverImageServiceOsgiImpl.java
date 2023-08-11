/*
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

package org.opencastproject.coverimage.impl;

import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AbstractCoverImageService} for use in OSGi environment
 */
@Component(
    immediate = true,
    service = CoverImageService.class,
    property = {
        "service.description=Cover Image Service"
    }
)
public class CoverImageServiceOsgiImpl extends AbstractCoverImageService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageServiceOsgiImpl.class);

  /**
   * OSGi activation callback
   *
   * @param cc
   *          the OSGi component context
   */
  @Override
  @Activate
  public void activate(ComponentContext cc) {
    super.activate(cc);

    logger.info("Cover image service activated");
  }

  /**
   * OSGi callback to set the workspace service.
   *
   * @param workspace
   *          the workspace service
   */
  @Reference
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi callback to set the service registry service
   *
   * @param serviceRegistry
   *          the service registry service
   */
  @Reference
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * OSGi callback to set the security service
   *
   * @param securityService
   *          the security service
   */
  @Reference
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service
   *
   * @param userDirectoryService
   *          the user directory service
   */
  @Reference
  protected void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the organization directory service
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  @Reference
  protected void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
