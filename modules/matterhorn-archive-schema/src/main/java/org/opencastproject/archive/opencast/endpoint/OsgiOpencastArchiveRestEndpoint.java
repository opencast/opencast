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

package org.opencastproject.archive.opencast.endpoint;

import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.kernel.rest.RestEndpoint;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.osgi.SimpleServicePublisher;
import org.opencastproject.workflow.api.WorkflowService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

import static org.opencastproject.util.OsgiUtil.getComponentContextProperty;
import static org.opencastproject.util.OsgiUtil.getContextProperty;

/** OSGi bound implementation. */
@Path("/")
public final class OsgiOpencastArchiveRestEndpoint extends AbstractOpencastArchiveRestEndpoint implements RestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(OsgiOpencastArchiveRestEndpoint.class);

  private OpencastArchive archive;
  private WorkflowService workflowService;
  private SecurityService securityService;
  private String serverUrl;
  private String mountPoint;
  private ComponentContext cc;

  @Override
  public OpencastArchive getArchive() {
    return archive;
  }

  @Override
  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  @Override
  public String getServerUrl() {
    return serverUrl;
  }

  @Override public String getMountPoint() {
    return mountPoint;
  }

  @Override public SecurityService getSecurityService() {
    return securityService;
  }

  @Override public void endpointPublished() {
    logger.info("Publishing as HttpMediaPackageElementProvider");
    SimpleServicePublisher.registerService(cc, this, HttpMediaPackageElementProvider.class, "HttpMediaPackageElementProvider");
  }

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    this.cc = cc;
    serverUrl = getContextProperty(cc, "org.opencastproject.server.url");
    mountPoint = getComponentContextProperty(cc, "opencast.service.path");
  }

  /** OSGi DI callback. */
  public void setArchive(OpencastArchive archive) {
    this.archive = archive;
  }

  /** OSGi DI callback. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
