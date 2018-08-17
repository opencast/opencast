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

package org.opencastproject.scheduler.impl;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.data.Effect0;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Clear outdated scheduler transactions {@link org.opencastproject.scheduler.api.SchedulerService.SchedulerTransaction}
 */
public class TransactionCleaner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(TransactionCleaner.class);

  private static final String JOB_NAME = "mh-scheduler-transaction-cleaner-job";
  private static final String JOB_GROUP = "mh-scheduler-transaction-cleaner-job-group";
  private static final String TRIGGER_NAME = "mh-scheduler-transaction-cleaner-trigger";
  private static final String TRIGGER_GROUP = "mh-scheduler-transaction-cleaner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  /** The scheduler service */
  private SchedulerService schedulerService;

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectoryService;

  /** The security service */
  private SecurityService securityService;

  /** The system user name */
  private String systemUserName;

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  public OrganizationDirectoryService getOrgDirectoryService() {
    return orgDirectoryService;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public String getSystemUserName() {
    return systemUserName;
  }

  protected TransactionCleaner(SchedulerService schedulerService, SecurityService securityService,
          OrganizationDirectoryService orgDirectoryService, String systemUserName) {
    this.schedulerService = RequireUtil.notNull(schedulerService, "schedulerService");
    this.securityService = RequireUtil.notNull(securityService, "securityService");
    this.orgDirectoryService = RequireUtil.notNull(orgDirectoryService, "orgDirectoryService");
    this.systemUserName = RequireUtil.notNull(systemUserName, "systemUserName");
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    logger.debug("Transaction cleaner is run every hour.");
    try {
      final Trigger trigger = TriggerUtils.makeHourlyTrigger();
      trigger.setStartTime(new Date());
      trigger.setName(TRIGGER_NAME);
      trigger.setGroup(TRIGGER_GROUP);
      trigger.setJobName(JOB_NAME);
      trigger.setJobGroup(JOB_GROUP);
      if (quartz.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.debug("Start scheduler transaction cleaner");
      try {
        execute((TransactionCleaner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while cleaning scheduler transactions", e);
      }
      logger.debug("Finished scheduler transaction cleaner");
    }

    private void execute(final TransactionCleaner transactionCleaner) throws UnauthorizedException, SchedulerException {
      for (final Organization org : transactionCleaner.getOrgDirectoryService().getOrganizations()) {
        User user = SecurityUtil.createSystemUser(transactionCleaner.getSystemUserName(), org);
        SecurityUtil.runAs(transactionCleaner.getSecurityService(), org, user, new Effect0() {
          @Override
          protected void run() {
            try {
              transactionCleaner.getSchedulerService().cleanupTransactions();
            } catch (UnauthorizedException | SchedulerException e) {
              logger.error("Unable to cleanup transactions for organization {}: {}", org, getStackTrace(e));
            }
          }
        });
      }
    }

  }

}
