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
package org.opencastproject.kernel.scanner;

import static org.opencastproject.security.util.SecurityUtil.createSystemUser;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * This class is designed to provide a template for sub classes that will scan
 * their data and respond accordingly.
 */
public abstract class AbstractScanner {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractScanner.class);

  public static final String JOB_PARAM_PARENT = "parent";
  /** The key whose value will be used to determine if a scanner is enabled or disabled.*/
  public static final String PARAM_KEY_ENABLED = "enabled";
  /** The key whose value will be a cron expression to determine how often the Scanner scans.*/
  public static final String PARAM_KEY_CRON_EXPR = "cron-expression";

  /** The quartz scheduler */
  protected Scheduler quartz;

  /** Is the scanner enabled? */
  private boolean enabled = false;

  /** Cron schedule expression */
  private String cronExpression = "0 0 2 * * ?"; // every day at 2:00 am

  /** Reference to the service registry */
  private ServiceRegistry serviceRegistry;

  /** Reference to the security service */
  private SecurityService securityService;

  /** Reference to the organization directory service */
  private OrganizationDirectoryService directoryService;

  private String systemUserName;

  /** The name of the job group to schedule this quartz job under. */
  public abstract String getJobGroup();

  /** The name of the job */
  public abstract String getJobName();

  /** The name of the group to store the quartz triggers under. */
  public abstract String getTriggerGroupName();

  /** The name of the triggers to use for the quartz jobs. */
  public abstract String getTriggerName();

  /** The name of the scanner to use in log files. */
  public abstract String getScannerName();

  public abstract void scan();

  /** Returns a cron style statement that defines how often this scanner will run. */
  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  /**
   * @return Returns the current quartz scheduler used to schedule scanning jobs.
   */
  protected Scheduler getQuartz() {
    return quartz;
  }

  protected void setQuartz(Scheduler quartz) {
    this.quartz = quartz;
  }

  /** True if this scanner should be enabled. */
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Schedule the scanning job to execute according to the cron schedule.
   */
  public void schedule() {
    if (!isEnabled()) {
      logger.info(getScannerName() + " is disabled");
      return;
    }

    if (quartz == null) {
      logger.warn("No quartz scheduler available to schedule scanner.");
      return;
    }

    logger.info("Schedule " + getScannerName() + " as a cron job ({})", getCronExpression());
    try {
      final CronTrigger trigger = new CronTrigger();
      trigger.setCronExpression(getCronExpression());
      trigger.setName(getTriggerName());
      trigger.setGroup(getTriggerGroupName());
      trigger.setJobName(getJobName());
      trigger.setJobGroup(getJobGroup());
      if (getQuartz().getTriggersOfJob(getJobName(), getJobGroup()).length == 0) {
        getQuartz().scheduleJob(trigger);
      } else {
        getQuartz().rescheduleJob(getTriggerName(), getTriggerGroupName(), trigger);
      }
    } catch (ParseException e) {
      logger.error("Error scheduling " + getScannerName() + ", the cron expression '{}' could not be parsed: {}",
              getCronExpression(), e.getMessage());
    } catch (Exception e) {
      logger.error("Error scheduling " + getScannerName(), e);
    }
  }

  /**
   * Unschedule the scanning job.
   */
  public void unschedule() {
    try {
      if (quartz != null) {
        quartz.unscheduleJob(getTriggerName(), getTriggerGroupName());
      }
    } catch (SchedulerException e) {
      logger.error("Error unscheduling " + getScannerName(), e);
    }
  }

  /** OSGi callback to set organization directory service */
  protected void bindOrganizationDirectoryService(OrganizationDirectoryService directoryService) {
    this.directoryService = directoryService;
  }

  /** OSGi callback to set security service */
  protected void bindSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected void bindServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** Get an organization directory service */
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return directoryService;
  }

  /**
   * Get a system administrator context for the given organization id.
   * @param orgId The organization's id.
   * @return A SecurityContext for the admin.
   */
  public SecurityContext getAdminContextFor(String orgId) {
    try {
      final Organization org = directoryService.getOrganization(orgId);
      return new SecurityContext(securityService, org, createSystemUser(systemUserName, org));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /** Get a service registry */
  public ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  /** The user name to run this scanner job under. */
  public String getSystemUserName() {
    return systemUserName;
  }

  /** OSGi component activate callback */
  protected void activate(ComponentContext cc) {
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  /** OSGi deactivate component callback. */
  public void deactivate() {
    shutdown();
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      if (quartz != null) {
        quartz.shutdown();
      }
    } catch (org.quartz.SchedulerException e) {
      logger.debug("Exception while shutting down quartz scheduler this will be ignored: {}",
              ExceptionUtils.getStackTrace(e));
    }
  }

  /** Trigger the scheduler once independent of it's actual schedule. */
  public void trigger() {
    try {
      quartz.triggerJobWithVolatileTrigger(getJobName(), getJobGroup());
    } catch (Exception e) {
      logger.error("Error triggering Quartz job", e);
    }
  }

  /** Just to make sure Quartz is being shut down... */
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
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
