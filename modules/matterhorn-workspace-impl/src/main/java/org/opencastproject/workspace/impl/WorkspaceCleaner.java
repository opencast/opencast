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

package org.opencastproject.workspace.impl;

import org.opencastproject.workspace.api.Workspace;

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

/** Clear outdated workspace files {@link Workspace}. */
public class WorkspaceCleaner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceCleaner.class);

  private static final String JOB_NAME = "mh-workspace-cleaner-job";
  private static final String JOB_GROUP = "mh-workspace-cleaner-job-group";
  private static final String TRIGGER_NAME = "mh-workspace-cleaner-trigger";
  private static final String TRIGGER_GROUP = "mh-workspace-cleaner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  private final Workspace workspace;
  private final int maxAge;
  private int schedulerPeriod;

  protected WorkspaceCleaner(Workspace workspace, int schedulerPeriod, int maxAge) {
    this.workspace = workspace;
    this.maxAge = maxAge;
    this.schedulerPeriod = schedulerPeriod;

    // Continue only if we have a sensible period value
    if (schedulerPeriod <= 0) {
      logger.debug("No scheduler initialized due to invalid scheduling period ({})", schedulerPeriod);
      quartz = null;
      return;
    }

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

  public Workspace getWorkspace() {
    return workspace;
  }

  public int getMaxAge() {
    return maxAge;
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    if (quartz == null || schedulerPeriod <= 0) {
      logger.debug("Cancel scheduling of workspace cleaner due to invalid scheduling period");
      return;
    }
    logger.debug("Scheduling workspace cleaner to run every {} seconds.", schedulerPeriod);
    try {
      final Trigger trigger = TriggerUtils.makeSecondlyTrigger(schedulerPeriod);
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
      logger.debug("Start workspace cleaner");
      try {
        execute((WorkspaceCleaner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while cleaning workspace", e);
      }
      logger.debug("Finished workspace cleaner");
    }

    private void execute(WorkspaceCleaner workspaceCleaner) {
      workspaceCleaner.getWorkspace().cleanup(workspaceCleaner.getMaxAge());
    }

  }

}
