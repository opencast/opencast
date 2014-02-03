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
package org.opencastproject.episode.endpoint;

import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workflow.api.WorkflowService;
import org.osgi.service.component.ComponentContext;

import javax.ws.rs.Path;

import static org.opencastproject.util.OsgiUtil.getComponentContextProperty;
import static org.opencastproject.util.OsgiUtil.getContextProperty;

/** OSGi bound implementation. */
@Path("/")
public final class OsgiEpisodeServiceRestEndpoint extends AbstractEpisodeServiceRestEndpoint {
  private EpisodeService episodeService;
  private WorkflowService workflowService;
  private SecurityService securityService;
  private String serverUrl;
  private String mountPoint;

  @Override
  public EpisodeService getEpisodeService() {
    return episodeService;
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

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    serverUrl = getContextProperty(cc, "org.opencastproject.server.url");
    mountPoint = getComponentContextProperty(cc, "opencast.service.path");
  }

  /** OSGi DI callback. */
  public void setEpisodeService(EpisodeService episodeService) {
    this.episodeService = episodeService;
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
