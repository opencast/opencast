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

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;

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

import java.text.ParseException;

/**
 * Schedules the run of a {@link AbstractWorkflowCleanupScheduler.Runner}
 */
public abstract class AbstractWorkflowCleanupScheduler {

  private static final String JOB_NAME = "mh-workflow-cleanup-job";
  private static final String JOB_GROUP = "mh-workflow-cleanup-job-group";
  private static final String TRIGGER_NAME = "mh-workflow-cleanup-trigger";
  private static final String TRIGGER_GROUP = "mh-workflow-cleanup-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  /** The quartz scheduler */
  private final Scheduler quartz;

  /** Get an organization directory service */
  public abstract OrganizationDirectoryService getOrganizationDirectoryService();

  /** Get a system administrator context for the given organization id. */
  public abstract SecurityContext getAdminContextFor(String orgId);

  /** Get a workflow service */
  public abstract WorkflowService getWorkflowService();

  /** Get a service registry */
  public abstract ServiceRegistry getServiceRegistry();

  /** Is the scheduler enabled? */
  protected boolean enabled = false;

  /** Cron schedule expression */
  protected String cronExpression = "0 0 2 * * ?"; // every day at 2:00 am

  /** Lifetime of successful jobs in days */
  protected static int lifetimeSuccessfulJobs = -1;

  /** Lifetime of failed jobs in days */
  protected static int lifetimeFailedJobs = -1;

  /** Lifetime of failed jobs in days */
  protected static int lifetimeStoppedJobs = -1;

  /** Lifetime of parentless jobs in days */
  protected static int lifetimeParentlessJobs = -1;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractWorkflowCleanupScheduler.class);

  protected AbstractWorkflowCleanupScheduler() {
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
    if (!enabled) {
      logger.info("Workflow cleanup job scheduler is disabled");
      return;
    }

    logger.info("Schedule workflow cleanup as a cron job ({})", cronExpression);
    try {
      final CronTrigger trigger = new CronTrigger();
      trigger.setCronExpression(cronExpression);
      trigger.setName(TRIGGER_NAME);
      trigger.setGroup(TRIGGER_GROUP);
      trigger.setJobName(JOB_NAME);
      trigger.setJobGroup(JOB_GROUP);
      if (quartz.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (ParseException e) {
      logger.error("Error scheduling workflow cleanup job, the cron expression '{}' could not be parsed: {}",
              cronExpression, e.getMessage());
    } catch (Exception e) {
      logger.error("Error scheduling workflow cleanup job", e);
    }
  }

  public void unschedule() {
    try {
      quartz.unscheduleJob(TRIGGER_NAME, TRIGGER_GROUP);
    } catch (SchedulerException e) {
      logger.error("Error unscheduling workflow cleanup job", e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  /** Trigger the scheduler once independent of it's actual schedule. */
  public void trigger() {
    try {
      quartz.triggerJobWithVolatileTrigger(JOB_NAME, JOB_GROUP);
    } catch (Exception e) {
      logger.error("Error triggering Quartz job", e);
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  /** Quartz job to which cleans up the workflow instances */
  public static class Runner extends TypedQuartzJob<AbstractWorkflowCleanupScheduler> {
    private static final NeedleEye eye = new NeedleEye();

    public Runner() {
      super(some(eye));
    }

    @Override
    protected void execute(final AbstractWorkflowCleanupScheduler parameters, JobExecutionContext ctx) {
      logger.debug("Starting workflow cleanup job");

      // iterate all organizations
      for (final Organization org : parameters.getOrganizationDirectoryService().getOrganizations()) {
        // set the organization on the current thread
        parameters.getAdminContextFor(org.getId()).runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              if (lifetimeFailedJobs > 0)
                parameters.getWorkflowService().cleanupWorkflowInstances(lifetimeFailedJobs,
                        WorkflowInstance.WorkflowState.FAILED);

              if (lifetimeSuccessfulJobs > 0)
                parameters.getWorkflowService().cleanupWorkflowInstances(lifetimeSuccessfulJobs,
                        WorkflowInstance.WorkflowState.SUCCEEDED);

              if (lifetimeStoppedJobs > 0)
                parameters.getWorkflowService().cleanupWorkflowInstances(lifetimeStoppedJobs,
                        WorkflowInstance.WorkflowState.STOPPED);

              if (lifetimeParentlessJobs > 0)
                parameters.getServiceRegistry().removeParentlessJobs(lifetimeParentlessJobs);

            } catch (WorkflowDatabaseException e) {
              logger.error("Unable to cleanup jobs: {}", e);
            } catch (UnauthorizedException e) {
              logger.error("Workflow cleanup job doesn't have right to delete jobs!");
              throw new IllegalStateException(e);
            } catch (ServiceRegistryException e) {
              logger.error("There was an error while removing parentless jobs: {}", e.getMessage());
            }
          }
        });
      }

      logger.info("Finished workflow cleanup job");
    }
  }

  /**
   * Please remember that derived classes need a no-arg constructor in order to work with Quartz. Sample usage:
   * 
   * <pre>
   * public class Runner extends TypedQuartzJob&lt;Scheduler&gt; {
   *   protected abstract void execute(Scheduler parameters, JobExecutionContext ctx) {
   *     // type safe parameter access
   *     parameters.getConfig();
   *   }
   * }
   * 
   * public class Scheduler {
   *   ...
   *   // create the job
   *   final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
   *   // set the scheduler as parameter source
   *   job.getJobDataMap().put(JOB_PARAM_PARENT, this);
   *   // add to Quartz scheduler
   *   quartz.addJob(job, true);
   *   ...
   *   // provide a config string
   *   public String getConfig() {...}
   * }
   * </pre>
   */
  public abstract static class TypedQuartzJob<A> implements Job {
    private final Option<NeedleEye> allowParallel;

    /**
     * @param allowParallel
     *          Pass a needle eye if only one job may be run at a time. Make the needle eye static to the inheriting
     *          class.
     */
    protected TypedQuartzJob(Option<NeedleEye> allowParallel) {
      this.allowParallel = allowParallel;
    }

    @Override
    public final void execute(final JobExecutionContext ctx) throws JobExecutionException {
      for (NeedleEye eye : allowParallel) {
        eye.apply(executeF(ctx));
        return;
      }
      executeF(ctx).apply();
    }

    /** Typesafe replacement for {@link #execute(org.quartz.JobExecutionContext)}. */
    protected abstract void execute(A parameters, JobExecutionContext ctx);

    private Function0<Integer> executeF(final JobExecutionContext ctx) {
      return new Function0.X<Integer>() {
        @Override
        public Integer xapply() throws Exception {
          try {
            execute((A) ctx.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT), ctx);
            return 0;
          } catch (Exception e) {
            logger.error("An error occurred while harvesting schedule", e);
            throw new JobExecutionException("An error occurred while harvesting schedule", e);
          }
        }
      };
    }
  }

}
