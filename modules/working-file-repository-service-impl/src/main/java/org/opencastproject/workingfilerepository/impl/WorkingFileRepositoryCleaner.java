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

package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/** Clear outdated files {@link WorkingFileRepository}. */
public class WorkingFileRepositoryCleaner {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryCleaner.class);

  private static final String JOB_NAME = "working-file-repository-cleaner-job";
  private static final String JOB_GROUP = "working-file-repository-cleaner-job-group";
  private static final String TRIGGER_NAME = "working-file-repository-cleaner-trigger";
  private static final String TRIGGER_GROUP = "working-file-repository-cleaner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  private final WorkingFileRepository workingFileRepository;
  private final int maxAge;
  private int schedulerPeriod;
  private List<String> collectionIds;

  protected WorkingFileRepositoryCleaner(WorkingFileRepository workingFileRepository, int schedulerPeriod, int maxAge,
    List<String> collectionIds) {
    this.workingFileRepository = workingFileRepository;
    this.maxAge = maxAge;
    this.schedulerPeriod = schedulerPeriod;
    this.collectionIds = collectionIds;

    if (maxAge <= 0) {
      logger.debug("No scheduler initialized due to invalid max age setting ({})", schedulerPeriod);
      quartz = null;
      return;
    }

    // Continue only if we have a sensible period value
    if (schedulerPeriod <= 0) {
      logger.debug("No scheduler initialized due to invalid scheduling period ({})", schedulerPeriod);
      quartz = null;
      return;
    }

    if (collectionIds == null || collectionIds.size() == 0) {
      logger.debug("No scheduler initialized due to invalid working file collection ({})", collectionIds);
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

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    if (quartz == null || schedulerPeriod <= 0) {
      logger.warn("Cancel scheduling of workspace cleaner due to invalid scheduling period");
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

  // call working file repository cleaner to clean up
  private void cleanup() {
    for (String collectionId : collectionIds) {
      try {
        workingFileRepository.cleanupOldFilesFromCollection(collectionId, maxAge);
      } catch (IOException e) {
        logger.error("Cleaning of collection with id:{} failed", collectionId);
      }
    }
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.debug("Start working file repository cleaner");
      try {
        execute((WorkingFileRepositoryCleaner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while cleaning working file repository", e);
      }
      logger.debug("Finished working file repository cleaner");
    }

    private void execute(WorkingFileRepositoryCleaner workingFileRepositoryCleaner) {
        workingFileRepositoryCleaner.cleanup();
    }
  }
}
