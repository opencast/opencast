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

package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;

import org.apache.commons.lang3.text.WordUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** OSGi implementation of {@link org.opencastproject.authorization.xacml.manager.api.AclServiceFactory}. */
public class OsgiAclServiceFactory extends AbstractIndexProducer implements AclServiceFactory {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OsgiAclServiceFactory.class);

  private AclDb aclDb;
  private SeriesService seriesService;
  private AssetManager assetManager;
  private AuthorizationService authorizationService;
  private SecurityService securityService;
  private MessageReceiver messageReceiver;
  private MessageSender messageSender;
  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService;
  private ComponentContext cc;

  @Override
  public AclService serviceFor(Organization org) {
    return new AclServiceImpl(org, aclDb, seriesService, assetManager,
            authorizationService, messageSender);
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
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi DI callback. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI callback. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
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
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), () -> {
        AclService aclService = serviceFor(organization);
        List<ManagedAcl> acls = aclService.getAcls();
        int total = aclService.getAcls().size();
        logIndexRebuildBegin(logger, indexName, total, "ACLs", organization);
        int current = 1;
        for (ManagedAcl acl : acls) {
          messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                  AclItem.create(acl.getName()));
          logIndexRebuildProgress(logger, indexName, total, current);
          current++;
        }
      });
    }
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    this.cc = cc;
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Acl;
  }
}
