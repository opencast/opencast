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
package org.opencastproject.workflow.impl;

import static org.opencastproject.security.util.SecurityUtil.createSystemUser;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Implementation of {@link AbstractWorkflowCleanupScheduler} to run in an OSGi environment
 */
public class OsgiWorkflowCleanupScheduler extends AbstractWorkflowCleanupScheduler implements ManagedService {

  /** Reference to the Workflow service */
  private WorkflowService workflowService;

  /** Reference to the security service */
  private SecurityService securityService;

  /** Reference to the organization directory service */
  private OrganizationDirectoryService directoryService;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OsgiWorkflowCleanupScheduler.class);

  private static final String PARAM_KEY_ENABLED = "enabled";
  private static final String PARAM_KEY_CRON_EXPR = "cron-expression";
  private static final String PARAM_KEY_LIFETIME_SUCCEEDED = "lifetime.succeeded";
  private static final String PARAM_KEY_LIFETIME_FAILED = "lifetime.failed";
  private static final String PARAM_KEY_LIFETIME_STOPPED = "lifetime.stopped";

  private String systemUserName;

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(PARAM_KEY_ENABLED));
      logger.debug("enabled = {}", enabled);

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression))
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      logger.debug("cronExpression = {}", cronExpression);

      lifetimeSuccessfulJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_SUCCEEDED));
      logger.debug("lifetimeFinishedJobs = {}", lifetimeSuccessfulJobs);

      lifetimeFailedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_FAILED));
      logger.debug("lifetimeFailedJobs = {}", lifetimeFailedJobs);

      lifetimeStoppedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_STOPPED));
      logger.debug("lifetimeStoppedJobs = {}", lifetimeStoppedJobs);
    }

    schedule();
  }

  @Override
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return directoryService;
  }

  @Override
  public SecurityContext getAdminContextFor(String orgId) {
    try {
      final Organization org = directoryService.getOrganization(orgId);
      return new SecurityContext(securityService, org, createSystemUser(systemUserName, org));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  @Override
  public WorkflowService getWorkflowService() {
    return this.workflowService;
  }

  /** OSGi component activate callback */
  protected void activate(ComponentContext cc) {
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  /** OSGi deactivate component callback. */
  public void deactivate() {
    shutdown();
  }

  /** OSGi callback to set workflow service */
  protected void bindWorkflowService(WorkflowService srv) {
    this.workflowService = srv;
  }

  /** OSGi callback to set organization directory service */
  protected void bindOrganizationDirectoryService(OrganizationDirectoryService directoryService) {
    this.directoryService = directoryService;
  }

  /** OSGi callback to set security service */
  protected void bindSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
