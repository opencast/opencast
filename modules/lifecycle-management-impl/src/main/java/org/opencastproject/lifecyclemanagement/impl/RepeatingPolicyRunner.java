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

package org.opencastproject.lifecyclemanagement.impl;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RepeatingPolicyRunner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(RepeatingPolicyRunner.class);

  private static final String JOB_NAME = "mh-repeating-policy-runner-job";
  private static final String JOB_GROUP = "mh-repeating-policy-runner-job-group";
  private static final String TRIGGER_NAME = "mh-repeating-policy-runner-trigger";
  private static final String TRIGGER_GROUP = "mh-repeating-policy-runner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private String dynamicJobName;
  private String dynamicJobGroup;
  private String dynamicTriggerName;
  private String dynamicTriggerGroup;

  private final Scheduler quartz;

  private LifeCycleService lifeCycleService;
  private SecurityService securityService;
  private LifeCyclePolicy policy;
  private Organization organization;
  private User systemAdminUser;

  protected RepeatingPolicyRunner(
      LifeCyclePolicy policy,
      Organization organization,
      LifeCycleService lifeCycleService,
      SecurityService securityService,
      User systemAdminUser) {
    this.policy = policy;
    this.organization = organization;
    this.lifeCycleService = lifeCycleService;
    this.securityService = securityService;
    this.systemAdminUser = systemAdminUser;

    this.dynamicJobName = JOB_NAME + policy.getId();
    this.dynamicJobGroup = JOB_GROUP + policy.getId();
    this.dynamicTriggerName = TRIGGER_NAME + policy.getId();
    this.dynamicTriggerGroup = TRIGGER_GROUP + policy.getId();

    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(dynamicJobName, dynamicJobGroup, Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public LifeCyclePolicy getLifeCyclePolicy() {
    return policy;
  }

  public LifeCycleService getLifeCycleService() {
    return lifeCycleService;
  }

  public Organization getOrganization() {
    return organization;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public User getSystemAdminUser() {
    return systemAdminUser;
  }

  public void schedule() {
    logger.info("Scheduling repeating policy runner to run every {} seconds.", policy.getCronTrigger());
    try {
      final CronTrigger trigger = new CronTrigger();
      trigger.setCronExpression(policy.getCronTrigger());
      trigger.setName(dynamicTriggerName);
      trigger.setGroup(dynamicTriggerGroup);
      trigger.setJobName(dynamicJobName);
      trigger.setJobGroup(dynamicJobGroup);

      if (quartz.getTriggersOfJob(dynamicJobName, dynamicJobGroup).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(dynamicTriggerName, dynamicTriggerGroup, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  public void unschedule() {
    try {
      if (quartz != null) {
        quartz.unscheduleJob(dynamicTriggerName, dynamicTriggerGroup);
      }
    } catch (SchedulerException e) {
      logger.error("Error unscheduling " + policy.getTitle(), e);
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
      logger.info("Start repeating policy runner");
      try {
        execute((RepeatingPolicyRunner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while running policy", e);
      }
      logger.info("Finished repeating policy runner");
    }

    private void execute(RepeatingPolicyRunner repeatingPolicyRunner) {
      logger.info("Starting RepeatingPolicyRunner job for " + repeatingPolicyRunner.getLifeCyclePolicy().getTitle());
      SecurityUtil.runAs(repeatingPolicyRunner.getSecurityService(), repeatingPolicyRunner.getOrganization(),
          repeatingPolicyRunner.getSystemAdminUser(), () -> {
            try {
              // Check if this should stop running
              LifeCyclePolicy policy;
              try {
                policy = repeatingPolicyRunner.getLifeCycleService().getLifeCyclePolicyById(
                    repeatingPolicyRunner.getLifeCyclePolicy().getId()
                );
                if (!policy.isActive()) {
                  repeatingPolicyRunner.unschedule();
                  return;
                }
                logger.info("Exists: " + policy.getTitle());
              } catch (NotFoundException e) {
                logger.info("Unschedule " + repeatingPolicyRunner.getLifeCyclePolicy().getTitle());
                repeatingPolicyRunner.unschedule();
                return;
              }

              // Filter for entities
              List<Event> events = repeatingPolicyRunner.getLifeCycleService()
                  .filterForEntities(policy.getTargetFilters());

              // For every entity
              for (Event event : events) {
                // If entity does not yet have a task for this policy
                try {
                  repeatingPolicyRunner.getLifeCycleService().getLifeCycleTaskByTargetId(event.getIdentifier());
                  // Task does exist, skip creating one
                  continue;
                } catch (NotFoundException e) {
                  // Task does not exist yet, so create one
                }

                // Create task
                LifeCycleTask task;
                if (policy.getAction() == Action.START_WORKFLOW) {
                  task = new LifeCycleTaskStartWorkflow();
                } else {
                  task = new LifeCycleTaskImpl();
                }

                task.setLifeCyclePolicyId(policy.getId());
                task.setTargetId(event.getIdentifier());
                task.setStatus(Status.SCHEDULED);

                repeatingPolicyRunner.getLifeCycleService().createLifeCycleTask(task);
                logger.info("Created task based on policy " + policy.getTitle());
              }
            } catch (SearchIndexException e) {
              logger.warn(e.toString());
            } catch (UnauthorizedException e) {
              logger.warn(e.toString());
            }
          });
      logger.info("Finished RepeatingPolicyRunner job for " + repeatingPolicyRunner.getLifeCyclePolicy().getTitle());
    }
  }
}
