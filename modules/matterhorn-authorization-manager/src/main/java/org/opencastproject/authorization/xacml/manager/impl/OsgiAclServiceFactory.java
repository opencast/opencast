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

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.HttpMediaPackageElementProvider;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OSGi implementation of {@link org.opencastproject.authorization.xacml.manager.api.AclServiceFactory}. */
public class OsgiAclServiceFactory implements AclServiceFactory {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(AclServiceImpl.class);

  private AclTransitionDb transitionDb;
  private AclDb aclDb;
  private SeriesService seriesService;
  private EpisodeService episodeService;
  private AuthorizationService authorizationService;
  private SearchService searchService;
  private WorkflowService workflowService;
  private SecurityService securityService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private ServiceRegistry serviceRegistry;
  private DistributionService distributionService;

  @Override
  public AclService serviceFor(Organization org) {
    return new AclServiceImpl(org, aclDb, transitionDb, seriesService, episodeService, searchService, workflowService,
            securityService, httpMediaPackageElementProvider, authorizationService, distributionService,
            serviceRegistry);
  }

  /** OSGi DI callback. */
  public void setTransitionDb(AclTransitionDb transitionDb) {
    this.transitionDb = transitionDb;
  }

  /** OSGi DI callback. */
  public void setAclDb(AclDb aclDb) {
    this.aclDb = aclDb;
  }

  /** OSGi DI callback. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI callback. */
  public void setEpisodeService(EpisodeService episodeService) {
    this.episodeService = episodeService;
  }

  /** OSGi DI callback. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI callback. */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /** OSGi DI callback. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI callback. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** OSGi DI callback. */
  public void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /** OSGi DI callback. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }
}
