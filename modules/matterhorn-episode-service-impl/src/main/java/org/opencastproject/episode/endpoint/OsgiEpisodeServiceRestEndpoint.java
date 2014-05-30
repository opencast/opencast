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

import static org.opencastproject.systems.MatterhornConstans.SERVER_URL_PROPERTY;
import static org.opencastproject.util.OsgiUtil.getComponentContextProperty;
import static org.opencastproject.util.OsgiUtil.getContextProperty;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workflow.api.WorkflowService;

import org.osgi.service.component.ComponentContext;

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/")
public final class OsgiEpisodeServiceRestEndpoint extends AbstractEpisodeServiceRestEndpoint {
  // todo this key is also defined in RuntimeInfo. Access to these properties should be unified somewhere else
  private static final String ADMIN_URL_PROPERTY = "org.opencastproject.admin.ui.url";

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
    Organization org = securityService.getOrganization();
    // return the organization specific admin url or the global server url as a fallback
    return option(org.getProperties().get(ADMIN_URL_PROPERTY)).getOrElse(serverUrl);
  }

  @Override
  public String getMountPoint() {
    return mountPoint;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    serverUrl = getContextProperty(cc, SERVER_URL_PROPERTY);
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
