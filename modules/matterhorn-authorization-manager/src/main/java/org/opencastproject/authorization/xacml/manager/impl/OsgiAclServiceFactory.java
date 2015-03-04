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

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.index.IndexProducer;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang.WordUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** OSGi implementation of {@link org.opencastproject.authorization.xacml.manager.api.AclServiceFactory}. */
public class OsgiAclServiceFactory extends AbstractIndexProducer implements AclServiceFactory {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OsgiAclServiceFactory.class);

  private final String clazzName = OsgiAclServiceFactory.class.getName();

  private AclTransitionDb transitionDb;
  private AclDb aclDb;
  private SeriesService seriesService;
  private Archive<?> archive;
  private AuthorizationService authorizationService;
  private SearchService searchService;
  private WorkflowService workflowService;
  private SecurityService securityService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private ServiceRegistry serviceRegistry;
  private DistributionService distributionService;
  private MessageReceiver messageReceiver;
  private MessageSender messageSender;
  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;
  private ComponentContext cc;

  @Override
  public AclService serviceFor(Organization org) {
    return new AclServiceImpl(org, aclDb, transitionDb, seriesService, archive, searchService, workflowService,
            securityService, httpMediaPackageElementProvider, authorizationService, distributionService,
            serviceRegistry, messageSender);
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
  public void setArchive(Archive<?> archive) {
    this.archive = archive;
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

  /** OSGi DI callback. */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /** OSGi DI callback. */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /** OSGi DI callback. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Override
  public void repopulate(final String indexName) {
    final String destinationId = AclItem.ACL_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
        @Override
        protected void run() {
          AclService aclService = serviceFor(organization);
          List<ManagedAcl> acls = aclService.getAcls();
          int total = aclService.getAcls().size();
          logger.info("Re-populating index with acls. There are {} acls(s) to add to the index.", total);
          int current = 1;
          for (ManagedAcl acl : acls) {
            logger.trace("Adding acl '{}' for org '{}'", acl.getName(), organization.getId());
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    AclItem.create(acl.getName()));
            messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                    IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Acl, total, current));
            current++;
          }
        }
      });
    }

    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
      @Override
      protected void run() {
        messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Acl));
      }
    });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    this.cc = cc;
    super.activate();
  }

  @Override
  public Service getService() {
    return Service.Acl;
  }

  @Override
  public String getClassName() {
    return OsgiAclServiceFactory.class.getName();
  }

}
