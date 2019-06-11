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
package org.opencastproject.terminationstate.impl;

import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.terminationstate.api.AbstractJobTerminationStateService;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public final class TerminationStateServiceImpl extends AbstractJobTerminationStateService {
  private static final Log logger = new Log(LoggerFactory.getLogger(TerminationStateServiceImpl.class));

  public static final String CONFIG_JOB_POLLING_PERIOD = "job.polling.period";
  private static final int DEFAULT_JOB_POLLING_PERIOD = 300; // secs

  protected static final String SCHEDULE_GROUP = AbstractJobTerminationStateService.class.getSimpleName();
  private static final String SCHEDULE_JOB_POLLING = "JobPolling";
  protected static final String SCHEDULE_JOB_POLLING_TRIGGER = "TriggerJobPolling";
  private static final String SCHEDULE_JOB_PARAM_PARENT = "parent";

  private Scheduler scheduler;

  private int jobPollingPeriod = DEFAULT_JOB_POLLING_PERIOD;


  protected void activate(ComponentContext componentContext) {
    try {
      configure(componentContext.getProperties());
    } catch (ConfigurationException e) {
      logger.error("Unable to read configuration, using defaults", e.getMessage());
    }

    try {
      scheduler = new StdSchedulerFactory().getScheduler();
    } catch (SchedulerException e) {
      logger.error("Cannot create quartz scheduler", e.getMessage());
    }
  }


  protected void configure(Dictionary config) throws ConfigurationException {
    this.jobPollingPeriod = OsgiUtil.getOptCfgAsInt(config, CONFIG_JOB_POLLING_PERIOD).getOrElse(DEFAULT_JOB_POLLING_PERIOD);
  }

  @Override
  public void setState(TerminationState state) {
    super.setState(state);

    if (getState() != TerminationState.NONE) {
      // stop accepting new jobs, maintenance mode
      try {
        String host = getServiceRegistry().getRegistryHostname();
        getServiceRegistry().setMaintenanceStatus(host, true);
      } catch (ServiceRegistryException | NotFoundException e) {
        logger.error("Cannot put this host into maintenance", e);
      }
      startJobPolling();
    } else {
      // termination terminated? unset maintenance
      try {
        String host = getServiceRegistry().getRegistryHostname();
        getServiceRegistry().setMaintenanceStatus(host, false);
      } catch (ServiceRegistryException | NotFoundException e) {
        logger.error("Cannot take this host out of maintenance", e);
      }
    }
  }

  protected void startJobPolling() {
    try {
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(SCHEDULE_GROUP, SCHEDULE_JOB_POLLING, CheckTerminationState.class);
      job.getJobDataMap().put(SCHEDULE_JOB_PARAM_PARENT, this);
      final Trigger trigger = TriggerUtils.makeSecondlyTrigger(jobPollingPeriod);
      trigger.setGroup(SCHEDULE_GROUP);
      trigger.setName(SCHEDULE_JOB_POLLING_TRIGGER);
      scheduler.scheduleJob(job, trigger);
      scheduler.start();
      logger.info("Started polling if jobs are complete");
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stopJobPolling() {
    try {
      scheduler.deleteJob(SCHEDULE_GROUP, SCHEDULE_JOB_POLLING);
    } catch (SchedulerException e) {
      // ignore
    }
  }

  public static class CheckTerminationState implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      TerminationStateServiceImpl parent = (TerminationStateServiceImpl) context.getJobDetail().getJobDataMap().get(SCHEDULE_JOB_PARAM_PARENT);

      if (parent.readyToTerminate()) {
        logger.info("No jobs running, sent complete Lifecycle action");
        parent.stopJobPolling();
      } else if (parent.getState() == TerminationState.WAIT) {
        logger.info("Jobs still running");
      }
    }
  }

  /**
   * Stop scheduled jobs and free resources
   */
  private void stop() {
    try {
      if (scheduler != null) {
        this.scheduler.shutdown();
      }
    } catch (SchedulerException e) {
      logger.error("Failed to stop scheduler", e);
    }
  }

  /**
   * OSGI deactivate callback
   */
  public void deactivate() {
    stop();
  }

  /** Methods below are used by test class */

  protected void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }
}
